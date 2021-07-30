package co.arago.hiro.client.model;

import co.arago.hiro.client.util.JsonTools;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

public class MetaValueField {
    public String value;
    public Long created;
    public String key;

    public MetaValueField(String value) {
        this.value = value;
    }

    public MetaValueField(String value, Long created, String key) {
        this.value = value;
        this.created = created;
        this.key = key;
    }

    /**
     * To store any additional fields that were unknown at time of writing this
     */
    private final Map<String, Object> fieldMap = new HashMap<>();

    @JsonAnySetter
    public void setField(String key, Object value) {
        fieldMap.put(key, value);
    }

    public Object getField(String key) {
        return fieldMap.get(key);
    }

    public static MetaValueField create(Object object) {
        return JsonTools.DEFAULT.toObject(object, MetaValueField.class);
    }

}
