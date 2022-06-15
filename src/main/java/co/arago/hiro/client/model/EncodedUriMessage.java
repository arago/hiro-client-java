package co.arago.hiro.client.model;

import co.arago.hiro.client.util.httpclient.UriEncodedMap;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This interface defines a default method to create an EncodedForm String from this object.
 */
public interface EncodedUriMessage extends Serializable, ToMap {

    /**
     * @return Encoded Form representation of this object. Also removes fields with blank values.
     */
    default String toEncodedString() {
        Map<String, Object> map = toMap()
                .entrySet()
                .stream()
                .filter(entry -> StringUtils.isNotBlank((String) entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return new UriEncodedMap(map).toString();
    }

    /**
     * @return Encoded Form representation of this object. Keeps blank values.
     */
    default String toEncodedStringKeepBlanks() {
        return new UriEncodedMap(toMap()).toString();
    }
}
