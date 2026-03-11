package klepaas.backend.auth.weblogin.entity;

import jakarta.persistence.*;
import klepaas.backend.global.entity.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "cli_auth_sessions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CliAuthSession extends BaseTimeEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, unique = true, length = 16)
    private String userCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CliAuthSessionStatus status;

    @Column(nullable = false, length = 120)
    private String clientName;

    @Column(nullable = false, length = 120)
    private String hostname;

    @Column(nullable = false, length = 80)
    private String platform;

    @Column(nullable = false, length = 40)
    private String cliVersion;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime approvedAt;

    private LocalDateTime rejectedAt;

    private LocalDateTime consumedAt;

    private Long approvedByUserId;

    @Builder
    public CliAuthSession(
            String id,
            String userCode,
            CliAuthSessionStatus status,
            String clientName,
            String hostname,
            String platform,
            String cliVersion,
            LocalDateTime expiresAt
    ) {
        this.id = id;
        this.userCode = userCode;
        this.status = status;
        this.clientName = clientName;
        this.hostname = hostname;
        this.platform = platform;
        this.cliVersion = cliVersion;
        this.expiresAt = expiresAt;
    }

    public void markApproved(Long userId, LocalDateTime approvedAt) {
        this.status = CliAuthSessionStatus.APPROVED;
        this.approvedByUserId = userId;
        this.approvedAt = approvedAt;
    }

    public void markRejected(LocalDateTime rejectedAt) {
        this.status = CliAuthSessionStatus.REJECTED;
        this.rejectedAt = rejectedAt;
    }

    public void markExpired() {
        this.status = CliAuthSessionStatus.EXPIRED;
    }

    public void markConsumed(LocalDateTime consumedAt) {
        this.status = CliAuthSessionStatus.CONSUMED;
        this.consumedAt = consumedAt;
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt.isBefore(now);
    }
}
