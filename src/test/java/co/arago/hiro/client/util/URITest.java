package co.arago.hiro.client.util;

import co.arago.hiro.client.connection.AbstractAPIHandler;
import co.arago.hiro.client.util.httpclient.URIEncodedData;
import co.arago.hiro.client.util.httpclient.URIPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class URITest {
    final static Logger log = LoggerFactory.getLogger(URITest.class);

    static URI finalURI;
    static Map<String, String> checkQueryMap;

    @BeforeAll
    public static void init() {
        URIPath uriPath = new URIPath("a strange path Hübner", "some", "..", ".", "more");

        checkQueryMap = Map.of(
                "test", "Hello World",
                "complex", "http://www.google.de?attr=16#final",
                "weird param", "Oh+my+stuff?some&more");

        URIEncodedData uriQuery = new URIEncodedData(checkQueryMap);

        String fragment = "This is a fragment";

        finalURI = AbstractAPIHandler.addQueryFragmentAndNormalize(
                URI.create("http://localhost:8000/" + uriPath.build()),
                uriQuery,
                fragment);

        log.info("Test URL encoded: " + finalURI);
    }

    @Test
    public void testNoPlusSpace() {
        assertFalse(finalURI.getRawQuery().contains("+"));
    }

    @Test
    public void testEncodedSpace() {
        assertTrue(finalURI.getRawQuery().contains("%20"));
    }

    @Test
    public void testUTF8Encoding() {
        assertTrue(finalURI.getRawPath().contains("H%C3%BCbner"));
    }

    @Test
    public void testEncodedNormalizedPath() {
        assertEquals(finalURI.getPath(), "/a strange path Hübner/more");
    }

    @Test
    public void testEncodedFragment() {
        assertEquals(finalURI.getFragment(), "This is a fragment");
    }

    @Test
    public void testDecodedQueryParameters() {
        String decodedQuery = finalURI.getQuery();
        checkQueryMap.forEach((key, value) -> assertTrue(decodedQuery.contains(key + "=" + value)));
    }

}
