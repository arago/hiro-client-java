package co.arago.hiro.client.model;

import co.arago.hiro.client.util.httpclient.UriQueryMap;

import java.io.Serializable;

/**
 * This interface defines a default method to create an EncodedForm String from this object.
 */
public interface EncodedFormMessage extends Serializable, ToMap {

    /**
     * @return Encoded Form representation of this object.
     */
    default String toFormString() {
        return new UriQueryMap(toMap()).toString();
    }

}
