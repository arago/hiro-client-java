package co.arago.hiro.client.connection;

import co.arago.hiro.client.model.*;
import co.arago.hiro.client.util.AuthenticationTokenException;
import co.arago.hiro.client.util.HiroException;
import co.arago.hiro.client.util.JsonTools;
import co.arago.hiro.client.util.TokenUnauthorizedException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class PasswordAuthTokenAPIHandler extends AbstractTokenAPIHandler {

    final Logger log = LoggerFactory.getLogger(PasswordAuthTokenAPIHandler.class);

    public static class TokenInfo {
        /**
         * The token string
         */
        public String token;
        /**
         * Token expiration
         */
        public Instant expiresAt;
        /**
         * Refresh token to use - if any
         */
        public String refreshToken;
        /**
         * Timestamp of when the token has been fetched
         */
        public Instant lastUpdate;
        /**
         * ms of offset for token expiry
         */
        public Long refreshOffset = 5000L;

        /**
         * ms of time, where no refresh calls are sent to the backend to avoid request flooding
         */
        public Long freshBuffer = 30000L;

        public TokenInfo setRefreshOffset(Long refreshOffset) {
            this.refreshOffset = refreshOffset;
            return this;
        }

        public TokenInfo setFreshBuffer(Long freshBuffer) {
            this.freshBuffer = freshBuffer;
            return this;
        }

        /**
         * Get values from a {@link TokenResponse}. Also set {@link #lastUpdate}.
         *
         * @param response The TokenResponse
         */
        public void handleResponse(TokenResponse response) {
            token = response.getToken();
            expiresAt = Instant.ofEpochMilli(response.getExpiresAt());
            refreshToken = response.getRefreshToken();
            lastUpdate = Instant.now();
        }

        /**
         * Check for token expiration
         *
         * @return true if expired, false otherwise.
         */
        public boolean expired() {
            return Instant.now().isAfter(expiryInstant());
        }

        /**
         * Check, whether the token has been renewed within the last {@link #freshBuffer} ms.
         *
         * @return true if within the timespan, false otherwise.
         */
        public boolean fresh() {
            return Instant.now().isBefore(lastUpdate.plus(freshBuffer, ChronoUnit.MILLIS));
        }

        /**
         * Calculate the expiresAt minus the {@link #refreshOffset}.
         *
         * @return The modified Instant.
         */
        public Instant expiryInstant() {
            return expiresAt.minus(refreshOffset, ChronoUnit.MILLIS);
        }
    }

    public final String username;
    public final String password;
    public final String clientId;
    public final String clientSecret;

    protected TokenInfo tokenInfo = new TokenInfo();

    /**
     * Constructor
     *
     * @param apiUrl       The root URL for the HIRO API.
     * @param username     HIRO Username
     * @param password     HIRO Password
     * @param clientId     HIRO APP clientId
     * @param clientSecret HIRO APP clientSecret
     * @throws IOException          When the endpoint cannot be determined via API call.
     * @throws InterruptedException When the API call gets interrupted.
     */
    public PasswordAuthTokenAPIHandler(
            String apiUrl,
            String username,
            String password,
            String clientId,
            String clientSecret
    ) throws IOException, InterruptedException {
        this(apiUrl, username, password, clientId, clientSecret, (HttpClient) null);
    }

    /**
     * Constructor
     *
     * @param apiUrl       The root URL for the HIRO API.
     * @param username     HIRO Username
     * @param password     HIRO Password
     * @param clientId     HIRO APP clientId
     * @param clientSecret HIRO APP clientSecret
     */
    public PasswordAuthTokenAPIHandler(
            String apiUrl,
            String username,
            String password,
            String clientId,
            String clientSecret,
            String endpoint
    ) {
        this(apiUrl, username, password, clientId, clientSecret, endpoint, null);
    }

    /**
     * Constructor
     *
     * @param apiUrl       The root URL for the HIRO API.
     * @param username     HIRO Username
     * @param password     HIRO Password
     * @param clientId     HIRO APP clientId
     * @param clientSecret HIRO APP clientSecret
     * @param client       Use a pre-defined {@link HttpClient}.
     * @throws IOException          When the endpoint cannot be determined via API call.
     * @throws InterruptedException When the API call gets interrupted.
     */
    public PasswordAuthTokenAPIHandler(
            String apiUrl,
            String username,
            String password,
            String clientId,
            String clientSecret,
            HttpClient client
    ) throws IOException, InterruptedException {
        super(apiUrl, "auth", client);
        this.username = username;
        this.password = password;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    /**
     * Constructor
     *
     * @param apiUrl       The root URL for the HIRO API.
     * @param username     HIRO Username
     * @param password     HIRO Password
     * @param clientId     HIRO APP clientId
     * @param clientSecret HIRO APP clientSecret
     * @param endpoint     Externally provided endpoint URI for token requests.
     * @param client       Use a pre-defined {@link HttpClient}.
     */
    public PasswordAuthTokenAPIHandler(
            String apiUrl,
            String username,
            String password,
            String clientId,
            String clientSecret,
            String endpoint,
            HttpClient client
    ) {
        super(apiUrl, "auth", endpoint, client);
        this.username = username;
        this.password = password;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    /**
     * @param refreshOffset ms of offset for token expiry. This is subtracted from the instant given via "expires-at".
     * @param freshBuffer   ms of time, where no refresh calls are sent to the backend to avoid request flooding
     * @return this
     */
    public PasswordAuthTokenAPIHandler setTokenRefreshOptions(Long refreshOffset, Long freshBuffer) {
        this.tokenInfo.setRefreshOffset(refreshOffset).setFreshBuffer(freshBuffer);
        return this;
    }

    /**
     * Check for the kind of error. 401 throws TokenUnauthorizedExcetion, all other throw AuthenticationTokenException.
     *
     * @param httpResponse The httpResponse from the HttpRequest
     * @param retry        the current state of retry. If this is set to false, false will also be returned.
     *                     (Ignored here)
     * @return always false.
     * @throws TokenUnauthorizedException   On error 401
     * @throws AuthenticationTokenException On all other status errors
     * @throws HiroException                On IO errors with the inputstream
     */
    @Override
    protected boolean checkResponse(HttpResponse<InputStream> httpResponse, boolean retry) throws HiroException {
        int statusCode = httpResponse.statusCode();

        if (statusCode < 200 || statusCode > 399) {
            try {
                TokenErrorResponse errorResponse = TokenErrorResponse.fromInputStream(httpResponse.body());

                if (errorResponse.getCode() == 401) {
                    throw new TokenUnauthorizedException(errorResponse.getMessage(), errorResponse.getCode(), null);
                } else {
                    throw new AuthenticationTokenException(errorResponse.getMessage(), errorResponse.getCode(), null);
                }
            } catch (IOException e) {
                throw new HiroException("Internal error", 500, null, e);
            }
        }

        return false;
    }

    /**
     * Return the current token.
     *
     * @return The current token.
     */
    @Override
    public synchronized String getToken() throws IOException, InterruptedException {
        if (StringUtils.isBlank(tokenInfo.token)) {
            requestToken();
        } else if (tokenInfo.expired()) {
            refreshToken();
        }

        return tokenInfo.token;
    }

    /**
     * Obtain a new token from the auth API.
     */
    protected void requestToken() throws IOException, InterruptedException {

        TokenRequest tokenRequest = new TokenRequest(username, password, clientId, clientSecret);

        HiroResponse hiroResponse = post(
                getEndpointUri().resolve("app"),
                Map.of("Content-Type", "application/json"),
                JsonTools.DEFAULT.toString(tokenRequest));

        tokenInfo.handleResponse(TokenResponse.fromResponse(hiroResponse));
    }

    /**
     * Refresh an invalid token.
     */
    @Override
    public synchronized void refreshToken() throws IOException, InterruptedException {

        if (StringUtils.isBlank(tokenInfo.refreshToken)) {
            requestToken();
            return;
        }

        if (tokenInfo.fresh()) {
            return;
        }

        TokenRefreshRequest tokenRequest = new TokenRefreshRequest(clientId, clientSecret, tokenInfo.refreshToken);

        HiroResponse hiroResponse = post(
                getEndpointUri().resolve("refresh"),
                Map.of("Content-Type", "application/json"),
                JsonTools.DEFAULT.toString(tokenRequest));

        tokenInfo.handleResponse(TokenResponse.fromResponse(hiroResponse));
    }

    /**
     * Calculate the Instant after which the token should be refreshed.
     *
     * @return The Instant after which the token shall be refreshed. null if token cannot be refreshed.
     */
    @Override
    public synchronized Instant expiryInstant() {
        return tokenInfo.expiryInstant();
    }
}
