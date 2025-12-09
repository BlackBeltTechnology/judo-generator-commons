package hu.blackbelt.judo.generator.commons;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
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

class NormalizerPresetsTest {

    @Test
    void testJavaPreset() {
        Collection<FileTypeNormalizer> presets = NormalizerPresets.getPreset(
            "java"
        );

        assertEquals(1, presets.size());
        FileTypeNormalizer normalizer = presets.iterator().next();

        assertTrue(normalizer.getExtensions().contains("java"));
        assertTrue(normalizer.isRemoveDoubleSpaces());
        assertTrue(normalizer.isRemoveTabs());
        assertFalse(normalizer.isRemoveNewLines());
        assertFalse(normalizer.getPatterns().isEmpty());

        // Test import removal - pattern removes import line content
        String input = "import java.util.List;\n\npublic class Foo {}";
        String expected = "\npublic class Foo {}";
        assertEquals(expected, normalizer.normalize(input));
    }

    @Test
    void testTypeScriptPreset() {
        Collection<FileTypeNormalizer> presets = NormalizerPresets.getPreset(
            "ts"
        );

        assertEquals(1, presets.size());
        FileTypeNormalizer normalizer = presets.iterator().next();

        assertTrue(normalizer.getExtensions().contains("ts"));
        assertTrue(normalizer.getExtensions().contains("tsx"));
        assertTrue(normalizer.isRemoveDoubleSpaces());
        assertTrue(normalizer.isRemoveTabs());
        assertFalse(normalizer.isRemoveNewLines());

        // Test import removal - pattern removes import line content
        String input =
            "import { Component } from '@angular/core';\n\nexport class Foo {}";
        String expected = "\nexport class Foo {}";
        assertEquals(expected, normalizer.normalize(input));
    }

    @Test
    void testJavaScriptPreset() {
        Collection<FileTypeNormalizer> presets = NormalizerPresets.getPreset(
            "js"
        );

        assertEquals(1, presets.size());
        FileTypeNormalizer normalizer = presets.iterator().next();

        assertTrue(normalizer.getExtensions().contains("js"));
        assertTrue(normalizer.getExtensions().contains("jsx"));
        assertTrue(normalizer.getExtensions().contains("mjs"));
        assertTrue(normalizer.getExtensions().contains("cjs"));
        assertTrue(normalizer.isRemoveDoubleSpaces());
        assertTrue(normalizer.isRemoveTabs());
    }

    @Test
    void testRustPreset() {
        Collection<FileTypeNormalizer> presets = NormalizerPresets.getPreset(
            "rust"
        );

        assertEquals(1, presets.size());
        FileTypeNormalizer normalizer = presets.iterator().next();

        assertTrue(normalizer.getExtensions().contains("rs"));
        assertTrue(normalizer.isRemoveDoubleSpaces());
        assertTrue(normalizer.isRemoveTabs());

        // Test use statement removal - pattern removes use line content
        String input = "use std::io;\n\nfn main() {}";
        String expected = "\nfn main() {}";
        assertEquals(expected, normalizer.normalize(input));
    }

    @Test
    void testGoPreset() {
        Collection<FileTypeNormalizer> presets = NormalizerPresets.getPreset(
            "go"
        );

        assertEquals(1, presets.size());
        FileTypeNormalizer normalizer = presets.iterator().next();

        assertTrue(normalizer.getExtensions().contains("go"));
        assertTrue(normalizer.isRemoveDoubleSpaces());
        assertTrue(normalizer.isRemoveTabs());
    }

    @Test
    void testPythonPreset() {
        Collection<FileTypeNormalizer> presets = NormalizerPresets.getPreset(
            "python"
        );

        assertEquals(1, presets.size());
        FileTypeNormalizer normalizer = presets.iterator().next();

        assertTrue(normalizer.getExtensions().contains("py"));
        assertTrue(normalizer.isRemoveDoubleSpaces());
        assertTrue(normalizer.isRemoveTabs());

        // Test import removal
        String input = "import os\nfrom sys import path\n\ndef main(): pass";
        String expected = "\n\n\ndef main(): pass";
        assertEquals(expected, normalizer.normalize(input));
    }

    @Test
    void testAutoPreset() {
        Collection<FileTypeNormalizer> presets = NormalizerPresets.getPreset(
            "auto"
        );

        // Auto should return all presets
        assertTrue(presets.size() >= 6);

        // Verify all language presets are included
        List<String> allExtensions = presets
            .stream()
            .flatMap(p -> p.getExtensions().stream())
            .toList();

        assertTrue(allExtensions.contains("java"));
        assertTrue(allExtensions.contains("ts"));
        assertTrue(allExtensions.contains("tsx"));
        assertTrue(allExtensions.contains("js"));
        assertTrue(allExtensions.contains("rs"));
        assertTrue(allExtensions.contains("go"));
        assertTrue(allExtensions.contains("py"));
    }

    @Test
    void testPresetCaseInsensitive() {
        assertDoesNotThrow(() -> NormalizerPresets.getPreset("JAVA"));
        assertDoesNotThrow(() -> NormalizerPresets.getPreset("Java"));
        assertDoesNotThrow(() -> NormalizerPresets.getPreset("AUTO"));
    }

    @Test
    void testUnknownPresetThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
            NormalizerPresets.getPreset("unknown")
        );
    }

    @Test
    void testNullPresetThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
            NormalizerPresets.getPreset(null)
        );
    }

    @Test
    void testEmptyPresetThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
            NormalizerPresets.getPreset("")
        );
    }

    @Test
    void testGetAvailablePresets() {
        List<String> presets = NormalizerPresets.getAvailablePresets();

        assertTrue(presets.contains("java"));
        assertTrue(presets.contains("ts"));
        assertTrue(presets.contains("js"));
        assertTrue(presets.contains("rust"));
        assertTrue(presets.contains("go"));
        assertTrue(presets.contains("python"));
        assertFalse(presets.contains("auto")); // auto is not in the list
    }

    @Test
    void testHasPreset() {
        assertTrue(NormalizerPresets.hasPreset("java"));
        assertTrue(NormalizerPresets.hasPreset("auto"));
        assertTrue(NormalizerPresets.hasPreset("JAVA"));
        assertFalse(NormalizerPresets.hasPreset("unknown"));
        assertFalse(NormalizerPresets.hasPreset(null));
        assertFalse(NormalizerPresets.hasPreset(""));
    }
}
