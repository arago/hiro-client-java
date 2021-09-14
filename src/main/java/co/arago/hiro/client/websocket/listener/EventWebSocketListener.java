package co.arago.hiro.client.websocket.listener;

import co.arago.hiro.client.exceptions.WebSocketException;
import co.arago.hiro.client.model.HiroMessage;
import co.arago.hiro.client.model.websocket.events.impl.EventsMessage;
import co.arago.util.json.JsonTools;

public interface EventWebSocketListener extends HiroWebSocketListener {

    /**
     * Called right after the websocket has been opened.
     * Does nothing on default.
     */
    @Override
    default void onOpen() {
    }

    /**
     * Called with an incoming message.
     *
     * @param message incoming events message. Must not be null.
     */
    @Override
    default void onMessage(HiroMessage message) throws WebSocketException {
        onEvent(JsonTools.DEFAULT.toObject(message, EventsMessage.class));
    }

    /**
     * Called right before the websocket is about to close.
     * Does nothing on default.
     */
    @Override
    default void onClose() throws WebSocketException {

    }

    /**
     * Called when an Exception is detected.
     * Does nothing on default.
     *
     * @param t The Throwable thrown.
     */
    @Override
    default void onError(Throwable t) {

    }

    /**
     * Called with the parsed {@link EventsMessage}.
     *
     * @param eventsMessage The parsed message.
     */
    default void onEvent(EventsMessage eventsMessage) {
        switch (eventsMessage.type) {
            case "CREATE":
                onCreate(eventsMessage);
                break;
            case "UPDATE":
                onUpdate(eventsMessage);
                break;
            case "DELETE":
                onDelete(eventsMessage);
                break;
        }
    }

    /**
     * On a create message
     *
     * @param eventsMessage The parsed message.
     */
    void onCreate(EventsMessage eventsMessage);

    /**
     * On an update message
     *
     * @param eventsMessage The parsed message.
     */
    void onUpdate(EventsMessage eventsMessage);

    /**
     * On a delete message
     *
     * @param eventsMessage The parsed message.
     */
    void onDelete(EventsMessage eventsMessage);
}
