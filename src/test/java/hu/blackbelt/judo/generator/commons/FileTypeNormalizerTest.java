package hu.blackbelt.judo.generator.commons;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
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

class FileTypeNormalizerTest {

    @Test
    void testRemoveDoubleSpaces() {
        FileTypeNormalizer normalizer =
            FileTypeNormalizer.fileTypeNormalizerBuilder()
                .removeDoubleSpaces(true)
                .build();

        assertEquals("hello world", normalizer.normalize("hello    world"));
        assertEquals("a b c", normalizer.normalize("a  b   c"));
    }

    @Test
    void testRemoveTabs() {
        FileTypeNormalizer normalizer =
            FileTypeNormalizer.fileTypeNormalizerBuilder()
                .removeTabs(true)
                .build();

        assertEquals("helloworld", normalizer.normalize("hello\tworld"));
        assertEquals("abc", normalizer.normalize("a\tb\tc"));
    }

    @Test
    void testRemoveNewLines() {
        FileTypeNormalizer normalizer =
            FileTypeNormalizer.fileTypeNormalizerBuilder()
                .removeNewLines(true)
                .build();

        assertEquals("helloworld", normalizer.normalize("hello\nworld"));
        assertEquals("abc", normalizer.normalize("a\nb\nc"));
    }

    @Test
    void testCrlfNormalization() {
        FileTypeNormalizer normalizer =
            FileTypeNormalizer.fileTypeNormalizerBuilder().build();

        // CRLF should always be normalized to LF
        assertEquals("hello\nworld", normalizer.normalize("hello\r\nworld"));
    }

    @Test
    void testCombinedFlags() {
        FileTypeNormalizer normalizer =
            FileTypeNormalizer.fileTypeNormalizerBuilder()
                .removeDoubleSpaces(true)
                .removeTabs(true)
                .removeNewLines(true)
                .build();

        // After CRLF→LF normalization: "hello\t  world\n\ntest"
        // After tab removal: "hello  world\n\ntest"
        // After double space collapse: "hello world\n\ntest"
        // After newline removal: "hello worldtest"
        String input = "hello\t  world\n\r\ntest";
        assertEquals("hello worldtest", normalizer.normalize(input));
    }

    @Test
    void testNormalizationOrder() {
        // Order: CRLF→LF, tabs, double spaces, newlines, patterns
        FileTypeNormalizer normalizer =
            FileTypeNormalizer.fileTypeNormalizerBuilder()
                .removeTabs(true)
                .removeDoubleSpaces(true)
                .removeNewLines(true)
                .build();

        // Tab followed by space should become single space after tab removal and double space collapse
        String input = "a\t b";
        assertEquals("a b", normalizer.normalize(input));
    }

    @Test
    void testWithPatterns() {
        FileTypeNormalizer normalizer =
            FileTypeNormalizer.fileTypeNormalizerBuilder()
                .patterns(
                    List.of(
                        NormalizerPattern.normalizerPatternBuilder()
                            .pattern("TODO:.*$")
                            .replacement("")
                            .flags(List.of("MULTILINE"))
                            .build()
                    )
                )
                .build();

        String input = "code(); // TODO: fix this\nmore code();";
        String expected = "code(); // \nmore code();";

        assertEquals(expected, normalizer.normalize(input));
    }

    @Test
    void testWithBooleanFlagsAndPatterns() {
        FileTypeNormalizer normalizer =
            FileTypeNormalizer.fileTypeNormalizerBuilder()
                .removeDoubleSpaces(true)
                .removeTabs(true)
                .patterns(
                    List.of(
                        NormalizerPattern.normalizerPatternBuilder()
                            .pattern("^import\\s+.*?;\\s*$")
                            .replacement("")
                            .flags(List.of("MULTILINE"))
                            .build()
                    )
                )
                .build();

        // Pattern removes the import line, leaving: "\n\npublic class  \tFoo {}"
        // After tab removal: "\n\npublic class  Foo {}"
        // After double space collapse: "\n\npublic class Foo {}"
        // BUT: normalization order is tabs → double spaces → patterns
        // So: "import java.util.List;\n\npublic class  Foo {}" (tabs removed)
        // Then: "import java.util.List;\n\npublic class Foo {}" (double spaces removed)
        // Then pattern removes import: "\npublic class Foo {}" (pattern replaces whole match with empty)
        String input = "import java.util.List;\n\npublic class  \tFoo {}";
        String expected = "\npublic class Foo {}";

        assertEquals(expected, normalizer.normalize(input));
    }

    @Test
    void testNullContentReturnsNull() {
        FileTypeNormalizer normalizer =
            FileTypeNormalizer.fileTypeNormalizerBuilder()
                .removeDoubleSpaces(true)
                .build();

        assertNull(normalizer.normalize(null));
    }

    @Test
    void testDefaultFlagsAreFalse() {
        FileTypeNormalizer normalizer =
            FileTypeNormalizer.fileTypeNormalizerBuilder().build();

        // With default flags (all false), content should only have CRLF normalized
        String input = "hello\t  world\ntest";
        assertEquals("hello\t  world\ntest", normalizer.normalize(input));
    }

    @Test
    void testEmptyStringNormalization() {
        FileTypeNormalizer normalizer =
            FileTypeNormalizer.fileTypeNormalizerBuilder()
                .removeDoubleSpaces(true)
                .removeTabs(true)
                .removeNewLines(true)
                .build();

        assertEquals("", normalizer.normalize(""));
    }

    @Test
    void testMultiplePatternsAppliedInOrder() {
        FileTypeNormalizer normalizer =
            FileTypeNormalizer.fileTypeNormalizerBuilder()
                .patterns(
                    List.of(
                        NormalizerPattern.normalizerPatternBuilder()
                            .pattern("first")
                            .replacement("FIRST")
                            .build(),
                        NormalizerPattern.normalizerPatternBuilder()
                            .pattern("FIRST")
                            .replacement("FINAL")
                            .build()
                    )
                )
                .build();

        assertEquals("FINAL second", normalizer.normalize("first second"));
    }
}
