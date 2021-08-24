package co.arago.hiro.client.model.websocket.events;

import co.arago.hiro.client.model.JsonMessage;

/**
 * <pre><code>
 *     {
 *         "type": "subscribe",
 *         "id": {@link #id}
 *     }
 * </code></pre>
 */
public class SubscribeScopeMessage implements JsonMessage {
    public final String type = "subscribe";
    public final String id;

    public SubscribeScopeMessage(String id) {
        this.id = id;
    }
}
