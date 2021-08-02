package co.arago.hiro.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * This is a renaming of a LinkedHashMap&lt;String, Object&gt; to avoid unmapped errors when using Jackson.
 * Also adds some HIRO specific functions like handling Lists under { "Items": [] }.
 */
public class HiroVertexListResponse extends HiroResponse {

    private static final long serialVersionUID = 8265977186057475224L;

    @JsonProperty("items")
    public List<HiroVertexResponse> items;

}
