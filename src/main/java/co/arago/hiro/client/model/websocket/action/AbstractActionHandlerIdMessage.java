package co.arago.hiro.client.model.websocket.action;

import co.arago.hiro.client.model.AbstractJsonMap;

public abstract class AbstractActionHandlerIdMessage extends AbstractJsonMap implements ActionHandlerIdMessage {

    private final String id;

    public AbstractActionHandlerIdMessage(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

}
