package co.arago.hiro.client.model;

import co.arago.hiro.client.util.JsonTools;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.stream.Collectors;

/**
 * Implementation of a meta value from HIRO attributes in vertices.
 *
 * <code>
 * <pre>
 * [
 *     {
 *         "value": "value",
 *         "created": [epoch ms],
 *         "key": "key value"
 *     }
 * ]
 * </pre>
 * </code>
 *
 */
public class MetaValueList extends LinkedList<MetaValueList.MetaValueField> {

    private static final long serialVersionUID = 8233445177105381442L;

    public static class MetaValueField implements Serializable {
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

    public static MetaValueList create(Object object) {
        return JsonTools.DEFAULT.toObject(object, MetaValueList.class);
    }

    public String createSingleValue() {
        return this.stream().map(v -> v.value).collect(Collectors.joining(","));
    }
}
