package co.arago.hiro.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents a HIRO list of items.
 *
 * <code>
 * <pre>
 * {
 *     "items": [
 *        [HiroMessage],
 *        ...
 *     ]
 * }
 * </pre>
 * </code>
 */
public class HiroItemListResponse<T extends HiroMessage> extends HiroMessage {

    private static final long serialVersionUID = -8485209005414408396L;

    @JsonProperty("items")
    protected List<T> items;

    public void setItems(List<T> items) {
        this.items = items;
    }

    public List<T> getItems() {
        return items;
    }

}
