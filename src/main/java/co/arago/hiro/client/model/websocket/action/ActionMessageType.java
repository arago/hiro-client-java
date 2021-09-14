package co.arago.hiro.client.model.websocket.action;

import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.StringUtils;

/**
 * Possible message types for action handler messages
 */
public enum ActionMessageType {
    ACKNOWLEDGED("acknowledged"),
    NEGATIVE_ACKNOWLEDGED("negativeAcknowledged"),
    SEND_ACTION_RESULT("sendActionResult"),
    SUBMIT_ACTION("submitAction"),
    ERROR("error"),
    CONFIG_CHANGED("configChanged");

    private final String text;

    ActionMessageType(String text) {
        this.text = text;
    }

    @Override
    @JsonValue
    public String toString() {
        return text;
    }

    /**
     * @param text Text of an {@link ActionMessageType}
     * @return The matching {@link ActionMessageType} instance
     * @throws NullPointerException     When text == null
     * @throws IllegalArgumentException When text does not represent any of the message types
     */
    public static ActionMessageType fromString(String text) {
        if (text == null)
            throw new NullPointerException();

        for (ActionMessageType entry : ActionMessageType.values()) {
            if (StringUtils.equalsIgnoreCase(entry.text, text))
                return entry;
        }

        throw new IllegalArgumentException("No such action message type '" + text + "'.");
    }
}
