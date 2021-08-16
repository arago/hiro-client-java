package co.arago.hiro.client.rest;

import co.arago.hiro.client.connection.token.AbstractTokenAPIHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.model.HiroResponse;
import co.arago.hiro.client.model.HiroVertexListResponse;
import co.arago.hiro.client.model.HiroVertexResponse;
import co.arago.hiro.client.util.JsonTools;
import co.arago.hiro.client.util.RequiredFieldChecker;
import co.arago.hiro.client.util.httpclient.HttpResponseParser;
import co.arago.hiro.client.util.httpclient.StreamContainer;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
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

    // ----------------------------------- GetMeAccount -----------------------------------

    /**
     * get account information about current token
     * <p>
     * API GET /api/auth/[version]/me/account
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/auth.yaml#/[Me]_Identity">API Documentation</a>
     */
    public class GetMeAccount extends APIRequestConf<GetMeAccount, HiroVertexResponse> {

        protected GetMeAccount() {
        }

        /**
         * @param profile Query parameter "profile=[true|false]".
         * @return this
         */
        public GetMeAccount setProfile(Boolean profile) {
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
         * @return A {@link HiroVertexResponse} with the result data.
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        public HiroVertexResponse execute() throws HiroException, IOException, InterruptedException {
            return get(HiroVertexResponse.class, getUri("me/account", query, fragment), headers);
        }
    }

    /**
     * @return New instance of the request.
     */
    public GetMeAccount getMeAccount() {
        return new GetMeAccount();
    }

    // ----------------------------------- GetMeAvatar -----------------------------------

    /**
     * get avatar of current token's account
     * <p>
     * API GET /api/auth/[version]/me/avatar
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/auth.yaml#/[Me]_Identity/get_me_avatar">API Documentation</a>
     */
    public class GetMeAvatar extends APIRequestConf<GetMeAvatar, HttpResponseParser> {

        protected GetMeAvatar() {
        }

        @Override
        protected GetMeAvatar self() {
            return this;
        }

        /**
         * get avatar of current token's account
         * <p>
         * API GET /api/auth/[version]/me/avatar
         *
         * @return A {@link HttpResponseParser} containing the InputStream of the image, the mediaType and the size
         * (if available).
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        public HttpResponseParser execute() throws HiroException, IOException, InterruptedException {
            return getBinary(getUri("me/avatar", query, fragment), headers);
        }
    }

    /**
     * @return New instance of the request.
     */
    public GetMeAvatar getMeAvatar() {
        return new GetMeAvatar();
    }

    // ----------------------------------- PutMeAccount -----------------------------------

    /**
     * set avatar of current token's account
     * <p>
     * API PUT /api/auth/[version]/me/avatar
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/auth.yaml#/[Me]_Identity/put_me_avatar">API Documentation</a>
     */
    public class PutMeAvatar extends SendBinaryAPIRequestConf<PutMeAvatar, String> {

        protected PutMeAvatar(StreamContainer streamContainer) {
            super(streamContainer);
        }

        protected PutMeAvatar(InputStream inputStream) {
            super(inputStream);
        }

        @Override
        protected PutMeAvatar self() {
            return this;
        }

        /**
         * set avatar of current token's account
         * <p>
         * API PUT /api/auth/[version]/me/avatar
         *
         * @return A String with the size of the uploaded avatar.
         * @throws HiroException            When the call returns a http status error.
         * @throws IOException              When the call got an IO error.
         * @throws InterruptedException     When the call gets interrupted.
         * @throws IllegalArgumentException When the Content-Type is missing.
         */
        public String execute() throws HiroException, IOException, InterruptedException {
            RequiredFieldChecker.notBlank(streamContainer.getContentType(), "contentType");
            return executeBinary(getUri("me/avatar", query, fragment), "PUT", streamContainer, headers).consumeResponseAsString();
        }
    }

    /**
     * @param streamContainer The existing {@link StreamContainer}. Must not be null.
     * @return New instance of the request.
     */
    public PutMeAvatar putMeAvatar(StreamContainer streamContainer) {
        return new PutMeAvatar(streamContainer);
    }

    /**
     * @param inputStream The inputStream for the request body. Must not be null.
     * @return New instance of the request.
     */
    public PutMeAvatar putMeAvatar(InputStream inputStream) {
        return new PutMeAvatar(inputStream);
    }

    // ----------------------------------- PutMePassword -----------------------------------

    /**
     * change password of current token's account
     * <p>
     * API PUT /api/auth/[version]/me/password
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/auth.yaml#/[Me]_Identity/put_me_password">API Documentation</a>
     */
    public class PutMePassword extends SendJsonAPIRequestConf<PutMePassword, HiroVertexResponse> {

        protected PutMePassword() {
        }

        public PutMePassword setPasswords(String oldPassword, String newPassword) {
            try {
                body = JsonTools.DEFAULT.toString(
                        Map.of("oldPassword", oldPassword,
                                "newPassword", newPassword)
                );
            } catch (JsonProcessingException e) {
                // ignore
            }
            return this;
        }

        @Override
        protected PutMePassword self() {
            return this;
        }

        /**
         * change password of current token's account
         * <p>
         * API PUT /api/auth/[version]/me/password
         *
         * @return A {@link HiroVertexResponse} with the result data.
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        public HiroVertexResponse execute() throws HiroException, IOException, InterruptedException {
            RequiredFieldChecker.notBlank(body, "body");
            return put(HiroVertexResponse.class, getUri("me/password", query, fragment), body, headers);
        }
    }

    /**
     * @return New instance of the request.
     */
    public PutMePassword putMePassword() {
        return new PutMePassword();
    }

    // ----------------------------------- GetMeProfile -----------------------------------

    /**
     * get profile of current token's account
     * <p>
     * API GET /api/auth/[version]/me/profile
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/auth.yaml#/[Me]_Identity/get_me_profile">API Documentation</a>
     */
    public class GetMeProfile extends APIRequestConf<GetMeProfile, HiroVertexResponse> {

        protected GetMeProfile() {
        }

        @Override
        protected GetMeProfile self() {
            return this;
        }

        /**
         * get profile of current token's account
         * <p>
         * API GET /api/auth/[version]/me/profile
         *
         * @return A {@link HiroVertexResponse} with the result data.
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        public HiroVertexResponse execute() throws HiroException, IOException, InterruptedException {
            return get(HiroVertexResponse.class, getUri("me/profile", query, fragment), headers);
        }
    }

    /**
     * @return New instance of the request.
     */
    public GetMeProfile getMeProfile() {
        return new GetMeProfile();
    }

    // ----------------------------------- PostMeProfile -----------------------------------

    /**
     * change profile of current token's account
     * <p>
     * API POST /api/auth/[version]/me/profile
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/auth.yaml#/[Me]_Identity/post_me_profile">API Documentation</a>
     */
    public class PostMeProfile extends SendJsonAPIRequestConf<PostMeProfile, HiroVertexResponse> {

        protected PostMeProfile() {
        }

        @Override
        protected PostMeProfile self() {
            return this;
        }

        /**
         * change profile of current token's account
         * <p>
         * API POST /api/auth/[version]/me/profile
         *
         * @return A {@link HiroVertexResponse} with the result data.
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        public HiroVertexResponse execute() throws HiroException, IOException, InterruptedException {
            RequiredFieldChecker.notBlank(body, "body");
            return post(HiroVertexResponse.class, getUri("me/profile", query, fragment), body, headers);
        }
    }

    /**
     * @return New instance of the request.
     */
    public PostMeProfile postMeProfile() {
        return new PostMeProfile();
    }

    // ----------------------------------- GetMeRoles -----------------------------------

    /**
     * get roles connected to of current token teams
     * <p>
     * API GET /api/auth/[version]/me/roles
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/auth.yaml#/[Me]_Identity/get_me_roles">API Documentation</a>
     */
    public class GetMeRoles extends APIRequestConf<GetMeRoles, HiroResponse> {

        protected GetMeRoles() {
        }

        @Override
        protected GetMeRoles self() {
            return this;
        }

        /**
         * get roles connected to of current token teams
         * <p>
         * API GET /api/auth/[version]/me/roles
         *
         * @return A {@link HiroResponse} with the result data.
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        public HiroResponse execute() throws HiroException, IOException, InterruptedException {
            return get(HiroResponse.class, getUri("me/roles", query, fragment), headers);
        }
    }

    /**
     * @return New instance of the request.
     */
    public GetMeRoles getMeRoles() {
        return new GetMeRoles();
    }

    // ----------------------------------- GetMeTeams -----------------------------------

    /**
     * gets the teams connected to current token
     * <p>
     * API GET /api/auth/[version]/me/teams
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/auth.yaml#/[Me]_Identity/get_me_teams">API Documentation</a>
     */
    public class GetMeTeams extends APIRequestConf<GetMeTeams, HiroVertexListResponse> {

        protected GetMeTeams() {
        }

        /**
         * @param includeVirtual Query parameter "include-virtual=[true|false]".
         * @return this
         */
        public GetMeTeams setIncludeVirtual(Boolean includeVirtual) {
            query.put("include-virtual", String.valueOf(includeVirtual));
            return self();
        }


        @Override
        protected GetMeTeams self() {
            return this;
        }

        /**
         * gets the teams connected to current token
         * <p>
         * API GET /api/auth/[version]/me/teams
         *
         * @return A {@link HiroVertexListResponse} with the result data.
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        public HiroVertexListResponse execute() throws HiroException, IOException, InterruptedException {
            return get(HiroVertexListResponse.class, getUri("me/teams", query, fragment), headers);
        }
    }

    /**
     * @return New instance of the request.
     */
    public GetMeTeams getMeTeams() {
        return new GetMeTeams();
    }
}
