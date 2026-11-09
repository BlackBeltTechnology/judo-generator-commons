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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModelCacheTest {

    @Test
    void withoutScopeTheLoaderRunsEveryTime() {
        Object model = new Object();
        AtomicInteger calls = new AtomicInteger();
        ModelCache.get(model, "k", calls::incrementAndGet);
        ModelCache.get(model, "k", calls::incrementAndGet);
        assertThat(calls.get(), equalTo(2));
        assertThat(ModelCache.get(null, "k", () -> "v"), equalTo("v"));
    }

    @Test
    void insideScopeTheValueIsComputedOnceAndDroppedOnClose() {
        Object model = new Object();
        AtomicInteger calls = new AtomicInteger();
        try (ModelCache.Handle ignored = ModelCache.open(model)) {
            assertThat(ModelCache.get(model, "k", calls::incrementAndGet), equalTo(1));
            assertThat(ModelCache.get(model, "k", calls::incrementAndGet), equalTo(1));
            assertThat(ModelCache.get(model, "other", calls::incrementAndGet), equalTo(2));
            assertThat(ModelCache.get(new Object(), "k", () -> -1), equalTo(-1));
        }
        assertThat(ModelCache.isOpen(model), is(false));
        assertThat(ModelCache.get(model, "k", calls::incrementAndGet), equalTo(3));
    }

    @Test
    void scopesAreMatchedByIdentityNotEquals() {
        String model = new String("model");
        String equalModel = new String("model");
        try (ModelCache.Handle ignored = ModelCache.open(model)) {
            ModelCache.get(model, "k", () -> "cached");
            assertThat(ModelCache.isOpen(equalModel), is(false));
            assertThat(ModelCache.get(equalModel, "k", () -> "fresh"), equalTo("fresh"));
        }
    }

    @Test
    void nestedScopesShareTheCacheUntilTheLastOneCloses() {
        Object model = new Object();
        AtomicInteger calls = new AtomicInteger();
        ModelCache.Handle outer = ModelCache.open(model);
        ModelCache.Handle inner = ModelCache.open(model);
        ModelCache.get(model, "k", calls::incrementAndGet);
        inner.close();
        inner.close(); // idempotent: must not close the outer scope
        assertThat(ModelCache.get(model, "k", calls::incrementAndGet), equalTo(1));
        outer.close();
        assertThat(ModelCache.isOpen(model), is(false));
    }

    @Test
    void nullValuesAreCached() {
        Object model = new Object();
        AtomicInteger calls = new AtomicInteger();
        try (ModelCache.Handle ignored = ModelCache.open(model)) {
            assertThat(ModelCache.get(model, "k", () -> { calls.incrementAndGet(); return null; }), nullValue());
            assertThat(ModelCache.get(model, "k", () -> { calls.incrementAndGet(); return null; }), nullValue());
        }
        assertThat(calls.get(), equalTo(1));
    }

    @Test
    void aFailingLoaderIsNotCachedAndIsRetried() {
        Object model = new Object();
        try (ModelCache.Handle ignored = ModelCache.open(model)) {
            assertThrows(IllegalStateException.class,
                    () -> ModelCache.get(model, "k", () -> { throw new IllegalStateException("boom"); }));
            assertThat(ModelCache.get(model, "k", () -> "ok"), equalTo("ok"));
        }
    }

    @Test
    void aLoaderMayReadOtherKeys() {
        Object model = new Object();
        try (ModelCache.Handle ignored = ModelCache.open(model)) {
            int value = ModelCache.get(model, "outer", () -> ModelCache.get(model, "inner", () -> 20) + 1);
            assertThat(value, equalTo(21));
        }
    }

    @Test
    void parallelCallersComputeEachKeyOnce() throws Exception {
        Object model = new Object();
        AtomicInteger calls = new AtomicInteger();
        int threads = 16;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        try (ModelCache.Handle ignored = ModelCache.open(model)) {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < threads * 4; i++) {
                results.add(executor.submit(() -> {
                    start.await();
                    return ModelCache.get(model, "k", () -> {
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        return calls.incrementAndGet();
                    });
                }));
            }
            start.countDown();
            for (Future<Integer> result : results) {
                assertThat(result.get(10, TimeUnit.SECONDS), equalTo(1));
            }
        } finally {
            executor.shutdownNow();
        }
        assertThat(calls.get(), equalTo(1));
    }
}
