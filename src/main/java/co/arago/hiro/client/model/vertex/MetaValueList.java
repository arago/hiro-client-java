package co.arago.hiro.client.model.vertex;

import co.arago.hiro.client.model.HiroJsonMap;
import co.arago.hiro.client.model.JsonMessage;
import co.arago.util.json.JsonUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Implementation of a meta value from HIRO attributes in vertices.
 *
 *
 * <pre>
 * [
 *     {
 *         "value": "value",
 *         "created": [epoch ms],
 *         "key": "key value"
 *     }
 * ]
 * </pre>
 */
public class MetaValueList extends ArrayList<MetaValueList.MetaValueField> implements JsonMessage {

    private static final long serialVersionUID = 8233445177105381442L;

    public static class MetaValueField extends HiroJsonMap {
        public String value;
        public Long created;
        public String key;

        public MetaValueField(String value) {
            this(value, null, null);
        }

        @JsonCreator
        public MetaValueField(
                @JsonProperty("value") String value,
                @JsonProperty("created") Long created,
                @JsonProperty("key") String key
        ) {
            this.value = value;
            this.created = created;
            this.key = key;
        }

        public static MetaValueField create(Object object) {
            return JsonUtil.DEFAULT.toObject(object, MetaValueField.class);
        }

    }

    public static MetaValueList create(Object object) {
        return JsonUtil.DEFAULT.toObject(object, MetaValueList.class);
    }

    public String createSingleValue() {
        return this.stream().map(v -> v.value).collect(Collectors.joining(","));
    }

}
