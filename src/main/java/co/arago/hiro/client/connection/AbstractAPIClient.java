package co.arago.hiro.client.connection;

import co.arago.hiro.client.util.HiroException;
import co.arago.hiro.client.model.HiroResponse;
import co.arago.hiro.client.util.JsonTools;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.net.ssl.*;
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
import java.util.HashMap;
import java.util.Map;

/**
 * Root class for all API httpRequests
 */
public abstract class AbstractAPIClient {
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
    private final static TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
    };

    static {
        String v = AbstractAPIClient.class.getPackage().getImplementationVersion();
        version = (v != null ? v : "");
        String t = AbstractAPIClient.class.getPackage().getImplementationTitle();
        title = (t != null ? t : "java-hiro-client");
    }

    public static String title;
    public static String version;

    /**
     * The root url for the API
     */
    private final String apiUrl;
    /**
     * Simple proxy with one address and port
     */
    private AbstractAPIClient.ProxySpec proxy;
    /**
     * Enable Redirect.NORMAL
     */
    private boolean followRedirects = true;
    /**
     * Connect timeout in milliseconds
     */
    private long connectTimeout = 0;
    /**
     * Request timeout in ms.
     */
    private long httpRequestTimeout = 0;
    /**
     * Skip SSL certificate verification
     */
    private boolean acceptAllCerts = false;
    /**
     * The specific SSLContext to use.
     */
    private SSLContext sslContext;
    /**
     * The specific SSLParameters to use.
     */
    private SSLParameters sslParameters;
    /**
     * For header "User-Agent". Default is "java-hiro-client".
     */
    private String userAgent;
    /**
     * Instance of the configured http client. Can also be set externally via {@link #setClient(HttpClient)}
     */
    private HttpClient client;

    // ###############################################################################################
    // ## Constructors                                                                              ##
    // ###############################################################################################

    /**
     * Constructor
     *
     * @param apiUrl The root URL for the HIRO API. Also adds a '/' at the end if necessary.
     */
    public AbstractAPIClient(String apiUrl) {
        this.apiUrl = (StringUtils.endsWith(apiUrl, "/") ? apiUrl : apiUrl + "/");
    }

    /**
     * Constructor
     *
     * @param apiUrl The root URL for the HIRO API. Also adds a '/' at the end if necessary.
     * @param client Use a pre-defined {@link HttpClient}.
     */
    public AbstractAPIClient(String apiUrl, HttpClient client) {
        this.apiUrl = (StringUtils.endsWith(apiUrl, "/") ? apiUrl : apiUrl + "/");
        this.client = client;
    }

    // ###############################################################################################
    // ## Getter/Setter                                                                             ##
    // ###############################################################################################

    public String getApiUrl() {
        return apiUrl;
    }

    public AbstractAPIClient.ProxySpec getProxy() {
        return proxy;
    }

    public AbstractAPIClient setProxy(AbstractAPIClient.ProxySpec proxy) {
        this.proxy = proxy;
        return this;
    }

    public boolean isFollowRedirects() {
        return followRedirects;
    }

    public AbstractAPIClient setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
        return this;
    }

    public long getConnectTimeout() {
        return connectTimeout;
    }

    public AbstractAPIClient setConnectTimeout(long connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public long getRequestTimeout() {
        return httpRequestTimeout;
    }

    public AbstractAPIClient setRequestTimeout(long httpRequestTimeout) {
        this.httpRequestTimeout = httpRequestTimeout;
        return this;
    }

    public boolean isAcceptAllCerts() {
        return acceptAllCerts;
    }

    /**
     * Easily set the {@link #sslContext} to ignore invalid certificates.
     *
     * @param acceptAllCerts true sets the {@link #sslContext} to a context allowing all certificates, false
     *                       resets {@link #sslContext} to null.
     * @return this
     * @throws NoSuchAlgorithmException As per {@link SSLContext#getInstance(String)}.
     * @throws KeyManagementException   As per {@link SSLContext#init(KeyManager[], TrustManager[], SecureRandom)}
     * @see #setSslContext(SSLContext)
     */
    public AbstractAPIClient setAcceptAllCerts(boolean acceptAllCerts) throws NoSuchAlgorithmException, KeyManagementException {
        this.acceptAllCerts = acceptAllCerts;
        if (acceptAllCerts) {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
        } else {
            sslContext = null;
        }
        return this;
    }

    public SSLContext getSslContext() {
        return sslContext;
    }

    /**
     * Set a specific {@link #sslContext}. Overwrites the {@link #sslContext} set via {@link #setAcceptAllCerts(boolean)}.
     *
     * @param sslContext The sslContext to set
     * @return this
     * @see #setAcceptAllCerts(boolean)
     */
    public AbstractAPIClient setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
        return this;
    }

    public SSLParameters getSslParameters() {
        return sslParameters;
    }

    public AbstractAPIClient setSslParameters(SSLParameters sslParameters) {
        this.sslParameters = sslParameters;
        return this;
    }

    /**
     * @return The internally set {@link #userAgent} or a combination of the pre-set {@link #title} and {@link #version}.
     */
    public String getUserAgent() {
        return (userAgent != null ? userAgent : (version != null ? title + " " + version : title));
    }

    public AbstractAPIClient setUserAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    /**
     * Set a pre-defined HttpClient. This will override all other parameters specific to the HttpClient in this class,
     * like {@link #followRedirects}, {@link #proxy} and {@link #connectTimeout}.
     * <p>
     * Set the client to null to force building of a new HttpClient in {@link #getOrBuildClient()}.
     *
     * @param client The Java 11 HttpClient
     * @return this
     */
    public AbstractAPIClient setClient(HttpClient client) {
        this.client = client;
        return this;
    }

    public HttpClient getClient() {
        return client;
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
    // ## Tool methods                                                                              ##
    // ###############################################################################################

    /**
     * @param headers Initial headers. Can be null to use default headers only.
     * @return Map of basic headers
     */
    public Map<String, String> getBasicHeaders(Map<String, String> headers) {
        Map<String, String> headerMap = new HashMap<>(Map.of("User-Agent", getUserAgent()));

        if (headers != null)
            headerMap.putAll(headers);

        return headerMap;
    }

    /**
     * Build a complete uri from the apiUrl and endpoint, optional query parameters and an optional fragment
     *
     * @param endpoint The endpoint to append to {@link #apiUrl}.
     * @param query    Map of query parameters to set.
     * @param fragment URI Fragment
     * @return The constructed URI
     */
    protected URI buildURI(String endpoint, Map<String, String> query, String fragment) {

        URI uri = URI.create(apiUrl).resolve(StringUtils.startsWith(endpoint, "/") ? endpoint.substring(1) : endpoint);

        StringBuilder queryStringBuilder = new StringBuilder();

        if (query != null) {
            for (Map.Entry<String, String> entry : query.entrySet()) {
                queryStringBuilder
                        .append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }
        }

        try {
            return new URI(
                    uri.getScheme(),
                    uri.getAuthority(),
                    uri.getPath(),
                    queryStringBuilder.length() > 0 ? queryStringBuilder.toString() : null,
                    fragment);
        } catch (URISyntaxException e) {
            return uri;
        }
    }

    /**
     * Create a HttpRequest.Builder with common options and headers.
     *
     * @param uri     The uri for the httpRequest.
     * @param headers Initial headers for the httpRequest.
     * @return The HttpRequest.Builder
     */
    protected HttpRequest.Builder getRequestBuilder(URI uri, Map<String, String> headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri);

        if (httpRequestTimeout > 0)
            builder.timeout(Duration.ofMillis(httpRequestTimeout));

        for (Map.Entry<String, String> headerEntry : getHeaders(headers).entrySet()) {
            builder.header(headerEntry.getKey(), headerEntry.getValue());
        }

        return builder;
    }

    /**
     * Decodes the error body from ta httpResponse.
     *
     * @param statusCode The statusCode from the httpResponse.
     * @param body       The body from the httpResponse as String. Can be null when no httpResponse body was returned.
     * @return A string representing the error extracted from the body or from the status code.
     */
    protected String getErrorMessage(int statusCode, String body) {
        String reason = "HttpResponse code " + String.valueOf(statusCode);

        if (StringUtils.isNotBlank(body)) {
            try {
                Object json = JsonTools.DEFAULT.toPOJO(body);

                if (json instanceof Map) {
                    Map<?, ?> jsonMap = (Map<?, ?>) json;
                    Object error = jsonMap.get("error");
                    if (error instanceof String) {
                        reason = (String) error;
                    } else if (error instanceof Map) {
                        Map<?, ?> errorMap = (Map<?, ?>) error;
                        Object message = errorMap.get("message");

                        if (message instanceof String) {
                            reason = (String) message;
                        }
                    }
                }

            } catch (JsonProcessingException e) {
                // ignore
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
     * Send a HttpRequest synchronously and return the httpResponse
     *
     * @param httpRequest The httpRequest to send
     * @return A HttpResponse containing an InputStream of the incoming body part of the result.
     * @throws HiroException        When status errors occur.
     * @throws IOException          On IO errors with the connection.
     * @throws InterruptedException When the call gets interrupted.
     */
    protected HttpResponse<InputStream> send(HttpRequest httpRequest) throws HiroException, IOException, InterruptedException {
        HttpResponse<InputStream> httpResponse = null;
        boolean retry = true;

        while (retry) {
            httpResponse = getOrBuildClient().send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            retry = checkResponse(httpResponse, retry);
        }

        // Default error handling
        int statusCode = httpResponse.statusCode();

        if (statusCode < 200 || statusCode > 399) {
            String body;
            try {
                body = getBodyAsString(httpResponse.body());
                throw new HiroException(getErrorMessage(statusCode, body), statusCode, body);
            } catch (IOException e) {
                throw new HiroException(getErrorMessage(statusCode, null), statusCode, null, e);
            }
        }

        return httpResponse;
    }

    // ###############################################################################################
    // ## Public Request Methods                                                                    ##
    // ###############################################################################################

    /**
     * Basic method which returns a Map constructed via a JSON body httpResponse.
     *
     * @param uri     The uri to use.
     * @param method  The method to use.
     * @param headers Initial headers for the httpRequest.
     * @param body    The body as String. Can be null.
     * @return A map constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public HiroResponse execute(URI uri, String method, Map<String, String> headers, String body) throws HiroException, IOException, InterruptedException {
        HttpRequest httpRequest = getRequestBuilder(uri, headers)
                .method(method, (body != null ?
                        HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8) :
                        HttpRequest.BodyPublishers.noBody()))
                .build();

        HttpResponse<InputStream> httpResponse = send(httpRequest);

        return JsonTools.DEFAULT.toObject(httpResponse.body(), HiroResponse.class);
    }

    /**
     * Method to communicate via InputStreams.
     *
     * @param uri     The uri to use.
     * @param method  The method to use.
     * @param headers Initial headers for the httpRequest.
     * @param body    The body as InputStream. Can be null.
     * @return An InputStream with the result body.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public InputStream executeStreaming(URI uri, String method, Map<String, String> headers, InputStream body) throws HiroException, IOException, InterruptedException {
        HttpRequest httpRequest = getRequestBuilder(uri, headers)
                .method(method, (body != null ?
                        HttpRequest.BodyPublishers.ofInputStream(() -> body) :
                        HttpRequest.BodyPublishers.noBody()))
                .build();

        HttpResponse<InputStream> httpResponse = send(httpRequest);

        return httpResponse.body();
    }

    /**
     * Basic GET method which returns a Map constructed via a JSON body httpResponse.
     *
     * @param uri     The uri to use.
     * @param headers Initial headers for the httpRequest.
     * @return A map constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public HiroResponse get(URI uri, Map<String, String> headers) throws HiroException, IOException, InterruptedException {
        return execute(uri, "GET", headers, null);
    }

    /**
     * Basic POST method which returns a Map constructed via a JSON body httpResponse.
     *
     * @param uri     The uri to use.
     * @param headers Initial headers for the httpRequest.
     * @param body    The body as String.
     * @return A map constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public HiroResponse post(URI uri, Map<String, String> headers, String body) throws HiroException, IOException, InterruptedException {
        return execute(uri, "POST", headers, body);
    }

    /**
     * Basic PUT method which returns a Map constructed via a JSON body httpResponse.
     *
     * @param uri     The uri to use.
     * @param headers Initial headers for the httpRequest.
     * @param body    The body as String.
     * @return A map constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public HiroResponse put(URI uri, Map<String, String> headers, String body) throws HiroException, IOException, InterruptedException {
        return execute(uri, "PUT", headers, body);
    }

    /**
     * Basic PATCH method which returns a Map constructed via a JSON body httpResponse.
     *
     * @param uri     The uri to use.
     * @param headers Initial headers for the httpRequest.
     * @param body    The body as String.
     * @return A map constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public HiroResponse patch(URI uri, Map<String, String> headers, String body) throws HiroException, IOException, InterruptedException {
        return execute(uri, "PATCH", headers, body);
    }

    /**
     * Basic DELETE method which returns a Map constructed via a JSON body httpResponse.
     *
     * @param uri     The uri to use.
     * @param headers Initial headers for the httpRequest.
     * @return A map constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public HiroResponse delete(URI uri, Map<String, String> headers) throws HiroException, IOException, InterruptedException {
        return execute(uri, "DELETE", headers, null);
    }

    /**
     * Basic GET method which returns an InputStream from the body httpResponse.
     *
     * @param uri     The uri to use.
     * @param headers Initial headers for the httpRequest.
     * @return An InputStream with the result body.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public InputStream getBinary(URI uri, Map<String, String> headers) throws HiroException, IOException, InterruptedException {
        return executeStreaming(uri, "GET", headers, null);
    }

    /**
     * Basic POST method which sends an InputStream.
     *
     * @param uri     The uri to use.
     * @param headers Initial headers for the httpRequest.
     * @param body    Body as inputStream.
     * @return A map constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public HiroResponse postBinary(URI uri, Map<String, String> headers, InputStream body) throws HiroException, IOException, InterruptedException {
        InputStream resultBody = executeStreaming(uri, "POST", headers, body);

        return JsonTools.DEFAULT.toObject(resultBody, HiroResponse.class);
    }

    /**
     * Basic PUT method which sends an InputStream.
     *
     * @param uri     The uri to use.
     * @param headers Initial headers for the httpRequest.
     * @param body    Body as inputStream.
     * @return A map constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public HiroResponse putBinary(URI uri, Map<String, String> headers, InputStream body) throws HiroException, IOException, InterruptedException {
        InputStream resultBody = executeStreaming(uri, "PUT", headers, body);

        return JsonTools.DEFAULT.toObject(resultBody, HiroResponse.class);
    }

    /**
     * Basic PATCH method which sends an InputStream.
     *
     * @param uri     The uri to use.
     * @param headers Initial headers for the httpRequest.
     * @param body    Body as inputStream.
     * @return A map constructed from the JSON result.
     * @throws HiroException        On errors indicated by http status codes.
     * @throws IOException          On io errors
     * @throws InterruptedException When the connection gets interrupted.
     */
    public HiroResponse patchBinary(URI uri, Map<String, String> headers, InputStream body) throws HiroException, IOException, InterruptedException {
        InputStream resultBody = executeStreaming(uri, "PATCH", headers, body);

        return JsonTools.DEFAULT.toObject(resultBody, HiroResponse.class);
    }

    // ###############################################################################################
    // ## Abstract methods to override                                                              ##
    // ###############################################################################################

    /**
     * Override this to add authentication tokens.
     *
     * @param headers Map of headers with initial values. Can be null to use only default headers.
     * @return The headers for this httpRequest.
     */
    abstract protected Map<String, String> getHeaders(Map<String, String> headers);

    /**
     * Override this to add automatic token renewal if necessary.
     *
     * @param httpResponse The httpResponse from the HttpRequest
     * @param retry        the current state of retry. If this is set to false, false will also be returned.
     *                     (Ignored here)
     * @return true for a retry, false otherwise.
     * @throws HiroException if the check fails.
     */
    abstract protected boolean checkResponse(HttpResponse<InputStream> httpResponse, boolean retry) throws HiroException;

}


