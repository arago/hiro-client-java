package co.arago.hiro.client.websocket.listener;

import co.arago.hiro.client.model.websocket.action.impl.ActionHandlerSubmit;

public interface ActionWebSocketListener extends HiroStandardListener {

    /**
     * Called with the parsed {@link ActionHandlerSubmit}.
     *
     * @param submit The incoming message.
     */
    void onActionSubmit(ActionHandlerSubmit submit);

    /**
     * Called when {@link co.arago.hiro.client.model.websocket.action.impl.ActionHandlerConfigChanged} is received.
     */
    void onConfigChanged();

}
