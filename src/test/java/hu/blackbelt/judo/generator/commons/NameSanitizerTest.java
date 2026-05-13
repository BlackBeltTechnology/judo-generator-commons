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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NameSanitizerTest {

    @Nested
    @DisplayName("toJavaIdentifier")
    class ToJavaIdentifier {
        @Test
        void nullInputReturnsNull() {
            assertNull(NameSanitizer.toJavaIdentifier(null));
        }

        @Test
        void emptyInputReturnsEmpty() {
            assertEquals("", NameSanitizer.toJavaIdentifier(""));
        }

        @Test
        void dashIsReplacedWithUnderscore() {
            assertEquals("my_app", NameSanitizer.toJavaIdentifier("my-app"));
        }

        @Test
        void leadingDigitIsPrefixedWithUnderscore() {
            assertEquals("_1abc", NameSanitizer.toJavaIdentifier("1abc"));
        }

        @Test
        void reservedWordIsSuffixedWithUnderscore() {
            assertEquals("class_", NameSanitizer.toJavaIdentifier("class"));
        }

        @Test
        void anotherReservedWord() {
            assertEquals("for_", NameSanitizer.toJavaIdentifier("for"));
        }

        @Test
        void multipleUnsafeCharsAreEachReplaced() {
            assertEquals("a_b_c_d", NameSanitizer.toJavaIdentifier("a.b-c d"));
        }

        @Test
        void underscoreAndAlphanumericArePreserved() {
            assertEquals("Already_Valid_1", NameSanitizer.toJavaIdentifier("Already_Valid_1"));
        }

        @Test
        void colonsAreReplaced() {
            // toJavaIdentifier does NOT split; callers use toJavaPackage for that.
            assertEquals("a__b", NameSanitizer.toJavaIdentifier("a::b"));
        }
    }

    @Nested
    @DisplayName("toJavaPackage")
    class ToJavaPackage {
        @Test
        void nullInputReturnsNull() {
            assertNull(NameSanitizer.toJavaPackage(null));
        }

        @Test
        void emptyInputReturnsEmpty() {
            assertEquals("", NameSanitizer.toJavaPackage(""));
        }

        @Test
        void twoSegmentQualifiedWithDashedLeaf() {
            assertEquals("acme.my_app", NameSanitizer.toJavaPackage("Acme::my-app"));
        }

        @Test
        void threeSegmentQualified() {
            assertEquals("my.sub.dash_here", NameSanitizer.toJavaPackage("MY::sub::dash-here"));
        }

        @Test
        void singleSegmentIsLowercased() {
            assertEquals("myapp", NameSanitizer.toJavaPackage("MyApp"));
        }

        @Test
        void reservedWordSegmentIsSuffixed() {
            assertEquals("acme.class_", NameSanitizer.toJavaPackage("acme::class"));
        }

        @Test
        void leadingDigitSegmentIsPrefixed() {
            assertEquals("acme._1abc", NameSanitizer.toJavaPackage("acme::1abc"));
        }
    }

    @Nested
    @DisplayName("toJsIdentifier")
    class ToJsIdentifier {
        @Test
        void nullInputReturnsNull() {
            assertNull(NameSanitizer.toJsIdentifier(null));
        }

        @Test
        void emptyInputReturnsEmpty() {
            assertEquals("", NameSanitizer.toJsIdentifier(""));
        }

        @Test
        void dashIsReplacedWithUnderscore() {
            assertEquals("my_app", NameSanitizer.toJsIdentifier("my-app"));
        }

        @Test
        void leadingDigitIsPrefixedWithUnderscore() {
            assertEquals("_1abc", NameSanitizer.toJsIdentifier("1abc"));
        }

        @Test
        void jsReservedWordIsSuffixedWithUnderscore() {
            assertEquals("function_", NameSanitizer.toJsIdentifier("function"));
        }

        @Test
        void anotherJsReservedWord() {
            assertEquals("const_", NameSanitizer.toJsIdentifier("const"));
        }
    }

    @Nested
    @DisplayName("toEnvVarName")
    class ToEnvVarName {
        @Test
        void nullInputReturnsNull() {
            assertNull(NameSanitizer.toEnvVarName(null));
        }

        @Test
        void emptyInputReturnsEmpty() {
            assertEquals("", NameSanitizer.toEnvVarName(""));
        }

        @Test
        void dashedNameBecomesUppercaseUnderscored() {
            assertEquals("MY_APP", NameSanitizer.toEnvVarName("my-app"));
        }

        @Test
        void qualifiedDashedNamePreservesSegmentBoundary() {
            assertEquals("ACME__MY_APP__V2", NameSanitizer.toEnvVarName("acme::my-app::v2"));
        }

        @Test
        void leadingDigitIsPrefixed() {
            assertEquals("_1ABC", NameSanitizer.toEnvVarName("1abc"));
        }
    }

    @Nested
    @DisplayName("toFileNameSafe")
    class ToFileNameSafe {
        @Test
        void nullInputReturnsNull() {
            assertNull(NameSanitizer.toFileNameSafe(null));
        }

        @Test
        void emptyInputReturnsEmpty() {
            assertEquals("", NameSanitizer.toFileNameSafe(""));
        }

        @Test
        void dashesArePreserved() {
            assertEquals("acme__my-app", NameSanitizer.toFileNameSafe("acme::my-app"));
        }

        @Test
        void forwardSlashIsReplaced() {
            assertEquals("with_slash", NameSanitizer.toFileNameSafe("with/slash"));
        }

        @Test
        void multipleUnsafeCharsAreEachReplaced() {
            assertEquals("a_b_c_d", NameSanitizer.toFileNameSafe("a*b?c|d"));
        }

        @Test
        void backslashIsReplaced() {
            assertEquals("a_b", NameSanitizer.toFileNameSafe("a\\b"));
        }

        @Test
        void controlCharIsReplaced() {
            assertEquals("a_b", NameSanitizer.toFileNameSafe("a\u0001b"));
        }
    }

    @Nested
    @DisplayName("class shape")
    class ClassShape {
        @Test
        void classIsFinal() {
            assertTrue(Modifier.isFinal(NameSanitizer.class.getModifiers()),
                    "NameSanitizer must be final");
        }

        @Test
        void noArgConstructorIsPrivate() throws Exception {
            Constructor<NameSanitizer> ctor = NameSanitizer.class.getDeclaredConstructor();
            assertTrue(Modifier.isPrivate(ctor.getModifiers()),
                    "NameSanitizer no-arg constructor must be private");
            ctor.setAccessible(true);
            // Instantiation must succeed reflectively (utility class is uninstantiable
            // by intent, but the private ctor itself is permitted).
            assertThrows(Exception.class, () -> {
                Constructor<NameSanitizer> publicCtor =
                        NameSanitizer.class.getConstructor();
                publicCtor.newInstance();
            });
        }
    }
}
