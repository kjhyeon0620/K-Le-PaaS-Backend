package klepaas.backend.auth.token.controller;

import jakarta.validation.Valid;
import klepaas.backend.auth.config.CustomUserDetails;
import klepaas.backend.auth.token.dto.CliAccessTokenResponse;
import klepaas.backend.auth.token.dto.CreateCliAccessTokenRequest;
import klepaas.backend.auth.token.dto.CreateCliAccessTokenResponse;
import klepaas.backend.auth.token.service.CliAccessTokenService;
import klepaas.backend.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cli-tokens")
@RequiredArgsConstructor
public class CliAccessTokenController {

    private final CliAccessTokenService cliAccessTokenService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreateCliAccessTokenResponse> createToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateCliAccessTokenRequest request
    ) {
        return ApiResponse.success(cliAccessTokenService.createToken(userDetails.getUserId(), request));
    }

    @GetMapping
    public ApiResponse<List<CliAccessTokenResponse>> listTokens(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.success(cliAccessTokenService.listTokens(userDetails.getUserId()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> revokeToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id
    ) {
        cliAccessTokenService.revokeToken(userDetails.getUserId(), id);
        return ApiResponse.success(null, "CLI 토큰이 폐기되었습니다");
    }
}
