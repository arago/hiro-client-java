package co.arago.hiro.client.connection.token;

import co.arago.hiro.client.connection.AbstractVersionAPIHandler;
import co.arago.hiro.client.exceptions.FixedTokenException;
import co.arago.hiro.client.exceptions.HiroException;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;

public class FixedTokenAPIHandler extends AbstractTokenAPIHandler {

    // ###############################################################################################
    // ## Conf and Builder ##
    // ###############################################################################################

    public static abstract class Conf<T extends Conf<T>> extends AbstractTokenAPIHandler.Conf<T> {
        private String token;

        public String getToken() {
            return token;
        }

        /**
         * Set a static token
         *
         * @param token The token string
         * @return {@link #self()}
         */
        public T setToken(String token) {
            this.token = token;
            return self();
        }

        public abstract FixedTokenAPIHandler build();
    }

    public static final class Builder extends Conf<Builder> {

        @Override
        protected Builder self() {
            return this;
        }

        public FixedTokenAPIHandler build() {
            return sharedConnectionHandler != null ? new FixedTokenAPIHandler(sharedConnectionHandler, this)
                    : new FixedTokenAPIHandler(this);
        }

    }

    // ###############################################################################################
    // ## Main part ##
    // ###############################################################################################

    protected final String token;

    /**
     * Constructor
     *
     * @param builder The builder to use.
     */
    protected FixedTokenAPIHandler(Conf<Builder> builder) {
        super(builder);
        this.token = notNull(builder.getToken(), "token");
    }

    /**
     * Constructor
     *
     * @param versionAPIHandler The AbstractVersionAPIHandler with the source data.
     * @param builder           The builder to use for all specific data for this class.
     */
    protected FixedTokenAPIHandler(AbstractVersionAPIHandler versionAPIHandler, Conf<Builder> builder) {
        super(versionAPIHandler);
        this.token = notNull(builder.getToken(), "token");
    }

    public static Conf<?> newBuilder() {
        return new Builder();
    }

    /**
     * Return the current token.
     *
     * @return The current token.
     */
    @Override
    public String getToken() {
        return token;
    }

    /**
     * Refresh an invalid token.
     */
    @Override
    public void refreshToken() throws HiroException {
        throw new FixedTokenException("Cannot change a fixed token.");
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
        return StringUtils.isNotBlank(token);
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
