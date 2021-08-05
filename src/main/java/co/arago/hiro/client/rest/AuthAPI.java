package co.arago.hiro.client.rest;

import co.arago.hiro.client.connection.AbstractTokenAPIHandler;
import co.arago.hiro.client.connection.AuthenticatedAPIHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.model.HiroVertexResponse;
import co.arago.hiro.client.util.RequiredFieldChecker;
import co.arago.hiro.client.util.httpclient.HttpResponseContainer;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AuthAPI extends AuthenticatedAPIHandler {

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

        public AuthAPI build() {
            RequiredFieldChecker.notNull(getTokenApiHandler(), "tokenApiHandler");
            if (StringUtils.isBlank(getApiName()) && StringUtils.isBlank(getEndpoint()))
                RequiredFieldChecker.anyError("Either 'apiName' or 'endpoint' have to be set.");
            return new AuthAPI(this);
        }
    }


    /**
     * Create this APIHandler by using its Builder.
     *
     * @param builder The builder to use.
     */
    protected AuthAPI(Conf<?> builder) {
        super(builder);
    }

    public static Builder newBuilder(AbstractTokenAPIHandler tokenAPIHandler) {
        return new Builder("auth", tokenAPIHandler);
    }

    // ###############################################################################################
    // ## API Requests ##
    // ###############################################################################################

    /**
     * get account information about current token
     * <p>
     * API GET /api/auth/[version]/me/account
     * @see <a href=https://core.arago.co/help/specs/?url=definitions/auth.yaml#/[Me]_Identity>API Documentation</a>
     */
    public class GetMeAccount extends APIRequestConf<GetMeAccount, HiroVertexResponse> {

        /**
         * @param profile Query parameter "profile=[true|false]".
         * @return this
         */
        public GetMeAccount setProfile(Boolean profile) {
            if (query == null)
                query = new HashMap<>();

            query.put("profile", String.valueOf(profile));
            return self();
        }

        @Override
        protected GetMeAccount self() {
            return this;
        }

        /**
         * get account information about current token
         * <p>
         * API GET /api/auth/[version]/me/account
         *
         * @return A HiroVertexResponse with the result data.
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         * @see <a href=https://core.arago.co/help/specs/?url=definitions/auth.yaml#/[Me]_Identity>API Documentation</a>
         */
        public HiroVertexResponse execute() throws HiroException, IOException, InterruptedException {
            return get(HiroVertexResponse.class, getUri("me/account", query, fragment), headers);
        }
    }

    public GetMeAccount newGetMeAccount() {
        return new GetMeAccount();
    }

    /**
     * get avatar of current token's account
     * <p>
     * API GET /api/auth/[version]/me/avatar
     *
     * @see <a href=https://core.arago.co/help/specs/?url=definitions/auth.yaml#/[Me]_Identity/get_me_avatar>API Documentation</a>
     */
    public class GetMeAvatar extends APIRequestConf<GetMeAvatar, HttpResponseContainer> {

        @Override
        protected GetMeAvatar self() {
            return this;
        }

        /**
         * get avatar of current token's account
         * <p>
         * API GET /api/auth/[version]/me/avatar
         *
         * @return A {@link HttpResponseContainer} containing the InputStream of the image, the mediaType and the size
         * (if available).
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         * @see <a href=https://core.arago.co/help/specs/?url=definitions/auth.yaml#/[Me]_Identity/get_me_avatar>API Documentation</a>
         */
        public HttpResponseContainer execute() throws HiroException, IOException, InterruptedException {
            return getBinary(getUri("me/avatar", query, fragment), headers);
        }
    }

    public GetMeAvatar newGetMeAvatar() {
        return new GetMeAvatar();
    }

}
