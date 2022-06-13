package co.arago.hiro.client.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PkceUtil {

    private String codeVerifier;

    public PkceUtil() {
        renew();
    }

    public String getCodeVerifier() {
        return codeVerifier;
    }

    public String getCodeChallengeMethod() {
        return "S256";
    }

    public String getCodeChallenge() throws NoSuchAlgorithmException {
        byte[] bytes = codeVerifier.getBytes(StandardCharsets.US_ASCII);
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] digest = messageDigest.digest(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    public void renew() {
        codeVerifier = generateRandomBase64(64);
    }

    public static String generateRandomBase64(int size) {
        byte[] codeVerifier = new byte[size];

        new SecureRandom().nextBytes(codeVerifier);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier);
    }
}
