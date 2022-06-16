package co.arago.hiro.client.connection.token;

import co.arago.hiro.client.connection.AbstractVersionAPIHandler;
import co.arago.hiro.client.exceptions.FixedTokenException;
import co.arago.hiro.client.exceptions.HiroException;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;

public class EnvironmentTokenAPIHandler extends AbstractTokenAPIHandler {

    // ###############################################################################################
    // ## Conf and Builder ##
    // ###############################################################################################

    public static abstract class Conf<T extends Conf<T>> extends AbstractTokenAPIHandler.Conf<T> {
        private String tokenEnv;

        public String getTokenEnv() {
            return tokenEnv;
        }

        /**
         * @param tokenEnv Name of the environment variable. Blank values are ignored. Default is
         *                 {@link EnvironmentTokenAPIHandler#DEFAULT_ENV}.
         * @return {@link #self()}
         */
        public T setTokenEnv(String tokenEnv) {
            return self();
        }

        public abstract EnvironmentTokenAPIHandler build();
    }

    public static final class Builder extends Conf<Builder> {

        @Override
        protected Builder self() {
            return this;
        }

        public EnvironmentTokenAPIHandler build() {
            return sharedConnectionHandler != null ? new EnvironmentTokenAPIHandler(sharedConnectionHandler, this)
                    : new EnvironmentTokenAPIHandler(this);
        }

    }

    // ###############################################################################################
    // ## Main part ##
    // ###############################################################################################

    public final static String DEFAULT_ENV = "HIRO_TOKEN";

    protected final String tokenEnv;

    /**
     * Constructor.
     *
     * @param builder The builder to use.
     */
    protected EnvironmentTokenAPIHandler(Conf<?> builder) {
        super(builder);
        this.tokenEnv = StringUtils.isBlank(builder.getTokenEnv()) ? DEFAULT_ENV : builder.getTokenEnv();
    }

    /**
     * Special Copy Constructor. Uses the connection of another existing AbstractVersionAPIHandler.
     *
     * @param versionAPIHandler The AbstractVersionAPIHandler with the source data.
     * @param builder           The builder with the configuration data for this specific class.
     */
    protected EnvironmentTokenAPIHandler(AbstractVersionAPIHandler versionAPIHandler, Conf<?> builder) {
        super(versionAPIHandler);
        this.tokenEnv = StringUtils.isBlank(builder.getTokenEnv()) ? DEFAULT_ENV : builder.getTokenEnv();
    }

    public static Conf<?> newBuilder() {
        return new Builder();
    }

    /**
     * Return the current token from the System environment.
     *
     * @return The current token.
     */
    @Override
    public String getToken() {
        return System.getenv(tokenEnv);
    }

    /**
     * Refresh an invalid token.
     */
    @Override
    public void refreshToken() throws HiroException {
        throw new FixedTokenException("Cannot change an environment token.");
    }

    /**
     * Revoke a token
     */
    @Override
    public void revokeToken() throws HiroException {
        throw new FixedTokenException("Cannot revoke a fixed token.");
    }

    /**
     * Check for existence of a token in the TokenAPIHandler.
     *
     * @return true if a token has been set or retrieved, false if the token is empty.
     */
    @Override
    public boolean hasToken() {
        return StringUtils.isNotBlank(getToken());
    }

    /**
     * Check for existence of a refresh token in the TokenAPIHandler.
     *
     * @return false
     */
    @Override
    public boolean hasRefreshToken() {
        return false;
    }

    /**
     * Calculate the Instant after which the token should be refreshed.
     *
     * @return The Instant after which the token shall be refreshed. null if token cannot be refreshed.
     */
    @Override
    public Instant expiryInstant() {
        return null;
    }

}
