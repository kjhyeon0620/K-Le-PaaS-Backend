package klepaas.backend.auth.weblogin.controller;

import jakarta.validation.Valid;
import klepaas.backend.auth.config.CustomUserDetails;
import klepaas.backend.auth.weblogin.dto.CliAuthSessionResponse;
import klepaas.backend.auth.weblogin.dto.CreateCliAuthSessionRequest;
import klepaas.backend.auth.weblogin.dto.ExchangeCliAuthSessionRequest;
import klepaas.backend.auth.weblogin.dto.ExchangeCliAuthSessionResponse;
import klepaas.backend.auth.weblogin.service.CliAuthSessionService;
import klepaas.backend.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cli-auth/sessions")
@RequiredArgsConstructor
public class CliAuthSessionController {

    private final CliAuthSessionService cliAuthSessionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CliAuthSessionResponse> create(@Valid @RequestBody CreateCliAuthSessionRequest request) {
        return ApiResponse.success(cliAuthSessionService.createSession(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<CliAuthSessionResponse> get(@PathVariable String id) {
        return ApiResponse.success(cliAuthSessionService.getSession(id));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<Void> approve(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        cliAuthSessionService.approve(id, userDetails.getUserId());
        return ApiResponse.success(null, "CLI 로그인 요청이 승인되었습니다");
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<Void> reject(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        cliAuthSessionService.reject(id, userDetails.getUserId());
        return ApiResponse.success(null, "CLI 로그인 요청이 거부되었습니다");
    }

    @PostMapping("/{id}/exchange")
    public ApiResponse<ExchangeCliAuthSessionResponse> exchange(
            @PathVariable String id,
            @Valid @RequestBody ExchangeCliAuthSessionRequest request
    ) {
        return ApiResponse.success(cliAuthSessionService.exchange(id, request));
    }
}
