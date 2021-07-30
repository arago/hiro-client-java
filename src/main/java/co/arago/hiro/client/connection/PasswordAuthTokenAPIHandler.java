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
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class PasswordAuthTokenAPIHandler extends AbstractTokenAPIHandler {

    final Logger log = LoggerFactory.getLogger(PasswordAuthTokenAPIHandler.class);

    public static abstract class Conf<B extends Conf<B>> extends AbstractTokenAPIHandler.Conf<Builder> {
        protected String username;
        protected String password;
        protected String clientId;
        protected String clientSecret;
        protected Long refreshOffset = 5000L;
        protected Long freshBuffer = 30000L;

        /**
         * @param username HIRO username
         */
        public Builder setUsername(String username) {
            this.username = username;
            return self();
        }

        /**
         * @param password HIRO password
         */
        public Builder setPassword(String password) {
            this.password = password;
            return self();
        }

        /**
         * @param clientId HIRO client_id
         */
        public Builder setClientId(String clientId) {
            this.clientId = clientId;
            return self();
        }

        /**
         * @param clientSecret HIRO client_secret
         */
        public Builder setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
            return self();
        }

        /**
         * @param refreshOffset ms of offset for token expiry. This is subtracted from the instant given via "expires-at".
         */
        public Builder setRefreshOffset(Long refreshOffset) {
            this.refreshOffset = refreshOffset;
            return self();
        }

        /**
         * @param freshBuffer ms of time, where no refresh calls are sent to the backend to avoid request flooding
         */
        public Builder setFreshBuffer(Long freshBuffer) {
            this.freshBuffer = freshBuffer;
            return self();
        }

        @Override
        abstract PasswordAuthTokenAPIHandler build();
    }


    public static final class Builder extends Conf<Builder> {
        @Override
        Builder self() {
            return this;
        }

        @Override
        PasswordAuthTokenAPIHandler build() {
            this.apiName = (this.apiName != null ? this.apiName : "auth");
            return new PasswordAuthTokenAPIHandler(this);
        }
    }

    protected static class TokenInfo {
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

    public static Builder newBuilder() {
        return new Builder();
    }

    protected PasswordAuthTokenAPIHandler(Conf<?> builder) {
        super(builder);
        this.username = builder.username;
        this.password = builder.password;
        this.clientId = builder.clientId;
        this.clientSecret = builder.clientSecret;

        this.tokenInfo = new TokenInfo()
                .setFreshBuffer(builder.freshBuffer)
                .setRefreshOffset(builder.refreshOffset);
    }

    /**
     * @param refreshOffset ms of offset for token expiry. This is subtracted from the instant given via "expires-at".
     * @param freshBuffer   ms of time, where no refresh calls are sent to the backend to avoid request flooding
     */
    public void setTokenRefreshOptions(Long refreshOffset, Long freshBuffer) {
        this.tokenInfo.setRefreshOffset(refreshOffset).setFreshBuffer(freshBuffer);
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

        return super.checkResponse(httpResponse, retry);
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
