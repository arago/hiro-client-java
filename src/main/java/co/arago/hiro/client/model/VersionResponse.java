package co.arago.hiro.client.model;

import co.arago.hiro.client.exceptions.HiroException;
import co.arago.util.json.JsonUtil;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The response to a version GET request against "/api/version".
 *
 *
 * <pre>
 * {
 *     "[name of first api]": {
 *         "endpoint": "...",
 *         "version": "...",
 *         ...
 *     },
 *     "[name of second api]": {
 *         ...
 *     },
 *     ...
 * }
 *
 * </pre>
 */
public class VersionResponse extends HiroMessage {

    private static final long serialVersionUID = 2903834856447071619L;

    public static class VersionEntry extends HiroJsonMap {
        private static final long serialVersionUID = -5055749186188400575L;
        public String endpoint;
        public String version;
        public String docs;
        public String support;
        public String specs;
        public String protocols;
        public String lifecycle;
    }

    protected final Map<String, VersionEntry> versionEntryMap = new LinkedHashMap<>();

    /**
     * @return A merged map of {@link #versionEntryMap} and {@link #fieldsMap}. This is a shallow copy of both.
     */
    @Override
    public Map<String, ?> getMap() {
        Map<String, Object> mergedMap = new HashMap<>(versionEntryMap);
        mergedMap.putAll(fieldsMap);
        return mergedMap;
    }

    /**
     * Jackson Setter that transforms incoming maps into VersionEntries.
     *
     * @param key   Incoming JSON key
     * @param value Incoming JSON value
     */
    @Override
    public void setField(String key, Object value) {
        if (catchError(key, value))
            return;

        if (value instanceof Map) {
            versionEntryMap.put(key, JsonUtil.DEFAULT.transformObject(value, VersionEntry.class));
        } else {
            super.setField(key, value);
        }
    }

    /**
     * @param apiName Name of the API
     * @return The {@link VersionEntry} of that API.
     * @throws HiroException If no API with this name can be found.
     */
    public VersionEntry getVersionEntryOf(String apiName) throws HiroException {
        VersionEntry versionEntry = versionEntryMap.get(apiName);
        if (versionEntry == null)
            throw new HiroException("No API named '" + apiName + "' found in versions.");
        return versionEntry;
    }

}
