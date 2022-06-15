package co.arago.hiro.client.connection.token;

import co.arago.hiro.client.connection.AbstractVersionAPIHandler;
import co.arago.hiro.client.exceptions.AuthenticationTokenException;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.exceptions.HiroHttpException;
import co.arago.hiro.client.exceptions.TokenUnauthorizedException;
import co.arago.hiro.client.model.token.AuthorizeRequest;
import co.arago.hiro.client.model.token.CodeFlowTokenRequest;
import co.arago.hiro.client.model.token.TokenResponse;
import co.arago.hiro.client.util.PkceUtil;
import co.arago.hiro.client.util.httpclient.HttpHeaderMap;
import co.arago.hiro.client.util.httpclient.UriEncodedMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public class CodeFlowAuthTokenAPIHandler extends AbstractRemoteAuthTokenAPIHandler {

    final static Logger log = LoggerFactory.getLogger(CodeFlowAuthTokenAPIHandler.class);

    // ###############################################################################################
    // ## Conf and Builder ##
    // ###############################################################################################

    public static abstract class Conf<T extends Conf<T>> extends AbstractRemoteAuthTokenAPIHandler.Conf<T> {

        private String redirectUri;

        private String scope;

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

        public String getScope() {
            return scope;
        }

        /**
         * @param scope Optional authorization scope (not used atm).
         * @return {@link #self()}
         */
        public T setScope(String scope) {
            this.scope = scope;
            return self();
        }

        /**
         * Shorthand to set all credentials at once.
         *
         * @param redirectUri Redirect uri from the initial redirect.
         * @param clientId    HIRO client_id of app
         * @return {@link #self()}
         */
        public T setCredentials(String redirectUri, String clientId) {
            setRedirectUri(redirectUri);
            setClientId(clientId);
            return self();
        }

        public abstract CodeFlowAuthTokenAPIHandler build();
    }

    public static final class Builder extends Conf<Builder> {

        private final AbstractVersionAPIHandler versionAPIHandler;

        public Builder() {
            versionAPIHandler = null;
        }

        public Builder(AbstractVersionAPIHandler versionAPIHandler) {
            this.versionAPIHandler = versionAPIHandler;
        }

        @Override
        protected Builder self() {
            return this;
        }

        public CodeFlowAuthTokenAPIHandler build() {
            return versionAPIHandler != null ? new CodeFlowAuthTokenAPIHandler(versionAPIHandler, this)
                    : new CodeFlowAuthTokenAPIHandler(this);
        }
    }

    // ###############################################################################################
    // ## Main part ##
    // ###############################################################################################

    protected final String redirectUri;

    protected final String scope;

    private final PkceUtil pkceUtil = new PkceUtil();

    protected String code;

    protected String state = PkceUtil.generateRandomBase64(16);

    protected CodeFlowAuthTokenAPIHandler(Conf<?> builder) {
        super(builder);
        this.redirectUri = notBlank(builder.getRedirectUri(), "redirectUri");
        this.scope = builder.getScope();
    }

    /**
     * Special Copy Constructor. Uses the connection of another existing AbstractVersionAPIHandler.
     *
     * @param versionAPIHandler The AbstractVersionAPIHandler with the source data.
     * @param builder           Only configuration specific to a CodeFlowAuthTokenAPIHandler, see {@link Conf}, will
     *                          be copied from the builder. The AbstractVersionAPIHandler overwrites everything else.
     */
    protected CodeFlowAuthTokenAPIHandler(
            AbstractVersionAPIHandler versionAPIHandler,
            Conf<?> builder) {
        super(versionAPIHandler, builder);
        this.redirectUri = notBlank(builder.getRedirectUri(), "redirectUri");
        this.scope = builder.getScope();
    }

    public static Conf<?> newBuilder() {
        return new Builder();
    }

    /**
     * Special Copy Constructor Builder. Uses the connection of another existing AbstractVersionAPIHandler.
     *
     * @param versionAPIHandler The AbstractVersionAPIHandler with the source data.
     * @return A new builder
     */
    public static Conf<?> newBuilder(AbstractVersionAPIHandler versionAPIHandler) {
        return new Builder(versionAPIHandler);
    }

    /**
     * Generate a URI for a browser to use for the authorization call. This call will be answered with a
     * redirection to a login page.
     *
     * @return The URI for a login page.
     * @throws IOException          On a failed call to /api/version
     * @throws InterruptedException Call got interrupted
     * @throws HiroException        When calling /api/version responds with an error
     */
    public URI getAuthorizeUri() throws HiroException, IOException, InterruptedException {
        try {
            return addQueryFragmentAndNormalize(
                    getUri("authorize"),
                    new UriEncodedMap(new AuthorizeRequest(
                            clientId,
                            redirectUri,
                            pkceUtil.getCodeChallenge(),
                            state,
                            scope).toMap()),
                    null);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Handle authorization callback data.
     *
     * @param state The state returned by the callback. Must not have changed.
     * @param code  The one-time-code returned (used to obtain a full authorization token).
     * @throws AuthenticationTokenException When the state of the callback does not match the internal state.
     */
    public void handleAuthorizeCallback(String state, String code) throws AuthenticationTokenException {
        if (!StringUtils.equals(state, this.state))
            throw new AuthenticationTokenException("The parameter 'state' of the callback does not match.", 400, null);

        this.code = code;
    }

    /**
     * Obtain a new token from the auth API.
     *
     * @param organization   : Optional organization name. May be null.
     * @param organizationId : Optional organization id. May be null.
     * @throws InterruptedException       When call gets interrupted.
     * @throws IOException                When call has IO errors.
     * @throws HiroException              On Hiro protocol / handling errors.
     * @throws TokenUnauthorizedException When {@link #code} is blank, i.e. the code has never been submitted via
     *                                    {@link #handleAuthorizeCallback(String state, String code)} or has already
     *                                    been used.
     */
    @Override
    protected void requestToken(String organization, String organizationId)
            throws IOException, InterruptedException, HiroException {

        if (StringUtils.isBlank(code))
            throw new TokenUnauthorizedException("parameter \"code\" has either been used before or never been set.", 401,
                    null);

        if (organization != null)
            this.organization = organization;
        if (organizationId != null)
            this.organizationId = organizationId;

        float authApiVersion = Float.parseFloat(getVersionMap().getVersionEntryOf("auth").version);

        TokenResponse tokenResponse;

        if (authApiVersion < 6.6f)
            throw new HiroHttpException("Auth api version /api/auth/[version] has to be at least 6.6.", 500, null);

        CodeFlowTokenRequest tokenRequest = new CodeFlowTokenRequest(
                code, pkceUtil.getCodeVerifier(), redirectUri, clientId, clientSecret, organization, organizationId);

        tokenResponse = post(
                TokenResponse.class,
                getUri("token"),
                tokenRequest.toEncodedString(),
                new HttpHeaderMap(Map.of("Content-Type", "application/x-www-form-urlencoded")),
                httpRequestTimeout,
                maxRetries);

        this.tokenInfo.parse(tokenResponse);
        this.code = null;
    }

    /**
     * Refresh an invalid token.
     * <p>
     * Uses the internal values of organization and organizationId if present.
     *
     * @throws InterruptedException       When call gets interrupted.
     * @throws IOException                When call has IO errors.
     * @throws HiroException              On Hiro protocol / handling errors.
     * @throws TokenUnauthorizedException When no refresh_token exists.
     */
    @Override
    public synchronized void refreshToken() throws HiroException, IOException, InterruptedException {
        if (!hasRefreshToken())
            throw new AuthenticationTokenException("no refresh token available", 401, null);

        super.refreshToken();
    }
}
