package co.arago.hiro.client.model.websocket.events.impl;

import co.arago.hiro.client.model.JsonMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * <pre>
 *     {
 *         "type": "clear",
 *         "args": {
 *         }
 *     }
 * </pre>
 */
public class ClearEventsMessage implements JsonMessage {
    private static final long serialVersionUID = 1195559607767891454L;
    public final String type = "clear";
    public final Map<String, Object> args = new HashMap<>();
}
