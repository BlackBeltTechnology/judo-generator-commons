package hu.blackbelt.judo.generator.commons;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContentComparatorTest {

    private FileNormalizerRegistry registry;
    private ContentComparator comparator;

    @BeforeEach
    void setUp() {
        registry = new FileNormalizerRegistry();
        registry.register(
            FileTypeNormalizer.fileTypeNormalizerBuilder()
                .extensions(List.of("java"))
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
                .build()
        );

        comparator = new ContentComparator(registry);
    }

    @Test
    void testCompareIdenticalContent() {
        byte[] content = "public class Foo {}".getBytes(StandardCharsets.UTF_8);

        assertTrue(comparator.compareNormalized("Foo.java", content, content));
    }

    @Test
    void testCompareWithWhitespaceDifferences() {
        byte[] contentA = "public class  Foo {}".getBytes(
            StandardCharsets.UTF_8
        );
        byte[] contentB = "public class Foo {}".getBytes(
            StandardCharsets.UTF_8
        );

        assertTrue(
            comparator.compareNormalized("Foo.java", contentA, contentB)
        );
    }

    @Test
    void testCompareWithTabDifferences() {
        byte[] contentA = "public class\tFoo {}".getBytes(
            StandardCharsets.UTF_8
        );
        byte[] contentB = "public classFoo {}".getBytes(StandardCharsets.UTF_8);

        assertTrue(
            comparator.compareNormalized("Foo.java", contentA, contentB)
        );
    }

    @Test
    void testCompareWithImportDifferences() {
        byte[] contentA =
            "import java.util.List;\n\npublic class Foo {}".getBytes(
                StandardCharsets.UTF_8
            );
        byte[] contentB =
            "import java.util.Map;\n\npublic class Foo {}".getBytes(
                StandardCharsets.UTF_8
            );

        // Both imports are removed, so the remaining content is the same
        assertTrue(
            comparator.compareNormalized("Foo.java", contentA, contentB)
        );
    }

    @Test
    void testCompareWithDifferentImportOrder() {
        byte[] contentA =
            "import java.util.List;\nimport java.util.Map;\n\npublic class Foo {}".getBytes(
                StandardCharsets.UTF_8
            );
        byte[] contentB =
            "import java.util.Map;\nimport java.util.List;\n\npublic class Foo {}".getBytes(
                StandardCharsets.UTF_8
            );

        assertTrue(
            comparator.compareNormalized("Foo.java", contentA, contentB)
        );
    }

    @Test
    void testCompareWithActualDifferences() {
        byte[] contentA = "public class Foo {}".getBytes(
            StandardCharsets.UTF_8
        );
        byte[] contentB = "public class Bar {}".getBytes(
            StandardCharsets.UTF_8
        );

        assertFalse(
            comparator.compareNormalized("Foo.java", contentA, contentB)
        );
    }

    @Test
    void testCompareWithNoNormalizerConfigured() {
        byte[] contentA = "public class  Foo {}".getBytes(
            StandardCharsets.UTF_8
        );
        byte[] contentB = "public class Foo {}".getBytes(
            StandardCharsets.UTF_8
        );

        // No normalizer for .txt files - should do exact comparison
        assertFalse(
            comparator.compareNormalized("Foo.txt", contentA, contentB)
        );
    }

    @Test
    void testCompareWithNullContent() {
        byte[] content = "test".getBytes(StandardCharsets.UTF_8);

        assertTrue(comparator.compareNormalized("Foo.java", null, null));
        assertFalse(comparator.compareNormalized("Foo.java", content, null));
        assertFalse(comparator.compareNormalized("Foo.java", null, content));
    }

    @Test
    void testNormalize() {
        byte[] content =
            "import java.util.List;\n\npublic class  Foo {}".getBytes(
                StandardCharsets.UTF_8
            );

        String normalized = comparator.normalize("Foo.java", content);

        // Pattern removes import line, tabs removed (none here), double spaces collapsed
        assertEquals("\npublic class Foo {}", normalized);
    }

    @Test
    void testNormalizeWithNoNormalizer() {
        byte[] content = "public class  Foo {}".getBytes(
            StandardCharsets.UTF_8
        );

        String normalized = comparator.normalize("Foo.txt", content);

        assertEquals("public class  Foo {}", normalized);
    }

    @Test
    void testHasNormalizer() {
        assertTrue(comparator.hasNormalizer("Foo.java"));
        assertFalse(comparator.hasNormalizer("Foo.txt"));
    }

    @Test
    void testCompareWithCrlfDifferences() {
        byte[] contentA = "line1\r\nline2".getBytes(StandardCharsets.UTF_8);
        byte[] contentB = "line1\nline2".getBytes(StandardCharsets.UTF_8);

        assertTrue(
            comparator.compareNormalized("Foo.java", contentA, contentB)
        );
    }

    @Test
    void testCompareEmptyContent() {
        byte[] empty = "".getBytes(StandardCharsets.UTF_8);

        assertTrue(comparator.compareNormalized("Foo.java", empty, empty));
    }

    @Test
    void testNormalizeNullContent() {
        assertNull(comparator.normalize("Foo.java", null));
    }
}
