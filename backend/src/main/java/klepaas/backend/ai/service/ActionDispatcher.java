package klepaas.backend.ai.service;

import klepaas.backend.ai.dto.ParsedIntent;
import klepaas.backend.ai.entity.Intent;
import klepaas.backend.ai.entity.RiskLevel;
import klepaas.backend.deployment.dto.CreateDeploymentRequest;
import klepaas.backend.deployment.dto.ScaleRequest;
import klepaas.backend.deployment.service.DeploymentService;
import klepaas.backend.deployment.service.RepositoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActionDispatcher {

    private final DeploymentService deploymentService;
    private final RepositoryService repositoryService;

    public RiskLevel classifyRisk(Intent intent) {
        return switch (intent) {
            case DEPLOY -> RiskLevel.HIGH;
            case SCALE, RESTART -> RiskLevel.MEDIUM;
            default -> RiskLevel.LOW;
        };
    }

    public String dispatch(ParsedIntent parsedIntent, Long userId) {
        Map<String, Object> args = parsedIntent.args();
        log.info("Action 실행: intent={}, args={}", parsedIntent.intent(), args);

        return switch (parsedIntent.intent()) {
            case DEPLOY -> executeDeploy(args, userId);
            case SCALE -> executeScale(args);
            case RESTART -> executeRestart(args);
            case STATUS -> executeStatus(args);
            case LOGS -> executeLogs(args);
            case LIST_DEPLOYMENTS -> executeListDeployments(args);
            case LIST_REPOSITORIES -> executeListRepositories(userId);
            case OVERVIEW -> executeOverview(userId);
            case HELP -> executeHelp();
            case UNKNOWN -> parsedIntent.message();
        };
    }

    private String executeDeploy(Map<String, Object> args, Long userId) {
        Long repositoryId = toLong(args.get("repository_id"));
        String branchName = (String) args.getOrDefault("branch_name", "main");
        String commitHash = (String) args.getOrDefault("commit_hash", "HEAD");

        var request = new CreateDeploymentRequest(repositoryId, branchName, commitHash);
        var response = deploymentService.createDeployment(request, userId);
        return "배포가 시작되었습니다. 배포 ID: " + response.id() + ", 상태: " + response.status();
    }

    private String executeScale(Map<String, Object> args) {
        Long deploymentId = toLong(args.get("deployment_id"));
        int replicas = ((Number) args.getOrDefault("replicas", 1)).intValue();

        deploymentService.scaleDeployment(deploymentId, new ScaleRequest(replicas));
        return "스케일링 완료: 배포 ID " + deploymentId + " → " + replicas + "개 레플리카";
    }

    private String executeRestart(Map<String, Object> args) {
        Long deploymentId = toLong(args.get("deployment_id"));
        deploymentService.restartDeployment(deploymentId);
        return "재시작 완료: 배포 ID " + deploymentId;
    }

    private String executeStatus(Map<String, Object> args) {
        Long deploymentId = toLong(args.get("deployment_id"));
        var status = deploymentService.getDeploymentStatus(deploymentId);
        return "배포 ID " + deploymentId + " 상태: " + status.status();
    }

    private String executeLogs(Map<String, Object> args) {
        Long deploymentId = toLong(args.get("deployment_id"));
        var logs = deploymentService.getDeploymentLogs(deploymentId);
        return "배포 ID " + deploymentId + " 로그:\n" + String.join("\n", logs.logs());
    }

    private String executeListDeployments(Map<String, Object> args) {
        Long repositoryId = toLong(args.get("repository_id"));
        var deployments = deploymentService.getDeployments(repositoryId,
                org.springframework.data.domain.PageRequest.of(0, 10));
        if (deployments.isEmpty()) {
            return "해당 저장소에 배포 이력이 없습니다.";
        }
        var sb = new StringBuilder("배포 목록:\n");
        deployments.forEach(d -> sb.append("- ID: ").append(d.id())
                .append(", 상태: ").append(d.status())
                .append(", 브랜치: ").append(d.branchName()).append("\n"));
        return sb.toString();
    }

    private String executeListRepositories(Long userId) {
        var repos = repositoryService.getRepositories(userId);
        if (repos.isEmpty()) {
            return "등록된 저장소가 없습니다.";
        }
        var sb = new StringBuilder("저장소 목록:\n");
        repos.forEach(r -> sb.append("- ID: ").append(r.id())
                .append(", 이름: ").append(r.owner()).append("/").append(r.repoName()).append("\n"));
        return sb.toString();
    }

    private String executeOverview(Long userId) {
        var repos = repositoryService.getRepositories(userId);
        return "프로젝트 개요:\n- 등록된 저장소 수: " + repos.size();
    }

    private String executeHelp() {
        return """
                사용 가능한 명령어:
                - "내 프로젝트 배포해줘" → 배포 실행
                - "레플리카 3개로 늘려줘" → 스케일링
                - "서비스 재시작해줘" → 재시작
                - "배포 상태 알려줘" → 상태 조회
                - "로그 보여줘" → 로그 조회
                - "배포 목록" → 배포 이력 조회
                - "저장소 목록" → 저장소 목록 조회
                - "전체 현황" → 프로젝트 개요
                """;
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String str) {
            return Long.parseLong(str);
        }
        throw new IllegalArgumentException("Long으로 변환할 수 없는 값: " + value);
    }
}
