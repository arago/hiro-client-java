package co.arago.hiro.client.model.token;

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

    public PasswordTokenRequest(
            String username,
            String password,
            String clientId,
            String clientSecret,
            String organization,
            String organizationId) {
        super("password", clientId, clientSecret, organization, organizationId);
        this.username = username;
        this.password = password;
    }

}
