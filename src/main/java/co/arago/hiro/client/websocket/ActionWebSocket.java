package co.arago.hiro.client.websocket;

import co.arago.hiro.client.connection.token.AbstractTokenAPIHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.exceptions.WebSocketException;
import co.arago.hiro.client.model.HiroMessage;
import co.arago.hiro.client.model.websocket.action.ActionMessageType;
import co.arago.hiro.client.model.websocket.action.impl.*;
import co.arago.hiro.client.websocket.listener.ActionWebSocketListener;
import co.arago.util.collections.expiringstore.ExpiringRetryStore;
import co.arago.util.collections.expiringstore.ExpiringStore;
import co.arago.util.collections.expiringstore.exceptions.StoreItemExistsException;
import co.arago.util.collections.expiringstore.exceptions.StoreItemExpiredException;
import co.arago.util.json.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.WebSocket;
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

        public abstract ActionWebSocket build();
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

                        ActionHandlerSubmit actionHandlerSubmit = JsonUtil.DEFAULT.toObject(message, ActionHandlerSubmit.class);
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
                            actionWebSocketListener.onActionSubmit(ActionWebSocket.this, actionHandlerSubmit);
                        } catch (Exception e) {
                            log.error("Handling action threw exception.", e);
                            sendActionResult(actionHandlerSubmit.getId(), new ResultParams().setCode(500).setMessage(e.getMessage()));
                        }

                        break;
                    }
                    case SEND_ACTION_RESULT: {

                        ActionHandlerResult actionHandlerResult = JsonUtil.DEFAULT.toObject(message, ActionHandlerResult.class);
                        log.info("Handling \"{}\" (id: {})", actionHandlerResult.getType(), actionHandlerResult.getId());

                        send(new ActionHandlerNack(actionHandlerResult.getId(), 400, "sendActionResult rejected").toJsonString());

                        break;
                    }
                    case ACKNOWLEDGED: {

                        ActionHandlerAck actionHandlerAck = JsonUtil.DEFAULT.toObject(message, ActionHandlerAck.class);
                        log.info("Handling \"{}\" (id: {})", actionHandlerAck.getType(), actionHandlerAck.getId());

                        actionResultStore.remove(actionHandlerAck.getId());

                        break;
                    }
                    case NEGATIVE_ACKNOWLEDGED: {

                        ActionHandlerNack actionHandlerNack = JsonUtil.DEFAULT.toObject(message, ActionHandlerNack.class);
                        log.info("Handling \"{}\" (id: {})", actionHandlerNack.getType(), actionHandlerNack.getId());

                        ActionHandlerResult actionHandlerResult = actionResultStore.get(actionHandlerNack.getId());
                        if (actionHandlerResult != null) {
                            Thread.sleep(1);
                            send(actionHandlerResult.toJsonString());
                        }

                        break;
                    }
                    case ERROR: {

                        ActionHandlerError actionHandlerError = JsonUtil.DEFAULT.toObject(message, ActionHandlerError.class);
                        log.info("Received error message (code {}): {}", actionHandlerError.getCode(), actionHandlerError.getMessage());

                        break;
                    }
                    case CONFIG_CHANGED: {

                        actionWebSocketListener.onConfigChanged(ActionWebSocket.this);

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
    // ## ActionResult payload builder ##
    // #############################################################################################

    /**
     * This creates a useful payload for the {@link ActionHandlerResult}.
     * <p>
     * If no code is set, 200 or 204 will be assumed depending on whether data is present or not.
     * <p>
     * If no message is set, a standard message will be generated depending on whether data is present or not.
     * <p>
     * Without data:
     * <pre>
     *     {
     *         "code": code,
     *         "message": message
     *     }
     * </pre>
     * <p>
     * or with data:
     * <pre>
     *     {
     *         "code": code,
     *         "message": message,
     *         "data": data
     *     }
     * </pre>
     */
    public static class ResultParams {
        private Integer code;
        private String message;
        private String data;

        /**
         * Sets the status code.
         *
         * @param code The status code.
         * @return this
         */
        public ResultParams setCode(Integer code) {
            this.code = code;
            return this;
        }

        /**
         * Sets the status message
         *
         * @param message The status message
         * @return this
         */
        public ResultParams setMessage(String message) {
            this.message = message;
            return this;
        }

        /**
         * Sets the data as String
         *
         * @param data The data to set
         * @return this
         */
        public ResultParams setData(String data) {
            this.data = data;
            return this;
        }

        /**
         * Sets the data string from a map.
         *
         * @param result The data to set
         * @return this
         * @throws JsonProcessingException If the map cannot be transformed to a String.
         */
        public ResultParams setData(Map<String, Object> result) throws JsonProcessingException {
            this.data = JsonUtil.DEFAULT.toString(result);
            return this;
        }

        /**
         * Get the code.
         *
         * @return The code or 200 when code == null and data != null or 204 when both are null.
         */
        public Integer getCode() {
            return (code != null ? code : (data != null ? 200 : 204));
        }

        /**
         * Get the message.
         *
         * @return The message or a standard message when message == null.
         */
        public String getMessage() {
            return (message != null ? message : "Action successful" + (data == null ? " (no data)" : ""));
        }

        /**
         * Get data
         *
         * @return The data. Can be null.
         */
        public String getData() {
            return data;
        }

        /**
         * Transform message to message string. Skip {@link #data} if it is null.
         *
         * @return The string representation of this message.
         */
        public String toResultPayload() {
            try {
                return JsonUtil.SKIP_NULL.toString(this);
            } catch (JsonProcessingException e) {
                // should not happen.
                return "";
            }
        }
    }

    /**
     * Static creator
     *
     * @return A new {@link ResultParams} structure.
     */
    public static ResultParams newResultParams() {
        return new ResultParams();
    }

    // #############################################################################################
    // ## Main part ##
    // #############################################################################################

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

        this.internalListener = new InternalListener(
                name + "-listener",
                new InternalActionListener(
                        notNull(builder.getActionWebSocketListener(), "actionWebSocketListener")
                )
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
    public static Conf<?> newBuilder(
            AbstractTokenAPIHandler tokenAPIHandler,
            ActionWebSocketListener actionWebSocketListener
    ) {
        return new ActionWebSocket.Builder(tokenAPIHandler, actionWebSocketListener);
    }

    /**
     * Send an {@link ActionHandlerResult} using the provided id and a ResultParams structure.
     *
     * @param id           The id of the action message conversation.
     * @param resultParams Standard result structure for the result.
     * @throws HiroException        On protocol errors
     * @throws IOException          On IO or JSON parsing errors.
     * @throws InterruptedException When a sleep gets interrupted.
     */
    public void sendActionResult(String id, ResultParams resultParams) throws HiroException, IOException, InterruptedException {
        sendActionResult(id, resultParams.toResultPayload());
    }

    /**
     * Send an {@link ActionHandlerResult} using the provided id and result string.
     *
     * @param id     The id of the action message conversation.
     * @param result The result string for the {@link ActionHandlerResult}.
     * @throws HiroException        On protocol errors
     * @throws IOException          On IO or JSON parsing errors.
     * @throws InterruptedException When a sleep gets interrupted.
     */
    public void sendActionResult(String id, String result) throws HiroException, IOException, InterruptedException {
        ActionHandlerSubmit actionHandlerSubmit = submitActionStore.get(id);
        if (actionHandlerSubmit == null) {
            log.info("Handling \"{}\" (id: {}): Submit not stored - maybe it has expired?",
                    ActionMessageType.SEND_ACTION_RESULT,
                    id);
            return;
        }

        ActionHandlerResult actionHandlerResult;
        try {
            try {

                actionHandlerResult = new ActionHandlerResult(id, result);

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
