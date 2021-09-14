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
    public final String type = "clear";
    public final Map<String, Object> args = new HashMap<>();
}
