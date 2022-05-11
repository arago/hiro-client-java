package co.arago.hiro.client.util.httpclient;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A map for http query parameters.
 */
public class UriQueryMap extends MultiValueMap {

    /**
     * Constructor for an empty map.
     */
    public UriQueryMap() {
    }

    /**
     * Constructor
     *
     * @param initialValues A map of initial values. The values need to be either String or Collection&lt;String&gt;.
     */
    public UriQueryMap(Map<String, ?> initialValues) {
        super(initialValues);
    }

    /**
     * Create a fully encoded query String (without the '?' at the beginning).
     *
     * @return The fully encoded query String or null if no data was in this map.
     */
    @Override
    public String toString() {
        return map.entrySet().stream()
                .map(entry -> entry.getValue().stream()
                        .map(value -> URLPartEncoder.encodeNoPlus(entry.getKey(), StandardCharsets.UTF_8) +
                                "=" +
                                URLPartEncoder.encodeNoPlus(value, StandardCharsets.UTF_8))
                        .collect(Collectors.joining("&"))
                )
                .collect(Collectors.joining("&"));
    }

}
