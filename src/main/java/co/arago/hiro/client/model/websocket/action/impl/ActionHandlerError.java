package co.arago.hiro.client.model.websocket.action.impl;

import co.arago.hiro.client.model.HiroJsonMap;
import co.arago.hiro.client.model.websocket.action.ActionHandlerCodeMessage;
import co.arago.hiro.client.model.websocket.action.ActionMessageType;

/**
 * <pre>
 *     {
 *         "type": "error",
 *         "code": 400,
 *         "message: "bad request"
 *     }
 * </pre>
 */
public class ActionHandlerError extends HiroJsonMap implements ActionHandlerCodeMessage {
    public Integer code;
    public String message;

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
        return ActionMessageType.ERROR;
    }
}
