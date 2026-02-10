package klepaas.backend.infra.service;

import klepaas.backend.deployment.entity.Deployment;
import klepaas.backend.infra.dto.BuildResult;
import klepaas.backend.infra.entity.CloudInfraProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import software.amazon.awssdk.services.s3.S3Client;

@Slf4j
@Service("ncpInfraService")
@RequiredArgsConstructor
public class NcpInfraService implements CloudInfraProvider {
    private final S3Client s3Client; // NCP Object Storage 연결용
    private final RestClient githubClient; // GitHub API 호출용
    private final RestClient ncpApiClient; // NCP SourceBuild 호출용

    @Value("${cloud.ncp.storage.bucket}")
    private String bucketName;


    /**
     * GitHub -> NCP Obeject Storage 스트리밍 업로드
     */
    @Override
    public String uploadSourceToStorage(String gitToken, Deployment deployment) {
        return "";
    }

    @Override
    public BuildResult triggerBuildAndDeploy(String storageKey, Deployment deployment) {
        return null;
    }

    @Override
    public void scaleService(String resourceName, int replicas) {

    }

}
