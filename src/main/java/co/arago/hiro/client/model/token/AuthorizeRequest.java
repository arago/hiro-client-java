package co.arago.hiro.client.model.token;

import co.arago.hiro.client.model.EncodedURIMessage;
import co.arago.util.json.JsonUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Map;

public class AuthorizeRequest implements EncodedURIMessage {

    @JsonProperty("response_type")
    public String responseType = "code";

    @JsonProperty("client_id")
    public String clientId;

    @JsonProperty("redirect_uri")
    public String redirectURI;

    @JsonProperty("code_challenge")
    public String codeChallenge;

    @JsonProperty("code_challenge_method")
    public String codeChallengeMethod;

    public String state;

    public String scope;

    public AuthorizeRequest(
            String clientId,
            String redirectURI,
            String codeChallenge,
            String codeChallengeMethod,
            String state,
            String scope) {
        this.clientId = clientId;
        this.redirectURI = redirectURI;
        this.codeChallenge = codeChallenge;
        this.codeChallengeMethod = codeChallengeMethod;
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
