package co.arago.hiro.client.connection;

import co.arago.hiro.client.model.*;
import co.arago.hiro.client.util.AuthenticationTokenException;
import co.arago.hiro.client.util.HiroException;
import co.arago.hiro.client.util.JsonTools;
import co.arago.hiro.client.util.TokenUnauthorizedException;
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
        String getUsername();

        String getPassword();

        String getClientId();

        String getClientSecret();

        Long getRefreshOffset();

        Long getFreshBuffer();
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


        public Builder setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
            return this;
        }

        public Builder setProxy(ProxySpec proxy) {
            this.proxy = proxy;
            return this;
        }

        public Builder setFollowRedirects(boolean followRedirects) {
            this.followRedirects = followRedirects;
            return this;
        }

        public Builder setConnectTimeout(long connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder setHttpRequestTimeout(long httpRequestTimeout) {
            this.httpRequestTimeout = httpRequestTimeout;
            return this;
        }

        public Builder setAcceptAllCerts(Boolean acceptAllCerts) {
            this.acceptAllCerts = acceptAllCerts;
            return this;
        }

        public Builder setSslContext(SSLContext sslContext) {
            this.sslContext = sslContext;
            return this;
        }

        public Builder setSslParameters(SSLParameters sslParameters) {
            this.sslParameters = sslParameters;
            return this;
        }

        public Builder setUserAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder setClient(HttpClient client) {
            this.client = client;
            return this;
        }

        public Builder setApiName(String apiName) {
            this.apiName = apiName;
            return this;
        }

        public Builder setEndpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder setUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder setPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder setClientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
            return this;
        }

        public Builder setRefreshOffset(Long refreshOffset) {
            this.refreshOffset = refreshOffset;
            return this;
        }

        public Builder setFreshBuffer(Long freshBuffer) {
            this.freshBuffer = freshBuffer;
            return this;
        }

        @Override
        public String getApiUrl() {
            return apiUrl;
        }

        @Override
        public ProxySpec getProxy() {
            return proxy;
        }

        @Override
        public boolean isFollowRedirects() {
            return followRedirects;
        }

        @Override
        public long getConnectTimeout() {
            return connectTimeout;
        }

        @Override
        public long getHttpRequestTimeout() {
            return httpRequestTimeout;
        }

        @Override
        public Boolean getAcceptAllCerts() {
            return acceptAllCerts;
        }

        @Override
        public SSLContext getSslContext() {
            return sslContext;
        }

        @Override
        public SSLParameters getSslParameters() {
            return sslParameters;
        }

        @Override
        public String getUserAgent() {
            return userAgent;
        }

        @Override
        public HttpClient getClient() {
            return client;
        }

        @Override
        public String getApiName() {
            return apiName;
        }

        @Override
        public String getEndpoint() {
            return endpoint;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public String getClientId() {
            return clientId;
        }

        @Override
        public String getClientSecret() {
            return clientSecret;
        }

        @Override
        public Long getRefreshOffset() {
            return refreshOffset;
        }

        @Override
        public Long getFreshBuffer() {
            return freshBuffer;
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

    public final String username;
    public final String password;
    public final String clientId;
    public final String clientSecret;

    protected TokenInfo tokenInfo = new TokenInfo();

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
