package co.arago.hiro.client.websocket;

import co.arago.hiro.client.exceptions.WebSocketException;
import co.arago.hiro.client.model.HiroMessage;

/**
 * Interface for external WebSocket listeners
 */
public interface Listener {

    /**
     * Called right after the websocket has been opened.
     */
    void onOpen() throws WebSocketException;

    /**
     * Called with an incoming message.
     */
    void onMessage(HiroMessage message) throws WebSocketException;

    /**
     * Called right before the websocket is about to close.
     */
    void onClose() throws WebSocketException;

    /**
     * Called when an Exception is detected.
     *
     * @param t The Throwable thrown.
     */
    void onError(Throwable t);

}
