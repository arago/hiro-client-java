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

import java.io.IOException;
import java.io.InputStream;
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
     * The basic configuration for all APIHAndler
     *
     * @param <T> The type of the Builder
     */
    public static abstract class Conf<T extends Conf<T>> {
        private URI rootApiURI;
        private URI webSocketURI;
        private String userAgent;
        private Long httpRequestTimeout;
        private int maxRetries;
        private HttpClientHandler httpClientHandler;

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
         * Sets the httpClientHandler for the backend connection. This handler will be shared among all
         * APIHandlers that use this configuration instance.
         *
         * @param httpClientHandler The connection handler to use.
         * @return {@link #self()}
         */
        public T setHttpClientHandler(HttpClientHandler httpClientHandler) {
            this.httpClientHandler = httpClientHandler;
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
        version = DefaultHttpClientHandler.class.getPackage().getImplementationVersion();
        String t = DefaultHttpClientHandler.class.getPackage().getImplementationTitle();
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
                : DefaultHttpClientHandler.newBuilder().build();
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
