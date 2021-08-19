package co.arago.hiro.client.websocket;

import co.arago.hiro.client.exceptions.WebSocketException;
import co.arago.hiro.client.model.HiroMessage;

/**
 * Interface for external WebSocket listeners
 */
public interface HiroWebSocketListener {

    /**
     * Called right after the websocket has been opened.
     * Does nothing on default.
     */
    default void onOpen() throws WebSocketException {

    }

    /**
     * Called with an incoming message.
     */
    void onMessage(HiroMessage message) throws WebSocketException;

    /**
     * Called right before the websocket is about to close.
     * Does nothing on default.
     */
    default void onClose() throws WebSocketException {

    }

    /**
     * Called when an Exception is detected.
     * Does nothing on default.
     *
     * @param t The Throwable thrown.
     */
    default void onError(Throwable t) {

    }

}
