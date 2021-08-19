package co.arago.hiro.client.connection;

import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.exceptions.HiroHttpException;
import co.arago.hiro.client.exceptions.TokenUnauthorizedException;
import co.arago.hiro.client.model.HiroError;
import co.arago.hiro.client.model.HiroMessage;
import co.arago.hiro.client.util.HttpLogger;
import co.arago.hiro.client.util.JsonTools;
import co.arago.hiro.client.util.httpclient.HttpResponseParser;
import co.arago.hiro.client.util.httpclient.StreamContainer;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Root class with fields and tool methods for all API Handlers
 */
public abstract class AbstractAPIHandler {

    final static Logger log = LoggerFactory.getLogger(AbstractAPIHandler.class);

    // ###############################################################################################
    // ## Conf and Builder ##
    // ###############################################################################################

    public interface GetterConf {
        URL getApiUrl();

        Long getHttpRequestTimeout();

        int getMaxRetries();

        String getUserAgent();
    }

    /**
     * The basic configuration for all APIHAndler
     *
     * @param <T> The type of the Builder
     */
    public static abstract class Conf<T extends Conf<T>> implements GetterConf {
        private URL apiUrl;
        private String userAgent;
        private Long httpRequestTimeout;
        private int maxRetries;

        @Override
        public URL getApiUrl() {
            return apiUrl;
        }

        /**
         * @param apiUrl The root url for the API
         * @return {@link #self()}
         */
        public T setApiUrl(String apiUrl) throws MalformedURLException {
            this.apiUrl = new URL(RegExUtils.removePattern(apiUrl, "/+$") + "/");
            return self();
        }

        /**
         * @param apiUrl The root url for the API
         * @return {@link #self()}
         */
        public T setApiUrl(URL apiUrl) {
            this.apiUrl = apiUrl;
            return self();
        }

        @Override
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

        @Override
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

        @Override
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

        protected abstract T self();

        public abstract AbstractAPIHandler build();
    }

    // ###############################################################################################
    // ## Main part ##
    // ###############################################################################################

    public static final String title;
    public static final String version;

    static {
        version = AbstractClientAPIHandler.class.getPackage().getImplementationVersion();
        String t = AbstractClientAPIHandler.class.getPackage().getImplementationTitle();
        title = (t != null ? t : "java-hiro-client");
    }


    protected final URL apiUrl;
    protected final String userAgent;
    protected final Long httpRequestTimeout;
    protected int maxRetries;

    protected AbstractAPIHandler(GetterConf builder) {
        this.apiUrl = builder.getApiUrl();
        this.maxRetries = builder.getMaxRetries();
        this.httpRequestTimeout = builder.getHttpRequestTimeout();
        this.userAgent = (builder.getUserAgent() != null ? builder.getUserAgent() : (version != null ? title + " " + version : title));
    }

    public URL getApiUrl() {
        return apiUrl;
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
     * Build a complete uri from the apiUrl and endpoint.
     *
     * @param endpoint The endpoint to append to {@link #apiUrl}.
     * @return The constructed URI
     */
    public URI buildURI(String endpoint) {
        try {
            return apiUrl.toURI().resolve(RegExUtils.removePattern(endpoint, "^/+"));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Cannot create URI using endpoint '" + endpoint + "'", e);
        }
    }

    /**
     * Build a complete uri from the apiUrl and endpoint, optional query parameters
     * and an optional fragment
     *
     * @param endpoint The endpoint to append to {@link #apiUrl}.
     * @param query    Map of query parameters to set. Can be null for no query parameters.
     * @param fragment URI Fragment. Can be null for no fragment.
     * @return The constructed URI
     */
    public URI buildURI(String endpoint, Map<String, String> query, String fragment) {
        return addQueryAndFragment(buildURI(endpoint), query, fragment);
    }

    /**
     * Add query and fragment to a URI - if any.
     *
     * @param uri      The URI for the query and fragment.
     * @param query    Map of query parameters to set. Can be null for no query parameters.
     * @param fragment URI Fragment. Can be null for no fragment.
     * @return The constructed URI
     */
    public static URI addQueryAndFragment(URI uri, Map<String, String> query, String fragment) {

        String queryString = null;

        if (query != null) {
            StringBuilder queryStringBuilder = null;
            for (Map.Entry<String, String> entry : query.entrySet()) {
                if (queryStringBuilder == null) {
                    queryStringBuilder = new StringBuilder();
                } else {
                    queryStringBuilder.append("&");
                }
                queryStringBuilder
                        .append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }
            if (queryStringBuilder != null)
                queryString = queryStringBuilder.toString();
        }

        try {
            return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), queryString, fragment);
        } catch (URISyntaxException e) {
            return uri;
        }
    }

    /**
     * Create a HttpRequest.Builder with common options and headers.
     *
     * @param uri                The uri for the httpRequest.
     * @param headers            Initial headers for the httpRequest. Must NOT be null.
     * @param httpRequestTimeout The timeout in ms for the http request. If this is null, the
     *                           {@link #httpRequestTimeout} will be used.
     * @return The HttpRequest.Builder
     */
    public HttpRequest.Builder getRequestBuilder(
            URI uri,
            Map<String, String> headers,
            Long httpRequestTimeout
    ) throws InterruptedException, IOException, HiroException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri);
        addToHeaders(headers);

        Long finalTimeout = (httpRequestTimeout != null ? httpRequestTimeout : this.httpRequestTimeout);

        if (finalTimeout != null)
            builder.timeout(Duration.ofMillis(finalTimeout));

        for (Map.Entry<String, String> headerEntry : headers.entrySet()) {
            builder.header(headerEntry.getKey(), headerEntry.getValue());
        }

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
                                            Map<String, String> headers,
                                            Long httpRequestTimeout
    ) throws InterruptedException, IOException, HiroException {

        if (body != null && body.hasContentType()) {
            headers.put("Content-Type", body.getContentType());
        }

        HttpRequest httpRequest = getRequestBuilder(uri, headers, httpRequestTimeout)
                .method(method, (body != null ?
                        HttpRequest.BodyPublishers.ofInputStream(body::getInputStream) :
                        HttpRequest.BodyPublishers.noBody()))
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
                                            Map<String, String> headers,
                                            Long httpRequestTimeout
    ) throws InterruptedException, IOException, HiroException {

        HttpRequest httpRequest = getRequestBuilder(uri, headers, httpRequestTimeout)
                .method(method, (body != null ?
                        HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8) :
                        HttpRequest.BodyPublishers.noBody()))
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
     * the result.
     * @throws HiroException        When status errors occur.
     * @throws IOException          On IO errors with the connection.
     * @throws InterruptedException When the call gets interrupted.
     */
    public HttpResponse<InputStream> send(
            HttpRequest httpRequest,
            Integer maxRetries
    ) throws HiroException, IOException, InterruptedException {

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
     * Send a HttpRequest asynchronously and return a future for httpResponse
     *
     * @param httpRequest The httpRequest to send
     * @return A future for the HttpResponse&lt;InputStream&gt;.
     * @see #getAsyncResponse(CompletableFuture)
     */
    public CompletableFuture<HttpResponse<InputStream>> sendAsync(HttpRequest httpRequest) {
        return getOrBuildClient().sendAsync(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
    }

    /**
     * Check the future for the incoming response and appy the internal {@link #checkResponse(HttpResponse, int)}.
     * The retry counter is irrelevant here. When the token expires, you need to handle this
     * {@link co.arago.hiro.client.exceptions.TokenUnauthorizedException} externally by trying the same HttpRequest
     * again.
     * Also logs the response.
     *
     * @param asyncRequestFuture The future from {@link #sendAsync(HttpRequest)}
     * @return A future for the {@link HttpResponseParser}.
     */
    public CompletableFuture<HttpResponseParser> getAsyncResponse(
            CompletableFuture<HttpResponse<InputStream>> asyncRequestFuture
    ) {
        return asyncRequestFuture
                .thenApply(response -> {
                    try {
                        checkResponse(response, 0);
                        return new HttpResponseParser(response, getHttpLogger());
                    } catch (HiroException | IOException | InterruptedException e) {
                        throw new CompletionException(e);
                    }
                });
    }

    // ###############################################################################################
    // ## Public Request Methods ##
    // ###############################################################################################

    private Map<String, String> startHeaders(String body, Map<String, String> headers) {
        Map<String, String> initialHeaders = new HashMap<>();

        if (headers != null)
            initialHeaders.putAll(headers);

        return initialHeaders;
    }

    private Map<String, String> startHeaders(StreamContainer bodyContainer, Map<String, String> headers) {
        Map<String, String> initialHeaders = new HashMap<>();

        if (headers != null)
            initialHeaders.putAll(headers);

        if (bodyContainer != null && bodyContainer.hasContentType())
            initialHeaders.put("Content-Type", bodyContainer.getContentType());

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
            Map<String, String> headers,
            Long httpRequestTimeout,
            Integer maxRetries
    ) throws HiroException, IOException, InterruptedException {

        Map<String, String> initialHeaders = startHeaders(body, headers);

        initialHeaders.put("Accept", "application/json");

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
     *                           {@link #httpRequestTimeout} will be used.
     * @param maxRetries         The amount of retries on errors. When this is null, {@link #maxRetries} will be used.
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
            Map<String, String> headers,
            Long httpRequestTimeout,
            Integer maxRetries
    ) throws HiroException, IOException, InterruptedException {

        Map<String, String> initialHeaders = startHeaders(bodyContainer, headers);

        initialHeaders.put("Accept", "application/json");

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
            Map<String, String> headers,
            Long httpRequestTimeout,
            Integer maxRetries
    ) throws HiroException, IOException, InterruptedException {
        Map<String, String> initialHeaders = startHeaders(bodyContainer, headers);

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
     * @return A future for the HttpResponse&lt;InputStream&gt;.
     */
    public CompletableFuture<HttpResponse<InputStream>> executeAsyncWithStringBody(
            URI uri,
            String method,
            String body,
            Map<String, String> headers,
            Long httpRequestTimeout
    ) throws InterruptedException, IOException, HiroException {
        Map<String, String> initialHeaders = startHeaders(body, headers);

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
     * @return A future for the HttpResponse&lt;InputStream&gt;.
     */
    public CompletableFuture<HttpResponse<InputStream>> executeAsyncWithStreamBody(
            URI uri,
            String method,
            StreamContainer bodyContainer,
            Map<String, String> headers,
            Long httpRequestTimeout
    ) throws InterruptedException, IOException, HiroException {
        Map<String, String> initialHeaders = startHeaders(bodyContainer, headers);

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
     * @return A {@link HiroMessage} constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroMessage> T get(
            Class<T> clazz,
            URI uri,
            Map<String, String> headers,
            Long httpRequestTimeout,
            Integer maxRetries
    ) throws HiroException, IOException, InterruptedException {

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
     * @return A {@link HiroMessage} constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroMessage> T post(
            Class<T> clazz,
            URI uri,
            String body,
            Map<String, String> headers,
            Long httpRequestTimeout,
            Integer maxRetries
    ) throws HiroException, IOException, InterruptedException {

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
     * @return A {@link HiroMessage} constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroMessage> T put(
            Class<T> clazz,
            URI uri,
            String body,
            Map<String, String> headers,
            Long httpRequestTimeout,
            Integer maxRetries
    ) throws HiroException, IOException, InterruptedException {

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
     * @return A {@link HiroMessage} constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroMessage> T patch(
            Class<T> clazz,
            URI uri,
            String body,
            Map<String, String> headers,
            Long httpRequestTimeout,
            Integer maxRetries
    ) throws HiroException, IOException, InterruptedException {

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
     * @return A {@link HiroMessage} constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroMessage> T delete(
            Class<T> clazz,
            URI uri,
            Map<String, String> headers,
            Long httpRequestTimeout,
            Integer maxRetries
    ) throws HiroException, IOException, InterruptedException {

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
            Map<String, String> headers,
            Long httpRequestTimeout,
            Integer maxRetries
    )
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
     * @return A {@link HiroMessage} constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroMessage> T postBinary(
            Class<T> clazz,
            URI uri,
            StreamContainer body,
            Map<String, String> headers,
            Long httpRequestTimeout,
            Integer maxRetries
    ) throws HiroException, IOException, InterruptedException {

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
     * @return A {@link HiroMessage} constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroMessage> T putBinary(
            Class<T> clazz,
            URI uri,
            StreamContainer body,
            Map<String, String> headers,
            Long httpRequestTimeout,
            Integer maxRetries
    ) throws HiroException, IOException, InterruptedException {

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
     * @return A {@link HiroMessage} constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroMessage> T patchBinary(
            Class<T> clazz,
            URI uri,
            StreamContainer body,
            Map<String, String> headers,
            Long httpRequestTimeout,
            Integer maxRetries
    ) throws HiroException, IOException, InterruptedException {

        return executeWithStreamBody(clazz, uri, "PATCH", body, headers, httpRequestTimeout, maxRetries);
    }

    // ###############################################################################################
    // ## Methods to override ##
    // ###############################################################################################

    /**
     * Abstract class that needs to be overwritten by a supplier of a HttpLogger.
     *
     * @return The HttpLogger to use with this class.
     */
    abstract protected HttpLogger getHttpLogger();

    /**
     * Abstract class that needs to be overwritten by a supplier of a HttpClient.
     *
     * @return The HttpClient to use with this class.
     */
    abstract protected HttpClient getOrBuildClient();

    /**
     * Override this to add authentication tokens.
     *
     * @param headers Map of headers with initial values.
     */
    abstract public void addToHeaders(Map<String, String> headers) throws InterruptedException, IOException, HiroException;

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
    public boolean checkResponse(HttpResponse<InputStream> httpResponse, int retryCount) throws HiroException, IOException, InterruptedException {
        int statusCode = httpResponse.statusCode();

        if (statusCode < 200 || statusCode > 399) {
            HttpResponseParser responseParser = new HttpResponseParser(httpResponse, getHttpLogger());
            String body = responseParser.consumeResponseAsString();
            String message = statusCode + ": [no additional message]";
            int errorCode = statusCode;

            try {

                if (responseParser.contentIsJson()) {
                    HiroError hiroError = JsonTools.DEFAULT.toObject(body, HiroError.class);
                    String hiroMessage = hiroError.getMessage();
                    if (StringUtils.isNotBlank(hiroMessage))
                        message = statusCode + ": " + hiroMessage;
                    errorCode = hiroError.getCode();
                }

                throw (errorCode == 401 ?
                        new TokenUnauthorizedException(message, errorCode, null) :
                        new HiroHttpException(message, errorCode, null));
            } catch (IOException e) {
                throw new HiroHttpException(message, errorCode, body, e);
            }


        }

        return false;
    }

}
