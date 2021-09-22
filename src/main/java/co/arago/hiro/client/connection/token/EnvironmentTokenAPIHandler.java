package co.arago.hiro.client.connection.token;

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
         * @param tokenEnv Name of the environment variable. Default is "HIRO_TOKEN".
         * @return {@link #self()}
         */
        public T setTokenEnv(String tokenEnv) {
            this.tokenEnv = tokenEnv;
            return self();
        }
    }

    public static final class Builder extends Conf<Builder> {

        @Override
        protected Builder self() {
            return this;
        }

        public EnvironmentTokenAPIHandler build() {
            return new EnvironmentTokenAPIHandler(this);
        }

    }

    // ###############################################################################################
    // ## Main part ##
    // ###############################################################################################

    protected final String tokenEnv;

    /**
     * Constructor.
     *
     * @param builder The builder to use.
     */
    protected EnvironmentTokenAPIHandler(Conf<?> builder) {
        super(builder);
        this.tokenEnv = builder.getTokenEnv();
    }

    public static Builder newBuilder() {
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
