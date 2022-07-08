package co.arago.hiro.client.mock.handler;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

public abstract class SendJsonResponse {
    public void sendJsonResponse(HttpExchange exchange, int rCode, String responseString) throws IOException {
        byte[] response = responseString.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().put("Content-Type", Collections.singletonList("application/json"));
        exchange.sendResponseHeaders(rCode, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }
}
