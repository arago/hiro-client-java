package co.arago.hiro.client.connection;

import co.arago.hiro.client.util.HttpLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.concurrent.Executors;

/**
 * Class for API httpRequests that contains a HttpClient and a HttpLogger.
 */
public abstract class AbstractClientAPIHandler extends AbstractAPIHandler {

    final Logger log = LoggerFactory.getLogger(AbstractClientAPIHandler.class);

    public static abstract class Conf<T extends Conf<T>> extends AbstractAPIHandler.Conf<T> {
        protected AbstractClientAPIHandler.ProxySpec proxy;
        protected boolean followRedirects = true;
        protected Boolean acceptAllCerts;
        protected Long connectTimeout;
        protected SSLParameters sslParameters;
        protected HttpClient client;
        protected SSLContext sslContext;
        protected Integer maxConnectionPool;

        ProxySpec getProxy() {
            return proxy;
        }

        /**
         * @param proxy Simple proxy with one address and port
         * @return this
         */
        public T setProxy(ProxySpec proxy) {
            this.proxy = proxy;
            return self();
        }

        public boolean isFollowRedirects() {
            return followRedirects;
        }

        /**
         * @param followRedirects Enable Redirect.NORMAL. Default is true.
         * @return this
         */
        public T setFollowRedirects(boolean followRedirects) {
            this.followRedirects = followRedirects;
            return self();
        }

        public Long getConnectTimeout() {
            return connectTimeout;
        }

        /**
         * @param connectTimeout Connect timeout in milliseconds.
         * @return this
         */
        public T setConnectTimeout(Long connectTimeout) {
            this.connectTimeout = connectTimeout;
            return self();
        }

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
        public T setAcceptAllCerts(Boolean acceptAllCerts) {
            this.acceptAllCerts = acceptAllCerts;
            return self();
        }

        public SSLContext getSslContext() {
            return sslContext;
        }

        /**
         * @param sslContext The specific SSLContext to use.
         * @return this
         * @see #setAcceptAllCerts(Boolean)
         */
        public T setSslContext(SSLContext sslContext) {
            this.sslContext = sslContext;
            return self();
        }

        public SSLParameters getSslParameters() {
            return sslParameters;
        }

        /**
         * @param sslParameters The specific SSLParameters to use.
         * @return this
         */
        public T setSslParameters(SSLParameters sslParameters) {
            this.sslParameters = sslParameters;
            return self();
        }

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
        public T setClient(HttpClient client) {
            this.client = client;
            return self();
        }

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
        public T setMaxConnectionPool(Integer maxConnectionPool) {
            this.maxConnectionPool = maxConnectionPool;
            return self();
        }

        @Override
        public abstract AbstractClientAPIHandler build();
    }

    /**
     * A simple data class for a proxy
     */
    public static class ProxySpec {
        private final String address;
        private final int port;

        public ProxySpec(String address, int port) {
            this.address = address;
            this.port = port;
        }

        public String getAddress() {
            return address;
        }

        public int getPort() {
            return port;
        }
    }

    /**
     * A TrustManager trusting all certificates
     */
    private final static TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }

        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }
    }};

    protected final AbstractClientAPIHandler.ProxySpec proxy;
    protected final boolean followRedirects;
    protected final Long connectTimeout;
    protected final SSLParameters sslParameters;
    protected final HttpClient client;
    protected SSLContext sslContext;
    protected final Integer maxConnectionPool;

    protected final HttpLogger httpLogger = new HttpLogger();

    // ###############################################################################################
    // ## Constructors ##
    // ###############################################################################################

    /**
     * Protected Constructor. Attributes shall be filled via builders.
     *
     * @param builder The builder to use.
     */
    protected AbstractClientAPIHandler(Conf<?> builder) {
        super(builder);
        this.proxy = builder.getProxy();
        this.followRedirects = builder.isFollowRedirects();
        this.connectTimeout = builder.getConnectTimeout();
        Boolean acceptAllCerts = builder.getAcceptAllCerts();
        this.sslParameters = builder.getSslParameters();
        this.client = builder.getClient();
        this.maxConnectionPool = builder.getMaxConnectionPool() != null ? builder.getMaxConnectionPool() : 8;

        if (acceptAllCerts == null) {
            this.sslContext = builder.getSslContext();
        } else {
            if (acceptAllCerts) {
                try {
                    this.sslContext = SSLContext.getInstance("TLS");
                    this.sslContext.init(null, trustAllCerts, new SecureRandom());
                } catch (NoSuchAlgorithmException | KeyManagementException e) {
                    // ignore
                }
            } else {
                this.sslContext = null;
            }
        }
    }

    // ###############################################################################################
    // ## Overwritten methods ##
    // ###############################################################################################

    /**
     * Build a new Java 11 HttpClient.
     *
     * @return The HttpClient
     */
    @Override
    public HttpClient getOrBuildClient() {
        if (client != null)
            return client;

        HttpClient.Builder builder = HttpClient.newBuilder();

        if (followRedirects)
            builder.followRedirects(HttpClient.Redirect.NORMAL);

        if (proxy != null)
            builder.proxy(ProxySelector.of(new InetSocketAddress(proxy.getAddress(), proxy.getPort())));

        if (connectTimeout != null)
            builder.connectTimeout(Duration.ofMillis(connectTimeout));

        if (sslContext != null)
            builder.sslContext(sslContext);

        if (sslParameters != null)
            builder.sslParameters(sslParameters);

        builder.executor(Executors.newFixedThreadPool(maxConnectionPool));

        return builder.build();
    }

    /**
     * Abstract class that needs to be overwritten by a supplier of a HttpLogger.
     *
     * @return The HttpLogger to use with this class.
     */
    @Override
    public HttpLogger getHttpLogger() {
        return httpLogger;
    }
}
