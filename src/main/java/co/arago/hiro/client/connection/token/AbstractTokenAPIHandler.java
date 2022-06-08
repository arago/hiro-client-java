package co.arago.hiro.client.connection.token;

import co.arago.hiro.client.connection.AbstractVersionAPIHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.model.token.DecodedToken;
import co.arago.hiro.client.util.httpclient.HttpHeaderMap;

import java.io.IOException;

public abstract class AbstractTokenAPIHandler extends AbstractVersionAPIHandler implements TokenAPIHandler {

    // ###############################################################################################
    // ## Conf and Builder ##
    // ###############################################################################################

    public static abstract class Conf<T extends Conf<T>> extends AbstractVersionAPIHandler.Conf<T> {
        @Override
        public abstract AbstractTokenAPIHandler build();
    }

    // ###############################################################################################
    // ## Main part ##
    // ###############################################################################################

    /**
     * Constructor
     *
     * @param builder The builder to use.
     */
    protected AbstractTokenAPIHandler(Conf<?> builder) {
        super(builder);
    }

    /**
     * Special Copy Constructor. Uses the connection of another existing AbstractVersionAPIHandler.
     *
     * @param versionAPIHandler The AbstractVersionAPIHandler with the source data.
     */
    protected AbstractTokenAPIHandler(AbstractVersionAPIHandler versionAPIHandler) {
        super(versionAPIHandler);
    }

    /**
     * This just adds the userAgent to the headers.
     *
     * @param headers Map of headers with initial values.
     */
    @Override
    public void addToHeaders(HttpHeaderMap headers) {
        headers.put("User-Agent", userAgent);
    }

    /**
     * Decode the payload part of the internal token.
     *
     * @return Decoded token as {@link DecodedToken}.
     * @throws InterruptedException When call gets interrupted.
     * @throws IOException          When call has IO errors.
     * @throws HiroException        On Hiro protocol / handling errors.
     */
    @Override
    public DecodedToken decodeToken() throws HiroException, IOException, InterruptedException {
        return TokenAPIHandler.decodeToken(getToken());
    }

}
