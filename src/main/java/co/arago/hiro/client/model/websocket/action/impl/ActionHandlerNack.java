package co.arago.hiro.client.model.websocket.action.impl;

import co.arago.hiro.client.model.websocket.action.AbstractActionHandlerIdMessage;
import co.arago.hiro.client.model.websocket.action.ActionHandlerCodeMessage;
import co.arago.hiro.client.model.websocket.action.ActionMessageType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * <pre>
 *     {
 *         "type": "negativeAcknowledged",
 *         "id": "id1",
 *         "code": 400,
 *         "message: "bad request"
 *     }
 * </pre>
 */
public class ActionHandlerNack extends AbstractActionHandlerIdMessage implements ActionHandlerCodeMessage {

    private static final long serialVersionUID = -3319292361271625459L;

    public Integer code;
    public String message;

    @JsonCreator
    public ActionHandlerNack(
            @JsonProperty(value = "id", required = true) String id) {
        super(id);
    }

    public ActionHandlerNack(String id, Integer code, String message) {
        super(id);
        this.code = code;
        this.message = message;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public ActionMessageType getType() {
        return ActionMessageType.NEGATIVE_ACKNOWLEDGED;
    }
}
