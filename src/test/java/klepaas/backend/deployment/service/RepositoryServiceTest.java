package klepaas.backend.deployment.service;

import klepaas.backend.deployment.dto.*;
import klepaas.backend.deployment.entity.CloudVendor;
import klepaas.backend.deployment.entity.DeploymentConfig;
import klepaas.backend.deployment.entity.SourceRepository;
import klepaas.backend.deployment.repository.DeploymentConfigRepository;
import klepaas.backend.deployment.repository.SourceRepositoryRepository;
import klepaas.backend.global.exception.DuplicateResourceException;
import klepaas.backend.global.exception.EntityNotFoundException;
import klepaas.backend.user.entity.Role;
import klepaas.backend.user.entity.User;
import klepaas.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RepositoryServiceTest {

    @Mock
    private SourceRepositoryRepository sourceRepositoryRepository;
    @Mock
    private DeploymentConfigRepository deploymentConfigRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RepositoryService repositoryService;

    private User testUser;
    private SourceRepository testRepo;
    private DeploymentConfig testConfig;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@github.com")
                .name("tester")
                .role(Role.USER)
                .providerId("12345")
                .build();

        testRepo = SourceRepository.builder()
                .user(testUser)
                .owner("testowner")
                .repoName("testrepo")
                .gitUrl("https://github.com/testowner/testrepo")
                .cloudVendor(CloudVendor.NCP)
                .build();

        testConfig = DeploymentConfig.builder()
                .sourceRepository(testRepo)
                .minReplicas(1)
                .maxReplicas(1)
                .envVars(new HashMap<>())
                .containerPort(8080)
                .domainUrl("testrepo.klepaas.io")
                .build();
    }

    @Nested
    @DisplayName("createRepository")
    class CreateRepository {

        @Test
        @DisplayName("성공: 레포지토리 + 기본 배포 설정 생성")
        void success() {
            var request = new CreateRepositoryRequest("testowner", "testrepo",
                    "https://github.com/testowner/testrepo", CloudVendor.NCP);
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(sourceRepositoryRepository.findByOwnerAndRepoName("testowner", "testrepo"))
                    .willReturn(Optional.empty());
            given(sourceRepositoryRepository.save(any(SourceRepository.class))).willReturn(testRepo);
            given(deploymentConfigRepository.save(any(DeploymentConfig.class))).willReturn(testConfig);

            RepositoryResponse response = repositoryService.createRepository(1L, request);

            assertThat(response.owner()).isEqualTo("testowner");
            assertThat(response.repoName()).isEqualTo("testrepo");
            assertThat(response.cloudVendor()).isEqualTo(CloudVendor.NCP);
            verify(deploymentConfigRepository).save(any(DeploymentConfig.class));
        }

        @Test
        @DisplayName("실패: 이미 존재하는 레포지토리")
        void failDuplicate() {
            var request = new CreateRepositoryRequest("testowner", "testrepo",
                    "https://github.com/testowner/testrepo", CloudVendor.NCP);
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(sourceRepositoryRepository.findByOwnerAndRepoName("testowner", "testrepo"))
                    .willReturn(Optional.of(testRepo));

            assertThatThrownBy(() -> repositoryService.createRepository(1L, request))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자")
        void failUserNotFound() {
            var request = new CreateRepositoryRequest("testowner", "testrepo",
                    "https://github.com/testowner/testrepo", CloudVendor.NCP);
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> repositoryService.createRepository(999L, request))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getRepositories")
    class GetRepositories {

        @Test
        @DisplayName("성공: 사용자의 레포지토리 목록 조회")
        void success() {
            given(sourceRepositoryRepository.findAllByUserId(1L)).willReturn(List.of(testRepo));

            List<RepositoryResponse> result = repositoryService.getRepositories(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).owner()).isEqualTo("testowner");
        }
    }

    @Nested
    @DisplayName("deleteRepository")
    class DeleteRepository {

        @Test
        @DisplayName("성공: 레포지토리 + 배포 설정 삭제")
        void success() {
            given(sourceRepositoryRepository.findById(1L)).willReturn(Optional.of(testRepo));
            given(deploymentConfigRepository.findBySourceRepositoryId(1L))
                    .willReturn(Optional.of(testConfig));

            repositoryService.deleteRepository(1L);

            verify(deploymentConfigRepository).delete(testConfig);
            verify(sourceRepositoryRepository).delete(testRepo);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 레포지토리")
        void failNotFound() {
            given(sourceRepositoryRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> repositoryService.deleteRepository(999L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateDeploymentConfig")
    class UpdateDeploymentConfig {

        @Test
        @DisplayName("성공: 배포 설정 업데이트")
        void success() {
            var request = new UpdateDeploymentConfigRequest(2, 5, Map.of("ENV", "prod"), 3000, "custom.klepaas.io");
            given(sourceRepositoryRepository.findById(1L)).willReturn(Optional.of(testRepo));
            given(deploymentConfigRepository.findBySourceRepositoryId(1L))
                    .willReturn(Optional.of(testConfig));

            DeploymentConfigResponse response = repositoryService.updateDeploymentConfig(1L, request);

            assertThat(response.minReplicas()).isEqualTo(2);
            assertThat(response.maxReplicas()).isEqualTo(5);
            assertThat(response.containerPort()).isEqualTo(3000);
            assertThat(response.domainUrl()).isEqualTo("custom.klepaas.io");
        }
    }
}
