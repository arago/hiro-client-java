package co.arago.hiro.client.exceptions;

/**
 * Used when tokens expire with error 401.
 */
public class RefreshTokenWebSocketException extends WebSocketException {

    private static final long serialVersionUID = -1339343734517969528L;

    public RefreshTokenWebSocketException(final String message) {
        super(message);
    }

    public RefreshTokenWebSocketException(final String message, final Throwable t) {
        super(message, t);
    }
}
