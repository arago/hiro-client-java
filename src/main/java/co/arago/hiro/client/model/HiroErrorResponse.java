package co.arago.hiro.client.model;

import co.arago.hiro.client.util.JsonTools;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.io.Serializable;
import java.util.Map;


/**
 * Handles error responses from HIRO of the format.
 *
 * <code>
 * <pre>
 * {
 *     "error": {
 *         "message": "error message",
 *         "code": number
 *     }
 * }
 * </pre>
 * </code>
 * <p>
 * or
 *
 * <code>
 * <pre>
 * {
 *     "error": "error message"
 * }
 * </pre>
 * </code>
 */
public class HiroErrorResponse extends HiroResponse {

    private static final long serialVersionUID = -5474697322866873485L;
    @JsonProperty("error")
    public HiroErrorEntry error;

    public HiroErrorResponse() {
    }

    /**
     * Creates the intended {@link HiroErrorEntry} from either one of the two JSON data formats.
     *
     * @param errorObj The value for the key "error" in the source JSON. Must be either a Map or a String.
     * @see HiroErrorResponse
     */
    @JsonSetter("error")
    public void setError(Object errorObj) {
        if (errorObj instanceof Map)
            this.error = JsonTools.DEFAULT.toObject(errorObj, HiroErrorEntry.class);
        if (errorObj instanceof String)
            this.error = new HiroErrorEntry((String) errorObj, null);
    }

    @JsonIgnore
    public String getHiroErrorMessage() {
        return (error != null ? error.message : null);
    }

    @JsonIgnore
    public Integer getHiroErrorCode() {
        return (error != null ? error.code : null);
    }

    public static class HiroErrorEntry implements Serializable {
        private static final long serialVersionUID = 960096627468148981L;
        public String message;
        public Integer code;

        public HiroErrorEntry() {
        }

        public HiroErrorEntry(
                String message,
                Integer code
        ) {
            this.message = message;
            this.code = code;
        }
    }
}
