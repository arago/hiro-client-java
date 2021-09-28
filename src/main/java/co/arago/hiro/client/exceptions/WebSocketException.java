package co.arago.hiro.client.exceptions;

public class WebSocketException extends HiroException {
    private static final long serialVersionUID = 4116077569200887434L;

    public WebSocketException(String message) {
        super(message);
    }

    public WebSocketException(String message, Throwable cause) {
        super(message, cause);
    }

    public WebSocketException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
