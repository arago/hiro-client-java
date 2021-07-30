package co.arago.hiro.client.model;

import co.arago.hiro.client.util.JsonTools;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * This is a renaming of a LinkedHashMap&lt;String, Object&gt; to avoid unmapped errors when using Jackson.
 * Also adds some HIRO specific functions like handling Lists under { "Items": [] }.
 */
public class HiroResponse extends LinkedHashMap<String, Object> {

    /**
     * Get an internal map of fields from the key "items" if it exists and is a list.
     *
     * @return The list of items or null if no such list exists.
     */
    public List<HiroResponse> getItems() {
        Object items = get("items");
        if (items instanceof List) {
            HiroItemData hiroItemMap = JsonTools.DEFAULT.toObject(this, HiroItemData.class);
            return hiroItemMap.items;
        }

        return null;
    }

    /**
     * Check for a meta value field.
     *
     * @param key The key of the field.
     * @return true if the field is a List, false otherwise.
     */
    public Boolean isMetaValueField(String key) {
        return (get(key) instanceof List);
    }

    /**
     * Get an attribute field value.
     *
     * @param key The key of the field.
     * @return The value of the field as String. If the value is a MetaValueList, create a csv from all the values.
     * If the value is a number or a boolean, null will be returned !!
     */
    public String getAttributeField(String key) {
        Object value = get(key);

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
    public MetaValueList getAttributeMetaValueField(String key) {
        Object value = get(key);

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
