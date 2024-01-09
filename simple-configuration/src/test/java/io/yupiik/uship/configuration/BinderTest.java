/*
 * Copyright (c) 2021-present - Yupiik SAS - https://www.yupiik.com
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
package io.yupiik.uship.configuration;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.StringJoiner;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BinderTest {
    @Test
    void unset() {
        assertEquals(
                "ListConfiguration[list=[a, b]]",
                new Binder("a", List.of("--a-list", "yupiik.binder.unset")).bind(ListConfiguration.class).toString());
    }

    @Test
    void list() {
        assertEquals(
                "ListConfiguration[list=[a, b]]",
                new Binder("a", List.of()).bind(ListConfiguration.class).toString());
    }

    @Test
    void listWithConfig() {
        assertEquals(
                "ListConfiguration[list=[c]]",
                new Binder("a", List.of("--a-list", "c")).bind(ListConfiguration.class).toString());
    }

    @Test
    void listWithConfigAndMultipleValues() {
        final var configuration = new Binder("a", List.of("--a-list", "c,b")).bind(ListConfiguration.class);
        assertEquals("ListConfiguration[list=[c, b]]", configuration.toString());
    }

    @Test
    void enumValues() {
        assertEquals(
                "EnumValue[values=[A, B], value=B]",
                new Binder(null, List.of("--list", "A,B", "--value", "B")).bind(EnumValue.class).toString());
    }

    public enum EnumType {
        A, B
    }

    public static class EnumValue {
        @Param(name = "list", description = "")
        List<EnumType> values;

        @Param(name = "value", description = "")
        EnumType value;

        @Override
        public String toString() {
            return new StringJoiner(", ", EnumValue.class.getSimpleName() + "[", "]")
                    .add("values=" + values)
                    .add("value=" + value)
                    .toString();
        }
    }

    public static class ListConfiguration {
        @Param(name = "list", description = "")
        List<String> values = List.of("a", "b");

        @Override
        public String toString() {
            return new StringJoiner(", ", ListConfiguration.class.getSimpleName() + "[", "]")
                    .add("list=" + values)
                    .toString();
        }
    }
}
