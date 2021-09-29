package co.arago.hiro.client.connection.token;

import co.arago.hiro.client.connection.AbstractVersionAPIHandler;
import co.arago.hiro.client.exceptions.AuthenticationTokenException;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.model.token.DecodedToken;
import co.arago.util.json.JsonUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

public abstract class AbstractTokenAPIHandler extends AbstractVersionAPIHandler {

    // ###############################################################################################
    // ## Conf and Builder ##
    // ###############################################################################################

    public static abstract class Conf<T extends Conf<T>> extends AbstractVersionAPIHandler.Conf<T> {
        @Override
        public abstract AbstractTokenAPIHandler build();
    }

    // ###############################################################################################
    // ## Main part ##
    // ###############################################################################################

    /**
     * Constructor
     *
     * @param builder The builder to use.
     */
    protected AbstractTokenAPIHandler(Conf builder) {
        super(builder);
    }

    /**
     * Override this to add authentication tokens. TokenHandlers do not have tokens, so this only returns default
     * headers.
     *
     * @param headers Map of headers with initial values.
     */
    @Override
    public void addToHeaders(Map<String, String> headers) {
        headers.put("User-Agent", userAgent);
    }

    /**
     * Return the current token.
     *
     * @return The current token.
     * @throws InterruptedException When call gets interrupted.
     * @throws IOException          When call has IO errors.
     * @throws HiroException        On Hiro protocol / handling errors.
     */
    public abstract String getToken() throws IOException, InterruptedException, HiroException;

    /**
     * Decode the payload part of a token.
     *
     * @return Decoded token as {@link DecodedToken}.
     * @throws InterruptedException When call gets interrupted.
     * @throws IOException          When call has IO errors.
     * @throws HiroException        On Hiro protocol / handling errors.
     */
    public DecodedToken decodeToken() throws HiroException, IOException, InterruptedException {
        String token = getToken();

        if (!StringUtils.contains(token, "."))
            throw new AuthenticationTokenException("Token is missing base64 encoded data.", 500, token);

        String data = token.split("\\.")[1];

        String json = new String(Base64.getUrlDecoder().decode(data), StandardCharsets.UTF_8);

        return JsonUtil.DEFAULT.toObject(json, DecodedToken.class);
    }

    /**
     * Refresh an invalid token.
     *
     * @throws InterruptedException When call gets interrupted.
     * @throws IOException          When call has IO errors.
     * @throws HiroException        On Hiro protocol / handling errors.
     */
    public abstract void refreshToken() throws IOException, InterruptedException, HiroException;

    /**
     * Revoke a token
     *
     * @throws InterruptedException When call gets interrupted.
     * @throws IOException          When call has IO errors.
     * @throws HiroException        On Hiro protocol / handling errors.
     */
    public abstract void revokeToken() throws IOException, InterruptedException, HiroException;

    /**
     * Check for existence of a token in the TokenAPIHandler.
     *
     * @return true if a token has been set or retrieved, false if the token is empty.
     */
    public abstract boolean hasToken();

    /**
     * Check for existence of a refresh token in the TokenAPIHandler.
     *
     * @return true if a refresh token retrieved, false if no such token exists or these tokens are not applicable for
     * this TokenAPIHandler.
     */
    public abstract boolean hasRefreshToken();

    /**
     * Calculate the Instant after which the token should be refreshed.
     *
     * @return The Instant after which the token shall be refreshed. null if token cannot be refreshed.
     */
    public abstract Instant expiryInstant();

}
