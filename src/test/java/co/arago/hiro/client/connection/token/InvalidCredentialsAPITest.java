package co.arago.hiro.client.connection.token;

import co.arago.hiro.client.Config;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.exceptions.TokenUnauthorizedException;
import co.arago.hiro.client.mock.MockGraphitServer;
import co.arago.hiro.client.mock.handler.BadRequestHandler;
import co.arago.hiro.client.rest.AuthAPI;
import co.arago.util.json.JsonUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InvalidCredentialsAPITest {
    public static Config config;

    final static Logger log = LoggerFactory.getLogger(InvalidCredentialsAPITest.class);

    static MockGraphitServer mockingServer;

    @BeforeAll
    static void init() throws IOException {
        try {
            config = JsonUtil.DEFAULT.toObject(
                    InvalidCredentialsAPITest.class.getClassLoader().getResourceAsStream("config.json"),
                    Config.class);

            mockingServer = new MockGraphitServer();

            mockingServer.addContext("auth", "me", new BadRequestHandler());
            mockingServer.start();

        } catch (HiroException e) {
            log.warn("Skipping tests: {}.", e.getMessage());
        }
    }

    @AfterAll
    static void shutdown() {
        if (mockingServer != null)
            mockingServer.close();
    }

    @Test
    void wrongCredentials() {
        if (config == null)
            return;

        try (AbstractTokenAPIHandler handler = PasswordAuthTokenAPIHandler.newBuilder()
                .setRootApiURI(config.api_url)
                .setCredentials(config.username, config.password, config.client_id, config.client_secret)
                .setAcceptAllCerts(config.accept_all_certs)
                .setForceLogging(config.force_logging)
                .setShutdownTimeout(0)
                .setPassword("Wrong")
                .build()) {

            AuthAPI apiHandler = AuthAPI.newBuilder(handler).build();

            TokenUnauthorizedException tokenUnauthorizedException = assertThrows(
                    TokenUnauthorizedException.class,
                    () -> apiHandler.getMeAccountCommand().execute());

            log.info(tokenUnauthorizedException.toString());

            assertEquals(tokenUnauthorizedException.getCode(), 401);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void wrongClient() {
        if (config == null)
            return;

        try (AbstractTokenAPIHandler handler = PasswordAuthTokenAPIHandler.newBuilder()
                .setRootApiURI(config.api_url)
                .setCredentials(config.username, config.password, config.client_id, config.client_secret)
                .setAcceptAllCerts(config.accept_all_certs)
                .setForceLogging(config.force_logging)
                .setShutdownTimeout(0)
                .setClientSecret("Wrong")
                .build()) {

            AuthAPI apiHandler = AuthAPI.newBuilder(handler).build();

            TokenUnauthorizedException tokenUnauthorizedException = assertThrows(
                    TokenUnauthorizedException.class,
                    () -> apiHandler.getMeAccountCommand().execute());

            log.info(tokenUnauthorizedException.toString());

            assertEquals(tokenUnauthorizedException.getCode(), 401);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Disabled
    void wrongUrl() throws MalformedURLException {
        if (config == null)
            return;

        try (AbstractTokenAPIHandler handler = PasswordAuthTokenAPIHandler.newBuilder()
                .setCredentials(config.username, config.password, config.client_id, config.client_secret)
                .setAcceptAllCerts(config.accept_all_certs)
                .setForceLogging(config.force_logging)
                .setShutdownTimeout(0)
                .setRootApiURI("http://nothing.here")
                .build()) {

            AuthAPI apiHandler = AuthAPI.newBuilder(handler).build();

            ConnectException exception = assertThrows(
                    ConnectException.class,
                    () -> apiHandler.getMeAccountCommand().execute());

            log.info(exception.toString());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

    }
}
