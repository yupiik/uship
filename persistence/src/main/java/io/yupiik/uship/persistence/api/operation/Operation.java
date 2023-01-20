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
package io.yupiik.uship.persistence.api.operation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Mark an interface as owning operations, ie methods with @{@link Statement}.
 *
 * IMPORTANT: right now default methods are not yet supported.
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface Operation {
    /**
     * @return the list of aliases to use for the statements of this interface.
     */
    Alias[] aliases() default {};

    /**
     * Enables to mark some types as aliased for SQL interpolations.
     * Ex:
     * <ul>
     *     <li>{@code foo#fields} instead of {@code org.superbiz.MyEntity#fields}</li>
     * </ul>
     */
    @Retention(RUNTIME)
    @interface Alias {
        Class<?> type();

        String alias();
    }
}
