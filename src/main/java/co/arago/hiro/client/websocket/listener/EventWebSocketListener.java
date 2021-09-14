package co.arago.hiro.client.websocket.listener;

import co.arago.hiro.client.model.websocket.events.impl.EventsMessage;

public interface EventWebSocketListener extends HiroStandardListener {

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
