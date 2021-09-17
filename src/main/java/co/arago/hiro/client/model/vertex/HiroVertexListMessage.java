package co.arago.hiro.client.model.vertex;

import co.arago.hiro.client.model.HiroItemListMessage;

/**
 * Special case of the {@link HiroItemListMessage} where the data is a {@link HiroVertexMessage}.
 *
 *
 * <pre>
 * {
 *     "items": [
 *         [HiroVertexListResponse],
 *         ...
 *     ]
 * }
 * </pre>
 */
public class HiroVertexListMessage extends HiroItemListMessage<HiroVertexMessage> {
    private static final long serialVersionUID = 8265977186057475224L;
}
