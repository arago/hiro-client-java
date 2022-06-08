package co.arago.hiro.client.connection;

import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.model.VersionResponse;

import java.io.IOException;
import java.net.URI;

public interface VersionAPIHandler extends ClientAPIHandler {
    /**
     * Get API Versions
     * <p>
     * <i>HIRO REST query API: `GET apiUrl + '/api/version'`</i>
     *
     * @return A map with the api versions
     * @throws HiroException        When the request fails.
     * @throws IOException          When the connection fails.
     * @throws InterruptedException When interrupted.
     */
    VersionResponse requestVersionMap() throws IOException, InterruptedException, HiroException;

    /**
     * Returns the current versionMap. If no versionMap is available, it will be requested, cached
     * and then returned.
     *
     * @return The (cached) versionMap.
     * @throws HiroException        When the request fails.
     * @throws IOException          When the connection fails.
     * @throws InterruptedException When interrupted.
     */
    VersionResponse getVersionMap() throws HiroException, IOException, InterruptedException;

    /**
     * Determine the API URI path for a named API.
     *
     * @param apiName Name of the API
     * @return The URI for that API
     * @throws HiroException        When the request fails.
     * @throws IOException          When the connection fails.
     * @throws InterruptedException When interrupted.
     */
    URI getApiUriOf(String apiName) throws IOException, InterruptedException, HiroException;
}
