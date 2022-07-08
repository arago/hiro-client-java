package co.arago.hiro.client.util.httpclient;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class URIEncodedDataTest {

    final static Logger log = LoggerFactory.getLogger(URIEncodedDataTest.class);

    final static String compareWithBlank = "blank=&hello=world&key=value&nice=weather";
    final static String compareWithoutBlank = "hello=world&key=value&nice=weather";

    final static Map<String, String> initialMap = new TreeMap<>(Map.of(
            "blank", "",
            "hello", "world",
            "key", "value",
            "nice", "weather"));

    @Test
    public void testReturnsEmptyString() {
        URIEncodedData uriEncodedData = new URIEncodedData();

        assertEquals(uriEncodedData.toString(), "");
    }

    @Test
    public void testCreateViaString() {
        URIEncodedData uriEncodedData = new URIEncodedData();

        uriEncodedData.set(compareWithBlank);

        assertEquals(uriEncodedData.toString(), compareWithBlank);
        assertEquals(uriEncodedData.toStringRemoveBlanks(), compareWithoutBlank);
    }

    @Test
    public void testCreateViaMap() {
        URIEncodedData uriEncodedData = new URIEncodedData();

        uriEncodedData.setAll(initialMap);

        assertEquals(uriEncodedData.toString(), compareWithBlank);
        assertEquals(uriEncodedData.toStringRemoveBlanks(), compareWithoutBlank);
    }

    @Test
    public void ignoreNullAppend() {
        URIEncodedData uriEncodedData = new URIEncodedData();

        uriEncodedData.setAll(initialMap);

        uriEncodedData.append("nice", (String) null);

        assertEquals(compareWithBlank, uriEncodedData.toString());
        assertEquals(compareWithoutBlank, uriEncodedData.toStringRemoveBlanks());
    }

    @Test
    public void setNullRemovesKey() {
        URIEncodedData uriEncodedData = new URIEncodedData();

        uriEncodedData.setAll(initialMap);

        uriEncodedData.set("nice", (String) null);

        assertEquals(uriEncodedData.toString(), "blank=&hello=world&key=value");
        assertEquals(uriEncodedData.toStringRemoveBlanks(), "hello=world&key=value");
    }

}
