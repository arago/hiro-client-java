package co.arago.hiro.client.mock.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class DelayHandler implements HttpHandler {
    final static Logger log = LoggerFactory.getLogger(DelayHandler.class);
    long delayMs;

    public DelayHandler(long delayMs) {
        this.delayMs = delayMs;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            Thread.sleep(delayMs);
        } catch (Throwable t) {
            log.error("DelayHandler", t);
        }
    }
}
