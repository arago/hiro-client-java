package co.arago.hiro.client.websocket;

import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.util.RequiredFieldChecker;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * The handler for Action WebSocket.
 * @see <a href="https://core.arago.co/help/specs/?url=definitions/action-ws.yaml">API Documentation</a>
 */
public class ActionWebSocket extends AbstractWebSocketHandler {
    final static Logger log = LoggerFactory.getLogger(ActionWebSocket.class);

    public static abstract class Conf<T extends Conf<T>> extends AbstractWebSocketHandler.Conf<T> {

    }

    public static final class Builder extends Conf<Builder> {

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public ActionWebSocket build() {
            RequiredFieldChecker.notNull(getTokenApiHandler(), "tokenApiHandler");
            RequiredFieldChecker.notNull(getWebSocketListener(), "webSocketListener");
            if (StringUtils.isBlank(getApiName()) && (StringUtils.isAnyBlank(getEndpoint(), getProtocol())))
                RequiredFieldChecker.anyError("Either 'apiName' or 'endpoint' and 'protocol' have to be set.");
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
