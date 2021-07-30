package co.arago.hiro.client.connection;

import co.arago.hiro.client.util.FixedTokenException;
import co.arago.hiro.client.util.HiroException;

import java.io.InputStream;
import java.net.http.HttpResponse;
import java.time.Instant;

public class FixedTokenAPIHandler extends AbstractTokenAPIHandler {

    public static abstract class Conf<B extends Conf<B>> extends AbstractTokenAPIHandler.Conf<B> {
        protected  String token;

        /**
         * @param token The static token to use.
         */
        public B setToken(String token) {
            this.token = token;
            return self();
        }

        abstract FixedTokenAPIHandler build();
    }

    public static final class Builder extends Conf<Builder> {
        @Override
        Builder self() {
            return this;
        }

        FixedTokenAPIHandler build() {
            return new FixedTokenAPIHandler(this);
        }
    }

    private final String token;

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Constructor
     *
     * @param builder The builder to use.
     */
    protected FixedTokenAPIHandler(Conf<?> builder) {
        super(builder);
        this.token = builder.token;
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
    public void refreshToken() {
        throw new FixedTokenException("Cannot change a fixed token.", 500, null);
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
