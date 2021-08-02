package co.arago.hiro.client.connection;

import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.model.VersionResponse;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Map;

public abstract class AbstractTokenAPIHandler extends AbstractAPIClient {

    public interface Conf extends AbstractAPIClient.Conf {
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

    private VersionResponse versionResponse;

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

    /**
     * Get API Versions
     * <p>
     * <i>HIRO REST query API: `GET {@link #apiUrl} + '/api/version'`</i>
     *
     * @return A map with the api versions
     * @throws HiroException        When the request fails.
     * @throws IOException          When the connection fails.
     * @throws InterruptedException When interrupted.
     */
    public VersionResponse requestVersionData() throws IOException, InterruptedException, HiroException {
        return get(VersionResponse.class,
                buildURI("/api/version", null, null),
                null,
                null);
    }

    protected URI getEndpointUri() throws IOException, InterruptedException, HiroException {
        if (endpoint != null)
            return buildURI(endpoint, null, null);

        if (endpointUri == null)
            endpointUri = getApiUriOf(apiName);

        return endpointUri;
    }

    /**
     * Determine the API URI endpoint for a named API.
     *
     * @param apiName  Name of the API
     * @param query    Map of query parameters to set.
     * @param fragment URI Fragment
     * @return The URI for that API
     * @throws HiroException        When the request fails or the apiName cannot be found in {@link #versionResponse}.
     * @throws IOException          When the connection fails.
     * @throws InterruptedException When interrupted.
     */
    public URI getApiUriOf(String apiName, Map<String, String> query, String fragment) throws IOException, InterruptedException, HiroException {
        if (versionResponse == null)
            versionResponse = requestVersionData();

        return buildURI(versionResponse.getVersionEntryOf(apiName).endpoint, query, fragment);
    }

    /**
     * Determine the API URI endpoint for a named API.
     *
     * @param apiName Name of the API
     * @param query   Map of query parameters to set.
     * @return The URI for that API
     * @throws HiroException        When the request fails.
     * @throws IOException          When the connection fails.
     * @throws InterruptedException When interrupted.
     */
    public URI getApiUriOf(String apiName, Map<String, String> query) throws IOException, InterruptedException, HiroException {
        return getApiUriOf(apiName, query, null);
    }

    /**
     * Determine the API URI endpoint for a named API.
     *
     * @param apiName Name of the API
     * @return The URI for that API
     * @throws HiroException        When the request fails.
     * @throws IOException          When the connection fails.
     * @throws InterruptedException When interrupted.
     */
    public URI getApiUriOf(String apiName) throws IOException, InterruptedException, HiroException {
        return getApiUriOf(apiName, null, null);
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
