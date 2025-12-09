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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FileNormalizerRegistryTest {

    @Test
    void testEmptyRegistry() {
        FileNormalizerRegistry registry = new FileNormalizerRegistry();

        assertFalse(registry.hasNormalizers());
        assertTrue(registry.getNormalizer("java").isEmpty());
    }

    @Test
    void testRegisterAndRetrieve() {
        FileNormalizerRegistry registry = new FileNormalizerRegistry();
        FileTypeNormalizer normalizer = FileTypeNormalizer.fileTypeNormalizerBuilder()
                .extensions(List.of("java"))
                .removeDoubleSpaces(true)
                .build();

        registry.register(normalizer);

        assertTrue(registry.hasNormalizers());
        Optional<FileTypeNormalizer> retrieved = registry.getNormalizer("java");
        assertTrue(retrieved.isPresent());
        assertTrue(retrieved.get().isRemoveDoubleSpaces());
    }

    @Test
    void testMultipleExtensions() {
        FileNormalizerRegistry registry = new FileNormalizerRegistry();
        FileTypeNormalizer normalizer = FileTypeNormalizer.fileTypeNormalizerBuilder()
                .extensions(List.of("ts", "tsx"))
                .removeTabs(true)
                .build();

        registry.register(normalizer);

        assertTrue(registry.getNormalizer("ts").isPresent());
        assertTrue(registry.getNormalizer("tsx").isPresent());
        assertSame(registry.getNormalizer("ts").get(), registry.getNormalizer("tsx").get());
    }

    @Test
    void testExtensionWithDot() {
        FileNormalizerRegistry registry = new FileNormalizerRegistry();
        FileTypeNormalizer normalizer = FileTypeNormalizer.fileTypeNormalizerBuilder()
                .extensions(List.of(".java"))
                .build();

        registry.register(normalizer);

        assertTrue(registry.getNormalizer("java").isPresent());
        assertTrue(registry.getNormalizer(".java").isPresent());
    }

    @Test
    void testCaseInsensitiveExtension() {
        FileNormalizerRegistry registry = new FileNormalizerRegistry();
        FileTypeNormalizer normalizer = FileTypeNormalizer.fileTypeNormalizerBuilder()
                .extensions(List.of("JAVA"))
                .build();

        registry.register(normalizer);

        assertTrue(registry.getNormalizer("java").isPresent());
        assertTrue(registry.getNormalizer("JAVA").isPresent());
        assertTrue(registry.getNormalizer("Java").isPresent());
    }

    @Test
    void testGetNormalizerForFile() {
        FileNormalizerRegistry registry = new FileNormalizerRegistry();
        FileTypeNormalizer normalizer = FileTypeNormalizer.fileTypeNormalizerBuilder()
                .extensions(List.of("java"))
                .build();

        registry.register(normalizer);

        assertTrue(registry.getNormalizerForFile("src/main/java/Foo.java").isPresent());
        assertTrue(registry.getNormalizerForFile("Foo.java").isPresent());
        assertFalse(registry.getNormalizerForFile("Foo.txt").isPresent());
    }

    @Test
    void testGetNormalizerForFileWithPath() {
        FileNormalizerRegistry registry = new FileNormalizerRegistry();
        FileTypeNormalizer normalizer = FileTypeNormalizer.fileTypeNormalizerBuilder()
                .extensions(List.of("ts"))
                .build();

        registry.register(normalizer);

        assertTrue(registry.getNormalizerForFile("/home/user/project/src/component.ts").isPresent());
        assertTrue(registry.getNormalizerForFile("C:\\Users\\project\\src\\component.ts").isPresent());
    }

    @Test
    void testExtractExtension() {
        assertEquals("java", FileNormalizerRegistry.extractExtension("Foo.java"));
        assertEquals("ts", FileNormalizerRegistry.extractExtension("src/component.ts"));
        assertEquals("tsx", FileNormalizerRegistry.extractExtension("C:\\project\\App.tsx"));
        assertNull(FileNormalizerRegistry.extractExtension("Makefile"));
        assertNull(FileNormalizerRegistry.extractExtension(null));
        assertEquals("gitignore", FileNormalizerRegistry.extractExtension(".gitignore"));
    }

    @Test
    void testFromConfigsWithExplicitConfig() {
        FileTypeNormalizer config = FileTypeNormalizer.fileTypeNormalizerBuilder()
                .extensions(List.of("kt"))
                .removeDoubleSpaces(true)
                .build();

        FileNormalizerRegistry registry = FileNormalizerRegistry.fromConfigs(List.of(config));

        assertTrue(registry.getNormalizer("kt").isPresent());
    }

    @Test
    void testFromConfigsWithPreset() {
        FileTypeNormalizer config = FileTypeNormalizer.fileTypeNormalizerBuilder()
                .preset("java")
                .build();

        FileNormalizerRegistry registry = FileNormalizerRegistry.fromConfigs(List.of(config));

        assertTrue(registry.getNormalizer("java").isPresent());
    }

    @Test
    void testFromConfigsWithAutoPreset() {
        FileTypeNormalizer config = FileTypeNormalizer.fileTypeNormalizerBuilder()
                .preset("auto")
                .build();

        FileNormalizerRegistry registry = FileNormalizerRegistry.fromConfigs(List.of(config));

        // Auto preset should register all language presets
        assertTrue(registry.getNormalizer("java").isPresent());
        assertTrue(registry.getNormalizer("ts").isPresent());
        assertTrue(registry.getNormalizer("tsx").isPresent());
        assertTrue(registry.getNormalizer("js").isPresent());
        assertTrue(registry.getNormalizer("rs").isPresent());
        assertTrue(registry.getNormalizer("go").isPresent());
        assertTrue(registry.getNormalizer("py").isPresent());
    }

    @Test
    void testFromConfigsWithNullCollection() {
        FileNormalizerRegistry registry = FileNormalizerRegistry.fromConfigs(null);

        assertFalse(registry.hasNormalizers());
    }

    @Test
    void testFromConfigsWithMixedConfigs() {
        List<FileTypeNormalizer> configs = List.of(
                FileTypeNormalizer.fileTypeNormalizerBuilder()
                        .preset("java")
                        .build(),
                FileTypeNormalizer.fileTypeNormalizerBuilder()
                        .extensions(List.of("kt"))
                        .removeDoubleSpaces(true)
                        .removeTabs(true)
                        .build()
        );

        FileNormalizerRegistry registry = FileNormalizerRegistry.fromConfigs(configs);

        assertTrue(registry.getNormalizer("java").isPresent());
        assertTrue(registry.getNormalizer("kt").isPresent());
    }

    @Test
    void testNullExtensionReturnsEmpty() {
        FileNormalizerRegistry registry = new FileNormalizerRegistry();

        assertTrue(registry.getNormalizer(null).isEmpty());
    }
}
