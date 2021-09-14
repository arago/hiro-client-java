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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Class for API httpRequests that contains a HttpClient and a HttpLogger.
 */
public abstract class AbstractClientAPIHandler extends AbstractAPIHandler implements AutoCloseable {

    final static Logger log = LoggerFactory.getLogger(AbstractClientAPIHandler.class);

    // ###############################################################################################
    // ## Conf and Builder ##
    // ###############################################################################################

    public static abstract class Conf<T extends Conf<T>> extends AbstractAPIHandler.Conf<T> {
        protected AbstractClientAPIHandler.ProxySpec proxy;
        protected boolean followRedirects = true;
        protected Boolean acceptAllCerts;
        protected Long connectTimeout;
        protected SSLParameters sslParameters;
        protected HttpClient httpClient;
        protected SSLContext sslContext;
        protected int maxConnectionPool = 8;
        protected int maxBinaryLogLength = 1024;

        ProxySpec getProxy() {
            return proxy;
        }

        /**
         * @param proxy Simple proxy with one address and port
         * @return {@link #self()}
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
         * @return {@link #self()}
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
         * @return {@link #self()}
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
         * @return {@link #self()}
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
         * @return {@link #self()}
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
         * @return {@link #self()}
         */
        public T setSslParameters(SSLParameters sslParameters) {
            this.sslParameters = sslParameters;
            return self();
        }

        public HttpClient getHttpClient() {
            return httpClient;
        }

        /**
         * Instance of an externally configured http client. An internal HttpClient will be built with parameters
         * given by this Builder if this is not set.
         *
         * @param httpClient Instance of an HttpClient.
         * @return {@link #self()}
         */
        public T setHttpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return self();
        }

        public int getMaxConnectionPool() {
            return maxConnectionPool;
        }

        /**
         * Set the maximum of open connections for this HttpClient (This sets the fixedThreadPool for the
         * Executor of the HttpClient).
         *
         * @param maxConnectionPool Maximum size of the pool. Default is 8.
         * @return {@link #self()}
         */
        public T setMaxConnectionPool(int maxConnectionPool) {
            this.maxConnectionPool = maxConnectionPool;
            return self();
        }

        public int getMaxBinaryLogLength() {
            return maxBinaryLogLength;
        }

        /**
         * Maximum size to log binary data in logfiles. Default is 1024.
         *
         * @param maxBinaryLogLength Size in bytes
         * @return {@link #self()}
         */
        public T setMaxBinaryLogLength(int maxBinaryLogLength) {
            this.maxBinaryLogLength = maxBinaryLogLength;
            return self();
        }

        @Override
        public abstract AbstractClientAPIHandler build();
    }

    // ###############################################################################################
    // ## Inner classes ##
    // ###############################################################################################

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

    // ###############################################################################################
    // ## Main part ##
    // ###############################################################################################

    protected final AbstractClientAPIHandler.ProxySpec proxy;
    protected final boolean followRedirects;
    protected final Long connectTimeout;
    protected final SSLParameters sslParameters;
    protected HttpClient httpClient;
    protected SSLContext sslContext;
    protected final int maxConnectionPool;

    protected final HttpLogger httpLogger;

    private ExecutorService clientExecutorService;

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
        this.httpClient = builder.getHttpClient();
        this.maxConnectionPool = builder.getMaxConnectionPool();
        this.httpLogger = new HttpLogger(builder.getMaxBinaryLogLength());

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
     * Return {@link #httpClient}. Build a new client if necessary.
     *
     * @return The cached HttpClient
     */
    @Override
    public HttpClient getOrBuildClient() {
        if (httpClient != null)
            return httpClient;

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

        clientExecutorService = Executors.newFixedThreadPool(maxConnectionPool);
        builder.executor(clientExecutorService);

        return builder.build();
    }

    /**
     * Shut the {@link #httpClient} down by shutting down its {@link #clientExecutorService}. If the
     * {@link #httpClient} has been provided externally, this call will be ignored.
     */
    @Override
    public void close() {
        if (clientExecutorService == null)
            return;

        clientExecutorService.shutdown();
        try {
            if (!clientExecutorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                clientExecutorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            clientExecutorService.shutdownNow();
        }
        clientExecutorService = null;
        httpClient = null;
        System.gc();
    }

    /**
     * @return The HttpLogger to use with this class.
     */
    @Override
    public HttpLogger getHttpLogger() {
        return httpLogger;
    }
}
