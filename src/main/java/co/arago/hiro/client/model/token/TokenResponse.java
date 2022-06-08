package co.arago.hiro.client.model.token;

import co.arago.hiro.client.model.HiroMessage;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The token response from the auth API
 *
 *
 * <pre>
 * {
 *     "access_token" or "_TOKEN": "...",
 *     "refresh_token": "...",
 *     "expires-at": 1234567890,
 *     "_IDENTITY": "...",
 *     "_IDENTITY_ID": "...",
 *     "_APPLICATION": "...",
 *     "type": "..."
 * }
 * </pre>
 */
public class TokenResponse extends HiroMessage {

    private static final long serialVersionUID = -4007996463168969516L;

    @JsonProperty("access_token")
    @JsonAlias("_TOKEN")
    public String token;

    @JsonProperty("refresh_token")
    public String refreshToken;

    @JsonProperty("expires-at")
    public Long expiresAt;

    @JsonProperty("_IDENTITY")
    public String identity;

    @JsonProperty("_IDENTITY_ID")
    public String identityId;

    @JsonProperty("_APPLICATION")
    public String application;

    @JsonProperty("type")
    public String type;

}
