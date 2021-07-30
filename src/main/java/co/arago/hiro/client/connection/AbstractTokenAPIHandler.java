package co.arago.hiro.client.connection;

import co.arago.hiro.client.model.VersionResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.Map;

public abstract class AbstractTokenAPIHandler extends AbstractAPIClient {

    public interface Conf extends AbstractAPIClient.Conf {
        String getApiName();

        String getEndpoint();
    }

    private final String apiName;
    private final String endpoint;

    protected URI endpointUri;

    private VersionResponse versionMap;

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
     * <i>HIRO REST query API: `GET {@link #getApiUrl()} + '/api/version'`</i>
     *
     * @return A map with the api versions
     * @throws IOException          When the connection fails.
     * @throws InterruptedException When interrupted.
     */
    public VersionResponse requestVersionMap() throws IOException, InterruptedException {
        InputStream inputStream = getBinary(buildURI("/api/version", null, null), null);

        return VersionResponse.fromInputStream(inputStream);
    }

    protected URI getEndpointUri() throws IOException, InterruptedException {
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
     * @throws IOException          When the connection fails.
     * @throws InterruptedException When interrupted.
     */
    public URI getApiUriOf(String apiName, Map<String, String> query, String fragment) throws IOException, InterruptedException {
        if (versionMap == null)
            versionMap = requestVersionMap();

        return buildURI(versionMap.getValueOf(apiName, "endpoint"), query, fragment);
    }

    /**
     * Determine the API URI endpoint for a named API.
     *
     * @param apiName Name of the API
     * @param query   Map of query parameters to set.
     * @return The URI for that API
     * @throws IOException          When the connection fails.
     * @throws InterruptedException When interrupted.
     */
    public URI getApiUriOf(String apiName, Map<String, String> query) throws IOException, InterruptedException {
        return getApiUriOf(apiName, query, null);
    }

    /**
     * Determine the API URI endpoint for a named API.
     *
     * @param apiName Name of the API
     * @return The URI for that API
     * @throws IOException          When the connection fails.
     * @throws InterruptedException When interrupted.
     */
    public URI getApiUriOf(String apiName) throws IOException, InterruptedException {
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
        return getBasicHeaders(headers);
    }

    /**
     * Return the current token.
     *
     * @return The current token.
     */
    public abstract String getToken() throws IOException, InterruptedException;

    /**
     * Refresh an invalid token.
     */
    public abstract void refreshToken() throws IOException, InterruptedException;

    /**
     * Calculate the Instant after which the token should be refreshed.
     *
     * @return The Instant after which the token shall be refreshed. null if token cannot be refreshed.
     */
    public abstract Instant expiryInstant();
}
