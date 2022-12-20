package hu.blackbelt.judo.generator.commons;

import java.util.Map;

/**
 * The handlebars context inaccessible in helpers / value resolvers
 * because there is no state for them. The ThreadLocal is used
 * to init variable values from template execution.
 */
public abstract class ThreadLocalContextHolder {
    private static ThreadLocal<Map<String, ?>> contextLocal = new ThreadLocal<>();

    public static ThreadLocal<Map<String, ?>>getContextLocal() {
        return contextLocal;
    }

    public static void bindContext(Map<String, ?> context) {
        contextLocal.set(context);
    }

    public static synchronized Object getVariable(String key) {
        if (contextLocal.get() == null) {
            System.out.println("Could not retrieve contextLocal");
            throw new RuntimeException("Could not retrieve contextLocal");
        } else {
            return contextLocal.get().get(key);
        }
    }

}
