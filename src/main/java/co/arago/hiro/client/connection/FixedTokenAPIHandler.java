package co.arago.hiro.client.connection;

import co.arago.hiro.client.util.FixedTokenException;
import co.arago.hiro.client.util.HiroException;

import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Instant;

public class FixedTokenAPIHandler extends AbstractTokenAPIHandler {

    private final String token;

    /**
     * Constructor
     *
     * @param apiUrl The root URL for the HIRO API.
     * @param token  The fixed token.
     */
    public FixedTokenAPIHandler(String apiUrl, String token) {
        super(apiUrl, "none", null, null);
        this.token = token;
    }

    /**
     * Constructor
     *
     * @param apiUrl The root URL for the HIRO API.
     * @param token  The fixed token.
     * @param client Use a pre-defined {@link HttpClient}.
     */
    public FixedTokenAPIHandler(String apiUrl, String token, HttpClient client) {
        super(apiUrl, "none", null, client);
        this.token = token;
    }

    /**
     * Override this to add automatic token renewal if necessary.
     *
     * @param httpResponse The httpResponse from the HttpRequest
     * @param retry        the current state of retry. If this is set to false, false will also be returned.
     *                     (Ignored here)
     * @return always false.
     * @throws HiroException Never thrown here.
     */
    @Override
    protected boolean checkResponse(HttpResponse<InputStream> httpResponse, boolean retry) throws HiroException {
        return false;
    }

    /**
     * Return the current token.
     *
     * @return The current token.
     */
    @Override
    public String getToken() {
        return token;
    }

    /**
     * Refresh an invalid token.
     */
    @Override
    public void refreshToken() {
        throw new FixedTokenException("Cannot change a fixed token.", 500, null);
    }

    /**
     * Calculate the Instant after which the token should be refreshed.
     *
     * @return The Instant after which the token shall be refreshed. null if token cannot be refreshed.
     */
    @Override
    public Instant expiryInstant() {
        return null;
    }
}
