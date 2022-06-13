package co.arago.hiro.client.connection.token;

import co.arago.hiro.client.connection.AbstractVersionAPIHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.model.token.PasswordTokenRequest;
import co.arago.hiro.client.model.token.TokenResponse;
import co.arago.hiro.client.util.httpclient.HttpHeaderMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class PasswordAuthTokenAPIHandler extends AbstractRemoteAuthTokenAPIHandler {

    final static Logger log = LoggerFactory.getLogger(PasswordAuthTokenAPIHandler.class);

    // ###############################################################################################
    // ## Conf and Builder ##
    // ###############################################################################################

    public static abstract class Conf<T extends Conf<T>> extends AbstractRemoteAuthTokenAPIHandler.Conf<T> {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        /**
         * @param username HIRO username for user account
         * @return {@link #self()}
         */
        public T setUsername(String username) {
            this.username = username;
            return self();
        }

        public String getPassword() {
            return password;
        }

        /**
         * @param password HIRO password for user account
         * @return {@link #self()}
         */
        public T setPassword(String password) {
            this.password = password;
            return self();
        }

        /**
         * Shorthand to set all credentials at once.
         *
         * @param username     HIRO username for user account
         * @param password     HIRO password for user account
         * @param clientId     HIRO client_id of app
         * @param clientSecret HIRO client_secret of app
         * @return {@link #self()}
         */
        public T setCredentials(String username, String password, String clientId, String clientSecret) {
            setUsername(username);
            setPassword(password);
            setClientId(clientId);
            setClientSecret(clientSecret);
            return self();
        }

        public abstract PasswordAuthTokenAPIHandler build();
    }

    public static final class Builder extends Conf<Builder> {

        @Override
        protected Builder self() {
            return this;
        }

        public PasswordAuthTokenAPIHandler build() {
            return new PasswordAuthTokenAPIHandler(this);
        }
    }

    // ###############################################################################################
    // ## Main part ##
    // ###############################################################################################

    protected final String username;
    protected final String password;

    protected PasswordAuthTokenAPIHandler(Conf<?> builder) {
        super(builder);
        notBlank(clientSecret, "clientSecret");
        this.username = notBlank(builder.getUsername(), "username");
        this.password = notBlank(builder.getPassword(), "password");
    }

    /**
     * Special Copy Constructor. Uses the connection of another existing AbstractVersionAPIHandler.
     *
     * @param versionAPIHandler The AbstractVersionAPIHandler with the source data.
     * @param builder           Only configuration specific to a PasswordAuthTokenAPIHandler, see {@link Conf}, will
     *                          be copied from the builder. The AbstractVersionAPIHandler overwrites everything else.
     */
    public PasswordAuthTokenAPIHandler(
            AbstractVersionAPIHandler versionAPIHandler,
            Conf<?> builder) {
        super(versionAPIHandler, builder);
        notBlank(clientSecret, "clientSecret");
        this.username = notBlank(builder.getUsername(), "username");
        this.password = notBlank(builder.getPassword(), "password");
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

        PasswordTokenRequest tokenRequest = new PasswordTokenRequest(
                username, password, clientId, clientSecret, organization, organizationId);

        if (authApiVersion >= 6.6f) {
            tokenResponse = post(
                    TokenResponse.class,
                    getUri("token"),
                    tokenRequest.toEncodedString(),
                    new HttpHeaderMap(Map.of("Content-Type", "application/x-www-form-urlencoded")),
                    httpRequestTimeout,
                    maxRetries);
        } else {
            tokenResponse = post(
                    TokenResponse.class,
                    getUri("app"),
                    tokenRequest.toJsonStringNoNull(),
                    new HttpHeaderMap(Map.of("Content-Type", "application/json")),
                    httpRequestTimeout,
                    maxRetries);
        }

        this.tokenInfo.parse(tokenResponse);
    }
}
