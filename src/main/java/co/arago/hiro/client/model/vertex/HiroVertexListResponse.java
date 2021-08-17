package co.arago.hiro.client.model.vertex;

import co.arago.hiro.client.model.HiroItemListResponse;

/**
 * Special case of the {@link HiroItemListResponse} where the data is a {@link HiroVertexResponse}.
 *
 * <code>
 * <pre>
 * {
 *     "items": [
 *         [HiroVertexListResponse],
 *         ...
 *     ]
 * }
 * </pre>
 * </code>
 */
public class HiroVertexListResponse extends HiroItemListResponse<HiroVertexResponse> {
    private static final long serialVersionUID = 8265977186057475224L;
}
