package co.arago.hiro.client.model;

public class TokenRequest {
    // Match the JSON structure for the request.
    public String username;
    public String password;
    public String client_id;
    public String client_secret;

    public TokenRequest(String username, String password, String client_id, String client_secret) {
        this.username = username;
        this.password = password;
        this.client_id = client_id;
        this.client_secret = client_secret;
    }
}
