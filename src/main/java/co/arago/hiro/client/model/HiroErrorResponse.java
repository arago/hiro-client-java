package co.arago.hiro.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class HiroErrorResponse extends HiroResponse {

    private static final long serialVersionUID = -5474697322866873485L;

    public static class HiroErrorEntry implements Serializable {
        private static final long serialVersionUID = 960096627468148981L;
        public String message;
        public Integer code;

        public HiroErrorEntry() {
        }

        public HiroErrorEntry(
                String message,
                Integer code
        ) {
            this.message = message;
            this.code = code;
        }
    }

    @JsonProperty("error")
    public HiroErrorEntry error;

    public HiroErrorResponse() {
    }

    public String getHiroErrorMessage() {
        return (error != null ? error.message : null);
    }

    public Integer getHiroErrorCode() {
        return (error != null ? error.code : null);
    }

    /**
     * Creates an HiroErrorResponse from a simple error of the form { "error": "[text]" }.
     * This error has no error code.
     *
     * @param error The error text.
     */
    public HiroErrorResponse(String error) {
        this.error = new HiroErrorEntry(error, null);
    }
}
