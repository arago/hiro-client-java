package co.arago.hiro.client.model;

import co.arago.hiro.client.util.JsonTools;

public class MetaValueField extends HiroResponse {
    private static final long serialVersionUID = 475241931530978500L;
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

    public static MetaValueField create(Object object) {
        return JsonTools.DEFAULT.toObject(object, MetaValueField.class);
    }

}
