package co.arago.hiro.client.model;

import co.arago.hiro.client.util.httpclient.UriEncodedMap;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Map;

/**
 * This interface defines a default method to create an EncodedForm String from this object.
 */
public interface EncodedUriMessage extends Serializable, ToMap {

    /**
     * @return Encoded Form representation of this object.
     */
    default String toEncodedString() {
        return new UriEncodedMap(toMap()).toString();
    }

}
