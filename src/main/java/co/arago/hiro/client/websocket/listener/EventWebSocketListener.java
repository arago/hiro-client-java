package co.arago.hiro.client.websocket.listener;

import co.arago.hiro.client.model.websocket.events.impl.EventsMessage;

public interface EventWebSocketListener extends HiroStandardListener {

    /**
     * Called with the parsed {@link EventsMessage}.
     *
     * @param eventsMessage The parsed message.
     * @throws Exception any Exception
     */
    default void onEvent(EventsMessage eventsMessage) throws Exception {
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
     * @throws Exception any Exception
     */
    void onCreate(EventsMessage eventsMessage) throws Exception;

    /**
     * On an update message
     *
     * @param eventsMessage The parsed message.
     * @throws Exception any Exception
     */
    void onUpdate(EventsMessage eventsMessage) throws Exception;

    /**
     * On a delete message
     *
     * @param eventsMessage The parsed message.
     * @throws Exception any Exception
     */
    void onDelete(EventsMessage eventsMessage) throws Exception;
}
