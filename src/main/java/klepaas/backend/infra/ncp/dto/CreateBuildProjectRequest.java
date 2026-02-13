package klepaas.backend.infra.ncp.dto;

import java.util.Map;

/**
 * NCP SourceBuild 프로젝트 생성 요청 payload.
 * Template Payload 패턴: 기본 설정 고정, 동적 값(projectName, bucketName, objectKey 등)만 주입.
 */
public record CreateBuildProjectRequest(
        String projectName,
        String description,
        Map<String, Object> source,
        Map<String, Object> build,
        Map<String, Object> artifact
) {

    public static CreateBuildProjectRequest of(
            String projectName,
            String bucketName,
            String objectKey,
            String registryEndpoint,
            String imageName
    ) {
        Map<String, Object> source = Map.of(
                "type", "ObjectStorage",
                "config", Map.of(
                        "bucket", bucketName,
                        "object", objectKey
                )
        );

        Map<String, Object> build = Map.of(
                "cmd", Map.of(
                        "pre", new String[]{},
                        "build", new String[]{"docker build -t " + imageName + " ."}
                ),
                "timeout", 3600,
                "env", Map.of()
        );

        Map<String, Object> artifact = Map.of(
                "use", true,
                "type", "ContainerRegistry",
                "config", Map.of(
                        "registry", registryEndpoint,
                        "image", imageName,
                        "tag", "latest"
                )
        );

        return new CreateBuildProjectRequest(
                projectName,
                "KLePaaS auto-generated build project",
                source,
                build,
                artifact
        );
    }
}
