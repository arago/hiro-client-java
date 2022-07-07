package co.arago.hiro.client.util;

import co.arago.hiro.client.connection.AbstractAPIHandler;
import co.arago.hiro.client.util.httpclient.URIEncodedData;
import co.arago.hiro.client.util.httpclient.URIPath;
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

    @Test
    public void testURI() {

        URIPath uriPath = new URIPath("a strange path Hübner", "some", "..", ".", "more");

        Map<String, String> checkQueryMap = Map.of(
                "test", "Hello World",
                "complex", "http://www.google.de?attr=16#final",
                "weird param", "Oh+my+stuff?some&more");

        URIEncodedData uriQueryMap = new URIEncodedData(checkQueryMap);

        String fragment = "This is a fragment";

        URI finalURI = AbstractAPIHandler.addQueryFragmentAndNormalize(
                URI.create("http://localhost:8080/" + uriPath.build()),
                uriQueryMap,
                fragment);

        log.info("Test URL encoded: " + finalURI.toString());

        String decodedQuery = finalURI.getQuery();

        assertFalse(finalURI.getRawQuery().contains("+"));
        assertTrue(finalURI.getRawQuery().contains("%20"));
        assertTrue(finalURI.getRawPath().contains("H%C3%BCbner"));

        assertEquals(finalURI.getPath(), "/a strange path Hübner/more");
        assertEquals(finalURI.getFragment(), "This is a fragment");

        for (Map.Entry<String, String> checkQueryEntry : checkQueryMap.entrySet()) {
            assertTrue(decodedQuery.contains(checkQueryEntry.getKey() + "=" + checkQueryEntry.getValue()));
        }
    }
}
