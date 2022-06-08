package co.arago.hiro.client.model.token;

import co.arago.hiro.client.model.HiroJsonMap;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Data structure of the encoded JSON payload of a HIRO token
 */
public class DecodedToken extends HiroJsonMap {

    private static final long serialVersionUID = -4395614281008269396L;

    public String sub;

    public String aud;

    public static class Data extends HiroJsonMap {

        private static final long serialVersionUID = -4885257322637217350L;

        public List<String> teams;

        @JsonProperty("default-scope")
        public String defaultScope;

        public String organization;

        public List<String> organizations;

        @JsonProperty("_APPLICATION")
        public String application;

        @JsonProperty("_IDENTITY_ID")
        public String identityId;

        @JsonProperty("expires-at")
        public Long expiresAt;

        public Map<String, Object> parameters;

        @JsonProperty("_IDENTITY")
        public String identity;

        @JsonProperty("created-on")
        public Long createdOn;

        @JsonProperty("default-team")
        public String defaultTeam;
    }

    public Data data;

    public String iss;

    public Long exp;

    public Long iat;
}
