package io.yupiik.uship.jsonrpc.core.protocol;

import jakarta.json.JsonValue;
import jakarta.json.bind.annotation.JsonbPropertyOrder;

@JsonbPropertyOrder({"jsonrpc", "id", "result", "error"})
public class Response {
    private String jsonrpc;
    private JsonValue id;
    private JsonValue result;
    private ErrorResponse error;

    public Response() {
        // no-op
    }

    public Response(final String jsonrpc, final JsonValue id, final JsonValue result, final ErrorResponse error) {
        this.jsonrpc = jsonrpc;
        this.id = id;
        this.result = result;
        this.error = error;
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(final String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public JsonValue getId() {
        return id;
    }

    public void setId(final JsonValue id) {
        this.id = id;
    }

    public JsonValue getResult() {
        return result;
    }

    public void setResult(final JsonValue result) {
        this.result = result;
    }

    public ErrorResponse getError() {
        return error;
    }

    public void setError(final ErrorResponse error) {
        this.error = error;
    }

    @JsonbPropertyOrder({"code", "message", "data"})
    public static class ErrorResponse {
        private int code;
        private String message;
        private JsonValue data;

        public ErrorResponse() {
            // no-op
        }

        public ErrorResponse(final int code, final String message, final JsonValue data) {
            this.code = code;
            this.message = message;
            this.data = data;
        }

        public int getCode() {
            return code;
        }

        public void setCode(final int code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(final String message) {
            this.message = message;
        }

        public JsonValue getData() {
            return data;
        }

        public void setData(final JsonValue data) {
            this.data = data;
        }
    }
}
