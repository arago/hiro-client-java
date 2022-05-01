package co.arago.hiro.client.util.httpclient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A special map that contains a string as key and a list of string as value. Will not contain null values.
 * This map is used for multi-value-maps like http headers and uri query parameters.
 */
public class MultiValueMap {

    protected final LinkedHashMap<String, List<String>> map = new LinkedHashMap<>();

    /**
     * Constructor without data.
     */
    public MultiValueMap() {
    }

    /**
     * Constructor
     *
     * @param initialValues A map of initial values. The values need to be either String or Collection&lt;String&gt;.
     */
    public MultiValueMap(Map<String, ?> initialValues) {
        for (Map.Entry<String, ?> entry : initialValues.entrySet()) {
            put(entry.getKey(), (String) entry.getValue());
        }
    }

    /**
     * Clear list and set values.
     *
     * @param initialValues A map of initial values. The values need to be either String or Collection&lt;String&gt;.
     *                      ATTENTION: If a key with a value == null is encountered, all values under this key will be
     *                      removed.
     */
    public void putAll(Map<String, ?> initialValues) {
        map.clear();

        for (Map.Entry<String, ?> entry : initialValues.entrySet()) {
            put(entry.getKey(), (String) entry.getValue());
        }
    }

    /**
     * Put a single value into the map or remove it.
     *
     * @param key   The key for the value. Overwrites all previous values of that key.
     * @param value The value itself. ATTENTION: If this is null, the key and its values will be removed from the map.
     */
    public void put(String key, String value) {
        appendAtKeyInMapOrRemove(new ArrayList<>(), key, value);
    }

    /**
     * Add values. These will be appended to the lists of existing values if possible.
     *
     * @param values A map of values. The values need to be either String or Collection&lt;String&gt;.
     *               ATTENTION: If a key with a value == null is encountered, all values under this key will be removed.
     */
    public void addAll(Map<String, ?> values) {
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            add(entry.getKey(), (String) entry.getValue());
        }
    }

    /**
     * Add a single value into the map or remove it.
     *
     * @param key   The key for the value. If the key already has values, the value is appended.
     * @param value The value itself. ATTENTION: If this is null, the key and its values will be removed from the map.
     */
    public void add(String key, String value) {
        List<String> existingValues = map.get(key);
        List<String> valueList = (existingValues == null) ? new ArrayList<>() : existingValues;

        appendAtKeyInMapOrRemove(valueList, key, value);
    }

    /**
     * Obtain the first value for a key.
     *
     * @param key Key for a value.
     * @return The first value found.
     */
    public String getFirst(String key) {
        return map.get(key).iterator().next();
    }

    /**
     * Add (or remove) an entry to the map.
     *
     * @param valueList The value list where the value will be appended to.
     * @param key       The key for the entry in the map.
     * @param value     The value to be appended. ATTENTION: If this is null, the key and its values will be
     *                  removed from the map.
     * @throws ClassCastException When the value is not null, not of type String nor Collection&lt;String&gt;.
     */
    private void appendAtKeyInMapOrRemove(List<String> valueList, String key, Object value) {
        if (value == null) {
            map.remove(key);
            return;
        }

        if (value instanceof String) {
            valueList.add((String) value);
        } else if (value instanceof Collection) {
            for (Object collectionEntry : (Collection<?>) value) {
                if (collectionEntry instanceof String) {
                    valueList.add((String) collectionEntry);
                } else {
                    throw new ClassCastException("Values need to be of String type.");
                }
            }
        } else {
            throw new ClassCastException("Value needs to be of String type.");
        }

        map.put(key, valueList);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MultiValueMap that = (MultiValueMap) o;
        return Objects.equals(map, that.map);
    }

    @Override
    public int hashCode() {
        return Objects.hash(map);
    }
}
