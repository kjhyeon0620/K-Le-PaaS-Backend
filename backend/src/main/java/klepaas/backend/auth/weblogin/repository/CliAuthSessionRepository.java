package klepaas.backend.auth.weblogin.repository;

import jakarta.persistence.LockModeType;
import klepaas.backend.auth.weblogin.entity.CliAuthSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CliAuthSessionRepository extends JpaRepository<CliAuthSession, String> {

    Optional<CliAuthSession> findByUserCode(String userCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from CliAuthSession s where s.id = :id")
    Optional<CliAuthSession> findForUpdateById(@Param("id") String id);
}
