package co.arago.hiro.client.websocket;

import co.arago.hiro.client.connection.token.AbstractTokenAPIHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.exceptions.UnauthorizedWebSocketException;
import co.arago.hiro.client.exceptions.WebSocketException;
import co.arago.hiro.client.exceptions.WebSocketMessageException;
import co.arago.hiro.client.model.HiroError;
import co.arago.hiro.client.model.HiroMessage;
import co.arago.hiro.client.model.VersionResponse;
import co.arago.hiro.client.util.JsonTools;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Handles websockets. Tries to renew any aborted connection until the websocket gets closed from this side.
 */
public abstract class AbstractWebSocketHandler implements AutoCloseable {

    final static Logger log = LoggerFactory.getLogger(AbstractWebSocketHandler.class);

    // ###############################################################################################
    // ## Conf and Builder ##
    // ###############################################################################################

    /**
     * Configuration interface for all the parameters of an AuthenticatedAPIHandler.
     * Builder need to implement this.
     */
    public static abstract class Conf<T extends Conf<T>> {
        private String apiName;
        private String endpoint;
        private String protocol;
        private Map<String, String> query = new HashMap<>();
        private Map<String, String> headers = new HashMap<>();
        private String fragment;
        private Long httpRequestTimeout;
        private AbstractTokenAPIHandler tokenAPIHandler;
        private HiroWebSocketListener webSocketListener;
        private int maxRetries = 2;
        private boolean reconnectOnFailedSend = false;

        public String getApiName() {
            return apiName;
        }

        /**
         * @param apiName Set the name of the api. This name will be used to determine the API endpoint.
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

        public Long getHttpRequestTimeout() {
            return this.httpRequestTimeout;
        }

        /**
         * @param httpRequestTimeout Request timeout in ms.
         * @return {@link #self()}
         */
        public T setHttpRequestTimeout(Long httpRequestTimeout) {
            this.httpRequestTimeout = httpRequestTimeout;
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
         * Reset the websocket when {@link AbstractWebSocketHandler#send(String)} fails. The default is false - throw
         * an Exception when all retries are exhausted.
         *
         * @param reconnectOnFailedSend The flag to set.
         * @return {@link #self()}
         */
        public T setReconnectOnFailedSend(boolean reconnectOnFailedSend) {
            this.reconnectOnFailedSend = reconnectOnFailedSend;
            return self();
        }

        public HiroWebSocketListener getWebSocketListener() {
            return webSocketListener;
        }

        /**
         * Set the {@link HiroWebSocketListener} for the websocket data.
         *
         * @param webSocketListener The listener to use.
         * @return {@link #self()}
         */
        public T setWebSocketListener(HiroWebSocketListener webSocketListener) {
            this.webSocketListener = webSocketListener;
            return self();
        }

        protected abstract T self();

        public abstract AbstractWebSocketHandler build();
    }

    /**
     * HiroWebSocketListener (thread) for incoming websocket messages. Derivates of {@link AbstractWebSocketHandler} have
     * to supply an Object implementing the interface {@link HiroWebSocketListener} for specific handling.
     */
    protected class InternalListener implements WebSocket.Listener {

        private final StringBuffer stringBuffer = new StringBuffer();

        private final HiroWebSocketListener listener;
        private final String name;

        /**
         * Constructor
         *
         * @param name     Name of the handler (mainly for logging)
         * @param listener The listener which received messages.
         */
        public InternalListener(String name, HiroWebSocketListener listener) {
            this.name = name;
            this.listener = listener;
        }

        /**
         * Sets the status from {@link Status#STARTING} or {@link Status#RESTARTING} to
         * {@link Status#RUNNING_PRELIMINARY}. Any other state will result in an Exception.
         *
         * @param webSocket The webSocket using this HiroWebSocketListener.
         * @throws IllegalStateException When the status is neither {@link Status#STARTING} nor
         *                               {@link Status#RESTARTING}.
         */
        @Override
        public void onOpen(WebSocket webSocket) {
            log.debug("{}: WebSocket open.", name);

            try {
                listener.onOpen();

                synchronized (AbstractWebSocketHandler.this) {
                    if (getStatus() != Status.STARTING && getStatus() != Status.RESTARTING)
                        throw new IllegalStateException("WebSocket not in a starting state.");

                    setStatus(Status.RUNNING_PRELIMINARY);
                }

            } catch (Exception e) {
                setStatus(Status.CLOSING);
                onError(webSocket, e);
            } finally {
                WebSocket.Listener.super.onOpen(webSocket);
            }
        }

        /**
         * Collects a text message from the websocket until 'last' is true. Then checks for an error message. If the
         * error is 401 and the status is {@link Status#RUNNING}, tries to refresh the token.
         * When the first non-error message comes it, sets the status from {@link Status#RUNNING_PRELIMINARY} to
         * {@link Status#RUNNING}.
         *
         * @param webSocket The webSocket using this HiroWebSocketListener.
         * @param data      Message block
         * @param last      True if this is the last message block of a message
         * @return null.
         */
        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            stringBuffer.append(data);

            if (last) {
                String message = stringBuffer.toString();
                stringBuffer.setLength(0);

                try {
                    synchronized (AbstractWebSocketHandler.this) {
                        HiroMessage hiroMessage = JsonTools.DEFAULT.toObject(message, HiroMessage.class);

                        HiroError hiroError = hiroMessage.getError();
                        if (hiroError != null) {
                            if (hiroError.getCode() == 401) {
                                if (getStatus() == Status.RUNNING) {
                                    tokenAPIHandler.refreshToken();
                                    setStatus(Status.RESTARTING);

                                    log.info("{}: Refreshing token because of error: {}", name, message);

                                    restartWebSocket();
                                } else {
                                    throw new UnauthorizedWebSocketException(hiroError.getMessage(), hiroError.getCode());
                                }
                            } else {
                                throw new WebSocketMessageException(hiroError.getMessage(), hiroError.getCode());
                            }
                        }

                        reconnectDelay = 0;

                        listener.onMessage(hiroMessage);

                        if (getStatus() == Status.RUNNING_PRELIMINARY) {
                            setStatus(Status.RUNNING);
                        }
                    }
                } catch (JsonProcessingException e) {
                    reconnectDelay = 0;
                    log.warn("Ignoring unknown websocket message: {}", message, e);
                } catch (HiroException | IOException e) {
                    setStatus(Status.FAILED);
                    onError(webSocket, e);
                } catch (InterruptedException e) {
                    // Just return immediately
                    return null;
                }
            }

            return WebSocket.Listener.super.onText(webSocket, data, last);
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
            log.debug("{}: Got close message {}: {}", name, statusCode, reason);

            try {
                listener.onClose();

                synchronized (AbstractWebSocketHandler.this) {
                    handleRestart();
                }

            } catch (WebSocketException e) {
                log.error("{}: WebSocket caught error while closing.", name, e);
                setStatus(Status.CLOSING);
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

            listener.onError(error);

            synchronized (AbstractWebSocketHandler.this) {
                if (getStatus() == Status.FAILED) {
                    closeWebSocket(1011, "Abnormal close because of status 'FAILED'.");
                } else handleRestart();
            }

            WebSocket.Listener.super.onError(webSocket, error);
        }

        private void handleRestart() {
            if (getStatus() != Status.CLOSING && getStatus() != Status.CLOSED && getStatus() != Status.FAILED) {
                try {
                    setStatus(Status.RESTARTING);
                    restartWebSocket();
                } catch (HiroException | IOException | InterruptedException e) {
                    log.error("{}: Cannot restart WebSocket because of error.", name, e);
                    closeWebSocket(1011, "Abnormal close because of error " + e.getMessage());
                    setStatus(Status.FAILED);
                }
            }
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

    private Status status;

    public synchronized Status getStatus() {
        return status;
    }

    public synchronized void setStatus(Status status) {
        this.status = status;
    }

    protected final String apiName;
    protected final String endpoint;
    protected final String protocol;
    protected final int maxRetries;
    protected final AbstractTokenAPIHandler tokenAPIHandler;
    protected final Map<String, String> query = new HashMap<>();
    protected final Map<String, String> headers = new HashMap<>();
    protected final String fragment;
    protected final boolean reconnectOnFailedSend;

    protected URI apiUri;

    private WebSocket webSocket;
    private final InternalListener internalListener;
    private int reconnectDelay = 0;

    /**
     * Protected Constructor
     *
     * @param builder The builder for this Handler.
     */
    protected AbstractWebSocketHandler(Conf<?> builder) {
        this.apiName = builder.getApiName();
        this.endpoint = builder.getEndpoint();
        this.protocol = builder.getProtocol();
        this.maxRetries = builder.getMaxRetries();
        this.internalListener = constructInternalListener(builder.getWebSocketListener());
        this.tokenAPIHandler = builder.getTokenApiHandler();

        this.query.putAll(builder.query);
        this.headers.putAll(builder.headers);
        this.fragment = builder.fragment;
        this.reconnectOnFailedSend = builder.isReconnectOnFailedSend();
    }

    /**
     * Abstract method to embed the {@link HiroWebSocketListener} in the required {@link InternalListener}.
     *
     * @param webSocketListener The listener to embed.
     * @return The {@link InternalListener} containing the {@link HiroWebSocketListener}.
     */
    protected abstract InternalListener constructInternalListener(HiroWebSocketListener webSocketListener);

    /**
     * Create new WebSocket. Closes any old websocket.
     *
     * @throws HiroException        When creation of the webSocket fails.
     * @throws IOException          On IO problems.
     * @throws InterruptedException When the call gets interrupted.
     */
    protected void createWebSocket() throws HiroException, IOException, InterruptedException {
        synchronized (this) {
            if (webSocket != null) {
                closeWebSocket(1012, tokenAPIHandler.getUserAgent() + " restarts");
            }
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

        synchronized (this) {
            if (apiUri == null)
                apiUri = tokenAPIHandler.buildURI(endpoint);
        }

        try {
            URI uri = AbstractTokenAPIHandler.addQueryAndFragment(apiUri, query, fragment);
            String scheme = StringUtils.equals(uri.getScheme(), "http") ? "ws" : "wss";
            URI webSocketUri = new URI(scheme, uri.getAuthority(), uri.getPath(), uri.getQuery(), uri.getFragment());

            try {
                WebSocket webSocket = tokenAPIHandler.getOrBuildClient()
                        .newWebSocketBuilder()
                        .subprotocols(protocol, "token-" + tokenAPIHandler.getToken())
                        .buildAsync(webSocketUri, internalListener)
                        .get();

                synchronized (this) {
                    this.webSocket = webSocket;
                }
            } catch (ExecutionException e) {
                if (e.getCause() instanceof ConnectException)
                    throw new ConnectException("Cannot create webSocket " + webSocketUri.toString() + ".");
                else if (e.getCause() instanceof IOException)
                    throw new IOException("Cannot create webSocket " + webSocketUri.toString() + ".", e);
                else
                    throw new HiroException("Cannot create webSocket " + webSocketUri.toString() + ".", e);
            }
        } catch (URISyntaxException e) {
            throw new HiroException("Cannot create webSocket because of invalid URI.", e);
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
                    webSocket.sendClose(status, reason).get(5, TimeUnit.SECONDS);
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
     * @throws HiroException        When creation of the webSocket fails.
     * @throws IOException          On IO problems.
     * @throws InterruptedException When the call gets interrupted.
     */
    protected void restartWebSocket() throws HiroException, IOException, InterruptedException {
        reconnectDelay = backoff(reconnectDelay);

        createWebSocket();
    }

    /**
     * Backoff operations that fail. The delay increases with every try up to 10 minutes.
     *
     * @param reconnectDelay The old delay value (seconds).
     * @return The new delay value (seconds).
     * @throws InterruptedException When {@link Thread#sleep(long)} gets interrupted.
     * @see #send(String)
     * @see #restartWebSocket()
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
                switch (getStatus()) {
                    case NONE:
                        throw new WebSocketException("Websocket not started");
                    case CLOSED:
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
                webSocketRef.sendText(message, true).get();
                return;
            } catch (InterruptedException e) {
                throw e;
            } catch (Exception e) {
                if (retries >= maxRetries) {
                    if (reconnectOnFailedSend) {
                        log.warn("Restarting webSocket.", e);
                        restartWebSocket();
                    } else {
                        throw new HiroException("Cannot send message because of error.", e);
                    }
                } else {
                    log.warn("Retry to send message.", e);
                    retries++;
                }
            }
        }
    }

    public void start() throws HiroException, IOException, InterruptedException {
        synchronized (this) {
            if (webSocket != null && !webSocket.isInputClosed() && !webSocket.isOutputClosed())
                return;

            setStatus(Status.STARTING);
        }
        createWebSocket();
    }

    @Override
    public synchronized void close() {
        setStatus(Status.CLOSING);
        closeWebSocket(WebSocket.NORMAL_CLOSURE, tokenAPIHandler.getUserAgent() + " closing");
        setStatus(Status.CLOSED);
    }

}
