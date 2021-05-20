package io.yupiik.uship.backbone.reflect;

import org.junit.jupiter.api.Test;

import static io.yupiik.uship.backbone.reflect.Reflections.resolveType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ReflectionsTest {
    @Test
    void resolve() throws NoSuchMethodException {
        final var returnType = Base.class.getMethod("getValue").getGenericReturnType();
        assertNotEquals(Foo.class, returnType, returnType::toString);
        assertEquals(Foo.class, resolveType(returnType, Child.class));
    }

    public static class Foo {
    }

    public static abstract class Base<A> {
        public A getValue() {
            return get();
        }

        public abstract A get();
    }

    public static class Child extends Base<Foo> {
        @Override
        public Foo get() {
            return null;
        }
    }
}
