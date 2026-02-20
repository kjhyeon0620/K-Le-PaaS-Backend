package klepaas.backend.ai.service;

import klepaas.backend.ai.dto.ParsedIntent;
import klepaas.backend.ai.entity.Intent;
import klepaas.backend.ai.entity.RiskLevel;
import klepaas.backend.deployment.dto.DeploymentStatusResponse;
import klepaas.backend.deployment.dto.RepositoryResponse;
import klepaas.backend.deployment.entity.CloudVendor;
import klepaas.backend.deployment.entity.DeploymentStatus;
import klepaas.backend.deployment.service.DeploymentService;
import klepaas.backend.deployment.service.RepositoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ActionDispatcherTest {

    @InjectMocks
    private ActionDispatcher actionDispatcher;

    @Mock
    private DeploymentService deploymentService;

    @Mock
    private RepositoryService repositoryService;

    @Test
    @DisplayName("DEPLOY는 HIGH 리스크")
    void classifyDeployAsHigh() {
        assertThat(actionDispatcher.classifyRisk(Intent.DEPLOY)).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    @DisplayName("SCALE은 MEDIUM 리스크")
    void classifyScaleAsMedium() {
        assertThat(actionDispatcher.classifyRisk(Intent.SCALE)).isEqualTo(RiskLevel.MEDIUM);
    }

    @Test
    @DisplayName("RESTART은 MEDIUM 리스크")
    void classifyRestartAsMedium() {
        assertThat(actionDispatcher.classifyRisk(Intent.RESTART)).isEqualTo(RiskLevel.MEDIUM);
    }

    @Test
    @DisplayName("STATUS는 LOW 리스크")
    void classifyStatusAsLow() {
        assertThat(actionDispatcher.classifyRisk(Intent.STATUS)).isEqualTo(RiskLevel.LOW);
    }

    @Test
    @DisplayName("HELP는 LOW 리스크")
    void classifyHelpAsLow() {
        assertThat(actionDispatcher.classifyRisk(Intent.HELP)).isEqualTo(RiskLevel.LOW);
    }

    @Test
    @DisplayName("STATUS 디스패치 시 DeploymentService 호출")
    void dispatchStatus() {
        var parsedIntent = new ParsedIntent(Intent.STATUS, Map.of("deployment_id", 1), 0.9, "상태 확인");
        var statusResponse = new DeploymentStatusResponse(1L, DeploymentStatus.SUCCESS, null);
        given(deploymentService.getDeploymentStatus(1L)).willReturn(statusResponse);

        String result = actionDispatcher.dispatch(parsedIntent, 1L);

        assertThat(result).contains("SUCCESS");
        verify(deploymentService).getDeploymentStatus(1L);
    }

    @Test
    @DisplayName("SCALE 디스패치 시 DeploymentService.scaleDeployment 호출")
    void dispatchScale() {
        var parsedIntent = new ParsedIntent(Intent.SCALE, Map.of("deployment_id", 1, "replicas", 3), 0.9, "스케일링");

        String result = actionDispatcher.dispatch(parsedIntent, 1L);

        assertThat(result).contains("3개 레플리카");
        verify(deploymentService).scaleDeployment(eq(1L), any());
    }

    @Test
    @DisplayName("LIST_REPOSITORIES 디스패치 시 RepositoryService 호출")
    void dispatchListRepositories() {
        var repo = new RepositoryResponse(1L, "owner", "repo", "https://github.com/owner/repo",
                CloudVendor.NCP, 1L, LocalDateTime.now(), LocalDateTime.now());
        given(repositoryService.getRepositories(1L)).willReturn(List.of(repo));

        var parsedIntent = new ParsedIntent(Intent.LIST_REPOSITORIES, Map.of(), 0.9, "저장소 목록");
        String result = actionDispatcher.dispatch(parsedIntent, 1L);

        assertThat(result).contains("owner/repo");
    }

    @Test
    @DisplayName("HELP 디스패치 시 도움말 반환")
    void dispatchHelp() {
        var parsedIntent = new ParsedIntent(Intent.HELP, Map.of(), 1.0, "도움말");
        String result = actionDispatcher.dispatch(parsedIntent, 1L);

        assertThat(result).contains("사용 가능한 명령어");
    }
}
