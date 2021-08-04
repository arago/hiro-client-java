package co.arago.hiro.client.util.httpclient;

import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Contains the inputStream for the body and decodes the headers Content-Type and Content-Length for further
 * information about that stream.
 */
public class StreamContainer extends HeaderContainer {
    /**
     * The inputStream for the response body
     */
    public InputStream inputStream;

    /**
     * Constructor used when streams have to be sent.
     *
     * @param inputStream   The inputStream with the data.
     * @param mediaType     The mime type for the data.
     * @param charset       The charset - if any. Can be null.
     * @param contentLength The length of the content - if any. Can be null.
     */
    public StreamContainer(InputStream inputStream, String mediaType, Charset charset, Long contentLength) {
        super(mediaType, charset, contentLength);
        if (inputStream == null)
            throw new IllegalArgumentException("InputStream must not be null");

        this.inputStream = inputStream;
    }

}
