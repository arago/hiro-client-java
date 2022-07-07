package co.arago.hiro.client.util.httpclient;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataTest {

    final static Logger log = LoggerFactory.getLogger(DataTest.class);

    @Test
    public void testMultiValues() {
        URIEncodedData uriEncodedData = new URIEncodedData();

        assertEquals(uriEncodedData.toString(), "");

        final String compareWithBlank = "blank=&hello=world&key=value&nice=weather";
        final String compareWithoutBlank = "hello=world&key=value&nice=weather";

        uriEncodedData.set(compareWithBlank);

        assertEquals(uriEncodedData.toString(), compareWithBlank);
        assertEquals(uriEncodedData.toStringRemoveBlanks(), compareWithoutBlank);

        Map<String, String> initialMap = new TreeMap<>(Map.of(
                "blank", "",
                "hello", "world",
                "key", "value",
                "nice", "weather"));

        uriEncodedData.setAll(initialMap);

        String withBlank = uriEncodedData.toString();
        String withoutBlank = uriEncodedData.toStringRemoveBlanks();

        assertEquals(withBlank, compareWithBlank);
        assertEquals(withoutBlank, compareWithoutBlank);

        uriEncodedData.append("nice", (String) null);

        assertEquals(withoutBlank, uriEncodedData.toStringRemoveBlanks());

        uriEncodedData.set("nice", (String) null);

        assertEquals(uriEncodedData.toStringRemoveBlanks(), "hello=world&key=value");
    }

}
