package co.arago.hiro.client.model;

import co.arago.util.json.JsonUtil;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This is a generic model class that collects all keys and values of a JSON object in {@link #fieldsMap}.
 * The keys need to be strings. Derived children will specify specific fields for JSON data, while all remaining
 * data will be collected in this map.
 */
public abstract class JacksonJsonMap implements JsonMessage {
    /**
     * All unknown keys - if any - of the JSON will be collected here
     */
    @JsonIgnore
    protected final Map<String, Object> fieldsMap = new LinkedHashMap<>();

    @JsonAnySetter
    public void setField(String key, Object value) {
        fieldsMap.put(key, value);
    }

    /**
     * Returns the map of fields that have NOT been assigned to fields of derived children directly.
     *
     * @return The map of the remaining fields.
     */
    @JsonAnyGetter
    public Map<String, Object> getMap() {
        return fieldsMap;
    }

    /**
     * Return a map of ALL values, including assigned fields specified in derived child classes.
     *
     * @return The map of all fields of this object.
     */
    @JsonIgnore
    public Map<String, Object> toMap() {
        return JsonUtil.DEFAULT.toObject(this, new TypeReference<>() {
        });
    }
}
