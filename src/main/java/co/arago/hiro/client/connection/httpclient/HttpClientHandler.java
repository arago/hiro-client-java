package co.arago.hiro.client.connection.httpclient;

import co.arago.hiro.client.util.HttpLogger;

import java.net.http.HttpClient;

/**
 * Interface for a class that contains a HttpClient and a HttpLogger.
 */
public interface HttpClientHandler extends AutoCloseable {

    /**
     * A simple data class for a proxy
     */
    class ProxySpec {
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
