package co.arago.hiro.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents a HIRO list of items.
 *
 * <pre>
 * {
 *     "items": [
 *        [JacksonJsonMap],
 *        ...
 *     ]
 * }
 * </pre>
 */
public class HiroItemListResponse<T extends JacksonJsonMap> extends HiroMessage {

    private static final long serialVersionUID = -8485209005414408396L;

    @JsonProperty("items")
    protected List<T> items;

    /**
     * Set all items at once
     *
     * @param items List of times
     */
    public void setItems(List<T> items) {
        this.items = items;
    }

    /**
     * Append a single item
     *
     * @param item An item of type T
     */
    public void addItem(T item) {
        this.items.add(item);
    }

    /**
     * @return All items.
     */
    public List<T> getItems() {
        return items;
    }

}
