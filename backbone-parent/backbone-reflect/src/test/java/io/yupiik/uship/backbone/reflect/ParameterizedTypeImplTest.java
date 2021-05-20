package io.yupiik.uship.backbone.reflect;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParameterizedTypeImplTest {
    @Test
    void equalsHashCode() {
        final var p1 = new ParameterizedTypeImpl(List.class, String.class);
        final var p2 = new ParameterizedTypeImpl(List.class, String.class);
        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }
}
