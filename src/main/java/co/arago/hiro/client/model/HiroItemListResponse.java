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
 *        [HiroResponse],
 *        ...
 *     ]
 * }
 * </pre>
 * </code>
 */
public class HiroItemListResponse<T extends HiroResponse> extends HiroResponse {

    private static final long serialVersionUID = -8485209005414408396L;

    @JsonProperty("items")
    public List<T> items;

}