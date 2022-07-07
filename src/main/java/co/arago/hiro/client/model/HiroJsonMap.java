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
public class HiroJsonMap implements JsonMessage, ToMap {

    private static final long serialVersionUID = 2435046646890203816L;

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
     * Returns a value from the {@link #fieldsMap}, which are fields that have NOT been assigned to fields of derived
     * children directly.
     *
     * @param key The key in the {@link #fieldsMap}.
     * @return The value found in {@link #fieldsMap} or null if not found.
     */
    @JsonIgnore
    public Object getUnmappedField(String key) {
        return fieldsMap.get(key);
    }

    /**
     * Returns any field found in this class, being inside {@link #fieldsMap} or a field of a derived class.
     * <p>
     * (This is a costly operation to be called multiple times on the same object. It might be more efficient to use
     * {@link #toMap()} and get the fields from the map returned.)
     *
     * @param key The name of the field.
     * @return The value found.
     */
    @JsonIgnore
    public Object getField(String key) {
        return toMap().get(key);
    }

    /**
     * Returns the map of fields that have NOT been assigned to fields of derived children directly.
     *
     * @return The map of the remaining fields.
     */
    @JsonAnyGetter
    public Map<String, ?> getMap() {
        return fieldsMap;
    }

    /**
     * Return a map of ALL values, including assigned fields specified in derived child classes.
     *
     * @return The map of all fields of this object.
     */
    @JsonIgnore
    public Map<String, Object> toMap() {
        return JsonUtil.DEFAULT.transformObject(this, new TypeReference<>() {
        });
    }
}
