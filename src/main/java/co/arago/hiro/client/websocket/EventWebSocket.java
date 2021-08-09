package co.arago.hiro.client.websocket;

import co.arago.hiro.client.connection.AbstractWebSocketHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.exceptions.WebSocketException;
import co.arago.hiro.client.model.websocket.WebSocketTokenRefreshMessage;
import co.arago.hiro.client.util.RequiredFieldChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EventWebSocket extends AbstractWebSocketHandler {
    final Logger log = LoggerFactory.getLogger(EventWebSocket.class);

    public static abstract class Conf<T extends Conf<T>> extends AbstractWebSocketHandler.Conf<T> {

    }

    public static final class Builder extends Conf<Builder> {

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public EventWebSocket build() {
            RequiredFieldChecker.notNull(webSocketListener, "webSocketListener");
            return new EventWebSocket(this);
        }
    }

    protected class EventWebSocketListener implements Listener {

        /**
         * Called right after the websocket has been opened.
         */
        @Override
        public void onOpen() throws WebSocketException {
            tokenRefreshHandler.start();
        }

        /**
         * Called with an incoming message.
         *
         * @param message
         */
        @Override
        public void onMessage(String message) throws WebSocketException {

        }

        /**
         * Called right before the websocket is about to close.
         */
        @Override
        public void onClose() throws WebSocketException {
            tokenRefreshHandler.stop();
        }

        /**
         * Called when an Exception is detected.
         *
         * @param t The Throwable thrown.
         */
        @Override
        public void onError(Throwable t) {

        }
    }

    // ###############################################################################################
    // ## Token refresh thread ##
    // ###############################################################################################

    private class TokenRefreshHandler {

        private class TokenRefreshThread implements Runnable {

            @Override
            public void run() {
                try {
                    send(new WebSocketTokenRefreshMessage(tokenAPIHandler.getToken()).toJsonString());

                    tokenRefreshExecutor.schedule(this, msTillNextStart(), TimeUnit.MILLISECONDS);
                } catch (IOException | InterruptedException | HiroException e) {
                    log.error("Cannot refresh token.", e);
                }
            }

        }

        private ScheduledExecutorService tokenRefreshExecutor;

        public synchronized void start() {
            if (tokenRefreshExecutor != null)
                stop();

            tokenRefreshExecutor = Executors.newSingleThreadScheduledExecutor();
            tokenRefreshExecutor.schedule(new TokenRefreshThread(), msTillNextStart(), TimeUnit.MILLISECONDS);
        }

        public synchronized void stop() {
            if (tokenRefreshExecutor == null)
                return;

            tokenRefreshExecutor.shutdown();
            try {
                if (!tokenRefreshExecutor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                    tokenRefreshExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                tokenRefreshExecutor.shutdownNow();
            }

            tokenRefreshExecutor = null;
        }

        private long msTillNextStart() {
            Instant now = Instant.now();
            Instant nextStart = tokenAPIHandler.expiryInstant();

            return nextStart.toEpochMilli() - now.toEpochMilli();
        }
    }

    private final TokenRefreshHandler tokenRefreshHandler = new TokenRefreshHandler();

    protected EventWebSocket(Conf<?> builder) {
        super(builder);
    }

}
