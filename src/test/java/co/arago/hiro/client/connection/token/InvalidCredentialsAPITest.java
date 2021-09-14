package co.arago.hiro.client.connection.token;

import co.arago.hiro.client.Config;
import co.arago.hiro.client.exceptions.TokenUnauthorizedException;
import co.arago.hiro.client.rest.AuthAPI;
import co.arago.util.json.JsonTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InvalidCredentialsAPITest {
    public PasswordAuthTokenAPIHandler.Builder handlerBuilder;

    final static Logger log = LoggerFactory.getLogger(InvalidCredentialsAPITest.class);

    @BeforeEach
    void init() throws IOException {
        try {
            Config config = JsonTools.DEFAULT.toObject(Paths.get("src", "test", "resources", "config.json").toFile(), Config.class);

            handlerBuilder = PasswordAuthTokenAPIHandler.newBuilder()
                    .setApiUrl(config.api_url)
                    .setCredentials(config.username, config.password, config.client_id, config.client_secret)
                    .setAcceptAllCerts(config.accept_all_certs)
                    .setForceLogging(config.force_logging);
        } catch (FileNotFoundException e) {
            log.warn("Skipping tests: {}.", e.getMessage());
        }
    }

    @Test
    void wrongCredentials() {
        if (handlerBuilder == null)
            return;

        try (AbstractTokenAPIHandler handler = handlerBuilder.setPassword("Wrong").build()) {

            AuthAPI apiHandler = AuthAPI.newBuilder(handler).build();

            TokenUnauthorizedException tokenUnauthorizedException = assertThrows(
                    TokenUnauthorizedException.class,
                    () -> apiHandler.getMeAccount().execute()
            );

            log.info(tokenUnauthorizedException.toString());

            assertEquals(tokenUnauthorizedException.getCode(), 401);
        }
    }

    @Test
    void wrongClient() {
        if (handlerBuilder == null)
            return;

        try (AbstractTokenAPIHandler handler = handlerBuilder.setClientSecret("Wrong").build()) {

            AuthAPI apiHandler = AuthAPI.newBuilder(handler).build();

            TokenUnauthorizedException tokenUnauthorizedException = assertThrows(
                    TokenUnauthorizedException.class,
                    () -> apiHandler.getMeAccount().execute()
            );

            log.info(tokenUnauthorizedException.toString());

            assertEquals(tokenUnauthorizedException.getCode(), 401);
        }
    }

    @Test
    void wrongUrl() throws MalformedURLException {
        if (handlerBuilder == null)
            return;

        try (AbstractTokenAPIHandler handler = handlerBuilder.setApiUrl("http://nothing.here").build()) {

            AuthAPI apiHandler = AuthAPI.newBuilder(handler).build();

            ConnectException exception = assertThrows(
                    ConnectException.class,
                    () -> apiHandler.getMeAccount().execute()
            );

            log.info(exception.toString());
        }

    }
}
