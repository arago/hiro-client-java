package co.arago.hiro.client.connection;

import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.exceptions.HiroHttpException;
import co.arago.hiro.client.model.HiroErrorResponse;
import co.arago.hiro.client.model.HiroResponse;
import co.arago.hiro.client.util.HttpLogger;
import co.arago.hiro.client.util.JsonTools;
import co.arago.hiro.client.util.httpclient.HttpResponseContainer;
import co.arago.hiro.client.util.httpclient.StreamContainer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
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

    final Logger log = LoggerFactory.getLogger(AbstractAPIHandler.class);

    public interface GetterConf {
        String getApiUrl();

        Long getHttpRequestTimeout();

        int getMaxRetries();

        String getUserAgent();
    }

    public interface Conf extends GetterConf {
        /**
         * @param apiUrl The root url for the API
         * @return this
         */
        Conf setApiUrl(String apiUrl);

        /**
         * @param httpRequestTimeout Request timeout in ms.
         * @return this
         */
        Conf setHttpRequestTimeout(Long httpRequestTimeout);

        /**
         * @param maxRetries Max amount of retries when http errors are received.
         * @return this
         */
        Conf setMaxRetries(int maxRetries);

        /**
         * For header "User-Agent". Default is determined by the package.
         *
         * @param userAgent The line for the User-Agent header.
         * @return this
         */
        Conf setUserAgent(String userAgent);
    }

    public static final String title;
    public static final String version;

    static {
        version = AbstractClientAPIHandler.class.getPackage().getImplementationVersion();
        String t = AbstractClientAPIHandler.class.getPackage().getImplementationTitle();
        title = (t != null ? t : "java-hiro-client");
    }


    protected final String apiUrl;
    protected final String userAgent;
    protected final Long httpRequestTimeout;
    protected int maxRetries;

    protected AbstractAPIHandler(GetterConf builder) {
        this.apiUrl = (StringUtils.endsWith(builder.getApiUrl(), "/") ? builder.getApiUrl() : builder.getApiUrl() + "/");
        this.maxRetries = builder.getMaxRetries();
        this.httpRequestTimeout = builder.getHttpRequestTimeout();
        this.userAgent = (builder.getUserAgent() != null ? builder.getUserAgent() : (version != null ? title + " " + version : title));
    }

    public String getApiUrl() {
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
        return URI.create(apiUrl).resolve(
                StringUtils.startsWith(endpoint, "/") ? endpoint.substring(1) : endpoint
        );
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
    public URI addQueryAndFragment(URI uri, Map<String, String> query, String fragment) {

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
     * @param uri     The uri for the httpRequest.
     * @param headers Initial headers for the httpRequest. Must NOT be null.
     * @return The HttpRequest.Builder
     */
    public HttpRequest.Builder getRequestBuilder(URI uri, Map<String, String> headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri);
        addToHeaders(headers);

        if (httpRequestTimeout != null)
            builder.timeout(Duration.ofMillis(httpRequestTimeout));

        for (Map.Entry<String, String> headerEntry : headers.entrySet()) {
            builder.header(headerEntry.getKey(), headerEntry.getValue());
        }

        return builder;
    }

    /**
     * Decodes the error body from ta httpResponse.
     *
     * @param statusCode   The statusCode from the httpResponse.
     * @param hiroResponse The body from the httpResponse as HiroResponse. Can be null when
     *                     no httpResponse body was returned.
     * @return A string representing the error extracted from the body or from the
     * status code.
     */
    public String getErrorMessage(int statusCode, HiroResponse hiroResponse) {
        String reason = "HttpResponse code " + statusCode;

        if (hiroResponse != null) {
            HiroErrorResponse hiroErrorResponse = hiroResponse.getError();
            String message = (hiroErrorResponse != null ? hiroErrorResponse.getHiroErrorMessage() : null);
            if (StringUtils.isNotBlank(message)) {
                Integer errorCode = hiroErrorResponse.getHiroErrorCode();
                return (errorCode != null ? message + " (code: " + errorCode + ")" : message);
            }

        }

        return reason;
    }

    // ###############################################################################################
    // ## Tool methods for sending and receiving ##
    // ###############################################################################################

    /**
     * Build a httpRequest from a {@link StreamContainer}.
     *
     * @param uri     The uri to use.
     * @param method  The method to use.
     * @param body    The body as {@link StreamContainer}. Can be null for methods that do not supply
     *                a body.
     * @param headers Initial headers for the httpRequest. Must not be null.
     * @return The constructed HttpRequest.
     */
    private HttpRequest createStreamRequest(URI uri,
                                            String method,
                                            StreamContainer body,
                                            Map<String, String> headers) {

        if (body != null && body.contentType != null) {
            headers.put("Content-Type", body.contentType);
        }

        HttpRequest httpRequest = getRequestBuilder(uri, headers)
                .method(method, (body != null ?
                        HttpRequest.BodyPublishers.ofInputStream(() -> body.inputStream) :
                        HttpRequest.BodyPublishers.noBody()))
                .build();

        getHttpLogger().logRequest(httpRequest, (body != null ? body.inputStream : null));

        return httpRequest;
    }

    /**
     * Build a httpRequest from a String.
     *
     * @param uri     The uri to use.
     * @param method  The method to use.
     * @param body    The body as String. Can be null for methods that do not supply a body.
     * @param headers Initial headers for the httpRequest. Must not be null.
     * @return The constructed HttpRequest.
     */
    private HttpRequest createStringRequest(URI uri,
                                            String method,
                                            String body,
                                            Map<String, String> headers) {

        HttpRequest httpRequest = getRequestBuilder(uri, headers)
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
     * @return A HttpResponse containing an InputStream of the incoming body part of
     * the result.
     * @throws HiroException        When status errors occur.
     * @throws IOException          On IO errors with the connection.
     * @throws InterruptedException When the call gets interrupted.
     */
    public HttpResponse<InputStream> send(HttpRequest httpRequest)
            throws HiroException, IOException, InterruptedException {

        HttpResponse<InputStream> httpResponse = null;
        int retryCount = maxRetries;

        while (retryCount >= 0) {
            httpResponse = getOrBuildClient().send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            if (!checkResponse(httpResponse, retryCount))
                break;
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
     * @see #getFromAsyncResponse(CompletableFuture, Class)
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
     * @return A future for the {@link HttpResponseContainer}.
     */
    public CompletableFuture<HttpResponseContainer> getAsyncResponse(
            CompletableFuture<HttpResponse<InputStream>> asyncRequestFuture
    ) {
        return asyncRequestFuture
                .thenApply(response -> {
                    try {
                        checkResponse(response, 0);
                        return new HttpResponseContainer(response, getHttpLogger());
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

        if (bodyContainer != null && bodyContainer.contentType != null)
            initialHeaders.put("Content-Type", bodyContainer.contentType);

        return initialHeaders;
    }

    /**
     * Basic method which returns an object of type {@link HiroResponse} constructed via a JSON body httpResponse. Sets an appropriate Accept header.
     *
     * @param clazz   The object to create from the incoming JSON data.
     * @param uri     The uri to use.
     * @param method  The method to use.
     * @param body    The body as String. Can be null for methods that do not supply a body.
     * @param headers Initial headers for the httpRequest. Can be null for no additional headers.
     * @return An object of clazz constructed from the JSON result or null if the response has no body.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroResponse> T executeWithStringBody(
            Class<T> clazz,
            URI uri,
            String method,
            String body,
            Map<String, String> headers
    ) throws HiroException, IOException, InterruptedException {

        Map<String, String> initialHeaders = startHeaders(body, headers);

        initialHeaders.put("Accept", "application/json");

        HttpRequest httpRequest = createStringRequest(
                uri,
                method,
                body,
                initialHeaders);

        HttpResponse<InputStream> httpResponse = send(httpRequest);

        return new HttpResponseContainer(httpResponse, getHttpLogger()).createResponseObject(clazz);
    }

    /**
     * Basic method which returns an object of type {@link HiroResponse} constructed via a JSON body httpResponse. Sets an appropriate Accept header.
     *
     * @param clazz         The object to create from the incoming JSON data.
     * @param uri           The uri to use.
     * @param method        The method to use.
     * @param bodyContainer The body as {@link StreamContainer}. Can be null for methods that do not supply a body.
     * @param headers       Initial headers for the httpRequest. Can be null for no additional headers.
     * @return An object of clazz constructed from the JSON result or null if the response has no body.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroResponse> T executeWithStreamBody(
            Class<T> clazz,
            URI uri,
            String method,
            StreamContainer bodyContainer,
            Map<String, String> headers
    ) throws HiroException, IOException, InterruptedException {

        Map<String, String> initialHeaders = startHeaders(bodyContainer, headers);

        initialHeaders.put("Accept", "application/json");

        HttpRequest httpRequest = createStreamRequest(
                uri,
                method,
                bodyContainer,
                initialHeaders);

        HttpResponse<InputStream> httpResponse = send(httpRequest);

        return new HttpResponseContainer(httpResponse, getHttpLogger()).createResponseObject(clazz);
    }

    /**
     * Method to communicate via InputStreams.
     *
     * @param uri           The uri to use.
     * @param method        The method to use.
     * @param bodyContainer The body as {@link StreamContainer}. Can be null for methods that do not supply a body.
     * @param headers       Initial headers for the httpRequest. Can be null for no additional headers.
     * @return A {@link HttpResponseContainer} with the result body as InputStream and associated header information.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public HttpResponseContainer executeBinary(
            URI uri,
            String method,
            StreamContainer bodyContainer,
            Map<String, String> headers
    ) throws HiroException, IOException, InterruptedException {
        Map<String, String> initialHeaders = startHeaders(bodyContainer, headers);

        HttpRequest httpRequest = createStreamRequest(uri, method, bodyContainer, initialHeaders);

        HttpResponse<InputStream> httpResponse = send(httpRequest);

        return new HttpResponseContainer(httpResponse, getHttpLogger());
    }

    /**
     * Method to communicate asynchronously via String body.
     * This logs only the request, not the result.
     *
     * @param uri     The uri to use.
     * @param method  The method to use.
     * @param body    The body as String. Can be null for methods that do not supply a body.
     * @param headers Initial headers for the httpRequest. Can be null for no additional headers.
     * @return A future for the HttpResponse&lt;InputStream&gt;.
     */
    public CompletableFuture<HttpResponse<InputStream>> executeAsyncWithStringBody(
            URI uri,
            String method,
            String body,
            Map<String, String> headers
    ) {
        Map<String, String> initialHeaders = startHeaders(body, headers);

        HttpRequest httpRequest = createStringRequest(uri, method, body, initialHeaders);

        return sendAsync(httpRequest);
    }

    /**
     * Method to communicate asynchronously via InputStreams.
     * This logs only the request, not the result.
     *
     * @param uri           The uri to use.
     * @param method        The method to use.
     * @param bodyContainer The body as {@link StreamContainer}. Can be null for methods that do not supply a body.
     * @param headers       Initial headers for the httpRequest. Can be null for no additional headers.
     * @return A future for the HttpResponse&lt;InputStream&gt;.
     */
    public CompletableFuture<HttpResponse<InputStream>> executeAsyncWithStreamBody(
            URI uri,
            String method,
            StreamContainer bodyContainer,
            Map<String, String> headers
    ) {
        Map<String, String> initialHeaders = startHeaders(bodyContainer, headers);

        HttpRequest httpRequest = createStreamRequest(uri, method, bodyContainer, initialHeaders);

        return sendAsync(httpRequest);
    }

    // ###############################################################################################
    // ## Public Request Methods for common methods ##
    // ###############################################################################################

    /**
     * Basic GET method which returns a Map constructed via a JSON body
     * httpResponse.
     *
     * @param clazz   The object to create from the incoming JSON data.
     * @param uri     The uri to use.
     * @param headers Initial headers for the httpRequest. Can be null for no additional headers.
     * @return A map constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroResponse> T get(Class<T> clazz, URI uri, Map<String, String> headers)
            throws HiroException, IOException, InterruptedException {

        return executeWithStringBody(clazz, uri, "GET", null, headers);
    }

    /**
     * Basic POST method which returns a Map constructed via a JSON body
     * httpResponse.
     *
     * @param clazz   The object to create from the incoming JSON data.
     * @param uri     The uri to use.
     * @param body    The body as String.
     * @param headers Initial headers for the httpRequest. Can be null for no additional headers.
     * @return A map constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroResponse> T post(Class<T> clazz, URI uri, String body, Map<String, String> headers)
            throws HiroException, IOException, InterruptedException {

        return executeWithStringBody(clazz, uri, "POST", body, headers);
    }

    /**
     * Basic PUT method which returns a Map constructed via a JSON body
     * httpResponse.
     *
     * @param clazz   The object to create from the incoming JSON data.
     * @param uri     The uri to use.
     * @param body    The body as String.
     * @param headers Initial headers for the httpRequest. Can be null for no additional headers.
     * @return A map constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroResponse> T put(Class<T> clazz, URI uri, String body, Map<String, String> headers)
            throws HiroException, IOException, InterruptedException {

        return executeWithStringBody(clazz, uri, "PUT", body, headers);
    }

    /**
     * Basic PATCH method which returns a Map constructed via a JSON body
     * httpResponse.
     *
     * @param clazz   The object to create from the incoming JSON data.
     * @param uri     The uri to use.
     * @param body    The body as String.
     * @param headers Initial headers for the httpRequest. Can be null for no additional headers.
     * @return A map constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroResponse> T patch(Class<T> clazz, URI uri, String body, Map<String, String> headers)
            throws HiroException, IOException, InterruptedException {

        return executeWithStringBody(clazz, uri, "PATCH", body, headers);
    }

    /**
     * Basic DELETE method which returns a Map constructed via a JSON body
     * httpResponse.
     *
     * @param clazz   The object to create from the incoming JSON data.
     * @param uri     The uri to use.
     * @param headers Initial headers for the httpRequest. Can be null for no additional headers.
     * @return A map constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroResponse> T delete(Class<T> clazz, URI uri, Map<String, String> headers)
            throws HiroException, IOException, InterruptedException {

        return executeWithStringBody(clazz, uri, "DELETE", null, headers);
    }

    /**
     * Basic GET method which returns an InputStream from the body of the httpResponse.
     *
     * @param uri     The uri to use.
     * @param headers Initial headers for the httpRequest. Can be null for no additional headers.
     * @return A {@link HttpResponseContainer} with the result body as InputStream and associated header information.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public HttpResponseContainer getBinary(URI uri, Map<String, String> headers)
            throws HiroException, IOException, InterruptedException {

        return executeBinary(uri, "GET", null, headers);
    }

    /**
     * Basic POST method which sends an InputStream.
     *
     * @param clazz   The object to create from the incoming JSON data.
     * @param uri     The uri to use.
     * @param body    Body as {@link StreamContainer}.
     * @param headers Initial headers for the httpRequest. Can be null for no additional headers.
     * @return A map constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroResponse> T postBinary(Class<T> clazz, URI uri, StreamContainer body, Map<String, String> headers)
            throws HiroException, IOException, InterruptedException {

        return executeWithStreamBody(clazz, uri, "POST", body, headers);
    }

    /**
     * Basic PUT method which sends an InputStream.
     *
     * @param clazz   The object to create from the incoming JSON data.
     * @param uri     The uri to use.
     * @param body    Body as {@link StreamContainer}.
     * @param headers Initial headers for the httpRequest. Can be null for no additional headers.
     * @return A map constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroResponse> T putBinary(Class<T> clazz, URI uri, StreamContainer body, Map<String, String> headers)
            throws HiroException, IOException, InterruptedException {

        return executeWithStreamBody(clazz, uri, "PUT", body, headers);
    }

    /**
     * Basic PATCH method which sends an InputStream.
     *
     * @param clazz   The object to create from the incoming JSON data.
     * @param uri     The uri to use.
     * @param body    Body as {@link StreamContainer}.
     * @param headers Initial headers for the httpRequest. Can be null for no additional headers.
     * @return A map constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroResponse> T patchBinary(Class<T> clazz, URI uri, StreamContainer body, Map<String, String> headers)
            throws HiroException, IOException, InterruptedException {

        return executeWithStreamBody(clazz, uri, "PATCH", body, headers);
    }

    // ###############################################################################################
    // ## Methods to override ##
    // ###############################################################################################

    /**
     * Abstract class that needs to be overwritten by a supplier of a HttpLogger.
     *
     * @return The HttpLogger to use with this class.
     */
    abstract public HttpLogger getHttpLogger();

    /**
     * Abstract class that needs to be overwritten by a supplier of a HttpClient.
     *
     * @return The HttpClient to use with this class.
     */
    abstract public HttpClient getOrBuildClient();

    /**
     * Override this to add authentication tokens.
     *
     * @param headers Map of headers with initial values.
     */
    abstract public void addToHeaders(Map<String, String> headers);

    /**
     * The default way to check responses for errors and extract error messages. Override this to add automatic token
     * renewal if necessary.
     *
     * @param httpResponse The httpResponse from the HttpRequest
     * @param retryCount   current counter for retries
     * @return true for a retry, false otherwise.
     * @throws HiroException if the check fails.
     */
    public boolean checkResponse(HttpResponse<InputStream> httpResponse, int retryCount) throws HiroException, IOException, InterruptedException {
        int statusCode = httpResponse.statusCode();

        if (statusCode < 200 || statusCode > 399) {
            String body;
            try {
                HttpResponseContainer responseContainer = new HttpResponseContainer(httpResponse, getHttpLogger());

                body = responseContainer.consumeResponseAsString();

                String message;

                if (responseContainer.contentIsJson()) {
                    HiroResponse response = JsonTools.DEFAULT.toObject(body, HiroResponse.class);
                    message = getErrorMessage(statusCode, response);
                } else {
                    message = "Response has error";
                }

                throw new HiroHttpException(message, statusCode, body);
            } catch (IOException e) {
                throw new HiroHttpException(getErrorMessage(statusCode, null), statusCode, null, e);
            }
        }

        return false;
    }

}
