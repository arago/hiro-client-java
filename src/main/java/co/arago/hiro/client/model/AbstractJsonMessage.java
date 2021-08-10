package co.arago.hiro.client.model;

import co.arago.hiro.client.util.JsonTools;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.Serializable;

/**
 * This interface defines a default method to create a JSON String from this object.
 */
public interface AbstractJsonMessage extends Serializable {
    /**
     * @return JSON representation of this object. Returns "" on JsonProcessingException (which should never happen).
     */
    default String toJsonString() {
        try {
            return JsonTools.DEFAULT.toString(this);
        } catch (JsonProcessingException e) {
            return ""; // should never happen
        }
    }
}
