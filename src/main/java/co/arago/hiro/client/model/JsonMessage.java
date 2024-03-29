package co.arago.hiro.client.model;

import co.arago.util.json.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.Serializable;

/**
 * This interface defines a default method to create a JSON String from this object.
 */
public interface JsonMessage extends Serializable {

    /**
     * @return JSON representation of this object.
     * @throws JsonProcessingException When no JSON string can be created from this object.
     */
    default String toJsonString() throws JsonProcessingException {
        return JsonUtil.DEFAULT.toString(this);
    }

    /**
     * @return JSON representation of this object without null values.
     * @throws JsonProcessingException When no JSON string can be created from this object.
     */
    default String toJsonStringNoNull() throws JsonProcessingException {
        return JsonUtil.SKIP_NULL.toString(this);
    }

    /**
     * @return Pretty formatted JSON representation of this object.
     * @throws JsonProcessingException When no JSON string can be created from this object.
     */
    default String toPrettyJsonString() throws JsonProcessingException {
        return JsonUtil.DEFAULT.toPrettyString(this);
    }

    /**
     * @return Pretty formatted JSON representation of this object without null values.
     * @throws JsonProcessingException When no JSON string can be created from this object.
     */
    default String toPrettyJsonStringNoNull() throws JsonProcessingException {
        return JsonUtil.SKIP_NULL.toPrettyString(this);
    }
}
