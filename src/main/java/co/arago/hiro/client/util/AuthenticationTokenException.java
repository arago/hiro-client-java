package co.arago.hiro.client.util;

/**
 * An exception with authentication for HIRO
 */
public class AuthenticationTokenException extends HiroException {

    public AuthenticationTokenException(final String message, final int code, final String body) {
        super(message, code, body);
    }

    public AuthenticationTokenException(final String message, final int code, final String body, final Throwable t) {
        super(message, code, body, t);
    }

}
