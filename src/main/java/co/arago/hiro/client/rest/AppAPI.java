package co.arago.hiro.client.rest;

import co.arago.hiro.client.connection.token.TokenAPIHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.model.HiroMessage;
import co.arago.hiro.client.model.vertex.HiroVertexListMessage;
import co.arago.hiro.client.model.vertex.HiroVertexMessage;
import co.arago.hiro.client.util.httpclient.HttpResponseParser;

import java.io.IOException;

import static co.arago.util.validation.ValueChecks.notBlank;
import static co.arago.util.validation.ValueChecks.notEmpty;

public class AppAPI extends AuthenticatedAPIHandler {

    // ###############################################################################################
    // ## Conf and Builder ##
    // ###############################################################################################

    public static abstract class Conf<T extends Conf<T>> extends AuthenticatedAPIHandler.Conf<T> {

        public abstract AppAPI build();
    }

    public static final class Builder extends Conf<Builder> {

        private Builder(String apiName, TokenAPIHandler tokenAPIHandler) {
            setApiName(apiName);
            setTokenApiHandler(tokenAPIHandler);
        }

        @Override
        protected Builder self() {
            return this;
        }

        public AppAPI build() {
            return new AppAPI(this);
        }
    }

    // ###############################################################################################
    // ## Main part ##
    // ###############################################################################################

    /**
     * Protected constructor. Create this {@link AppAPI} by using its {@link Builder}.
     *
     * @param builder The builder to use.
     */
    protected AppAPI(Conf<?> builder) {
        super(builder);
    }

    /**
     * Get a {@link Builder} for {@link AppAPI}.
     *
     * @param tokenAPIHandler The API handler for this websocket.
     * @return The {@link Builder} for {@link AppAPI}.
     */
    public static Conf<?> newBuilder(TokenAPIHandler tokenAPIHandler) {
        return new Builder("app", tokenAPIHandler);
    }

    // ###############################################################################################
    // ## API Requests ##
    // ###############################################################################################

    // ----------------------------------- GetApp -----------------------------------

    /**
     * Get an application
     * <p>
     * API GET /api/app/[version]/{ogit\_id}
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/app.yaml#/[Application]/get__id_">API Documentation</a>
     */
    public class GetApplicationCommand extends APIRequestConf<GetApplicationCommand, HiroVertexMessage> {

        /**
         * @param ogitId ogit/_id of the app.
         */
        protected GetApplicationCommand(String ogitId) {
            super(notBlank(ogitId, "ogitId"));
        }

        @Override
        protected GetApplicationCommand self() {
            return this;
        }

        /**
         * @return A {@link HiroVertexMessage} with the result data.
         * @throws HiroException        TokenAPIHandler.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        @Override
        public HiroVertexMessage execute() throws HiroException, IOException, InterruptedException {
            return get(
                    HiroVertexMessage.class,
                    getEndpointURI(path, query, fragment),
                    headers,
                    httpRequestTimeout,
                    maxRetries);
        }
    }

    /**
     * Get an application
     * <p>
     * API GET /api/app/[version]/{ogit\_id}
     *
     * @param ogitId ogit/_id of the app.
     * @return New instance of the command. Use method "execute()" after all parameters have been set to run the command.
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/app.yaml#/[Application]/get__id_">API Documentation</a>
     */
    public GetApplicationCommand getApplicationCommand(String ogitId) {
        return new GetApplicationCommand(ogitId);
    }

    // ----------------------------------- GetConfig -----------------------------------

    /**
     * Get an application instance config
     * <p>
     * API GET /api/app/[version]/config
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/app.yaml#/[Config]/get_config">API Documentation</a>
     */
    public class GetConfigCommand extends APIRequestConf<GetConfigCommand, HiroMessage> {

        protected GetConfigCommand() {
            super("config");
        }

        @Override
        protected GetConfigCommand self() {
            return this;
        }

        /**
         * @return A {@link HiroMessage} with the result data.
         * @throws HiroException        TokenAPIHandler.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        @Override
        public HiroMessage execute() throws HiroException, IOException, InterruptedException {
            return get(
                    HiroMessage.class,
                    getEndpointURI(path, query, fragment),
                    headers,
                    httpRequestTimeout,
                    maxRetries);
        }
    }

    /**
     * Get an application instance config
     * <p>
     * API GET /api/app/[version]/config
     *
     * @return New instance of the command. Use method "execute()" after all parameters have been set to run the command.
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/app.yaml#/[Config]/get_config">API Documentation</a>
     */
    public GetConfigCommand getConfigCommand() {
        return new GetConfigCommand();
    }

    // ----------------------------------- GetContent -----------------------------------

    /**
     * Get the content of an application
     * <p>
     * API GET /api/app/[version]/{ogit/_id}/content/{path}
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/app.yaml#/[Content]/get__id__content__path_">API Documentation</a>
     */
    public class GetContentCommand extends APIRequestConf<GetContentCommand, HttpResponseParser> {

        /**
         * @param ogitId ogit/_id of the app.
         * @param path   Path parts for the subcontent.
         */
        protected GetContentCommand(String ogitId, String... path) {
            super(notBlank(ogitId, "ogitId"), "content");
            this.path.append(notEmpty("path", path));
        }

        @Override
        protected GetContentCommand self() {
            return this;
        }

        /**
         * @return A {@link HttpResponseParser} with the result data.
         * @throws HiroException        TokenAPIHandler.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        @Override
        public HttpResponseParser execute() throws HiroException, IOException, InterruptedException {
            return getBinary(
                    getEndpointURI(path, query, fragment),
                    headers,
                    httpRequestTimeout,
                    maxRetries);
        }
    }

    /**
     * Get the content of an application
     * <p>
     * API GET /api/app/[version]/{ogit/_id}/content/{path}
     *
     * @param ogitId ogit/_id of the app.
     * @param path   Path parts for the subcontent.
     * @return New instance of the command. Use method "execute()" after all parameters have been set to run the command.
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/app.yaml#/[Content]/get__id__content__path_">API Documentation</a>
     */
    public GetContentCommand getContentCommand(String ogitId, String... path) {
        return new GetContentCommand(ogitId, path);
    }

    // ----------------------------------- GetManifest -----------------------------------

    /**
     * Get a manifest of the content of an application
     * <p>
     * API GET /api/app/[version]/{ogit/_id}/manifest
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/app.yaml#/[Content]/get__id__manifest">API Documentation</a>
     */
    public class GetManifestCommand extends APIRequestConf<GetManifestCommand, HiroMessage> {

        /**
         * @param ogitId ogit/_id of the app.
         */
        protected GetManifestCommand(String ogitId) {
            super(notBlank(ogitId, "ogitId"), "manifest");
        }

        @Override
        protected GetManifestCommand self() {
            return this;
        }

        /**
         * @return A {@link HiroMessage} with the result data.
         * @throws HiroException        TokenAPIHandler.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        @Override
        public HiroMessage execute() throws HiroException, IOException, InterruptedException {
            return get(
                    HiroMessage.class,
                    getEndpointURI(path, query, fragment),
                    headers,
                    httpRequestTimeout,
                    maxRetries);
        }
    }

    /**
     * Get a manifest of the content of an application
     * <p>
     * API GET /api/app/[version]/{ogit/_id}/manifest
     *
     * @param ogitId ogit/_id of the app.
     * @return New instance of the command. Use method "execute()" after all parameters have been set to run the command.
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/app.yaml#/[Content]/get__id__manifest">API Documentation</a>
     */
    public GetManifestCommand getManifestCommand(String ogitId) {
        return new GetManifestCommand(ogitId);
    }

    // ----------------------------------- GetDesktopApplications -----------------------------------

    /**
     * Returns a list of all visible desktop applications
     * <p>
     * API GET /api/app/[version]/desktop
     *
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/app.yaml#/[Desktop]/get_desktop">API Documentation</a>
     */
    public class GetDesktopApplicationsCommand extends APIRequestConf<GetDesktopApplicationsCommand, HiroVertexListMessage> {

        protected GetDesktopApplicationsCommand() {
            super("desktop");
        }

        @Override
        protected GetDesktopApplicationsCommand self() {
            return this;
        }

        /**
         * @return A {@link HiroVertexListMessage} with the result data.
         * @throws HiroException        TokenAPIHandler.
         * @throws IOException          When the call got an IO error.
         * @throws InterruptedException When the call gets interrupted.
         */
        @Override
        public HiroVertexListMessage execute() throws HiroException, IOException, InterruptedException {
            return get(
                    HiroVertexListMessage.class,
                    getEndpointURI(path, query, fragment),
                    headers,
                    httpRequestTimeout,
                    maxRetries);
        }
    }

    /**
     * Returns a list of all visible desktop applications
     * <p>
     * API GET /api/app/[version]/desktop
     *
     * @return New instance of the command. Use method "execute()" after all parameters have been set to run the command.
     * @see <a href="https://core.arago.co/help/specs/?url=definitions/app.yaml#/[Desktop]/get_desktop">API Documentation</a>
     */
    public GetDesktopApplicationsCommand getDesktopApplicationsCommand() {
        return new GetDesktopApplicationsCommand();
    }
}
