package co.arago.hiro.client.connection;

import co.arago.hiro.client.connection.token.PasswordAuthTokenAPIHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.util.JsonTools;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class TokenAPIHandlerTest {

    public static PasswordAuthTokenAPIHandler handler;
    final Logger log = LoggerFactory.getLogger(TokenAPIHandlerTest.class);

    @BeforeAll
    static void init() throws IOException {
        Config config = JsonTools.DEFAULT.toObject(Paths.get("src", "test", "resources", "config.json").toFile(), Config.class);

        handler = PasswordAuthTokenAPIHandler.newBuilder()
                .setApiUrl(config.api_url)
                .setCredentials(config.username, config.password, config.client_id, config.client_secret)
                .setAcceptAllCerts(config.accept_all_certs)
                .setForceLogging(true)
                .build();
    }

    @AfterAll
    static void shutdown() {
        if (handler != null)
            handler.close();
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