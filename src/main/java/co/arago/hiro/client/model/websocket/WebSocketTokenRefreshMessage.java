package co.arago.hiro.client.model.websocket;

import co.arago.hiro.client.util.JsonTools;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.Serializable;

/**
 * <pre><code>
 *     {
 *         "type": "token",
 *         "args": {
 *             "_TOKEN": {@link Token#token}
 *         }
 *     }
 * </code></pre>
 */
public class WebSocketTokenRefreshMessage implements Serializable {

    public static class Token implements Serializable {
        @JsonProperty("_TOKEN")
        public String token;

        public Token(String token) {
            this.token = token;
        }
    }

    public final String type = "token";

    public final Token args;

    public WebSocketTokenRefreshMessage(String token) {
        this.args = new Token(token);
    }

    public String toJsonString() {
        try {
            return JsonTools.DEFAULT.toString(this);
        } catch (JsonProcessingException e) {
            return ""; // should never happen
        }
    }
}
