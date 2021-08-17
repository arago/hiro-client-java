package co.arago.hiro.client.model;

import co.arago.hiro.client.util.JsonTools;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This contains a LinkedHashMap&lt;String, Object&gt; {@link #responseMap} for the generic incoming data.
 * Children will specify specific fields for JSON data, while all remaining data will be collected in this map.
 *
 * This class is meant to parse HTTP responses and WebSocket messages received from HIRO.
 *
 * <code>
 * <pre>
 * {
 *     "key": [any value]
 *     ...
 * }
 * </pre>
 * </code>
 */
public class HiroMessage implements AbstractJsonMessage {

    private static final long serialVersionUID = 2382405135196485958L;
    protected final Map<String, Object> responseMap = new LinkedHashMap<>();

    @JsonAnySetter
    public void setField(String key, Object value) {
        responseMap.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getMap() {
        return responseMap;
    }

    /**
     * Checks for a received error.
     *
     * @return true or false
     */
    @JsonIgnore
    public boolean isError() {
        return responseMap.containsKey("error");
    }

    /**
     * Gets the received error in the response.
     *
     * @return The HiroErrorResponse or null if no error is present.
     */
    @JsonIgnore
    public HiroError getError() {
        if (isError()) {
            return JsonTools.DEFAULT.toObject(responseMap, HiroError.class);
        }

        return null;
    }
}
