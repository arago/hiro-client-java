package co.arago.hiro.client.model;

import co.arago.hiro.client.util.httpclient.UriEncodedMap;

import java.io.Serializable;

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
