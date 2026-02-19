package klepaas.backend.auth.oauth;

import io.jsonwebtoken.Jwts;
import klepaas.backend.auth.config.GitHubAppConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class GitHubAppJwtProvider {

    private final GitHubAppConfig config;

    private volatile RSAPrivateKey cachedKey;

    public String generateAppJwt() {
        return Jwts.builder()
                .issuer(String.valueOf(config.getAppId()))
                .issuedAt(new Date(System.currentTimeMillis() - 60_000))
                .expiration(new Date(System.currentTimeMillis() + 9 * 60_000))
                .signWith(getPrivateKey(), Jwts.SIG.RS256)
                .compact();
    }

    private RSAPrivateKey getPrivateKey() {
        if (cachedKey == null) {
            synchronized (this) {
                if (cachedKey == null) {
                    cachedKey = parsePrivateKey(config.getPrivateKey());
                }
            }
        }
        return cachedKey;
    }

    private RSAPrivateKey parsePrivateKey(String pem) {
        String cleanPem = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(cleanPem);
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
            return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            throw new IllegalStateException("GitHub App private key 파싱 실패", e);
        }
    }
}
