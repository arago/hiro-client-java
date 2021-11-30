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

    private static final long serialVersionUID = -1345807437979508716L;

    public static class Token implements Serializable {

        private static final long serialVersionUID = 2335533223814817461L;

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
