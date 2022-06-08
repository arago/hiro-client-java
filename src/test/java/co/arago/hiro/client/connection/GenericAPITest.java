package co.arago.hiro.client.connection;

import co.arago.hiro.client.Config;
import co.arago.hiro.client.connection.token.PasswordAuthTokenAPIHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.exceptions.HiroHttpException;
import co.arago.hiro.client.model.HiroMessage;
import co.arago.hiro.client.model.VersionResponse;
import co.arago.hiro.client.rest.AbstractAuthenticatedAPIHandler;
import co.arago.hiro.client.rest.GraphAPI;
import co.arago.util.json.JsonUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GenericAPITest {
    public static PasswordAuthTokenAPIHandler handler;
    public static AbstractAuthenticatedAPIHandler apiHandler;
    final static Logger log = LoggerFactory.getLogger(GenericAPITest.class);

    @BeforeAll
    static void init() throws IOException {
        try {
            Config config = JsonUtil.DEFAULT.toObject(Paths.get("src", "test", "resources", "config.json").toFile(),
                    Config.class);

            handler = PasswordAuthTokenAPIHandler.newBuilder()
                    .setApiUrl(config.api_url)
                    .setCredentials(config.username, config.password, config.client_id, config.client_secret)
                    .setAcceptAllCerts(config.accept_all_certs)
                    .setForceLogging(config.force_logging)
                    .build();

            apiHandler = GraphAPI.newBuilder(handler).build();
        } catch (FileNotFoundException e) {
            log.warn("Skipping tests: {}.", e.getMessage());
        }
    }

    @AfterAll
    static void shutdown() {
        if (handler != null)
            handler.close();
    }

    @Test
    void getVersion() throws IOException, InterruptedException, HiroException {
        if (handler == null)
            return;

        VersionResponse versionResponse = handler.getVersionMap();
        log.info(versionResponse.toJsonString());
    }

    @Test
    void getApiUriOf() throws IOException, InterruptedException, HiroException {
        if (handler == null)
            return;

        URI uri = handler.getApiUriOf("graph");
        log.info("URI {}", uri);
        assertNotNull(uri);

        uri = handler.getApiUriOf("auth");
        log.info("URI {}", uri);
        assertNotNull(uri);

        assertThrows(
                HiroException.class,
                () -> handler.getApiUriOf("no_such_api"));
    }

    @Test
    void test400() throws InterruptedException, IOException, HiroException {
        if (handler == null)
            return;

        URI uri = handler.getApiUriOf("graph");

        HiroHttpException hiroHttpException = assertThrows(
                HiroHttpException.class,
                () -> apiHandler.get(HiroMessage.class, uri, null, null, null));

        log.info(hiroHttpException.toString());

        assertEquals(hiroHttpException.getCode(), 400);
    }

    @Test
    void test404() throws InterruptedException, IOException, HiroException {
        if (handler == null)
            return;

        URI uri = handler.getApiUriOf("graph");

        HiroHttpException hiroHttpException = assertThrows(
                HiroHttpException.class,
                () -> apiHandler.get(HiroMessage.class, uri.resolve("wrongPath"), null, null, null));

        log.info(hiroHttpException.toString());

        assertEquals(hiroHttpException.getCode(), 404);
    }

    @Test
    void testTimeout() throws InterruptedException, IOException, HiroException {
        if (handler == null)
            return;

        URI uri = handler.getApiUriOf("graph");

        HttpTimeoutException httpTimeoutException = assertThrows(
                HttpTimeoutException.class,
                () -> apiHandler.get(HiroMessage.class, uri, null, 10L, null));

        log.info(httpTimeoutException.toString());
    }

    @Test
    void testRetries() throws InterruptedException, IOException, HiroException {
        if (handler == null)
            return;

        URI uri = handler.getApiUriOf("graph");

        HiroHttpException hiroHttpException = assertThrows(
                HiroHttpException.class,
                () -> apiHandler.get(HiroMessage.class, uri, null, null, 2));

        log.info(hiroHttpException.toString());

        assertEquals(hiroHttpException.getCode(), 400);
    }
}
