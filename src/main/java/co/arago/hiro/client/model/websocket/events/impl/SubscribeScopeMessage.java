package co.arago.hiro.client.model.websocket.events.impl;

import co.arago.hiro.client.model.JsonMessage;

/**
 * <pre>
 *     {
 *         "type": "subscribe",
 *         "id": {@link #id}
 *     }
 * </pre>
 */
public class SubscribeScopeMessage implements JsonMessage {

    private static final long serialVersionUID = 4519228709071726324L;

    public final String type = "subscribe";
    public final String id;

    public SubscribeScopeMessage(String id) {
        this.id = id;
    }
}
