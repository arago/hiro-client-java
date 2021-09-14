package co.arago.hiro.client.websocket.listener;

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
    void onOpen();

    /**
     * Called with an incoming message.
     *
     * @param message The incoming {@link HiroMessage}.
     * @throws WebSocketException Thrown when handling the message results in an error.
     */
    void onMessage(HiroMessage message) throws WebSocketException;

    /**
     * Called right before the websocket is about to close.
     * Does nothing on default.
     *
     * @throws WebSocketException If the WebSocket has errors on closing.
     */
    void onClose() throws WebSocketException;

    /**
     * Called when an Exception is detected.
     * Does nothing on default.
     *
     * @param t The Throwable thrown.
     */
    void onError(Throwable t);

}
