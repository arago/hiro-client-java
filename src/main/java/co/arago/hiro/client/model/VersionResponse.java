package co.arago.hiro.client.model;

import co.arago.hiro.client.exceptions.HiroException;
import co.arago.util.json.JsonTools;

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

    public static class VersionEntry extends AbstractJsonMap {
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
            versionEntryMap.put(key, JsonTools.DEFAULT.toObject(value, VersionEntry.class));
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
