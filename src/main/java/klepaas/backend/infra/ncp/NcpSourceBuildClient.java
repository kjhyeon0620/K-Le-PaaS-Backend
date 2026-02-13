package klepaas.backend.infra.ncp;

import klepaas.backend.global.exception.BusinessException;
import klepaas.backend.global.exception.ErrorCode;
import klepaas.backend.infra.ncp.dto.BuildStatusResponse;
import klepaas.backend.infra.ncp.dto.BuildTriggerResponse;
import klepaas.backend.infra.ncp.dto.CreateBuildProjectRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
public class NcpSourceBuildClient {

    private final RestClient sourceBuildClient;
    private final NcpApiSigner apiSigner;

    public NcpSourceBuildClient(
            @Qualifier("sourceBuildRestClient") RestClient sourceBuildClient,
            NcpApiSigner apiSigner
    ) {
        this.sourceBuildClient = sourceBuildClient;
        this.apiSigner = apiSigner;
    }

    /**
     * SourceBuild 프로젝트 생성
     * @return 생성된 프로젝트 ID
     */
    public String createProject(CreateBuildProjectRequest request) {
        String uri = "/api/v1/project";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String signature = apiSigner.makeSignature("POST", uri, timestamp);

        Map<String, Object> response = sourceBuildClient.post()
                .uri(uri)
                .header("x-ncp-apigw-timestamp", timestamp)
                .header("x-ncp-iam-access-key", apiSigner.getAccessKey())
                .header("x-ncp-apigw-signature-v2", signature)
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    log.error("NCP SourceBuild createProject failed: status={}", res.getStatusCode());
                    throw new BusinessException(ErrorCode.NCP_API_ERROR,
                            "SourceBuild 프로젝트 생성 실패: " + res.getStatusCode());
                })
                .body(Map.class);

        if (response == null || !response.containsKey("id")) {
            throw new BusinessException(ErrorCode.NCP_API_ERROR, "SourceBuild 프로젝트 생성 응답에 ID가 없습니다");
        }
        return String.valueOf(response.get("id"));
    }

    /**
     * 빌드 트리거
     */
    public BuildTriggerResponse triggerBuild(String projectId) {
        String uri = "/api/v1/project/" + projectId + "/build";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String signature = apiSigner.makeSignature("POST", uri, timestamp);

        return sourceBuildClient.post()
                .uri(uri)
                .header("x-ncp-apigw-timestamp", timestamp)
                .header("x-ncp-iam-access-key", apiSigner.getAccessKey())
                .header("x-ncp-apigw-signature-v2", signature)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    log.error("NCP SourceBuild triggerBuild failed: status={}", res.getStatusCode());
                    throw new BusinessException(ErrorCode.BUILD_TRIGGER_FAILED,
                            "빌드 트리거 실패: " + res.getStatusCode());
                })
                .body(BuildTriggerResponse.class);
    }

    /**
     * 빌드 상태 조회
     */
    public BuildStatusResponse getBuildStatus(String projectId, String buildId) {
        String uri = "/api/v1/project/" + projectId + "/build/" + buildId;
        String timestamp = String.valueOf(System.currentTimeMillis());
        String signature = apiSigner.makeSignature("GET", uri, timestamp);

        return sourceBuildClient.get()
                .uri(uri)
                .header("x-ncp-apigw-timestamp", timestamp)
                .header("x-ncp-iam-access-key", apiSigner.getAccessKey())
                .header("x-ncp-apigw-signature-v2", signature)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    log.error("NCP SourceBuild getBuildStatus failed: status={}", res.getStatusCode());
                    throw new BusinessException(ErrorCode.NCP_API_ERROR,
                            "빌드 상태 조회 실패: " + res.getStatusCode());
                })
                .body(BuildStatusResponse.class);
    }
}
