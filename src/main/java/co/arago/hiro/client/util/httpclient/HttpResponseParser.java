package co.arago.hiro.client.util.httpclient;

import co.arago.hiro.client.exceptions.HiroException;
import co.arago.hiro.client.model.HiroMessage;
import co.arago.hiro.client.util.HexUtil;
import co.arago.hiro.client.util.HttpLogger;
import co.arago.util.json.JsonUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.TeeInputStream;
import org.apache.commons.lang3.StringUtils;

import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Contains the httpResult and decodes the headers Content-Type and Content-Length for further
 * information about that stream.
 */
public class HttpResponseParser extends ContentHeaderHandler {

    private static final String[] TEXT_CONTENT = new String[] {
            "text/",
            "application/json",
            "application/xhtml",
            "application/xml"
    };

    /**
     * The response with its InputStream.
     */
    private final HttpResponse<InputStream> httpResponse;
    private final HttpLogger httpLogger;

    /**
     * Constructor. Creates this object with all its data from the incoming httpResponse.
     *
     * @param httpResponse The httpResponse to decode.
     * @param httpLogger   The logger to log the response.
     */
    public HttpResponseParser(HttpResponse<InputStream> httpResponse, HttpLogger httpLogger) {
        super(httpResponse.headers());
        this.httpResponse = httpResponse;
        this.httpLogger = httpLogger;

        if (httpLogger != null && httpLogger.active())
            httpLogger.logResponseHeaders(httpResponse);
    }

    public int getStatusCode() {
        return httpResponse.statusCode();
    }

    public HttpRequest getRequest() {
        return httpResponse.request();
    }

    public URI getURI() {
        return httpResponse.uri();
    }

    public Optional<SSLSession> getSSLSession() {
        return httpResponse.sslSession();
    }

    public HttpHeaders getHeaders() {
        return httpResponse.headers();
    }

    /**
     * Access the inputStream and apply the logger
     *
     * @return The inputStream of the httpResponse.
     */
    public InputStream getInputStream() {
        InputStream inputStream = httpResponse.body();
        if (httpLogger != null && httpLogger.active()) {
            return new TeeInputStream(httpResponse.body(), new OutputStream() {
                private final ByteBuffer buffer = ByteBuffer.allocate(httpLogger.getMaxBinaryLength());

                private long count = 0;

                @Override
                public void write(int b) throws IOException {
                    if (count < httpLogger.getMaxBinaryLength())
                        buffer.put((byte) b);
                    count++;
                }

                @Override
                public void close() {
                    String message = (StringUtils.startsWithAny(mediaType, TEXT_CONTENT))
                            ? new String(buffer.array(), (charset == null ? StandardCharsets.UTF_8 : charset))
                            : "(hex) " + HexUtil.bytesToHex(buffer.array());

                    if (count >= httpLogger.getMaxBinaryLength())
                        message += "... [rest omitted. final size: " + count + " char]";

                    httpLogger.logResponseBody(httpResponse, message);
                }
            }, true);
        } else {
            return inputStream;
        }
    }

    /**
     * Read the inputStream and return it as String in UTF-8 or the charset provided .
     *
     * @return The String constructed from the {@link #httpResponse} or null if the {@link #httpResponse} is null.
     * @throws IOException If the {@link #httpResponse} cannot be read.
     */
    public String consumeResponseAsString() throws IOException {
        if (httpResponse == null)
            return null;

        try (InputStream inputStream = httpResponse.body()) {
            String body = IOUtils.toString(inputStream, (charset != null ? charset : StandardCharsets.UTF_8));
            if (httpLogger != null && httpLogger.active())
                httpLogger.logResponseBody(httpResponse, body);
            return body;
        }
    }

    /**
     * Convert the response into an object.
     *
     * @param clazz Class of the desired object.
     * @param <T>   Type of the desired object.
     * @return The new object created from the JSON or null if no body is available.
     * @throws HiroException When the incoming data is not of type "application/json".
     * @throws IOException   When the creation of the object fails.
     */
    public <T extends HiroMessage> T createResponseObject(Class<T> clazz) throws IOException, HiroException {
        String body = consumeResponseAsString();
        if (StringUtils.isBlank(body))
            return null;

        if (!contentIsJson())
            throw new HiroException("Incoming data is not of type 'application/json'");

        return JsonUtil.DEFAULT.toObject(body, clazz);
    }

}
