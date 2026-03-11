package klepaas.backend.auth.token.service;

import klepaas.backend.auth.token.dto.CreateCliAccessTokenRequest;
import klepaas.backend.auth.token.repository.CliAccessTokenRepository;
import klepaas.backend.global.exception.InvalidRequestException;
import klepaas.backend.user.entity.Role;
import klepaas.backend.user.entity.User;
import klepaas.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CliAccessTokenServiceTest {

    @Mock
    private CliAccessTokenRepository cliAccessTokenRepository;

    @Mock
    private UserRepository userRepository;

    private CliAccessTokenService cliAccessTokenService;

    @BeforeEach
    void setUp() {
        cliAccessTokenService = new CliAccessTokenService(cliAccessTokenRepository, userRepository);
    }

    @Test
    void createTokenReturnsPlaintextTokenOnce() {
        User user = User.builder()
                .email("cli@example.com")
                .name("CLI User")
                .role(Role.USER)
                .providerId("123")
                .build();
        setUserId(user, 1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cliAccessTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = cliAccessTokenService.createToken(1L, new CreateCliAccessTokenRequest("local-cli", 30));

        assertTrue(response.token().startsWith("kpa_cli_"));
        assertEquals("local-cli", response.metadata().name());
    }

    @Test
    void authenticateRejectsUnknownToken() {
        when(cliAccessTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThrows(InvalidRequestException.class, () -> cliAccessTokenService.authenticate("missing"));
    }

    private void setUserId(User user, Long id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
