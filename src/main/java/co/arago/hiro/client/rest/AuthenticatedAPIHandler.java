package co.arago.hiro.client.rest;

import co.arago.hiro.client.connection.APIHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.util.httpclient.URIPath;
import co.arago.hiro.client.util.httpclient.UriQueryMap;

import java.io.IOException;
import java.net.URI;

public interface AuthenticatedAPIHandler extends APIHandler {
    /**
     * Construct my URI with query parameters and fragment.
     * This method will query /api/version once to construct the URI unless apiPath is set.
     *
     * @param path     The path to append to the API path.
     * @param query    Query parameters for this URI. Can be null for no query parameters.
     * @param fragment The fragment to add to the URI.
     * @return The URI with query parameters and fragment.
     * @throws IOException          On a failed call to /api/version
     * @throws InterruptedException Call got interrupted
     * @throws HiroException        When calling /api/version responds with an error
     */
    URI getEndpointUri(URIPath path, UriQueryMap query, String fragment)
            throws IOException, InterruptedException, HiroException;

}
