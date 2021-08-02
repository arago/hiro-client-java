package co.arago.hiro.client.connection;

import co.arago.hiro.client.exceptions.HiroException;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Map;

public abstract class AbstractTokenAPIHandler extends AbstractVersionAPIHandler {

    public interface Conf extends AbstractVersionAPIHandler.Conf {
        /**
         * @param apiName Set the name of the api. This name will be used to determine the API endpoint.
         * @return this
         */
        Conf setApiName(String apiName);

        String getApiName();

        /**
         * @param endpoint Set a custom endpoint directly, omitting automatic endpoint detection via apiName.
         * @return this
         */
        Conf setEndpoint(String endpoint);

        String getEndpoint();
    }

    protected final String apiName;
    protected final String endpoint;

    protected URI endpointUri;

    /**
     * Constructor
     *
     * @param builder The builder to use.
     */
    protected AbstractTokenAPIHandler(Conf builder) {
        super(builder);
        this.apiName = builder.getApiName();
        this.endpoint = builder.getEndpoint();
    }

    protected URI getEndpointUri() throws IOException, InterruptedException, HiroException {
        if (endpoint != null)
            return buildURI(endpoint, null, null);

        if (endpointUri == null)
            endpointUri = getApiUriOf(apiName);

        return endpointUri;
    }

    /**
     * Override this to add authentication tokens. TokenHandlers do not have tokens, so this only returns default
     * headers.
     *
     * @param headers Map of headers with initial values. Can be null to use only default headers.
     * @return The headers for this request.
     */
    @Override
    protected Map<String, String> getHeaders(Map<String, String> headers) {
        return initializeHeaders(headers);
    }

    /**
     * Return the current token.
     *
     * @return The current token.
     */
    public abstract String getToken() throws IOException, InterruptedException, HiroException;

    /**
     * Refresh an invalid token.
     */
    public abstract void refreshToken() throws IOException, InterruptedException, HiroException;

    /**
     * Calculate the Instant after which the token should be refreshed.
     *
     * @return The Instant after which the token shall be refreshed. null if token cannot be refreshed.
     */
    public abstract Instant expiryInstant();
}
