package co.arago.hiro.client.model.websocket.events.impl;

import co.arago.hiro.client.model.JsonMessage;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * <pre>
 *     {
 *         "type": "token",
 *         "args": {
 *             "_TOKEN": {@link Token#token}
 *         }
 *     }
 * </pre>
 */
public class TokenRefreshMessage implements JsonMessage {

    public static class Token implements Serializable {
        @JsonProperty("_TOKEN")
        public String token;

        public Token(String token) {
            this.token = token;
        }
    }

    public final String type = "token";

    public final Token args;

    public TokenRefreshMessage(String token) {
        this.args = new Token(token);
    }
}
