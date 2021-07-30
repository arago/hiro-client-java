package co.arago.hiro.client.model;

public class TokenRefreshRequest {
    // Match the JSON structure for the request.
    public String client_id;
    public String client_secret;
    public String refresh_token;

    public TokenRefreshRequest(String client_id, String client_secret, String refresh_token) {
        this.client_id = client_id;
        this.client_secret = client_secret;
        this.refresh_token = refresh_token;
    }
}
