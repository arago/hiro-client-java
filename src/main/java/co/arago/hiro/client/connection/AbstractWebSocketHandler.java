package co.arago.hiro.client.connection;

import co.arago.hiro.client.connection.token.AbstractTokenAPIHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.exceptions.WebSocketException;
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
import java.util.concurrent.atomic.AtomicReference;

public class AbstractWebSocketHandler {

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

        public abstract AuthenticatedAPIHandler build();
    }

    public static abstract class WebSocketListener implements WebSocket.Listener {

        public enum Status {
            NONE,
            STARTING,
            RUNNING_PRELIMINARY,
            RUNNING,
            RESTARTING,
            DONE,
            FAILED
        }

        public AtomicReference<Status> status = new AtomicReference<>(Status.NONE);

        @Override
        public void onOpen(WebSocket webSocket) {
            status.set(Status.RUNNING_PRELIMINARY);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        @Override
        public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
            return webSocket.sendPong(message);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            WebSocket.Listener.super.onError(webSocket, error);
        }
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
    protected WebSocket webSocket;
    protected WebSocketListener webSocketListener;

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
     * Get the current webSocket or create it if it is not there yet.
     *
     * @param webSocketListener The listener to apply to the webSocket. Must not be null.
     * @return The {@link #webSocket}.
     * @throws HiroException        When creation of the webSocket fails.
     * @throws IOException          On IO problems.
     * @throws InterruptedException When the call gets interrupted.
     */
    public WebSocket getOrCreateWebSocket(WebSocketListener webSocketListener) throws HiroException, IOException, InterruptedException {
        if (webSocket != null)
            return webSocket;

        try {
            webSocket = tokenAPIHandler.getOrBuildClient()
                    .newWebSocketBuilder()
                    .header("Sec-WebSocket-Protocol", protocol + ", token-" + tokenAPIHandler.getToken())
                    .buildAsync(getUri("/", query, fragment), webSocketListener)
                    .get();

            this.webSocketListener = webSocketListener;
        } catch (ExecutionException e) {
            throw new HiroException("Cannot create webSocket.", e);
        }

        return webSocket;
    }

    protected int backoff(int reconnectDelay) throws InterruptedException {
        if (reconnectDelay > 0)
            Thread.sleep(reconnectDelay * 1000L);

        return (reconnectDelay < 10 ? reconnectDelay + 1 : (reconnectDelay < 60 ? reconnectDelay + 10 : 60 + new Random().nextInt(540)));
    }

    public void send(String message) throws WebSocketException, InterruptedException {
        if (webSocket == null)
            throw new WebSocketException("No webSocket available.");

        int retries = 0;
        int retry_delay = 0;

        while (true) {
            retry_delay = backoff(retry_delay);

            switch (webSocketListener.status.get()) {
                case NONE:
                    throw new WebSocketException("Websocket not started");
                case DONE:
                case FAILED:
                    throw new WebSocketException("Websocket has exited");
                case RUNNING_PRELIMINARY:
                case RUNNING:
                    break;
                default:
                    throw new WebSocketException("Websocket not ready");
            }

            try {
                webSocket.sendText(message, true);
                return;
            } catch (Exception e) {
                if (retries >= maxRetries) {
                    log.warn("Restarting webSocket.", e);
                    // restart();
                } else {
                    log.warn("Retry to send message.", e);
                    retries++;
                }
            }
        }
    }

}
