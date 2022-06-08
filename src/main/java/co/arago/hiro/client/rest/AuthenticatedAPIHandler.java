package co.arago.hiro.client.rest;

import co.arago.hiro.client.connection.APIHandler;
import co.arago.hiro.client.connection.AbstractAPIHandler;
import co.arago.hiro.client.connection.token.TokenAPIHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.exceptions.HiroHttpException;
import co.arago.hiro.client.exceptions.TokenUnauthorizedException;
import co.arago.hiro.client.util.HttpLogger;
import co.arago.hiro.client.util.httpclient.HttpHeaderMap;
import co.arago.hiro.client.util.httpclient.URIPath;
import co.arago.hiro.client.util.httpclient.UriQueryMap;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;

public interface AuthenticatedAPIHandler extends APIHandler {
    /**
     * Construct an AbstractAPIHandler.GetterConf from the values of this Conf and the supplied tokenAPIHandler.
     * This ensures, that some values (apiUrl and userAgent) are always set via the tokenAPIHandler
     * and some others use default values from there (httpRequestTimeout and maxRetries) unless set
     * in the builder for this Handler.
     *
     * @param builder         The builder of this handler.
     * @param tokenAPIHandler The tokenApiHandler for this Handler.
     * @return An AbstractAPIHandler.GetterConf for the parent class.
     */
    static AbstractAPIHandler.GetterConf makeHandlerConf(AbstractAuthenticatedAPIHandler.Conf<?> builder,
            TokenAPIHandler tokenAPIHandler) {
        return new AbstractAPIHandler.GetterConf() {
            @Override
            public URL getApiUrl() {
                return tokenAPIHandler.getApiUrl();
            }

            @Override
            public URI getWebSocketUri() {
                return tokenAPIHandler.getWebSocketUri();
            }

            @Override
            public Long getHttpRequestTimeout() {
                return builder.getHttpRequestTimeout() != null ? builder.getHttpRequestTimeout()
                        : tokenAPIHandler.getHttpRequestTimeout();
            }

            @Override
            public int getMaxRetries() {
                return builder.getMaxRetries() > 0 ? builder.getMaxRetries() : tokenAPIHandler.getMaxRetries();
            }

            @Override
            public String getUserAgent() {
                return tokenAPIHandler.getUserAgent();
            }
        };
    }

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

    /**
     * Add Authorization.
     *
     * @param headers Map of headers with initial values.
     * @throws InterruptedException On interrupt when a token needs to be obtained.
     * @throws IOException          On IO Errors when a token needs to be obtained.
     * @throws HiroException        All other known errors when a token needs to be obtained.
     */
    void addToHeaders(HttpHeaderMap headers) throws InterruptedException, IOException, HiroException;

    /**
     * Checks for {@link TokenUnauthorizedException} and {@link HiroHttpException}
     * until maxRetries is exhausted.
     * Also tries to refresh the token on {@link TokenUnauthorizedException}.
     *
     * @param httpResponse The httpResponse from the HttpRequest
     * @param retryCount   current counter for retries
     * @return true for a retry, false otherwise.
     * @throws HiroException        If the check fails with a http status code error.
     * @throws IOException          When the refresh fails with an IO error.
     * @throws InterruptedException Call got interrupted.
     */
    boolean checkResponse(HttpResponse<InputStream> httpResponse, int retryCount)
            throws HiroException, IOException, InterruptedException;

    /**
     * Redirect the HttpLogger to the one provided in tokenAPIHandler.
     *
     * @return The HttpLogger to use with this class.
     */
    HttpLogger getHttpLogger();

    /**
     * Redirect the HttpClient to the one provided in tokenAPIHandler.
     *
     * @return The HttpClient to use with this class.
     */
    HttpClient getOrBuildClient();
}
