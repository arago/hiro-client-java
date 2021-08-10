package co.arago.hiro.client.model.websocket.events;

import co.arago.hiro.client.model.AbstractJsonMessage;
import com.fasterxml.jackson.annotation.JsonProperty;

public class EventsFilter implements AbstractJsonMessage {
    @JsonProperty("filter-id")
    public final String id;
    @JsonProperty("filter-type")
    public final String type;
    @JsonProperty("filter-content")
    public final String content;

    /**
     * Constructor
     *
     * @param id      ID of the filter
     * @param content JFilter content of the filter.
     */
    public EventsFilter(String id, String content) {
        this(id, content, "jfilter");
    }

    /**
     * Constructor
     *
     * @param id      ID of the filter
     * @param content JFilter content of the filter.
     * @param type    Only "jfilter" recognized at the moment.
     */
    public EventsFilter(String id, String content, String type) {
        this.id = id;
        this.type = type;
        this.content = content;
    }
}
