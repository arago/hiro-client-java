package co.arago.hiro.client.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;

/**
 * This contains a LinkedHashMap&lt;String, Object&gt; {@link #fieldsMap} (from {@link JacksonJsonMap} for the generic
 * incoming data.
 * <p>
 * This class is meant to parse HTTP responses and WebSocket messages received from HIRO.
 *
 * <pre>
 * {
 *     "key": [any value]
 *     ...
 * }
 * </pre>
 */
public class HiroMessage extends JacksonJsonMap {

    protected HiroError hiroError;

    /**
     * Set the field in {@link #fieldsMap} unless an error message is found via {@link #catchError(String, Object)}.
     *
     * @param key   The name of the field.
     * @param value The value of the field.
     */
    @Override
    public void setField(String key, Object value) {
        if (catchError(key, value))
            return;

        super.setField(key, value);
    }

    /**
     * If the key is "error", create {@link #hiroError} from the value.
     *
     * @param key   The name of the field.
     * @param value The value of the field.
     * @return true if an error was found (key is "error"), false otherwise.
     */
    protected boolean catchError(String key, Object value) {
        if (StringUtils.equals(key, "error")) {
            hiroError = new HiroError(value);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks for a received error.
     *
     * @return true or false
     */
    @JsonIgnore
    public boolean isError() {
        return (hiroError != null);
    }

    /**
     * Gets the received error in the response.
     *
     * @return The HiroErrorResponse or null if no error is present.
     */
    @JsonIgnore
    public HiroError getError() {
        return hiroError;
    }
}
