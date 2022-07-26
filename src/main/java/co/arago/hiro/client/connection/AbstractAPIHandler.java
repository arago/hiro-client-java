package co.arago.hiro.client.connection;

import co.arago.hiro.client.connection.httpclient.DefaultHttpClientHandler;
import co.arago.hiro.client.connection.httpclient.HttpClientHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.exceptions.HiroHttpException;
import co.arago.hiro.client.exceptions.RetryException;
import co.arago.hiro.client.exceptions.TokenUnauthorizedException;
import co.arago.hiro.client.model.HiroError;
import co.arago.hiro.client.model.HiroMessage;
import co.arago.hiro.client.util.HttpLogger;
import co.arago.hiro.client.util.httpclient.HttpHeaderMap;
import co.arago.hiro.client.util.httpclient.HttpResponseParser;
import co.arago.hiro.client.util.httpclient.StreamContainer;
import co.arago.hiro.client.util.httpclient.URIEncodedData;
import co.arago.hiro.client.util.httpclient.URLPartEncoder;
import co.arago.util.json.JsonUtil;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieManager;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static co.arago.util.validation.ValueChecks.notNull;

/**
 * Root class with fields and tool methods for all API Handlers
 */
public abstract class AbstractAPIHandler {

    // ###############################################################################################
    // ## Conf and Builder ##
    // ###############################################################################################

    /**
     * The basic configuration for all APIHAndler. This also integrates the configuration for a default
     * HttpClientHandler.
     *
     * @param <T> The type of the Builder
     */
    public static abstract class Conf<T extends Conf<T>> implements HttpClientHandler.ConfTemplate<T> {
        private URI rootApiURI;
        private URI webSocketURI;
        private String userAgent;
        private Long httpRequestTimeout;
        private int maxRetries;

        // Reference to an already existing httpClientHandler.
        private HttpClientHandler httpClientHandler;

        // Configuration for a DefaultHttpClientHandler that is created internally.
        private final DefaultHttpClientHandler.Conf<?> defaultHttpClientHandlerBuilder = DefaultHttpClientHandler.newBuilder();

        public URI getRootApiURI() {
            return rootApiURI;
        }

        public URI getWebSocketURI() {
            return webSocketURI;
        }

        /**
         * @param rootApiURI The root url for the API
         * @return {@link #self()}
         * @throws URISyntaxException When the rootApiURI is malformed.
         */
        public T setRootApiURI(String rootApiURI) throws URISyntaxException {
            this.rootApiURI = new URI(RegExUtils.removePattern(rootApiURI, "/+$") + "/");
            return self();
        }

        /**
         * @param rootApiURI The root uri for the API
         * @return {@link #self()}
         */
        public T setRootApiURI(URI rootApiURI) {
            this.rootApiURI = rootApiURI;
            return self();
        }

        /**
         * @param webSocketURI The root uri for the WebSockets. If this is missing, the uri will be constructed
         *                     from an apiUrl.
         * @return {@link #self()}
         * @throws URISyntaxException When the webSocketURI is a malformed URI.
         */
        public T setWebSocketURI(String webSocketURI) throws URISyntaxException {
            this.webSocketURI = new URI(RegExUtils.removePattern(webSocketURI, "/+$") + "/");
            return self();
        }

        /**
         * @param apiURI The root url for the WebSockets
         * @return {@link #self()}
         */
        public T setWebSocketURI(URI apiURI) {
            this.rootApiURI = apiURI;
            return self();
        }

        public String getUserAgent() {
            return userAgent;
        }

        /**
         * For header "User-Agent". Default is determined by the package.
         *
         * @param userAgent The line for the User-Agent header.
         * @return {@link #self()}
         */
        public T setUserAgent(String userAgent) {
            this.userAgent = userAgent;
            return self();
        }

        public Long getHttpRequestTimeout() {
            return httpRequestTimeout;
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
         * @param maxRetries Max amount of retries when http errors are received. The default is 0.
         * @return {@link #self()}
         */
        public T setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return self();
        }

        public HttpClientHandler getHttpClientHandler() {
            return httpClientHandler;
        }

        /**
         * <p>
         * Sets the httpClientHandler for the backend connection. This handler will be shared among all
         * APIHandlers that use this configuration instance.
         * </p>
         * <p>
         * Setting this handler means, that no DefaultHttpClientHandler will be created internally. All configuration
         * options that are used for the internal handler will be ignored.
         * </p>
         *
         * @param httpClientHandler The connection handler to use.
         * @return {@link #self()}
         * @see HttpClientHandler.ConfTemplate
         */
        public T setHttpClientHandler(HttpClientHandler httpClientHandler) {
            this.httpClientHandler = httpClientHandler;
            return self();
        }

        @Override
        public HttpClientHandler.ProxySpec getProxy() {
            return defaultHttpClientHandlerBuilder.getProxy();
        }

        /**
         * @param proxy Simple proxy with one address and port
         * @return {@link #self()}
         * @implNote Will be ignored if {@link #httpClientHandler} is set.
         */
        @Override
        public T setProxy(HttpClientHandler.ProxySpec proxy) {
            defaultHttpClientHandlerBuilder.setProxy(proxy);
            return self();
        }

        @Override
        public boolean isFollowRedirects() {
            return defaultHttpClientHandlerBuilder.isFollowRedirects();
        }

        /**
         * @param followRedirects Enable Redirect.NORMAL. Default is true.
         * @return {@link #self()}
         * @implNote Will be ignored if {@link #httpClientHandler} is set.
         */
        @Override
        public T setFollowRedirects(boolean followRedirects) {
            defaultHttpClientHandlerBuilder.setFollowRedirects(followRedirects);
            return self();
        }

        @Override
        public Long getConnectTimeout() {
            return defaultHttpClientHandlerBuilder.getShutdownTimeout();
        }

        /**
         * @param connectTimeout Connect timeout in milliseconds.
         * @return {@link #self()}
         * @implNote Will be ignored if {@link #httpClientHandler} is set.
         */
        @Override
        public T setConnectTimeout(Long connectTimeout) {
            defaultHttpClientHandlerBuilder.setConnectTimeout(connectTimeout);
            return self();
        }

        @Override
        public long getShutdownTimeout() {
            return defaultHttpClientHandlerBuilder.getShutdownTimeout();
        }

        /**
         * @param shutdownTimeout Time to wait in milliseconds for a complete shutdown of the Java 11 HttpClientImpl.
         *                        If this is set to a value too low, you might need to wait elsewhere for the HttpClient
         *                        to shut down properly. Default is 3000ms.
         * @return {@link #self()}
         * @implNote Will be ignored if {@link #httpClientHandler} is set.
         */
        @Override
        public T setShutdownTimeout(long shutdownTimeout) {
            defaultHttpClientHandlerBuilder.setShutdownTimeout(shutdownTimeout);
            return self();
        }

        @Override
        public Boolean getAcceptAllCerts() {
            return defaultHttpClientHandlerBuilder.getAcceptAllCerts();
        }

        /**
         * Skip SSL certificate verification. Leave this unset to use the default in HttpClient. Setting this to true
         * installs a permissive SSLContext, setting it to false removes the SSLContext to use the default.
         *
         * @param acceptAllCerts the toggle
         * @return {@link #self()}
         * @implNote Will be ignored if {@link #httpClientHandler} is set.
         */
        @Override
        public T setAcceptAllCerts(Boolean acceptAllCerts) {
            defaultHttpClientHandlerBuilder.setAcceptAllCerts(acceptAllCerts);
            return self();
        }

        @Override
        public SSLContext getSslContext() {
            return defaultHttpClientHandlerBuilder.getSslContext();
        }

        /**
         * @param sslContext The specific SSLContext to use.
         * @return {@link #self()}
         * @see #setAcceptAllCerts(Boolean)
         * @implNote Will be ignored if {@link #httpClientHandler} is set.
         */
        @Override
        public T setSslContext(SSLContext sslContext) {
            defaultHttpClientHandlerBuilder.setSslContext(sslContext);
            return self();
        }

        @Override
        public SSLParameters getSslParameters() {
            return defaultHttpClientHandlerBuilder.getSslParameters();
        }

        /**
         * @param sslParameters The specific SSLParameters to use.
         * @return {@link #self()}
         * @implNote Will be ignored if {@link #httpClientHandler} is set.
         */
        @Override
        public T setSslParameters(SSLParameters sslParameters) {
            defaultHttpClientHandlerBuilder.setSslParameters(sslParameters);
            return self();
        }

        @Override
        public HttpClient getHttpClient() {
            return defaultHttpClientHandlerBuilder.getHttpClient();
        }

        /**
         * Instance of an externally configured http client. An internal HttpClient will be built with parameters
         * given by this Builder if this is not set.
         *
         * @param httpClient Instance of an HttpClient.
         * @return {@link #self()}
         * @implNote Be aware, that any httpClient given via this method will be marked as external and has to be
         *           closed externally as well. A call to {@link DefaultHttpClientHandler#close()} with an external httpclient
         *           will have no effect.
         * @implNote Will be ignored if {@link #httpClientHandler} is set.
         */
        @Override
        public T setHttpClient(HttpClient httpClient) {
            defaultHttpClientHandlerBuilder.setHttpClient(httpClient);
            return self();
        }

        @Override
        public CookieManager getCookieManager() {
            return defaultHttpClientHandlerBuilder.getCookieManager();
        }

        /**
         * Instance of an externally configured CookieManager. An internal CookieManager will be built if this is not
         * set.
         *
         * @param cookieManager Instance of a CookieManager.
         * @return {@link #self()}
         * @implNote Will be ignored if {@link #httpClientHandler} is set.
         */
        @Override
        public T setCookieManager(CookieManager cookieManager) {
            defaultHttpClientHandlerBuilder.setCookieManager(cookieManager);
            return self();
        }

        @Override
        public int getMaxConnectionPool() {
            return defaultHttpClientHandlerBuilder.getMaxConnectionPool();
        }

        /**
         * Set the maximum of open connections for this HttpClient (This sets the fixedThreadPool for the
         * Executor of the HttpClient).
         *
         * @param maxConnectionPool Maximum size of the pool. Default is 8.
         * @return {@link #self()}
         * @implNote Will be ignored if {@link #httpClientHandler} is set.
         */
        @Override
        public T setMaxConnectionPool(int maxConnectionPool) {
            defaultHttpClientHandlerBuilder.setMaxConnectionPool(maxConnectionPool);
            return self();
        }

        @Override
        public int getMaxBinaryLogLength() {
            return defaultHttpClientHandlerBuilder.getMaxBinaryLogLength();
        }

        /**
         * Maximum size to log binary data in logfiles. Default is 1024.
         *
         * @param maxBinaryLogLength Size in bytes
         * @return {@link #self()}
         * @implNote Will be ignored if {@link #httpClientHandler} is set.
         */
        @Override
        public T setMaxBinaryLogLength(int maxBinaryLogLength) {
            defaultHttpClientHandlerBuilder.setMaxBinaryLogLength(maxBinaryLogLength);
            return self();
        }

        @Override
        public Boolean getHttpClientAutoClose() {
            return defaultHttpClientHandlerBuilder.getHttpClientAutoClose();
        }

        /**
         * <p>
         * Close internal httpClient automatically, even when it has been set externally.
         * </p>
         * <p>
         * The default is to close the internal httpClient when it has been created internally and to
         * not close the internal httpClient when it has been set via {@link #setHttpClient(HttpClient)}
         * </p>
         *
         * @param httpClientAutoClose true: enable, false: disable.
         * @return {@link #self()}
         * @implNote Will be ignored if {@link #httpClientHandler} is set.
         */
        @Override
        public T setHttpClientAutoClose(boolean httpClientAutoClose) {
            defaultHttpClientHandlerBuilder.setHttpClientAutoClose(httpClientAutoClose);
            return self();
        }

        protected abstract T self();

        public abstract AbstractAPIHandler build();
    }

    // ###############################################################################################
    // ## Main part ##
    // ###############################################################################################

    public static final String title;
    public static final String version;

    static {
        version = AbstractAPIHandler.class.getPackage().getImplementationVersion();
        String t = AbstractAPIHandler.class.getPackage().getImplementationTitle();
        title = (t != null ? t : "hiro-client-java");
    }

    protected final URI rootApiURI;
    protected final URI webSocketURI;
    protected final String userAgent;
    protected final Long httpRequestTimeout;
    protected int maxRetries;

    /**
     * This is a reference which will be shared among all AbstractAPIHandler that use the same configuration
     * {@link Conf}.
     */
    protected HttpClientHandler httpClientHandler;

    /**
     * Store the original configuration, so it can be used in other APIHandlers which will use the same underlying
     * {@link #httpClientHandler}.
     */
    private final Conf<?> conf;

    /**
     * Constructor.
     *
     * @param builder The builder to use.
     * @implNote If the builder does not carry a httpClientHandler, a default will be created here.
     */
    protected AbstractAPIHandler(Conf<?> builder) {
        this.conf = builder;
        this.rootApiURI = notNull(builder.getRootApiURI(), "rootApiURI");
        this.webSocketURI = builder.getWebSocketURI();
        this.maxRetries = builder.getMaxRetries();
        this.httpRequestTimeout = builder.getHttpRequestTimeout();
        this.userAgent = builder.getUserAgent() != null ? builder.getUserAgent()
                : (version != null ? title + " " + version : title);

        this.httpClientHandler = builder.getHttpClientHandler() != null ? builder.getHttpClientHandler()
                : builder.defaultHttpClientHandlerBuilder.build();
    }

    /**
     * Return a copy of the configuration.
     *
     * @return A copy of the configuration.
     * @implNote Please take note, that the included httpClientHandler of this class will be
     *           added to the returned {@link Conf} and therefore will be shared among all APIHandlers that use this
     *           configuration.
     * @see co.arago.hiro.client.rest.AuthenticatedAPIHandler
     */
    public Conf<?> getConf() {
        conf.setHttpClientHandler(httpClientHandler);
        return conf;
    }

    public URI getRootApiURI() {
        return rootApiURI;
    }

    /**
     * Get the {@link #webSocketURI} or, if it is missing, construct one from {@link #rootApiURI}.
     *
     * @return The URI for the webSocket.
     */
    public URI getWebSocketURI() {
        try {
            return (webSocketURI != null ? webSocketURI
                    : new URI(RegExUtils.replaceFirst(getRootApiURI().toString(), "^httpclient", "ws")));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Cannot create webSocketURI from rootApiURI.", e);
        }
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Long getHttpRequestTimeout() {
        return httpRequestTimeout;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * Build a complete uri from the apiUrl and path. Appends a '/'.
     *
     * @param path The path to append to {@link #rootApiURI}.
     * @return The constructed URI
     */
    public URI buildApiURI(String path) {
        return buildURI(getRootApiURI(), path, true);
    }

    /**
     * Build a complete uri from the apiUrl and path. Does not append a '/'
     *
     * @param path The path to append to {@link #rootApiURI}.
     * @return The constructed URI
     */
    public URI buildEndpointURI(String path) {
        return buildURI(getRootApiURI(), path, false);
    }

    /**
     * Build a complete uri from the webSocketApi and path.
     *
     * @param path The path to append to {@link #webSocketURI}.
     * @return The constructed URI
     */
    public URI buildWebSocketURI(String path) {
        return buildURI(getWebSocketURI(), path, false);
    }

    /**
     * Build a complete uri from the url and path.
     *
     * @param uri        The uri to use as root.
     * @param path       The path to append to the url.
     * @param finalSlash Append a final slash?
     * @return The constructed URI
     */
    public static URI buildURI(URI uri, String path, boolean finalSlash) {
        return uri.resolve(RegExUtils.removePattern(path, "^/+") + (finalSlash ? "/" : ""));
    }

    /**
     * Add query and fragment to a URI - if any.
     *
     * @param uri      The URI for the query and fragment.
     * @param query    Map of query parameters to set. Can be null for no query parameters, otherwise uri must not have
     *                 a query already.
     * @param fragment URI Fragment. Can be null for no fragment, otherwise uri must not have a fragment already.
     * @return The constructed URI
     */
    public static URI addQueryFragmentAndNormalize(URI uri, URIEncodedData query, String fragment) {

        String sourceURI = uri.toASCIIString();

        if (query != null) {
            String encodedQueryString = query.toString();

            if (StringUtils.isNotBlank(encodedQueryString)) {
                if (sourceURI.contains("?")) {
                    throw new IllegalArgumentException("Given uri must not have a query part already.");
                }
                sourceURI += "?" + encodedQueryString;
            }
        }

        if (StringUtils.isNotBlank(fragment)) {
            if (sourceURI.contains("#")) {
                throw new IllegalArgumentException("Given uri must not have a fragment part already.");
            }
            sourceURI += "#" + URLPartEncoder.encodeNoPlus(fragment, StandardCharsets.UTF_8);
        }

        try {
            return new URI(sourceURI).normalize();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static void addQueryPart(StringBuilder builder, String key, String value) {
        builder
                .append(URLPartEncoder.encodeNoPlus(key, StandardCharsets.UTF_8))
                .append("=")
                .append(URLPartEncoder.encodeNoPlus(value, StandardCharsets.UTF_8));
    }

    /**
     * Create a HttpRequest.Builder with common options and headers.
     *
     * @param uri                The uri for the httpRequest.
     * @param headers            Initial headers for the httpRequest. Must NOT be null.
     * @param httpRequestTimeout The timeout in ms for the http request. If this is null, the
     *                           {@link #httpRequestTimeout} will be used.
     * @return The HttpRequest.Builder
     * @throws InterruptedException When call gets interrupted.
     * @throws IOException          When call has IO errors.
     * @throws HiroException        On Hiro protocol / handling errors.
     */
    public HttpRequest.Builder getRequestBuilder(
            URI uri,
            HttpHeaderMap headers,
            Long httpRequestTimeout) throws InterruptedException, IOException, HiroException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri);
        addToHeaders(headers);

        Long finalTimeout = (httpRequestTimeout != null ? httpRequestTimeout : this.httpRequestTimeout);

        if (finalTimeout != null)
            builder.timeout(Duration.ofMillis(finalTimeout));

        headers.addHeaders(builder);

        return builder;
    }

    // ###############################################################################################
    // ## Tool methods for sending and receiving ##
    // ###############################################################################################

    /**
     * Build a httpRequest from a {@link StreamContainer}.
     *
     * @param uri                The uri to use.
     * @param method             The method to use.
     * @param body               The body as {@link StreamContainer}. Can be null for methods that do not supply
     *                           a body.
     * @param headers            Initial headers for the httpRequest. Must not be null.
     * @param httpRequestTimeout The timeout in ms for the http request. If this is null, the
     *                           {@link #httpRequestTimeout} will be used.
     * @return The constructed HttpRequest.
     */
    private HttpRequest createStreamRequest(URI uri,
            String method,
            StreamContainer body,
            HttpHeaderMap headers,
            Long httpRequestTimeout) throws InterruptedException, IOException, HiroException {

        if (body != null && body.hasContentType()) {
            headers.set("Content-Type", body.getContentType());
        }

        HttpRequest httpRequest = getRequestBuilder(uri, headers, httpRequestTimeout)
                .method(method,
                        (body != null ? HttpRequest.BodyPublishers.ofInputStream(body::getInputStream)
                                : HttpRequest.BodyPublishers.noBody()))
                .build();

        getHttpLogger().logRequest(httpRequest, (body != null ? body.getInputStream() : null));

        return httpRequest;
    }

    /**
     * Build a httpRequest from a String.
     *
     * @param uri                The uri to use.
     * @param method             The method to use.
     * @param body               The body as String. Can be null for methods that do not supply a body.
     * @param headers            Initial headers for the httpRequest. Must not be null.
     * @param httpRequestTimeout The timeout in ms for the http request. If this is null, the
     *                           {@link #httpRequestTimeout} will be used.
     * @return The constructed HttpRequest.
     */
    private HttpRequest createStringRequest(URI uri,
            String method,
            String body,
            HttpHeaderMap headers,
            Long httpRequestTimeout) throws InterruptedException, IOException, HiroException {

        HttpRequest httpRequest = getRequestBuilder(uri, headers, httpRequestTimeout)
                .method(method,
                        (body != null ? HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)
                                : HttpRequest.BodyPublishers.noBody()))
                .build();

        getHttpLogger().logRequest(httpRequest, body);

        return httpRequest;
    }

    // ###############################################################################################
    // ## Synchronous sending and receiving ##
    // ###############################################################################################

    /**
     * Send a HttpRequest synchronously and return the httpResponse
     *
     * @param httpRequest The httpRequest to send
     * @param maxRetries  The amount of retries on errors. When this is null, {@link #maxRetries} will be used.
     * @return A HttpResponse containing an InputStream of the incoming body part of
     *         the result.
     * @throws HiroException        When status errors occur.
     * @throws IOException          On IO errors with the connection.
     * @throws InterruptedException When the call gets interrupted.
     */
    public HttpResponse<InputStream> send(
            HttpRequest httpRequest,
            Integer maxRetries) throws HiroException, IOException, InterruptedException {

        HttpResponse<InputStream> httpResponse = null;
        int retryCount = (maxRetries != null ? maxRetries : this.maxRetries);
        boolean retry = true;

        while (retry) {
            httpResponse = getOrBuildClient().send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            retry = checkResponse(httpResponse, retryCount);
            retryCount--;
        }

        return httpResponse;
    }

    // ###############################################################################################
    // ## Asynchronous sending and receiving ##
    // ###############################################################################################

    /**
     * <p>
     * Send a HttpRequest asynchronously and return a future for a {@link HttpResponseParser}.
     * </p>
     * <p>
     * Applies the internal {@link #checkResponse(HttpResponse, int)}.
     * The retry counter is irrelevant here. When the token expires, it will be detected and a new token will be
     * requested, but you need to handle a {@link CompletionException} and look for the cause {@link RetryException}
     * within it. If this is the cause, you need to retry the same HttpRequest again externally.
     * </p>
     * <p>
     * Also logs the response.
     * </p>
     *
     * @param httpRequest The httpRequest to send
     * @return A future for a {@link HttpResponseParser} containing the response.
     */
    public CompletableFuture<HttpResponseParser> sendAsync(HttpRequest httpRequest) {
        return getOrBuildClient()
                .sendAsync(httpRequest, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(response -> {
                    try {
                        if (checkResponse(response, 0))
                            throw new RetryException("Call needs a retry.");
                        return new HttpResponseParser(response, getHttpLogger());
                    } catch (HiroException | IOException | InterruptedException e) {
                        throw new CompletionException(e);
                    }
                });
    }

    // ###############################################################################################
    // ## Public Request Methods ##
    // ###############################################################################################

    private HttpHeaderMap startHeaders(String body, HttpHeaderMap headers) {
        return headers != null ? headers : new HttpHeaderMap();
    }

    private HttpHeaderMap startHeaders(StreamContainer bodyContainer, HttpHeaderMap headers) {
        HttpHeaderMap initialHeaders = (headers == null) ? new HttpHeaderMap() : headers;

        if (bodyContainer != null && bodyContainer.hasContentType())
            initialHeaders.set("Content-Type", bodyContainer.getContentType());

        return initialHeaders;
    }

    /**
     * Basic method which returns an object of type {@link HiroMessage} constructed via a JSON body httpResponse. Sets an appropriate Accept header.
     *
     * @param clazz              The object to create from the incoming JSON data.
     * @param uri                The uri to use.
     * @param method             The method to use.
     * @param body               The body as String. Can be null for methods that do not supply a body.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout in ms for the http request. If this is null, the
     *                           {@link #httpRequestTimeout} will be used.
     * @param maxRetries         The amount of retries on errors. When this is null, {@link #maxRetries} will be used.
     * @param <T>                Type of result object, derived of {@link HiroMessage}.
     * @return An object of clazz constructed from the JSON result or null if the response has no body.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroMessage> T executeWithStringBody(
            Class<T> clazz,
            URI uri,
            String method,
            String body,
            HttpHeaderMap headers,
            Long httpRequestTimeout,
            Integer maxRetries) throws HiroException, IOException, InterruptedException {

        HttpHeaderMap initialHeaders = startHeaders(body, headers);

        initialHeaders.set("Accept", "application/json");

        HttpRequest httpRequest = createStringRequest(
                uri,
                method,
                body,
                initialHeaders,
                httpRequestTimeout);

        HttpResponse<InputStream> httpResponse = send(httpRequest, maxRetries);

        return new HttpResponseParser(httpResponse, getHttpLogger()).createResponseObject(clazz);
    }

    /**
     * Basic method which returns an object of type {@link HiroMessage} constructed via a JSON body httpResponse. Sets an appropriate Accept header.
     *
     * @param clazz              The object to create from the incoming JSON data.
     * @param uri                The uri to use.
     * @param method             The method to use.
     * @param bodyContainer      The body as {@link StreamContainer}. Can be null for methods that do not supply a body.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout in ms for the http request. If this is null, the
     *                           {@link #httpRequestTimeout} will be used
     * @param maxRetries         The amount of retries on errors. When this is null, {@link #maxRetries} will be used.
     * @param <T>                Type of result object, derived of {@link HiroMessage}.
     * @return An object of clazz constructed from the JSON result or null if the response has no body.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroMessage> T executeWithStreamBody(
            Class<T> clazz,
            URI uri,
            String method,
            StreamContainer bodyContainer,
            HttpHeaderMap headers,
            Long httpRequestTimeout,
            Integer maxRetries) throws HiroException, IOException, InterruptedException {

        HttpHeaderMap initialHeaders = startHeaders(bodyContainer, headers);

        initialHeaders.set("Accept", "application/json");

        HttpRequest httpRequest = createStreamRequest(
                uri,
                method,
                bodyContainer,
                initialHeaders,
                httpRequestTimeout);

        HttpResponse<InputStream> httpResponse = send(httpRequest, maxRetries);

        return new HttpResponseParser(httpResponse, getHttpLogger()).createResponseObject(clazz);
    }

    /**
     * Method to communicate via InputStreams.
     *
     * @param uri                The uri to use.
     * @param method             The method to use.
     * @param bodyContainer      The body as {@link StreamContainer}. Can be null for methods that do not supply a body.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout in ms for the http request. If this is null, the
     *                           {@link #httpRequestTimeout} will be used.
     * @param maxRetries         The amount of retries on errors. When this is null, {@link #maxRetries} will be used.
     * @return A {@link HttpResponseParser} with the result body as InputStream and associated header information.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public HttpResponseParser executeBinary(
            URI uri,
            String method,
            StreamContainer bodyContainer,
            HttpHeaderMap headers,
            Long httpRequestTimeout,
            Integer maxRetries) throws HiroException, IOException, InterruptedException {

        HttpHeaderMap initialHeaders = startHeaders(bodyContainer, headers);

        HttpRequest httpRequest = createStreamRequest(uri, method, bodyContainer, initialHeaders, httpRequestTimeout);

        HttpResponse<InputStream> httpResponse = send(httpRequest, maxRetries);

        return new HttpResponseParser(httpResponse, getHttpLogger());
    }

    /**
     * Method to communicate asynchronously via String body.
     * This logs only the request, not the result.
     *
     * @param uri                The uri to use.
     * @param method             The method to use.
     * @param body               The body as String. Can be null for methods that do not supply a body.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout in ms for the http request. If this is null, the
     *                           {@link #httpRequestTimeout} will be used.
     * @return A future for the {@link HttpResponseParser}.
     * @throws HiroException        On errors with protocol handling.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public CompletableFuture<HttpResponseParser> executeAsyncWithStringBody(
            URI uri,
            String method,
            String body,
            HttpHeaderMap headers,
            Long httpRequestTimeout) throws InterruptedException, IOException, HiroException {
        HttpHeaderMap initialHeaders = startHeaders(body, headers);

        HttpRequest httpRequest = createStringRequest(uri, method, body, initialHeaders, httpRequestTimeout);

        return sendAsync(httpRequest);
    }

    /**
     * Method to communicate asynchronously via InputStreams.
     * This logs only the request, not the result.
     *
     * @param uri                The uri to use.
     * @param method             The method to use.
     * @param bodyContainer      The body as {@link StreamContainer}. Can be null for methods that do not supply a body.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout in ms for the http request. If this is null, the
     *                           {@link #httpRequestTimeout} will be used.
     * @return A future for the {@link HttpResponseParser}.
     * @throws HiroException        On errors with protocol handling.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public CompletableFuture<HttpResponseParser> executeAsyncWithStreamBody(
            URI uri,
            String method,
            StreamContainer bodyContainer,
            HttpHeaderMap headers,
            Long httpRequestTimeout) throws InterruptedException, IOException, HiroException {
        HttpHeaderMap initialHeaders = startHeaders(bodyContainer, headers);

        HttpRequest httpRequest = createStreamRequest(uri, method, bodyContainer, initialHeaders, httpRequestTimeout);

        return sendAsync(httpRequest);
    }

    // ###############################################################################################
    // ## Public Request Methods for common methods ##
    // ###############################################################################################

    /**
     * Basic GET method which returns a Map constructed via a JSON body
     * httpResponse.
     *
     * @param clazz              The object to create from the incoming JSON data.
     * @param uri                The uri to use.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout in ms for the http request. If this is null, the
     *                           {@link #httpRequestTimeout} will be used.
     * @param maxRetries         The amount of retries on errors. When this is null, {@link #maxRetries} will be used.
     * @param <T>                Type of result object, derived of {@link HiroMessage}.
     * @return A {@link HiroMessage} constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroMessage> T get(
            Class<T> clazz,
            URI uri,
            HttpHeaderMap headers,
            Long httpRequestTimeout,
            Integer maxRetries) throws HiroException, IOException, InterruptedException {

        return executeWithStringBody(clazz, uri, "GET", null, headers, httpRequestTimeout, maxRetries);
    }

    /**
     * Basic POST method which returns a Map constructed via a JSON body
     * httpResponse.
     *
     * @param clazz              The object to create from the incoming JSON data.
     * @param uri                The uri to use.
     * @param body               The body as String.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout in ms for the http request. If this is null, the
     *                           {@link #httpRequestTimeout} will be used.
     * @param maxRetries         The amount of retries on errors. When this is null, {@link #maxRetries} will be used.
     * @param <T>                Type of result object, derived of {@link HiroMessage}.
     * @return A {@link HiroMessage} constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroMessage> T post(
            Class<T> clazz,
            URI uri,
            String body,
            HttpHeaderMap headers,
            Long httpRequestTimeout,
            Integer maxRetries) throws HiroException, IOException, InterruptedException {

        return executeWithStringBody(clazz, uri, "POST", body, headers, httpRequestTimeout, maxRetries);
    }

    /**
     * Basic PUT method which returns a Map constructed via a JSON body
     * httpResponse.
     *
     * @param clazz              The object to create from the incoming JSON data.
     * @param uri                The uri to use.
     * @param body               The body as String.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout in ms for the http request. If this is null, the
     *                           {@link #httpRequestTimeout} will be used.
     * @param maxRetries         The amount of retries on errors. When this is null, {@link #maxRetries} will be used.
     * @param <T>                Type of result object, derived of {@link HiroMessage}.
     * @return A {@link HiroMessage} constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroMessage> T put(
            Class<T> clazz,
            URI uri,
            String body,
            HttpHeaderMap headers,
            Long httpRequestTimeout,
            Integer maxRetries) throws HiroException, IOException, InterruptedException {

        return executeWithStringBody(clazz, uri, "PUT", body, headers, httpRequestTimeout, maxRetries);
    }

    /**
     * Basic PATCH method which returns a Map constructed via a JSON body
     * httpResponse.
     *
     * @param clazz              The object to create from the incoming JSON data.
     * @param uri                The uri to use.
     * @param body               The body as String.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout in ms for the http request. If this is null, the
     *                           {@link #httpRequestTimeout} will be used.
     * @param maxRetries         The amount of retries on errors. When this is null, {@link #maxRetries} will be used.
     * @param <T>                Type of result object, derived of {@link HiroMessage}.
     * @return A {@link HiroMessage} constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroMessage> T patch(
            Class<T> clazz,
            URI uri,
            String body,
            HttpHeaderMap headers,
            Long httpRequestTimeout,
            Integer maxRetries) throws HiroException, IOException, InterruptedException {

        return executeWithStringBody(clazz, uri, "PATCH", body, headers, httpRequestTimeout, maxRetries);
    }

    /**
     * Basic DELETE method which returns a Map constructed via a JSON body
     * httpResponse.
     *
     * @param clazz              The object to create from the incoming JSON data.
     * @param uri                The uri to use.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout in ms for the http request. If this is null, the
     *                           {@link #httpRequestTimeout} will be used.
     * @param maxRetries         The amount of retries on errors. When this is null, {@link #maxRetries} will be used.
     * @param <T>                Type of result object, derived of {@link HiroMessage}.
     * @return A {@link HiroMessage} constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroMessage> T delete(
            Class<T> clazz,
            URI uri,
            HttpHeaderMap headers,
            Long httpRequestTimeout,
            Integer maxRetries) throws HiroException, IOException, InterruptedException {

        return executeWithStringBody(clazz, uri, "DELETE", null, headers, httpRequestTimeout, maxRetries);
    }

    /**
     * Basic GET method which returns an InputStream from the body of the httpResponse.
     *
     * @param uri                The uri to use.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout in ms for the http request. If this is null, the
     *                           {@link #httpRequestTimeout} will be used.
     * @param maxRetries         The amount of retries on errors. When this is null, {@link #maxRetries} will be used.
     * @return A {@link HttpResponseParser} with the result body as InputStream and associated header information.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public HttpResponseParser getBinary(
            URI uri,
            HttpHeaderMap headers,
            Long httpRequestTimeout,
            Integer maxRetries)
            throws HiroException, IOException, InterruptedException {

        return executeBinary(uri, "GET", null, headers, httpRequestTimeout, maxRetries);
    }

    /**
     * Basic POST method which sends an InputStream.
     *
     * @param clazz              The object to create from the incoming JSON data.
     * @param uri                The uri to use.
     * @param body               Body as {@link StreamContainer}.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout in ms for the http request. If this is null, the
     *                           {@link #httpRequestTimeout} will be used.
     * @param maxRetries         The amount of retries on errors. When this is null, {@link #maxRetries} will be used.
     * @param <T>                Type of result object, derived of {@link HiroMessage}.
     * @return A {@link HiroMessage} constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroMessage> T postBinary(
            Class<T> clazz,
            URI uri,
            StreamContainer body,
            HttpHeaderMap headers,
            Long httpRequestTimeout,
            Integer maxRetries) throws HiroException, IOException, InterruptedException {

        return executeWithStreamBody(clazz, uri, "POST", body, headers, httpRequestTimeout, maxRetries);
    }

    /**
     * Basic PUT method which sends an InputStream.
     *
     * @param clazz              The object to create from the incoming JSON data.
     * @param uri                The uri to use.
     * @param body               Body as {@link StreamContainer}.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout in ms for the http request. If this is null, the
     *                           {@link #httpRequestTimeout} will be used.
     * @param maxRetries         The amount of retries on errors. When this is null, {@link #maxRetries} will be used.
     * @param <T>                Type of result object, derived of {@link HiroMessage}.
     * @return A {@link HiroMessage} constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroMessage> T putBinary(
            Class<T> clazz,
            URI uri,
            StreamContainer body,
            HttpHeaderMap headers,
            Long httpRequestTimeout,
            Integer maxRetries) throws HiroException, IOException, InterruptedException {

        return executeWithStreamBody(clazz, uri, "PUT", body, headers, httpRequestTimeout, maxRetries);
    }

    /**
     * Basic PATCH method which sends an InputStream.
     *
     * @param clazz              The object to create from the incoming JSON data.
     * @param uri                The uri to use.
     * @param body               Body as {@link StreamContainer}.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout in ms for the http request. If this is null, the
     *                           {@link #httpRequestTimeout} will be used.
     * @param maxRetries         The amount of retries on errors. When this is null, {@link #maxRetries} will be used.
     * @param <T>                Type of result object, derived of {@link HiroMessage}.
     * @return A {@link HiroMessage} constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroMessage> T patchBinary(
            Class<T> clazz,
            URI uri,
            StreamContainer body,
            HttpHeaderMap headers,
            Long httpRequestTimeout,
            Integer maxRetries) throws HiroException, IOException, InterruptedException {

        return executeWithStreamBody(clazz, uri, "PATCH", body, headers, httpRequestTimeout, maxRetries);
    }

    // ###############################################################################################
    // ## Methods to override ##
    // ###############################################################################################

    /**
     * @return The HttpLogger to use with this class.
     */
    public HttpLogger getHttpLogger() {
        return httpClientHandler.getHttpLogger();
    }

    /**
     * @return The HttpClient to use with this class.
     */
    public HttpClient getOrBuildClient() {
        return httpClientHandler.getOrBuildClient();
    }

    /**
     * Override this to add authentication tokens.
     *
     * @param headers Map of headers with initial values.
     * @throws HiroException        On internal errors regarding hiro data processing.
     * @throws IOException          On IO errors.
     * @throws InterruptedException When a call (possibly of an overwritten method) gets interrupted.
     */
    abstract public void addToHeaders(HttpHeaderMap headers) throws InterruptedException, IOException, HiroException;

    /**
     * The default way to check responses for errors and extract error messages.
     *
     * @param httpResponse The httpResponse from the HttpRequest
     * @param retryCount   current counter for retries
     * @return true for a retry, false otherwise.
     * @throws TokenUnauthorizedException When the statusCode is 401.
     * @throws HiroHttpException          When the statusCode is &lt; 200 or &gt; 399.
     * @throws HiroException              On internal errors regarding hiro data processing.
     * @throws IOException                On IO errors.
     * @throws InterruptedException       When a call (possibly of an overwritten method) gets interrupted.
     */
    public boolean checkResponse(HttpResponse<InputStream> httpResponse, int retryCount)
            throws HiroException, IOException, InterruptedException {
        int statusCode = httpResponse.statusCode();

        if (statusCode < 200 || statusCode > 399) {
            HttpResponseParser responseParser = new HttpResponseParser(httpResponse, getHttpLogger());
            String body = responseParser.consumeResponseAsString();
            String message = statusCode + ": [no additional message]";
            int errorCode = statusCode;

            try {

                if (responseParser.contentIsJson()) {
                    HiroError hiroError = JsonUtil.DEFAULT.toObject(body, HiroError.class);
                    String hiroMessage = hiroError.getMessage();
                    if (StringUtils.isNotBlank(hiroMessage))
                        message = statusCode + ": " + hiroMessage;
                    errorCode = hiroError.getCode();
                }

                throw (errorCode == 401 ? new TokenUnauthorizedException(message, errorCode, null)
                        : new HiroHttpException(message, errorCode, null));
            } catch (IOException e) {
                throw new HiroHttpException(message, errorCode, body, e);
            }

        }

        return false;
    }

}
