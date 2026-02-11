package klepaas.backend.infra.ncp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class NcpApiSigner {

    @Value("${cloud.ncp.credentials.access-key}")
    private String accessKey;

    @Value("${cloud.ncp.credentials.secret-key}")
    private String secretKey;

    /**
     * NCP API HMAC-SHA256 서명 생성
     * StringToSign = METHOD + " " + URI + "\n" + timestamp + "\n" + accessKey
     */
    public String makeSignature(String method, String uri, String timestamp) {
        try {
            String message = method + " " + uri + "\n" + timestamp + "\n" + accessKey;

            SecretKeySpec signingKey = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);

            byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("NCP API 서명 생성 실패", e);
        }
    }

    public String getAccessKey() {
        return accessKey;
    }
}
