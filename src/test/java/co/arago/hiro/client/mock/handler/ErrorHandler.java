package co.arago.hiro.client.mock.handler;

import co.arago.hiro.client.model.HiroError;
import co.arago.util.json.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class ErrorHandler extends JsonHandler implements HttpHandler {
    final static Logger log = LoggerFactory.getLogger(ErrorHandler.class);

    int rCode;
    String message;

    public ErrorHandler(int rCode, String message) {
        this.rCode = rCode;
        this.message = message;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            HiroError hiroError = JsonUtil.DEFAULT.transformObject(Map.of(
                    "error", Map.of(
                            "message", message,
                            "code", rCode)),
                    HiroError.class);

            sendJsonResponse(exchange, rCode, hiroError.toJsonString());
        } catch (Throwable t) {
            log.error("ErrorHandler {} {}", rCode, message, t);
        }
    }
}
