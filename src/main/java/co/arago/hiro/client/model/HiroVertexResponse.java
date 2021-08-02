package co.arago.hiro.client.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

/**
 * Contains data of a HIRO vertex. Handles string and metaInfo list entries of vertices.
 *
 * <code>
 * <pre>
 * {
 *     "attribute1": "value",
 *     "attribute2": [
 *          {
 *              "value": "value",
 *              "created": [epoch ms],
 *              "key": "key value"
 *          }
 *      ],
 *     "attribute2": ...
 * }
 * </pre>
 * </code>
 *
 * <p>
 * Because the types of attributes can be manifold, they remain stored in the HiroResponse
 * and will be cast to the correct types when reading them with {@link #getAttributeAsString(String)} or
 * {@link #getAttributeAsMetaValue(String)}.
 */
public class HiroVertexResponse extends HiroResponse {

    private static final long serialVersionUID = -7719281559398624722L;

    /**
     * Jackson Setter that transforms incoming lists into MetaValueLists.
     *
     * @param key   Incoming JSON key
     * @param value Incoming JSON value
     */
    @Override
    public void setField(String key, Object value) {
        super.setField(key, (value instanceof List ? MetaValueList.create(value) : value));
    }

    /**
     * Check for a meta value field.
     *
     * @param key The key of the field.
     * @return true if the field is a List, false otherwise.
     */
    @JsonIgnore
    public Boolean isMetaValueField(String key) {
        return (responseMap.get(key) instanceof MetaValueList);
    }

    /**
     * Get an attribute field directly without converting anything.
     *
     * @param key The key of the field.
     * @return THe value of the key as Object.
     */
    public Object getAttribute(String key) {
        return responseMap.get(key);
    }

    /**
     * Get an attribute field value.
     *
     * @param key The key of the field.
     * @return The value of the field as String. If the value is a MetaValueList, create a csv from all the values.
     * @see #getAttribute(String)
     */
    @JsonIgnore
    public String getAttributeAsString(String key) {
        Object value = responseMap.get(key);

        if (value instanceof MetaValueList) {
            return ((MetaValueList) value).createSingleValue();
        } else if (value != null) {
            return String.valueOf(value);
        }

        return null;
    }

    /**
     * Get an attribute field as MetaValueField
     *
     * @param key The key of the field.
     * @return A MetaValueList. If the value is a String, a list with only one entry with the field 'value' will be
     * returned.
     * @see #getAttribute(String)
     */
    @JsonIgnore
    public MetaValueList getAttributeAsMetaValue(String key) {
        Object value = responseMap.get(key);

        if (value instanceof MetaValueList) {
            return (MetaValueList) value;
        } else if (value != null) {
            MetaValueList list = new MetaValueList();
            list.add(new MetaValueList.MetaValueField(String.valueOf(value)));
            return list;
        }

        return null;
    }
}
