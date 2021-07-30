package co.arago.hiro.client.model;

import co.arago.hiro.client.util.JsonTools;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class VersionResponse extends LinkedHashMap<String, Map<String, String>> {

    /**
     * Get a value from an API version map.
     *
     * @param apiName The API to use.
     * @param key The key of the value to get.
     * @return The value found
     * @throws RuntimeException When no value has been found for the key under apiName.
     */
    public String getValueOf(String apiName, String key) {
        String result = null;
        Map<String, String> targetMap = get(apiName);
        if (targetMap != null)
            result = targetMap.get(key);

        if (StringUtils.isBlank(result))
            throw new RuntimeException("Cannot determine '" + key + "' for API named '" + apiName + "'");

        return result;
    }

    public static VersionResponse fromInputStream(InputStream inputStream) throws IOException {
        return JsonTools.DEFAULT.toObject(inputStream, VersionResponse.class);
    }
}
