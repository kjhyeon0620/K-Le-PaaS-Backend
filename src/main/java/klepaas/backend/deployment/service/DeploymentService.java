package klepaas.backend.deployment.service;

import klepaas.backend.deployment.dto.*;
import klepaas.backend.deployment.entity.Deployment;
import klepaas.backend.deployment.entity.SourceRepository;
import klepaas.backend.deployment.repository.DeploymentRepository;
import klepaas.backend.deployment.repository.SourceRepositoryRepository;
import klepaas.backend.global.exception.EntityNotFoundException;
import klepaas.backend.global.exception.ErrorCode;
import klepaas.backend.infra.CloudInfraProviderFactory;
import klepaas.backend.infra.kubernetes.KubernetesManifestGenerator;
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

    @Transactional
    public DeploymentResponse createDeployment(CreateDeploymentRequest request) {
        return createDeployment(request, null);
    }

    @Transactional
    public DeploymentResponse createDeployment(CreateDeploymentRequest request, Long userId) {
        SourceRepository repository = sourceRepositoryRepository.findById(request.repositoryId())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.REPOSITORY_NOT_FOUND));

        Deployment deployment = Deployment.builder()
                .sourceRepository(repository)
                .branchName(request.branchName())
                .commitHash(request.commitHash())
                .build();
        deploymentRepository.save(deployment);

        log.info("Deployment created: id={}, repo={}/{}, branch={}", deployment.getId(),
                repository.getOwner(), repository.getRepoName(), request.branchName());

        pipelineService.executePipeline(deployment.getId());

        return DeploymentResponse.from(deployment);
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

        SourceRepository repo = deployment.getSourceRepository();
        String appName = repo.getOwner() + "-" + repo.getRepoName();
        k8sGenerator.scale(appName, 0);
        k8sGenerator.scale(appName, 1);

        log.info("Restart completed: deploymentId={}", deploymentId);
    }
}
