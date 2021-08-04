package co.arago.hiro.client.connection;

import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.exceptions.HiroHttpException;
import co.arago.hiro.client.model.HiroErrorResponse;
import co.arago.hiro.client.model.HiroResponse;
import co.arago.hiro.client.model.HiroStreamContainer;
import co.arago.hiro.client.util.HttpLogger;
import co.arago.hiro.client.util.JsonTools;
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
import java.util.List;
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

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
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
     * This creates the headerMap, set the User-Agent header and adds what is given in parameter 'headers'.
     *
     * @param headers Initial headers. Can be null to use default headers only.
     * @return Map of basic headers
     */
    public Map<String, String> initializeHeaders(Map<String, String> headers) {
        Map<String, String> headerMap = new HashMap<>(Map.of("User-Agent", userAgent));

        if (headers != null)
            headerMap.putAll(headers);

        return headerMap;
    }

    /**
     * Add an entry to the header map. A new map will be created. The contents of headers will be copied into it if it is not null.
     *
     * @param headers The initial header map.
     * @param key     The name of the Header.
     * @param value   The value for the Header.
     * @return The updated header map.
     */
    protected Map<String, String> addHeader(Map<String, String> headers, String key, String value) {
        Map<String, String> finalHeaders = new HashMap<>();
        if (headers != null) {
            finalHeaders.putAll(headers);
        }
        finalHeaders.put(key, value);
        return finalHeaders;
    }

    /**
     * Create a HttpRequest.Builder with common options and headers.
     *
     * @param uri                The uri for the httpRequest.
     * @param headers            Initial headers for the httpRequest.
     * @param httpRequestTimeout The timeout for the response. Set this to null to use the internal default.
     * @return The HttpRequest.Builder
     */
    public HttpRequest.Builder getRequestBuilder(URI uri, Map<String, String> headers, Long httpRequestTimeout) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri);
        Map<String, String> allHeaders = getHeaders(headers);

        Long finalTimeout = (httpRequestTimeout == null ? this.httpRequestTimeout : httpRequestTimeout);
        if (finalTimeout != null)
            builder.timeout(Duration.ofMillis(finalTimeout));

        for (Map.Entry<String, String> headerEntry : allHeaders.entrySet()) {
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

    /**
     * Get header content from a httpResponse. If multiple values are present, a CSV from all values is created.
     *
     * @param httpResponse The response with headers.
     * @param headerName   The name of the header field to read.
     * @return The value of the header field or null if no such header exists.
     */
    public String getFromHeader(HttpResponse<?> httpResponse, String headerName) {
        if (httpResponse == null)
            return null;

        List<String> values = httpResponse.headers().map().get(headerName);
        return String.join(",", values);
    }

    // ###############################################################################################
    // ## Tool methods for sending and receiving ##
    // ###############################################################################################

    /**
     * Build a httpRequest from a {@link HiroStreamContainer}.
     *
     * @param uri                The uri to use.
     * @param method             The method to use.
     * @param body               The body as {@link HiroStreamContainer}. Can be null for methods that do not supply
     *                           a body.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout for the response. Set this to null to use the internal default.
     * @return The constructed HttpRequest.
     */
    private HttpRequest createStreamRequest(URI uri,
                                            String method,
                                            HiroStreamContainer body,
                                            Map<String, String> headers,
                                            Long httpRequestTimeout) {

        Map<String, String> finalHeaders = new HashMap<>();

        if (headers != null) {
            finalHeaders.putAll(headers);
        }

        if (body != null) {
            if (body.contentType != null) {
                finalHeaders.put("Content-Type", body.contentType);
            }
            if (body.contentLength != null) {
                finalHeaders.put("Content-Length", body.contentLength.toString());
            }
        }

        return getRequestBuilder(uri, finalHeaders, httpRequestTimeout)
                .method(method, (body != null ?
                        HttpRequest.BodyPublishers.ofInputStream(() -> body.inputStream) :
                        HttpRequest.BodyPublishers.noBody()))
                .build();
    }

    /**
     * Build a httpRequest from a String.
     *
     * @param uri                The uri to use.
     * @param method             The method to use.
     * @param body               The body as String. Can be null for methods that do not supply a body.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout for the response. Set this to null to use the internal default.
     * @return The constructed HttpRequest.
     */
    private HttpRequest createStringRequest(URI uri,
                                            String method,
                                            String body,
                                            Map<String, String> headers,
                                            Long httpRequestTimeout) {

        return getRequestBuilder(uri, headers, httpRequestTimeout)
                .method(method, (body != null ?
                        HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8) :
                        HttpRequest.BodyPublishers.noBody()))
                .build();
    }

    /**
     * Logs the httpResponse, checks for the correct contentType and casts the httpResponse InputStream to the object
     * of class "clazz"
     *
     * @param httpResponse The incoming HttpResponse
     * @param clazz        The type of object you want to create from the incoming InputStream.
     * @param <T>          Typecast
     * @return The object of type "clazz" or null if there is no response body or the body is blank.
     * @throws IOException   When the JSON has errors.
     * @throws HiroException When the contentType  of the httpResponse is not "application/json".
     */
    private <T extends HiroResponse> T handleAndCastResponseData(HttpResponse<InputStream> httpResponse, Class<T> clazz) throws IOException, HiroException {
        HiroStreamContainer streamContainer = new HiroStreamContainer(httpResponse);

        String responseBody = streamContainer.getBodyAsString();

        getHttpLogger().logResponse(httpResponse, responseBody);

        if (StringUtils.isBlank(responseBody))
            return null;

        if (!streamContainer.contentIsJson())
            throw new HiroException("Incoming data is not of type 'application/json'");

        return JsonTools.DEFAULT.toObject(responseBody, clazz);
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
     * @see #checkAsyncResponse(CompletableFuture)
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
     * @return A future for the HttpResponse&lt;InputStream&gt;.
     */
    public CompletableFuture<HttpResponse<InputStream>> checkAsyncResponse(
            CompletableFuture<HttpResponse<InputStream>> asyncRequestFuture
    ) {
        return asyncRequestFuture
                .thenApply(response -> {
                    try {
                        checkResponse(response, 0);
                        getHttpLogger().logResponse(response, response.body());
                    } catch (HiroException | IOException | InterruptedException e) {
                        throw new CompletionException(e);
                    }
                    return response;
                });
    }

    /**
     * Check the future for the incoming response and apply the internal {@link #checkResponse(HttpResponse, int)},
     * then convert the body to an object of Class&lt;T&gt;.
     * The retry counter is irrelevant here. When the token expires, you need to handle this
     * {@link co.arago.hiro.client.exceptions.TokenUnauthorizedException} externally by trying the same HttpRequest
     * again.
     * Also logs the response.
     *
     * @param asyncRequestFuture The future from {@link #sendAsync(HttpRequest)}
     * @return A future for the T.
     */
    public <T extends HiroResponse> CompletableFuture<T> getFromAsyncResponse(
            CompletableFuture<HttpResponse<InputStream>> asyncRequestFuture,
            final Class<T> clazz
    ) {
        return asyncRequestFuture
                .thenApply(httpResponse -> {
                    try {
                        checkResponse(httpResponse, 0);

                        return handleAndCastResponseData(httpResponse, clazz);
                    } catch (HiroException | IOException | InterruptedException e) {
                        throw new CompletionException(e);
                    }
                });
    }

    // ###############################################################################################
    // ## Public Request Methods ##
    // ###############################################################################################

    /**
     * Basic method which returns an object of type {@link HiroResponse} constructed via a JSON body httpResponse. Sets an appropriate Accept header.
     *
     * @param clazz              The class to create from the incoming JSON data.
     * @param uri                The uri to use.
     * @param method             The method to use.
     * @param body               The body as String. Can be null for methods that do not supply a body.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout for the response. Set this to null to use the internal default.
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
            Map<String, String> headers,
            Long httpRequestTimeout
    ) throws HiroException, IOException, InterruptedException {

        HttpRequest httpRequest = createStringRequest(
                uri,
                method,
                body,
                addHeader(headers, "Accept", "application/json"),
                httpRequestTimeout);

        getHttpLogger().logRequest(httpRequest, body);

        HttpResponse<InputStream> httpResponse = send(httpRequest);

        return handleAndCastResponseData(httpResponse, clazz);
    }

    /**
     * Basic method which returns an object of type {@link HiroResponse} constructed via a JSON body httpResponse. Sets an appropriate Accept header.
     *
     * @param clazz              The class to create from the incoming JSON data.
     * @param uri                The uri to use.
     * @param method             The method to use.
     * @param body               The body as {@link HiroStreamContainer}. Can be null for methods that do not supply a body.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout for the response. Set this to null to use the internal default.
     * @return An object of clazz constructed from the JSON result or null if the response has no body.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroResponse> T executeWithStreamBody(
            Class<T> clazz,
            URI uri,
            String method,
            HiroStreamContainer body,
            Map<String, String> headers,
            Long httpRequestTimeout
    ) throws HiroException, IOException, InterruptedException {

        HttpRequest httpRequest = createStreamRequest(
                uri,
                method,
                body,
                addHeader(headers, "Accept", "application/json"),
                httpRequestTimeout);

        getHttpLogger().logRequest(httpRequest, body);

        HttpResponse<InputStream> httpResponse = send(httpRequest);

        return handleAndCastResponseData(httpResponse, clazz);
    }

    /**
     * Method to communicate via InputStreams.
     *
     * @param uri                The uri to use.
     * @param method             The method to use.
     * @param body               The body as {@link HiroStreamContainer}. Can be null for methods that do not supply a body.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout for the response. Set this to null to use the internal default.
     * @return A {@link HiroStreamContainer} with the result body as InputStream and associated header information.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public HiroStreamContainer executeBinary(
            URI uri,
            String method,
            HiroStreamContainer body,
            Map<String, String> headers,
            Long httpRequestTimeout
    ) throws HiroException, IOException, InterruptedException {
        HttpRequest httpRequest = createStreamRequest(uri, method, body, headers, httpRequestTimeout);

        getHttpLogger().logRequest(httpRequest, body);

        HttpResponse<InputStream> httpResponse = send(httpRequest);

        getHttpLogger().logResponse(httpResponse, httpResponse.body());

        return new HiroStreamContainer(httpResponse);
    }

    /**
     * Method to communicate asynchronously via String body.
     * This logs only the request, not the result.
     *
     * @param uri                The uri to use.
     * @param method             The method to use.
     * @param body               The body as String. Can be null for methods that do not supply a body.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout for the response. Set this to null to use the internal default.
     * @return A future for the HttpResponse&lt;InputStream&gt;.
     */
    public CompletableFuture<HttpResponse<InputStream>> executeAsyncWithStringBody(
            URI uri,
            String method,
            String body,
            Map<String, String> headers,
            Long httpRequestTimeout
    ) {
        HttpRequest httpRequest = createStringRequest(uri, method, body, headers, httpRequestTimeout);

        getHttpLogger().logRequest(httpRequest, body);
        return sendAsync(httpRequest);
    }

    /**
     * Method to communicate asynchronously via InputStreams.
     * This logs only the request, not the result.
     *
     * @param uri                The uri to use.
     * @param method             The method to use.
     * @param body               The body as {@link HiroStreamContainer}. Can be null for methods that do not supply a body.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout for the response. Set this to null to use the internal default.
     * @return A future for the HttpResponse&lt;InputStream&gt;.
     */
    public CompletableFuture<HttpResponse<InputStream>> executeAsyncWithStreamBody(
            URI uri,
            String method,
            HiroStreamContainer body,
            Map<String, String> headers,
            Long httpRequestTimeout
    ) {
        HttpRequest httpRequest = createStreamRequest(uri, method, body, headers, httpRequestTimeout);

        getHttpLogger().logRequest(httpRequest, body);
        return sendAsync(httpRequest);
    }

    /**
     * Basic GET method which returns a Map constructed via a JSON body
     * httpResponse.
     *
     * @param clazz   The class to create from the incoming JSON data.
     * @param uri     The uri to use.
     * @param headers Initial headers for the httpRequest. Can be null for no additional headers.
     * @return A map constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroResponse> T get(Class<T> clazz, URI uri, Map<String, String> headers)
            throws HiroException, IOException, InterruptedException {

        return get(clazz, uri, headers, null);
    }

    /**
     * Basic GET method which returns a Map constructed via a JSON body
     * httpResponse.
     *
     * @param clazz              The class to create from the incoming JSON data.
     * @param uri                The uri to use.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout for the response. Set this to null to use the internal default.
     * @return A map constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroResponse> T get(Class<T> clazz, URI uri, Map<String, String> headers, Long httpRequestTimeout)
            throws HiroException, IOException, InterruptedException {

        return executeWithStringBody(clazz, uri, "GET", null, headers, httpRequestTimeout);
    }

    /**
     * Basic POST method which returns a Map constructed via a JSON body
     * httpResponse.
     *
     * @param clazz   The class to create from the incoming JSON data.
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

        return post(clazz, uri, body, headers, null);
    }

    /**
     * Basic POST method which returns a Map constructed via a JSON body
     * httpResponse.
     *
     * @param clazz              The class to create from the incoming JSON data.
     * @param uri                The uri to use.
     * @param body               The body as String.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout for the response. Set this to null to use the internal default.
     * @return A map constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroResponse> T post(Class<T> clazz, URI uri, String body, Map<String, String> headers, Long httpRequestTimeout)
            throws HiroException, IOException, InterruptedException {

        return executeWithStringBody(clazz, uri, "POST", body, headers, httpRequestTimeout);
    }

    /**
     * Basic PUT method which returns a Map constructed via a JSON body
     * httpResponse.
     *
     * @param clazz   The class to create from the incoming JSON data.
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

        return put(clazz, uri, body, headers, null);
    }

    /**
     * Basic PUT method which returns a Map constructed via a JSON body
     * httpResponse.
     *
     * @param clazz              The class to create from the incoming JSON data.
     * @param uri                The uri to use.
     * @param body               The body as String.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout for the response. Set this to null to use the internal default.
     * @return A map constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroResponse> T put(Class<T> clazz, URI uri, String body, Map<String, String> headers, Long httpRequestTimeout)
            throws HiroException, IOException, InterruptedException {

        return executeWithStringBody(clazz, uri, "PUT", body, headers, httpRequestTimeout);
    }

    /**
     * Basic PATCH method which returns a Map constructed via a JSON body
     * httpResponse.
     *
     * @param clazz   The class to create from the incoming JSON data.
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

        return patch(clazz, uri, body, headers, null);
    }

    /**
     * Basic PATCH method which returns a Map constructed via a JSON body
     * httpResponse.
     *
     * @param clazz              The class to create from the incoming JSON data.
     * @param uri                The uri to use.
     * @param body               The body as String.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout for the response. Set this to null to use the internal default.
     * @return A map constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroResponse> T patch(Class<T> clazz, URI uri, String body, Map<String, String> headers, Long httpRequestTimeout)
            throws HiroException, IOException, InterruptedException {

        return executeWithStringBody(clazz, uri, "PATCH", body, headers, httpRequestTimeout);
    }

    /**
     * Basic DELETE method which returns a Map constructed via a JSON body
     * httpResponse.
     *
     * @param clazz   The class to create from the incoming JSON data.
     * @param uri     The uri to use.
     * @param headers Initial headers for the httpRequest. Can be null for no additional headers.
     * @return A map constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroResponse> T delete(Class<T> clazz, URI uri, Map<String, String> headers)
            throws HiroException, IOException, InterruptedException {

        return delete(clazz, uri, headers, null);
    }

    /**
     * Basic DELETE method which returns a Map constructed via a JSON body
     * httpResponse.
     *
     * @param clazz              The class to create from the incoming JSON data.
     * @param uri                The uri to use.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout for the response. Set this to null to use the internal default.
     * @return A map constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroResponse> T delete(Class<T> clazz, URI uri, Map<String, String> headers, Long httpRequestTimeout)
            throws HiroException, IOException, InterruptedException {

        return executeWithStringBody(clazz, uri, "DELETE", null, headers, httpRequestTimeout);
    }

    /**
     * Basic GET method which returns an InputStream from the body of the httpResponse.
     *
     * @param uri     The uri to use.
     * @param headers Initial headers for the httpRequest. Can be null for no additional headers.
     * @return A {@link HiroStreamContainer} with the result body as InputStream and associated header information.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public HiroStreamContainer getBinary(URI uri, Map<String, String> headers)
            throws HiroException, IOException, InterruptedException {

        return getBinary(uri, headers, null);
    }

    /**
     * Basic GET method which returns an InputStream from the body of the httpResponse.
     *
     * @param uri                The uri to use.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout for the response. Set this to null to use the internal default.
     * @return A {@link HiroStreamContainer} with the result body as InputStream and associated header information.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public HiroStreamContainer getBinary(URI uri, Map<String, String> headers, Long httpRequestTimeout)
            throws HiroException, IOException, InterruptedException {

        return executeBinary(uri, "GET", null, headers, httpRequestTimeout);
    }

    /**
     * Basic POST method which sends an InputStream.
     *
     * @param clazz   The class to create from the incoming JSON data.
     * @param uri     The uri to use.
     * @param body    Body as {@link HiroStreamContainer}.
     * @param headers Initial headers for the httpRequest. Can be null for no additional headers.
     * @return A map constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroResponse> T postBinary(Class<T> clazz, URI uri, HiroStreamContainer body, Map<String, String> headers)
            throws HiroException, IOException, InterruptedException {

        return postBinary(clazz, uri, body, headers, null);
    }

    /**
     * Basic POST method which sends an InputStream.
     *
     * @param clazz              The class to create from the incoming JSON data.
     * @param uri                The uri to use.
     * @param body               Body as {@link HiroStreamContainer}.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout for the response. Set this to null to use the internal default.
     * @return A map constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroResponse> T postBinary(Class<T> clazz, URI uri, HiroStreamContainer body, Map<String, String> headers, Long httpRequestTimeout)
            throws HiroException, IOException, InterruptedException {

        return executeWithStreamBody(clazz, uri, "POST", body, addHeader(headers, "Accept", "application/json"), httpRequestTimeout);
    }

    /**
     * Basic PUT method which sends an InputStream.
     *
     * @param clazz   The class to create from the incoming JSON data.
     * @param uri     The uri to use.
     * @param body    Body as {@link HiroStreamContainer}.
     * @param headers Initial headers for the httpRequest. Can be null for no additional headers.
     * @return A map constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroResponse> T putBinary(Class<T> clazz, URI uri, HiroStreamContainer body, Map<String, String> headers)
            throws HiroException, IOException, InterruptedException {

        return putBinary(clazz, uri, body, headers, null);
    }

    /**
     * Basic PUT method which sends an InputStream.
     *
     * @param clazz              The class to create from the incoming JSON data.
     * @param uri                The uri to use.
     * @param body               Body as {@link HiroStreamContainer}.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout for the response. Set this to null to use the internal default.
     * @return A map constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroResponse> T putBinary(Class<T> clazz, URI uri, HiroStreamContainer body, Map<String, String> headers, Long httpRequestTimeout)
            throws HiroException, IOException, InterruptedException {

        return executeWithStreamBody(clazz, uri, "PUT", body, addHeader(headers, "Accept", "application/json"), httpRequestTimeout);
    }

    /**
     * Basic PATCH method which sends an InputStream.
     *
     * @param clazz   The class to create from the incoming JSON data.
     * @param uri     The uri to use.
     * @param body    Body as {@link HiroStreamContainer}.
     * @param headers Initial headers for the httpRequest. Can be null for no additional headers.
     * @return A map constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroResponse> T patchBinary(Class<T> clazz, URI uri, HiroStreamContainer body, Map<String, String> headers)
            throws HiroException, IOException, InterruptedException {

        return patchBinary(clazz, uri, body, headers, null);
    }

    /**
     * Basic PATCH method which sends an InputStream.
     *
     * @param clazz              The class to create from the incoming JSON data.
     * @param uri                The uri to use.
     * @param body               Body as {@link HiroStreamContainer}.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout for the response. Set this to null to use the internal default.
     * @return A map constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroResponse> T patchBinary(Class<T> clazz, URI uri, HiroStreamContainer body, Map<String, String> headers, Long httpRequestTimeout)
            throws HiroException, IOException, InterruptedException {

        return executeWithStreamBody(clazz, uri, "PATCH", body, addHeader(headers, "Accept", "application/json"), httpRequestTimeout);
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
     * Override this to add authentication tokens. Call {@link #initializeHeaders(Map)} to get the initial map of
     * headers to adjust.
     *
     * @param headers Map of headers with initial values. Can be null to use only
     *                default headers.
     * @return The headers for this httpRequest.
     * @see #initializeHeaders(Map)
     */
    abstract public Map<String, String> getHeaders(Map<String, String> headers);

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
                HiroStreamContainer streamContainer = new HiroStreamContainer(httpResponse);

                body = streamContainer.getBodyAsString();
                getHttpLogger().logResponse(httpResponse, body);

                String message;

                if (streamContainer.contentIsJson()) {
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
