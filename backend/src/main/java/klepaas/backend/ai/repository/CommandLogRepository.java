package klepaas.backend.ai.repository;

import klepaas.backend.ai.entity.CommandLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommandLogRepository extends JpaRepository<CommandLog, Long> {

    Page<CommandLog> findByUserId(Long userId, Pageable pageable);
}
