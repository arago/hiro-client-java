package co.arago.hiro.client.connection.httpclient;

import co.arago.hiro.client.util.HttpLogger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.net.CookieManager;
import java.net.http.HttpClient;

/**
 * Interface for a class that contains a HttpClient and a HttpLogger.
 */
public interface HttpClientHandler extends AutoCloseable {

    /**
     * Interface for the configuration parameters of a HttpClientHandler.
     * 
     * @param <T> Type of the Builder using this configuration.
     */
    interface ConfTemplate<T extends ConfTemplate<T>> {
        ProxySpec getProxy();

        /**
         * @param proxy Simple proxy with one address and port
         * @return self()
         */

        T setProxy(ProxySpec proxy);

        boolean isFollowRedirects();

        /**
         * @param followRedirects Enable Redirect.NORMAL. Default is true.
         * @return self()
         */

        T setFollowRedirects(boolean followRedirects);

        Long getConnectTimeout();

        /**
         * @param connectTimeout Connect timeout in milliseconds.
         * @return self()
         */

        T setConnectTimeout(Long connectTimeout);

        long getShutdownTimeout();

        /**
         * @param shutdownTimeout Time to wait in milliseconds for a complete shutdown of the Java 11 HttpClientImpl.
         *                        If this is set to a value too low, you might need to wait elsewhere for the HttpClient
         *                        to shut down properly. Default is 3000ms.
         * @return self()
         */

        T setShutdownTimeout(long shutdownTimeout);

        Boolean getAcceptAllCerts();

        /**
         * Skip SSL certificate verification. Leave this unset to use the default in HttpClient. Setting this to true
         * installs a permissive SSLContext, setting it to false removes the SSLContext to use the default.
         *
         * @param acceptAllCerts the toggle
         * @return self()
         */

        T setAcceptAllCerts(Boolean acceptAllCerts);

        SSLContext getSslContext();

        /**
         * @param sslContext The specific SSLContext to use.
         * @return self()
         * @see #setAcceptAllCerts(Boolean)
         */

        T setSslContext(SSLContext sslContext);

        SSLParameters getSslParameters();

        /**
         * @param sslParameters The specific SSLParameters to use.
         * @return self()
         */

        T setSslParameters(SSLParameters sslParameters);

        HttpClient getHttpClient();

        /**
         * Instance of an externally configured http client. An internal HttpClient will be built with parameters
         * given by this Builder if this is not set.
         *
         * @param httpClient Instance of an HttpClient.
         * @return self()
         * @implNote Be aware, that any httpClient given via this method will be marked as external and has to be
         *           closed externally as well. A call to {@link DefaultHttpClientHandler#close()} with an external httpclient
         *           will have no effect.
         */

        T setHttpClient(HttpClient httpClient);

        CookieManager getCookieManager();

        /**
         * Instance of an externally configured CookieManager. An internal CookieManager will be built if this is not
         * set.
         *
         * @param cookieManager Instance of a CookieManager.
         * @return self()
         */

        T setCookieManager(CookieManager cookieManager);

        int getMaxConnectionPool();

        /**
         * Set the maximum of open connections for this HttpClient (This sets the fixedThreadPool for the
         * Executor of the HttpClient).
         *
         * @param maxConnectionPool Maximum size of the pool. Default is 8.
         * @return self()
         */

        T setMaxConnectionPool(int maxConnectionPool);

        int getMaxBinaryLogLength();

        /**
         * Maximum size to log binary data in logfiles. Default is 1024.
         *
         * @param maxBinaryLogLength Size in bytes
         * @return self()
         */

        T setMaxBinaryLogLength(int maxBinaryLogLength);

        Boolean getHttpClientAutoClose();

        /**
         * <p>
         * Close internal httpClient automatically, even when it has been set externally.
         * </p>
         * <p>
         * The default is to close the internal httpClient when it has been created internally and to
         * not close the internal httpClient when it has been set via {@link #setHttpClient(HttpClient)}
         * </p>
         *
         * @param httpClientAutoClose true: enable, false: disable.
         * @return self()
         */

        T setHttpClientAutoClose(boolean httpClientAutoClose);

    }

    interface ProxySpec {
        String getAddress();

        int getPort();
    }

    /**
     * Return the internal {@link HttpClient}.
     *
     * @return The HttpClient
     */
    HttpClient getOrBuildClient();

    /**
     * @return The HttpLogger to use with this class.
     */
    HttpLogger getHttpLogger();

    /**
     * <p>
     * Shut the {@link HttpClient} down by shutting down its ExecutorService.
     * </p>
     * <p>
     * Be aware, that there is a shutdown timeout so the Java 11 HttpClient can clean itself up properly. See
     * {@link DefaultHttpClientHandler.Conf#setShutdownTimeout(long)}.
     * </p>
     */
    @Override
    void close();

}
