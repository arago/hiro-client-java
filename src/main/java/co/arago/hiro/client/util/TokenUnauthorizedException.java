package co.arago.hiro.client.util;

/**
 * Used when tokens expire with error 401.
 */
public class TokenUnauthorizedException extends AuthenticationTokenException {

    public TokenUnauthorizedException(final String message, final int code, final String body) {
        super(message, code, body);
    }

    public TokenUnauthorizedException(final String message, final int code, final String body, final Throwable t) {
        super(message, code, body, t);
    }

}
