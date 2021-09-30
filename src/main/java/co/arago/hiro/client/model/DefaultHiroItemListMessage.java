package co.arago.hiro.client.model;

/**
 * Represents a HIRO list of items. This is a default implementation for generic JacksonJsonMap lists.
 *
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
public class DefaultHiroItemListMessage extends HiroItemListMessage<HiroJsonMap> {

    private static final long serialVersionUID = -2314085561734584523L;
}
