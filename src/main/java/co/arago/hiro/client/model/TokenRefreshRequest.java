package co.arago.hiro.client.model;

import co.arago.hiro.client.util.JsonTools;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

public class TokenRefreshRequest {

    // Match the JSON structure for the request.

    @JsonProperty("client_id")
    public String clientId;

    @JsonProperty("client_secret")
    public String clientSecret;

    @JsonProperty("refresh_token")
    public String refreshToken;

    public TokenRefreshRequest() {
    }

    public TokenRefreshRequest(String clientId, String clientSecret, String refreshToken) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = refreshToken;
    }

    public String toJsonString() {
        try {
            return JsonTools.DEFAULT.toString(this);
        } catch (JsonProcessingException e) {
            return ""; // Should never happen
        }
    }
}
