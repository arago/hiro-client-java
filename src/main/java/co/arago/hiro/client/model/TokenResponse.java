package co.arago.hiro.client.model;

import co.arago.hiro.client.util.JsonTools;

import java.util.LinkedHashMap;

public class TokenResponse extends LinkedHashMap<String, Object> {

    public String getToken() {
        return (String) get("_TOKEN");
    }

    public String getRefreshToken() {
        return (String) get("refresh_token");
    }

    public Long getExpiresAt() {
        return (Long) get("expires-at");
    }

    public String getIdentity() {
        return (String) get("_IDENTITY");
    }

    public String getIdentityId() {
        return (String) get("_IDENTITY_ID");
    }

    public String getApplication() {
        return (String) get("_APPLICATION");
    }

    public String getType() {
        return (String) get("type");
    }

    public static TokenResponse fromResponse(HiroResponse hiroResponse) {
        return JsonTools.DEFAULT.toObject(hiroResponse, TokenResponse.class);
    }


}
