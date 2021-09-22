package co.arago.hiro.client.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.ArrayList;
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
public class HiroItemListMessage<T extends HiroJsonMap> extends HiroMessage {

    private static final long serialVersionUID = -8485209005414408396L;

    @JsonProperty("items")
    protected final List<T> items = new ArrayList<>();

    /**
     * Default constructor for Jackson
     */
    public HiroItemListMessage() {
    }

    /**
     * Constructor with initial data
     *
     * @param items List with initial data
     */
    public HiroItemListMessage(List<T> items) {
        addItems(items);
    }

    /**
     * @return True if {@link #items} is empty.
     */
    @JsonIgnore
    public boolean isEmpty() {
        return items.isEmpty();
    }


    /**
     * @return Number of items in {@link #items}.
     */
    public int size() {
        return items.size();
    }

    /**
     * Set all items at once
     *
     * @param items List of times
     */
    @JsonSetter
    public void setItems(List<T> items) {
        this.items.clear();
        addItems(items);
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
     * Append multiple items
     *
     * @param items A list of items of type T
     */
    public void addItems(List<T> items) {
        this.items.addAll(items);
    }

    /**
     * Return a single item.
     *
     * @param index Index of that item.
     * @return Item or null if index is out of bounds.
     */
    public T getItem(int index) {
        if (index < 0 || index >= items.size())
            return null;

        return items.get(index);
    }

    /**
     * Return first item in list.
     *
     * @return Item or null items is empty.
     */
    @JsonIgnore
    public T getFirst() {
        return getItem(0);
    }

    /**
     * Return last item in list.
     *
     * @return Item or null items is empty.
     */
    @JsonIgnore
    public T getLast() {
        return getItem(items.size() - 1);
    }

    /**
     * @return Reference to inner {@link #items}.
     */
    @JsonGetter
    public List<T> getItems() {
        return items;
    }

}
