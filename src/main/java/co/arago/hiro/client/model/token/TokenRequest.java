package co.arago.hiro.client.model.token;

import co.arago.hiro.client.model.JsonMessage;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON for a token request.
 *
 *
 * <pre>
 * {
 *     "username": "...",
 *     "password": "...",
 *     "client_id": "...",
 *     "client_secret": "...",
 * }
 * </pre>
 */
public class TokenRequest implements JsonMessage {

    // Match the JSON structure for the request.

    private static final long serialVersionUID = -7893814135720193310L;

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

}
