package hu.blackbelt.judo.generator.commons;

import com.github.jknack.handlebars.ValueResolver;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

public abstract class StaticMethodValueResolver implements ValueResolver {

    private CacheLoader<CacheKey, AtomicReference<Method>> loader = new CacheLoader<CacheKey, AtomicReference<Method>>() {
        @Override
        public AtomicReference<Method> load(CacheKey key) {
            return getMethodForContext(key.context, key.name);
        }
    };

    private LoadingCache<CacheKey, AtomicReference<Method>> cache = CacheBuilder.newBuilder().build(loader);

    static private class CacheKey {
        Object context;
        String name;

        public CacheKey(Object context, String name) {
            this.context = context;
            this.name = name;
        }
    }


    @Override
    public Object resolve(Object context, String name) {
        if (context == null) {
            return UNRESOLVED;
        }
        try {
            AtomicReference<Method> method = cache.get(new CacheKey(context, name));
            if (method.get() != null) {
                return method.get().invoke(null, context);
            }
        } catch (ExecutionException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return UNRESOLVED;

    }

    private AtomicReference<Method> getMethodForContext(Object context, String name) {
        Class methodClazz = this.getClass();

        AtomicReference<Method> method = new AtomicReference<>(null);
        AtomicReference<Class> contextClass = new AtomicReference<>(context.getClass());

        while (method.get() == null && contextClass.get() != null) {
            Arrays.stream(methodClazz.getDeclaredMethods())
                    .filter(m -> m.getName().equals(name))
                    .filter(m -> Modifier.isPublic(m.getModifiers()))
                    .filter(m -> Modifier.isStatic(m.getModifiers()))
                    .filter(m -> m.getParameterCount() == 1)
                    .filter(m -> m.getParameters()[0].getType().isAssignableFrom(contextClass.get()))
                    .findFirst()
                    .ifPresentOrElse(m -> method.set(m), () -> contextClass.set(contextClass.get().getSuperclass()));
        }

        return method;
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
