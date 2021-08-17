package co.arago.hiro.client.connection;

import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.model.VersionResponse;

import java.io.IOException;
import java.net.URI;

/**
 * Handles Version information for HIRO.
 */
public abstract class AbstractVersionAPIHandler extends AbstractClientAPIHandler {

    public static abstract class Conf<T extends Conf<T>> extends AbstractClientAPIHandler.Conf<T> {
        @Override
        public abstract AbstractVersionAPIHandler build();
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
    protected VersionResponse requestVersionMap() throws IOException, InterruptedException, HiroException {
        return get(VersionResponse.class,
                buildURI("/api/version", null, null),
                null,
                httpRequestTimeout,
                maxRetries);
    }

    /**
     * Returns the current {@link #versionMap}. If no {@link #versionMap} is available, it will be requested, cached
     * and then returned.
     *
     * @return The (cached) versionMap.
     * @throws HiroException        When the request fails.
     * @throws IOException          When the connection fails.
     * @throws InterruptedException When interrupted.
     */
    public VersionResponse getVersionMap() throws HiroException, IOException, InterruptedException {
        if (versionMap == null)
            versionMap = requestVersionMap();

        return versionMap;
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
        return buildURI(getVersionMap().getVersionEntryOf(apiName).endpoint, null, null);
    }

}
