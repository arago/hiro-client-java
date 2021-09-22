package co.arago.hiro.client.rest;

import co.arago.hiro.client.connection.AbstractAPIHandler;
import co.arago.hiro.client.connection.token.AbstractTokenAPIHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.exceptions.HiroHttpException;
import co.arago.hiro.client.exceptions.TokenUnauthorizedException;
import co.arago.hiro.client.model.JsonMessage;
import co.arago.hiro.client.util.HttpLogger;
import co.arago.hiro.client.util.httpclient.StreamContainer;
import co.arago.hiro.client.util.httpclient.URIPath;
import co.arago.util.json.JsonUtil;
import co.arago.util.validation.RequiredFieldChecks;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is the basis of all authenticated API handlers that make use of the different sections of the HIRO API.
 */
public abstract class AuthenticatedAPIHandler extends AbstractAPIHandler {

    final static Logger log = LoggerFactory.getLogger(AuthenticatedAPIHandler.class);

    // ###############################################################################################
    // ## Conf and Builder ##
    // ###############################################################################################

    /**
     * Configuration interface for all the parameters of an AuthenticatedAPIHandler.
     * Builder need to implement this.
     */
    public static abstract class Conf<T extends Conf<T>> {
        private String apiName;
        private String apiPath;
        private Long httpRequestTimeout;
        private AbstractTokenAPIHandler tokenAPIHandler;
        private int maxRetries;

        public String getApiName() {
            return apiName;
        }

        /**
         * @param apiName Set the name of the api. This name will be used to determine the API endpoint entry via
         *                /api/version.
         * @return {@link #self()}
         */
        public T setApiName(String apiName) {
            this.apiName = apiName;
            return self();
        }

        public String getApiPath() {
            return apiPath;
        }

        /**
         * @param apiPath Set a custom API path directly, omitting automatic endpoint detection via apiName.
         * @return {@link #self()}
         */
        public T setApiPath(String apiPath) {
            this.apiPath = apiPath;
            return self();
        }

        public Long getHttpRequestTimeout() {
            return this.httpRequestTimeout;
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
         * @param maxRetries Max amount of retries when http errors are received.
         * @return {@link #self()}
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
         * @return {@link #self()}
         */
        public T setTokenApiHandler(AbstractTokenAPIHandler tokenAPIHandler) {
            this.tokenAPIHandler = tokenAPIHandler;
            return self();
        }

        protected abstract T self();

        public abstract AuthenticatedAPIHandler build();
    }

    // ###############################################################################################
    // ## Inner abstract classes for Builders of API calls ##
    // ###############################################################################################

    /**
     * The basic configuration for all requests. Handle queries, headers and fragments.
     *
     * @param <T> The Builder type
     * @param <R> The type of the result expected from {@link #execute()}
     */
    public static abstract class APIRequestConf<T extends APIRequestConf<T, R>, R> extends RequiredFieldChecks {

        protected final URIPath path;
        protected Map<String, String> query = new HashMap<>();
        protected Map<String, String> headers = new HashMap<>();
        protected String fragment;

        protected Long httpRequestTimeout;
        protected Integer maxRetries;

        /**
         * @param pathParts Array for the path appended to the API path.
         */
        public APIRequestConf(String... pathParts) {
            this.path = new URIPath(pathParts);
        }

        /**
         * @param query Set query parameters.
         * @return {@link #self()}
         */
        public T setUrlQuery(Map<String, String> query) {
            this.query = query;
            return self();
        }

        /**
         * @param headers Set headers.
         * @return {@link #self()}
         */
        public T setHttpHeaders(Map<String, String> headers) {
            this.headers = headers;
            return self();
        }

        /**
         * @param fragment Set URL fragment (http://...#[fragment])
         * @return {@link #self()}
         */
        public T setUrlFragment(String fragment) {
            this.fragment = fragment;
            return self();
        }

        public T setHttpRequestTimeout(Long httpRequestTimeout) {
            this.httpRequestTimeout = httpRequestTimeout;
            return self();
        }

        public T setMaxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
            return self();
        }

        public abstract R execute() throws HiroException, IOException, InterruptedException;

        protected abstract T self();
    }

    /**
     * The basic configuration for all requests that are sending JSON data. Handle queries, headers and fragments.
     *
     * @param <T> The Builder type
     * @param <R> The type of the result expected from {@link #execute()}
     */
    public static abstract class SendBodyAPIRequestConf<T extends SendBodyAPIRequestConf<T, R>, R> extends APIRequestConf<T, R> {
        protected String body;

        /**
         * @param pathParts Array for the path appended to the API path.
         */
        public SendBodyAPIRequestConf(String... pathParts) {
            super(pathParts);
            headers.put("Content-Type", "application/json;encoding=UTF-8");
        }

        /**
         * Convert the Map into a JSON body String.
         *
         * @param map The map to convert.
         * @return {@link #self()}.
         * @throws IllegalArgumentException When the map cannot be transformed to a Json String.
         */
        public T setJsonBodyFromMap(Map<String, ?> map) {
            try {
                return setBody(JsonUtil.DEFAULT.toString(map));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Cannot construct body.", e);
            }
        }

        /**
         * Convert the jsonMessage into a JSON body String.
         *
         * @param jsonMessage The jsonMessage to convert.
         * @return {@link #self()}.
         * @throws IllegalArgumentException When the jsonMessage cannot be transformed to a Json String.
         */
        public T setJsonBodyFromMessage(JsonMessage jsonMessage) {
            try {
                return setBody(jsonMessage.toJsonString());
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Cannot construct body.", e);
            }
        }

        /**
         * Set the body as plain String.
         *
         * @param body The body to set.
         * @return {@link #self()}
         */
        public T setBody(String body) {
            this.body = body;
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
    public static abstract class SendStreamAPIRequestConf<T extends SendStreamAPIRequestConf<T, R>, R> extends APIRequestConf<T, R> {
        protected StreamContainer streamContainer;

        /**
         * Use an existing {@link StreamContainer}
         *
         * @param streamContainer The existing {@link StreamContainer}. Must not be null.
         * @param pathParts       Array for the path appended to the API path.
         */
        public SendStreamAPIRequestConf(StreamContainer streamContainer, String... pathParts) {
            super(pathParts);
            this.streamContainer = notNull(streamContainer, "streamContainer");
        }

        /**
         * Use an inputStream for data and nothing else.
         *
         * @param inputStream The inputStream for the request body. Must not be null.
         * @param pathParts   Array for the path appended to the API path.
         */
        public SendStreamAPIRequestConf(InputStream inputStream, String... pathParts) {
            super(pathParts);
            this.streamContainer = new StreamContainer(notNull(inputStream, "inputStream"), null, null, null);
        }

        public T setCharset(Charset charset) {
            this.streamContainer.setCharset(charset);
            return self();
        }

        /**
         * @param mediaType The mediaType / MIME-Type.
         * @return {@link #self()}
         */
        public T setMediaType(String mediaType) {
            this.streamContainer.setMediaType(mediaType);
            return self();
        }

        /**
         * Decodes mediaType and charset from the contentType.
         *
         * @param contentType The HTTP header field "Content-Type".
         * @return {@link #self()}
         * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Type">Documentation of Content-Type</a>
         */
        public T setContentType(String contentType) {
            this.streamContainer.setContentType(contentType);
            return self();
        }

        /**
         * If "Content-Type" is present in the headers, also set it in the {@link #streamContainer}.
         *
         * @param headers The headers to set.
         * @return {@link #self()}
         */
        @Override
        public T setHttpHeaders(Map<String, String> headers) {

            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    if (StringUtils.equalsIgnoreCase("Content-Type", entry.getKey())) {
                        setContentType(entry.getValue());
                        break;
                    }
                }
            }

            return super.setHttpHeaders(headers);
        }

    }


    // ###############################################################################################
    // ## Main part ##
    // ###############################################################################################

    protected final String apiName;
    protected final String apiPath;
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
        this.apiPath = builder.getApiPath();
        this.tokenAPIHandler = notNull(builder.getTokenApiHandler(), "tokenApiHandler");

        if (StringUtils.isBlank(this.apiName) && StringUtils.isBlank(this.apiPath))
            anyError("Either 'apiName' or 'apiPath' have to be set.");
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
            public URL getApiUrl() {
                return tokenAPIHandler.getApiUrl();
            }

            @Override
            public URI getWebSocketUri() {
                return tokenAPIHandler.getWebSocketUri();
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
     * Construct my URI with query parameters and fragment.
     * This method will query /api/version once to construct the URI unless {@link #apiPath} is set.
     *
     * @param path     The path to append to the API path.
     * @param query    Map of query parameters for this URI. Can be null for no query parameters.
     * @param fragment The fragment to add to the URI.
     * @return The URI with query parameters and fragment.
     * @throws IOException          On a failed call to /api/version
     * @throws InterruptedException Call got interrupted
     * @throws HiroException        When calling /api/version responds with an error
     */
    public URI getEndpointUri(URIPath path, Map<String, String> query, String fragment) throws IOException, InterruptedException, HiroException {
        if (apiUri == null)
            apiUri = (apiPath != null ? buildApiURI(apiPath) : tokenAPIHandler.getApiUriOf(apiName));

        URI pathUri = buildURI(apiUri, path.build(), false);

        return addQueryAndFragment(pathUri, query, fragment);
    }

    /**
     * Add Authorization.
     *
     * @param headers Map of headers with initial values.
     */
    @Override
    public void addToHeaders(Map<String, String> headers) throws InterruptedException, IOException, HiroException {
        headers.put("User-Agent", userAgent);
        headers.put("Authorization", "Bearer " + tokenAPIHandler.getToken());
    }

    /**
     * Checks for {@link TokenUnauthorizedException} and {@link HiroHttpException}
     * until {@link #maxRetries} is exhausted.
     * Also tries to refresh the token on {@link TokenUnauthorizedException}.
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
            // Add one additional retry for obtaining a new token.
            if (retryCount >= 0) {
                log.info("Trying to refresh token because of {}.", e.toString());
                tokenAPIHandler.refreshToken();
                return true;
            } else {
                throw e;
            }
        } catch (HiroHttpException e) {
            if (retryCount > 0) {
                log.info("Retrying with {} retries left because of {}", retryCount, e.toString());
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
