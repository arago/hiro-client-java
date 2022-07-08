package co.arago.hiro.client.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;

public interface ToMap {
    /**
     * Return a map of ALL values, including assigned fields specified in derived child classes.
     *
     * @return The map of all fields of this object.
     */
    @JsonIgnore
    Map<String, Object> toMap();
}
