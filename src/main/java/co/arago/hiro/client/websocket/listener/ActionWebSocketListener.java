package co.arago.hiro.client.websocket.listener;

import co.arago.hiro.client.model.websocket.action.impl.ActionHandlerSubmit;

public interface ActionWebSocketListener extends HiroStandardListener {

    void onActionSubmit(ActionHandlerSubmit submit);

    void onConfigChanged();

}
