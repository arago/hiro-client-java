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
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Type">Documentation of Content-Type</a>
 */
public class ContentHeaderHandler {
    /**
     * The Content-Length as read from the response header. This might be null when this header is missing.
     */
    protected Long contentLength;

    /**
     * Decoded from the Content-Type. Might be null.
     */
    protected String mediaType;
    /**
     * Decoded from the Content-Type if it exists. Might be null.
     */
    protected Charset charset;
    /**
     * Decoded from the Content-Type if it exists. Might be null.
     */
    protected String boundary;

    /**
     * Constructor. Creates this object with all its data from the incoming httpHeaders.
     *
     * @param httpHeaders The httpHeaders to decode.
     */
    public ContentHeaderHandler(HttpHeaders httpHeaders) {
        setContentType(httpHeaders.firstValue("content-type").orElse(null));

        OptionalLong contentLength = httpHeaders.firstValueAsLong("content-length");
        this.contentLength = (contentLength.isPresent() ? contentLength.getAsLong() : null);
    }

    /**
     * Decodes a Content-Type header and sets mediaType, charset and boundary from it.
     *
     * @param contentType The content-type directly from a HTTP header.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Type">Documentation of Content-Type</a>
     */
    public void setContentType(String contentType) {
        if (contentType != null) {
            List<String> contentParts = Arrays.stream(contentType.split(";"))
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
     * Constructs the Header Content-Type from the mediaType, charset or boundary.
     *
     * @return The HTTP header value for Content-Type.
     */
    public String getContentType() {
        String contentType = null;

        if (mediaType != null) {
            contentType = mediaType;

            if (charset != null) {
                contentType += ";charset=" + charset;
            }
            if (boundary != null) {
                contentType += ";boundary=" + charset;
            }
        }

        return contentType;
    }

    public boolean hasContentType() {
        return StringUtils.isNotBlank(mediaType);
    }


    /**
     * Constructor used when Content-Headers have to be constructed.
     *
     * @param mediaType     The mime type for the data.
     * @param charset       The charset - if any. Can be null.
     * @param contentLength The length of the content - if any. Can be null.
     */
    public ContentHeaderHandler(String mediaType, Charset charset, Long contentLength) {
        this.mediaType = mediaType;
        this.charset = charset;
        this.contentLength = contentLength;
    }

    public void setContentLength(Long contentLength) {
        this.contentLength = contentLength;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public void setBoundary(String boundary) {
        this.boundary = boundary;
    }

    public Long getContentLength() {
        return contentLength;
    }

    public String getMediaType() {
        return mediaType;
    }

    public Charset getCharset() {
        return charset;
    }

    public String getBoundary() {
        return boundary;
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
