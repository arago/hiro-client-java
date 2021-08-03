package co.arago.hiro.client.connection;

import co.arago.hiro.client.exceptions.FixedTokenException;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.util.RequiredFieldChecker;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.net.http.HttpClient;
import java.time.Instant;

public class EnvironmentTokenAPIHandler extends AbstractTokenAPIHandler {

    public interface Conf extends AbstractTokenAPIHandler.Conf {
        String getTokenEnv();

        /**
         * @param tokenEnv Name of the environment variable. Default is "HIRO_TOKEN".
         * @return this
         */
        Conf setTokenEnv(String tokenEnv);
    }

    public static final class Builder implements Conf {

        private String apiUrl;
        private AbstractClientAPIHandler.ProxySpec proxy;
        private boolean followRedirects = true;
        private Long connectTimeout;
        private Long httpRequestTimeout;
        private Boolean acceptAllCerts;
        private SSLContext sslContext;
        private SSLParameters sslParameters;
        private String userAgent;
        private Integer maxConnectionPool;
        private HttpClient client;
        private String apiName;
        private String endpoint;
        private String tokenEnv;
        private int maxRetries = 2;

        @Override
        public String getApiUrl() {
            return apiUrl;
        }

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
        public ProxySpec getProxy() {
            return proxy;
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
        public boolean isFollowRedirects() {
            return followRedirects;
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
        public Long getConnectTimeout() {
            return connectTimeout;
        }

        /**
         * @param connectTimeout Connect timeout in milliseconds.
         * @return this
         */
        @Override
        public Builder setConnectTimeout(Long connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        @Override
        public Long getHttpRequestTimeout() {
            return httpRequestTimeout;
        }

        /**
         * @param httpRequestTimeout Request timeout in ms.
         * @return this
         */
        @Override
        public Builder setHttpRequestTimeout(Long httpRequestTimeout) {
            this.httpRequestTimeout = httpRequestTimeout;
            return this;
        }

        @Override
        public Boolean getAcceptAllCerts() {
            return acceptAllCerts;
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
        public SSLContext getSslContext() {
            return sslContext;
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
        public SSLParameters getSslParameters() {
            return sslParameters;
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
        public String getUserAgent() {
            return userAgent;
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
        public HttpClient getClient() {
            return client;
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
        public Integer getMaxConnectionPool() {
            return maxConnectionPool;
        }

        /**
         * Set the maximum of open connections for this HttpClient (This sets the fixedThreadPool for the
         * Executor of the HttpClient).
         *
         * @param maxConnectionPool Maximum size of the pool. Default is 8.
         * @return this
         */
        @Override
        public Builder setMaxConnectionPool(Integer maxConnectionPool) {
            this.maxConnectionPool = maxConnectionPool;
            return this;
        }

        @Override
        public int getMaxRetries() {
            return maxRetries;
        }

        /**
         * @param maxRetries Max amount of retries when http errors are received.
         * @return this
         */
        @Override
        public Builder setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        @Override
        public String getTokenEnv() {
            return tokenEnv;
        }

        /**
         * @param tokenEnv Name of the environment variable. Default is "HIRO_TOKEN".
         * @return this
         */
        @Override
        public Builder setTokenEnv(String tokenEnv) {
            this.tokenEnv = tokenEnv;
            return this;
        }



        public EnvironmentTokenAPIHandler build() {
            RequiredFieldChecker.notBlank(apiUrl, "apiUrl");
            return new EnvironmentTokenAPIHandler(this);
        }

    }

    protected final String tokenEnv;

    /**
     * Constructor.
     *
     * @param builder The builder to use.
     */
    protected EnvironmentTokenAPIHandler(Conf builder) {
        super(builder);
        this.tokenEnv = builder.getTokenEnv();
    }

    public static Builder newBuilder() {
        return new Builder();
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
    public void refreshToken() throws HiroException {
        throw new FixedTokenException("Cannot change an environment token.");
    }

    /**
     * Revoke a token
     */
    @Override
    public void revokeToken() throws HiroException {
        throw new FixedTokenException("Cannot revoke a fixed token.");
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
