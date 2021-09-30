package co.arago.hiro.client.model.websocket.events.impl;

import co.arago.hiro.client.model.JsonMessage;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * <pre>
 *     {
 *         "type": "unregister",
 *         "args": {
 *             "filter-id": {@link FilterId#id}
 *         }
 *     }
 * </pre>
 */
public class EventUnregisterMessage implements JsonMessage {

    private static final long serialVersionUID = -6429322952122966911L;

    public static class FilterId implements Serializable {

        private static final long serialVersionUID = -8248097749104501643L;

        @JsonProperty("filter-id")
        public final String id;

        public FilterId(String id) {
            this.id = id;
        }
    }

    public final String type = "unregister";

    public final FilterId args;

    public EventUnregisterMessage(String id) {
        args = new FilterId(id);
    }

}
