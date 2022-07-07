package co.arago.hiro.client.mock;

import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.mock.handler.BadRequestHandler;
import co.arago.hiro.client.mock.handler.DelayHandler;
import co.arago.hiro.client.mock.handler.TokenHandler;
import co.arago.hiro.client.mock.handler.VersionHttpHandler;
import co.arago.hiro.client.model.VersionResponse;
import co.arago.util.json.JsonUtil;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class MockGraphitServer implements AutoCloseable {

    private final VersionResponse versionResponse;

    private final HttpServer httpServer;

    public MockGraphitServer() throws IOException {
        try {
            versionResponse = JsonUtil.DEFAULT.toObject(
                    MockGraphitServer.class.getClassLoader().getResourceAsStream("responses/version.json"),
                    VersionResponse.class);

            this.httpServer = HttpServer.create(new InetSocketAddress(8000), 0);
            this.httpServer.setExecutor(Executors.newCachedThreadPool());

            addContext("/api/version", new VersionHttpHandler(versionResponse));
            addContext(versionResponse.getVersionEntryOf("auth").endpoint + "token", new TokenHandler());
            addContext(versionResponse.getVersionEntryOf("graph").endpoint + "", new BadRequestHandler());
            addContext("/timeout", new DelayHandler(2000));
        } catch (Exception e) {
            throw new IOException("Unexpected HiroException", e);
        }
    }

    public void addContext(String path, HttpHandler handler) {
        this.httpServer.createContext(path, handler);
    }

    public void addContext(String apiName, String endpoint, HttpHandler handler) throws HiroException {
        addContext(versionResponse.getVersionEntryOf(apiName).endpoint + endpoint, handler);
    }

    public void start() {
        this.httpServer.start();
    }

    @Override
    public void close() {
        httpServer.stop(0);
    }
}
