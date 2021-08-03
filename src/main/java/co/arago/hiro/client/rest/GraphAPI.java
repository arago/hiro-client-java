package co.arago.hiro.client.rest;

import co.arago.hiro.client.connection.AbstractTokenAPIHandler;
import co.arago.hiro.client.connection.AuthenticatedAPIHandler;
import co.arago.hiro.client.util.RequiredFieldChecker;
import org.apache.commons.lang3.StringUtils;

public class GraphAPI extends AuthenticatedAPIHandler {

    public interface Conf extends AuthenticatedAPIHandler.Conf {
    }

    public static final class Builder implements Conf {

        private String apiName;
        private String endpoint;
        private Long httpRequestTimeout;
        private int maxRetries = 2;
        private AbstractTokenAPIHandler tokenAPIHandler;

        public Builder(String apiName, AbstractTokenAPIHandler tokenAPIHandler) {
            this.apiName = apiName;
            this.tokenAPIHandler = tokenAPIHandler;
        }

        @Override
        public Long getHttpRequestTimeout() {
            return httpRequestTimeout;
        }

        /**
         * @param httpRequestTimeout Request timeout in ms.
         * @return this
         */
        @Override
        public Builder setHttpRequestTimeout(Long httpRequestTimeout) {
            this.httpRequestTimeout = httpRequestTimeout;
            return this;
        }

        @Override
        public int getMaxRetries() {
            return maxRetries;
        }

        /**
         * @param maxRetries Max amount of retries when http errors are received.
         * @return this
         */
        @Override
        public Builder setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        @Override
        public String getApiName() {
            return apiName;
        }

        /**
         * @param apiName Set the name of the api. This name will be used to determine the API endpoint.
         * @return this
         */
        @Override
        public Builder setApiName(String apiName) {
            this.apiName = apiName;
            return this;
        }

        @Override
        public String getEndpoint() {
            return endpoint;
        }

        /**
         * @param endpoint Set a custom endpoint directly, omitting automatic endpoint detection via apiName.
         * @return this
         */
        @Override
        public Builder setEndpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        @Override
        public AbstractTokenAPIHandler getTokenApiHandler() {
            return tokenAPIHandler;
        }

        /**
         * @param tokenAPIHandler The tokenAPIHandler for this API.
         * @return this
         */
        @Override
        public Builder setTokenApiHandler(AbstractTokenAPIHandler tokenAPIHandler) {
            this.tokenAPIHandler = tokenAPIHandler;
            return this;
        }

        public GraphAPI build() {
            RequiredFieldChecker.notNull(tokenAPIHandler, "tokenApiHandler");
            if (StringUtils.isBlank(apiName) && StringUtils.isBlank(endpoint))
                RequiredFieldChecker.anyError("Either 'apiName' or 'endpoint' have to be set.");
            return new GraphAPI(this);
        }
    }


    /**
     * Create this APIHandler by using its Builder.
     *
     * @param builder The builder to use.
     */
    protected GraphAPI(Conf builder) {
        super(builder);
    }

    public static Builder newBuilder(AbstractTokenAPIHandler tokenAPIHandler) {
        return new Builder("graph", tokenAPIHandler);
    }
}
