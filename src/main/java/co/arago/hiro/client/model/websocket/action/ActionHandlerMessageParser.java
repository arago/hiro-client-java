package co.arago.hiro.client.model.websocket.action;

import co.arago.hiro.client.model.HiroMessage;
import co.arago.hiro.client.model.websocket.action.impl.*;
import co.arago.util.json.JsonTools;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Map;

public class ActionHandlerMessageParser {

    private final HiroMessage message;

    public ActionHandlerMessageParser(HiroMessage message) {
        this.message = message;
    }

    public ActionHandlerMessage parse() {
        ActionMessageType type = ActionMessageType.fromString((String) message.getMap().get("type"));
        switch (type) {
            case SUBMIT_ACTION:
                return JsonTools.DEFAULT.toObject(message, ActionHandlerSubmit.class);
            case SEND_ACTION_RESULT:
                return JsonTools.DEFAULT.toObject(message, ActionHandlerResult.class);
            case ACKNOWLEDGED:
                return JsonTools.DEFAULT.toObject(message, ActionHandlerAck.class);
            case NEGATIVE_ACKNOWLEDGED:
                return JsonTools.DEFAULT.toObject(message, ActionHandlerNack.class);
            case ERROR:
                return JsonTools.DEFAULT.toObject(message, ActionHandlerError.class);
            case CONFIG_CHANGED:
                return JsonTools.DEFAULT.toObject(message, ActionHandlerConfigChanged.class);
            default:
                throw new IllegalStateException("Unexpected value: " + type);
        }
    }

}
