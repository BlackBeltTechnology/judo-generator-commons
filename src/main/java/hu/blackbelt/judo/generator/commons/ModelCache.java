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

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Generation-run scoped memoization for {@code @TemplateHelper} methods.
 *
 * <p>Template helpers are static and are called many times per generated file, often re-scanning the whole input
 * model. A generator engine opens a scope for the model's container object (typically the EMF {@code ResourceSet})
 * for the duration of one generation run; helpers call {@link #get(Object, Object, Supplier)} with the same scope
 * object to compute model-wide indexes once per run.</p>
 *
 * <ul>
 *   <li>Scopes are matched by identity and reference-counted: nested or concurrent runs over the same model share
 *       one cache, which is dropped when the last run closes it. Nothing is retained after a run, so cached values
 *       (which usually reference the model) cannot leak the model.</li>
 *   <li>Outside an open scope {@link #get} simply calls the loader: helpers stay correct when used by an engine
 *       that does not open scopes, or directly from tests.</li>
 *   <li>Each key is computed once even when helpers run on parallel threads. A loader may read other keys, but must
 *       not (directly or indirectly) request its own key: that would block forever.</li>
 *   <li>The model must not be modified while a scope is open.</li>
 * </ul>
 */
public final class ModelCache {

    private static final Map<Object, Scope> SCOPES = new IdentityHashMap<>();

    private ModelCache() {
    }

    /**
     * Opens (or joins) the cache scope of {@code scopeKey}. Close the returned handle when the run ends.
     */
    public static Handle open(Object scopeKey) {
        if (scopeKey == null) {
            return () -> { };
        }
        synchronized (SCOPES) {
            SCOPES.computeIfAbsent(scopeKey, k -> new Scope()).references++;
        }
        return new Handle() {
            private boolean closed;

            @Override
            public void close() {
                synchronized (SCOPES) {
                    if (closed) {
                        return;
                    }
                    closed = true;
                    Scope scope = SCOPES.get(scopeKey);
                    if (scope != null && --scope.references == 0) {
                        SCOPES.remove(scopeKey);
                    }
                }
            }
        };
    }

    /**
     * Returns the cached value of {@code key} in the scope of {@code scopeKey}, computing it with {@code loader} on
     * first use. Without an open scope the loader is called on every invocation. {@code null} values are cached.
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(Object scopeKey, Object key, Supplier<T> loader) {
        Scope scope;
        synchronized (SCOPES) {
            scope = scopeKey == null ? null : SCOPES.get(scopeKey);
        }
        if (scope == null) {
            return loader.get();
        }
        CompletableFuture<Object> created = new CompletableFuture<>();
        CompletableFuture<Object> existing = scope.values.putIfAbsent(key, created);
        if (existing == null) {
            try {
                created.complete(loader.get());
            } catch (RuntimeException | Error e) {
                scope.values.remove(key, created);
                created.completeExceptionally(e);
                throw e;
            }
            return (T) created.join();
        }
        try {
            return (T) existing.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (e.getCause() instanceof Error error) {
                throw error;
            }
            throw e;
        }
    }

    /**
     * @return whether a scope is currently open for {@code scopeKey}.
     */
    public static boolean isOpen(Object scopeKey) {
        synchronized (SCOPES) {
            return scopeKey != null && SCOPES.containsKey(scopeKey);
        }
    }

    /**
     * Scope handle; {@link #close()} never throws and is idempotent.
     */
    public interface Handle extends AutoCloseable {
        @Override
        void close();
    }

    private static final class Scope {
        private final ConcurrentHashMap<Object, CompletableFuture<Object>> values = new ConcurrentHashMap<>();
        private int references;
    }
}
