package co.arago.hiro.client.websocket.listener;

import co.arago.hiro.client.model.websocket.action.impl.ActionHandlerSubmit;
import co.arago.hiro.client.websocket.ActionWebSocket;

public interface ActionWebSocketListener extends HiroStandardListener {

    /**
     * Called with the parsed {@link ActionHandlerSubmit}.
     *
     * @param actionWebSocket Reference to the websocket.
     * @param submit          The incoming message.
     * @throws Exception Any exception.
     */
    void onActionSubmit(ActionWebSocket actionWebSocket, ActionHandlerSubmit submit) throws Exception;

    /**
     * Called when {@link co.arago.hiro.client.model.websocket.action.impl.ActionHandlerConfigChanged} is received.
     *
     * @param actionWebSocket Reference to the websocket.
     */
    void onConfigChanged(ActionWebSocket actionWebSocket);

}
