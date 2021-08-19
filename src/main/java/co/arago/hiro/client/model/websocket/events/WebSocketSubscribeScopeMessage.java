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
public class WebSocketSubscribeScopeMessage implements JsonMessage {
    public final String type = "subscribe";
    public final String id;

    public WebSocketSubscribeScopeMessage(String id) {
        this.id = id;
    }
}
