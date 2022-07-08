package co.arago.hiro.client.model.token;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON for a token refresh request.
 *
 *
 * <pre>
 * {
 *     "grant_type": "...",
 *     "client_id": "...",
 *     "client_secret": "...",
 *     "refresh_token": "...",
 *     "organization": "...",
 *     "organization_id: "..."
 * }
 * </pre>
 */
public class TokenRefreshRequest extends AbstractTokenRequest {

    // Match the JSON structure for the request.

    private static final long serialVersionUID = -4492202824089449055L;

    @JsonProperty("refresh_token")
    public String refreshToken;

    @JsonCreator
    public TokenRefreshRequest(
            @JsonProperty("clientId") String clientId,
            @JsonProperty("clientSecret") String clientSecret,
            @JsonProperty("refreshToken") String refreshToken,
            @JsonProperty("organization") String organization,
            @JsonProperty("organizationId") String organizationId) {
        super("refresh_token", clientId, clientSecret, organization, organizationId);
        this.refreshToken = refreshToken;
    }

}
