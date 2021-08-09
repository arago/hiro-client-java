package co.arago.hiro.client.websocket;

import co.arago.hiro.client.connection.AbstractWebSocketHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.util.RequiredFieldChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ActionWebSocket extends AbstractWebSocketHandler {
    final Logger log = LoggerFactory.getLogger(ActionWebSocket.class);

    public static abstract class Conf<T extends Conf<T>> extends AbstractWebSocketHandler.Conf<T> {

    }

    public static final class Builder extends Conf<Builder> {

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public ActionWebSocket build() {
            RequiredFieldChecker.notNull(webSocketListener, "webSocketListener");
            return new ActionWebSocket(this);
        }
    }

    protected ActionWebSocket(Conf<?> builder) {
        super(builder);
    }

    @Override
    public void send(String message) throws HiroException, InterruptedException, IOException {
        throw new IllegalAccessError("Cannot send data across the action websocket.");
    }
}
