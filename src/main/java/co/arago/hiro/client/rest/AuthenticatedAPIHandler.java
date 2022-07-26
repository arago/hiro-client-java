package co.arago.hiro.client.rest;

import co.arago.hiro.client.connection.AbstractAPIHandler;
import co.arago.hiro.client.connection.token.TokenAPIHandler;
import co.arago.hiro.client.exceptions.FixedTokenException;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.exceptions.HiroHttpException;
import co.arago.hiro.client.exceptions.TokenUnauthorizedException;
import co.arago.hiro.client.model.JsonMessage;
import co.arago.hiro.client.util.httpclient.HttpHeaderMap;
import co.arago.hiro.client.util.httpclient.StreamContainer;
import co.arago.hiro.client.util.httpclient.URIEncodedData;
import co.arago.hiro.client.util.httpclient.URIPath;
import co.arago.util.json.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.util.Map;

import static co.arago.util.validation.ValueChecks.anyError;
import static co.arago.util.validation.ValueChecks.notNull;

/**
 * This class is the basis of all authenticated API handlers that make use of the different sections of the HIRO API.
 * It copies its configuration from the supplied {@link TokenAPIHandler} and overrides
 * {@link AbstractAPIHandler#addToHeaders(HttpHeaderMap)} and {@link AbstractAPIHandler#checkResponse(HttpResponse, int)}
 * to handle tokens.
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
        private TokenAPIHandler tokenAPIHandler;
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

        public TokenAPIHandler getTokenApiHandler() {
            return this.tokenAPIHandler;
        }

        /**
         * @param tokenAPIHandler The tokenAPIHandler for this API.
         * @return {@link #self()}
         */
        public T setTokenApiHandler(TokenAPIHandler tokenAPIHandler) {
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
    public static abstract class APIRequestConf<T extends APIRequestConf<T, R>, R> {

        protected final URIPath path;
        protected URIEncodedData query = new URIEncodedData();
        protected HttpHeaderMap headers = new HttpHeaderMap();
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
        public T setUrlQuery(URIEncodedData query) {
            this.query.setAll(query);
            return self();
        }

        /**
         * @param headers Set headers.
         * @return {@link #self()}
         */
        public T setHttpHeaders(HttpHeaderMap headers) {
            this.headers.setAll(headers);
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
    public static abstract class SendBodyAPIRequestConf<T extends SendBodyAPIRequestConf<T, R>, R>
            extends APIRequestConf<T, R> {
        protected String body;

        /**
         * @param pathParts Array for the path appended to the API path.
         */
        public SendBodyAPIRequestConf(String... pathParts) {
            super(pathParts);
            headers.set("Content-Type", "application/json;encoding=UTF-8");
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
    public static abstract class SendStreamAPIRequestConf<T extends SendStreamAPIRequestConf<T, R>, R>
            extends APIRequestConf<T, R> {
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
        public T setHttpHeaders(HttpHeaderMap headers) {

            if (headers != null) {
                String contentType = headers.getFirstIgnoreCase("Content-Type");
                if (StringUtils.isNotBlank(contentType))
                    setContentType(contentType);
            }

            return super.setHttpHeaders(headers);
        }

    }

    // ###############################################################################################
    // ## Main part ##
    // ###############################################################################################

    protected final String apiName;
    protected final String apiPath;
    protected final TokenAPIHandler tokenAPIHandler;
    protected URI apiURI;

    /**
     * Create this APIHandler by using its Builder. The API configuration will be copied over from the
     * builder.tokenAPIHandler.
     *
     * @param builder The builder to use.
     */
    protected AuthenticatedAPIHandler(Conf<?> builder) {
        super(notNull(builder.tokenAPIHandler, "tokenApiHandler").getConf());
        this.tokenAPIHandler = builder.getTokenApiHandler();
        this.apiName = builder.getApiName();
        this.apiPath = builder.getApiPath();

        if (StringUtils.isAllBlank(this.apiName, this.apiPath))
            anyError("Either 'apiName' or 'apiPath' have to be set.");
    }

    /**
     * Construct my URI with query parameters and fragment.
     * This method will query /api/version once to construct the URI unless {@link #apiPath} is set.
     *
     * @param path     The path to append to the API path.
     * @param query    Query parameters for this URI. Can be null for no query parameters.
     * @param fragment The fragment to add to the URI.
     * @return The URI with query parameters and fragment.
     * @throws IOException          On a failed call to /api/version
     * @throws InterruptedException Call got interrupted
     * @throws HiroException        When calling /api/version responds with an error
     */
    public URI getEndpointURI(URIPath path, URIEncodedData query, String fragment)
            throws IOException, InterruptedException, HiroException {
        if (apiURI == null)
            apiURI = (apiPath != null ? buildApiURI(apiPath) : tokenAPIHandler.getApiURIOf(apiName));

        URI pathURI = AbstractAPIHandler.buildURI(apiURI, path.build(), false);

        return AbstractAPIHandler.addQueryFragmentAndNormalize(pathURI, query, fragment);
    }

    /**
     * Add Authorization.
     *
     * @param headers Map of headers with initial values.
     */
    @Override
    public void addToHeaders(HttpHeaderMap headers) throws InterruptedException, IOException, HiroException {
        tokenAPIHandler.addToHeaders(headers);
        headers.set("Authorization", "Bearer " + tokenAPIHandler.getToken());
    }

    /**
     * Checks for {@link TokenUnauthorizedException} and {@link HiroHttpException}
     * until {@link AbstractAPIHandler#getMaxRetries()} is exhausted.
     * Also tries to refresh the token on {@link TokenUnauthorizedException}.
     *
     * @param httpResponse The httpResponse from the HttpRequest
     * @param retryCount   current counter for retries. Counts down to zero.
     * @return true for a retry, false otherwise.
     * @throws TokenUnauthorizedException When the statusCode is 401 and all retries are exhausted or the token cannot
     *                                    be refreshed.
     * @throws HiroHttpException          When the statusCode is &lt; 200 or &gt; 399 and all retries are exhausted.
     * @throws HiroException              On internal errors regarding hiro data processing.
     * @throws IOException                When the refresh fails with an IO error.
     * @throws InterruptedException       Call got interrupted.
     */
    @Override
    public boolean checkResponse(HttpResponse<InputStream> httpResponse, int retryCount)
            throws HiroException, IOException, InterruptedException {
        try {
            return tokenAPIHandler.checkResponse(httpResponse, retryCount);
        } catch (TokenUnauthorizedException e) {
            try {
                if (retryCount < 0)
                    throw e;

                log.info("Trying to refresh token because of {}.", e.toString());
                tokenAPIHandler.refreshToken();
                return true;
            } catch (FixedTokenException fte) {
                // Integrate the FixedTokenException into the exception chain while keeping the original type.
                throw new TokenUnauthorizedException(e.getMessage() + " " + fte.getMessage(), e.getCode(), e.getBody(),
                        new FixedTokenException(fte.getMessage(), e));
            }
        } catch (HiroHttpException e) {
            if (retryCount <= 0)
                throw e;

            log.info("Retrying with {} retries left because of {}", retryCount, e.toString());
            return true;
        }
    }

}
