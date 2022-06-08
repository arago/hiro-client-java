package co.arago.hiro.client.connection;

import co.arago.hiro.client.util.httpclient.URIPath;
import co.arago.hiro.client.util.httpclient.UriQueryMap;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UriTest {
    final static Logger log = LoggerFactory.getLogger(UriTest.class);

    @Test
    public void testUri() {

        URIPath uriPath = new URIPath("a strange path Hübner", "some", "..", ".", "more");

        Map<String, String> checkQueryMap = Map.of(
                "test", "Hello World",
                "complex", "http://www.google.de?attr=16#final",
                "weird param", "Oh+my+stuff?some&more");

        UriQueryMap uriQueryMap = new UriQueryMap(checkQueryMap);

        String fragment = "This is a fragment";

        URI finalUri = AbstractAPIHandler.addQueryFragmentAndNormalize(
                URI.create("http://localhost:8080/" + uriPath.build()),
                uriQueryMap,
                fragment);

        log.info("Test URL encoded: " + finalUri.toString());

        String decodedQuery = finalUri.getQuery();

        assertFalse(finalUri.getRawQuery().contains("+"));
        assertTrue(finalUri.getRawQuery().contains("%20"));
        assertTrue(finalUri.getRawPath().contains("H%C3%BCbner"));

        assertEquals(finalUri.getPath(), "/a strange path Hübner/more");
        assertEquals(finalUri.getFragment(), "This is a fragment");

        for (Map.Entry<String, String> checkQueryEntry : checkQueryMap.entrySet()) {
            assertTrue(decodedQuery.contains(checkQueryEntry.getKey() + "=" + checkQueryEntry.getValue()));
        }
    }
}
