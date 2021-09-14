package co.arago.hiro.client.websocket.listener;

public interface HiroStandardListener {
    default void onOpen() {

    }

    default void onClose(int statusCode, String reason) {

    }

    default void onError(Throwable t) {

    }

}
