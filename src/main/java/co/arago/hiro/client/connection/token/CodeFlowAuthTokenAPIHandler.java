package co.arago.hiro.client.connection.token;

import co.arago.hiro.client.connection.AbstractVersionAPIHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.exceptions.HiroHttpException;
import co.arago.hiro.client.model.token.CodeFlowTokenRequest;
import co.arago.hiro.client.model.token.TokenResponse;
import co.arago.hiro.client.util.httpclient.HttpHeaderMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class CodeFlowAuthTokenAPIHandler extends AbstractRemoteAuthTokenAPIHandler {

    final static Logger log = LoggerFactory.getLogger(CodeFlowAuthTokenAPIHandler.class);

    // ###############################################################################################
    // ## Conf and Builder ##
    // ###############################################################################################

    public static abstract class Conf<T extends Conf<T>> extends AbstractRemoteAuthTokenAPIHandler.Conf<T> {
        private String code;
        private String codeVerifier;

        private String redirectUri;

        public String getCode() {
            return code;
        }

        /**
         * @param code Code value given back via initial redirect.
         * @return {@link #self()}
         */
        public T setCode(String code) {
            this.code = code;
            return self();
        }

        public String getCodeVerifier() {
            return codeVerifier;
        }

        /**
         * @param codeVerifier PKCE Code verifier for the code challenge of the initial redirect call.
         * @return {@link #self()}
         */
        public T setCodeVerifier(String codeVerifier) {
            this.codeVerifier = codeVerifier;
            return self();
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        /**
         * @param redirectUri Redirect uri from the initial redirect.
         * @return {@link #self()}
         */
        public T setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
            return self();
        }

        /**
         * Shorthand to set all credentials at once.
         *
         * @param code         Code value given back via initial redirect.
         * @param codeVerifier PKCE Code verifier for the code challenge of the initial redirect call.
         * @param redirectUri  Redirect uri from the initial redirect.
         * @param clientId     HIRO client_id of app
         * @return {@link #self()}
         */
        public T setCredentials(String code, String codeVerifier, String redirectUri, String clientId) {
            setCode(code);
            setCodeVerifier(codeVerifier);
            setRedirectUri(redirectUri);
            setClientId(clientId);
            return self();
        }

        public abstract CodeFlowAuthTokenAPIHandler build();
    }

    public static final class Builder extends Conf<Builder> {

        @Override
        protected Builder self() {
            return this;
        }

        public CodeFlowAuthTokenAPIHandler build() {
            return new CodeFlowAuthTokenAPIHandler(this);
        }
    }

    // ###############################################################################################
    // ## Main part ##
    // ###############################################################################################

    protected final String code;
    protected final String codeVerifier;
    protected final String redirectUri;

    protected CodeFlowAuthTokenAPIHandler(Conf<?> builder) {
        super(builder);
        this.code = notBlank(builder.getCode(), "code");
        this.codeVerifier = notBlank(builder.getCodeVerifier(), "codeVerifier");
        this.redirectUri = notBlank(builder.getRedirectUri(), "redirectUri");
    }

    /**
     * Special Copy Constructor. Uses the connection of another existing AbstractVersionAPIHandler.
     *
     * @param versionAPIHandler The AbstractVersionAPIHandler with the source data.
     * @param builder           Only configuration specific to a PasswordAuthTokenAPIHandler, see {@link Conf}, will
     *                          be copied from the builder. The AbstractVersionAPIHandler overwrites everything else.
     */
    public CodeFlowAuthTokenAPIHandler(
            AbstractVersionAPIHandler versionAPIHandler,
            Conf<?> builder) {
        super(versionAPIHandler, builder);
        this.code = notBlank(builder.getCode(), "code");
        this.codeVerifier = notBlank(builder.getCodeVerifier(), "codeVerifier");
        this.redirectUri = notBlank(builder.getRedirectUri(), "redirectUri");
    }

    public static Conf<?> newBuilder() {
        return new Builder();
    }

    /**
     * Obtain a new token from the auth API.
     *
     * @param organization   : Optional organization name. May be null.
     * @param organizationId : Optional organization id. May be null.
     * @throws InterruptedException When call gets interrupted.
     * @throws IOException          When call has IO errors.
     * @throws HiroException        On Hiro protocol / handling errors.
     */
    @Override
    protected void requestToken(String organization, String organizationId)
            throws IOException, InterruptedException, HiroException {
        if (organization != null)
            this.organization = organization;
        if (organizationId != null)
            this.organizationId = organizationId;

        float authApiVersion = Float.parseFloat(getVersionMap().getVersionEntryOf("auth").version);

        TokenResponse tokenResponse;

        if (authApiVersion < 6.6f)
            throw new HiroHttpException("Auth api version /api/auth/[version] has to be at least 6.6.", 500, null);

        CodeFlowTokenRequest tokenRequest = new CodeFlowTokenRequest(
                code, codeVerifier, redirectUri, clientId, clientSecret, organization, organizationId);

        tokenResponse = post(
                TokenResponse.class,
                getUri("token"),
                tokenRequest.toFormString(),
                new HttpHeaderMap(Map.of("Content-Type", "application/x-www-form-urlencoded")),
                httpRequestTimeout,
                maxRetries);

        this.tokenInfo.parse(tokenResponse);
    }
}
