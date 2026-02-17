package klepaas.backend.deployment.service;

import klepaas.backend.deployment.dto.*;
import klepaas.backend.deployment.entity.Deployment;
import klepaas.backend.deployment.entity.SourceRepository;
import klepaas.backend.deployment.repository.DeploymentRepository;
import klepaas.backend.deployment.repository.SourceRepositoryRepository;
import klepaas.backend.global.exception.EntityNotFoundException;
import klepaas.backend.global.exception.ErrorCode;
import klepaas.backend.global.exception.InvalidRequestException;
import klepaas.backend.infra.CloudInfraProviderFactory;
import klepaas.backend.infra.kubernetes.KubernetesManifestGenerator;
import klepaas.backend.user.entity.User;
import klepaas.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeploymentService {

    private final DeploymentRepository deploymentRepository;
    private final SourceRepositoryRepository sourceRepositoryRepository;
    private final DeploymentPipelineService pipelineService;
    private final CloudInfraProviderFactory infraProviderFactory;
    private final KubernetesManifestGenerator k8sGenerator;
    private final UserRepository userRepository;

    @Transactional
    public DeploymentResponse createDeployment(CreateDeploymentRequest request) {
        return createDeployment(request, null);
    }

    @Transactional
    public DeploymentResponse createDeployment(CreateDeploymentRequest request, Long userId) {
        SourceRepository repository = sourceRepositoryRepository.findById(request.repositoryId())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.REPOSITORY_NOT_FOUND));

        // gitToken 결정: 요청에 있으면 사용, 없으면 DB에서 저장된 토큰 사용
        String gitToken = resolveGitToken(request.gitToken(), userId);

        Deployment deployment = Deployment.builder()
                .sourceRepository(repository)
                .branchName(request.branchName())
                .commitHash(request.commitHash())
                .build();
        deploymentRepository.save(deployment);

        log.info("Deployment created: id={}, repo={}/{}, branch={}", deployment.getId(),
                repository.getOwner(), repository.getRepoName(), request.branchName());

        // 비동기 파이프라인 실행 (즉시 응답, 백그라운드 빌드/배포)
        pipelineService.executePipeline(deployment.getId(), gitToken);

        return DeploymentResponse.from(deployment);
    }

    private String resolveGitToken(String requestToken, Long userId) {
        if (requestToken != null && !requestToken.isBlank()) {
            return requestToken;
        }
        if (userId != null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));
            if (user.getGithubAccessToken() != null) {
                return user.getGithubAccessToken();
            }
        }
        throw new InvalidRequestException(ErrorCode.INVALID_REQUEST,
                "Git 토큰이 필요합니다. 다시 로그인하거나 토큰을 직접 제공해주세요.");
    }

    public Page<DeploymentResponse> getDeployments(Long repositoryId, Pageable pageable) {
        return deploymentRepository.findBySourceRepositoryId(repositoryId, pageable)
                .map(DeploymentResponse::from);
    }

    public DeploymentResponse getDeployment(Long deploymentId) {
        Deployment deployment = deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.DEPLOYMENT_NOT_FOUND));
        return DeploymentResponse.from(deployment);
    }

    public DeploymentStatusResponse getDeploymentStatus(Long deploymentId) {
        Deployment deployment = deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.DEPLOYMENT_NOT_FOUND));
        return DeploymentStatusResponse.from(deployment);
    }

    public DeploymentLogResponse getDeploymentLogs(Long deploymentId) {
        deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.DEPLOYMENT_NOT_FOUND));

        // TODO: Phase 5+ - NCP에서 실제 빌드/배포 로그 조회
        return new DeploymentLogResponse(deploymentId, List.of("로그 조회 기능은 향후 구현 예정입니다."));
    }

    @Transactional
    public void scaleDeployment(Long deploymentId, ScaleRequest request) {
        Deployment deployment = deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.DEPLOYMENT_NOT_FOUND));

        SourceRepository repo = deployment.getSourceRepository();
        String appName = repo.getOwner() + "-" + repo.getRepoName();
        k8sGenerator.scale(appName, request.replicas());

        log.info("Scale completed: deploymentId={}, replicas={}", deploymentId, request.replicas());
    }

    @Transactional
    public void restartDeployment(Long deploymentId) {
        Deployment deployment = deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.DEPLOYMENT_NOT_FOUND));

        // 재배포: 동일 설정으로 새 배포 파이프라인 실행은 gitToken이 필요하므로
        // 현재는 K8s rollout restart로 처리
        SourceRepository repo = deployment.getSourceRepository();
        String appName = repo.getOwner() + "-" + repo.getRepoName();
        k8sGenerator.scale(appName, 0);
        k8sGenerator.scale(appName, 1);

        log.info("Restart completed: deploymentId={}", deploymentId);
    }
}
