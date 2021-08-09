package co.arago.hiro.client.connection;

import co.arago.hiro.client.connection.token.AbstractTokenAPIHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.exceptions.WebSocketException;
import co.arago.hiro.client.model.HiroErrorResponse;
import co.arago.hiro.client.util.JsonTools;
import co.arago.hiro.client.websocket.Listener;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

public abstract class AbstractWebSocketHandler implements AutoCloseable {

    final Logger log = LoggerFactory.getLogger(AbstractWebSocketHandler.class);

    /**
     * Configuration interface for all the parameters of an AuthenticatedAPIHandler.
     * Builder need to implement this.
     */
    public static abstract class Conf<T extends Conf<T>> {
        protected String apiName;
        protected String endpoint;
        protected String protocol;
        protected Map<String, String> query = new HashMap<>();
        protected Map<String, String> headers = new HashMap<>();
        protected String fragment;
        protected Long httpRequestTimeout;
        protected AbstractTokenAPIHandler tokenAPIHandler;
        protected WebSocketListener webSocketListener;
        protected int maxRetries = 2;
        protected int maxListenerPool = 1;

        public String getApiName() {
            return apiName;
        }

        /**
         * @param apiName Set the name of the api. This name will be used to determine the API endpoint.
         * @return this
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
         * @return this
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
         * @return this
         */
        public T setQuery(Map<String, String> query) {
            this.query = query;
            return self();
        }

        /**
         * For initial connection to the WebSocket.
         *
         * @param headers Map of header fields.
         * @return this
         */
        public T setHeaders(Map<String, String> headers) {
            this.headers = headers;
            return self();
        }

        /**
         * For initial connection to the WebSocket.
         *
         * @param fragment The URI fragment.
         * @return this
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
         * @return this
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
         * @return this
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
         * @return this
         */
        public T setTokenApiHandler(AbstractTokenAPIHandler tokenAPIHandler) {
            this.tokenAPIHandler = tokenAPIHandler;
            return self();
        }

        protected abstract T self();

        public abstract AbstractWebSocketHandler build();
    }

    protected class WebSocketListener implements WebSocket.Listener {

        private final StringBuffer stringBuffer = new StringBuffer();

        private final Listener listener;
        private final String name;

        public WebSocketListener(String name, Listener listener) {
            this.name = name;
            this.listener = listener;
        }

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

            } catch (WebSocketException e) {
                setStatus(Status.CLOSING);
                onError(webSocket, e);
            }
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            stringBuffer.append(data);

            if (last) {
                String message = stringBuffer.toString();
                stringBuffer.setLength(0);

                try {
                    synchronized (AbstractWebSocketHandler.this) {
                        HiroErrorResponse hiroErrorResponse = JsonTools.DEFAULT.toObject(message, HiroErrorResponse.class);
                        if (hiroErrorResponse.isError()) {
                            if (hiroErrorResponse.getHiroErrorCode() == 401) {
                                if (getStatus() == Status.RUNNING) {
                                    tokenAPIHandler.refreshToken();
                                    setStatus(Status.RESTARTING);

                                    log.info("{}: Refreshing token because of error: {}", name, message);

                                    restartWebSocket();
                                } else if (getStatus() == Status.RUNNING_PRELIMINARY) {
                                    throw new WebSocketException("Received error message while token was never valid: " + message);
                                } else {
                                    throw new WebSocketException("Received error message: " + message);
                                }
                            } else {
                                throw new WebSocketException("Received error message: " + message);
                            }
                        }

                        reconnectDelay = 0;
                    }
                } catch (JsonProcessingException e) {
                    // Ignore processing exceptions
                } catch (HiroException | IOException e) {
                    setStatus(Status.CLOSING);
                    onError(webSocket, e);
                } catch (InterruptedException e) {
                    // Just return immediately
                    return null;
                }

                try {
                    listener.onMessage(message);

                    if (getStatus() == Status.RUNNING_PRELIMINARY) {
                        setStatus(Status.RUNNING);
                    }
                } catch (WebSocketException e) {
                    setStatus(Status.CLOSING);
                    onError(webSocket, e);
                }
            }
            webSocket.request(1);

            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        @Override
        public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
            return webSocket.sendPong(message);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            log.debug("{}: Got close message {}: {}", name, statusCode, reason);

            try {
                listener.onClose();
                handleRestart(webSocket);

            } catch (WebSocketException e) {
                log.error("{}: WebSocket caught error while closing.", name, e);
                setStatus(Status.CLOSING);
            }

            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.error("{}: WebSocket caught error.", name, error);

            listener.onError(error);

            handleRestart(webSocket);

            WebSocket.Listener.super.onError(webSocket, error);
        }

        private void handleRestart(WebSocket webSocket) {
            synchronized (AbstractWebSocketHandler.this) {
                if (getStatus() != Status.CLOSING) {
                    try {
                        setStatus(Status.RESTARTING);
                        restartWebSocket();
                    } catch (HiroException | IOException | InterruptedException e) {
                        log.error("{}: Cannot restart WebSocket because of error.", name, e);
                        closeWebSocket(1011, "Abnormal close because of error " + e.getMessage());
                        setStatus(Status.CLOSED);
                    }
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

    protected URI apiUri;

    private WebSocket webSocket;
    private WebSocketListener webSocketListener;
    private int reconnectDelay = 0;

    protected AbstractWebSocketHandler(Conf<?> builder) {
        this.apiName = builder.getApiName();
        this.endpoint = builder.getEndpoint();
        this.protocol = builder.getProtocol();
        this.maxRetries = builder.getMaxRetries();
        this.tokenAPIHandler = builder.getTokenApiHandler();

        this.query.putAll(builder.query);
        this.headers.putAll(builder.headers);
        this.fragment = builder.fragment;
    }

    /**
     * Construct my URI with query parameters and fragment.
     * This method will query /api/version once to construct the URI unless {@link #endpoint} is set.
     *
     * @param path     The path to append to the API path.
     * @param query    Map of query parameters for this URI. Can be null for no query parameters.
     * @param fragment The fragment to add to the URI.
     * @return The URI with query parameters and fragment.
     * @throws IOException          On a failed call to /api/version
     * @throws InterruptedException Call got interrupted
     * @throws HiroException        When calling /api/version responds with an error
     */
    public URI getUri(String path, Map<String, String> query, String fragment) throws IOException, InterruptedException, HiroException {
        if (apiUri == null)
            apiUri = (endpoint != null ? tokenAPIHandler.buildURI(endpoint) : tokenAPIHandler.getApiUriOf(apiName));

        URI pathUri = apiUri.resolve(StringUtils.removeStart(path, "/"));

        return tokenAPIHandler.addQueryAndFragment(pathUri, query, fragment);
    }

    /**
     * Create new WebSocket. Closes any old websocket.
     *
     * @param webSocketListener The listener to apply to the webSocket. Must not be null.
     * @throws HiroException        When creation of the webSocket fails.
     * @throws IOException          On IO problems.
     * @throws InterruptedException When the call gets interrupted.
     */
    protected synchronized void createWebSocket(WebSocketListener webSocketListener) throws HiroException, IOException, InterruptedException {
        if (webSocketListener == null)
            throw new HiroException("WebSocketListener must not be null");

        try {
            if (webSocket != null) {
                closeWebSocket(1012, tokenAPIHandler.getUserAgent() + " restarts");
            }

            webSocket = tokenAPIHandler.getOrBuildClient()
                    .newWebSocketBuilder()
                    .header("Sec-WebSocket-Protocol", protocol + ", token-" + tokenAPIHandler.getToken())
                    .buildAsync(getUri("/", query, fragment), webSocketListener)
                    .get();

            this.webSocketListener = webSocketListener;
        } catch (ExecutionException e) {
            throw new HiroException("Cannot create webSocket.", e);
        }
    }

    protected synchronized void closeWebSocket(int status, String reason) {
        if (webSocket != null) {
            webSocket.sendClose(status, reason);
            webSocket.abort();
            webSocket = null;
            System.gc();
        }
    }

    protected synchronized void restartWebSocket() throws HiroException, IOException, InterruptedException {
        if (webSocketListener == null)
            return;

        reconnectDelay = backoff(reconnectDelay);

        createWebSocket(webSocketListener);
    }


    protected int backoff(int reconnectDelay) throws InterruptedException {
        if (reconnectDelay > 0)
            Thread.sleep(reconnectDelay * 1000L);

        return (reconnectDelay < 10 ? reconnectDelay + 1 : (reconnectDelay < 60 ? reconnectDelay + 10 : 60 + new Random().nextInt(540)));
    }

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
                webSocketRef.sendText(message, true);
                return;
            } catch (Exception e) {
                if (retries >= maxRetries) {
                    log.warn("Restarting webSocket.", e);
                    restartWebSocket();
                } else {
                    log.warn("Retry to send message.", e);
                    retries++;
                }
            }
        }
    }

    public synchronized void start(WebSocketListener webSocketListener) throws HiroException, IOException, InterruptedException {
        if (webSocket != null && !webSocket.isInputClosed() && !webSocket.isOutputClosed())
            return;

        if (webSocketListener == null)
            throw new HiroException("WebSocketListener must not be null");

        setStatus(Status.STARTING);
        createWebSocket(webSocketListener);
    }

    @Override
    public synchronized void close() {
        setStatus(Status.CLOSING);
        closeWebSocket(WebSocket.NORMAL_CLOSURE, tokenAPIHandler.getUserAgent() + " closing");
        setStatus(Status.CLOSED);
    }

}
