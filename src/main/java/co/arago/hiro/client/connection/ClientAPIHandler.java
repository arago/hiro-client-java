package co.arago.hiro.client.connection;

public interface ClientAPIHandler extends APIHandler, AutoCloseable {
    /**
     * Shut the httpClient down by shutting down its ExecutorService. This call will be ignored if
     * externalConnection is set to true (this happens, when httpClient has been provided externally
     * or this ClientAPIHandler has been created via its connection copy constructor
     * {@link AbstractClientAPIHandler#AbstractClientAPIHandler(AbstractClientAPIHandler)}.
     */
    @Override
    void close();
}
