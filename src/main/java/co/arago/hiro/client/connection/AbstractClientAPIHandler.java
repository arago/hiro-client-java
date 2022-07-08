package co.arago.hiro.client.connection;

import co.arago.hiro.client.util.HttpLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.CookieManager;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Class for API httpRequests that contains a HttpClient and a HttpLogger.
 */
public abstract class AbstractClientAPIHandler extends AbstractAPIHandler implements AutoCloseable {

    final static Logger log = LoggerFactory.getLogger(AbstractClientAPIHandler.class);

    protected final static long DEFAULT_SHUTDOWN_TIMEOUT = 3000;
    protected final static int DEFAULT_MAX_BINARY_LOG_LENGTH = 1024;
    protected final static int DEFAULT_MAX_CONNECTION_POOL = 8;

    // ###############################################################################################
    // ## Conf and Builder ##
    // ###############################################################################################

    public static abstract class Conf<T extends Conf<T>> extends AbstractAPIHandler.Conf<T> {
        private AbstractClientAPIHandler.ProxySpec proxy;
        private boolean followRedirects = true;
        private Boolean acceptAllCerts;
        private Long connectTimeout;

        private long shutdownTimeout = DEFAULT_SHUTDOWN_TIMEOUT;
        private SSLParameters sslParameters;
        private HttpClient httpClient;
        private CookieManager cookieManager;
        private SSLContext sslContext;
        private int maxConnectionPool = DEFAULT_MAX_CONNECTION_POOL;
        private int maxBinaryLogLength = DEFAULT_MAX_BINARY_LOG_LENGTH;

        ProxySpec getProxy() {
            return proxy;
        }

        /**
         * @param proxy Simple proxy with one address and port
         * @return {@link #self()}
         * @implNote Will not be used in the final class when a sharedConnectionHandler is set.
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
         * @implNote Will not be used in the final class when a sharedConnectionHandler is set.
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
         * @implNote Will not be used in the final class when a sharedConnectionHandler is set.
         */
        public T setConnectTimeout(Long connectTimeout) {
            this.connectTimeout = connectTimeout;
            return self();
        }

        public long getShutdownTimeout() {
            return shutdownTimeout;
        }

        /**
         * @param shutdownTimeout Time to wait in milliseconds for a complete shutdown of the Java 11 HttpClientImpl.
         *                        If this is set to a value too low, you might need to wait elsewhere for the HttpClient
         *                        to shut down properly. Default is 3000ms.
         * @return {@link #self()}
         * @implNote Will not be used in the final class when a sharedConnectionHandler is set.
         */
        public T setShutdownTimeout(long shutdownTimeout) {
            this.shutdownTimeout = shutdownTimeout;
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
         * @implNote Will not be used in the final class when a sharedConnectionHandler is set.
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
         * @implNote Will not be used in the final class when a sharedConnectionHandler is set.
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
         * @implNote Will not be used in the final class when a sharedConnectionHandler is set.
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
         * @implNote Will not be used in the final class when a sharedConnectionHandler is set.
         */
        public T setHttpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return self();
        }

        public CookieManager getCookieManager() {
            return cookieManager;
        }

        /**
         * Instance of an externally configured CookieManager. An internal CookieManager will be built if this is not
         * set.
         *
         * @param cookieManager Instance of a CookieManager.
         * @return {@link #self()}
         * @implNote Will not be used in the final class when a sharedConnectionHandler is set.
         */
        public T setCookieManager(CookieManager cookieManager) {
            this.cookieManager = cookieManager;
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
         * @implNote Will not be used in the final class when a sharedConnectionHandler is set.
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
         * @implNote Will not be used in the final class when a sharedConnectionHandler is set.
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
    private final static TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }

        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }
    } };

    // ###############################################################################################
    // ## Main part ##
    // ###############################################################################################

    protected final AbstractClientAPIHandler.ProxySpec proxy;
    protected final boolean followRedirects;
    protected final Long connectTimeout;
    protected final SSLParameters sslParameters;
    protected final SSLContext sslContext;
    protected final int maxConnectionPool;
    protected final CookieManager cookieManager;

    private HttpClient httpClient;

    protected final HttpLogger httpLogger;
    protected boolean externalConnection;

    protected final Long shutdownTimeout;

    private final AbstractClientAPIHandler sharedConnectionHandler;

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
        this.shutdownTimeout = builder.getShutdownTimeout();
        Boolean acceptAllCerts = builder.getAcceptAllCerts();
        this.sslParameters = builder.getSslParameters();
        this.httpClient = builder.getHttpClient();
        this.maxConnectionPool = builder.getMaxConnectionPool();
        this.cookieManager = builder.cookieManager != null ? builder.cookieManager : new CookieManager();
        this.httpLogger = new HttpLogger(builder.getMaxBinaryLogLength());

        this.externalConnection = (this.httpClient != null);
        this.sharedConnectionHandler = null;

        if (acceptAllCerts == null) {
            this.sslContext = builder.getSslContext();
        } else {
            if (acceptAllCerts) {
                try {
                    this.sslContext = SSLContext.getInstance("TLS");
                    this.sslContext.init(null, trustAllCerts, new SecureRandom());
                } catch (NoSuchAlgorithmException | KeyManagementException e) {
                    throw new RuntimeException(e);
                }
            } else {
                this.sslContext = null;
            }
        }
    }

    /**
     * Protected Copy Constructor. Fields shall be copied from another AbstractClientAPIHandler.
     *
     * @param other The source AbstractClientAPIHandler.
     */
    protected AbstractClientAPIHandler(AbstractClientAPIHandler other) {
        super(other);
        this.proxy = other.proxy;
        this.followRedirects = other.followRedirects;
        this.connectTimeout = other.connectTimeout;
        this.shutdownTimeout = other.shutdownTimeout;
        this.sslContext = other.sslContext;
        this.sslParameters = other.sslParameters;
        this.maxConnectionPool = other.maxConnectionPool;
        this.cookieManager = other.cookieManager;
        this.httpLogger = other.httpLogger;
        // Always set externalClient to true, so a call to close() does not close the external connection.
        // External connections have to be closed on their own.
        this.externalConnection = true;
        this.sharedConnectionHandler = other;
    }

    // ###############################################################################################
    // ## Overwritten methods ##
    // ###############################################################################################

    /**
     * Return {@link #httpClient} or the httpClient of a sharedConnectionHandler if available.
     * Build a new client if necessary.
     *
     * @return The cached HttpClient
     */
    @Override
    public HttpClient getOrBuildClient() {
        if (httpClient != null)
            return httpClient;

        if (sharedConnectionHandler != null)
            return sharedConnectionHandler.getOrBuildClient();

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

        if (cookieManager != null)
            builder.cookieHandler(cookieManager);

        builder.executor(Executors.newFixedThreadPool(maxConnectionPool));

        httpClient = builder.build();

        log.debug("HttpClient created");

        return httpClient;
    }

    /**
     * <p>
     * Shut the {@link #httpClient} down by shutting down its ExecutorService. This call will be ignored if
     * {@link #externalConnection} is set to true (this happens, when {@link #httpClient} has been provided externally
     * or this ClientAPIHandler has been created via its connection copy constructor.
     * </p>
     * <p>
     * Be aware, that there is a shutdown timeout so the Java 11 HttpClient can clean itself up properly. See
     * {@link Conf#setShutdownTimeout(long)}.
     * </p>
     */
    @Override
    public void close() {
        try {
            if (externalConnection || httpClient == null || httpClient.executor().isEmpty())
                return;

            Executor executor = httpClient.executor().orElse(null);

            if (executor instanceof ExecutorService) {
                ExecutorService executorService = (ExecutorService) executor;

                executorService.shutdown();
                try {
                    if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                        executorService.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executorService.shutdownNow();
                }
            }

            httpClient = null;
            System.gc();

            Thread.sleep(shutdownTimeout);

            log.debug("HttpClient closed");
        } catch (Throwable t) {
            log.error("Error closing HttpClient.", t);
        }
    }

    /**
     * @return The HttpLogger to use with this class.
     */
    @Override
    public HttpLogger getHttpLogger() {
        return httpLogger;
    }
}
