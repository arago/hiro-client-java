package co.arago.hiro.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON for a token refresh request.
 *
 * <code>
 * <pre>
 * {
 *     "client_id": "...",
 *     "client_secret": "...",
 *     "refresh_token": "..."
 * }
 * </pre>
 * </code>
 */
public class TokenRefreshRequest implements AbstractJsonMessage {

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

}
