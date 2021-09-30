package co.arago.hiro.client.model.websocket.action.impl;

import co.arago.hiro.client.model.websocket.action.ActionHandlerMessage;
import co.arago.hiro.client.model.websocket.action.ActionMessageType;

/**
 * <pre>
 *     {
 *         "type": "configChanged"
 *     }
 * </pre>
 */
public class ActionHandlerConfigChanged implements ActionHandlerMessage {

    private static final long serialVersionUID = -5726676266325617923L;

    @Override
    public ActionMessageType getType() {
        return ActionMessageType.CONFIG_CHANGED;
    }
}
