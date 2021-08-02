package co.arago.hiro.client.connection;

import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.model.VersionResponse;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

/**
 * Handles Version information for HIRO.
 */
public abstract class AbstractVersionAPIHandler extends AbstractAPIHandler {

    public interface Conf extends AbstractAPIHandler.Conf {
    }


    private VersionResponse versionMap;

    /**
     * Constructor
     *
     * @param builder The builder to use.
     */
    protected AbstractVersionAPIHandler(Conf builder) {
        super(builder);
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
    public VersionResponse requestVersionMap() throws IOException, InterruptedException, HiroException {
        return get(VersionResponse.class,
                buildURI("/api/version", null, null),
                null,
                null);
    }

    /**
     * Determine the API URI endpoint for a named API.
     *
     * @param apiName  Name of the API
     * @param query    Map of query parameters to set.
     * @param fragment URI Fragment
     * @return The URI for that API
     * @throws HiroException        When the request fails.
     * @throws IOException          When the connection fails.
     * @throws InterruptedException When interrupted.
     */
    public URI getApiUriOf(String apiName, Map<String, String> query, String fragment) throws IOException, InterruptedException, HiroException {
        if (versionMap == null)
            versionMap = requestVersionMap();

        return buildURI(versionMap.getVersionEntryOf(apiName).endpoint, query, fragment);
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

}
