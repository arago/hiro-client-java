package co.arago.hiro.client.connection;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;

class TokenAPIHandlerTest {

    final Logger log = LoggerFactory.getLogger(TokenAPIHandlerTest.class);


    public static String API_URL = "https://core.arago.co";
    public static String USER = "";
    public static String PASS = "";
    public static String CLIENTID = "";
    public static String CLIENTSECRET = "";
    public static Boolean ACCEPT_ALL_CERTS = true;

    public static PasswordAuthTokenAPIHandler handler;

    @BeforeAll
    static void init() throws NoSuchAlgorithmException, KeyManagementException, IOException, InterruptedException {
        handler = PasswordAuthTokenAPIHandler.newBuilder()
                .setApiUrl(API_URL)
                .setAcceptAllCerts(true)
                .setUsername(USER)
                .setPassword(PASS)
                .setClientId(CLIENTID)
                .setClientSecret(CLIENTSECRET)
                .build();
    }

    @Test
    void getApiUriOf() throws IOException, InterruptedException {
        URI uri = handler.getApiUriOf("graph");
        assert uri != null;
        log.info("URI {}", uri);

        uri = handler.getApiUriOf("auth");
        assert uri != null;
        log.info("URI {}", uri);
    }

    @Test
    void getToken() throws IOException, InterruptedException {
        String token = handler.getToken();
        assertFalse(StringUtils.isBlank(token));
        log.info("Token {}...", token.substring(0, 12));
    }

    @Test
    void refreshToken() throws IOException, InterruptedException {
        String token1 = handler.getToken();
        assertFalse(StringUtils.isBlank(token1));
        log.info("Token 1 ...{}", token1.substring(token1.length() - 12));

        handler.refreshToken();

        String token2 = handler.getToken();
        assertFalse(StringUtils.isBlank(token2));
        assertEquals(token1, token2);
        log.info("Token 2 ...{}", token2.substring(token2.length() - 12));

        handler.setTokenRefreshOptions(0L, 0L);
        handler.refreshToken();

        String token3 = handler.getToken();
        assertFalse(StringUtils.isBlank(token3));
        assertNotEquals(token2, token3);
        log.info("Token 3 ...{}", token3.substring(token3.length() - 12));
    }
}