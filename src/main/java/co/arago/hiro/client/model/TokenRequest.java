package co.arago.hiro.client.model;

import co.arago.hiro.client.util.JsonTools;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

public class TokenRequest {

    // Match the JSON structure for the request.

    @JsonProperty("username")
    public String username;

    @JsonProperty("password")
    public String password;

    @JsonProperty("client_id")
    public String clientId;

    @JsonProperty("client_secret")
    public String clientSecret;

    public TokenRequest() {
    }

    public TokenRequest(String username, String password, String clientId, String clientSecret) {
        this.username = username;
        this.password = password;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public String toJsonString() {
        try {
            return JsonTools.DEFAULT.toString(this);
        } catch (JsonProcessingException e) {
            return ""; // Should never happen
        }
    }
}
