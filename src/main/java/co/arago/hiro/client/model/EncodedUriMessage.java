package co.arago.hiro.client.model;

import co.arago.hiro.client.util.httpclient.UriEncodedData;

import java.io.Serializable;

/**
 * This interface defines a default method to create an EncodedForm String from this object.
 */
public interface EncodedUriMessage extends Serializable, ToMap {

    /**
     * @return Encoded Form representation of this object. Also removes fields with blank values.
     */
    default String toUriEncodedStringRemoveBlanks() {
        return new UriEncodedData(toMap()).toStringRemoveBlanks();
    }

    /**
     * @return Encoded Form representation of this object. Keeps blank values.
     */
    default String toUriEncodedString() {
        return new UriEncodedData(toMap()).toString();
    }
}
