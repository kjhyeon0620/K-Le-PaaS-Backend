package klepaas.backend.auth.token.repository;

import klepaas.backend.auth.token.entity.CliAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CliAccessTokenRepository extends JpaRepository<CliAccessToken, Long> {

    Optional<CliAccessToken> findByTokenHash(String tokenHash);

    List<CliAccessToken> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
