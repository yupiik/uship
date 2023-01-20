/*
 * Copyright (c) 2021-2023 - Yupiik SAS - https://www.yupiik.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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
