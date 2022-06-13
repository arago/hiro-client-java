package co.arago.hiro.client.model.token;

import co.arago.hiro.client.model.JsonMessage;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON for a token revoke request.<br>
 *
 * Deprecated:
 * 
 * <pre>
 * {
 *     "client_id": "...",
 *     "client_secret": "...",
 *     "refresh_token": "..."
 * }
 * </pre>
 *
 * Current:
 * 
 * <pre>
 * {
 *     "client_id": "...",
 *     "client_secret": "...",
 *     "token": "...",
 *     "token_hint": "..."
 * }
 * </pre>
 */
public class TokenRevokeRequest implements JsonMessage {

    // Match the JSON structure for the request.

    private static final long serialVersionUID = -4492202824089449055L;

    @JsonProperty("client_id")
    public String clientId;

    @JsonProperty("client_secret")
    public String clientSecret;

    @JsonProperty("refresh_token")
    public String refreshToken;

    @JsonProperty("token")
    public String token;

    @JsonProperty("token_hint")
    public String tokenHint;

    public TokenRevokeRequest() {
    }

    @Deprecated
    public TokenRevokeRequest(String clientId, String clientSecret, String refreshToken) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = refreshToken;
    }

    public TokenRevokeRequest(String clientId, String clientSecret, String token, String tokenHint) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.token = token;
        this.tokenHint = tokenHint;
    }
}
