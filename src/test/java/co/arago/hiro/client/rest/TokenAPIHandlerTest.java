package co.arago.hiro.client.rest;

import co.arago.hiro.client.ConfigModel;
import co.arago.hiro.client.connection.httpclient.DefaultHttpClientHandler;
import co.arago.hiro.client.connection.httpclient.HttpClientHandler;
import co.arago.hiro.client.connection.token.PasswordAuthTokenAPIHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.util.json.JsonUtil;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Disabled
class TokenAPIHandlerTest {

    public static PasswordAuthTokenAPIHandler handler;
    final static Logger log = LoggerFactory.getLogger(TokenAPIHandlerTest.class);

    @BeforeAll
    static void init() throws IOException {
        try {
            ConfigModel config = JsonUtil.DEFAULT.toObject(
                    TokenAPIHandlerTest.class.getClassLoader().getResourceAsStream("config.json"),
                    ConfigModel.class);

            HttpClientHandler httpClientHandler = DefaultHttpClientHandler.newBuilder()
                    .setAcceptAllCerts(config.accept_all_certs)
                    .setShutdownTimeout(0)
                    .build();

            handler = PasswordAuthTokenAPIHandler.newBuilder()
                    .setHttpClientHandler(httpClientHandler)
                    .setRootApiURI(config.api_url)
                    .setCredentials(config.username, config.password, config.client_id, config.client_secret)
                    .setForceLogging(config.force_logging)
                    .build();
        } catch (URISyntaxException e) {
            log.warn("Skipping tests: {}.", e.getMessage());
        }
    }

    @AfterAll
    static void shutdown() {
        if (handler != null)
            handler.close();
    }

    @Test
    void getToken() throws IOException, InterruptedException, HiroException {
        if (handler == null)
            return;

        String token = handler.getToken();
        assertFalse(StringUtils.isBlank(token));
        log.info("Token ...{}", token.substring(token.length() - 12));
    }

    @Test
    void refreshToken() throws IOException, InterruptedException, HiroException {
        if (handler == null)
            return;

        String token1 = handler.getToken();
        log.info("Token 1 ...{}", token1.substring(token1.length() - 12));
        assertFalse(StringUtils.isBlank(token1));

        String token2;

        if (handler.hasRefreshToken()) {
            handler.refreshToken();

            token2 = handler.getToken();
            log.info("Token 2 ...{}", token2.substring(token2.length() - 12));
            assertFalse(StringUtils.isBlank(token2));

        } else {
            log.warn("No refreshToken available");
            token2 = token1;
        }

        handler.refreshToken();
        String token3 = handler.getToken();
        log.info("Token 3 ...{}", token3.substring(token3.length() - 12));
        assertFalse(StringUtils.isBlank(token3));
        assertNotEquals(token2, token3);
    }

    @Test
    void revokeToken() throws HiroException, IOException, InterruptedException {
        if (handler == null)
            return;

        String token1 = handler.getToken();
        assertFalse(StringUtils.isBlank(token1));

        if (!handler.hasRefreshToken()) {
            log.warn("No refreshToken available");
            return;
        }

        handler.revokeToken();
        assertFalse(handler.hasToken());
    }
}