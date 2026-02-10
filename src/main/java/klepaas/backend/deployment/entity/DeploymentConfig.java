package klepaas.backend.deployment.entity;

import jakarta.persistence.*;
import klepaas.backend.global.entity.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "deployment_configs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeploymentConfig extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id")
    private SourceRepository sourceRepository;

    private int minReplicas;

    private int maxReplicas;

    @Column(columnDefinition = "TEXT")
    private String envVars; // json String으로 저장

    @Column(nullable = false)
    private String domainUrl;

    @Builder
    public DeploymentConfig(SourceRepository sourceRepository, int minReplicas, int maxReplicas, String envVars, String domainUrl) {
        this.sourceRepository = sourceRepository;
        this.minReplicas = minReplicas;
        this.maxReplicas = maxReplicas;
        this.envVars = envVars;
        this.domainUrl = domainUrl;
    }

    public void updateConfig(int min, int max, String envVars) {
        this.minReplicas = min;
        this.maxReplicas = max;
        this.envVars = envVars;
    }
}
