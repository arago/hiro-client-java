package co.arago.hiro.client.connection;

import co.arago.hiro.client.exceptions.AuthenticationTokenException;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.exceptions.TokenUnauthorizedException;
import co.arago.hiro.client.model.HiroErrorResponse;
import co.arago.hiro.client.model.TokenRefreshRequest;
import co.arago.hiro.client.model.TokenRequest;
import co.arago.hiro.client.model.TokenResponse;
import co.arago.hiro.client.util.JsonTools;
import co.arago.hiro.client.util.RequiredFieldChecker;
import co.arago.hiro.client.util.httpclient.HttpResponseContainer;
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

    final Logger log = LoggerFactory.getLogger(PasswordAuthTokenAPIHandler.class);

    public static abstract class Conf<T extends Conf<T>> extends AbstractTokenAPIHandler.Conf<T> {
        private String username;
        private String password;
        private String clientId;
        private String clientSecret;
        private Long refreshOffset = 5000L;
        private Long refreshPause = 30000L;
        private boolean forceLogging = false;
        private String endpoint;

        public String getUsername() {
            return username;
        }

        /**
         * @param username HIRO username for user account
         * @return this
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
         * @return this
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
         * @return this
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
         * @return this
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
         * @return this
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
         * @return this
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
         * @return this
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
         * @return this
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
         * @return this
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
            RequiredFieldChecker.notBlank(getApiUrl(), "apiUrl");
            RequiredFieldChecker.notBlank(getUsername(), "username");
            RequiredFieldChecker.notBlank(getPassword(), "password");
            RequiredFieldChecker.notBlank(getClientId(), "clientId");
            RequiredFieldChecker.notBlank(getClientSecret(), "clientSecret");
            return new PasswordAuthTokenAPIHandler(this);
        }
    }

    protected final String apiName = "auth";
    protected final String username;
    protected final String password;
    protected final String clientId;
    protected final String clientSecret;
    protected final String endpoint;
    /**
     * ms of time, where no refresh calls are sent to the backend to avoid request flooding
     */
    public long refreshPause;
    /**
     * Timestamp of when the token has been fetched
     */
    protected Instant lastUpdate;
    /**
     * ms of offset for token expiry
     */
    protected long refreshOffset;
    /**
     * The last token response
     */
    protected TokenResponse tokenResponse;

    protected URI apiUri;


    protected PasswordAuthTokenAPIHandler(Conf<?> builder) {
        super(builder);
        this.username = builder.getUsername();
        this.password = builder.getPassword();
        this.clientId = builder.getClientId();
        this.clientSecret = builder.getClientSecret();
        this.refreshOffset = builder.getRefreshOffset();
        this.refreshPause = builder.getRefreshPause();
        this.endpoint = builder.getEndpoint();

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
        return refreshOffset;
    }

    /**
     * @param refreshOffset ms of offset for token expiry. This is subtracted from the instant given via "expires-at".
     */
    public void setRefreshOffset(long refreshOffset) {
        this.refreshOffset = refreshOffset;
    }

    public long getRefreshPause() {
        return refreshPause;
    }

    /**
     * @param refreshPause ms of time, where no refresh calls are sent to the backend to avoid request flooding
     */
    public void setRefreshPause(long refreshPause) {
        this.refreshPause = refreshPause;
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
            apiUri = (endpoint != null ? buildURI(endpoint) : getApiUriOf(apiName));

        return apiUri.resolve(StringUtils.startsWith(path, "/") ? path.substring(1) : path);
    }

    /**
     * Check for token expiration
     *
     * @return true if expired or no token is available, false otherwise.
     */
    public boolean tokenExpired() {
        if (tokenResponse != null)
            return Instant.now().isAfter(expiryInstant());
        return true;
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
        if (tokenResponse != null)
            return Instant.ofEpochMilli(tokenResponse.expiresAt).minus(refreshOffset, ChronoUnit.MILLIS);
        return null;
    }

    /**
     * Check for the kind of error. 401  throws TokenUnauthorizedException, all other
     * throw AuthenticationTokenException.
     *
     * @param httpResponse The httpResponse from the HttpRequest
     * @param retryCount   current counter for retries
     * @return true when the token has been refreshed, false otherwise.
     * @throws TokenUnauthorizedException   On error 401 when no retries are left
     * @throws AuthenticationTokenException On all other status errors
     * @throws IOException                  When reading the inputStream fails.
     */
    @Override
    public boolean checkResponse(HttpResponse<InputStream> httpResponse, int retryCount) throws HiroException, IOException {
        int statusCode = httpResponse.statusCode();

        if (statusCode < 200 || statusCode > 399) {
            HttpResponseContainer responseContainer = new HttpResponseContainer(httpResponse, getHttpLogger());

            String body = responseContainer.consumeResponseAsString();

            if (responseContainer.contentIsJson()) {
                HiroErrorResponse errorResponse = JsonTools.DEFAULT.toObject(body, HiroErrorResponse.class);

                if (errorResponse.getHiroErrorCode() == 401) {
                    throw new TokenUnauthorizedException(errorResponse.getHiroErrorMessage(), errorResponse.getHiroErrorCode(), body);
                } else {
                    throw new AuthenticationTokenException(errorResponse.getHiroErrorMessage(), errorResponse.getHiroErrorCode(), body);
                }
            } else {
                throw new AuthenticationTokenException("Unknown response", 500, body);
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
    public synchronized String getToken() throws IOException, InterruptedException, HiroException {
        if (tokenResponse == null || StringUtils.isBlank(tokenResponse.token)) {
            requestToken();
        } else if (tokenExpired()) {
            refreshToken();
        }

        return tokenResponse.token;
    }

    /**
     * Obtain a new token from the auth API.
     */
    protected synchronized void requestToken() throws IOException, InterruptedException, HiroException {

        TokenRequest tokenRequest = new TokenRequest(username, password, clientId, clientSecret);

        this.tokenResponse = post(
                TokenResponse.class,
                getUri("app"),
                tokenRequest.toJsonString(),
                Map.of("Content-Type", "application/json"));

        lastUpdate = Instant.now();
    }

    /**
     * Refresh an invalid token.
     */
    @Override
    public synchronized void refreshToken() throws IOException, InterruptedException, HiroException {

        if (tokenResponse == null || StringUtils.isBlank(tokenResponse.refreshToken)) {
            requestToken();
            return;
        }

        if (tokenFresh()) {
            return;
        }

        TokenRefreshRequest tokenRequest = new TokenRefreshRequest(clientId, clientSecret, tokenResponse.refreshToken);

        this.tokenResponse = post(
                TokenResponse.class,
                getUri("refresh"),
                tokenRequest.toJsonString(),
                Map.of("Content-Type", "application/json"));

        lastUpdate = Instant.now();
    }

    /**
     * Revoke a token
     */
    @Override
    public synchronized void revokeToken() throws IOException, InterruptedException, HiroException {
        if (tokenResponse == null || StringUtils.isBlank(tokenResponse.refreshToken)) {
            return;
        }

        if (tokenFresh()) {
            return;
        }

        TokenRefreshRequest tokenRequest = new TokenRefreshRequest(clientId, clientSecret, tokenResponse.refreshToken);

        // This should set tokenResponse to null, since a request to "revoke" does not return any data.
        this.tokenResponse = post(
                TokenResponse.class,
                getUri("revoke"),
                tokenRequest.toJsonString(),
                Map.of("Content-Type", "application/json"));

        lastUpdate = Instant.now();
    }

}
