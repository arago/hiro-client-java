package co.arago.hiro.client.model;

import co.arago.hiro.client.util.JsonTools;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.apache.commons.lang3.StringUtils;

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
public class HiroError extends HiroMessage {

    private static final long serialVersionUID = -5474697322866873485L;

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

    @JsonProperty("error")
    public HiroErrorEntry error;

    /**
     * Creates the intended {@link HiroErrorEntry} from either one of the two JSON data formats.
     *
     * @param errorObj The value for the key "error" in the source JSON. Must be either a Map or a String.
     * @see HiroError
     */
    @JsonSetter("error")
    public void setError(Object errorObj) {
        if (errorObj instanceof Map)
            this.error = JsonTools.DEFAULT.toObject(errorObj, HiroErrorEntry.class);
        if (errorObj instanceof String)
            this.error = new HiroErrorEntry((String) errorObj, null);
    }

    @JsonIgnore
    public String getMessage() {
        return (error != null ? error.message : null);
    }

    @JsonIgnore
    public Integer getCode() {
        return (error != null ? error.code : null);
    }

}
