package co.arago.hiro.client.exceptions;

/**
 * An exception with authentication for HIRO
 */
public class AuthenticationTokenException extends HiroHttpException {

    public AuthenticationTokenException(final String message, final int code, final String body) {
        super(message, code, body);
    }

    public AuthenticationTokenException(final String message, final int code, final String body, final Throwable t) {
        super(message, code, body, t);
    }

}
