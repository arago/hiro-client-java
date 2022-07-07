package co.arago.hiro.client.mock.handler;

import java.net.HttpURLConnection;

public class BadRequestHandler extends ErrorHandler {
    public BadRequestHandler() {
        super(HttpURLConnection.HTTP_BAD_REQUEST, "bad request");
    }

}
