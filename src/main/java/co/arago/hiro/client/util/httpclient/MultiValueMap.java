package co.arago.hiro.client.util.httpclient;

import org.apache.commons.lang3.StringUtils;

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
        initialValues.forEach(this::appendAtKey);
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

        initialValues.forEach(this::appendAtKey);
    }

    /**
     * Put a single value into the map or remove it.
     *
     * @param key   The key for the value. Overwrites all previous values of that key.
     * @param value The value itself. ATTENTION: If this is null, the key and its values will be removed from the map.
     */
    public void put(String key, String value) {
        map.remove(key);
        appendAtKey(key, value);
    }

    /**
     * Add values. These will be appended to the lists of existing values if possible.
     *
     * @param values A map of values. The values need to be either String or Collection&lt;String&gt;.
     *               ATTENTION: If a key with a value == null is encountered, all values under this key will be removed.
     */
    public void addAll(Map<String, ?> values) {
        values.forEach(this::appendAtKey);
    }

    /**
     * Add a single value into the map or remove it.
     *
     * @param key   The key for the value. If the key already has values, the value is appended.
     * @param value The value itself. ATTENTION: If this is null, the key and its values will be removed from the map.
     */
    public void add(String key, String value) {
        appendAtKey(key, value);
    }

    /**
     * Obtain the first value for a key.
     *
     * @param key Key for a value.
     * @return The first value found.
     */
    public String getFirst(String key) {
        return getAll(key).iterator().next();
    }

    /**
     * Obtain all values for a key.
     *
     * @param key Key for a value list.
     * @return List of all values under that key.
     */
    public List<String> getAll(String key) {
        return map.get(key);
    }

    /**
     * Add an entry in the map to the list named by key unless the value is null or blank.
     *
     * @param key   The key for the list in the map.
     * @param value The value to be appended.
     * @throws ClassCastException When the value is not null, not of type String nor Collection&lt;String&gt;.
     */
    private void appendAtKey(String key, Object value) {
        List<String> valueList = new ArrayList<>();

        if (value instanceof String) {
            if (StringUtils.isNotBlank((String) value))
                valueList.add((String) value);
        } else if (value instanceof Collection) {
            ((Collection<?>) value).forEach(collectionValue -> {
                if (!(collectionValue instanceof String))
                    throw new ClassCastException("Values in Collection need to be of String type.");
                if (StringUtils.isNotBlank((String) collectionValue))
                    valueList.add((String) collectionValue);
            });
        } else if (value != null) {
            throw new ClassCastException("Value needs to be of String or Collection<String> type.");
        }

        if (!valueList.isEmpty())
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
        return Objects.hashCode(map);
    }
}
