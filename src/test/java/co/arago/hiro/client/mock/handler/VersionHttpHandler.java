package co.arago.hiro.client.mock.handler;

import co.arago.hiro.client.model.VersionResponse;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;

public class VersionHttpHandler extends JsonHandler implements HttpHandler {

    final static Logger log = LoggerFactory.getLogger(VersionHttpHandler.class);

    final VersionResponse versionResponse;

    public VersionHttpHandler(VersionResponse versionResponse) {
        this.versionResponse = versionResponse;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            sendJsonResponse(exchange, HttpURLConnection.HTTP_OK, versionResponse.toJsonString());
        } catch (Throwable t) {
            log.error("VersionHttpHandler", t);
        }
    }
}
