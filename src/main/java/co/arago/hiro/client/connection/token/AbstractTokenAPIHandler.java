package co.arago.hiro.client.connection.token;

import co.arago.hiro.client.connection.AbstractVersionAPIHandler;
import co.arago.hiro.client.exceptions.HiroException;

import java.io.IOException;
import java.time.Instant;
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
