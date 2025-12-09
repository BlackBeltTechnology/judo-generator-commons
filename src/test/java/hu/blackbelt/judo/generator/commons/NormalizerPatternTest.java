package hu.blackbelt.judo.generator.commons;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.regex.Pattern;
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

class NormalizerPatternTest {

    @Test
    void testSimplePatternReplacement() {
        NormalizerPattern pattern = NormalizerPattern.normalizerPatternBuilder()
            .pattern("foo")
            .replacement("bar")
            .build();

        assertEquals("bar baz bar", pattern.apply("foo baz foo"));
    }

    @Test
    void testPatternRemoval() {
        NormalizerPattern pattern = NormalizerPattern.normalizerPatternBuilder()
            .pattern("remove-me")
            .replacement("")
            .build();

        assertEquals("keep  keep", pattern.apply("keep remove-me keep"));
    }

    @Test
    void testDefaultReplacementIsEmptyString() {
        NormalizerPattern pattern = NormalizerPattern.normalizerPatternBuilder()
            .pattern("remove")
            .build();

        assertEquals(" this", pattern.apply("remove this"));
    }

    @Test
    void testMultilineFlag() {
        NormalizerPattern pattern = NormalizerPattern.normalizerPatternBuilder()
            .pattern("^import.*$")
            .replacement("")
            .flags(List.of("MULTILINE"))
            .build();

        String input = "import java.util.List;\nclass Foo {}";
        String expected = "\nclass Foo {}";

        assertEquals(expected, pattern.apply(input));
    }

    @Test
    void testCaseInsensitiveFlag() {
        NormalizerPattern pattern = NormalizerPattern.normalizerPatternBuilder()
            .pattern("hello")
            .replacement("hi")
            .flags(List.of("CASE_INSENSITIVE"))
            .build();

        assertEquals("hi hi hi", pattern.apply("Hello HELLO hello"));
    }

    @Test
    void testDotallFlag() {
        NormalizerPattern pattern = NormalizerPattern.normalizerPatternBuilder()
            .pattern("start.*end")
            .replacement("REPLACED")
            .flags(List.of("DOTALL"))
            .build();

        String input = "start\nmiddle\nend";
        assertEquals("REPLACED", pattern.apply(input));
    }

    @Test
    void testMultipleFlags() {
        NormalizerPattern pattern = NormalizerPattern.normalizerPatternBuilder()
            .pattern("^HELLO$")
            .replacement("hi")
            .flags(List.of("MULTILINE", "CASE_INSENSITIVE"))
            .build();

        String input = "hello\nworld";
        assertEquals("hi\nworld", pattern.apply(input));
    }

    @Test
    void testNullContentReturnsNull() {
        NormalizerPattern pattern = NormalizerPattern.normalizerPatternBuilder()
            .pattern("test")
            .build();

        assertNull(pattern.apply(null));
    }

    @Test
    void testNullPatternReturnsOriginalContent() {
        NormalizerPattern pattern = NormalizerPattern.normalizerPatternBuilder()
            .pattern(null)
            .build();

        assertEquals("unchanged", pattern.apply("unchanged"));
    }

    @Test
    void testCompiledPatternIsCached() {
        NormalizerPattern pattern = NormalizerPattern.normalizerPatternBuilder()
            .pattern("test")
            .build();

        Pattern first = pattern.getCompiledPattern();
        Pattern second = pattern.getCompiledPattern();

        assertSame(first, second);
    }

    @Test
    void testJavaImportPattern() {
        NormalizerPattern pattern = NormalizerPattern.normalizerPatternBuilder()
            .pattern("^import\\s+.*?;\\s*$")
            .replacement("")
            .flags(List.of("MULTILINE"))
            .build();

        // Pattern removes import lines but newlines between them remain
        String input =
            "import java.util.List;\nimport java.util.Map;\n\npublic class Foo {}";
        String expected = "\n\npublic class Foo {}";

        assertEquals(expected, pattern.apply(input));
    }

    @Test
    void testTypeScriptImportPattern() {
        NormalizerPattern pattern = NormalizerPattern.normalizerPatternBuilder()
            .pattern("^import\\s+.*?['\"];?\\s*$")
            .replacement("")
            .flags(List.of("MULTILINE"))
            .build();

        // The pattern matches and removes lines starting with "import",
        // but the newlines after the lines remain
        String input =
            "import { Component } from '@angular/core';\nimport React from 'react';\n\nexport class Foo {}";
        String expected = "\n\nexport class Foo {}";

        assertEquals(expected, pattern.apply(input));
    }
}
