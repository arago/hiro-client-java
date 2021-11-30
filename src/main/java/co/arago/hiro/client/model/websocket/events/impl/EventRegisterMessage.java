package co.arago.hiro.client.model.websocket.events.impl;

import co.arago.hiro.client.model.JsonMessage;
import co.arago.hiro.client.model.websocket.events.EventsFilter;

/**
 * <pre>
 *     {
 *         "type": "register",
 *         "args": {
 *             "filter-id": {@link EventsFilter#id}
 *             "filter-type": {@link EventsFilter#type},
 *             "filter-content": {@link EventsFilter#content}
 *         }
 *     }
 * </pre>
 */
public class EventRegisterMessage implements JsonMessage {

    private static final long serialVersionUID = 7202735409241206750L;

    public final String type = "register";

    public final EventsFilter args;

    public EventRegisterMessage(String id, String content) {
        this(id, content, "jfilter");
    }

    public EventRegisterMessage(String id, String content, String type) {
        this(new EventsFilter(id, content, type));
    }

    public EventRegisterMessage(EventsFilter eventsFilter) {
        args = eventsFilter;
    }
}
