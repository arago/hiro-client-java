package co.arago.hiro.client.connection;

import co.arago.hiro.client.exceptions.HiroException;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class TokenAPIHandlerTest {

    public static String API_URL = "https://ec2-3-250-135-44.eu-west-1.compute.amazonaws.com:8443";
    public static String USER = "haas1000-connector-core";
    public static String PASS = "j9dad7gond4ls2taol37ulk56%1aZ";
    public static String CLIENTID = "cju16o7cf0000mz77pbwbhl3q_ckqjkfc0q08r90883i7x521sy";
    public static String CLIENTSECRET = "978fa4385da282ed8190b12e9ac70ed6e65ea750f6b5282c0205a9d049913ae9f2841998c2ef13bb14db2d6cee0fd1ca9834563865b8f45c555e6ad3dd1be36a";
    public static Boolean ACCEPT_ALL_CERTS = true;
    public static PasswordAuthTokenAPIHandler handler;
    final Logger log = LoggerFactory.getLogger(TokenAPIHandlerTest.class);

    @BeforeAll
    static void init() {
        handler = PasswordAuthTokenAPIHandler.newBuilder()
                .setApiUrl(API_URL)
                .setCredentials(USER, PASS, CLIENTID, CLIENTSECRET)
                .setAcceptAllCerts(ACCEPT_ALL_CERTS)
                .setForceLogging(true)
                .build();
    }

    @Test
    void getApiUriOf() throws IOException, InterruptedException, HiroException {
        URI uri = handler.getApiUriOf("graph");
        assert uri != null;
        log.info("URI {}", uri);

        uri = handler.getApiUriOf("auth");
        assert uri != null;
        log.info("URI {}", uri);

        assertThrows(
                HiroException.class,
                () -> handler.getApiUriOf("no_such_api")
        );
    }

    @Test
    void getToken() throws IOException, InterruptedException, HiroException {
        String token = handler.getToken();
        assertFalse(StringUtils.isBlank(token));
        log.info("Token ...{}", token.substring(token.length() - 12));
    }

    @Test
    void refreshToken() throws IOException, InterruptedException, HiroException {
        String token1 = handler.getToken();
        assertFalse(StringUtils.isBlank(token1));
        log.info("Token 1 ...{}", token1.substring(token1.length() - 12));

        handler.refreshToken();

        String token2 = handler.getToken();
        assertFalse(StringUtils.isBlank(token2));
        assertEquals(token1, token2);
        log.info("Token 2 ...{}", token2.substring(token2.length() - 12));

        handler.setRefreshPause(0L);
        handler.refreshToken();

        String token3 = handler.getToken();
        assertFalse(StringUtils.isBlank(token3));
        assertNotEquals(token2, token3);
        log.info("Token 3 ...{}", token3.substring(token3.length() - 12));
    }
}