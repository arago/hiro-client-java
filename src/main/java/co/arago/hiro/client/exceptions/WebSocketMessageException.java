package co.arago.hiro.client.exceptions;

/**
 * A base exception for WebSockets. Contains an error code.
 */
public class WebSocketMessageException extends HiroException {

    private static final long serialVersionUID = -4933538297607290220L;

    private final int code;

    /**
     * Constructs a new exception with the specified detail message.  The
     * cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     * @param code    The code for the error (Similar to a HTTP status code).
     */
    public WebSocketMessageException(String message, int code) {
        super(message);
        this.code = code;
    }

    /**
     * Constructs a new exception with the specified detail message and
     * cause.  <p>Note that the detail message associated with
     * {@code cause} is <i>not</i> automatically incorporated in
     * this exception's detail message.
     *
     * @param message the detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     * @param code    The code for the error (Similar to a HTTP status code).
     * @param cause   the cause (which is saved for later retrieval by the
     *                {@link #getCause()} method).  (A {@code null} value is
     *                permitted, and indicates that the cause is nonexistent or
     *                unknown.)
     * @since 1.4
     */
    public WebSocketMessageException(String message, int code, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * Constructs a new exception with the specified detail message,
     * cause, suppression enabled or disabled, and writable stack
     * trace enabled or disabled.
     *
     * @param message            the detail message.
     * @param code               The code for the error (Similar to a HTTP status code).
     * @param cause              the cause.  (A {@code null} value is permitted,
     *                           and indicates that the cause is nonexistent or unknown.)
     * @param enableSuppression  whether or not suppression is enabled
     *                           or disabled
     * @param writableStackTrace whether or not the stack trace should
     *                           be writable
     * @since 1.7
     */
    public WebSocketMessageException(String message, int code, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.code = code;
    }

    /**
     * get the error code
     *
     * @return The status code
     */
    public int getCode() {
        return code;
    }

    @Override
    public String toString() {
        return this.getClass().getName() + ": (" + code + ") " + getMessage();
    }
}
