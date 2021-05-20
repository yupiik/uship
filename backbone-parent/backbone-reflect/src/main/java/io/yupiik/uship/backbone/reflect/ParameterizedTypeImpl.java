package io.yupiik.uship.backbone.reflect;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Objects;

public class ParameterizedTypeImpl implements ParameterizedType {
    private final Type rawType;
    private final Type[] types;

    private Integer hash;

    public ParameterizedTypeImpl(final Type rawType, final Type... types) {
        this.rawType = rawType;
        this.types = types;
    }

    @Override
    public Type[] getActualTypeArguments() {
        return types;
    }

    @Override
    public Type getOwnerType() {
        return null;
    }

    @Override
    public Type getRawType() {
        return rawType;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(types) ^ (rawType == null ? 0 : rawType.hashCode());
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (ParameterizedType.class.isInstance(obj)) {
            final ParameterizedType that = ParameterizedType.class.cast(obj);
            final Type thatRawType = that.getRawType();
            return Objects.equals(rawType, thatRawType) && Arrays.equals(types, that.getActualTypeArguments());
        }
        return false;
    }

    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        buffer.append(Class.class.cast(rawType).getSimpleName());
        final Type[] actualTypes = getActualTypeArguments();
        if (actualTypes.length > 0) {
            buffer.append("<");
            final int length = actualTypes.length;
            for (int i = 0; i < length; i++) {
                if (Class.class.isInstance(actualTypes[i])) {
                    buffer.append(Class.class.cast(actualTypes[i]).getSimpleName());
                } else {
                    buffer.append(actualTypes[i].toString());
                }
                if (i != actualTypes.length - 1) {
                    buffer.append(",");
                }
            }
            buffer.append(">");
        }
        return buffer.toString();
    }
}