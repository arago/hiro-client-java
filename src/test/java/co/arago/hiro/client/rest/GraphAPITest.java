package co.arago.hiro.client.rest;

import co.arago.hiro.client.Config;
import co.arago.hiro.client.connection.token.PasswordAuthTokenAPIHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.model.vertex.HiroVertexListMessage;
import co.arago.hiro.client.model.vertex.HiroVertexMessage;
import co.arago.util.json.JsonUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Map;

@Disabled
class GraphAPITest {

    public static PasswordAuthTokenAPIHandler handler;
    public static GraphAPI graphAPI;
    final static Logger log = LoggerFactory.getLogger(AuthAPIHandlerTest.class);

    @BeforeAll
    static void init() throws IOException {
        try {
            Config config = JsonUtil.DEFAULT.toObject(Paths.get("src", "test", "resources", "config.json").toFile(),
                    Config.class);

            handler = PasswordAuthTokenAPIHandler.newBuilder()
                    .setRootApiURI(config.api_url)
                    .setCredentials(config.username, config.password, config.client_id, config.client_secret)
                    .setAcceptAllCerts(config.accept_all_certs)
                    .setForceLogging(config.force_logging)
                    .setShutdownTimeout(0)
                    .build();

            graphAPI = GraphAPI.newBuilder(handler).build();
        } catch (URISyntaxException e) {
            log.warn("Skipping tests: {}.", e.getMessage());
        }
    }

    @AfterAll
    static void shutdown() {
        if (handler != null)
            handler.close();
    }

    @Test
    void batchTest() throws HiroException, IOException, InterruptedException {
        if (graphAPI == null)
            return;

        // Cleanup remainders from previous runs.
        HiroVertexListMessage queryA = graphAPI.queryByXidCommand("test:machine:Server").execute();
        HiroVertexListMessage queryB = graphAPI.queryByXidCommand("test:software:Webserver").execute();

        if (!queryA.isEmpty()) {
            graphAPI.deleteVertexCommand(queryA.getFirst().getAttributeAsString("ogit/_id"));
        }

        if (!queryB.isEmpty()) {
            graphAPI.deleteVertexCommand(queryB.getFirst().getAttributeAsString("ogit/_id"));
        }

        Map<String, Object> newVertexA = Map.of(
                "ogit/_xid", "test:machine:Server",
                "ogit/_type", "ogit/MARS/Machine",
                "ogit/MARS/Machine/class", "Linux",
                "ogit/name", "Machine Alpha");

        Map<String, Object> newVertexB = Map.of(
                "ogit/_xid", "test:software:Webserver",
                "ogit/_type", "ogit/MARS/Software",
                "ogit/MARS/Software/class", "Webserver",
                "ogit/MARS/Software/subClass", "Apache",
                "ogit/name", "Standalone Apache Webserver");

        HiroVertexMessage resultA = graphAPI.createVertexCommand(newVertexA).execute();
        HiroVertexMessage resultB = graphAPI.createVertexCommand(newVertexB).execute();

        log.info(resultA.toJsonString());
        log.info(resultB.toJsonString());

        String idA = resultA.getAttributeAsString("ogit/_id");
        String idB = resultB.getAttributeAsString("ogit/_id");

        log.info(graphAPI.connectVerticesCommand(idB, "ogit/dependsOn", idA).execute().toJsonString());

        graphAPI.deleteVertexCommand(idA).execute();
        graphAPI.deleteVertexCommand(idB).execute();
    }

    @Test
    void graphAPIBulkTest() {
    }
}