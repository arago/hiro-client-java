package co.arago.hiro.client.connection;

import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.exceptions.TokenUnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.Map;

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

        AbstractTokenAPIHandler getTokenApiHandler();

        /**
         * @param tokenAPIHandler The tokenAPIHandler for this API.
         * @return this
         */
        Conf setTokenApiHandler(AbstractTokenAPIHandler tokenAPIHandler);
    }

    protected final String apiName;
    protected final String endpoint;
    protected final AbstractTokenAPIHandler tokenAPIHandler;
    protected URI endpointUri;

    /**
     * Create this APIHandler with the same connection configuration as the submitted TokenAPIHandler.
     *
     * @param builder The builder to use.
     */
    public AuthenticatedAPIHandler(Conf builder) {
        super(builder.getTokenApiHandler().getConf());
        this.apiName = builder.getApiName();
        this.endpoint = builder.getEndpoint();
        this.tokenAPIHandler = builder.getTokenApiHandler();
    }

    public URI getUri() throws IOException, InterruptedException, HiroException {
        return getUri(null, null);
    }

    public URI getUri(Map<String, String> query) throws IOException, InterruptedException, HiroException {
        return getUri(query, null);
    }

    public URI getUri(Map<String, String> query, String fragment) throws IOException, InterruptedException, HiroException {
        if (endpointUri == null)
            endpointUri = (endpoint != null ? buildURI(endpoint) : tokenAPIHandler.getApiUriOf(apiName));
        return addQueryAndFragment(endpointUri, query, fragment);
    }

    /**
     * Use the same client as the tokenAPIHandler when no special client for this connection is set.
     *
     * @return The HttpClient
     */
    @Override
    public HttpClient getOrBuildClient() {
        return (client != null ? client : tokenAPIHandler.getOrBuildClient());
    }

    /**
     * The default way to check responses for errors and extract error messages. Override this to add automatic token
     * renewal if necessary.
     *
     * @param httpResponse The httpResponse from the HttpRequest
     * @param retryCount current counter for retries
     * @return true for a retry, false otherwise.
     * @throws HiroException if the check fails.
     */
    @Override
    protected boolean checkResponse(HttpResponse<InputStream> httpResponse, int retryCount) throws HiroException, IOException, InterruptedException {
        try {
            return super.checkResponse(httpResponse, retryCount);
        } catch (TokenUnauthorizedException e) {
            if (retryCount > 0) {
                log.info("Refreshing token because of '{}'.", e.getMessage());
                tokenAPIHandler.refreshToken();
                return true;
            }
            throw e;
        }
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
    @Override
    protected Map<String, String> getHeaders(Map<String, String> headers) {
        Map<String, String> finalHeaders = initializeHeaders(headers);

        try {
            finalHeaders.put("Authorization", "Bearer " + tokenAPIHandler.getToken());
        } catch (IOException | InterruptedException | HiroException e) {
            log.error("Cannot get token: '{}'", e.getMessage());
        }

        return finalHeaders;
    }

}
