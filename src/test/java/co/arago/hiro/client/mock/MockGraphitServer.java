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

/**
 * A simple HttpServer which mocks the Graphit API Backend.
 * Used only for tests.
 */
public class MockGraphitServer implements AutoCloseable {

    private final VersionResponse versionResponse;

    private final HttpServer httpServer;

    /**
     * Create the HttpServer and register all paths that are used across all tests.
     * Loads a static version map from the resources.
     *
     * @throws IOException When the Server cannot be created.
     */
    public MockGraphitServer() throws IOException {
        try {

            versionResponse = JsonUtil.DEFAULT.toObject(
                    getClass().getClassLoader().getResourceAsStream("responses/version.json"),
                    VersionResponse.class);

            this.httpServer = HttpServer.create(new InetSocketAddress(8000), 0);
            this.httpServer.setExecutor(Executors.newCachedThreadPool());

            addContext("/api/version", new VersionHttpHandler(versionResponse));
            addAPIContext("auth", "token", new TokenHandler());
            addAPIContext("auth", "me", new BadRequestHandler());
            addAPIContext("graph", "", new BadRequestHandler());
            addContext("/timeout", new DelayHandler(2000));

            this.httpServer.start();
        } catch (HiroException e) {
            throw new IOException("Unexpected HiroException", e);
        }
    }

    private void addContext(String path, HttpHandler handler) {
        this.httpServer.createContext(path, handler);
    }

    private void addAPIContext(String apiName, String endpoint, HttpHandler handler) throws HiroException {
        addContext(versionResponse.getVersionEntryOf(apiName).endpoint + endpoint, handler);
    }

    /**
     * Stop the httpServer immediately.
     */
    @Override
    public void close() {
        httpServer.stop(0);
    }
}
