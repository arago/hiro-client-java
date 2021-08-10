package co.arago.hiro.client.model.websocket.events;

import co.arago.hiro.client.model.AbstractJsonMessage;

/**
 * <pre><code>
 *     {
 *         "type": "subscribe",
 *         "id": {@link #id}
 *     }
 * </code></pre>
 */
public class WebSocketSubscribeScopeMessage implements AbstractJsonMessage {
    public final String type = "subscribe";
    public final String id;

    public WebSocketSubscribeScopeMessage(String id) {
        this.id = id;
    }
}
