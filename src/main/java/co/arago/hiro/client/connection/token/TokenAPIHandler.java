package co.arago.hiro.client.connection.token;

import co.arago.hiro.client.connection.VersionAPIHandler;
import co.arago.hiro.client.exceptions.AuthenticationTokenException;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.model.token.DecodedToken;
import co.arago.util.json.JsonUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

public interface TokenAPIHandler extends VersionAPIHandler {
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
     * Decode the payload part of the internal token.
     *
     * @return Decoded token as {@link DecodedToken}.
     * @throws InterruptedException When call gets interrupted.
     * @throws IOException          When call has IO errors.
     * @throws HiroException        On Hiro protocol / handling errors.
     */
    DecodedToken decodeToken() throws HiroException, IOException, InterruptedException;

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
