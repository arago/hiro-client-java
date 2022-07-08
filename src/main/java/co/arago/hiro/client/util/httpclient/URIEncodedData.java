package co.arago.hiro.client.util.httpclient;

import org.apache.commons.lang3.StringUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A map for http query parameters.
 */
public class URIEncodedData extends MultiValueMap {

    /**
     * Constructor for an empty map.
     */
    public URIEncodedData() {
    }

    /**
     * Constructor
     *
     * @param initialValues A map of initial values. The values need to be either String or Collection&lt;String&gt;.
     */
    public URIEncodedData(Map<String, ?> initialValues) {
        super(initialValues);
    }

    /**
     * Constructor
     *
     * @param encodedData The incoming String will be parsed into the underlying MultiValueMap.
     */
    public URIEncodedData(String encodedData) {
        set(encodedData);
    }

    private static String urlDecode(String encoded) {
        return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
    }

    /**
     * @param encodedData The incoming String will be parsed into the underlying MultiValueMap, overwriting it.
     */
    public void set(String encodedData) {
        map.clear();
        append(encodedData);
    }

    /**
     * @param encodedData The incoming String will be parsed into the underlying MultiValueMap.
     */
    public void append(String encodedData) {
        Arrays.stream(encodedData.split("&")).forEach(entry -> {
            String[] pair = entry.split("=", 2);
            append(urlDecode(pair[0]), urlDecode(pair[1]));
        });
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
                        .map(value -> StringUtils.isBlank(value) ? ""
                                : URLPartEncoder.encodeNoPlus(entry.getKey(), StandardCharsets.UTF_8) + "="
                                        + URLPartEncoder.encodeNoPlus(value, StandardCharsets.UTF_8))
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.joining("&")))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining("&"));
    }

    public Map<String, ?> toMap() {
        return map;
    }

}
