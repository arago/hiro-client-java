package co.arago.hiro.client.connection;

import co.arago.hiro.client.util.FixedTokenException;
import co.arago.hiro.client.util.HiroException;

import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Instant;

public class EnvironmentTokenAPIHandler extends AbstractTokenAPIHandler {

    public static String DEFAULT_NAME = "HIRO_TOKEN";

    private final String tokenEnv;

    /**
     * Constructor. Use {@link #DEFAULT_NAME} as environment variable.
     *
     * @param apiUrl The root URL for the HIRO API.
     */
    public EnvironmentTokenAPIHandler(String apiUrl) {
        super(apiUrl, "none", null, null);
        this.tokenEnv = DEFAULT_NAME;
    }

    /**
     * Constructor. Use {@link #DEFAULT_NAME} as environment variable.
     *
     * @param apiUrl The root URL for the HIRO API.
     * @param client Use a pre-defined {@link HttpClient}.
     */
    public EnvironmentTokenAPIHandler(String apiUrl, HttpClient client) {
        super(apiUrl, "none", null, client);
        this.tokenEnv = DEFAULT_NAME;
    }

    /**
     * Constructor
     *
     * @param apiUrl   The root URL for the HIRO API.
     * @param tokenEnv The fixed token.
     */
    public EnvironmentTokenAPIHandler(String apiUrl, String tokenEnv) {
        super(apiUrl, "none", null, null);
        this.tokenEnv = tokenEnv;
    }

    /**
     * Constructor
     *
     * @param apiUrl   The root URL for the HIRO API.
     * @param tokenEnv The fixed token.
     * @param client   Use a pre-defined {@link HttpClient}.
     */
    public EnvironmentTokenAPIHandler(String apiUrl, String tokenEnv, HttpClient client) {
        super(apiUrl, "none", null, client);
        this.tokenEnv = tokenEnv;
    }

    /**
     * Override this to add automatic token renewal if necessary.
     *
     * @param httpResponse The httpResponse from the HttpRequest
     * @param retry        the current state of retry. If this is set to false, false will also be returned.
     *                     (Ignored here)
     * @return always false.
     * @throws HiroException never thrown here.
     */
    @Override
    protected boolean checkResponse(HttpResponse<InputStream> httpResponse, boolean retry) throws HiroException {
        return false;
    }

    /**
     * Return the current token from the System environment.
     *
     * @return The current token.
     */
    @Override
    public String getToken() {
        return System.getenv(tokenEnv);
    }

    /**
     * Refresh an invalid token.
     */
    @Override
    public void refreshToken() {
        throw new FixedTokenException("Cannot change an environment token.", 500, null);
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
