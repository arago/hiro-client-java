package co.arago.hiro.client.model.websocket.action;

public interface ActionHandlerCodeMessage extends ActionHandlerMessage {

    Integer getCode();

    String getMessage();

}
