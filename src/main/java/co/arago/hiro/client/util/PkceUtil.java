package co.arago.hiro.client.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PkceUtil {

    private String codeVerifier;

    public PkceUtil() {
    }

    public String getCodeVerifier() {
        if (codeVerifier == null)
            initialize();

        return codeVerifier;
    }

    public String getCodeChallengeMethod() {
        return "S256";
    }

    public String getCodeChallenge() throws NoSuchAlgorithmException {
        if (codeVerifier == null)
            initialize();

        byte[] bytes = codeVerifier.getBytes(StandardCharsets.US_ASCII);
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] digest = messageDigest.digest(bytes);

        return urlSafeBase64(digest);
    }

    public void initialize() {
        codeVerifier = generateRandomBase64(64);
    }

    public static String generateRandomBase64(int size) {
        byte[] randomBytes = new byte[size];

        new SecureRandom().nextBytes(randomBytes);

        return urlSafeBase64(randomBytes);
    }

    private static String urlSafeBase64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
