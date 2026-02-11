package klepaas.backend.deployment.repository;

import klepaas.backend.deployment.entity.Deployment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeploymentRepository extends JpaRepository<Deployment, Long> {

    // 배포 이력은 양이 많으므로 페이징 처리
    // SourceRepository 정보는 반드시 필요하므로 fetch join 걸기
    @EntityGraph(attributePaths = {"sourceRepository"})
    Page<Deployment> findBySourceRepositoryId(Long sourceRepositoryId, Pageable pageable);

    @EntityGraph(attributePaths = {"sourceRepository"})
    Page<Deployment> findBySourceRepositoryUserId(Long userId, Pageable pageable);
}
