package co.arago.hiro.client.model.token;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON for a token request.
 *
 *
 * <pre>
 * {
 *     "grant_type": "...",
 *     "username": "...",
 *     "password": "...",
 *     "client_id": "...",
 *     "client_secret": "...",
 *     "organization": "...",
 *     "organization_id: "..."
 * }
 * </pre>
 */
public class PasswordTokenRequest extends AbstractTokenRequest {

    // Match the JSON structure for the request.

    private static final long serialVersionUID = -7893814135720193310L;

    @JsonProperty("username")
    public String username;

    @JsonProperty("password")
    public String password;

    @JsonCreator
    public PasswordTokenRequest(
            @JsonProperty("username") String username,
            @JsonProperty("password") String password,
            @JsonProperty("clientId") String clientId,
            @JsonProperty("clientSecret") String clientSecret,
            @JsonProperty("organization") String organization,
            @JsonProperty("organizationId") String organizationId) {
        super("password", clientId, clientSecret, organization, organizationId);
        this.username = username;
        this.password = password;
    }

}
