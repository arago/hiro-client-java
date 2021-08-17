package co.arago.hiro.client.connection;

import co.arago.hiro.client.connection.token.PasswordAuthTokenAPIHandler;
import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.rest.AuthAPI;
import co.arago.hiro.client.util.JsonTools;
import co.arago.hiro.client.util.httpclient.HttpResponseParser;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Base64;

class AuthAPIHandlerTest {

    public static PasswordAuthTokenAPIHandler handler;
    public static AuthAPI authAPI;
    final static Logger log = LoggerFactory.getLogger(AuthAPIHandlerTest.class);

    @BeforeAll
    static void init() throws IOException {
        try {
            Config config = JsonTools.DEFAULT.toObject(Paths.get("src", "test", "resources", "config.json").toFile(), Config.class);

            handler = PasswordAuthTokenAPIHandler.newBuilder()
                    .setApiUrl(config.api_url)
                    .setCredentials(config.username, config.password, config.client_id, config.client_secret)
                    .setAcceptAllCerts(config.accept_all_certs)
//                .setForceLogging(true)
                    .build();

            authAPI = AuthAPI.newBuilder(handler)
                    .build();
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
    void checkMeAccount() throws HiroException, IOException, InterruptedException {
        if (authAPI == null)
            return;

        log.info(
                JsonTools.DEFAULT.toString(
                        authAPI.getMeAccount()
                                .setProfile(true)
                                .execute()
                )
        );
    }

    @Test
    void checkMeAvatar() throws HiroException, IOException, InterruptedException {
        if (authAPI == null)
            return;

        HttpResponseParser responseParser = authAPI
                .getMeAvatar()
                .execute();
        log.info(responseParser.getMediaType());
        log.info(String.valueOf(responseParser.getContentLength()));

        byte[] imageBytes;

        try (InputStream inputStream = responseParser.getInputStream()) {
            imageBytes = IOUtils.toByteArray(inputStream);
            String base64 = Base64.getEncoder().encodeToString(imageBytes);
            log.info(base64.length() > 1000 ? base64.substring(0, 1000) + "..." : base64);
        }

        String imageSize = authAPI.putMeAvatar(new ByteArrayInputStream(imageBytes))
                .setContentType(responseParser.getContentType())
                .execute();

        log.info(imageSize);
    }

    @Test
    void checkMeProfile() throws HiroException, IOException, InterruptedException {
        if (authAPI == null)
            return;

        log.info(
                JsonTools.DEFAULT.toString(
                        authAPI.getMeProfile()
                                .execute()
                )
        );
    }

    @Test
    void checkMeRoles() throws HiroException, IOException, InterruptedException {
        if (authAPI == null)
            return;

        log.info(
                JsonTools.DEFAULT.toString(
                        authAPI.getMeRoles()
                                .execute().getMap().get("items")
                )
        );
    }

    @Test
    void checkMeTeams() throws HiroException, IOException, InterruptedException {
        if (authAPI == null)
            return;

        log.info(
                JsonTools.DEFAULT.toString(
                        authAPI.getMeTeams()
                                .setIncludeVirtual(true)
                                .execute().getItems()
                )
        );
    }
}

