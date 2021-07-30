package co.arago.hiro.client.util;

import java.io.Serializable;

/**
 * a base exception for HIRO, contains an error code
 */
public class HiroException extends RuntimeException implements Serializable {

    private static final long serialVersionUID = 42L;
    private final int code;
    private final String body;
    private final transient Throwable cause;

    public HiroException(final String message, final int code, final String body) {
        super(message);

        this.code = code;
        this.body = body;
        this.cause = null;
    }

    public HiroException(final String message, final int code, final String body, final Throwable t) {
        super(message);

        this.code = code;
        this.body = body;
        this.cause = t;
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
    public synchronized Throwable getCause() {
        return (cause == this ? null : cause);
    }

    @Override
    public String toString() {
        return this.getClass().getName() + ": (" + String.valueOf(code) + ") " + getMessage() + (body != null ? ": " + body : "");
    }
}
