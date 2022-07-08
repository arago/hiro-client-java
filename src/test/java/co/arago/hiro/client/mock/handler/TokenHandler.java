package co.arago.hiro.client.mock.handler;

import co.arago.hiro.client.model.token.PasswordTokenRequest;
import co.arago.hiro.client.model.token.TokenResponse;
import co.arago.hiro.client.util.httpclient.URIEncodedData;
import co.arago.util.json.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class TokenHandler extends SendJsonResponse implements HttpHandler {

    final static Logger log = LoggerFactory.getLogger(TokenHandler.class);

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String data = IOUtils.toString(exchange.getRequestBody(), StandardCharsets.UTF_8);

            Map<String, ?> map = new URIEncodedData(data).toSingleValueMap();
            PasswordTokenRequest passwordTokenRequest = JsonUtil.DEFAULT.transformObject(
                    map,
                    PasswordTokenRequest.class);

            if (StringUtils.equals(passwordTokenRequest.password, "Wrong") ||
                    StringUtils.equals(passwordTokenRequest.clientSecret, "Wrong")) {
                new UnauthorizedHandler().handle(exchange);
                return;
            }

            TokenResponse tokenResponse = JsonUtil.DEFAULT.transformObject(Map.of(
                    "access_token", "test_1234567890",
                    "refresh_token", "test_refresh_1234567890",
                    "expires_in", "3600"), TokenResponse.class);

            sendJsonResponse(exchange, HttpURLConnection.HTTP_OK, tokenResponse.toJsonStringNoNull());
        } catch (Throwable t) {
            log.error("TokenHandler", t);
        }
    }
}
