package co.arago.hiro.client.websocket;

import co.arago.hiro.client.connection.token.AbstractTokenAPIHandler;
import co.arago.hiro.client.exceptions.*;
import co.arago.hiro.client.model.HiroError;
import co.arago.hiro.client.model.HiroMessage;
import co.arago.hiro.client.model.VersionResponse;
import co.arago.util.json.JsonUtil;
import co.arago.util.validation.RequiredFieldChecks;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Handles websockets. Tries to renew any aborted connection until the websocket gets closed from this side.
 */
public abstract class AuthenticatedWebSocketHandler extends RequiredFieldChecks implements AutoCloseable {

    final static Logger log = LoggerFactory.getLogger(AuthenticatedWebSocketHandler.class);

    // ###############################################################################################
    // ## Conf and Builder ##
    // ###############################################################################################

    /**
     * Configuration interface for all the parameters of an AuthenticatedAPIHandler.
     * Builder need to implement this.
     */
    public static abstract class Conf<T extends Conf<T>> {
        private String name;
        private String apiName;
        private String endpoint;
        private String protocol;
        private Map<String, String> query = new HashMap<>();
        private Map<String, String> headers = new HashMap<>();
        private String fragment;
        private long webSocketMessageTimeout = 60000L;
        private AbstractTokenAPIHandler tokenAPIHandler;
        private int maxRetries = 2;
        private boolean reconnectOnFailedSend = false;

        public String getName() {
            return name;
        }

        /**
         * @param name Set the name of this websocket. If this is not set, a default name will be created.
         *             This name will appear in logfiles.
         * @return {@link #self()}
         */
        public T setName(String name) {
            this.name = name;
            return self();
        }

        public String getApiName() {
            return apiName;
        }

        /**
         * @param apiName Set the name of the api. This name will be used to determine the API endpoint entry in the
         *                data obtained via a call to /api/version.
         * @return {@link #self()}
         */
        public T setApiName(String apiName) {
            this.apiName = apiName;
            return self();
        }

        public String getEndpoint() {
            return endpoint;
        }

        /**
         * Sets endpoint and protocol directly, omitting automatic endpoint detection via apiName.
         *
         * @param endpoint Set a custom endpoint.
         * @param protocol The protocol header for the websocket.
         * @return {@link #self()}
         */
        public T setEndpointAndProtocol(String endpoint, String protocol) {
            this.endpoint = endpoint;
            this.protocol = protocol;
            return self();
        }

        public String getProtocol() {
            return protocol;
        }

        /**
         * For initial connection to the WebSocket.
         *
         * @param query Map of query fields.
         * @return {@link #self()}
         */
        public T setQuery(Map<String, String> query) {
            this.query = query;
            return self();
        }

        /**
         * For initial connection to the WebSocket.
         *
         * @param key   Name of the query parameter
         * @param value Value of the query parameter
         * @return {@link #self()}
         */
        public T setQuery(String key, String value) {
            this.query.put(key, value);
            return self();
        }

        /**
         * For initial connection to the WebSocket.
         *
         * @param headers Map of header fields.
         * @return {@link #self()}
         */
        public T setHeaders(Map<String, String> headers) {
            this.headers = headers;
            return self();
        }

        /**
         * For initial connection to the WebSocket.
         *
         * @param fragment The URI fragment.
         * @return {@link #self()}
         */
        public T setFragment(String fragment) {
            this.fragment = fragment;
            return self();
        }

        public long getWebSocketMessageTimeout() {
            return this.webSocketMessageTimeout;
        }

        /**
         * @param webSocketMessageTimeout Message timeout in ms. Default is 60000.
         * @return {@link #self()}
         */
        public T setWebSocketMessageTimeout(Long webSocketMessageTimeout) {
            this.webSocketMessageTimeout = webSocketMessageTimeout;
            return self();
        }


        public int getMaxRetries() {
            return maxRetries;
        }

        /**
         * @param maxRetries Max amount of retries when http errors are received.
         * @return {@link #self()}
         */
        public T setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return self();
        }

        public AbstractTokenAPIHandler getTokenApiHandler() {
            return this.tokenAPIHandler;
        }

        /**
         * @param tokenAPIHandler The tokenAPIHandler for this API.
         * @return {@link #self()}
         */
        public T setTokenApiHandler(AbstractTokenAPIHandler tokenAPIHandler) {
            this.tokenAPIHandler = tokenAPIHandler;
            return self();
        }

        public boolean isReconnectOnFailedSend() {
            return reconnectOnFailedSend;
        }

        /**
         * Reset the websocket when {@link AuthenticatedWebSocketHandler#send(String)} fails. The default is false - throw
         * an Exception when all retries are exhausted.
         *
         * @param reconnectOnFailedSend The flag to set.
         * @return {@link #self()}
         */
        public T setReconnectOnFailedSend(boolean reconnectOnFailedSend) {
            this.reconnectOnFailedSend = reconnectOnFailedSend;
            return self();
        }

        protected abstract T self();

        public abstract AuthenticatedWebSocketHandler build();
    }

    /**
     * HiroWebSocketListener (thread) for incoming websocket messages. Derivates of {@link AuthenticatedWebSocketHandler} have
     * to supply an Object implementing the interface {@link SpecificWebSocketListener} for specific handling.
     */
    protected class InternalListener implements WebSocket.Listener {

        private final StringBuffer stringBuffer = new StringBuffer();

        private Throwable exception;

        private final SpecificWebSocketListener listener;
        private final String name;

        /**
         * Constructor
         *
         * @param name     Name of the handler (mainly for logging)
         * @param listener The listener which received messages.
         */
        public InternalListener(String name, SpecificWebSocketListener listener) {
            this.name = name;
            this.listener = listener;
        }

        /**
         * <p>
         * Sets the status from {@link Status#STARTING} or {@link Status#RESTARTING} to
         * {@link Status#RUNNING_PRELIMINARY}. Any other state will result in an Exception.
         * </p>
         * <p>
         * {@link WebSocket#request(long)} is NOT called here, but only after the CompletableFuture of
         * {@link WebSocket.Builder#buildAsync(URI, WebSocket.Listener)} has finished successfully in
         * {@link #createWebSocket()} and {@link #status} is not FAILURE. This ensures, that
         * {@link #onText(WebSocket, CharSequence, boolean)} is only called after that.
         * </p>
         *
         * @param webSocket The webSocket using this HiroWebSocketListener.
         * @throws IllegalStateException When the status is neither {@link Status#STARTING} nor
         *                               {@link Status#RESTARTING}.
         * @throws RuntimeException      On all other exceptions caught.
         * @see #createWebSocket()
         */
        @Override
        public void onOpen(WebSocket webSocket) {
            log.debug("{}: WebSocket open.", name);

            try {
                listener.onOpen(webSocket);

                Status currentStatus = status.updateAndGet(s ->
                        (s == Status.STARTING || s == Status.RESTARTING) ? Status.RUNNING_PRELIMINARY : s
                );

                if (currentStatus != Status.RUNNING_PRELIMINARY)
                    throw new IllegalStateException("WebSocket not in a starting state.");

            } catch (Exception e) {
                status.set(Status.FAILED);
                if (e instanceof RuntimeException)
                    throw (RuntimeException) e;
                else
                    throw new RuntimeException(e);
            }
        }

        /**
         * @see WebSocket.Listener
         */
        private CompletableFuture<?> accumulatedMessage = new CompletableFuture<>();

        /**
         * Collects a text message from the websocket until 'last' is true. Then checks for an error message. If the
         * error is 401 and the status is {@link Status#RUNNING}, tries to refresh the token.
         * When the first non-error message comes it, sets the status from {@link Status#RUNNING_PRELIMINARY} to
         * {@link Status#RUNNING}.
         *
         * @param webSocket The webSocket using this HiroWebSocketListener.
         * @param data      Message block
         * @param last      True if this is the last message block of a message
         * @return CompletionStage. See documentation at {@link WebSocket.Listener}.
         */
        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            stringBuffer.append(data);
            webSocket.request(1);

            CompletableFuture<?> currentFutureRef = accumulatedMessage;
            if (last) {
                String message = stringBuffer.toString();

                try {
                    HiroMessage hiroMessage = JsonUtil.DEFAULT.toObject(message, HiroMessage.class);

                    HiroError hiroError = hiroMessage.getError();
                    if (hiroError != null) {
                        if (hiroError.getCode() == 401) {

                            Status currentStatus = status.updateAndGet(s -> {
                                switch (s) {
                                    case RUNNING_PRELIMINARY:
                                        return Status.FAILED;
                                    case RUNNING:
                                        return Status.RESTARTING;
                                    default:
                                        return s;
                                }
                            });

                            if (currentStatus == Status.RESTARTING) {
                                throw new RefreshTokenWebSocketException("Refreshing token because of: " + message);
                            } else {
                                throw new UnauthorizedWebSocketException(hiroError.getMessage(), hiroError.getCode());
                            }
                        } else {
                            throw new WebSocketMessageException(hiroError.getMessage(), hiroError.getCode());
                        }
                    }

                    reconnectDelay.set(0);

                    listener.onMessage(webSocket, hiroMessage);

                    status.compareAndSet(Status.RUNNING_PRELIMINARY, Status.RUNNING);

                } catch (JsonProcessingException e) {
                    reconnectDelay.set(0);
                    log.warn("Ignoring unknown websocket message: {}", message, e);
                } catch (Exception e) {
                    onError(webSocket, e);
                } finally {
                    // Reset buffer and complete future when message has been fully received.
                    stringBuffer.setLength(0);

                    currentFutureRef.complete(null);
                    accumulatedMessage = new CompletableFuture<>();
                }
            }

            return currentFutureRef;
        }

        /**
         * Keep-alive messages
         *
         * @param webSocket The webSocket using this HiroWebSocketListener.
         * @param message   The message received. It will be reflected back via Pong.
         * @return The CompletionStage of {@link WebSocket#sendPong(ByteBuffer)}.
         */
        @Override
        public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
            webSocket.sendPong(message);
            return WebSocket.Listener.super.onPing(webSocket, message);
        }

        /**
         * Receives close messages. This will try to restart the WebSocket unless
         * the status is {@link Status#CLOSING}.
         *
         * @param webSocket  The webSocket using this HiroWebSocketListener.
         * @param statusCode Status code sent from the remote side.
         * @param reason     Reason text sent from the remote side.
         * @return null
         */
        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            log.debug("{}: Got close message {}", name, (StringUtils.isBlank(reason) ? statusCode : statusCode + ": " + reason));

            try {
                listener.onClose(webSocket, statusCode, reason);

                handleRestart(status.get(), false);

            } catch (WebSocketException e) {
                log.error("{}: WebSocket caught error while closing.", name, e);
                status.set(Status.CLOSING);
            }

            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        /**
         * Handles errors (Throwable) regarding websocket operation. This will try to restart the WebSocket unless
         * the status is {@link Status#CLOSING}. This will tear down the WebSocket when the status is
         * {@link Status#FAILED}.
         *
         * @param webSocket The webSocket using this HiroWebSocketListener.
         * @param error     The Throwable to handle.
         */
        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.error("{}: WebSocket caught error: {}", name, error.toString());
            this.exception = error;

            listener.onError(webSocket, error);

            handleRestart(status.get(), (error instanceof RefreshTokenWebSocketException));

            WebSocket.Listener.super.onError(webSocket, error);
        }

        private void handleRestart(Status currentStatus, boolean refreshToken) {
            if (currentStatus != Status.CLOSING && currentStatus != Status.CLOSED && currentStatus != Status.FAILED) {
                try {
                    status.set(Status.RESTARTING);
                    restartWebSocket(refreshToken);
                } catch (HiroException | IOException | InterruptedException e) {
                    status.set(Status.FAILED);
                    log.error("{}: Cannot restart WebSocket because of error.", name, e);
                    closeWebSocket(1006, "Abnormal close because of error " + e.getMessage());
                }
            }
        }

        /**
         * Reset listener for new usage. Clears any previous exception by setting {@link #exception} = null.
         */
        protected void reset() {
            exception = null;
        }

    }

    public enum Status {
        NONE,
        STARTING,
        RUNNING_PRELIMINARY,
        RUNNING,
        RESTARTING,
        CLOSING,
        FAILED,
        CLOSED
    }

    protected final AtomicReference<Status> status = new AtomicReference<>();

    protected final String name;
    protected final String apiName;
    protected final String endpoint;
    protected final String protocol;
    protected final int maxRetries;
    protected final AbstractTokenAPIHandler tokenAPIHandler;
    protected final Map<String, String> query = new HashMap<>();
    protected final Map<String, String> headers = new HashMap<>();
    protected final String fragment;
    protected final boolean reconnectOnFailedSend;
    protected final long webSocketRequestTimeout;

    protected URI webSocketUri;

    private WebSocket webSocket;
    protected InternalListener internalListener;
    private final AtomicInteger reconnectDelay = new AtomicInteger(0);

    /**
     * Protected Constructor
     *
     * @param builder The builder for this Handler.
     */
    protected AuthenticatedWebSocketHandler(Conf<?> builder) {
        this.name = builder.getName();
        this.apiName = builder.getApiName();
        this.endpoint = builder.getEndpoint();
        this.protocol = builder.getProtocol();
        this.maxRetries = builder.getMaxRetries();
        this.tokenAPIHandler = notNull(builder.getTokenApiHandler(), "tokenApiHandler");

        this.query.putAll(builder.query);
        this.headers.putAll(builder.headers);
        this.fragment = builder.fragment;
        this.reconnectOnFailedSend = builder.isReconnectOnFailedSend();

        this.webSocketRequestTimeout = builder.getWebSocketMessageTimeout();

        if (StringUtils.isBlank(this.apiName) && (StringUtils.isAnyBlank(this.endpoint, this.protocol)))
            anyError("Either 'apiName' or 'endpoint' and 'protocol' have to be set.");

    }

    /**
     * Create new WebSocket. Closes any old websocket.
     *
     * @throws HiroException        When creation of the webSocket fails.
     * @throws IOException          On IO problems.
     * @throws InterruptedException When the call gets interrupted.
     */
    protected synchronized void createWebSocket() throws HiroException, IOException, InterruptedException {
        if (webSocket != null) {
            closeWebSocket(1001, tokenAPIHandler.getUserAgent() + " restarts");
        }

        String protocol = this.protocol;
        String endpoint = this.endpoint;

        if (StringUtils.isAnyBlank(endpoint, protocol)) {
            VersionResponse.VersionEntry versionEntry = tokenAPIHandler.getVersionMap().getVersionEntryOf(apiName);
            if (StringUtils.isBlank(protocol))
                protocol = versionEntry.protocols;
            if (StringUtils.isBlank(endpoint))
                endpoint = versionEntry.endpoint;
        }

        if (webSocketUri == null)
            webSocketUri = tokenAPIHandler.buildWebSocketURI(endpoint);

        try {
            try {
                internalListener.reset();

                this.webSocket = tokenAPIHandler.getOrBuildClient()
                        .newWebSocketBuilder()
                        .subprotocols(protocol, "token-" + tokenAPIHandler.getToken())
                        .buildAsync(webSocketUri, internalListener)
                        .get();

                if (status.get() == Status.FAILED) {
                    throw new WebSocketException("Creating websocket returned with status \"FAILED\".",
                            internalListener.exception);
                }

                // After the webSocket has been created and the status been checked, start requesting messages.
                this.webSocket.request(1);

            } catch (ExecutionException e) {
                if (e.getCause() instanceof ConnectException)
                    throw new ConnectException("Cannot create webSocket " + webSocketUri + ".");
                else if (e.getCause() instanceof IOException)
                    throw new IOException("Cannot create webSocket " + webSocketUri + ".", e);
                else
                    throw new HiroException("Cannot create webSocket " + webSocketUri + ".", e);
            }
        } catch (Exception e) {
            closeWebSocket(1006, "Error at startup: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Close the websocket and set {@link #webSocket} to null.
     *
     * @param status The status for the close message
     * @param reason The reason string for the close message.
     */
    protected synchronized void closeWebSocket(int status, String reason) {
        if (webSocket != null) {
            try {
                if (!webSocket.isOutputClosed())
                    webSocket.sendClose(status, reason).get(webSocketRequestTimeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                log.warn("Exception when closing websocket.", e);
            }

            webSocket.abort();
            webSocket = null;
            System.gc();
        }
    }

    /**
     * Restarts the websocket by closing the old and creating a new one. Uses the same {@link #internalListener}. Delays
     * the restart according to {@link #backoff(int reconnectDelay)}.
     *
     * @param refreshToken Refresh the current token.
     * @throws HiroException        When creation of the webSocket fails.
     * @throws IOException          On IO problems.
     * @throws InterruptedException When the call gets interrupted.
     */
    protected synchronized void restartWebSocket(boolean refreshToken) throws HiroException, IOException, InterruptedException {
        if (refreshToken)
            tokenAPIHandler.refreshToken();

        reconnectDelay.set(backoff(reconnectDelay.get()));

        createWebSocket();
    }

    /**
     * Backoff operations that fail. The delay increases with every try up to 10 minutes.
     *
     * @param reconnectDelay The old delay value (seconds).
     * @return The new delay value (seconds).
     * @throws InterruptedException When {@link Thread#sleep(long)} gets interrupted.
     * @see #send(String)
     * @see #restartWebSocket(boolean)
     */
    protected int backoff(int reconnectDelay) throws InterruptedException {
        if (reconnectDelay > 0)
            Thread.sleep(reconnectDelay * 1000L);

        return (reconnectDelay < 10 ? reconnectDelay + 1 : (reconnectDelay < 60 ? reconnectDelay + 10 : 60 + new Random().nextInt(540)));
    }

    /**
     * Send a message across the websocket. Tries {@link #maxRetries} times to send a message before resetting the
     * websocket.
     *
     * @param message The message to send.
     * @throws HiroException        When sending finally fails.
     * @throws IOException          On IO problems.
     * @throws InterruptedException When the call gets interrupted.
     */
    public void send(String message) throws HiroException, InterruptedException, IOException {
        int retries = 0;
        int retry_delay = 0;

        while (true) {
            retry_delay = backoff(retry_delay);

            WebSocket webSocketRef;
            synchronized (this) {
                switch (status.get()) {
                    case NONE:
                        throw new WebSocketException("Websocket not started");
                    case CLOSED:
                    case FAILED:
                        throw new WebSocketException("Websocket has exited");
                    case RUNNING_PRELIMINARY:
                    case RUNNING:
                        break;
                    default:
                        throw new WebSocketException("Websocket not ready");
                }

                webSocketRef = webSocket;
            }
            if (webSocketRef == null)
                throw new WebSocketException("No webSocket available.");

            try {
                webSocketRef.sendText(message, true).get(webSocketRequestTimeout, TimeUnit.MILLISECONDS);
                return;
            } catch (InterruptedException e) {
                throw e;
            } catch (Exception e) {
                if (retries >= maxRetries) {
                    if (reconnectOnFailedSend) {
                        log.warn("Restarting webSocket because of: {}.", e.toString());
                        status.set(Status.RESTARTING);
                        restartWebSocket(false);
                    } else {
                        throw new HiroException("Cannot send message because of error.", e);
                    }
                } else {
                    log.warn("Retry to send message because of: {}", e.toString());
                    retries++;
                }
            }
        }
    }

    public synchronized void start() throws HiroException, IOException, InterruptedException {
        if (webSocket != null && !webSocket.isInputClosed() && !webSocket.isOutputClosed())
            return;

        status.set(Status.STARTING);
        createWebSocket();
    }

    @Override
    public synchronized void close() {
        status.set(Status.CLOSING);
        closeWebSocket(WebSocket.NORMAL_CLOSURE, tokenAPIHandler.getUserAgent() + " closing");
        status.set(Status.CLOSED);
    }

}
