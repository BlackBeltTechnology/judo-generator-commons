package hu.blackbelt.judo.generator.commons;

/*-
 * #%L
 * JUDO Generator commons
 * %%
 * Copyright (C) 2018 - 2023 BlackBelt Technology
 * %%
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 * 
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 2
 * with the GNU Classpath Exception which is
 * available at https://www.gnu.org/software/classpath/license.html.
 * 
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 * #L%
 */

import com.github.jknack.handlebars.ValueResolver;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
public abstract class StaticMethodValueResolver implements ValueResolver {

    private CacheLoader<String, Collection<Method>> methodListLoaderByName = new CacheLoader<String, Collection<Method>>() {
        @Override
        public Collection<Method> load(String key) {
            return getMethodListByName(key);
        }
    };

    private LoadingCache<String, Collection<Method>> methodListCacheByContextAndName = CacheBuilder.newBuilder().build(methodListLoaderByName);

    /**
     * Resolved method per context class and helper name ({@link Optional#empty()} = no match). The match depends on the
     * context class only, and templates call helpers per element, so the former per-call lookup (reflection,
     * {@code getParameters()} allocation, superclass walk) dominated template rendering.
     */
    private final Map<Class<?>, Map<String, Optional<Method>>> methodByContextClassAndName = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public Object resolve(Object context, String name) {
        try {
            AtomicReference<Method> method = getMethodByContextClassByName(context, name);
            if (method.get() != null) {
                if (context == null || method.get().getParameterCount() == 0) {
                    return method.get().invoke(null);
                } else if (method.get().getParameterCount() == 1) {
                    return method.get().invoke(null, context);
                }
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return UNRESOLVED;

    }

    private Collection<Method> getMethodListByName(String name) {
        Class methodClazz = this.getClass();
        Collection<Method> potentialMethods = Arrays.stream(methodClazz.getDeclaredMethods())
                .filter(m -> m.getName().equals(name))
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .filter(m -> Modifier.isStatic(m.getModifiers())).collect(Collectors.toList());
        return potentialMethods;
    }

    private AtomicReference<Method> getMethodByContextClassByName(Object context, String name) {
        Class<?> contextClass = context.getClass();
        Optional<Method> resolved = methodByContextClassAndName
                .computeIfAbsent(contextClass, c -> new java.util.concurrent.ConcurrentHashMap<>())
                .computeIfAbsent(name, n -> Optional.ofNullable(findMethodByContextClassByName(context, n)));
        if (resolved.isEmpty()) {
            warnOnMismatch(context, name);
        }
        return new AtomicReference<>(resolved.orElse(null));
    }

    private Collection<Method> methodsByName(String name) {
        try {
            return methodListCacheByContextAndName.get(name);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private Method findMethodByContextClassByName(Object context, String name) {

        AtomicReference<Method> method = new AtomicReference<>(null);
        AtomicReference<Class> contextClassRef = new AtomicReference<>(context.getClass());
        Collection<Method> methods = methodsByName(name);

        while (method.get() == null && contextClassRef.get() != null) {
            methods.stream()
                    .filter(m -> (m.getParameterCount() == 0 || m.getParameterCount() == 1 && m.getParameters()[0].getType().isAssignableFrom(contextClassRef.get())))
                    .findFirst()
                    .ifPresentOrElse(m -> method.set(m), () ->
                            contextClassRef.set(contextClassRef.get().getSuperclass())
                    );
        }
        return method.get();
    }

    /** The former warning for a helper name that exists with a non-matching signature; logged on every such call. */
    private void warnOnMismatch(Object context, String name) {
        Collection<Method> methods = methodsByName(name);
        if (!context.getClass().getName().equals("java.lang.Object")
            && !(context instanceof Map)
                && methods.stream().anyMatch(m -> m.getName().equals(name))) {
            for (Method methodByName : methods.stream().filter(m -> m.getName().equals(name)).collect(Collectors.toList())) {
                String methodParameters = " Method parameter(s): \n\t" + Arrays.stream(methodByName.getParameters()).sequential()
                        .map(m -> m.getName() + ": " + m.getType().getName())
                        .collect(Collectors.joining("\n\t"));
                log.warn("Method: " + name + " presented, but the argument number and/or type does not match. \nContext type: " + context.getClass().getName() +
                        " Value: " + (context == null ? "null" : context.toString()) + methodParameters);
            }
        }
    }

    @Override
    public Object resolve(final Object context) {
        return UNRESOLVED;
    }

    @Override
    public Set<Map.Entry<String, Object>> propertySet(Object context) {
        return new HashSet<>();
    }
}
