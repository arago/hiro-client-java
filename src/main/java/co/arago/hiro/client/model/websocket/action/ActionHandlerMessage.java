package co.arago.hiro.client.model.websocket.action;

import co.arago.hiro.client.model.JsonMessage;

public interface ActionHandlerMessage extends JsonMessage {

    ActionMessageType getType();

}
