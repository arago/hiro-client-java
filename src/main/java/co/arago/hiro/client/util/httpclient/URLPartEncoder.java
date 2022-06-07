package co.arago.hiro.client.util.httpclient;

import java.net.URLEncoder;
import java.nio.charset.Charset;

public class URLPartEncoder {
    public static String encodeNoPlus(String source, Charset charset) {
        return URLEncoder.encode(source, charset).replace("+", "%20");
    }
}
