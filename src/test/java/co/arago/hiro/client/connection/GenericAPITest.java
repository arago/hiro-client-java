package co.arago.hiro.client.connection;

import co.arago.hiro.client.ConfigModel;
import co.arago.hiro.client.connection.token.AbstractTokenAPIHandler;
import co.arago.hiro.client.connection.token.PasswordAuthTokenAPIHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.exceptions.HiroHttpException;
import co.arago.hiro.client.mock.MockGraphitServerExtension;
import co.arago.hiro.client.model.HiroMessage;
import co.arago.hiro.client.model.VersionResponse;
import co.arago.hiro.client.rest.AuthenticatedAPIHandler;
import co.arago.hiro.client.rest.GraphAPI;
import co.arago.util.json.JsonUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpTimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockGraphitServerExtension.class)
public class GenericAPITest {
    public static AbstractTokenAPIHandler handler;
    public static AuthenticatedAPIHandler apiHandler;
    final static Logger log = LoggerFactory.getLogger(GenericAPITest.class);

    @BeforeAll
    static void init() throws IOException {
        try {
            ConfigModel config = JsonUtil.DEFAULT.toObject(
                    GenericAPITest.class.getClassLoader().getResourceAsStream("config.json"),
                    ConfigModel.class);

            handler = PasswordAuthTokenAPIHandler.newBuilder()
                    .setRootApiURI(config.api_url)
                    .setCredentials(config.username, config.password, config.client_id, config.client_secret)
                    .setAcceptAllCerts(config.accept_all_certs)
                    .setForceLogging(config.force_logging)
                    .setShutdownTimeout(0)
                    .build();

            apiHandler = GraphAPI.newBuilder(handler).build();
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
    void getVersion() throws IOException, InterruptedException, HiroException {
        if (handler == null)
            return;

        VersionResponse versionResponse = handler.getVersionMap();
        log.info(versionResponse.toJsonString());
        assertNotNull(versionResponse);
    }

    @Test
    void getApiURIOf() throws IOException, InterruptedException, HiroException {
        if (handler == null)
            return;

        URI uri = handler.getApiURIOf("graph");
        log.info("URI {}", uri);
        assertNotNull(uri);

        uri = handler.getApiURIOf("auth");
        log.info("URI {}", uri);
        assertNotNull(uri);

        assertThrows(
                HiroException.class,
                () -> handler.getApiURIOf("no_such_api"));
    }

    @Test
    void test400() throws HiroException, IOException, InterruptedException {
        if (handler == null)
            return;

        URI uri = handler.getApiURIOf("graph");

        HiroHttpException hiroHttpException = assertThrows(
                HiroHttpException.class,
                () -> apiHandler.get(HiroMessage.class, uri, null, null, null));

        log.info(hiroHttpException.toString());

        assertEquals(hiroHttpException.getCode(), 400);
    }

    @Test
    void testTimeout() {
        if (handler == null)
            return;

        URI uri = handler.getRootApiURI().resolve("timeout");

        HttpTimeoutException httpTimeoutException = assertThrows(
                HttpTimeoutException.class,
                () -> apiHandler.get(HiroMessage.class, uri, null, 10L, null));

        log.info(httpTimeoutException.toString());
    }

    @Test
    void testRetries() throws HiroException, IOException, InterruptedException {
        if (handler == null)
            return;

        URI uri = handler.getApiURIOf("graph");

        HiroHttpException hiroHttpException = assertThrows(
                HiroHttpException.class,
                () -> apiHandler.get(HiroMessage.class, uri, null, null, 2));

        log.info(hiroHttpException.toString());

        assertEquals(hiroHttpException.getCode(), 400);
    }
}
