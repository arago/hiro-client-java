package co.arago.hiro.client.model.websocket.events;

import co.arago.hiro.client.model.JsonMessage;

/**
 * <pre><code>
 *     {
 *         "type": "register",
 *         "args": {
 *             "filter-id": {@link EventsFilter#id}
 *             "filter-type": {@link EventsFilter#type},
 *             "filter-content": {@link EventsFilter#content}
 *         }
 *     }
 * </code></pre>
 */
public class WebSocketEventRegisterMessage implements JsonMessage {


    public final String type = "register";

    public final EventsFilter args;

    public WebSocketEventRegisterMessage(String id, String content) {
        this(id, content, "jfilter");
    }

    public WebSocketEventRegisterMessage(String id, String content, String type) {
        this(new EventsFilter(id, content, type));
    }

    public WebSocketEventRegisterMessage(EventsFilter eventsFilter) {
        args = eventsFilter;
    }
}
