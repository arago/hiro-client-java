package co.arago.hiro.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The token response from the auth API
 *
 * <code>
 * <pre>
 * {
 *     "_TOKEN": "...",
 *     "refresh_token": "...",
 *     "expires-at": 1234567890,
 *     "_IDENTITY": "...",
 *     "_IDENTITY_ID": "...",
 *     "_APPLICATION": "...",
 *     "type": "..."
 * }
 * </pre>
 * </code>
 */
public class TokenResponse extends HiroResponse {

    private static final long serialVersionUID = -4007996463168969516L;

    @JsonProperty("_TOKEN")
    public String token;

    @JsonProperty("refresh_token")
    public String refreshToken;

    @JsonProperty("expires-at")
    public Long expiresAt;

    @JsonProperty("_IDENTITY")
    public String identity;

    @JsonProperty("_IDENTITY_ID")
    public String indentityId;

    @JsonProperty("_APPLICATION")
    public String application;

    @JsonProperty("type")
    public String type;

}
