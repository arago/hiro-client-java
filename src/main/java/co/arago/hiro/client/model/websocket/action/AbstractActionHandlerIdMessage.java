package co.arago.hiro.client.model.websocket.action;

import co.arago.hiro.client.model.JacksonJsonMap;

public abstract class AbstractActionHandlerIdMessage extends JacksonJsonMap implements ActionHandlerIdMessage {

    private final String id;

    public AbstractActionHandlerIdMessage(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

}
