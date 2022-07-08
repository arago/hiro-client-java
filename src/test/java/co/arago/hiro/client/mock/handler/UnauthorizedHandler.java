package co.arago.hiro.client.mock.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;

public class UnauthorizedHandler extends ErrorHandler {
    final static Logger log = LoggerFactory.getLogger(UnauthorizedHandler.class);

    public UnauthorizedHandler() {
        super(HttpURLConnection.HTTP_UNAUTHORIZED, "unauthorized");
    }

}
