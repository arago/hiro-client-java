package co.arago.hiro.client.model.websocket.events;

import co.arago.hiro.client.model.JsonMessage;
import com.fasterxml.jackson.annotation.JsonProperty;

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
public class WebSocketTokenRefreshMessage implements JsonMessage {

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
}
