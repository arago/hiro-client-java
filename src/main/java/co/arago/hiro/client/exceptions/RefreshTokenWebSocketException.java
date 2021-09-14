package co.arago.hiro.client.exceptions;

/**
 * Used when tokens expire with error 401.
 */
public class RefreshTokenWebSocketException extends WebSocketException {

    public RefreshTokenWebSocketException(final String message) {
        super(message);
    }

    public RefreshTokenWebSocketException(final String message, final Throwable t) {
        super(message, t);
    }
}
