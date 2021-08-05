package co.arago.hiro.client.connection;

import co.arago.hiro.client.connection.token.AbstractTokenAPIHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.exceptions.TokenUnauthorizedException;
import co.arago.hiro.client.util.HttpLogger;
import co.arago.hiro.client.util.JsonTools;
import co.arago.hiro.client.util.RequiredFieldChecker;
import co.arago.hiro.client.util.httpclient.StreamContainer;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is the basis of all authenticated API handlers that make use of the different sections of the HIRO API.
 */
public abstract class AuthenticatedAPIHandler extends AbstractAPIHandler {

    final Logger log = LoggerFactory.getLogger(AuthenticatedAPIHandler.class);

    /**
     * The basic configuration for all requests. Handle queries, headers and fragments.
     *
     * @param <T> The Builder type
     * @param <R> The type of the result expected from {@link #execute()}
     */
    public static abstract class APIRequestConf<T extends APIRequestConf<T, R>, R> {

        protected Map<String, String> query = new HashMap<>();
        protected Map<String, String> headers = new HashMap<>();
        protected String fragment;

        public T setQuery(Map<String, String> query) {
            this.query = query;
            return self();
        }

        public T setHeaders(Map<String, String> headers) {
            this.headers = headers;
            return self();
        }

        public T setFragment(String fragment) {
            this.fragment = fragment;
            return self();
        }

        public abstract R execute() throws HiroException, IOException, InterruptedException;

        protected abstract T self();
    }

    public static abstract class SendJsonAPIRequestConf<T extends SendJsonAPIRequestConf<T, R>, R> extends APIRequestConf<T, R> {
        protected String body;

        /**
         * Set the body as plain String.
         *
         * @param body The body to set.
         * @return this
         */
        public T setBody(String body) {
            this.body = body;
            return self();
        }

        /**
         * Convert the Map into a JSON body String.
         * @param body The map to convert.
         * @return this.
         */
        public T setJsonFromMap(Map<String,?> body) throws JsonProcessingException {
            this.body = JsonTools.DEFAULT.toString(body);
            return self();
        }
    }

    /**
     * The basic configuration for all requests that upload binary data. Handle queries, headers and fragments as well
     * as creation of the {@link StreamContainer}.
     *
     * @param <T> The Builder type
     * @param <R> The type of the result expected from {@link #execute()}
     */
    public static abstract class SendBinaryAPIRequestConf<T extends SendBinaryAPIRequestConf<T, R>, R> extends APIRequestConf<T, R> {
        protected StreamContainer streamContainer;

        /**
         * Use an existing {@link StreamContainer}
         *
         * @param streamContainer The existing {@link StreamContainer}. Must not be null.
         */
        public SendBinaryAPIRequestConf(StreamContainer streamContainer) {
            RequiredFieldChecker.notNull(streamContainer, "streamContainer");
            this.streamContainer = streamContainer;
        }

        /**
         * Use an inputStream for data and nothing else.
         *
         * @param inputStream The inputStream for the request body. Must not be null.
         */
        public SendBinaryAPIRequestConf(InputStream inputStream) {
            RequiredFieldChecker.notNull(inputStream, "inputStream");
            this.streamContainer = new StreamContainer(inputStream, null, null, null);
        }

        public T setCharset(Charset charset) {
            this.streamContainer.setCharset(charset);
            return self();
        }

        /**
         * @param mediaType The mediaType / MIME-Type.
         * @return this
         */
        public T setMediaType(String mediaType) {
            this.streamContainer.setMediaType(mediaType);
            return self();
        }

        /**
         * Decodes mediaType and charset from the contentType.
         *
         * @param contentType The HTTP header field "Content-Type".
         * @return this
         * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Type">Documentation of Content-Type</a>
         */
        public T setContentType(String contentType) {
            this.streamContainer.setContentType(contentType);
            return self();
        }

        /**
         * Override here to grab Content-Type header for {@link #streamContainer}.
         *
         * @param headers The headers to set.
         * @return this
         */
        @Override
        public T setHeaders(Map<String, String> headers) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (StringUtils.equalsIgnoreCase(entry.getKey(), "Content-Type")) {
                    setContentType(entry.getValue());
                }
            }

            return super.setHeaders(headers);
        }

    }


    /**
     * Configuration interface for all the parameters of an AuthenticatedAPIHandler.
     * Builder need to implement this.
     */
    public static abstract class Conf<T extends Conf<T>> {
        private String apiName;
        private String endpoint;
        private Long httpRequestTimeout;
        private AbstractTokenAPIHandler tokenAPIHandler;
        private int maxRetries;

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
         * @param endpoint Set a custom endpoint directly, omitting automatic endpoint detection via apiName.
         * @return this
         */
        public T setEndpoint(String endpoint) {
            this.endpoint = endpoint;
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

    protected final String apiName;
    protected final String endpoint;
    protected final AbstractTokenAPIHandler tokenAPIHandler;
    protected URI apiUri;

    /**
     * Create this APIHandler by using its Builder.
     *
     * @param builder The builder to use.
     */
    protected AuthenticatedAPIHandler(Conf<?> builder) {
        super(makeHandlerConf(builder, builder.getTokenApiHandler()));
        this.apiName = builder.getApiName();
        this.endpoint = builder.getEndpoint();
        this.tokenAPIHandler = builder.getTokenApiHandler();
    }

    /**
     * Construct an AbstractAPIHandler.GetterConf from the values of this Conf and the supplied tokenAPIHandler.
     * This ensures, that some values ({@link #apiUrl} and {@link #userAgent}) are always set via the tokenAPIHandler
     * and some others use default values from there ({@link #httpRequestTimeout} and {@link #maxRetries}) unless set
     * in the builder for this Handler.
     *
     * @param builder         The builder of this handler.
     * @param tokenAPIHandler The tokenApiHandler for this Handler.
     * @return An AbstractAPIHandler.GetterConf for the parent class.
     */
    protected static AbstractAPIHandler.GetterConf makeHandlerConf(Conf<?> builder, AbstractTokenAPIHandler tokenAPIHandler) {
        return new AbstractAPIHandler.GetterConf() {
            @Override
            public String getApiUrl() {
                return tokenAPIHandler.getApiUrl();
            }

            @Override
            public Long getHttpRequestTimeout() {
                return builder.getHttpRequestTimeout() != null ? builder.getHttpRequestTimeout() : tokenAPIHandler.getHttpRequestTimeout();
            }

            @Override
            public int getMaxRetries() {
                return builder.getMaxRetries() > 0 ? builder.getMaxRetries() : tokenAPIHandler.getMaxRetries();
            }

            @Override
            public String getUserAgent() {
                return tokenAPIHandler.getUserAgent();
            }
        };
    }

    /**
     * Construct my URI.
     * This method will query /api/version once to construct the URI unless {@link #endpoint} is set.
     *
     * @param path The path to append to the API path.
     * @return The URI without query or fragment.
     * @throws IOException          On a failed call to /api/version
     * @throws InterruptedException Call got interrupted
     * @throws HiroException        When calling /api/version responds with an error
     */
    public URI getUri(String path) throws IOException, InterruptedException, HiroException {
        return getUri(path, null, null);
    }

    /**
     * Construct my URI with query parameters
     * This method will query /api/version once to construct the URI unless {@link #endpoint} is set.
     *
     * @param path  The path to append to the API path.
     * @param query Map of query parameters for this URI
     * @return The URI with query parameters.
     * @throws IOException          On a failed call to /api/version
     * @throws InterruptedException Call got interrupted
     * @throws HiroException        When calling /api/version responds with an error
     */
    public URI getUri(String path, Map<String, String> query) throws IOException, InterruptedException, HiroException {
        return getUri(path, query, null);
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
            apiUri = (endpoint != null ? buildURI(endpoint) : tokenAPIHandler.getApiUriOf(apiName));

        URI pathUri = apiUri.resolve(StringUtils.startsWith(path, "/") ? path.substring(1) : path);

        return addQueryAndFragment(pathUri, query, fragment);
    }

    /**
     * Add Authorization.
     *
     * @param headers Map of headers with initial values.
     */
    @Override
    public void addToHeaders(Map<String, String> headers) {
        try {
            headers.put("User-Agent", userAgent);
            headers.put("Authorization", "Bearer " + tokenAPIHandler.getToken());
        } catch (IOException | InterruptedException | HiroException e) {
            log.error("Cannot get token: '{}'", e.getMessage());
        }
    }

    /**
     * Checks for {@link TokenUnauthorizedException} and tries to refresh the token unless retryCount is 0.
     *
     * @param httpResponse The httpResponse from the HttpRequest
     * @param retryCount   current counter for retries
     * @return true for a retry, false otherwise.
     * @throws HiroException        If the check fails with a http status code error.
     * @throws IOException          When the refresh fails with an IO error.
     * @throws InterruptedException Call got interrupted.
     */
    @Override
    public boolean checkResponse(HttpResponse<InputStream> httpResponse, int retryCount) throws HiroException, IOException, InterruptedException {
        try {
            return super.checkResponse(httpResponse, retryCount);
        } catch (TokenUnauthorizedException e) {
            if (retryCount > 0) {
                log.info("Refreshing token because of '{}'.", e.getMessage());
                tokenAPIHandler.refreshToken();
                return true;
            } else {
                throw e;
            }
        }
    }

    /**
     * Redirect the HttpLogger to the one provided in {@link #tokenAPIHandler}.
     *
     * @return The HttpLogger to use with this class.
     */
    @Override
    protected HttpLogger getHttpLogger() {
        return tokenAPIHandler.getHttpLogger();
    }

    /**
     * Redirect the HttpClient to the one provided in {@link #tokenAPIHandler}.
     *
     * @return The HttpClient to use with this class.
     */
    @Override
    protected HttpClient getOrBuildClient() {
        return tokenAPIHandler.getOrBuildClient();
    }
}
