package co.arago.hiro.client.connection;

import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.model.VersionResponse;

import java.io.IOException;
import java.net.URI;

/**
 * Handles Version information for HIRO.
 */
public abstract class AbstractVersionAPIHandler extends AbstractClientAPIHandler implements VersionAPIHandler {

    // ###############################################################################################
    // ## Conf and Builder ##
    // ###############################################################################################

    public static abstract class Conf<T extends Conf<T>> extends AbstractClientAPIHandler.Conf<T> {
        @Override
        public abstract AbstractVersionAPIHandler build();
    }

    // ###############################################################################################
    // ## Main part ##
    // ###############################################################################################

    private VersionResponse versionMap;

    /**
     * Constructor
     *
     * @param builder The builder to use.
     */
    protected AbstractVersionAPIHandler(Conf<?> builder) {
        super(builder);
    }

    /**
     * Protected Copy Constructor. Attributes shall be copied from another AbstractVersionAPIHandler.
     *
     * @param other The source AbstractVersionAPIHandler.
     */
    protected AbstractVersionAPIHandler(AbstractVersionAPIHandler other) {
        super(other);
        this.versionMap = other.versionMap;
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
    @Override
    public VersionResponse requestVersionMap() throws IOException, InterruptedException, HiroException {
        return get(VersionResponse.class,
                buildEndpointURI("/api/version"),
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
    @Override
    public VersionResponse getVersionMap() throws HiroException, IOException, InterruptedException {
        if (versionMap == null)
            versionMap = requestVersionMap();

        return versionMap;
    }

    /**
     * Determine the API URI path for a named API.
     *
     * @param apiName Name of the API
     * @return The URI for that API
     * @throws HiroException        When the request fails.
     * @throws IOException          When the connection fails.
     * @throws InterruptedException When interrupted.
     */
    @Override
    public URI getApiUriOf(String apiName) throws IOException, InterruptedException, HiroException {
        return buildApiURI(getVersionMap().getVersionEntryOf(apiName).endpoint);
    }

}
