package co.arago.hiro.client.model.websocket.events;

import co.arago.hiro.client.model.AbstractJsonMessage;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * <pre><code>
 *     {
 *         "type": "unregister",
 *         "args": {
 *             "filter-id": {@link FilterId#id}
 *         }
 *     }
 * </code></pre>
 */
public class WebSocketEventUnregisterMessage implements AbstractJsonMessage {

    public static class FilterId implements Serializable {
        @JsonProperty("filter-id")
        public final String id;

        public FilterId(String id) {
            this.id = id;
        }
    }

    public final String type = "unregister";

    public final FilterId args;

    public WebSocketEventUnregisterMessage(String id) {
        args = new FilterId(id);
    }

}
