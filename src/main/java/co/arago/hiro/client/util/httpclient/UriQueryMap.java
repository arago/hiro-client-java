package co.arago.hiro.client.util.httpclient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

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
        StringBuilder builder = new StringBuilder();

        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            for (String value : entry.getValue()) {
                if (builder.length() > 0)
                    builder.append("&");

                builder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                        .append("=")
                        .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
            }
        }

        return (builder.length() > 0) ? builder.toString() : null;
    }

}
