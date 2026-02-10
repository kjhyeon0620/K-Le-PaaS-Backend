package klepaas.backend.deployment.controller;

import jakarta.validation.Valid;
import klepaas.backend.deployment.dto.*;
import klepaas.backend.deployment.service.RepositoryService;
import klepaas.backend.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/repositories")
@RequiredArgsConstructor
public class RepositoryController {

    private final RepositoryService repositoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RepositoryResponse> createRepository(@Valid @RequestBody CreateRepositoryRequest request) {
        // TODO: Phase 3 - SecurityContext에서 userId 추출
        Long userId = 1L;
        return ApiResponse.success(repositoryService.createRepository(userId, request));
    }

    @GetMapping
    public ApiResponse<List<RepositoryResponse>> getRepositories() {
        // TODO: Phase 3 - SecurityContext에서 userId 추출
        Long userId = 1L;
        return ApiResponse.success(repositoryService.getRepositories(userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<RepositoryResponse> getRepository(@PathVariable Long id) {
        return ApiResponse.success(repositoryService.getRepository(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRepository(@PathVariable Long id) {
        repositoryService.deleteRepository(id);
        return ApiResponse.success(null, "저장소가 삭제되었습니다");
    }

    @GetMapping("/{id}/config")
    public ApiResponse<DeploymentConfigResponse> getDeploymentConfig(@PathVariable Long id) {
        return ApiResponse.success(repositoryService.getDeploymentConfig(id));
    }

    @PutMapping("/{id}/config")
    public ApiResponse<DeploymentConfigResponse> updateDeploymentConfig(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDeploymentConfigRequest request) {
        return ApiResponse.success(repositoryService.updateDeploymentConfig(id, request));
    }
}
