package co.arago.hiro.client.model;

import co.arago.util.json.JsonUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Handles error responses from HIRO of the format.
 *
 * <pre>
 * {
 *     "error": {
 *         "message": "error message",
 *         "code": number
 *     }
 * }
 * </pre>
 * <p>
 * or
 * 
 * <pre>
 * {
 *     "error": "error message"
 * }
 * </pre>
 */
public class HiroError extends HiroJsonMap {

    private static final long serialVersionUID = -8947894966816686991L;

    public static class HiroErrorEntry extends HiroJsonMap {
        /**
         * 
         */
        private static final long serialVersionUID = -7369549314100124499L;
        public String message;
        public Integer code;

        @JsonCreator
        public HiroErrorEntry(
                @JsonProperty("message") String message,
                @JsonProperty("code") Integer code) {
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
    @JsonCreator
    public HiroError(
            @JsonProperty("error") Object errorObj) {
        if (errorObj instanceof Map)
            this.error = JsonUtil.DEFAULT.transformObject(errorObj, HiroErrorEntry.class);
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
