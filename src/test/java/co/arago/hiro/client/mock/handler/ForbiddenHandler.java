package co.arago.hiro.client.mock.handler;

import java.net.HttpURLConnection;

public class ForbiddenHandler extends ErrorHandler {
    public ForbiddenHandler() {
        super(HttpURLConnection.HTTP_FORBIDDEN, "forbidden");
    }

}
