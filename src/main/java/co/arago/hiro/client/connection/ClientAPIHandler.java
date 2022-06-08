package co.arago.hiro.client.connection;

import co.arago.hiro.client.util.HttpLogger;

import java.net.http.HttpClient;

public interface ClientAPIHandler extends APIHandler, AutoCloseable {
    /**
     * Return httpClient. Build a new client if necessary.
     *
     * @return The cached HttpClient
     */
    HttpClient getOrBuildClient();

    /**
     * Shut the httpClient down by shutting down its ExecutorService. This call will be ignored if
     * externalConnection is set to true (this happens, when httpClient has been provided externally
     * or this ClientAPIHandler has been created via its connection copy constructor
     * {@link AbstractClientAPIHandler#AbstractClientAPIHandler(AbstractClientAPIHandler)}.
     */
    @Override
    void close();

    /**
     * @return The HttpLogger to use with this class.
     */
    HttpLogger getHttpLogger();
}
