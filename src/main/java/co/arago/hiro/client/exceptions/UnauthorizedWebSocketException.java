package co.arago.hiro.client.exceptions;

/**
 * Used when tokens expire with error 401.
 */
public class UnauthorizedWebSocketException extends WebSocketMessageException {

    private static final long serialVersionUID = -1637351596069365162L;

    public UnauthorizedWebSocketException(final String message, final int code) {
        super(message, code);
    }

    public UnauthorizedWebSocketException(final String message, final int code, final Throwable t) {
        super(message, code, t);
    }
}
