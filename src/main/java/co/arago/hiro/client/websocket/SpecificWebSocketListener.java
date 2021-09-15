package co.arago.hiro.client.websocket;

import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.exceptions.WebSocketException;
import co.arago.hiro.client.model.HiroMessage;

import java.io.IOException;
import java.net.http.WebSocket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Interface for internal WebSocket listeners of different WebSocket protocols.
 */
public interface SpecificWebSocketListener {

    /**
     * Called right after the websocket has been opened.
     * Does nothing on default.
     *
     * @param webSocket Reference to the websocket.
     * @throws InterruptedException On interrupt on sending.
     * @throws IOException          On IO errors or when messages cannot be parsed to JSON strings.
     * @throws ExecutionException   When sending fails generally.
     * @throws TimeoutException     When sending of data took longer than a timeout in ms.
     */
    void onOpen(WebSocket webSocket) throws ExecutionException, InterruptedException, TimeoutException, IOException;

    /**
     * Called with an incoming message.
     *
     * @param webSocket Reference to the websocket.
     * @param message   The incoming {@link HiroMessage}.
     * @throws HiroException        Thrown when handling the message results in an error.
     * @throws IOException          On underlying IO or Json parse error.
     * @throws InterruptedException When process gets interrupted.
     */
    void onMessage(WebSocket webSocket, HiroMessage message) throws HiroException, IOException, InterruptedException;

    /**
     * Called right before the websocket is about to close.
     * Does nothing on default.
     *
     * @param webSocket  Reference to the websocket.
     * @param statusCode Status code sent from the remote side.
     * @param reason     Reason text sent from the remote side.
     * @throws WebSocketException If the WebSocket has errors on closing.
     */
    void onClose(WebSocket webSocket, int statusCode, String reason) throws WebSocketException;

    /**
     * Called when an Exception is detected.
     * Does nothing on default.
     *
     * @param webSocket Reference to the websocket.
     * @param t         The Throwable thrown.
     */
    void onError(WebSocket webSocket, Throwable t);

}
