package co.arago.hiro.client.connection;

import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.model.VersionResponse;

import java.io.IOException;
import java.net.URI;

/**
 * Handles Version information for HIRO.
 */
public abstract class AbstractVersionAPIHandler extends AbstractClientAPIHandler {

    // ###############################################################################################
    // ## Conf and Builder ##
    // ###############################################################################################

    public static abstract class Conf<T extends Conf<T>> extends AbstractClientAPIHandler.Conf<T> {

        private AbstractVersionAPIHandler sharedConnectionHandler;

        public AbstractVersionAPIHandler getSharedConnectionHandler() {
            return sharedConnectionHandler;
        }

        /**
         * Set a shared connection handler. The fields of this class and its parents will be copied from this
         * shared handler, overriding other settings. This is used to share a connection between several handlers.
         *
         * @param sharedConnectionHandler The shared connection handler.
         * @return {@link #self()}
         */
        public T setSharedConnectionHandler(AbstractVersionAPIHandler sharedConnectionHandler) {
            this.sharedConnectionHandler = sharedConnectionHandler;
            return self();
        }

        @Override
        public abstract AbstractVersionAPIHandler build();
    }

    // ###############################################################################################
    // ## Main part ##
    // ###############################################################################################

    private VersionResponse versionMap;

    private final AbstractVersionAPIHandler sharedConnectionHandler;

    /**
     * Constructor
     *
     * @param builder The builder to use.
     */
    protected AbstractVersionAPIHandler(Conf<?> builder) {
        super(builder);
        this.sharedConnectionHandler = null;
    }

    /**
     * Protected Copy Constructor. Fields shall be copied from another AbstractVersionAPIHandler.
     *
     * @param other The source AbstractVersionAPIHandler.
     */
    protected AbstractVersionAPIHandler(AbstractVersionAPIHandler other) {
        super(other);
        this.versionMap = other.versionMap;
        this.sharedConnectionHandler = other;
    }

    /**
     * Get API Versions
     * <p>
     * <i>HIRO REST query API: `GET {@link #rootApiURI} + '/api/version'`</i>
     *
     * @return A map with the api versions
     * @throws HiroException        When the request fails.
     * @throws IOException          When the connection fails.
     * @throws InterruptedException When interrupted.
     */
    protected VersionResponse requestVersionMap() throws IOException, InterruptedException, HiroException {
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
    public VersionResponse getVersionMap() throws HiroException, IOException, InterruptedException {
        if (versionMap != null)
            return versionMap;

        if (sharedConnectionHandler != null)
            return sharedConnectionHandler.getVersionMap();

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
    public URI getApiURIOf(String apiName) throws IOException, InterruptedException, HiroException {
        return buildApiURI(getVersionMap().getVersionEntryOf(apiName).endpoint);
    }

}
