package co.arago.hiro.client.connection;

import co.arago.hiro.client.util.FixedTokenException;
import co.arago.hiro.client.util.HiroException;

import java.io.InputStream;
import java.net.http.HttpResponse;
import java.time.Instant;

public class EnvironmentTokenAPIHandler extends AbstractTokenAPIHandler {

    public abstract static class Conf<B extends Conf<B>> extends AbstractTokenAPIHandler.Conf<B> {
        protected  String tokenEnv = "HIRO_TOKEN";

        /**
         * @param tokenEnv The name of the environment variable.
         */
        public B setTokenEnv(String tokenEnv) {
            this.tokenEnv = tokenEnv;
            return self();
        }

        abstract EnvironmentTokenAPIHandler build();
    }

    public static final class Builder extends Conf<Builder> {
        @Override
        Builder self() {
            return this;
        }

        @Override
        EnvironmentTokenAPIHandler build() {
            return new EnvironmentTokenAPIHandler(this);
        }
    }

    private final String tokenEnv;

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Constructor.
     *
     * @param builder The builder to use.
     */
    protected EnvironmentTokenAPIHandler(Conf<?> builder) {
        super(builder);
        this.tokenEnv = builder.tokenEnv;
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
    public void refreshToken() {
        throw new FixedTokenException("Cannot change an environment token.", 500, null);
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
