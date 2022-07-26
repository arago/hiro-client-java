package co.arago.hiro.client.connection.token;

import co.arago.hiro.client.exceptions.AuthenticationTokenException;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.exceptions.HiroHttpException;
import co.arago.hiro.client.exceptions.TokenUnauthorizedException;
import co.arago.hiro.client.model.token.AuthorizeRequest;
import co.arago.hiro.client.model.token.CodeFlowTokenRequest;
import co.arago.hiro.client.model.token.TokenResponse;
import co.arago.hiro.client.util.PkceUtil;
import co.arago.hiro.client.util.httpclient.HttpHeaderMap;
import co.arago.hiro.client.util.httpclient.URIEncodedData;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import static co.arago.util.validation.ValueChecks.notBlank;

public class CodeFlowAuthTokenAPIHandler extends AbstractRemoteAuthTokenAPIHandler {

    final static Logger log = LoggerFactory.getLogger(CodeFlowAuthTokenAPIHandler.class);

    // ###############################################################################################
    // ## Conf and Builder ##
    // ###############################################################################################

    public static abstract class Conf<T extends Conf<T>> extends AbstractRemoteAuthTokenAPIHandler.Conf<T> {

        private String redirectURI;

        private String scope;

        public String getRedirectURI() {
            return redirectURI;
        }

        /**
         * @param redirectURI Redirect uri from the initial redirect.
         * @return {@link #self()}
         */
        public T setRedirectURI(String redirectURI) {
            this.redirectURI = redirectURI;
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
         * @param redirectURI Redirect uri from the initial redirect.
         * @param clientId    HIRO client_id of app
         * @return {@link #self()}
         */
        public T setCredentials(String redirectURI, String clientId) {
            setRedirectURI(redirectURI);
            setClientId(clientId);
            return self();
        }

        @Override
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

    protected final String redirectURI;

    protected final String scope;

    private final PkceUtil pkceUtil = new PkceUtil();

    protected String code;

    protected String state;

    protected CodeFlowAuthTokenAPIHandler(Conf<?> builder) {
        super(builder);
        this.redirectURI = notBlank(builder.getRedirectURI(), "redirectURI");
        this.scope = builder.getScope();
    }

    public static Conf<?> newBuilder() {
        return new Builder();
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
    public URI getAuthorizeURI() throws HiroException, IOException, InterruptedException {
        try {
            // Create new state and code_verifier with each call.
            state = PkceUtil.generateRandomBase64(16);
            pkceUtil.initialize();

            return addQueryFragmentAndNormalize(
                    getURI("authorize"),
                    new URIEncodedData(new AuthorizeRequest(
                            clientId,
                            redirectURI,
                            pkceUtil.getCodeChallenge(),
                            pkceUtil.getCodeChallengeMethod(),
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
            throw new TokenUnauthorizedException("parameter \"code\" has either been used before or never been set.", 400,
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
                code, pkceUtil.getCodeVerifier(), redirectURI, clientId, clientSecret, organization, organizationId);

        tokenResponse = post(
                TokenResponse.class,
                getURI("token"),
                tokenRequest.toURIEncodedStringRemoveBlanks(),
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
            throw new AuthenticationTokenException("no refresh token available", 400, null);

        super.refreshToken();
    }
}
