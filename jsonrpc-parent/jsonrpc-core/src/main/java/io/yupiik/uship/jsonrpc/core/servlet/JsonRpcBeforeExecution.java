package io.yupiik.uship.jsonrpc.core.servlet;

import jakarta.json.JsonStructure;
import jakarta.servlet.http.HttpServletRequest;

public class JsonRpcBeforeExecution {
    private final JsonStructure jsonRpcRequest;
    private final HttpServletRequest request;

    JsonRpcBeforeExecution(final JsonStructure jsonRpcRequest, final HttpServletRequest request) {
        this.jsonRpcRequest = jsonRpcRequest;
        this.request = request;
    }

    public JsonStructure getJsonRpcRequest() {
        return jsonRpcRequest;
    }

    public HttpServletRequest getRequest() {
        return request;
    }
}
