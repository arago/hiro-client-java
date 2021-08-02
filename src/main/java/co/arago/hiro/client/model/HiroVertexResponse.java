package co.arago.hiro.client.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

/**
 * Contains data of a HIRO vertex.
 */
public class HiroVertexResponse extends HiroResponse {

    private static final long serialVersionUID = -7719281559398624722L;

    /**
     * Check for a meta value field.
     *
     * @param key The key of the field.
     * @return true if the field is a List, false otherwise.
     */
    @JsonIgnore
    public Boolean isMetaValueField(String key) {
        return (responseMap.get(key) instanceof List);
    }

    /**
     * Get an attribute field value.
     *
     * @param key The key of the field.
     * @return The value of the field as String. If the value is a MetaValueList, create a csv from all the values.
     * If the value is a number or a boolean, null will be returned !!
     */
    @JsonIgnore
    public String getAttributeField(String key) {
        Object value = responseMap.get(key);

        if (value instanceof String)
            return (String) value;

        if (value instanceof List)
            return MetaValueList.create(value).createSingleValue();

        return null;
    }

    /**
     * Get an attribute field as MetaValueField
     *
     * @param key The key of the field.
     * @return A MetaValueList. If the value is a String, a list with only one entry with the field 'value' will be
     * returned. If the value is a number or a boolean, null will be returned !!
     */
    @JsonIgnore
    public MetaValueList getAttributeMetaValueField(String key) {
        Object value = responseMap.get(key);

        if (value instanceof String) {
            MetaValueList list = new MetaValueList();
            list.add(new MetaValueField((String) value));
            return list;
        }

        if (value instanceof List)
            return MetaValueList.create(value);

        return null;
    }
}
