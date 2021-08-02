package co.arago.hiro.client.connection;

import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.exceptions.HiroHttpException;
import co.arago.hiro.client.model.HiroErrorResponse;
import co.arago.hiro.client.model.HiroResponse;
import co.arago.hiro.client.util.HttpLogger;
import co.arago.hiro.client.util.JsonTools;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.*;

/**
 * Root class for all API httpRequests
 */
public abstract class AbstractAPIClient {

    final Logger log = LoggerFactory.getLogger(AbstractAPIClient.class);

    public interface Conf {
        /**
         * @param apiUrl The root url for the API
         * @return this
         */
        Conf setApiUrl(String apiUrl);

        String getApiUrl();

        /**
         * @param proxy Simple proxy with one address and port
         * @return this
         */
        Conf setProxy(ProxySpec proxy);

        ProxySpec getProxy();

        /**
         * @param followRedirects Enable Redirect.NORMAL. Default is true.
         * @return this
         */
        Conf setFollowRedirects(boolean followRedirects);

        boolean isFollowRedirects();

        /**
         * @param connectTimeout Connect timeout in milliseconds.
         * @return this
         */
        Conf setConnectTimeout(long connectTimeout);

        long getConnectTimeout();

        /**
         * @param httpRequestTimeout Request timeout in ms.
         * @return this
         */
        Conf setHttpRequestTimeout(long httpRequestTimeout);

        long getHttpRequestTimeout();

        /**
         * Skip SSL certificate verification. Leave this unset to use the default in HttpClient. Setting this to true
         * installs a permissive SSLContext, setting it to false removes the SSLContext to use the default.
         *
         * @param acceptAllCerts the toggle
         * @return this
         */
        Conf setAcceptAllCerts(Boolean acceptAllCerts);

        Boolean getAcceptAllCerts();

        /**
         * @param sslContext The specific SSLContext to use.
         * @return this
         * @see #setAcceptAllCerts(Boolean)
         */
        Conf setSslContext(SSLContext sslContext);

        SSLContext getSslContext();

        /**
         * @param sslParameters The specific SSLParameters to use.
         * @return this
         */
        Conf setSslParameters(SSLParameters sslParameters);

        SSLParameters getSslParameters();

        /**
         * For header "User-Agent". Default is determined by the package.
         *
         * @param userAgent The line for the User-Agent header.
         * @return this
         */
        Conf setUserAgent(String userAgent);

        String getUserAgent();

        /**
         * Instance of an externally configured http client. An internal HttpClient will be built with parameters
         * given by this Builder if this is not set.
         *
         * @param client Instance of an HttpClient.
         * @return this
         */
        Conf setClient(HttpClient client);

        HttpClient getClient();

    }

    /**
     * A simple data class for a proxy
     */
    public static class ProxySpec {
        private final String address;
        private final int port;

        public String getAddress() {
            return address;
        }

        public int getPort() {
            return port;
        }

        public ProxySpec(String address, int port) {
            this.address = address;
            this.port = port;
        }
    }

    /**
     * A TrustManager trusting all certificates
     */
    private final static TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }

        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }
    }};

    static {
        String v = AbstractAPIClient.class.getPackage().getImplementationVersion();
        version = (v != null ? v : "");
        String t = AbstractAPIClient.class.getPackage().getImplementationTitle();
        title = (t != null ? t : "java-hiro-client");
    }

    public static String title;
    public static String version;

    protected final String apiUrl;
    protected final AbstractAPIClient.ProxySpec proxy;
    protected final boolean followRedirects;
    protected final long connectTimeout;
    protected final long httpRequestTimeout;
    protected SSLContext sslContext;
    protected final SSLParameters sslParameters;
    protected final String userAgent;
    protected final HttpClient client;

    protected HttpLogger httpLogger = new HttpLogger();

    // ###############################################################################################
    // ## Constructors ##
    // ###############################################################################################

    /**
     * Protected Constructor. Attributes shall be filled via builders.
     *
     * @param builder The builder to use.
     */
    protected AbstractAPIClient(Conf builder) {
        this.apiUrl = (StringUtils.endsWith(builder.getApiUrl(), "/") ? builder.getApiUrl() : builder.getApiUrl() + "/");
        this.proxy = builder.getProxy();
        this.followRedirects = builder.isFollowRedirects();
        this.connectTimeout = builder.getConnectTimeout();
        this.httpRequestTimeout = builder.getHttpRequestTimeout();
        Boolean acceptAllCerts = builder.getAcceptAllCerts();
        this.sslParameters = builder.getSslParameters();
        this.userAgent = (builder.getUserAgent() != null ? builder.getUserAgent() : (version != null ? title + " " + version : title));
        this.client = builder.getClient();

        if (acceptAllCerts == null) {
            this.sslContext = builder.getSslContext();
        } else {
            if (acceptAllCerts) {
                try {
                    this.sslContext = SSLContext.getInstance("TLS");
                    this.sslContext.init(null, trustAllCerts, new SecureRandom());
                } catch (NoSuchAlgorithmException | KeyManagementException e) {
                    // ignore
                }
            } else {
                this.sslContext = null;
            }
        }
    }

    /**
     * Build a new Java 11 HttpClient.
     *
     * @return The HttpClient
     */
    public HttpClient getOrBuildClient() {
        if (client != null)
            return client;

        HttpClient.Builder builder = HttpClient.newBuilder();

        if (followRedirects)
            builder.followRedirects(HttpClient.Redirect.NORMAL);

        if (proxy != null)
            builder.proxy(ProxySelector.of(new InetSocketAddress(proxy.getAddress(), proxy.getPort())));

        if (connectTimeout > 0)
            builder.connectTimeout(Duration.ofMillis(connectTimeout));

        if (sslContext != null)
            builder.sslContext(sslContext);

        if (sslParameters != null)
            builder.sslParameters(sslParameters);

        return builder.build();
    }

    // ###############################################################################################
    // ## Tool methods ##
    // ###############################################################################################

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
     * Build a complete uri from the apiUrl and endpoint, optional query parameters
     * and an optional fragment
     *
     * @param endpoint The endpoint to append to {@link #apiUrl}.
     * @param query    Map of query parameters to set. Can be null for no query parameters.
     * @param fragment URI Fragment. Can be null for no fragment.
     * @return The constructed URI
     */
    protected URI buildURI(String endpoint, Map<String, String> query, String fragment) {

        URI uri = URI.create(apiUrl).resolve(
                StringUtils.startsWith(endpoint, "/") ? endpoint.substring(1) : endpoint
        );

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
     * @param method             The method for the request (used for logging).
     * @param headers            Initial headers for the httpRequest.
     * @param httpRequestTimeout The timeout for the response. Set this to null to use the internal default.
     * @return The HttpRequest.Builder
     */
    protected HttpRequest.Builder getRequestBuilder(URI uri, String method, Map<String, String> headers, Long httpRequestTimeout) {
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
    protected String getErrorMessage(int statusCode, HiroResponse hiroResponse) {
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
    protected String getBodyAsString(InputStream inputStream) throws IOException {
        if (inputStream == null)
            return null;

        return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    }

    /**
     * Check the header for a Content-Type of application/json.
     */
    protected boolean contentIsJson(HttpResponse<?> httpResponse) {
        return StringUtils.startsWithIgnoreCase(getFromHeader(httpResponse, "Content-Type"), "application/json");
    }

    /**
     * Get header content from a httpResponse. If multiple values are present, a CSV from all values is created.
     *
     * @param httpResponse The response with headers.
     * @param headerName   The name of the header field to read.
     * @return The value of the header field or null if no such header exists.
     */
    protected String getFromHeader(HttpResponse<?> httpResponse, String headerName) {
        if (httpResponse == null)
            return null;

        List<String> values = httpResponse.headers().map().get(headerName.toLowerCase());
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
    protected HttpResponse<InputStream> send(HttpRequest httpRequest)
            throws HiroException, IOException, InterruptedException {

        HttpResponse<InputStream> httpResponse = null;
        boolean retry = true;

        while (retry) {
            httpResponse = getOrBuildClient().send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            retry = checkResponse(httpResponse, retry);
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

        HttpRequest httpRequest = getRequestBuilder(uri, method, finalHeaders, httpRequestTimeout)
                .method(method, (body != null ?
                        HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8) :
                        HttpRequest.BodyPublishers.noBody()))
                .build();

        httpLogger.logRequest(httpRequest, body);
        HttpResponse<InputStream> httpResponse = send(httpRequest);

        String responseBody = getBodyAsString(httpResponse.body());

        httpLogger.logResponse(httpResponse, responseBody);

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

        HttpRequest httpRequest = getRequestBuilder(uri, method, headers, httpRequestTimeout)
                .method(method, (body != null ?
                        HttpRequest.BodyPublishers.ofInputStream(() -> body) :
                        HttpRequest.BodyPublishers.noBody()))
                .build();

        httpLogger.logRequest(httpRequest, body);

        HttpResponse<InputStream> httpResponse = send(httpRequest);

        httpLogger.logResponse(httpResponse, httpResponse.body());

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
     * Override this to add authentication tokens. Call {@link #initializeHeaders(Map)} to get the initial map of
     * headers to adjust.
     *
     * @param headers Map of headers with initial values. Can be null to use only
     *                default headers.
     * @return The headers for this httpRequest.
     * @see #initializeHeaders(Map)
     */
    abstract protected Map<String, String> getHeaders(Map<String, String> headers);

    /**
     * The default way to check responses for errors and extract error messages. Override this to add automatic token
     * renewal if necessary.
     *
     * @param httpResponse The httpResponse from the HttpRequest
     * @param retry        the current state of retry. If this is set to false,
     *                     false will also be returned. (Ignored here)
     * @return true for a retry, false otherwise.
     * @throws HiroException if the check fails.
     */
    protected boolean checkResponse(HttpResponse<InputStream> httpResponse, boolean retry) throws HiroException, IOException {
        int statusCode = httpResponse.statusCode();

        if (statusCode < 200 || statusCode > 399) {
            String body;
            try {
                body = getBodyAsString(httpResponse.body());
                httpLogger.logResponse(httpResponse, body);

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
