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
    void testGeneratorModelDefaultsForBuilder() {
        // Test that GeneratorModel builder applies defaults
        GeneratorModel model = GeneratorModel.generatorModelBuilder().build();
        
        assertTrue(model.isNormalizeContent(), "normalizeContent should default to true");
        assertNotNull(model.getFileNormalizers(), "fileNormalizers should not be null");
        assertFalse(model.getFileNormalizers().isEmpty(), "fileNormalizers should default to auto preset");
        
        // Verify it contains expected extensions from auto preset
        List<String> allExtensions = model.getFileNormalizers()
            .stream()
            .flatMap(p -> p.getExtensions().stream())
            .toList();
        assertTrue(allExtensions.contains("java"), "Should contain java extension");
        assertTrue(allExtensions.contains("ts"), "Should contain ts extension");
        assertTrue(allExtensions.contains("xml"), "Should contain xml extension");
    }

    @Test
    void testGeneratorModelDefaultsForNoArgsConstructor() {
        // Test that no-args constructor (used by Jackson) also has defaults
        // This simulates what happens when Jackson deserializes without fileNormalizers in YAML
        GeneratorModel model = new GeneratorModel();
        
        // normalizeContent uses Boolean wrapper, null means "use default" (true)
        assertTrue(model.isNormalizeContent(), "normalizeContent should default to true when null");
        assertNotNull(model.getFileNormalizers(), "fileNormalizers should not be null");
        assertFalse(model.getFileNormalizers().isEmpty(), "fileNormalizers should default to auto preset");
    }

    @Test
    void testNormalizeContentExplicitFalse() {
        // Test that explicit false is respected
        GeneratorModel model = new GeneratorModel();
        model.setNormalizeContent(false);
        
        assertFalse(model.isNormalizeContent(), "normalizeContent should be false when explicitly set");
    }

    @Test
    void testNormalizeContentExplicitTrue() {
        // Test that explicit true works
        GeneratorModel model = new GeneratorModel();
        model.setNormalizeContent(true);
        
        assertTrue(model.isNormalizeContent(), "normalizeContent should be true when explicitly set");
    }

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
    void testTypeScriptQuoteNormalization() {
        FileTypeNormalizer normalizer = NormalizerPresets.getPreset("ts").iterator().next();

        // Single quotes should be normalized to double quotes
        assertEquals(
            "const x = \"hello\"",
            normalizer.normalize("const x = 'hello'")
        );

        // Double quotes should remain unchanged
        assertEquals(
            "const x = \"hello\"",
            normalizer.normalize("const x = \"hello\"")
        );

        // Escaped quotes inside single-quoted strings
        assertEquals(
            "const x = \"it\\'s fine\"",
            normalizer.normalize("const x = 'it\\'s fine'")
        );

        // Both quote styles normalize to same result
        assertEquals(
            normalizer.normalize("const x = 'hello'"),
            normalizer.normalize("const x = \"hello\"")
        );
    }

    @Test
    void testTypeScriptTrailingCommas() {
        FileTypeNormalizer normalizer = NormalizerPresets.getPreset("ts").iterator().next();

        // Trailing comma before } should be removed (along with whitespace between comma and bracket)
        assertEquals(
            "const obj = { a: 1}",
            normalizer.normalize("const obj = { a: 1,}")
        );
        assertEquals(
            "const obj = { a: 1}",
            normalizer.normalize("const obj = { a: 1, }")
        );

        // Trailing comma before ] should be removed
        assertEquals(
            "const arr = [1, 2, 3]",
            normalizer.normalize("const arr = [1, 2, 3,]")
        );

        // Trailing comma before ) should be removed
        assertEquals(
            "foo(a, b)",
            normalizer.normalize("foo(a, b,)")
        );

        // With and without trailing commas normalize to same result
        assertEquals(
            normalizer.normalize("const obj = { a: 1, b: 2}"),
            normalizer.normalize("const obj = { a: 1, b: 2, }")
        );
    }

    @Test
    void testTypeScriptSemicolonNormalization() {
        FileTypeNormalizer normalizer = NormalizerPresets.getPreset("ts").iterator().next();

        // Semicolons at end of lines should be removed
        assertEquals(
            "const x = 1\nconst y = 2",
            normalizer.normalize("const x = 1;\nconst y = 2;")
        );

        // With and without semicolons normalize to same result
        assertEquals(
            normalizer.normalize("const x = 1;\nconst y = 2;"),
            normalizer.normalize("const x = 1\nconst y = 2")
        );
    }

    @Test
    void testTypeScriptBiomeFormattingEquivalence() {
        FileTypeNormalizer normalizer = NormalizerPresets.getPreset("ts").iterator().next();

        // Quote style differences normalize to same result
        assertEquals(
            normalizer.normalize("const name = \"app\""),
            normalizer.normalize("const name = 'app'")
        );

        // Semicolons normalize to same result
        assertEquals(
            normalizer.normalize("const x = 1;"),
            normalizer.normalize("const x = 1")
        );

        // Trailing commas normalize to same result (inline)
        assertEquals(
            normalizer.normalize("const obj = {a: 1, b: 2}"),
            normalizer.normalize("const obj = {a: 1, b: 2,}")
        );

        // Combined Biome vs Prettier differences on single-line constructs
        String biome = "const x = \"hello\"";
        String prettier = "const x = 'hello';";
        assertEquals(
            normalizer.normalize(biome),
            normalizer.normalize(prettier)
        );
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
    void testJavaScriptQuoteNormalization() {
        FileTypeNormalizer normalizer = NormalizerPresets.getPreset("js").iterator().next();

        // Single quotes normalized to double quotes
        assertEquals(
            "const x = \"hello\"",
            normalizer.normalize("const x = 'hello'")
        );

        // Both styles normalize to same result
        assertEquals(
            normalizer.normalize("const x = 'hello'"),
            normalizer.normalize("const x = \"hello\"")
        );
    }

    @Test
    void testJavaScriptTrailingCommasAndSemicolons() {
        FileTypeNormalizer normalizer = NormalizerPresets.getPreset("js").iterator().next();

        // Inline trailing commas and semicolons normalize equivalently
        assertEquals(
            normalizer.normalize("const obj = {a: 1, b: 2,}"),
            normalizer.normalize("const obj = {a: 1, b: 2}")
        );

        // Semicolons at end of line are removed
        assertEquals(
            normalizer.normalize("const x = 1;"),
            normalizer.normalize("const x = 1")
        );

        // Combined: quotes + trailing comma + semicolons
        assertEquals(
            normalizer.normalize("const x = \"hello\";"),
            normalizer.normalize("const x = 'hello'")
        );
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

        // Test import removal (multiple newlines collapsed)
        String input = "import os\nfrom sys import path\n\ndef main(): pass";
        String expected = "\ndef main(): pass";
        assertEquals(expected, normalizer.normalize(input));
    }

    @Test
    void testXmlPreset() {
        Collection<FileTypeNormalizer> presets = NormalizerPresets.getPreset(
            "xml"
        );

        assertEquals(1, presets.size());
        FileTypeNormalizer normalizer = presets.iterator().next();

        assertTrue(normalizer.getExtensions().contains("xml"));
        assertTrue(normalizer.isRemoveDoubleSpaces());
        assertTrue(normalizer.isRemoveTabs());
        assertFalse(normalizer.isRemoveNewLines());

        // Test comment removal
        String inputWithComment = "<root><!-- comment --><child/></root>";
        String expectedNoComment = "<root><child/></root>";
        assertEquals(expectedNoComment, normalizer.normalize(inputWithComment));

        // Test XML declaration removal
        String inputWithDeclaration = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root/>";
        String expectedNoDeclaration = "<root/>";
        assertEquals(expectedNoDeclaration, normalizer.normalize(inputWithDeclaration));

        // Test whitespace normalization between tags (also strips leading/trailing)
        String inputWithWhitespace = "  <root>  \n  <child/>  \n  </root>  ";
        String expectedNormalized = "<root><child/></root>";
        assertEquals(expectedNormalized, normalizer.normalize(inputWithWhitespace));
    }

    @Test
    void testHtmlPreset() {
        Collection<FileTypeNormalizer> presets = NormalizerPresets.getPreset(
            "html"
        );

        assertEquals(1, presets.size());
        FileTypeNormalizer normalizer = presets.iterator().next();

        assertTrue(normalizer.getExtensions().contains("html"));
        assertTrue(normalizer.getExtensions().contains("htm"));
        assertTrue(normalizer.isRemoveDoubleSpaces());
        assertTrue(normalizer.isRemoveTabs());
        assertFalse(normalizer.isRemoveNewLines());

        // Test comment removal
        String inputWithComment = "<div><!-- comment --><span/></div>";
        String expectedNoComment = "<div><span/></div>";
        assertEquals(expectedNoComment, normalizer.normalize(inputWithComment));

        // Test whitespace normalization between tags (also strips leading/trailing)
        String inputWithWhitespace = "  <div>  \n  <span/>  \n  </div>  ";
        String expectedNormalized = "<div><span/></div>";
        assertEquals(expectedNormalized, normalizer.normalize(inputWithWhitespace));
    }

    @Test
    void testCssPreset() {
        Collection<FileTypeNormalizer> presets = NormalizerPresets.getPreset(
            "css"
        );

        assertEquals(1, presets.size());
        FileTypeNormalizer normalizer = presets.iterator().next();

        assertTrue(normalizer.getExtensions().contains("css"));
        assertTrue(normalizer.isRemoveDoubleSpaces());
        assertTrue(normalizer.isRemoveTabs());
        assertFalse(normalizer.isRemoveNewLines());

        // Test comment removal (double space after removal gets collapsed to single)
        String inputWithComment = ".class { /* comment */ color: red; }";
        String expectedNoComment = ".class { color: red; }";
        assertEquals(expectedNoComment, normalizer.normalize(inputWithComment));

        // Test multiline comment removal (multiple newlines collapsed to single)
        String inputWithMultilineComment = ".class {\n/* multi\nline\ncomment */\ncolor: red;\n}";
        String expectedNoMultilineComment = ".class {\ncolor: red;\n}";
        assertEquals(expectedNoMultilineComment, normalizer.normalize(inputWithMultilineComment));
    }

    @Test
    void testJsonPreset() {
        Collection<FileTypeNormalizer> presets = NormalizerPresets.getPreset(
            "json"
        );

        assertEquals(1, presets.size());
        FileTypeNormalizer normalizer = presets.iterator().next();

        assertTrue(normalizer.getExtensions().contains("json"));
        assertTrue(normalizer.isRemoveDoubleSpaces());
        assertTrue(normalizer.isRemoveTabs());
        assertTrue(normalizer.isRemoveNewLines()); // JSON removes newlines

        // Test whitespace normalization (all whitespace around structural chars removed)
        String inputFormatted = "{\n  \"key\": \"value\",\n  \"number\": 42\n}";
        String expectedMinified = "{\"key\":\"value\",\"number\":42}";
        assertEquals(expectedMinified, normalizer.normalize(inputFormatted));

        // Test with tabs
        String inputWithTabs = "{\t\"key\":\t\"value\"}";
        String expectedNoTabs = "{\"key\":\"value\"}";
        assertEquals(expectedNoTabs, normalizer.normalize(inputWithTabs));

        // Test pretty printed vs minified are equal after normalization
        String minified = "{\"name\":\"test\",\"values\":[1,2,3]}";
        String pretty = "{\n  \"name\": \"test\",\n  \"values\": [\n    1,\n    2,\n    3\n  ]\n}";
        assertEquals(normalizer.normalize(minified), normalizer.normalize(pretty));

        // Test trailing commas (JSONC/Biome compatibility)
        String withTrailingCommas = "{\n  \"name\": \"test\",\n  \"values\": [1, 2, 3,],\n}";
        assertEquals(normalizer.normalize(minified), normalizer.normalize(withTrailingCommas));
    }

    @Test
    void testNonePreset() {
        Collection<FileTypeNormalizer> presets = NormalizerPresets.getPreset(
            "none"
        );

        // None should return empty collection
        assertTrue(presets.isEmpty());
    }

    @Test
    void testAutoPreset() {
        Collection<FileTypeNormalizer> presets = NormalizerPresets.getPreset(
            "auto"
        );

        // Auto should return all presets (10 now: java, ts, js, rust, go, python, xml, html, css, json)
        assertTrue(presets.size() >= 10);

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
        assertTrue(allExtensions.contains("xml"));
        assertTrue(allExtensions.contains("html"));
        assertTrue(allExtensions.contains("htm"));
        assertTrue(allExtensions.contains("css"));
        assertTrue(allExtensions.contains("json"));
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
        assertTrue(presets.contains("xml"));
        assertTrue(presets.contains("html"));
        assertTrue(presets.contains("css"));
        assertTrue(presets.contains("json"));
        assertFalse(presets.contains("auto")); // auto is not in the list
        assertFalse(presets.contains("none")); // none is not in the list
    }

    @Test
    void testHasPreset() {
        assertTrue(NormalizerPresets.hasPreset("java"));
        assertTrue(NormalizerPresets.hasPreset("auto"));
        assertTrue(NormalizerPresets.hasPreset("none"));
        assertTrue(NormalizerPresets.hasPreset("xml"));
        assertTrue(NormalizerPresets.hasPreset("html"));
        assertTrue(NormalizerPresets.hasPreset("css"));
        assertTrue(NormalizerPresets.hasPreset("json"));
        assertTrue(NormalizerPresets.hasPreset("JAVA"));
        assertFalse(NormalizerPresets.hasPreset("unknown"));
        assertFalse(NormalizerPresets.hasPreset(null));
        assertFalse(NormalizerPresets.hasPreset(""));
    }
}
