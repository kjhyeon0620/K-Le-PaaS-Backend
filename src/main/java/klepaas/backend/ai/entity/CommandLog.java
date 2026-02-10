package klepaas.backend.ai.entity;

import jakarta.persistence.*;
import klepaas.backend.global.entity.BaseTimeEntity;
import klepaas.backend.user.entity.User;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "command_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommandLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String rawCommand; // "nginx 3개로 늘려줘"

    private String interpretedIntent; // "SCALE"

    private boolean isExecuted; // 실제 실행 여부

    private String errorMessage;

    @Builder
    public CommandLog(User user, String rawCommand, String interpretedIntent, boolean isExecuted, String errorMessage) {
        this.user = user;
        this.rawCommand = rawCommand;
        this.interpretedIntent = interpretedIntent;
        this.isExecuted = isExecuted;
        this.errorMessage = errorMessage;
    }
}
