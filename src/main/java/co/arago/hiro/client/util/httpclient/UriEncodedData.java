package co.arago.hiro.client.util.httpclient;

import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A map for http query parameters.
 */
public class UriEncodedData extends MultiValueMap {

    /**
     * Constructor for an empty map.
     */
    public UriEncodedData() {
    }

    /**
     * Constructor
     *
     * @param initialValues A map of initial values. The values need to be either String or Collection&lt;String&gt;.
     */
    public UriEncodedData(Map<String, ?> initialValues) {
        super(initialValues);
    }

    /**
     * Create a fully encoded query String (without the '?' at the beginning).
     *
     * @return The fully encoded query String or an empty String if no data was in this map.
     */
    @Override
    public String toString() {
        return map.entrySet().stream()
                .map(entry -> entry.getValue().stream()
                        .map(value -> URLPartEncoder.encodeNoPlus(entry.getKey(), StandardCharsets.UTF_8) +
                                "=" +
                                URLPartEncoder.encodeNoPlus(value, StandardCharsets.UTF_8))
                        .collect(Collectors.joining("&")))
                .collect(Collectors.joining("&"));
    }

    /**
     * Create a fully encoded query String (without the '?' at the beginning). Remove blank values.
     *
     * @return The fully encoded query String or an empty String if no data was in this map.
     */
    public String toStringRemoveBlanks() {
        return map.entrySet().stream()
                .map(entry -> entry.getValue().stream()
                        .filter(StringUtils::isNotBlank)
                        .map(value -> URLPartEncoder.encodeNoPlus(entry.getKey(), StandardCharsets.UTF_8) +
                                "=" +
                                URLPartEncoder.encodeNoPlus(value, StandardCharsets.UTF_8))
                        .collect(Collectors.joining("&")))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining("&"));
    }
}
