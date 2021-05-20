package io.yupiik.uship.jsonrpc.core.protocol;

public class JsonRpcException extends RuntimeException {
    private final int code;
    private final String message;
    private final Object data;

    public JsonRpcException(final int code, final String message) {
        super(message);
        this.code = code;
        this.message = message;
        this.data = null;
    }

    public JsonRpcException(final int code, final String message, final Throwable parent) {
        this(code, message, null, parent);
    }

    public JsonRpcException(final int code, final String message, final Object data, final Throwable parent) {
        super(message, parent);
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public Object getData() {
        return data;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
