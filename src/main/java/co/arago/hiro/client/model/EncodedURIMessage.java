package co.arago.hiro.client.model;

import co.arago.hiro.client.util.httpclient.URIEncodedData;

import java.io.Serializable;

/**
 * This interface defines a default method to create an EncodedForm String from this object.
 */
public interface EncodedURIMessage extends Serializable, ToMap {

    /**
     * @return Encoded Form representation of this object. Also removes fields with blank values.
     */
    default String toURIEncodedStringRemoveBlanks() {
        return new URIEncodedData(toMap()).toStringRemoveBlanks();
    }

    /**
     * @return Encoded Form representation of this object. Keeps blank values.
     */
    default String toURIEncodedString() {
        return new URIEncodedData(toMap()).toString();
    }
}
