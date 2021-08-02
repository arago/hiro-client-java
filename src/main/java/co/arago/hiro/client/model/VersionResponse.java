package co.arago.hiro.client.model;

import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.util.JsonTools;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class VersionResponse extends HiroResponse {

    private static final long serialVersionUID = 1728631384531328476L;

    public static class VersionEntry implements Serializable {
        private static final long serialVersionUID = 7356571409803847816L;
        public String endpoint;
        public String version;
        public String docs;
        public String support;
        public String specs;
        public String protocols;
        public String lifecycle;

        /**
         * All unknown keys - if any - of the JSON will be collected here
         */
        protected Map<String, Object> remainingFieldsMap = new LinkedHashMap<>();

        @JsonAnySetter
        public void setField(String key, Object value) {
            remainingFieldsMap.put(key, value);
        }

        @JsonAnyGetter
        public Map<String, Object> getMap() {
            return remainingFieldsMap;
        }
    }

    /**
     * @param apiName Name of the API
     * @return The {@link VersionEntry} of that API.
     * @throws HiroException If no API with this name can be found.
     */
    public VersionEntry getVersionEntryOf(String apiName) throws HiroException {
        Object versionEntry = getMap().get(apiName);
        if (versionEntry == null)
            throw new HiroException("No api '" + apiName + "' found in versions.");
        return JsonTools.DEFAULT.toObject(versionEntry, VersionEntry.class);
    }
}
