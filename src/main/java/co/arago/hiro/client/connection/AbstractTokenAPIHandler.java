package co.arago.hiro.client.connection;

import co.arago.hiro.client.exceptions.HiroException;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

public abstract class AbstractTokenAPIHandler extends AbstractVersionAPIHandler {

    public interface Conf extends AbstractVersionAPIHandler.Conf {
    }

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
     * @param headers Map of headers with initial values. Can be null to use only default headers.
     * @return The headers for this request.
     */
    @Override
    public Map<String, String> getHeaders(Map<String, String> headers) {
        return initializeHeaders(headers);
    }

    /**
     * Return the current token.
     *
     * @return The current token.
     */
    public abstract String getToken() throws IOException, InterruptedException, HiroException;

    /**
     * Refresh an invalid token.
     */
    public abstract void refreshToken() throws IOException, InterruptedException, HiroException;

    /**
     * Revoke a token
     */
    public abstract void revokeToken() throws IOException, InterruptedException, HiroException;

    /**
     * Calculate the Instant after which the token should be refreshed.
     *
     * @return The Instant after which the token shall be refreshed. null if token cannot be refreshed.
     */
    public abstract Instant expiryInstant();

}
