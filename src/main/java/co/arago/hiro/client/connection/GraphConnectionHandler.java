package co.arago.hiro.client.connection;

import co.arago.hiro.client.util.httpclient.HttpHeaderMap;

/**
 * Basic connection to the HIRO Graph without any authentication. Can be used as root connection for several
 * TokenAPIHandlers, i.e. a FixedTokenApiHandler with a token for each user that shall all use the same connection.
 * Contains CookieHandler, the versionMap and the httpConnection.
 */
public class GraphConnectionHandler extends AbstractVersionAPIHandler implements AutoCloseable {

    // ###############################################################################################
    // ## Conf and Builder ##
    // ###############################################################################################

    public static abstract class Conf<T extends Conf<T>> extends AbstractVersionAPIHandler.Conf<T> {

        @Override
        public abstract GraphConnectionHandler build();
    }

    public static final class Builder extends Conf<Builder> {

        @Override
        protected Builder self() {
            return this;
        }

        public GraphConnectionHandler build() {
            return new GraphConnectionHandler(this);
        }

    }

    // ###############################################################################################
    // ## Main part ##
    // ###############################################################################################

    /**
     * Constructor
     *
     * @param builder The builder to use.
     */
    protected GraphConnectionHandler(Conf<?> builder) {
        super(builder);
    }

    public static Conf<?> newBuilder() {
        return new Builder();
    }

    /**
     * Override this to add authentication tokens. This only returns default headers since tokens are not available
     * here.
     *
     * @param headers Map of headers with initial values.
     */
    @Override
    public void addToHeaders(HttpHeaderMap headers) {
        headers.set("User-Agent", userAgent);
    }

    /**
     * Close the underlying httpClientHandler.
     */
    @Override
    public void close() {
        httpClientHandler.close();
    }
}
