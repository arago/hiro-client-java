package co.arago.hiro.client.model.websocket.events;

import co.arago.hiro.client.model.JsonMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * <pre><code>
 *     {
 *         "type": "clear",
 *         "args": {
 *         }
 *     }
 * </code></pre>
 */
public class WebSocketClearEventsMessage implements JsonMessage {
    public final String type = "clear";
    public final Map<String, Object> args = new HashMap<>();
}
