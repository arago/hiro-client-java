package co.arago.hiro.client.model.websocket.action.impl;

import co.arago.hiro.client.model.websocket.action.AbstractActionHandlerIdMessage;
import co.arago.hiro.client.model.websocket.action.ActionMessageType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * <pre>
 *     {
 *         "type": "sendActionResult",
 *         "id": "id1",
 *         "result": "..."
 *     }
 * </pre>
 */
public class ActionHandlerResult extends AbstractActionHandlerIdMessage {
    public String result;

    @JsonCreator
    public ActionHandlerResult(
            @JsonProperty(value = "id", required = true) String id
    ) {
        super(id);
    }

    public ActionHandlerResult(String id, String result) {
        super(id);
        this.result = result;
    }

    @Override
    public ActionMessageType getType() {
        return ActionMessageType.SEND_ACTION_RESULT;
    }

    public String getResult() {
        return result;
    }

}
