package co.arago.hiro.client.model.websocket.events.impl;

import co.arago.hiro.client.model.JacksonJsonMap;
import co.arago.hiro.client.model.vertex.HiroVertexResponse;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;

/**
 * An incoming events message from the events-ws.<br>
 * <i>Example:</i>
 * <pre>
 * {
 *   "id": "c1234567890abcdefghijklmn_c1234567890abcdefghijklmn",
 *   "timestamp": 1442235678000,
 *   "body": {
 *   },
 *   "type": "CREATE",
 *   "metadata": {
 *     "ogit/_modified-on": 1442235678000,
 *     "ogit/_modified-by-app": "c1234567890abcdefghijklmn_c1234567890abcdefghijklmn",
 *     "ogit/_modified-by": "c1234567890abcdefghijklmn_c1234567890abcdefghijklmn"
 *   }
 * }
 * </pre>
 */
public class EventsMessage extends JacksonJsonMap {
    public final String id;
    public final Long timestamp;
    public final Long nanotime;
    public final HiroVertexResponse body;
    public final String type;
    public final HiroVertexResponse metadata;

    @JsonCreator
    public EventsMessage(
            @JsonProperty(value = "id", required = true) String id,
            @JsonProperty(value = "type", required = true) String type,
            @JsonProperty("timestamp") Long timestamp,
            @JsonProperty("nanotime") Long nanotime,
            @JsonProperty("body") HiroVertexResponse body,
            @JsonProperty("metadata") HiroVertexResponse metadata
    ) {
        this.id = id;
        this.timestamp = timestamp;
        this.nanotime = nanotime;
        this.body = body;
        this.type = StringUtils.upperCase(type);
        this.metadata = metadata;
    }
}
