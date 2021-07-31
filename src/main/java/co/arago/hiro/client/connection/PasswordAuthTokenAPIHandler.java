package co.arago.hiro.client.connection;

import co.arago.hiro.client.exceptions.AuthenticationTokenException;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.exceptions.TokenUnauthorizedException;
import co.arago.hiro.client.model.*;
import co.arago.hiro.client.util.JsonTools;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class PasswordAuthTokenAPIHandler extends AbstractTokenAPIHandler {

    final Logger log = LoggerFactory.getLogger(PasswordAuthTokenAPIHandler.class);

    public interface Conf extends AbstractTokenAPIHandler.Conf {
        /**
         * @param username HIRO username for user account
         * @return this
         */
        Conf setUsername(String username);

        String getUsername();

        /**
         * @param password HIRO password for user account
         * @return this
         */
        Conf setPassword(String password);

        String getPassword();

        /**
         * @param clientId HIRO client_id of app
         * @return this
         */
        Conf setClientId(String clientId);

        String getClientId();

        /**
         * @param clientSecret HIRO client_secret of app
         * @return this
         */
        Conf setClientSecret(String clientSecret);

        String getClientSecret();

        /**
         * Timespan that gets subtracted from the official expiration instant of a token so the token can be refreshed
         * before it runs out. Default is 5000 (5s).
         *
         * @param refreshOffset Offset in ms
         * @return this
         */
        Conf setRefreshOffset(Long refreshOffset);

        Long getRefreshOffset();

        /**
         * Timespan where calls to refresh the token will be ignored and only the current token will be returned. Avoids
         * refresh floods that can happen with multiple threads using the same TokenAPIHandler. Default is 30000 (30s).
         *
         * @param freshBuffer Buffer span in ms
         * @return this
         */
        Conf setFreshBuffer(Long freshBuffer);

        Long getFreshBuffer();

        /**
         * Force logging of insecure request / response data. USE ONLY FOR DEBUGGING PURPOSES!!!
         *
         * @param forceLogging the flag to set.
         * @return this
         */
        Conf setForceLogging(boolean forceLogging);

        boolean getForceLogging();
    }

    public static final class Builder implements Conf {

        private String apiName = "auth";
        private String apiUrl;
        private AbstractAPIClient.ProxySpec proxy;
        private boolean followRedirects = true;
        private long connectTimeout;
        private long httpRequestTimeout;
        private Boolean acceptAllCerts;
        private SSLContext sslContext;
        private SSLParameters sslParameters;
        private String userAgent;
        private HttpClient client;
        private String endpoint;
        private String username;
        private String password;
        private String clientId;
        private String clientSecret;
        private Long refreshOffset = 5000L;
        private Long freshBuffer = 30000L;
        private boolean forceLogging = false;


        /**
         * @param apiUrl The root url for the API
         * @return this
         */
        @Override
        public Builder setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
            return this;
        }

        @Override
        public String getApiUrl() {
            return apiUrl;
        }

        /**
         * @param proxy Simple proxy with one address and port
         * @return this
         */
        @Override
        public Builder setProxy(ProxySpec proxy) {
            this.proxy = proxy;
            return this;
        }

        @Override
        public ProxySpec getProxy() {
            return proxy;
        }

        /**
         * @param followRedirects Enable Redirect.NORMAL. Default is true.
         * @return this
         */
        @Override
        public Builder setFollowRedirects(boolean followRedirects) {
            this.followRedirects = followRedirects;
            return this;
        }

        @Override
        public boolean isFollowRedirects() {
            return followRedirects;
        }

        /**
         * @param connectTimeout Connect timeout in milliseconds.
         * @return this
         */
        @Override
        public Builder setConnectTimeout(long connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        @Override
        public long getConnectTimeout() {
            return connectTimeout;
        }

        /**
         * @param httpRequestTimeout Request timeout in ms.
         * @return this
         */
        @Override
        public Builder setHttpRequestTimeout(long httpRequestTimeout) {
            this.httpRequestTimeout = httpRequestTimeout;
            return this;
        }

        @Override
        public long getHttpRequestTimeout() {
            return httpRequestTimeout;
        }

        /**
         * Skip SSL certificate verification. Leave this unset to use the default in HttpClient. Setting this to true
         * installs a permissive SSLContext, setting it to false removes the SSLContext to use the default.
         *
         * @param acceptAllCerts the toggle
         * @return this
         */
        @Override
        public Builder setAcceptAllCerts(Boolean acceptAllCerts) {
            this.acceptAllCerts = acceptAllCerts;
            return this;
        }

        @Override
        public Boolean getAcceptAllCerts() {
            return acceptAllCerts;
        }

        /**
         * @param sslContext The specific SSLContext to use.
         * @return this
         * @see #setAcceptAllCerts(Boolean)
         */
        @Override
        public Builder setSslContext(SSLContext sslContext) {
            this.sslContext = sslContext;
            return this;
        }

        @Override
        public SSLContext getSslContext() {
            return sslContext;
        }

        /**
         * @param sslParameters The specific SSLParameters to use.
         * @return this
         */
        @Override
        public Builder setSslParameters(SSLParameters sslParameters) {
            this.sslParameters = sslParameters;
            return this;
        }

        @Override
        public SSLParameters getSslParameters() {
            return sslParameters;
        }

        /**
         * For header "User-Agent". Default is determined by the package.
         *
         * @param userAgent The line for the User-Agent header.
         * @return this
         */
        @Override
        public Builder setUserAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        @Override
        public String getUserAgent() {
            return userAgent;
        }

        /**
         * Instance of an externally configured http client. An internal HttpClient will be built with parameters
         * given by this Builder if this is not set.
         *
         * @param client Instance of an HttpClient.
         * @return this
         */
        @Override
        public Builder setClient(HttpClient client) {
            this.client = client;
            return this;
        }

        @Override
        public HttpClient getClient() {
            return client;
        }

        /**
         * @param apiName Set the name of the api. This name will be used to determine the API endpoint. The default
         *                for {@link PasswordAuthTokenAPIHandler} is "auth".
         * @return this
         */
        @Override
        public Builder setApiName(String apiName) {
            this.apiName = apiName;
            return this;
        }

        @Override
        public String getApiName() {
            return apiName;
        }

        /**
         * @param endpoint Set a custom endpoint directly, omitting automatic endpoint detection via apiName.
         * @return this
         */
        @Override
        public Builder setEndpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        @Override
        public String getEndpoint() {
            return endpoint;
        }

        /**
         * @param username HIRO username for user account
         * @return this
         */
        @Override
        public Builder setUsername(String username) {
            this.username = username;
            return this;
        }

        @Override
        public String getUsername() {
            return username;
        }

        /**
         * @param password HIRO password for user account
         * @return this
         */
        @Override
        public Builder setPassword(String password) {
            this.password = password;
            return this;
        }

        @Override
        public String getPassword() {
            return password;
        }

        /**
         * @param clientId HIRO client_id of app
         * @return this
         */
        @Override
        public Builder setClientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        @Override
        public String getClientId() {
            return clientId;
        }

        /**
         * @param clientSecret HIRO client_secret of app
         * @return this
         */
        @Override
        public Builder setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
            return this;
        }

        @Override
        public String getClientSecret() {
            return clientSecret;
        }

        /**
         * Timespan that gets subtracted from the official expiration instant of a token so the token can be refreshed
         * before it runs out. Default is 5000 (5s).
         *
         * @param refreshOffset Offset in ms
         * @return this
         */
        @Override
        public Builder setRefreshOffset(Long refreshOffset) {
            this.refreshOffset = refreshOffset;
            return this;
        }

        @Override
        public Long getRefreshOffset() {
            return refreshOffset;
        }

        /**
         * Timespan where calls to refresh the token will be ignored and only the current token will be returned. Avoids
         * refresh floods that can happen with multiple threads using the same TokenAPIHandler. Default is 30000 (30s).
         *
         * @param freshBuffer Buffer span in ms
         * @return this
         */
        @Override
        public Builder setFreshBuffer(Long freshBuffer) {
            this.freshBuffer = freshBuffer;
            return this;
        }

        @Override
        public Long getFreshBuffer() {
            return freshBuffer;
        }

        /**
         * Force logging of insecure request / response data. USE ONLY FOR DEBUGGING PURPOSES!!!
         *
         * @param forceLogging the flag to set.
         * @return this
         */
        @Override
        public Builder setForceLogging(boolean forceLogging) {
            this.forceLogging = forceLogging;
            return this;
        }

        @Override
        public boolean getForceLogging() {
            return forceLogging;
        }

        PasswordAuthTokenAPIHandler build() {
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

    protected final String username;
    protected final String password;
    protected final String clientId;
    protected final String clientSecret;

    protected TokenInfo tokenInfo;

    public static Builder newBuilder() {
        return new Builder();
    }

    protected PasswordAuthTokenAPIHandler(Conf builder) {
        super(builder);
        this.username = builder.getUsername();
        this.password = builder.getPassword();
        this.clientId = builder.getClientId();
        this.clientSecret = builder.getClientSecret();

        this.tokenInfo = new TokenInfo()
                .setFreshBuffer(builder.getFreshBuffer())
                .setRefreshOffset(builder.getRefreshOffset());

        if (!builder.getForceLogging()) {
            try {
                httpLogger.addFilter(getEndpointUri().resolve("app"));
                httpLogger.addFilter(getEndpointUri().resolve("refresh"));
            } catch (IOException | InterruptedException | HiroException e) {
                log.error("Cannot get endpoint URI. Disable logging of http bodies.", e);
                httpLogger.setLogBody(false);
            }
        }
    }

    /**
     * @param refreshOffset ms of offset for token expiry. This is subtracted from the instant given via "expires-at".
     * @param freshBuffer   ms of time, where no refresh calls are sent to the backend to avoid request flooding
     */
    public void setTokenRefreshOptions(Long refreshOffset, Long freshBuffer) {
        this.tokenInfo.setRefreshOffset(refreshOffset).setFreshBuffer(freshBuffer);
    }

    /**
     * Check for the kind of error. 401 throws TokenUnauthorizedException, all other throw AuthenticationTokenException.
     *
     * @param httpResponse The httpResponse from the HttpRequest
     * @param retry        the current state of retry. If this is set to false, false will also be returned.
     *                     (Ignored here)
     * @return always false.
     * @throws TokenUnauthorizedException   On error 401
     * @throws AuthenticationTokenException On all other status errors
     * @throws IOException                  When reading the inputStream fails.
     */
    @Override
    protected boolean checkResponse(HttpResponse<InputStream> httpResponse, boolean retry) throws HiroException, IOException {
        int statusCode = httpResponse.statusCode();

        if (statusCode < 200 || statusCode > 399) {
            HiroResponse hiroResponse = JsonTools.DEFAULT.toObject(httpResponse.body(), HiroResponse.class);
            httpLogger.logResponse(httpResponse, hiroResponse);

            TokenErrorResponse errorResponse = TokenErrorResponse.fromResponse(hiroResponse);

            if (errorResponse.getCode() == 401) {
                throw new TokenUnauthorizedException(errorResponse.getMessage(), errorResponse.getCode(), null);
            } else {
                throw new AuthenticationTokenException(errorResponse.getMessage(), errorResponse.getCode(), null);
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
    public synchronized String getToken() throws IOException, InterruptedException, HiroException {
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
    protected synchronized void requestToken() throws IOException, InterruptedException, HiroException {

        TokenRequest tokenRequest = new TokenRequest(username, password, clientId, clientSecret);

        HiroResponse hiroResponse = post(
                getEndpointUri().resolve("app"),
                JsonTools.DEFAULT.toString(tokenRequest),
                Map.of("Content-Type", "application/json"),
                null);

        tokenInfo.handleResponse(TokenResponse.fromResponse(hiroResponse));
    }

    /**
     * Refresh an invalid token.
     */
    @Override
    public synchronized void refreshToken() throws IOException, InterruptedException, HiroException {

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
                JsonTools.DEFAULT.toString(tokenRequest),
                Map.of("Content-Type", "application/json"),
                null);

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
