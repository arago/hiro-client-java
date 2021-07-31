package co.arago.hiro.client.connection;

import co.arago.hiro.client.exceptions.FixedTokenException;
import co.arago.hiro.client.exceptions.HiroException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.net.http.HttpClient;
import java.time.Instant;

public class FixedTokenAPIHandler extends AbstractTokenAPIHandler {

    public interface Conf extends AbstractTokenAPIHandler.Conf {
        /**
         * Set a static token
         *
         * @param token The token string
         * @return this
         */
        Conf setToken(String token);

        String getToken();
    }

    public static final class Builder implements Conf {

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
        private String apiName;
        private String endpoint;
        private String token;

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
         * @param apiName Set the name of the api. This name will be used to determine the API endpoint.
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
         * Set a static token
         *
         * @param token The token string
         * @return this
         */
        @Override
        public Builder setToken(String token) {
            this.token = token;
            return this;
        }

        @Override
        public String getToken() {
            return token;
        }

        FixedTokenAPIHandler build() {
            return new FixedTokenAPIHandler(this);
        }
    }

    protected final String token;

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Constructor
     *
     * @param builder The builder to use.
     */
    protected FixedTokenAPIHandler(Conf builder) {
        super(builder);
        this.token = builder.getToken();
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
    public void refreshToken() throws HiroException {
        throw new FixedTokenException("Cannot change a fixed token.");
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
