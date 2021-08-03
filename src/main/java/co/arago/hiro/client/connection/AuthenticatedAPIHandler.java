package co.arago.hiro.client.connection;

import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.exceptions.TokenUnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.Map;

public abstract class AuthenticatedAPIHandler extends AbstractAPIHandler {

    final Logger log = LoggerFactory.getLogger(AuthenticatedAPIHandler.class);

    public interface Conf extends AbstractAPIHandler.Conf {
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
    protected final AbstractTokenAPIHandler hiroClient;
    protected URI endpointUri;

    /**
     * Create this APIHandler with the same connection configuration as the submitted TokenAPIHandler.
     *
     * @param builder The builder to use.
     */
    protected AuthenticatedAPIHandler(Conf builder) {
        super(builder);
        this.apiName = builder.getApiName();
        this.endpoint = builder.getEndpoint();
        this.hiroClient = builder.getTokenApiHandler();
    }

    public URI getUri() throws IOException, InterruptedException, HiroException {
        return getUri(null, null);
    }

    public URI getUri(Map<String, String> query) throws IOException, InterruptedException, HiroException {
        return getUri(query, null);
    }

    public URI getUri(Map<String, String> query, String fragment) throws IOException, InterruptedException, HiroException {
        if (endpointUri == null)
            endpointUri = (endpoint != null ? buildURI(endpoint) : hiroClient.getApiUriOf(apiName));
        return hiroClient.addQueryAndFragment(endpointUri, query, fragment);
    }

    /**
     * Add Authorization. Call {@link AbstractTokenAPIHandler#initializeHeaders(Map)} to get the initial map of
     * headers to adjust.
     *
     * @param headers Map of headers with initial values. Can be null to use only
     *                default headers.
     * @return The headers for this httpRequest.
     * @see AbstractTokenAPIHandler#initializeHeaders(Map)
     */
    public Map<String, String> getHeaders(Map<String, String> headers) {
        Map<String, String> finalHeaders = initializeHeaders(headers);

        try {
            finalHeaders.put("Authorization", "Bearer " + hiroClient.getToken());
        } catch (IOException | InterruptedException | HiroException e) {
            log.error("Cannot get token: '{}'", e.getMessage());
        }

        return finalHeaders;
    }

}
