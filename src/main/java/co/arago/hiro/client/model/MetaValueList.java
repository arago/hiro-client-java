package co.arago.hiro.client.model;

import co.arago.hiro.client.util.JsonTools;

import java.util.LinkedList;
import java.util.stream.Collectors;

public class MetaValueList extends LinkedList<MetaValueField> {

    private static final long serialVersionUID = 8233445177105381442L;

    public static MetaValueList create(Object object) {
        return JsonTools.DEFAULT.toObject(object, MetaValueList.class);
    }

    public String createSingleValue() {
        return this.stream().map(v -> v.value).collect(Collectors.joining(","));
    }
}
