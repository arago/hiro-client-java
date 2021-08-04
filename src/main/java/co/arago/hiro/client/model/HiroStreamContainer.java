package co.arago.hiro.client.model;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalLong;
import java.util.stream.Collectors;

/**
 * Contains the inputStream for the body and decodes the headers Content-Type and Content-Length for further
 * information about that stream.
 */
public class HiroStreamContainer {
    /**
     * The inputStream for the response body
     */
    public InputStream inputStream;
    /**
     * The Content-Type as read from the response header. This might be null when this header is missing.
     */
    public String contentType;
    /**
     * The Content-Length as read from the response header. This might be null when this header is missing.
     */
    public Long contentLength;

    /**
     * Decoded from the Content-Type. Might be null.
     */
    public String mediaType;
    /**
     * Decoded from the Content-Type if it exists. Might be null.
     */
    public Charset charset;
    /**
     * Decoded from the Content-Type if it exists. Might be null.
     */
    public String boundary;

    /**
     * Constructor. Creates this object with all its data from the incoming httpResponse.
     *
     * @param httpResponse The httpResponse to decode.
     */
    public HiroStreamContainer(HttpResponse<InputStream> httpResponse) {
        this.inputStream = httpResponse.body();

        this.contentType = httpResponse.headers().firstValue("content-type").orElse(null);

        OptionalLong contentLength = httpResponse.headers().firstValueAsLong("content-length");
        this.contentLength = (contentLength.isPresent() ? contentLength.getAsLong() : null);

        // Decode Content-Type
        if (this.contentType != null) {
            List<String> contentParts = Arrays.stream(this.contentType.split(";"))
                    .map(String::trim)
                    .collect(Collectors.toList());

            this.mediaType = contentParts.remove(0);

            for (String contentPart : contentParts) {
                String[] parameter = contentPart.split("=");
                if (parameter.length == 2) {
                    if (StringUtils.equalsIgnoreCase(parameter[0], "charset")) {
                        this.charset = Charset.forName(parameter[1]);
                    }
                    if (StringUtils.equalsIgnoreCase(parameter[0], "boundary")) {
                        this.boundary = parameter[1];
                    }
                }
            }
        }
    }

    /**
     * Constructor used when streams have to be sent.
     *
     * @param inputStream   The inputStream with the data.
     * @param mediaType     The mime type for the data.
     * @param charset       The charset - if any. Can be null.
     * @param contentLength The length of the content - if any. Can be null.
     */
    public HiroStreamContainer(InputStream inputStream, String mediaType, Charset charset, Long contentLength) {
        if (inputStream == null)
            throw new IllegalArgumentException("InputStream must not be null");

        this.inputStream = inputStream;
        this.mediaType = mediaType;
        this.charset = charset;
        this.contentLength = contentLength;

        if (mediaType != null) {
            this.contentType = mediaType;

            if (charset != null) {
                this.contentType += ";charset=" + charset;
            }
        }
    }

    /**
     * Read the inputStream and return it as String in UTF-8 or the charset provided .
     *
     * @return The String constructed from the {@link #inputStream} or null if the {@link #inputStream} is null.
     * @throws IOException If the {@link #inputStream} cannot be read.
     */
    public String getBodyAsString() throws IOException {
        if (inputStream == null)
            return null;

        return IOUtils.toString(inputStream, (charset != null ? charset : StandardCharsets.UTF_8));
    }


    /**
     * Check whether the mediaType is application/json
     *
     * @return true if contentType matches, false if not.
     */
    public boolean contentIsJson() {
        return StringUtils.equalsIgnoreCase(mediaType, "application/json");
    }

}
