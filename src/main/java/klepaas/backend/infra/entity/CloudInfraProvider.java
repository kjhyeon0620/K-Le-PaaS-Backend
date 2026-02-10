package klepaas.backend.infra.entity;

import klepaas.backend.deployment.entity.Deployment;
import klepaas.backend.infra.dto.BuildResult;

public interface CloudInfraProvider {

    /**
     * 1. 소스코드 스트리밍 업로드 (Source Staging)
     * GitHub ZIP 스트림 -> Object Storage
     * @param gitToken 사용자 GitHub 토큰
     * @param deployment 배포 엔티티 (Repo URL, Commit Hash 등 포함)
     * @return 업로드된 스토리지 경로 (Key)
     */
    String uploadSourceToStorage(String gitToken, Deployment deployment);

    /**
     * 2. 빌드 및 배포 트리거
     * @param storageKey Object Storage에 업로드된 소스 경로
     * @return 클라우드 벤더의 Build ID
     */
    BuildResult triggerBuildAndDeploy(String storageKey, Deployment deployment);

    /**
     * 3. 스케일링
     */
    void scaleService(String resourceName, int replicas);
}
