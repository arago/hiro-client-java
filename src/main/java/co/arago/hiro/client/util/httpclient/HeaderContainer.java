package co.arago.hiro.client.util.httpclient;

import org.apache.commons.lang3.StringUtils;

import java.net.http.HttpHeaders;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalLong;
import java.util.stream.Collectors;

/**
 * Decodes / Encodes the headers Content-Type and Content-Length.
 */
public class HeaderContainer {
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
     * Constructor. Creates this object with all its data from the incoming httpHeaders.
     *
     * @param httpHeaders The httpHeaders to decode.
     */
    public HeaderContainer(HttpHeaders httpHeaders) {
        this.contentType = httpHeaders.firstValue("content-type").orElse(null);

        OptionalLong contentLength = httpHeaders.firstValueAsLong("content-length");
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
     * Constructor used when Content-Headers have to be constructed.
     *
     * @param mediaType     The mime type for the data.
     * @param charset       The charset - if any. Can be null.
     * @param contentLength The length of the content - if any. Can be null.
     */
    public HeaderContainer(String mediaType, Charset charset, Long contentLength) {
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
     * Check whether the mediaType is application/json
     *
     * @return true if contentType matches, false if not.
     */
    public boolean contentIsJson() {
        return StringUtils.equalsIgnoreCase(mediaType, "application/json");
    }

}
