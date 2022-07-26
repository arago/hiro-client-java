package co.arago.hiro.client.connection.token;

import co.arago.hiro.client.ConfigModel;
import co.arago.hiro.client.exceptions.TokenUnauthorizedException;
import co.arago.hiro.client.mock.MockGraphitServerExtension;
import co.arago.hiro.client.rest.AuthAPI;
import co.arago.util.json.JsonUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockGraphitServerExtension.class)
public class InvalidCredentialsAPITest {
    public static ConfigModel config;

    final static Logger log = LoggerFactory.getLogger(InvalidCredentialsAPITest.class);

    @BeforeAll
    static void init() throws IOException {
        config = JsonUtil.DEFAULT.toObject(
                InvalidCredentialsAPITest.class.getClassLoader().getResourceAsStream("config.json"),
                ConfigModel.class);
    }

    @Test
    void wrongCredentials() {
        if (config == null)
            return;

        try (TokenAPIHandler handler = PasswordAuthTokenAPIHandler.newBuilder()
                .setAcceptAllCerts(config.accept_all_certs)
                .setShutdownTimeout(0)
                .setRootApiURI(config.api_url)
                .setCredentials(config.username, config.password, config.client_id, config.client_secret)
                .setForceLogging(config.force_logging)
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

        try (TokenAPIHandler handler = PasswordAuthTokenAPIHandler.newBuilder()
                .setAcceptAllCerts(config.accept_all_certs)
                .setShutdownTimeout(0)
                .setRootApiURI(config.api_url)
                .setCredentials(config.username, config.password, config.client_id, config.client_secret)
                .setForceLogging(config.force_logging)
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
    void wrongUrl() {
        if (config == null)
            return;

        try (TokenAPIHandler handler = PasswordAuthTokenAPIHandler.newBuilder()
                .setAcceptAllCerts(config.accept_all_certs)
                .setShutdownTimeout(0)
                .setCredentials(config.username, config.password, config.client_id, config.client_secret)
                .setForceLogging(config.force_logging)
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
