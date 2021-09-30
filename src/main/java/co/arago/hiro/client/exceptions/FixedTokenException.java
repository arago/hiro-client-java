package co.arago.hiro.client.exceptions;

/**
 * Used when tokens are fixed and cannot be refreshed.
 */
public class FixedTokenException extends HiroException {

    private static final long serialVersionUID = -2975260718069366281L;

    /**
     * Constructs a new exception with the specified detail message. The
     * cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public FixedTokenException(String message) {
        super(message);
    }
}
