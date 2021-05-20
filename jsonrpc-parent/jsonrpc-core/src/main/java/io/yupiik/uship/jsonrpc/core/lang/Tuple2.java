package io.yupiik.uship.jsonrpc.core.lang;

public class Tuple2<A, B> { // todo: move to lang module?
    private final A first;
    private final B second;

    public Tuple2(final A first, final B second) {
        this.first = first;
        this.second = second;
    }

    public A first() {
        return first;
    }

    public B second() {
        return second;
    }
}