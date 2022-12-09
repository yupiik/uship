/*
 * Copyright (c) 2021-2022 - Yupiik SAS - https://www.yupiik.com
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

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Mark a primitive, {@code String} or {@code List<String>} to be read from the main args.
 * When a list, the parameter can be repeated to get multiple values.
 *
 * Note that if the command line does not have the parameter and it is not a list, it is also read from the environment variables.
 * (normalized,  {@code foo.bar} becomes {@code FOO_BAR} for example).
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface Param {
    /**
     * The name of the parameter in the CLI without leading {@code -}.
     * Will match with one or two leading {@code -} and will support {@code --no-} prefix (value set to false).
     *
     * If no value follows the argument, value is set to {@code false}.
     *
     * If the value is not set, the field name is used.
     *
     * @return name of the parameter.
     */
    String name() default "";

    /**
     * For a list, required means not empty.
     *
     * @return true if the binding should fail if parameter is required.
     */
    boolean required() default false;

    /**
     * @return some light description on what the parameter does.
     */
    String description();
}
