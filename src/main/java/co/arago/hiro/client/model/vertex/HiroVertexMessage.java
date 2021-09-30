package co.arago.hiro.client.model.vertex;

import co.arago.hiro.client.model.HiroMessage;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;
import java.util.Map;

/**
 * Contains data of a HIRO vertex. Handles string and metaInfo list entries of vertices.
 *
 *
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
 *
 *
 * <p>
 * Because the types of attributes can be manifold, they remain stored in the HiroMessage
 * and will be cast to the correct types when reading them with {@link #getAttributeAsString(String)} or
 * {@link #getAttributeAsMetaValue(String)}.
 */
public class HiroVertexMessage extends HiroMessage {

    private static final long serialVersionUID = 4841532740228140969L;

    /**
     * Default constructor for Jackson
     */
    public HiroVertexMessage() {
    }

    /**
     * Constructor
     *
     * @param initialData Initial data for the fieldsMap.
     */
    public HiroVertexMessage(Map<String, Object> initialData) {
        super(initialData);
    }

    /**
     * Jackson Setter that transforms incoming lists into MetaValueLists.
     *
     * @param key   Incoming JSON key
     * @param value Incoming JSON value
     */
    @Override
    public void setField(String key, Object value) {
        if (catchError(key, value))
            return;

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
        return (fieldsMap.get(key) instanceof MetaValueList);
    }

    /**
     * Get an attribute field directly without converting anything.
     *
     * @param key The key of the field.
     * @return THe value of the key as Object.
     */
    @JsonIgnore
    public Object getAttribute(String key) {
        return fieldsMap.get(key);
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
        Object value = fieldsMap.get(key);

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
     *         returned.
     * @see #getAttribute(String)
     */
    @JsonIgnore
    public MetaValueList getAttributeAsMetaValue(String key) {
        Object value = fieldsMap.get(key);

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
