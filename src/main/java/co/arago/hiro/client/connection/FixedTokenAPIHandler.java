package co.arago.hiro.client.connection;

import co.arago.hiro.client.util.FixedTokenException;
import co.arago.hiro.client.util.HiroException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Instant;

public class FixedTokenAPIHandler extends AbstractTokenAPIHandler {

    public interface Conf extends AbstractTokenAPIHandler.Conf {
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

        public Builder setToken(String token) {
            this.token = token;
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
        public String getToken() {
            return token;
        }

        FixedTokenAPIHandler build() {
            return new FixedTokenAPIHandler(this);
        }
    }

    private final String token;

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
    public void refreshToken() {
        throw new FixedTokenException("Cannot change a fixed token.", 500, null);
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
