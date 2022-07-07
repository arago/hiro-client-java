package co.arago.hiro.client.connection.token;

import co.arago.hiro.client.connection.AbstractVersionAPIHandler;
import co.arago.hiro.client.exceptions.AuthenticationTokenException;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.exceptions.HiroHttpException;
import co.arago.hiro.client.exceptions.TokenUnauthorizedException;
import co.arago.hiro.client.model.token.TokenRefreshRequest;
import co.arago.hiro.client.model.token.TokenResponse;
import co.arago.hiro.client.model.token.TokenRevokeRequest;
import co.arago.hiro.client.util.httpclient.HttpHeaderMap;
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

import static co.arago.util.validation.RequiredFieldChecks.notBlank;

public abstract class AbstractRemoteAuthTokenAPIHandler extends AbstractTokenAPIHandler {

    final static Logger log = LoggerFactory.getLogger(AbstractRemoteAuthTokenAPIHandler.class);

    // ###############################################################################################
    // ## Conf and Builder ##
    // ###############################################################################################

    public static abstract class Conf<T extends Conf<T>> extends AbstractTokenAPIHandler.Conf<T> {
        private String organization;
        private String organizationId;
        private String clientId;
        private String clientSecret;
        private Long refreshOffset = 5000L;
        private boolean forceLogging = false;
        private String apiPath;

        public String getOrganization() {
            return organization;
        }

        /**
         * @param organization Optional HIRO organization name for returned token
         * @return {@link #self()}
         */
        public T setOrganization(String organization) {
            this.organization = organization;
            return self();
        }

        public String getOrganizationId() {
            return organizationId;
        }

        /**
         * @param organizationId Optional HIRO organization id for returned token. Overrides organziation field.
         * @return {@link #self()}
         */
        public T setOrganizationId(String organizationId) {
            this.organizationId = organizationId;
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
            setOrganization(username);
            setOrganizationId(password);
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

        public String getApiPath() {
            return apiPath;
        }

        /**
         * @param apiPath The apiPath set externally. Overrides the fetching of the endpoint via /api/version.
         * @return {@link #self()}
         */
        public T setApiPath(String apiPath) {
            this.apiPath = apiPath;
            return self();
        }

        public abstract AbstractRemoteAuthTokenAPIHandler build();
    }

    // ###############################################################################################
    // ## Inner classes ##
    // ###############################################################################################

    /**
     * Extends the TokenResponse with additional data and operations.
     */
    protected static class TokenInfo extends TokenResponse {

        private static final long serialVersionUID = 8659946445124247439L;

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
         * Calculate the Instant after which the token should be refreshed (expiresAt minus the {@link #refreshOffset}).
         *
         * @return The Instant after which the token shall be refreshed. null if token cannot be refreshed.
         */
        public Instant expiryInstant() {
            if (expiresAt() - refreshOffset < 0)
                return Instant.MIN;
            return Instant.ofEpochMilli(expiresAt()).minus(refreshOffset, ChronoUnit.MILLIS);
        }

        public Long expiresAt() {
            if (expiresAt == null && expiresIn == null)
                return Long.MAX_VALUE;

            return expiresAt == null ? lastUpdate.toEpochMilli() + expiresIn * 1000 : expiresAt;
        }

        /**
         * Copy data from the response to me and set {@link #lastUpdate}.
         *
         * @param tokenResponse The TokenResponse.
         */
        public void parse(TokenResponse tokenResponse) {
            this.token = tokenResponse.token;
            this.expiresIn = tokenResponse.expiresIn;
            this.expiresAt = tokenResponse.expiresAt;
            this.identity = tokenResponse.identity;
            this.identityId = tokenResponse.identityId;
            this.application = tokenResponse.application;
            this.type = tokenResponse.type;

            // Keep refresh token until it is set anew in the response.
            if (StringUtils.isNotBlank(tokenResponse.refreshToken))
                this.refreshToken = tokenResponse.refreshToken;

            this.lastUpdate = Instant.now();
        }

    }

    // ###############################################################################################
    // ## Main part ##
    // ###############################################################################################

    protected final String apiName = "auth";
    protected String organization;
    protected String organizationId;
    protected final String clientId;
    protected final String clientSecret;
    protected final String apiPath;

    protected final TokenInfo tokenInfo = new TokenInfo();

    protected URI apiUri;

    protected AbstractRemoteAuthTokenAPIHandler(Conf<?> builder) {
        super(builder);
        this.clientId = notBlank(builder.getClientId(), "clientId");
        this.clientSecret = builder.getClientSecret();
        this.organization = builder.getOrganization();
        this.organizationId = builder.getOrganizationId();
        this.apiPath = builder.getApiPath();

        this.tokenInfo.refreshOffset = builder.getRefreshOffset();

        if (!builder.getForceLogging()) {
            configureLogging();
        }
    }

    /**
     * Special Copy Constructor. Uses the connection of another existing AbstractVersionAPIHandler.
     *
     * @param versionAPIHandler The AbstractVersionAPIHandler with the source data.
     * @param builder           Only configuration specific to this TokenAPIHandler, see {@link Conf}, will
     *                          be copied from the builder. The AbstractVersionAPIHandler overwrites everything else.
     */
    protected AbstractRemoteAuthTokenAPIHandler(
            AbstractVersionAPIHandler versionAPIHandler,
            Conf<?> builder) {
        super(versionAPIHandler);
        this.clientId = notBlank(builder.getClientId(), "clientId");
        this.clientSecret = builder.getClientSecret();
        this.organization = builder.getOrganization();
        this.organizationId = builder.getOrganizationId();
        this.apiPath = builder.getApiPath();

        this.tokenInfo.refreshOffset = builder.getRefreshOffset();

        if (!builder.getForceLogging()) {
            configureLogging();
        }
    }

    private void configureLogging() {
        try {
            httpLogger.addFilter(getUri("token"));
            httpLogger.addFilter(getUri("app"));
            httpLogger.addFilter(getUri("refresh"));
            httpLogger.addFilter(getUri("revoke"));
        } catch (IOException | InterruptedException | HiroException e) {
            log.error("Cannot get apiPath URI. Disable logging of http bodies.", e);
            httpLogger.setLogBody(false);
        }
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

    /**
     * Construct my URI.
     * This method will query /api/version once to construct the URI unless {@link #apiPath} is set.
     *
     * @param path The path to append to the API path.
     * @return The URI without query or fragment.
     * @throws IOException          On a failed call to /api/version
     * @throws InterruptedException Call got interrupted
     * @throws HiroException        When calling /api/version responds with an error
     */
    public URI getUri(String path) throws IOException, InterruptedException, HiroException {
        if (apiUri == null)
            apiUri = (apiPath != null ? buildApiURI(apiPath) : getApiUriOf(apiName));

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
    public boolean checkResponse(HttpResponse<InputStream> httpResponse, int retryCount)
            throws HiroException, IOException, InterruptedException {
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
            requestToken(this.organization, this.organizationId);
        } else if (tokenExpired()) {
            refreshToken(this.organization, this.organizationId);
        }

        return tokenInfo.token;
    }

    /**
     * @return The refresh token or null if no token data or refresh token exists.
     */
    public String getRefreshToken() {
        return tokenInfo.refreshToken;
    }

    /**
     * Obtain a new token from the auth API.
     *
     * @param organization:   Optional organization name. May be null.
     * @param organizationId: Optional organization id. May be null.
     * @throws InterruptedException When call gets interrupted.
     * @throws IOException          When call has IO errors.
     * @throws HiroException        On Hiro protocol / handling errors.
     */
    protected abstract void requestToken(
            String organization,
            String organizationId) throws IOException, InterruptedException, HiroException;

    /**
     * Refresh an invalid token.
     * <p>
     * Uses the internal values of organization and organizationId if present.
     *
     * @throws InterruptedException When call gets interrupted.
     * @throws IOException          When call has IO errors.
     * @throws HiroException        On Hiro protocol / handling errors.
     */
    public synchronized void refreshToken() throws HiroException, IOException, InterruptedException {
        refreshToken(this.organization, this.organizationId);
    }

    /**
     * Refresh an invalid token.
     *
     * @param organization:   Optional organization name. May be null.
     * @param organizationId: Optional organization id. May be null.
     * @throws InterruptedException When call gets interrupted.
     * @throws IOException          When call has IO errors.
     * @throws HiroException        On Hiro protocol / handling errors.
     */
    public synchronized void refreshToken(
            String organization,
            String organizationId) throws IOException, InterruptedException, HiroException {

        try {

            if (!hasRefreshToken()) {
                requestToken(organization, organizationId);
                return;
            }

            if (organization != null)
                this.organization = organization;
            if (organizationId != null)
                this.organizationId = organizationId;

            float authApiVersion = Float.parseFloat(getVersionMap().getVersionEntryOf("auth").version);

            TokenResponse tokenResponse;

            TokenRefreshRequest tokenRequest = new TokenRefreshRequest(
                    clientId,
                    clientSecret,
                    tokenInfo.refreshToken,
                    organization,
                    organizationId);

            if (authApiVersion >= 6.6f) {
                tokenResponse = post(
                        TokenResponse.class,
                        getUri("token"),
                        tokenRequest.toUriEncodedStringRemoveBlanks(),
                        new HttpHeaderMap(Map.of("Content-Type", "application/x-www-form-urlencoded")),
                        httpRequestTimeout,
                        maxRetries);
            } else {
                tokenResponse = post(
                        TokenResponse.class,
                        getUri("refresh"),
                        tokenRequest.toJsonStringNoNull(),
                        new HttpHeaderMap(Map.of("Content-Type", "application/json")),
                        httpRequestTimeout,
                        maxRetries);
            }

            this.tokenInfo.parse(tokenResponse);
        } catch (AuthenticationTokenException e) {
            log.debug("Request new access_token");
            requestToken(organization, organizationId);
        }
    }

    /**
     * Revoke a token. This only works if a refreshToken is available.
     *
     * @throws InterruptedException When call gets interrupted.
     * @throws IOException          When call has IO errors.
     * @throws HiroException        On Hiro protocol / handling errors.
     */
    @Override
    public synchronized void revokeToken() throws IOException, InterruptedException, HiroException {
        revokeToken("refresh_token");
    }

    /**
     * Revoke a token.
     *
     * @param tokenHint The type of token to revoke. Valid are "refresh_token" and "access_token".
     * @throws InterruptedException       When call gets interrupted.
     * @throws IOException                When call has IO errors.
     * @throws HiroException              On Hiro protocol / handling errors.
     * @throws TokenUnauthorizedException When neither access_token nor refresh_token exist.
     */
    public synchronized void revokeToken(String tokenHint) throws IOException, InterruptedException, HiroException {
        if (!hasToken() || !hasRefreshToken()) {
            throw new TokenUnauthorizedException("no token provided", 401, null);
        }

        float authApiVersion = Float.parseFloat(getVersionMap().getVersionEntryOf("auth").version);

        TokenRevokeRequest tokenRequest;

        if (authApiVersion >= 6.6f) {
            tokenRequest = new TokenRevokeRequest(clientId, clientSecret, tokenInfo.refreshToken, tokenHint);
        } else {
            tokenRequest = new TokenRevokeRequest(clientId, clientSecret, tokenInfo.refreshToken);
        }

        TokenResponse tokenResponse = post(
                TokenResponse.class,
                getUri("revoke"),
                tokenRequest.toJsonStringNoNull(),
                new HttpHeaderMap(Map.of(
                        "Content-Type", "application/json",
                        "Authorization", "Bearer " + getToken())),
                httpRequestTimeout,
                maxRetries);

        this.tokenInfo.parse(tokenResponse);
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
