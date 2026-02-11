package klepaas.backend.infra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${cloud.ncp.api.base-url}")
    private String ncpApiBaseUrl;

    @Value("${cloud.ncp.source-build.base-url}")
    private String ncpSourceBuildBaseUrl;

    // GitHub API 호출용 클라이언트
    @Bean
    public RestClient githubClient() {
        return RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }

    // NCP API (NKS 등) 호출용 클라이언트
    @Bean
    public RestClient ncpApiClient() {
        return RestClient.builder()
                .baseUrl(ncpApiBaseUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    // NCP SourceBuild API 호출용 클라이언트
    @Bean
    public RestClient ncpSourceBuildClient() {
        return RestClient.builder()
                .baseUrl(ncpSourceBuildBaseUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
