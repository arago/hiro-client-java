package co.arago.hiro.client.connection;

import co.arago.hiro.client.connection.token.AbstractTokenAPIHandler;
import co.arago.hiro.client.connection.token.PasswordAuthTokenAPIHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.exceptions.TokenUnauthorizedException;
import co.arago.hiro.client.rest.AuthAPI;
import co.arago.hiro.client.util.JsonTools;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InvalidCredentialsAPITest {
    public static PasswordAuthTokenAPIHandler.Builder handlerBuilder;
    public static Config config;

    final Logger log = LoggerFactory.getLogger(TokenAPIHandlerTest.class);

    @BeforeEach
    void init() throws IOException {
        Config config = JsonTools.DEFAULT.toObject(Paths.get("src", "test", "resources", "config.json").toFile(), Config.class);

        handlerBuilder = PasswordAuthTokenAPIHandler.newBuilder()
                .setApiUrl(config.api_url)
                .setCredentials(config.username, config.password, config.client_id, config.client_secret)
                .setAcceptAllCerts(config.accept_all_certs)
                .setForceLogging(true);
    }

    @Test
    void wrongCredentials() throws InterruptedException, IOException, HiroException {

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
    void wrongClient() throws InterruptedException, IOException, HiroException {

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
}
