package co.arago.hiro.client.rest;

import co.arago.hiro.client.Config;
import co.arago.hiro.client.connection.token.PasswordAuthTokenAPIHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.model.vertex.HiroVertexListMessage;
import co.arago.hiro.client.model.vertex.HiroVertexMessage;
import co.arago.util.json.JsonUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

class GraphAPITest {

    public static PasswordAuthTokenAPIHandler handler;
    public static GraphAPI graphAPI;
    final static Logger log = LoggerFactory.getLogger(AuthAPIHandlerTest.class);

    @BeforeAll
    static void init() throws IOException {
        try {
            Config config = JsonUtil.DEFAULT.toObject(Paths.get("src", "test", "resources", "config.json").toFile(), Config.class);

            handler = PasswordAuthTokenAPIHandler.newBuilder()
                    .setApiUrl(config.api_url)
                    .setCredentials(config.username, config.password, config.client_id, config.client_secret)
                    .setAcceptAllCerts(config.accept_all_certs)
                    .setForceLogging(config.force_logging)
                    .build();

            graphAPI = GraphAPI.newBuilder(handler).build();
        } catch (FileNotFoundException e) {
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
        // Cleanup remainders from previous runs.
        HiroVertexListMessage queryA = graphAPI.queryByXid("test:machine:Server").execute();
        HiroVertexListMessage queryB = graphAPI.queryByXid("test:software:Webserver").execute();

        if (!queryA.isEmpty()) {
            graphAPI.deleteVertex(queryA.getFirst().getAttributeAsString("ogit/_id"));
        }

        if (!queryB.isEmpty()) {
            graphAPI.deleteVertex(queryB.getFirst().getAttributeAsString("ogit/_id"));
        }

        Map<String, Object> newVertexA = Map.of(
                "ogit/_xid", "test:machine:Server",
                "ogit/_type", "ogit/MARS/Machine",
                "ogit/MARS/Machine/class", "Linux",
                "ogit/name", "Machine Alpha"
        );

        Map<String, Object> newVertexB = Map.of(
                "ogit/_xid", "test:software:Webserver",
                "ogit/_type", "ogit/MARS/Software",
                "ogit/MARS/Software/class", "Webserver",
                "ogit/MARS/Software/subClass", "Apache",
                "ogit/name", "Standalone Apache Webserver"
        );

        HiroVertexMessage resultA = graphAPI.createVertex(newVertexA).execute();
        HiroVertexMessage resultB = graphAPI.createVertex(newVertexB).execute();

        log.info(resultA.toJsonString());
        log.info(resultB.toJsonString());

        String idA = resultA.getAttributeAsString("ogit/_id");
        String idB = resultB.getAttributeAsString("ogit/_id");

        log.info(graphAPI.connectVertices(idB, "ogit/dependsOn", idA).execute().toJsonString());

        graphAPI.deleteVertex(idA).execute();
        graphAPI.deleteVertex(idB).execute();
    }

    @Test
    void graphAPIBulkTest() {
    }
}