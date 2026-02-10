package klepaas.backend.deployment.service;

import klepaas.backend.deployment.dto.*;
import klepaas.backend.deployment.entity.Deployment;
import klepaas.backend.deployment.entity.SourceRepository;
import klepaas.backend.deployment.repository.DeploymentRepository;
import klepaas.backend.deployment.repository.SourceRepositoryRepository;
import klepaas.backend.global.exception.EntityNotFoundException;
import klepaas.backend.global.exception.ErrorCode;
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

    @Transactional
    public DeploymentResponse createDeployment(CreateDeploymentRequest request) {
        SourceRepository repository = sourceRepositoryRepository.findById(request.repositoryId())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.REPOSITORY_NOT_FOUND));

        Deployment deployment = Deployment.builder()
                .sourceRepository(repository)
                .branchName(request.branchName())
                .commitHash(request.commitHash())
                .build();
        deploymentRepository.save(deployment);

        // TODO: Phase 4 - CloudInfraProvider를 통한 실제 빌드/배포 트리거
        log.info("Deployment created: id={}, repo={}/{}, branch={}", deployment.getId(),
                repository.getOwner(), repository.getRepoName(), request.branchName());

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

        // TODO: Phase 4 - NCP에서 실제 빌드/배포 로그 조회
        return new DeploymentLogResponse(deploymentId, List.of("로그 조회는 Phase 4에서 구현 예정입니다."));
    }

    @Transactional
    public void scaleDeployment(Long deploymentId, ScaleRequest request) {
        deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.DEPLOYMENT_NOT_FOUND));

        // TODO: Phase 4 - CloudInfraProvider.scaleService() 호출
        log.info("Scale requested: deploymentId={}, replicas={}", deploymentId, request.replicas());
    }

    @Transactional
    public void restartDeployment(Long deploymentId) {
        deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.DEPLOYMENT_NOT_FOUND));

        // TODO: Phase 4 - 재배포 로직
        log.info("Restart requested: deploymentId={}", deploymentId);
    }
}
