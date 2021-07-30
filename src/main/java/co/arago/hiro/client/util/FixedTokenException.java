package co.arago.hiro.client.util;

/**
 * Used when tokens are fixed and cannot be refreshed.
 */
public class FixedTokenException extends AuthenticationTokenException {

    public FixedTokenException(final String message, final int code, final String body) {
        super(message, code, body);
    }

    public FixedTokenException(final String message, final int code, final String body, final Throwable t) {
        super(message, code, body, t);
    }

}
