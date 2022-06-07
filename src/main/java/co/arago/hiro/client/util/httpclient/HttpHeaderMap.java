package co.arago.hiro.client.util.httpclient;

import java.net.http.HttpRequest;
import java.util.List;
import java.util.Map;

/**
 * A map for HttpHeaders.
 */
public class HttpHeaderMap extends MultiValueMap {

    /**
     * Constructor for an empty map.
     */
    public HttpHeaderMap() {
    }

    /**
     * Constructor
     *
     * @param initialValues A map of initial values. The values need to be either String or Collection&lt;String&gt;.
     */
    public HttpHeaderMap(Map<String, ?> initialValues) {
        super(initialValues);
    }

    /**
     * Add the headers of this map to the HttpRequest.Builder.
     *
     * @param builder The builder which will receive the headers.
     */
    public void addHeaders(HttpRequest.Builder builder) {
        map.forEach((key, valueList) -> valueList.forEach(value -> builder.header(key, value)));
    }
}
