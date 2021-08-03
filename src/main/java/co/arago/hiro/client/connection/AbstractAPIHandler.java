package co.arago.hiro.client.connection;

import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.exceptions.HiroHttpException;
import co.arago.hiro.client.model.HiroErrorResponse;
import co.arago.hiro.client.model.HiroResponse;
import co.arago.hiro.client.util.HttpLogger;
import co.arago.hiro.client.util.JsonTools;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

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

/**
 * Root class with fields and tool methods for all API Handlers
 */
public abstract class AbstractAPIHandler {

    public interface Conf {
        String getApiUrl();

        /**
         * @param apiUrl The root url for the API
         * @return this
         */
        AbstractClientAPIHandler.Conf setApiUrl(String apiUrl);

        long getHttpRequestTimeout();

        /**
         * @param httpRequestTimeout Request timeout in ms.
         * @return this
         */
        AbstractClientAPIHandler.Conf setHttpRequestTimeout(long httpRequestTimeout);

        int getMaxRetries();

        /**
         * @param maxRetries Max amount of retries when http errors are received.
         * @return this
         */
        AbstractClientAPIHandler.Conf setMaxRetries(int maxRetries);

        String getUserAgent();

        /**
         * For header "User-Agent". Default is determined by the package.
         *
         * @param userAgent The line for the User-Agent header.
         * @return this
         */
        AbstractClientAPIHandler.Conf setUserAgent(String userAgent);
    }

    public static String title;
    public static String version;

    static {
        String v = AbstractClientAPIHandler.class.getPackage().getImplementationVersion();
        version = (v != null ? v : "");
        String t = AbstractClientAPIHandler.class.getPackage().getImplementationTitle();
        title = (t != null ? t : "java-hiro-client");
    }


    protected final String apiUrl;
    protected final String userAgent;
    protected final long httpRequestTimeout;
    protected int maxRetries;

    protected AbstractAPIHandler(Conf builder) {
        this.apiUrl = (StringUtils.endsWith(builder.getApiUrl(), "/") ? builder.getApiUrl() : builder.getApiUrl() + "/");
        this.maxRetries = builder.getMaxRetries();
        this.httpRequestTimeout = builder.getHttpRequestTimeout();
        this.userAgent = (builder.getUserAgent() != null ? builder.getUserAgent() : (version != null ? title + " " + version : title));

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

        long finalTimeout = (httpRequestTimeout == null ? this.httpRequestTimeout : httpRequestTimeout);
        if (finalTimeout > 0)
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
     * Read the inputStream from the httpResponse and return it as String.
     *
     * @param inputStream The inputStream to read.
     * @return The String constructed from the inputStream.
     * @throws IOException If the inputStream cannot be read.
     */
    public String getBodyAsString(InputStream inputStream) throws IOException {
        if (inputStream == null)
            return null;

        return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    }

    /**
     * Check the header for a Content-Type of application/json.
     */
    public boolean contentIsJson(HttpResponse<?> httpResponse) {
        return StringUtils.startsWithIgnoreCase(getFromHeader(httpResponse, "Content-Type"), "application/json");
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
    // ## Public Request Methods ##
    // ###############################################################################################

    /**
     * Basic method which returns a Map constructed via a JSON body httpResponse. Sets an appropriate Accept header.
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
    public <T extends HiroResponse> T execute(
            Class<T> clazz,
            URI uri,
            String method,
            String body,
            Map<String, String> headers,
            Long httpRequestTimeout
    ) throws HiroException, IOException, InterruptedException {

        // Set Accept to application/json.
        Map<String, String> finalHeaders = new HashMap<>();
        if (headers != null) {
            finalHeaders.putAll(headers);
        }
        finalHeaders.put("Accept", "application/json");

        HttpRequest httpRequest = getRequestBuilder(uri, finalHeaders, httpRequestTimeout)
                .method(method, (body != null ?
                        HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8) :
                        HttpRequest.BodyPublishers.noBody()))
                .build();

        getHttpLogger().logRequest(httpRequest, body);
        HttpResponse<InputStream> httpResponse = send(httpRequest);

        String responseBody = getBodyAsString(httpResponse.body());

        getHttpLogger().logResponse(httpResponse, responseBody);

        return (StringUtils.isNotBlank(responseBody) ? JsonTools.DEFAULT.toObject(responseBody, clazz) : null);
    }

    /**
     * Method to communicate via InputStreams.
     *
     * @param uri                The uri to use.
     * @param method             The method to use.
     * @param body               The body as InputStream. Can be null for methods that do not supply a body.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout for the response. Set this to null to use the internal default.
     * @return An InputStream with the result body.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public InputStream executeBinary(
            URI uri,
            String method,
            InputStream body,
            Map<String, String> headers,
            Long httpRequestTimeout
    ) throws HiroException, IOException, InterruptedException {

        HttpRequest httpRequest = getRequestBuilder(uri, headers, httpRequestTimeout)
                .method(method, (body != null ?
                        HttpRequest.BodyPublishers.ofInputStream(() -> body) :
                        HttpRequest.BodyPublishers.noBody()))
                .build();

        getHttpLogger().logRequest(httpRequest, body);

        HttpResponse<InputStream> httpResponse = send(httpRequest);

        getHttpLogger().logResponse(httpResponse, httpResponse.body());

        return httpResponse.body();
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

        return execute(clazz, uri, "GET", null, headers, httpRequestTimeout);
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

        return execute(clazz, uri, "POST", body, headers, httpRequestTimeout);
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

        return execute(clazz, uri, "PUT", body, headers, httpRequestTimeout);
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

        return execute(clazz, uri, "PATCH", body, headers, httpRequestTimeout);
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

        return execute(clazz, uri, "DELETE", null, headers, httpRequestTimeout);
    }

    /**
     * Basic GET method which returns an InputStream from the body of the httpResponse.
     *
     * @param uri     The uri to use.
     * @param headers Initial headers for the httpRequest. Can be null for no additional headers.
     * @return An InputStream with the result body.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public InputStream getBinary(URI uri, Map<String, String> headers)
            throws HiroException, IOException, InterruptedException {

        return getBinary(uri, headers, null);
    }

    /**
     * Basic GET method which returns an InputStream from the body of the httpResponse.
     *
     * @param uri                The uri to use.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout for the response. Set this to null to use the internal default.
     * @return An InputStream with the result body.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public InputStream getBinary(URI uri, Map<String, String> headers, Long httpRequestTimeout)
            throws HiroException, IOException, InterruptedException {

        return executeBinary(uri, "GET", null, headers, httpRequestTimeout);
    }

    /**
     * Basic POST method which sends an InputStream.
     *
     * @param clazz   The class to create from the incoming JSON data.
     * @param uri     The uri to use.
     * @param body    Body as inputStream.
     * @param headers Initial headers for the httpRequest. Can be null for no additional headers.
     * @return A map constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroResponse> T postBinary(Class<T> clazz, URI uri, InputStream body, Map<String, String> headers)
            throws HiroException, IOException, InterruptedException {

        return postBinary(clazz, uri, body, headers, null);
    }

    /**
     * Basic POST method which sends an InputStream.
     *
     * @param clazz              The class to create from the incoming JSON data.
     * @param uri                The uri to use.
     * @param body               Body as inputStream.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout for the response. Set this to null to use the internal default.
     * @return A map constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroResponse> T postBinary(Class<T> clazz, URI uri, InputStream body, Map<String, String> headers, Long httpRequestTimeout)
            throws HiroException, IOException, InterruptedException {

        InputStream resultBody = executeBinary(uri, "POST", body, headers, httpRequestTimeout);

        return JsonTools.DEFAULT.toObject(resultBody, clazz);
    }

    /**
     * Basic PUT method which sends an InputStream.
     *
     * @param clazz   The class to create from the incoming JSON data.
     * @param uri     The uri to use.
     * @param body    Body as inputStream.
     * @param headers Initial headers for the httpRequest. Can be null for no additional headers.
     * @return A map constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroResponse> T putBinary(Class<T> clazz, URI uri, InputStream body, Map<String, String> headers)
            throws HiroException, IOException, InterruptedException {

        return putBinary(clazz, uri, body, headers, null);
    }

    /**
     * Basic PUT method which sends an InputStream.
     *
     * @param clazz              The class to create from the incoming JSON data.
     * @param uri                The uri to use.
     * @param body               Body as inputStream.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout for the response. Set this to null to use the internal default.
     * @return A map constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroResponse> T putBinary(Class<T> clazz, URI uri, InputStream body, Map<String, String> headers, Long httpRequestTimeout)
            throws HiroException, IOException, InterruptedException {

        InputStream resultBody = executeBinary(uri, "PUT", body, headers, httpRequestTimeout);

        return JsonTools.DEFAULT.toObject(resultBody, clazz);
    }

    /**
     * Basic PATCH method which sends an InputStream.
     *
     * @param clazz   The class to create from the incoming JSON data.
     * @param uri     The uri to use.
     * @param body    Body as inputStream.
     * @param headers Initial headers for the httpRequest. Can be null for no additional headers.
     * @return A map constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroResponse> T patchBinary(Class<T> clazz, URI uri, InputStream body, Map<String, String> headers)
            throws HiroException, IOException, InterruptedException {

        return patchBinary(clazz, uri, body, headers, null);
    }

    /**
     * Basic PATCH method which sends an InputStream.
     *
     * @param clazz              The class to create from the incoming JSON data.
     * @param uri                The uri to use.
     * @param body               Body as inputStream.
     * @param headers            Initial headers for the httpRequest. Can be null for no additional headers.
     * @param httpRequestTimeout The timeout for the response. Set this to null to use the internal default.
     * @return A map constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public <T extends HiroResponse> T patchBinary(Class<T> clazz, URI uri, InputStream body, Map<String, String> headers, Long httpRequestTimeout)
            throws HiroException, IOException, InterruptedException {

        InputStream resultBody = executeBinary(uri, "PATCH", body, headers, httpRequestTimeout);

        return JsonTools.DEFAULT.toObject(resultBody, clazz);
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
     * @param retryCount current counter for retries
     * @return true for a retry, false otherwise.
     * @throws HiroException if the check fails.
     */
    public boolean checkResponse(HttpResponse<InputStream> httpResponse, int retryCount) throws HiroException, IOException, InterruptedException {
        int statusCode = httpResponse.statusCode();

        if (statusCode < 200 || statusCode > 399) {
            String body;
            try {
                body = getBodyAsString(httpResponse.body());
                getHttpLogger().logResponse(httpResponse, body);

                String message;

                if (contentIsJson(httpResponse)) {
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
