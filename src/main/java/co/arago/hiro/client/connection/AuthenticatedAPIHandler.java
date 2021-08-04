package co.arago.hiro.client.connection;

import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.exceptions.TokenUnauthorizedException;
import co.arago.hiro.client.util.HttpLogger;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * This class is the basis of all authenticated API handlers that make use of the different sections of the HIRO API.
 */
public abstract class AuthenticatedAPIHandler extends AbstractAPIHandler {

    final Logger log = LoggerFactory.getLogger(AuthenticatedAPIHandler.class);

    public interface Conf {
        String getApiName();

        /**
         * @param apiName Set the name of the api. This name will be used to determine the API endpoint.
         * @return this
         */
        Conf setApiName(String apiName);

        String getEndpoint();

        /**
         * @param endpoint Set a custom endpoint directly, omitting automatic endpoint detection via apiName.
         * @return this
         */
        Conf setEndpoint(String endpoint);

        Long getHttpRequestTimeout();

        /**
         * @param httpRequestTimeout Request timeout in ms.
         * @return this
         */
        Conf setHttpRequestTimeout(Long httpRequestTimeout);

        int getMaxRetries();

        /**
         * @param maxRetries Max amount of retries when http errors are received.
         * @return this
         */
        Conf setMaxRetries(int maxRetries);

        AbstractTokenAPIHandler getTokenApiHandler();

        /**
         * @param tokenAPIHandler The tokenAPIHandler for this API.
         * @return this
         */
        Conf setTokenApiHandler(AbstractTokenAPIHandler tokenAPIHandler);
    }

    protected final String apiName;
    protected final String endpoint;
    protected final AbstractTokenAPIHandler hiroClient;
    protected URI apiUri;

    /**
     * Create this APIHandler by using its Builder.
     *
     * @param builder The builder to use.
     */
    protected AuthenticatedAPIHandler(Conf builder) {
        super(makeHandlerConf(builder, builder.getTokenApiHandler()));
        this.apiName = builder.getApiName();
        this.endpoint = builder.getEndpoint();
        this.hiroClient = builder.getTokenApiHandler();
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
    protected static AbstractAPIHandler.GetterConf makeHandlerConf(Conf builder, AbstractTokenAPIHandler tokenAPIHandler) {
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
            apiUri = (endpoint != null ? buildURI(endpoint) : hiroClient.getApiUriOf(apiName));

        URI pathUri = apiUri.resolve(StringUtils.startsWith(path, "/") ? path.substring(1) : path);

        return addQueryAndFragment(pathUri, query, fragment);
    }

    /**
     * Add Authorization. Call {@link #initializeHeaders(Map)} to get the initial map of
     * headers to adjust.
     *
     * @param headers Map of headers with initial values. Can be null to use only
     *                default headers.
     * @return The headers for this httpRequest.
     * @see #initializeHeaders(Map)
     */
    public Map<String, String> getHeaders(Map<String, String> headers) {
        Map<String, String> finalHeaders = addHeader(headers, "User-Agent", userAgent);

        try {
            finalHeaders = addHeader(finalHeaders, "Authorization", "Bearer " + hiroClient.getToken());
        } catch (IOException | InterruptedException | HiroException e) {
            log.error("Cannot get token: '{}'", e.getMessage());
        }

        return finalHeaders;
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
                hiroClient.refreshToken();
                return true;
            } else {
                throw e;
            }
        }
    }

    /**
     * Redirect the HttpLogger to the one provided in {@link #hiroClient}.
     *
     * @return The HttpLogger to use with this class.
     */
    @Override
    public HttpLogger getHttpLogger() {
        return hiroClient.getHttpLogger();
    }

    /**
     * Redirect the HttpClient to the one provided in {@link #hiroClient}.
     *
     * @return The HttpClient to use with this class.
     */
    @Override
    public HttpClient getOrBuildClient() {
        return hiroClient.getOrBuildClient();
    }
}
