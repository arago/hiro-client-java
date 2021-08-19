package co.arago.hiro.client.websocket;

import co.arago.hiro.client.Config;
import co.arago.hiro.client.connection.token.FixedTokenAPIHandler;
import co.arago.hiro.client.connection.token.PasswordAuthTokenAPIHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.exceptions.UnauthorizedWebSocketException;
import co.arago.hiro.client.exceptions.WebSocketException;
import co.arago.hiro.client.model.HiroMessage;
import co.arago.hiro.client.rest.AuthAPI;
import co.arago.hiro.client.util.JsonTools;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class EventWebSocketTest {

    final static Logger log = LoggerFactory.getLogger(EventWebSocketTest.class);

    public static Config config;

    public static class EventListener implements HiroWebSocketListener {
        public Throwable innerError;

        @Override
        public void onMessage(HiroMessage message) throws WebSocketException {

        }

        @Override
        public void onError(Throwable t) {
            innerError = t;
        }
    }


    @BeforeAll
    static void init() throws IOException {
        try {
            config = JsonTools.DEFAULT.toObject(Paths.get("src", "test", "resources", "config.json").toFile(), Config.class);
        } catch (FileNotFoundException e) {
            log.warn("Skipping tests: {}.", e.getMessage());
        }
    }

    @Test
    void testEventWebsocket() throws InterruptedException, IOException, HiroException {
        if (config == null)
            return;

        try (PasswordAuthTokenAPIHandler handler = PasswordAuthTokenAPIHandler.newBuilder()
                .setApiUrl(config.api_url)
                .setCredentials(config.username, config.password, config.client_id, config.client_secret)
                .setAcceptAllCerts(config.accept_all_certs)
                .setForceLogging(config.force_logging)
                .build()) {


            AuthAPI authAPI = AuthAPI.newBuilder(handler).build();

            String defaultScope = authAPI.getMeProfile().execute().getAttributeAsString("ogit/Auth/Account/defaultScope");

            try (EventWebSocket eventWebSocket = EventWebSocket.newBuilder(handler, new EventListener())
                    .addScope(defaultScope)
                    .build()) {
                eventWebSocket.start();
                Thread.sleep(1000);
            }
        }
    }

    @Test
    void testInvalidToken() throws InterruptedException, IOException, HiroException {
        if (config == null)
            return;

        EventListener listener = new EventListener();

        try (FixedTokenAPIHandler handler = FixedTokenAPIHandler.newBuilder()
                .setApiUrl(config.api_url)
                .setToken("Invalid")
                .setAcceptAllCerts(config.accept_all_certs)
                .build()) {

            try (EventWebSocket eventWebSocket = EventWebSocket.newBuilder(handler, listener)
                    .build()) {
                eventWebSocket.start();
                Thread.sleep(1000);
            }
        }

        assertThrows(
                UnauthorizedWebSocketException.class,
                () -> {
                    throw listener.innerError;
                }
        );
    }

    @Test
    void testInvalidUrl() throws IOException {
        if (config == null)
            return;

        EventListener listener = new EventListener();

        try (FixedTokenAPIHandler handler = FixedTokenAPIHandler.newBuilder()
                .setApiUrl("http://nothing.here")
                .setToken("Invalid")
                .setAcceptAllCerts(config.accept_all_certs)
                .build()) {

            try (EventWebSocket eventWebSocket = EventWebSocket.newBuilder(handler, listener)
                    .setEndpointAndProtocol("none", "event")
                    .build()) {

                assertThrows(
                        ConnectException.class,
                        eventWebSocket::start
                );
            }
        }
    }
}
