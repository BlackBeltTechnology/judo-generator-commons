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
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;

/**
 * Pins the Epsilon-compatible behaviour of {@link BufferedSlf4jLogger} (test-plan #TP-33).
 */
class BufferedSlf4jLoggerTest {

    /** slf4j logger that only records what is replayed into it and reports configurable enabled levels. */
    private static final class Capturing {
        final List<String> received = new ArrayList<>();
        final List<Throwable> throwables = new ArrayList<>();
        final Logger logger;

        Capturing(Set<LogLevel> enabled) {
            logger = (Logger) Proxy.newProxyInstance(
                    Capturing.class.getClassLoader(), new Class<?>[]{Logger.class}, (proxy, method, args) -> {
                        String name = method.getName();
                        switch (name) {
                            case "isTraceEnabled": return enabled.contains(LogLevel.TRACE);
                            case "isDebugEnabled": return enabled.contains(LogLevel.DEBUG);
                            case "isInfoEnabled": return enabled.contains(LogLevel.INFO);
                            case "isWarnEnabled": return enabled.contains(LogLevel.WARN);
                            case "isErrorEnabled": return enabled.contains(LogLevel.ERROR);
                            case "getName": return "capturing";
                            case "hashCode": return System.identityHashCode(proxy);
                            case "equals": return proxy == args[0];
                            case "toString": return "capturing";
                            default:
                                received.add(name.toUpperCase() + ":" + args[0]);
                                throwables.add((Throwable) args[1]);
                                return null;
                        }
                    });
        }
    }

    private static void logEverything(Logger l, Throwable t) {
        l.error("error %s", "a");
        l.error("error %s %s", "a", "b");
        l.error("error %s %s %s", "a", "b", "c");
        l.error("error throwable", t);
        l.warn("warn %s", "a");
        l.warn("warn %s %s", "a", "b");
        l.warn("warn %s %s %s", "a", "b", "c");
        l.warn("warn throwable", t);
        l.info("info %s", "a");
        l.info("info %s %s", "a", "b");
        l.info("info %s %s %s", "a", "b", "c");
        l.info("info throwable", t);
        l.info("info plain");
        l.debug("debug %s", "a");
        l.debug("debug %s %s", "a", "b");
        l.debug("debug %s %s %s", "a", "b", "c");
        l.debug("debug throwable", t);
        l.trace("trace %s", "a");
        l.trace("trace %s %s", "a", "b");
        l.trace("trace %s %s %s", "a", "b", "c");
        l.trace("trace throwable", t);
    }

    @Test
    void infoLoggerBuffersOnlyEnabledLevelsInOrderAndFlushesOnClose() throws IOException {
        Capturing target = new Capturing(EnumSet.of(LogLevel.ERROR, LogLevel.WARN, LogLevel.INFO));
        Throwable boom = new IllegalStateException("boom");
        BufferedSlf4jLogger buffered = new BufferedSlf4jLogger(target.logger);

        logEverything(buffered, boom);
        assertThat("nothing is forwarded before close", target.received, empty());

        buffered.close();
        assertThat(target.received, contains(
                "ERROR:error a",
                "ERROR:error a b",
                "ERROR:error a b c",
                "ERROR:error throwable",
                "WARN:warn a",
                "WARN:warn a b",
                "WARN:warn a b c",
                "WARN:warn throwable",
                "INFO:info a",
                "INFO:info a b",
                "INFO:info a b c",
                "INFO:info throwable",
                "INFO:info plain"));
        assertThat(target.throwables.get(3), sameInstance(boom));
        assertThat(target.throwables.get(7), sameInstance(boom));
        assertThat(target.throwables.get(11), sameInstance(boom));
        assertThat(target.throwables.get(0), equalTo(null));
    }

    @Test
    void traceLoggerBuffersEveryLevelInCallOrder() throws IOException {
        Capturing target = new Capturing(EnumSet.allOf(LogLevel.class));
        Throwable boom = new IllegalStateException("boom");
        BufferedSlf4jLogger buffered = new BufferedSlf4jLogger(target.logger);

        logEverything(buffered, boom);
        assertThat(target.received, empty());

        buffered.close();
        assertThat(target.received.size(), equalTo(21));
        assertThat(target.received.subList(13, 21), contains(
                "DEBUG:debug a",
                "DEBUG:debug a b",
                "DEBUG:debug a b c",
                "DEBUG:debug throwable",
                "TRACE:trace a",
                "TRACE:trace a b",
                "TRACE:trace a b c",
                "TRACE:trace throwable"));
        assertThat(target.throwables.get(16), sameInstance(boom));
        assertThat(target.throwables.get(20), sameInstance(boom));
    }

    @Test
    void secondCloseIsNoOp() throws IOException {
        Capturing target = new Capturing(EnumSet.of(LogLevel.ERROR, LogLevel.WARN, LogLevel.INFO));
        BufferedSlf4jLogger buffered = new BufferedSlf4jLogger(target.logger);

        buffered.close();
        assertThat("closing an empty logger forwards nothing", target.received, empty());

        buffered.info("once %s", "only");
        buffered.close();
        buffered.close();
        assertThat(target.received, contains("INFO:once only"));
    }

    @Test
    void levelIsDeterminedFromTheWrappedLogger() {
        assertThat(LogLevel.determinateLogLevel(new Capturing(EnumSet.allOf(LogLevel.class)).logger), equalTo(LogLevel.TRACE));
        assertThat(LogLevel.determinateLogLevel(new Capturing(EnumSet.of(LogLevel.ERROR, LogLevel.WARN, LogLevel.INFO, LogLevel.DEBUG)).logger), equalTo(LogLevel.DEBUG));
        assertThat(LogLevel.determinateLogLevel(new Capturing(EnumSet.of(LogLevel.ERROR, LogLevel.WARN, LogLevel.INFO)).logger), equalTo(LogLevel.INFO));
        assertThat(LogLevel.determinateLogLevel(new Capturing(EnumSet.of(LogLevel.ERROR, LogLevel.WARN)).logger), equalTo(LogLevel.WARN));
        assertThat(LogLevel.determinateLogLevel(new Capturing(EnumSet.of(LogLevel.ERROR)).logger), equalTo(LogLevel.ERROR));
        assertThat(LogLevel.determinateLogLevel(new Capturing(EnumSet.noneOf(LogLevel.class)).logger), equalTo(LogLevel.INFO));
    }

    @Test
    void explicitLevelBuffersOnlyThatLevelAndAbove() {
        Capturing target = new Capturing(EnumSet.allOf(LogLevel.class));
        BufferedSlf4jLogger buffered = new BufferedSlf4jLogger(LogLevel.WARN);

        buffered.error("e");
        buffered.warn("w");
        buffered.info("i");
        buffered.debug("d");
        buffered.trace("t");
        buffered.sendToLogger(target.logger);

        assertThat(target.received, contains("ERROR:e", "WARN:w"));
    }
}
