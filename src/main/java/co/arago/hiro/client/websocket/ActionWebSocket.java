package co.arago.hiro.client.websocket;

import co.arago.hiro.client.connection.token.AbstractTokenAPIHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.exceptions.WebSocketException;
import co.arago.hiro.client.model.HiroMessage;
import co.arago.hiro.client.model.websocket.action.ActionMessageType;
import co.arago.hiro.client.model.websocket.action.impl.*;
import co.arago.hiro.client.util.RequiredFieldChecker;
import co.arago.hiro.client.websocket.listener.ActionWebSocketListener;
import co.arago.util.collections.expiringstore.ExpiringRetryStore;
import co.arago.util.collections.expiringstore.ExpiringStore;
import co.arago.util.collections.expiringstore.exceptions.StoreItemExistsException;
import co.arago.util.collections.expiringstore.exceptions.StoreItemExpiredException;
import co.arago.util.json.JsonTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.WebSocket;
import java.util.HashMap;
import java.util.Map;

/**
 * The handler for Action WebSocket.
 *
 * @see <a href="https://core.arago.co/help/specs/?url=definitions/action-ws.yaml">API Documentation</a>
 */
public class ActionWebSocket extends AuthenticatedWebSocketHandler {
    final static Logger log = LoggerFactory.getLogger(ActionWebSocket.class);

    // ###############################################################################################
    // ## Conf and Builder ##
    // ###############################################################################################

    public static abstract class Conf<T extends Conf<T>> extends AuthenticatedWebSocketHandler.Conf<T> {

        private ActionWebSocketListener actionWebSocketListener;
        private int resultRetries = 4;

        /**
         * Set the retries for sending a actionResponse message.
         *
         * @param resultRetries Number of retries. Default is 4.
         * @return {@link #self()}
         */
        public T setResultRetries(int resultRetries) {
            this.resultRetries = resultRetries;
            return self();
        }

        public int getResultRetries() {
            return resultRetries;
        }

        public ActionWebSocketListener getActionWebSocketListener() {
            return actionWebSocketListener;
        }

        /**
         * Set actionWebSocketListener. This will receive incoming data.
         *
         * @param actionWebSocketListener Reference to the listener to use.
         * @return {@link #self()}
         */
        public T setActionWebSocketListener(ActionWebSocketListener actionWebSocketListener) {
            this.actionWebSocketListener = actionWebSocketListener;
            return self();
        }
    }

    public static final class Builder extends Conf<Builder> {

        private Builder(AbstractTokenAPIHandler tokenAPIHandler, ActionWebSocketListener webSocketListener) {
            setTokenApiHandler(tokenAPIHandler);
            setActionWebSocketListener(webSocketListener);
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public ActionWebSocket build() {
            return new ActionWebSocket(this);
        }
    }

    // ###############################################################################################
    // ## InternalEventListener ##
    // ###############################################################################################

    /**
     * Listener class for Action WebSockets.
     */
    protected class InternalActionListener implements SpecificWebSocketListener {

        protected final ActionWebSocketListener actionWebSocketListener;

        /**
         * Constructor
         *
         * @param listener The listener which received messages.
         */
        public InternalActionListener(ActionWebSocketListener listener) {
            this.actionWebSocketListener = listener;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            actionWebSocketListener.onOpen();
        }

        /**
         * Handle incoming action messages.
         *
         * @param webSocket Reference to the websocket.
         * @param message   The incoming {@link HiroMessage}.
         * @throws HiroException        On protocol errors
         * @throws IOException          On IO or JSON parsing errors.
         * @throws InterruptedException When a sleep gets interrupted.
         */
        @Override
        public void onMessage(WebSocket webSocket, HiroMessage message) throws HiroException, IOException, InterruptedException {
            try {
                ActionMessageType actionMessageType = ActionMessageType.fromString((String) message.getMap().get("type"));

                switch (actionMessageType) {
                    case SUBMIT_ACTION: {

                        ActionHandlerSubmit actionHandlerSubmit = JsonTools.DEFAULT.toObject(message, ActionHandlerSubmit.class);
                        log.info("Handling \"{}\" (id: {})", actionHandlerSubmit.getType(), actionHandlerSubmit.getId());

                        send(new ActionHandlerAck(actionHandlerSubmit.getId()).toJsonString());

                        try {
                            submitActionStore.add(
                                    actionHandlerSubmit.expiresAt(),
                                    actionHandlerSubmit.getId(),
                                    actionHandlerSubmit
                            );
                        } catch (StoreItemExpiredException | StoreItemExistsException e) {
                            log.info(e.getMessage());
                            return;
                        }

                        ActionHandlerResult actionHandlerResult = actionResultStore.get(actionHandlerSubmit.getId());
                        if (actionHandlerResult != null) {
                            log.info("Handling \"{}\" (id: {}): Already processed.", actionHandlerSubmit.getType(), actionHandlerSubmit.getId());
                            send(actionHandlerResult.toJsonString());
                            return;
                        }

                        try {
                            actionWebSocketListener.onActionSubmit(actionHandlerSubmit);
                        } catch (Exception e) {
                            log.error("Handling action threw exception.", e);
                            sendActionResult(actionHandlerSubmit.getId(), null, 500, e.getMessage());
                        }

                        break;
                    }
                    case SEND_ACTION_RESULT: {

                        ActionHandlerResult actionHandlerResult = JsonTools.DEFAULT.toObject(message, ActionHandlerResult.class);
                        log.info("Handling \"{}\" (id: {})", actionHandlerResult.getType(), actionHandlerResult.getId());

                        send(new ActionHandlerNack(actionHandlerResult.getId(), 400, "sendActionResult rejected").toJsonString());

                        break;
                    }
                    case ACKNOWLEDGED: {

                        ActionHandlerAck actionHandlerAck = JsonTools.DEFAULT.toObject(message, ActionHandlerAck.class);
                        log.info("Handling \"{}\" (id: {})", actionHandlerAck.getType(), actionHandlerAck.getId());

                        actionResultStore.remove(actionHandlerAck.getId());

                        break;
                    }
                    case NEGATIVE_ACKNOWLEDGED: {

                        ActionHandlerNack actionHandlerNack = JsonTools.DEFAULT.toObject(message, ActionHandlerNack.class);
                        log.info("Handling \"{}\" (id: {})", actionHandlerNack.getType(), actionHandlerNack.getId());

                        ActionHandlerResult actionHandlerResult = actionResultStore.get(actionHandlerNack.getId());
                        if (actionHandlerResult != null) {
                            Thread.sleep(1);
                            send(actionHandlerResult.toJsonString());
                        }

                        break;
                    }
                    case ERROR: {

                        ActionHandlerError actionHandlerError = JsonTools.DEFAULT.toObject(message, ActionHandlerError.class);
                        log.info("Received error message (code {}): {}", actionHandlerError.getCode(), actionHandlerError.getMessage());

                        break;
                    }
                    case CONFIG_CHANGED: {

                        actionWebSocketListener.onConfigChanged();

                        break;
                    }
                }

            } catch (IllegalStateException | IllegalArgumentException e) {
                throw new WebSocketException("Cannot parse incoming message.", e);
            }

        }

        @Override
        public void onClose(WebSocket webSocket, int statusCode, String reason) {
            actionWebSocketListener.onClose(statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable t) {
            actionWebSocketListener.onError(t);
        }
    }

    // #############################################################################################
    // ## Main part ##
    // ###############################################################################################

    /**
     * Stores submitAction messages
     */
    private final ExpiringStore<ActionHandlerSubmit> submitActionStore;

    /**
     * Stores actionResult messages
     */
    private final ExpiringRetryStore<ActionHandlerResult> actionResultStore;

    protected int resultRetries;

    /**
     * Constructor
     *
     * @param builder The builder for this class.
     */
    protected ActionWebSocket(Conf<?> builder) {
        super(builder);

        RequiredFieldChecker.notNull(builder.getActionWebSocketListener(), "actionWebSocketListener");

        this.internalListener = new InternalListener(
                name + "-listener",
                new InternalActionListener(builder.getActionWebSocketListener())
        );

        this.resultRetries = builder.resultRetries;

        this.submitActionStore = new ExpiringStore<>();
        this.actionResultStore = new ExpiringRetryStore<>(builder.getResultRetries());
    }

    /**
     * Get a {@link ActionWebSocket.Builder} for {@link ActionWebSocket}.
     *
     * @param tokenAPIHandler         The API handler for this websocket.
     * @param actionWebSocketListener The listener for this websocket.
     * @return The {@link ActionWebSocket.Builder} for {@link ActionWebSocket}.
     */
    public static ActionWebSocket.Builder newBuilder(
            AbstractTokenAPIHandler tokenAPIHandler,
            ActionWebSocketListener actionWebSocketListener
    ) {
        return new ActionWebSocket.Builder(tokenAPIHandler, actionWebSocketListener);
    }

    /**
     * @param id      Id of the action message conversation.
     * @param result  Result string with detailed payload data.
     * @param code    Result code.
     * @param message Result message.
     * @throws HiroException        On protocol errors
     * @throws IOException          On IO or JSON parsing errors.
     * @throws InterruptedException When a sleep gets interrupted.
     */
    protected void sendActionResult(String id, String result, Integer code, String message) throws HiroException, IOException, InterruptedException {
        ActionHandlerResult actionHandlerResult;
        ActionHandlerSubmit actionHandlerSubmit = submitActionStore.get(id);
        try {
            try {
                if (actionHandlerSubmit == null) {
                    log.info("Handling \"{}\" (id: {}): Submit not stored - maybe it has expired?",
                            ActionMessageType.SEND_ACTION_RESULT,
                            id);
                    return;
                }

                Map<String, Object> resultParams = new HashMap<>();
                if (result == null) {
                    resultParams.put("message", (message != null ? message : "Action successful (no data)"));
                    resultParams.put("code", (code != null ? code : 204));
                } else {
                    resultParams.put("message", (message != null ? message : "Action successful"));
                    resultParams.put("code", (code != null ? code : 200));
                    resultParams.put("data", result);
                }

                actionHandlerResult = new ActionHandlerResult(id, JsonTools.DEFAULT.toString(resultParams));

                actionResultStore.add(actionHandlerSubmit.expiresAt(), actionHandlerResult.getId(), actionHandlerResult);
            } catch (StoreItemExpiredException e) {
                log.info(e.getMessage());
                actionHandlerResult = null;
            } catch (StoreItemExistsException e) {
                log.info(e.getMessage());
                actionHandlerResult = actionResultStore.get(id);
            }

            if (actionHandlerResult != null) {
                log.info("Sending \"{}\" (id: {})", actionHandlerResult.getType(), actionHandlerResult.getId());
                send(actionHandlerResult.toJsonString());
            }
        } finally {
            submitActionStore.remove(id);
        }
    }

    /**
     * Clears the stores. This object cannot be used again thereafter.
     */
    @Override
    public synchronized void close() {
        super.close();
        submitActionStore.close();
        actionResultStore.close();
    }
}
