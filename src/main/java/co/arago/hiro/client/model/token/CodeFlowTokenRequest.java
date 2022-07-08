package co.arago.hiro.client.model.token;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON for a token request.
 *
 *
 * <pre>
 * {
 *     "code": "...",
 *     "code_verifier": "...",
 *     "redirect_uri": "...",
 *     "client_id": "...",
 *     "client_secret": "...",
 *     "organization": "...",
 *     "organization_id: "..."
 * }
 * </pre>
 */
public class CodeFlowTokenRequest extends AbstractTokenRequest {

    // Match the JSON structure for the request.

    private static final long serialVersionUID = -3755194570324660144L;

    @JsonProperty("code")
    public String code;

    @JsonProperty("code_verifier")
    public String codeVerifier;

    @JsonProperty("redirect_uri")
    public String redirectURI;

    public CodeFlowTokenRequest(
            String code,
            String codeVerifier,
            String redirectURI,
            String clientId,
            String clientSecret,
            String organization,
            String organizationId) {
        super("authorization_code", clientId, clientSecret, organization, organizationId);
        this.code = code;
        this.codeVerifier = codeVerifier;
        this.redirectURI = redirectURI;
    }

}
