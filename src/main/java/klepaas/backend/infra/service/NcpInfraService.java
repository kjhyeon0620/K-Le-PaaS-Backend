package klepaas.backend.infra.service;

import klepaas.backend.deployment.entity.Deployment;
import klepaas.backend.deployment.entity.SourceRepository;
import klepaas.backend.global.exception.BusinessException;
import klepaas.backend.global.exception.ErrorCode;
import klepaas.backend.infra.CloudInfraProvider;
import klepaas.backend.infra.dto.BuildResult;
import klepaas.backend.infra.dto.BuildStatusResult;
import klepaas.backend.infra.ncp.NcpSourceBuildClient;
import klepaas.backend.infra.ncp.dto.BuildStatusResponse;
import klepaas.backend.infra.ncp.dto.BuildTriggerResponse;
import klepaas.backend.infra.ncp.dto.CreateBuildProjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Service("ncpInfraService")
@RequiredArgsConstructor
public class NcpInfraService implements CloudInfraProvider {

    private final S3Client s3Client;
    private final RestClient githubClient;
    private final NcpSourceBuildClient sourceBuildClient;

    @Value("${cloud.ncp.storage.bucket}")
    private String bucketName;

    @Value("${cloud.ncp.container-registry.endpoint}")
    private String registryEndpoint;

    @Override
    public String uploadSourceToStorage(String gitToken, Deployment deployment) {
        SourceRepository repo = deployment.getSourceRepository();
        String storageKey = "builds/" + deployment.getId() + "/source.zip";

        try {
            // GitHub ZIP 다운로드
            String zipUrl = String.format("/repos/%s/%s/zipball/%s",
                    repo.getOwner(), repo.getRepoName(), deployment.getCommitHash());

            byte[] zipBytes = githubClient.get()
                    .uri(zipUrl)
                    .header("Authorization", "Bearer " + gitToken)
                    .retrieve()
                    .body(byte[].class);

            if (zipBytes == null || zipBytes.length == 0) {
                throw new BusinessException(ErrorCode.SOURCE_UPLOAD_FAILED, "GitHub ZIP 다운로드 결과가 비어있습니다");
            }

            // S3 (NCP Object Storage) 업로드
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(storageKey)
                    .contentType("application/zip")
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(zipBytes));
            log.info("Source uploaded: bucket={}, key={}, size={}", bucketName, storageKey, zipBytes.length);

            return storageKey;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Source upload failed: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SOURCE_UPLOAD_FAILED, "소스 업로드 실패: " + e.getMessage());
        }
    }

    @Override
    public BuildResult triggerBuild(String storageKey, Deployment deployment) {
        SourceRepository repo = deployment.getSourceRepository();

        try {
            String projectId = repo.getExternalBuildProjectId();

            // 프로젝트가 없으면 생성
            if (projectId == null) {
                String imageName = repo.getOwner() + "-" + repo.getRepoName();
                CreateBuildProjectRequest createRequest = CreateBuildProjectRequest.of(
                        "klepaas-" + repo.getId(),
                        bucketName,
                        storageKey,
                        registryEndpoint,
                        imageName
                );
                projectId = sourceBuildClient.createProject(createRequest);
                repo.assignBuildProjectId(projectId);
                log.info("SourceBuild project created: projectId={}, repoId={}", projectId, repo.getId());
            }

            // 빌드 트리거
            BuildTriggerResponse triggerResponse = sourceBuildClient.triggerBuild(projectId);
            log.info("Build triggered: projectId={}, buildId={}", projectId, triggerResponse.buildId());

            return new BuildResult(triggerResponse.buildId(), projectId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Build trigger failed: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.BUILD_TRIGGER_FAILED, "빌드 트리거 실패: " + e.getMessage());
        }
    }

    @Override
    public BuildStatusResult getBuildStatus(String projectId, String buildId) {
        BuildStatusResponse response = sourceBuildClient.getBuildStatus(projectId, buildId);
        return new BuildStatusResult(
                response.isCompleted(),
                response.isSuccess(),
                response.imageUri(),
                response.statusMessage()
        );
    }

    @Override
    public void scaleService(String resourceName, int replicas) {
        // K8s 스케일링은 KubernetesManifestGenerator에서 처리
        log.info("Scale requested via NCP: resource={}, replicas={}", resourceName, replicas);
    }
}
