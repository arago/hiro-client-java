package co.arago.hiro.client.util.httpclient;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A special map that contains a string as key and a list of string as value. Will not contain null values.
 * This map is used for multi-value-maps like http headers and uri query parameters.
 */
public class MultiValueMap {

    protected final Map<String, List<String>> map = new LinkedHashMap<>();

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

    public MultiValueMap(MultiValueMap other) {
        appendAll(other);
    }

    /**
     * Clear map and set values.
     *
     * @param initialValues A map of initial values. The values need to be either String or Collection&lt;String&gt;.
     *                      If this is null, the map will be just cleared.
     *                      ATTENTION: If a key with a value == null is encountered, all values under this key will be
     *                      removed.
     */
    public void setAll(Map<String, ?> initialValues) {
        map.clear();

        if (initialValues == null)
            return;

        initialValues.forEach(this::appendAtKey);
    }

    /**
     * Clear map and set values.
     *
     * @param other Data will be copied over from here.
     *              If this is null, the map will be just cleared.
     */
    public void setAll(MultiValueMap other) {
        map.clear();
        appendAll(other);
    }

    /**
     * Put a single value into the map or remove it.
     *
     * @param key   The key for the value. Overwrites all previous values of that key.
     * @param value The value itself. ATTENTION: If this is null, the key and its values will be removed from the map.
     */
    public void set(String key, String value) {
        map.remove(key);
        appendAtKey(key, value);
    }

    /**
     * Put a list of values into the map or remove it.
     *
     * @param key    The key for the values. Overwrites all previous values of that key.
     * @param values The values themselves. ATTENTION: If this is null, the key and its values will be removed from the map.
     */
    public void set(String key, Collection<String> values) {
        map.remove(key);
        appendAtKey(key, values);
    }

    /**
     * Add values. These will be appended to the lists of existing values if possible.
     *
     * @param other Data will be copied over from here.
     */
    public void appendAll(MultiValueMap other) {
        if (other == null)
            return;

        other.map.forEach((key, values) -> map.put(key, new ArrayList<>(values)));
    }

    /**
     * Add values. These will be appended to the lists of existing values if possible.
     *
     * @param values A map of values. The values need to be either String or Collection&lt;String&gt;.
     *               ATTENTION: If a key with a value == null is encountered, all values under this key will be removed.
     */
    public void appendAll(Map<String, ?> values) {
        if (values == null)
            return;

        values.forEach(this::appendAtKey);
    }

    /**
     * Add a single value into the map or remove it.
     *
     * @param key   The key for the value. If the key already has values, the value is appended.
     * @param value The value itself. ATTENTION: If this is null, the key and its values will be removed from the map.
     */
    public void append(String key, String value) {
        appendAtKey(key, value);
    }

    /**
     * Add a multiple values into the map or remove it.
     *
     * @param key    The key for the values. If the key already has values, the values are appended.
     * @param values The values themselves. ATTENTION: If this is null, the key and its values will be removed from the map.
     */
    public void append(String key, Collection<String> values) {
        appendAtKey(key, values);
    }

    /**
     * Obtain the first value for a key.
     *
     * @param key Key for a value.
     * @return The first value found.
     */
    public String getFirst(String key) {
        List<String> found = getAll(key);

        return found != null ? found.iterator().next() : null;
    }

    /**
     * Obtain the first value for a key. Ignore case of key.
     *
     * @param key Key for a value.
     * @return The first value found.
     */
    public String getFirstIgnoreCase(String key) {
        List<String> found = getAllIgnoreCase(key);

        return found != null ? found.iterator().next() : null;
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
     * Obtain all values for a key. Ignore case of key.
     *
     * @param key Key for a value list.
     * @return List of all values under that key.
     */
    public List<String> getAllIgnoreCase(String key) {
        Map.Entry<String, List<String>> found = map.entrySet().stream()
                .filter(entry -> StringUtils.equalsIgnoreCase(key, entry.getKey()))
                .findFirst().orElse(null);

        return found != null ? found.getValue() : null;
    }

    /**
     * Add an entry in the map to the list named by key unless the value is null.
     *
     * @param key   The key for the list in the map.
     * @param value The value to be appended. Must be String, Collection&lt;String&gt; or null. null values will be
     *              ignored silently.
     * @throws ClassCastException When the value is not null, not of type String nor Collection&lt;String&gt;.
     */
    private void appendAtKey(String key, Object value) {
        if (key == null)
            return;

        List<String> valueList = map.computeIfAbsent(key, k -> new ArrayList<>());

        if (value instanceof String) {
            valueList.add((String) value);
        } else if (value instanceof Collection) {
            ((Collection<?>) value).forEach(collectionValue -> valueList.add((String) collectionValue));
        } else if (value != null) {
            throw new ClassCastException("Value needs to be of String or Collection<String> type.");
        }

        if (valueList.isEmpty())
            map.remove(key);
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

    /**
     * @return A deep copy of the internal map.
     */
    public Map<String, List<String>> getMapCopy() {
        Map<String, List<String>> clone = new LinkedHashMap<>();
        map.forEach((key, values) -> clone.put(key, new ArrayList<>(values)));
        return clone;
    }

    /**
     * @return Create a map with keys and only the first value of the arrays behind those keys.
     */
    public Map<String, String> toSingleValueMap() {
        Map<String, String> singleValueMap = new HashMap<>();
        map.forEach((key, values) -> singleValueMap.put(key, values.iterator().next()));
        return singleValueMap;
    }

}
