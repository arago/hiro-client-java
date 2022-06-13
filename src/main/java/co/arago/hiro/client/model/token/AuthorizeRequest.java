package co.arago.hiro.client.model.token;

import co.arago.hiro.client.model.EncodedUriMessage;
import co.arago.util.json.JsonUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Map;

public class AuthorizeRequest implements EncodedUriMessage {

    @JsonProperty("response_type")
    public String responseType = "code";

    @JsonProperty("client_id")
    public String clientId;

    @JsonProperty("redirect_uri")
    public String redirectUri;

    @JsonProperty("code_challenge")
    public String codeChallenge;

    @JsonProperty("code_challenge_method")
    public String codeChallengeMethod = "S256";

    public String state;

    public String scope;

    public AuthorizeRequest(
            String clientId,
            String redirectUri,
            String codeChallenge,
            String state,
            String scope) {
        this.clientId = clientId;
        this.redirectUri = redirectUri;
        this.codeChallenge = codeChallenge;
        this.state = state;
        this.scope = scope;
    }

    /**
     * Return a map of all values. This skips fields that are set to null.
     *
     * @return The map of all fields of this object unless being null.
     */
    @Override
    public Map<String, Object> toMap() {
        return JsonUtil.SKIP_NULL.transformObject(this, new TypeReference<>() {
        });
    }
}
