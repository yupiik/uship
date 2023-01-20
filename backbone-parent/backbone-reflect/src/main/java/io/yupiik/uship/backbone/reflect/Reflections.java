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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

public final class Reflections {
    private Reflections() {
        // no-op
    }

    // not a full and complete impl but what we use
    public static Type resolveType(final Type type, final Class<?> declaringClass) {
        final Type realType = extractRealType(declaringClass, type);
        if (ParameterizedType.class.isInstance(type) && (realType != type ||
                Stream.of(ParameterizedType.class.cast(type).getActualTypeArguments()).anyMatch(TypeVariable.class::isInstance))) {
            return resolveParameterizedType(type, declaringClass);
        }
        if (TypeVariable.class.isInstance(type) && declaringClass.getSuperclass() != null) {
            final TypeVariable tv = TypeVariable.class.cast(type);
            final TypeVariable<? extends Class<?>>[] typeParameters = declaringClass.getSuperclass().getTypeParameters();
            if (typeParameters != null && ParameterizedType.class.isInstance(declaringClass.getGenericSuperclass())) {
                final ParameterizedType pt = ParameterizedType.class.cast(declaringClass.getGenericSuperclass());
                if (typeParameters.length == pt.getActualTypeArguments().length) {
                    for (int i = 0; i < typeParameters.length; i++) {
                        if (tv == typeParameters[i]) {
                            return pt.getActualTypeArguments()[i];
                        }
                    }
                }
            }
        }
        return type;
    }

    private static Type resolveParameterizedType(final Type type, final Class<?> declaringClass) {
        final ParameterizedType pt = ParameterizedType.class.cast(type);
        final Type resolvedParam = resolveType(pt.getActualTypeArguments()[0], declaringClass);
        if (pt.getActualTypeArguments()[0] != resolvedParam) {
            return new ParameterizedTypeImpl(pt.getRawType(), new Type[]{resolvedParam});
        }
        return type;
    }

    public static Type extractRealType(final Class<?> root, final Type type) {
        if (ParameterizedType.class.isInstance(type)) {
            final ParameterizedType pt = ParameterizedType.class.cast(type);
            return Stream.of(Optional.class, CompletionStage.class, CompletableFuture.class)
                    .anyMatch(gt -> pt.getRawType() == gt) ?
                    (ParameterizedType.class.isInstance(pt.getActualTypeArguments()[0]) ?
                            resolveParameterizedType(pt.getActualTypeArguments()[0], root) : pt.getActualTypeArguments()[0]) :
                    pt;
        }
        if (TypeVariable.class.isInstance(type)) {
            final Map<Type, Type> resolution = toResolvedTypes(root, 0);
            Type value = type;
            int max = 15;
            do {
                value = resolution.get(value);
                max--;
            } while (max > 0 && value != null && resolution.containsKey(value));
            return ofNullable(value).orElse(type);
        }
        return type;
    }

    private static Map<Type, Type> toResolvedTypes(final Type clazz, final int maxIt) {
        if (maxIt > 15) { // avoid loops
            return emptyMap();
        }
        if (Class.class.isInstance(clazz)) {
            return toResolvedTypes(Class.class.cast(clazz).getGenericSuperclass(), maxIt + 1);
        }
        if (ParameterizedType.class.isInstance(clazz)) {
            final ParameterizedType parameterizedType = ParameterizedType.class.cast(clazz);
            if (!Class.class.isInstance(parameterizedType.getRawType())) {
                return emptyMap(); // not yet supported
            }
            final Class<?> raw = Class.class.cast(parameterizedType.getRawType());
            final Type[] arguments = parameterizedType.getActualTypeArguments();
            if (arguments.length > 0) {
                final TypeVariable<? extends Class<?>>[] parameters = raw.getTypeParameters();
                final Map<Type, Type> map = new HashMap<>(parameters.length);
                for (int i = 0; i < parameters.length && i < arguments.length; i++) {
                    map.put(parameters[i], arguments[i]);
                }
                return Stream.concat(map.entrySet().stream(), toResolvedTypes(raw, maxIt + 1).entrySet().stream())
                        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));
            }
        }
        return emptyMap();
    }
}
