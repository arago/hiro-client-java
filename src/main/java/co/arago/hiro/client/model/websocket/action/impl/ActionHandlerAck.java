package co.arago.hiro.client.model.websocket.action.impl;

import co.arago.hiro.client.model.websocket.action.AbstractActionHandlerIdMessage;
import co.arago.hiro.client.model.websocket.action.ActionHandlerCodeMessage;
import co.arago.hiro.client.model.websocket.action.ActionMessageType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * <pre>
 *     {
 *         "type": "acknowledged",
 *         "id": "id1",
 *         "code": 200,
 *         "message: "..."
 *     }
 * </pre>
 */
public class ActionHandlerAck extends AbstractActionHandlerIdMessage implements ActionHandlerCodeMessage {

    private static final long serialVersionUID = 2595732787786087081L;

    public Integer code;
    public String message;

    @JsonCreator
    public ActionHandlerAck(
            @JsonProperty(value = "id", required = true) String id
    ) {
        super(id);
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
        return ActionMessageType.ACKNOWLEDGED;
    }
}
