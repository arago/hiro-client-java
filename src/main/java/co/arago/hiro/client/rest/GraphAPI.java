package co.arago.hiro.client.rest;

import co.arago.hiro.client.connection.AuthenticatedAPIHandler;
import co.arago.hiro.client.connection.token.AbstractTokenAPIHandler;
import co.arago.hiro.client.util.RequiredFieldChecker;
import org.apache.commons.lang3.StringUtils;

public class GraphAPI extends AuthenticatedAPIHandler {

    public static abstract class Conf<T extends Conf<T>> extends AuthenticatedAPIHandler.Conf<T> {
    }

    public static final class Builder extends Conf<Builder> {

        public Builder(String apiName, AbstractTokenAPIHandler tokenAPIHandler) {
            setApiName(apiName);
            setTokenApiHandler(tokenAPIHandler);
        }

        @Override
        protected Builder self() {
            return this;
        }

        public GraphAPI build() {
            RequiredFieldChecker.notNull(getTokenApiHandler(), "tokenApiHandler");
            if (StringUtils.isBlank(getApiName()) && StringUtils.isBlank(getEndpoint()))
                RequiredFieldChecker.anyError("Either 'apiName' or 'endpoint' have to be set.");
            return new GraphAPI(this);
        }
    }


    /**
     * Create this APIHandler by using its Builder.
     *
     * @param builder The builder to use.
     */
    protected GraphAPI(Conf<?> builder) {
        super(builder);
    }

    public static Builder newBuilder(AbstractTokenAPIHandler tokenAPIHandler) {
        return new Builder("graph", tokenAPIHandler);
    }
}
