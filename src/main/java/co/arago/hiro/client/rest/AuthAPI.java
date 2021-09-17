package co.arago.hiro.client.rest;

import co.arago.hiro.client.connection.token.AbstractTokenAPIHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.model.HiroMessage;
import co.arago.hiro.client.model.vertex.HiroVertexListMessage;
import co.arago.hiro.client.model.vertex.HiroVertexMessage;
import co.arago.hiro.client.util.httpclient.HttpResponseParser;
import co.arago.hiro.client.util.httpclient.StreamContainer;
import co.arago.util.json.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class AuthAPI extends AuthenticatedAPIHandler {

    // ###############################################################################################
    // ## Conf and Builder ##
    // ###############################################################################################

    public static abstract class Conf<T extends Conf<T>> extends AuthenticatedAPIHandler.Conf<T> {
    }

    public static final class Builder extends Conf<Builder> {

        private Builder(String apiName, AbstractTokenAPIHandler tokenAPIHandler) {
            setApiName(apiName);
            setTokenApiHandler(tokenAPIHandler);
        }

        @Override
        protected Builder self() {
            return this;
        }

        public AuthAPI build() {
            return new AuthAPI(this);
        }
    }

    // ###############################################################################################
    // ## Main part ##
    // ###############################################################################################

    /**
     * Protected constructor. Create this {@link AuthAPI} by using its {@link Builder}.
     *
     * @param builder The builder to use.
     */
    protected AuthAPI(Conf<?> builder) {
        super(builder);
    }

    /**
     * Get a {@link Builder} for {@link AuthAPI}.
     *
     * @param tokenAPIHandler The API handler for this websocket.
     * @return The {@link Builder} for {@link AuthAPI}.
     */
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
    public class GetMeAccount extends APIRequestConf<GetMeAccount, HiroVertexMessage> {

        protected GetMeAccount() {
        }

        /**
         * @param profile Query parameter "profile=[true|false]".
         * @return {@link #self()}
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
         * @return A {@link HiroVertexMessage} with the result data.
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        @Override
        public HiroVertexMessage execute() throws HiroException, IOException, InterruptedException {
            return get(
                    HiroVertexMessage.class,
                    getUri("me/account", query, fragment),
                    headers,
                    httpRequestTimeout,
                    maxRetries);
        }
    }

    /**
     * get account information about current token
     * <p>
     * API GET /api/auth/[version]/me/account
     *
     * @return New instance of the request.
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/auth.yaml#/[Me]_Identity">API Documentation</a>
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
         * @return A {@link HttpResponseParser} containing the InputStream of the image, the mediaType and the size
         * (if available).
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        @Override
        public HttpResponseParser execute() throws HiroException, IOException, InterruptedException {
            return getBinary(
                    getUri("me/avatar", query, fragment),
                    headers,
                    httpRequestTimeout,
                    maxRetries);
        }
    }

    /**
     * get avatar of current token's account
     * <p>
     * API GET /api/auth/[version]/me/avatar
     *
     * @return New instance of the request.
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/auth.yaml#/[Me]_Identity/get_me_avatar">API Documentation</a>
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
         * @return A String with the size of the uploaded avatar.
         * @throws HiroException            When the call returns a http status error.
         * @throws IOException              When the call got an IO error.
         * @throws InterruptedException     When the call gets interrupted.
         * @throws IllegalArgumentException When the Content-Type is missing.
         */
        @Override
        public String execute() throws HiroException, IOException, InterruptedException {
            notBlank(streamContainer.getContentType(), "contentType");
            return executeBinary(
                    getUri("me/avatar", query, fragment),
                    "PUT",
                    streamContainer,
                    headers,
                    httpRequestTimeout,
                    maxRetries).consumeResponseAsString();
        }
    }

    /**
     * set avatar of current token's account
     * <p>
     * API PUT /api/auth/[version]/me/avatar
     *
     * @param streamContainer The existing {@link StreamContainer}. Must not be null.
     * @return New instance of the request.
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/auth.yaml#/[Me]_Identity/put_me_avatar">API Documentation</a>
     */
    public PutMeAvatar putMeAvatar(StreamContainer streamContainer) {
        return new PutMeAvatar(streamContainer);
    }

    /**
     * set avatar of current token's account
     * <p>
     * API PUT /api/auth/[version]/me/avatar
     *
     * @param inputStream The inputStream for the request body. Must not be null.
     * @return New instance of the request.
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/auth.yaml#/[Me]_Identity/put_me_avatar">API Documentation</a>
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
    public class PutMePassword extends SendJsonAPIRequestConf<PutMePassword, HiroVertexMessage> {

        protected PutMePassword() {
        }

        public PutMePassword setPasswords(String oldPassword, String newPassword) {
            try {
                body = JsonUtil.DEFAULT.toString(
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
         * @return A {@link HiroVertexMessage} with the result data.
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        @Override
        public HiroVertexMessage execute() throws HiroException, IOException, InterruptedException {
            return put(
                    HiroVertexMessage.class,
                    getUri("me/password", query, fragment),
                    notBlank(body, "body"),
                    headers,
                    httpRequestTimeout,
                    maxRetries);
        }
    }

    /**
     * change password of current token's account
     * <p>
     * API PUT /api/auth/[version]/me/password
     *
     * @return New instance of the request.
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/auth.yaml#/[Me]_Identity/put_me_password">API Documentation</a>
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
    public class GetMeProfile extends APIRequestConf<GetMeProfile, HiroVertexMessage> {

        protected GetMeProfile() {
        }

        @Override
        protected GetMeProfile self() {
            return this;
        }

        /**
         * @return A {@link HiroVertexMessage} with the result data.
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        @Override
        public HiroVertexMessage execute() throws HiroException, IOException, InterruptedException {
            return get(
                    HiroVertexMessage.class,
                    getUri("me/profile", query, fragment),
                    headers,
                    httpRequestTimeout,
                    maxRetries);
        }
    }

    /**
     * get profile of current token's account
     * <p>
     * API GET /api/auth/[version]/me/profile
     *
     * @return New instance of the request.
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/auth.yaml#/[Me]_Identity/get_me_profile">API Documentation</a>
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
    public class PostMeProfile extends SendJsonAPIRequestConf<PostMeProfile, HiroVertexMessage> {

        protected PostMeProfile() {
        }

        @Override
        protected PostMeProfile self() {
            return this;
        }

        /**
         * @return A {@link HiroVertexMessage} with the result data.
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        @Override
        public HiroVertexMessage execute() throws HiroException, IOException, InterruptedException {
            return post(
                    HiroVertexMessage.class,
                    getUri("me/profile", query, fragment),
                    notBlank(body, "body"),
                    headers,
                    httpRequestTimeout,
                    maxRetries);
        }
    }

    /**
     * change profile of current token's account
     * <p>
     * API POST /api/auth/[version]/me/profile
     *
     * @return New instance of the request.
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/auth.yaml#/[Me]_Identity/post_me_profile">API Documentation</a>
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
    public class GetMeRoles extends APIRequestConf<GetMeRoles, HiroMessage> {

        protected GetMeRoles() {
        }

        @Override
        protected GetMeRoles self() {
            return this;
        }

        /**
         * @return A {@link HiroMessage} with the result data.
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        @Override
        public HiroMessage execute() throws HiroException, IOException, InterruptedException {
            return get(
                    HiroMessage.class,
                    getUri("me/roles", query, fragment),
                    headers,
                    httpRequestTimeout,
                    maxRetries);
        }
    }

    /**
     * get roles connected to of current token teams
     * <p>
     * API GET /api/auth/[version]/me/roles
     *
     * @return New instance of the request.
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/auth.yaml#/[Me]_Identity/get_me_roles">API Documentation</a>
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
    public class GetMeTeams extends APIRequestConf<GetMeTeams, HiroVertexListMessage> {

        protected GetMeTeams() {
        }

        /**
         * @param includeVirtual Query parameter "include-virtual=[true|false]".
         * @return {@link #self()}
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
         * @return A {@link HiroVertexListMessage} with the result data.
         * @throws HiroException        When the call returns a http status error.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        @Override
        public HiroVertexListMessage execute() throws HiroException, IOException, InterruptedException {
            return get(
                    HiroVertexListMessage.class,
                    getUri("me/teams", query, fragment),
                    headers,
                    httpRequestTimeout,
                    maxRetries);
        }
    }

    /**
     * gets the teams connected to current token
     * <p>
     * API GET /api/auth/[version]/me/teams
     *
     * @return New instance of the request.
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/auth.yaml#/[Me]_Identity/get_me_teams">API Documentation</a>
     */
    public GetMeTeams getMeTeams() {
        return new GetMeTeams();
    }
}
