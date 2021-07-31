package co.arago.hiro.client.exceptions;

/**
 * a base exception for HIRO, contains an error code
 */
public class HiroHttpException extends HiroException {

    private final int code;
    private final String body;

    /**
     * Constructs a new exception with the specified detail message.  The
     * cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     * @param code    The code for the error (Similar to a HTTP status code).
     * @param body    The body - if any.
     */
    public HiroHttpException(String message, int code, String body) {
        super(message);
        this.code = code;
        this.body = body;
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
     * @param body    The body - if any.
     * @param cause   the cause (which is saved for later retrieval by the
     *                {@link #getCause()} method).  (A {@code null} value is
     *                permitted, and indicates that the cause is nonexistent or
     *                unknown.)
     * @since 1.4
     */
    public HiroHttpException(String message, int code, String body, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.body = body;
    }

    /**
     * Constructs a new exception with the specified detail message,
     * cause, suppression enabled or disabled, and writable stack
     * trace enabled or disabled.
     *
     * @param message            the detail message.
     * @param code               The code for the error (Similar to a HTTP status code).
     * @param body               The body - if any.
     * @param cause              the cause.  (A {@code null} value is permitted,
     *                           and indicates that the cause is nonexistent or unknown.)
     * @param enableSuppression  whether or not suppression is enabled
     *                           or disabled
     * @param writableStackTrace whether or not the stack trace should
     *                           be writable
     * @since 1.7
     */
    public HiroHttpException(String message, int code, String body, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.code = code;
        this.body = body;
    }

    /**
     * get the error code
     *
     * @return The status code
     */
    public int getCode() {
        return code;
    }

    /**
     * get the body
     *
     * @return body or null if no body is available.
     */
    public String getBody() {
        return body;
    }

    @Override
    public String toString() {
        return this.getClass().getName() + ": (" + code + ") " + getMessage() + (body != null ? ": " + body : "");
    }
}
