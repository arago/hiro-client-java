package co.arago.hiro.client.connection;

import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.exceptions.HiroHttpException;
import co.arago.hiro.client.model.HiroErrorResponse;
import co.arago.hiro.client.model.HiroResponse;
import co.arago.hiro.client.util.HttpLogger;
import co.arago.hiro.client.util.JsonTools;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class for API httpRequests that contains a HttpClient
 */
public abstract class AbstractClientAPIHandler extends AbstractAPIHandler {

    final Logger log = LoggerFactory.getLogger(AbstractClientAPIHandler.class);

    public interface Conf extends AbstractAPIHandler.Conf {

        ProxySpec getProxy();

        /**
         * @param proxy Simple proxy with one address and port
         * @return this
         */
        Conf setProxy(ProxySpec proxy);

        boolean isFollowRedirects();

        /**
         * @param followRedirects Enable Redirect.NORMAL. Default is true.
         * @return this
         */
        Conf setFollowRedirects(boolean followRedirects);

        long getConnectTimeout();

        /**
         * @param connectTimeout Connect timeout in milliseconds.
         * @return this
         */
        Conf setConnectTimeout(long connectTimeout);

        long getHttpRequestTimeout();

        /**
         * @param httpRequestTimeout Request timeout in ms.
         * @return this
         */
        Conf setHttpRequestTimeout(long httpRequestTimeout);

        Boolean getAcceptAllCerts();

        /**
         * Skip SSL certificate verification. Leave this unset to use the default in HttpClient. Setting this to true
         * installs a permissive SSLContext, setting it to false removes the SSLContext to use the default.
         *
         * @param acceptAllCerts the toggle
         * @return this
         */
        Conf setAcceptAllCerts(Boolean acceptAllCerts);

        SSLContext getSslContext();

        /**
         * @param sslContext The specific SSLContext to use.
         * @return this
         * @see #setAcceptAllCerts(Boolean)
         */
        Conf setSslContext(SSLContext sslContext);

        SSLParameters getSslParameters();

        /**
         * @param sslParameters The specific SSLParameters to use.
         * @return this
         */
        Conf setSslParameters(SSLParameters sslParameters);

        HttpClient getClient();

        /**
         * Instance of an externally configured http client. An internal HttpClient will be built with parameters
         * given by this Builder if this is not set.
         *
         * @param client Instance of an HttpClient.
         * @return this
         */
        Conf setClient(HttpClient client);


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
    protected final long connectTimeout;
    protected final SSLParameters sslParameters;
    protected final HttpClient client;
    protected SSLContext sslContext;
    protected HttpLogger httpLogger = new HttpLogger();

    // ###############################################################################################
    // ## Constructors ##
    // ###############################################################################################

    /**
     * Protected Constructor. Attributes shall be filled via builders.
     *
     * @param builder The builder to use.
     */
    protected AbstractClientAPIHandler(Conf builder) {
        super(builder);
        this.proxy = builder.getProxy();
        this.followRedirects = builder.isFollowRedirects();
        this.connectTimeout = builder.getConnectTimeout();
        Boolean acceptAllCerts = builder.getAcceptAllCerts();
        this.sslParameters = builder.getSslParameters();
        this.client = builder.getClient();

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
    // ## Tool methods ##
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

        if (connectTimeout > 0)
            builder.connectTimeout(Duration.ofMillis(connectTimeout));

        if (sslContext != null)
            builder.sslContext(sslContext);

        if (sslParameters != null)
            builder.sslParameters(sslParameters);

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
