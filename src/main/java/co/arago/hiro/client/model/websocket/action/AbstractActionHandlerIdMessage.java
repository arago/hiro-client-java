package co.arago.hiro.client.model.websocket.action;

import co.arago.hiro.client.model.HiroJsonMap;

public abstract class AbstractActionHandlerIdMessage extends HiroJsonMap implements ActionHandlerIdMessage {

    private final String id;

    public AbstractActionHandlerIdMessage(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

}
