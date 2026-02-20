package klepaas.backend.deployment.repository;

import klepaas.backend.deployment.entity.DeploymentConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeploymentConfigRepository extends JpaRepository<DeploymentConfig, Long> {

    Optional<DeploymentConfig> findBySourceRepositoryId(Long sourceRepositoryId);
}
