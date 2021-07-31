package co.arago.hiro.client.model;

import co.arago.hiro.client.util.JsonTools;

import java.util.LinkedHashMap;
import java.util.Map;

public class TokenErrorResponse extends LinkedHashMap<String, Object> {

    public String getMessage() {
        Object error = get("error");
        if (error instanceof Map)
            return (String) ((Map<?, ?>) error).get("message");
        return null;
    }

    public Integer getCode() {
        Object error = get("error");
        if (error instanceof Map)
            return (Integer) ((Map<?, ?>) error).get("code");
        return null;
    }

    public static TokenErrorResponse fromResponse(HiroResponse hiroResponse) {
        return JsonTools.DEFAULT.toObject(hiroResponse, TokenErrorResponse.class);
    }

}
