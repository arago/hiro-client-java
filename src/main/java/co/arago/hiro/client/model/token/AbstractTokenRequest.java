package co.arago.hiro.client.model.token;

import co.arago.hiro.client.model.EncodedUriMessage;
import co.arago.hiro.client.model.JsonMessage;
import co.arago.util.json.JsonUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Map;

/**
 * JSON for a token request.
 *
 *
 * <pre>
 * {
 *     "grant_type": "...",
 *     "client_id": "...",
 *     "client_secret": "...",
 *     "organization": "...",
 *     "organization_id: "..."
 * }
 * </pre>
 */
public abstract class AbstractTokenRequest implements JsonMessage, EncodedUriMessage {

    // Match the JSON structure for the request.

    private static final long serialVersionUID = 5952932214083553438L;

    @JsonProperty("grant_type")
    public String grantType;

    @JsonProperty("client_id")
    public String clientId;

    @JsonProperty("client_secret")
    public String clientSecret;

    /**
     * Optional
     */
    @JsonProperty("organization")
    public String organization;

    /**
     * Optional
     */
    @JsonProperty("organization_id")
    public String organizationId;

    public AbstractTokenRequest(
            String grantType,
            String clientId,
            String clientSecret,
            String organization,
            String organizationId) {
        this.grantType = grantType;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.organization = organization;
        this.organizationId = organizationId;
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
