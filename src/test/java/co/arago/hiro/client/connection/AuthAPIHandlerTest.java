package co.arago.hiro.client.connection;

import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.rest.AuthAPI;
import co.arago.hiro.client.util.JsonTools;
import co.arago.hiro.client.util.httpclient.HttpResponseContainer;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

class AuthAPIHandlerTest {

    public static String API_URL = "https://ec2-3-250-135-44.eu-west-1.compute.amazonaws.com:8443";
    public static String USER = "haas1000-connector-core";
    public static String PASS = "j9dad7gond4ls2taol37ulk56%1aZ";
    public static String CLIENTID = "cju16o7cf0000mz77pbwbhl3q_ckqjkfc0q08r90883i7x521sy";
    public static String CLIENTSECRET = "978fa4385da282ed8190b12e9ac70ed6e65ea750f6b5282c0205a9d049913ae9f2841998c2ef13bb14db2d6cee0fd1ca9834563865b8f45c555e6ad3dd1be36a";
    public static Boolean ACCEPT_ALL_CERTS = true;
    public static PasswordAuthTokenAPIHandler handler;
    public static AuthAPI authAPI;
    final Logger log = LoggerFactory.getLogger(AuthAPIHandlerTest.class);

    @BeforeAll
    static void init() {
        handler = PasswordAuthTokenAPIHandler.newBuilder()
                .setApiUrl(API_URL)
                .setCredentials(USER, PASS, CLIENTID, CLIENTSECRET)
                .setAcceptAllCerts(ACCEPT_ALL_CERTS)
                .setForceLogging(true)
                .build();

        authAPI = AuthAPI.newBuilder(handler)
                .build();
    }

    @Test
    void checkMeAccount() throws HiroException, IOException, InterruptedException {
        System.out.println(
                JsonTools.DEFAULT.toPrettyString(
                        authAPI.newGetMeAccount().setProfile(true).execute()
                )
        );
    }

    @Test
    void checkMeAvatar() throws HiroException, IOException, InterruptedException {
        HttpResponseContainer responseContainer = authAPI.newGetMeAvatar().execute();
        System.out.println(responseContainer.mediaType);
        System.out.println(responseContainer.contentLength);
        try (InputStream inputStream = responseContainer.getInputStream()) {
            System.out.println(IOUtils.toString(inputStream, StandardCharsets.UTF_8));
        }
    }
}

