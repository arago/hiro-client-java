package co.arago.hiro.client.connection.token;

import co.arago.hiro.client.connection.AbstractAPIHandler;
import co.arago.hiro.client.exceptions.AuthenticationTokenException;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.exceptions.HiroHttpException;
import co.arago.hiro.client.exceptions.TokenUnauthorizedException;
import co.arago.hiro.client.model.VersionResponse;
import co.arago.hiro.client.model.token.DecodedToken;
import co.arago.hiro.client.util.httpclient.HttpHeaderMap;
import co.arago.util.json.JsonUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

public interface TokenAPIHandler extends AutoCloseable {
    /**
     * Return a copy of the configuration.
     *
     * @return A copy of the configuration.
     * @implNote Please take note, that the included httpClientHandler of this class will be
     *           added to the returned {@link AbstractAPIHandler.Conf} and therefore will be shared among all
     *           APIHandlers that use this configuration.
     * @see co.arago.hiro.client.rest.AuthenticatedAPIHandler
     */
    AbstractAPIHandler.Conf<?> getConf();

    /**
     * Override this to add authentication tokens. TokenHandlers do not have tokens, so this only returns default
     * headers.
     *
     * @param headers Map of headers with initial values.
     */
    void addToHeaders(HttpHeaderMap headers);

    /**
     * Return the current token.
     *
     * @return The current token.
     * @throws InterruptedException When call gets interrupted.
     * @throws IOException          When call has IO errors.
     * @throws HiroException        On Hiro protocol / handling errors.
     */
    String getToken() throws IOException, InterruptedException, HiroException;

    /**
     * Decode the payload part of the internal token.
     *
     * @return Decoded token as {@link DecodedToken}.
     * @throws InterruptedException When call gets interrupted.
     * @throws IOException          When call has IO errors.
     * @throws HiroException        On Hiro protocol / handling errors.
     */
    DecodedToken decodeToken() throws HiroException, IOException, InterruptedException;

    /**
     * Refresh an invalid token.
     *
     * @throws InterruptedException When call gets interrupted.
     * @throws IOException          When call has IO errors.
     * @throws HiroException        On Hiro protocol / handling errors.
     */
    void refreshToken() throws IOException, InterruptedException, HiroException;

    /**
     * Revoke a token
     *
     * @throws InterruptedException When call gets interrupted.
     * @throws IOException          When call has IO errors.
     * @throws HiroException        On Hiro protocol / handling errors.
     */
    void revokeToken() throws IOException, InterruptedException, HiroException;

    /**
     * Check for existence of a token in the TokenAPIHandler.
     *
     * @return true if a token has been set or retrieved, false if the token is empty.
     */
    boolean hasToken();

    /**
     * Check for existence of a refresh token in the TokenAPIHandler.
     *
     * @return true if a refresh token retrieved, false if no such token exists or these tokens are not applicable for
     *         this TokenAPIHandler.
     */
    boolean hasRefreshToken();

    /**
     * Calculate the Instant after which the token should be refreshed.
     *
     * @return The Instant after which the token shall be refreshed. null if token cannot be refreshed.
     */
    Instant expiryInstant();

    /**
     * The default way to check responses for errors and extract error messages.
     *
     * @param httpResponse The httpResponse from the HttpRequest
     * @param retryCount   current counter for retries
     * @return true for a retry, false otherwise.
     * @throws TokenUnauthorizedException When the statusCode is 401.
     * @throws HiroHttpException          When the statusCode is &lt; 200 or &gt; 399.
     * @throws HiroException              On internal errors regarding hiro data processing.
     * @throws IOException                On IO errors.
     * @throws InterruptedException       When a call (possibly of an overwritten method) gets interrupted.
     */
    boolean checkResponse(HttpResponse<InputStream> httpResponse, int retryCount)
            throws HiroException, IOException, InterruptedException;

    /**
     * Determine the API URI path for a named API.
     *
     * @param apiName Name of the API
     * @return The URI for that API
     * @throws HiroException        When the request fails.
     * @throws IOException          When the connection fails.
     * @throws InterruptedException When interrupted.
     */

    URI getApiURIOf(String apiName) throws IOException, InterruptedException, HiroException;

    /**
     * Build a complete uri from the webSocketApi and path.
     *
     * @param path The path to append to webSocketURI.
     * @return The constructed URI
     */
    URI buildWebSocketURI(String path);

    /**
     * @return The HttpClient to use with this class.
     */
    HttpClient getOrBuildClient();

    /**
     * Returns the current versionMap. If no versionMap is available, it will be requested, cached
     * and then returned. if a sharedConnectionHandler is set, the versionMap will be obtained from there.
     *
     * @return The (cached) versionMap.
     * @throws HiroException        When the request fails.
     * @throws IOException          When the connection fails.
     * @throws InterruptedException When interrupted.
     */
    VersionResponse getVersionMap() throws HiroException, IOException, InterruptedException;

    /**
     * @return Root URI of the API.
     */
    URI getRootApiURI();

    String getUserAgent();

    /**
     * Close the underlying httpClientHandler.
     */
    @Override
    void close();

    /**
     * Decode the payload part of any token.
     *
     * @param token The token to decode.
     * @return Decoded token as {@link DecodedToken}.
     * @throws IOException   When call has IO errors.
     * @throws HiroException On Hiro protocol / handling errors.
     */
    static DecodedToken decodeToken(String token) throws HiroException, IOException {
        String[] data = token.split("\\.");

        if (data.length == 1)
            throw new AuthenticationTokenException("Token is missing base64 encoded data.", 500, token);

        String json = new String(Base64.getUrlDecoder().decode(data[1]), StandardCharsets.UTF_8);

        return JsonUtil.DEFAULT.toObject(json, DecodedToken.class);
    }

}
