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
public class HiroJsonMap implements JsonMessage {
    /**
     * All unknown keys - if any - of the JSON will be collected here
     */
    @JsonIgnore
    protected final Map<String, Object> fieldsMap = new LinkedHashMap<>();

    /**
     * Default constructor for Jackson
     */
    public HiroJsonMap() {
    }

    /**
     * Constructor
     *
     * @param initialData Initial data for the fieldsMap. Data will be copied via {@link Map#putAll(Map)}
     *                    into {@link #fieldsMap}.
     */
    public HiroJsonMap(Map<String, Object> initialData) {
        addFields(initialData);
    }

    /**
     * Set a single field in {@link #fieldsMap}. This is also the generic method for Jackson to set fields.
     *
     * @param key   Key of the field.
     * @param value Value of the field.
     */
    @JsonAnySetter
    public void setField(String key, Object value) {
        fieldsMap.put(key, value);
    }

    /**
     * @param data {@link #fieldsMap} will be cleared and data will be copied via {@link Map#putAll(Map)} into it.
     */
    @JsonIgnore
    public void setFields(Map<String, Object> data) {
        fieldsMap.clear();
        addFields(data);
    }

    /**
     * @param data Data will be copied via {@link Map#putAll(Map)} into {@link #fieldsMap}.
     */
    @JsonIgnore
    public void addFields(Map<String, Object> data) {
        fieldsMap.putAll(data);
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
