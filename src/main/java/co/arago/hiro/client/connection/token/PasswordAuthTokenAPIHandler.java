package co.arago.hiro.client.connection.token;

import co.arago.hiro.client.exceptions.AuthenticationTokenException;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.exceptions.HiroHttpException;
import co.arago.hiro.client.exceptions.TokenUnauthorizedException;
import co.arago.hiro.client.model.token.TokenRefreshRequest;
import co.arago.hiro.client.model.token.TokenRequest;
import co.arago.hiro.client.model.token.TokenResponse;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class PasswordAuthTokenAPIHandler extends AbstractTokenAPIHandler {

    final static Logger log = LoggerFactory.getLogger(PasswordAuthTokenAPIHandler.class);

    // ###############################################################################################
    // ## Conf and Builder ##
    // ###############################################################################################

    public static abstract class Conf<T extends Conf<T>> extends AbstractTokenAPIHandler.Conf<T> {
        private String username;
        private String password;
        private String clientId;
        private String clientSecret;
        private Long refreshOffset = 5000L;
        private Long refreshPause = 0L;
        private boolean forceLogging = false;
        private String endpoint;

        public String getUsername() {
            return username;
        }

        /**
         * @param username HIRO username for user account
         * @return {@link #self()}
         */
        public T setUsername(String username) {
            this.username = username;
            return self();
        }

        public String getPassword() {
            return password;
        }

        /**
         * @param password HIRO password for user account
         * @return {@link #self()}
         */
        public T setPassword(String password) {
            this.password = password;
            return self();
        }

        public String getClientId() {
            return clientId;
        }

        /**
         * @param clientId HIRO client_id of app
         * @return {@link #self()}
         */
        public T setClientId(String clientId) {
            this.clientId = clientId;
            return self();
        }

        public String getClientSecret() {
            return clientSecret;
        }

        /**
         * @param clientSecret HIRO client_secret of app
         * @return {@link #self()}
         */
        public T setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
            return self();
        }

        /**
         * Shorthand to set all credentials at once.
         *
         * @param username     HIRO username for user account
         * @param password     HIRO password for user account
         * @param clientId     HIRO client_id of app
         * @param clientSecret HIRO client_secret of app
         * @return {@link #self()}
         */
        public T setCredentials(String username, String password, String clientId, String clientSecret) {
            setUsername(username);
            setPassword(password);
            setClientId(clientId);
            setClientSecret(clientSecret);
            return self();
        }

        public Long getRefreshOffset() {
            return refreshOffset;
        }

        /**
         * Timespan that gets subtracted from the official expiration instant of a token so the token can be refreshed
         * before it runs out. Default is 5000 (5s).
         *
         * @param refreshOffset Offset in ms
         * @return {@link #self()}
         */
        public T setRefreshOffset(Long refreshOffset) {
            this.refreshOffset = refreshOffset;
            return self();
        }

        public Long getRefreshPause() {
            return refreshPause;
        }

        /**
         * Timespan where calls to refresh the token will be ignored and only the current token will be returned. Avoids
         * refresh floods that can happen with multiple threads using the same TokenAPIHandler. Default is 30000 (30s).
         *
         * @param refreshPause Buffer span in ms
         * @return {@link #self()}
         */
        public T setRefreshPause(Long refreshPause) {
            this.refreshPause = refreshPause;
            return self();
        }

        public boolean getForceLogging() {
            return forceLogging;
        }

        /**
         * Force logging of insecure request / response data. USE ONLY FOR DEBUGGING PURPOSES!!!
         *
         * @param forceLogging the flag to set.
         * @return {@link #self()}
         */
        public T setForceLogging(boolean forceLogging) {
            this.forceLogging = forceLogging;
            return self();
        }

        public String getEndpoint() {
            return endpoint;
        }

        /**
         * @param endpoint The endpoint set externally. Overrides the fetching of the endpoint via /api/version.
         * @return {@link #self()}
         */
        public T setEndpoint(String endpoint) {
            this.endpoint = endpoint;
            return self();
        }
    }

    public static final class Builder extends Conf<Builder> {

        @Override
        protected Builder self() {
            return this;
        }

        public PasswordAuthTokenAPIHandler build() {
            return new PasswordAuthTokenAPIHandler(this);
        }
    }

    // ###############################################################################################
    // ## Inner classes ##
    // ###############################################################################################

    /**
     * Extends the TokenResponse with additional data and operations.
     */
    protected static class TokenInfo extends TokenResponse {

        /**
         * ms of time, where no refresh calls are sent to the backend to avoid request flooding
         */
        public long refreshPause = 0;

        /**
         * Timestamp of when the token has been fetched
         */
        protected Instant lastUpdate;

        /**
         * ms of offset for token expiry
         */
        protected long refreshOffset = 5000;

        /**
         * Check for token expiration
         *
         * @return true if expired or no token is available, false otherwise.
         */
        public boolean tokenExpired() {
            return Instant.now().isAfter(expiryInstant());
        }

        /**
         * Check, whether the token has been renewed within the last {@link #refreshPause} ms.
         *
         * @return true if within the timespan, false otherwise.
         */
        public boolean tokenFresh() {
            return Instant.now().isBefore(lastUpdate.plus(refreshPause, ChronoUnit.MILLIS));
        }

        /**
         * Calculate the Instant after which the token should be refreshed (expiresAt minus the {@link #refreshOffset}).
         *
         * @return The Instant after which the token shall be refreshed. null if token cannot be refreshed.
         */
        public Instant expiryInstant() {
            if (expiresAt == null)
                return null;
            if (expiresAt - refreshOffset < 0)
                return Instant.MIN;
            return Instant.ofEpochMilli(expiresAt).minus(refreshOffset, ChronoUnit.MILLIS);
        }

        /**
         * Copy data from the response to me and set {@link #lastUpdate}.
         *
         * @param tokenResponse The TokenResponse.
         */
        public void parse(TokenResponse tokenResponse) {
            this.token = tokenResponse.token;
            this.refreshToken = tokenResponse.refreshToken;
            this.expiresAt = tokenResponse.expiresAt;
            this.identity = tokenResponse.identity;
            this.indentityId = tokenResponse.indentityId;
            this.application = tokenResponse.application;
            this.type = tokenResponse.type;

            this.lastUpdate = Instant.now();
        }

    }

    // ###############################################################################################
    // ## Main part ##
    // ###############################################################################################

    protected final String apiName = "auth";
    protected final String username;
    protected final String password;
    protected final String clientId;
    protected final String clientSecret;
    protected final String endpoint;

    protected final TokenInfo tokenInfo = new TokenInfo();

    protected URI apiUri;


    protected PasswordAuthTokenAPIHandler(Conf<?> builder) {
        super(builder);
        this.username = notBlank(builder.getUsername(), "username");
        this.password = notBlank(builder.getPassword(), "password");
        this.clientId = notBlank(builder.getClientId(), "clientId");
        this.clientSecret = notBlank(builder.getClientSecret(), "clientSecret");
        this.endpoint = builder.getEndpoint();

        this.tokenInfo.refreshOffset = builder.getRefreshOffset();
        this.tokenInfo.refreshPause = builder.getRefreshPause();

        if (!builder.getForceLogging()) {
            try {
                httpLogger.addFilter(getUri("app"));
                httpLogger.addFilter(getUri("refresh"));
                httpLogger.addFilter(getUri("revoke"));
            } catch (IOException | InterruptedException | HiroException e) {
                log.error("Cannot get endpoint URI. Disable logging of http bodies.", e);
                httpLogger.setLogBody(false);
            }
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public long getRefreshOffset() {
        return tokenInfo.refreshOffset;
    }

    /**
     * @param refreshOffset ms of offset for token expiry. This is subtracted from the instant given via "expires-at".
     */
    public void setRefreshOffset(long refreshOffset) {
        this.tokenInfo.refreshOffset = refreshOffset;
    }

    public long getRefreshPause() {
        return tokenInfo.refreshPause;
    }

    /**
     * @param refreshPause ms of time, where no refresh calls are sent to the backend to avoid request flooding
     */
    public void setRefreshPause(long refreshPause) {
        this.tokenInfo.refreshPause = refreshPause;
    }

    /**
     * Construct my URI.
     * This method will query /api/version once to construct the URI unless {@link #endpoint} is set.
     *
     * @param path The path to append to the API path.
     * @return The URI without query or fragment.
     * @throws IOException          On a failed call to /api/version
     * @throws InterruptedException Call got interrupted
     * @throws HiroException        When calling /api/version responds with an error
     */
    public URI getUri(String path) throws IOException, InterruptedException, HiroException {
        if (apiUri == null)
            apiUri = (endpoint != null ? buildApiURI(endpoint) : getApiUriOf(apiName));

        return apiUri.resolve(RegExUtils.removePattern(path, "^/+"));
    }

    /**
     * Check for token expiration
     *
     * @return true if expired or no token is available, false otherwise.
     */
    public synchronized boolean tokenExpired() {
        return tokenInfo.tokenExpired();
    }

    /**
     * Check, whether the token has been renewed within the last {@link TokenInfo#refreshPause} ms.
     *
     * @return true if within the timespan, false otherwise.
     */
    public synchronized boolean tokenFresh() {
        return tokenInfo.tokenFresh();
    }

    /**
     * Calculate the Instant after which the token should be refreshed (expiresAt minus the {@link TokenInfo#refreshOffset}).
     *
     * @return The Instant after which the token shall be refreshed. null if token cannot be refreshed.
     */
    public synchronized Instant expiryInstant() {
        return tokenInfo.expiryInstant();
    }

    /**
     * Check for the kind of error. 401 throws TokenUnauthorizedException, all other
     * throw AuthenticationTokenException.
     *
     * @param httpResponse The httpResponse from the HttpRequest
     * @param retryCount   current counter for retries
     * @return true for retry, false otherwise.
     * @throws TokenUnauthorizedException   On error 401 immediately.
     * @throws AuthenticationTokenException On all other status errors when no retries are left.
     * @throws IOException                  When reading the inputStream fails.
     */
    @Override
    public boolean checkResponse(HttpResponse<InputStream> httpResponse, int retryCount) throws HiroException, IOException, InterruptedException {
        try {
            super.checkResponse(httpResponse, retryCount);
        } catch (TokenUnauthorizedException e) {
            throw e;
        } catch (HiroHttpException e) {
            throw new AuthenticationTokenException(e.getMessage(), e.getCode(), e.getBody(), e);
        }

        return false;
    }

    /**
     * Return the current token.
     *
     * @return The current token.
     * @throws InterruptedException When call gets interrupted.
     * @throws IOException          When call has IO errors.
     * @throws HiroException        On Hiro protocol / handling errors.
     */
    @Override
    public synchronized String getToken() throws IOException, InterruptedException, HiroException {
        if (!hasToken()) {
            requestToken();
        } else if (tokenExpired()) {
            refreshToken();
        }

        return tokenInfo.token;
    }

    /**
     * Obtain a new token from the auth API.
     *
     * @throws InterruptedException When call gets interrupted.
     * @throws IOException          When call has IO errors.
     * @throws HiroException        On Hiro protocol / handling errors.
     */
    protected synchronized void requestToken() throws IOException, InterruptedException, HiroException {

        TokenRequest tokenRequest = new TokenRequest(username, password, clientId, clientSecret);

        this.tokenInfo.parse(
                post(
                        TokenResponse.class,
                        getUri("app"),
                        tokenRequest.toJsonString(),
                        Map.of("Content-Type", "application/json"),
                        httpRequestTimeout,
                        maxRetries
                )
        );
    }

    /**
     * Refresh an invalid token.
     */
    @Override
    public synchronized void refreshToken() throws IOException, InterruptedException, HiroException {

        if (!hasRefreshToken()) {
            requestToken();
            return;
        }

        if (tokenFresh()) {
            return;
        }

        TokenRefreshRequest tokenRequest = new TokenRefreshRequest(clientId, clientSecret, tokenInfo.refreshToken);

        this.tokenInfo.parse(
                post(
                        TokenResponse.class,
                        getUri("refresh"),
                        tokenRequest.toJsonString(),
                        Map.of("Content-Type", "application/json"),
                        httpRequestTimeout,
                        maxRetries
                )
        );
    }

    /**
     * Revoke a token. This only works if a refreshToken is available.
     */
    @Override
    public synchronized void revokeToken() throws IOException, InterruptedException, HiroException {
        if (!hasToken() || !hasRefreshToken()) {
            return;
        }

        TokenRefreshRequest tokenRequest = new TokenRefreshRequest(clientId, clientSecret, tokenInfo.refreshToken);

        this.tokenInfo.parse(
                post(
                        TokenResponse.class,
                        getUri("revoke"),
                        tokenRequest.toJsonString(),
                        Map.of(
                                "Content-Type", "application/json",
                                "Authorization", "Bearer " + getToken()
                        ),
                        httpRequestTimeout,
                        maxRetries
                )
        );
    }

    /**
     * Check for existence of a token in the TokenAPIHandler.
     *
     * @return true if a token has been set or retrieved, false if the token is empty.
     */
    @Override
    public synchronized boolean hasToken() {
        return StringUtils.isNotBlank(tokenInfo.token);
    }

    /**
     * Check for existence of a refresh token in the TokenAPIHandler.
     *
     * @return true if a refresh token retrieved, false otherwise.
     */
    @Override
    public boolean hasRefreshToken() {
        return StringUtils.isNotBlank(tokenInfo.refreshToken);
    }

}
