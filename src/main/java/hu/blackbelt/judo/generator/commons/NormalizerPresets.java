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

import java.util.*;

/**
 * Built-in normalizer presets for common programming languages.
 * Presets provide sensible defaults for whitespace normalization and import stripping.
 */
public final class NormalizerPresets {

    private static final Map<String, FileTypeNormalizer> PRESETS = new HashMap<>();

    static {
        // Java preset
        PRESETS.put("java", FileTypeNormalizer.fileTypeNormalizerBuilder()
                .extensions(List.of("java"))
                .removeDoubleSpaces(true)
                .removeTabs(true)
                .removeNewLines(false)
                .patterns(List.of(
                        NormalizerPattern.normalizerPatternBuilder()
                                .pattern("^import\\s+.*?;\\s*$")
                                .replacement("")
                                .flags(List.of("MULTILINE"))
                                .build()
                ))
                .build());

        // TypeScript preset
        PRESETS.put("ts", FileTypeNormalizer.fileTypeNormalizerBuilder()
                .extensions(List.of("ts", "tsx"))
                .removeDoubleSpaces(true)
                .removeTabs(true)
                .removeNewLines(false)
                .patterns(List.of(
                        NormalizerPattern.normalizerPatternBuilder()
                                .pattern("^import\\s+.*?['\"];?\\s*$")
                                .replacement("")
                                .flags(List.of("MULTILINE"))
                                .build()
                ))
                .build());

        // JavaScript preset
        PRESETS.put("js", FileTypeNormalizer.fileTypeNormalizerBuilder()
                .extensions(List.of("js", "jsx", "mjs", "cjs"))
                .removeDoubleSpaces(true)
                .removeTabs(true)
                .removeNewLines(false)
                .patterns(List.of(
                        NormalizerPattern.normalizerPatternBuilder()
                                .pattern("^import\\s+.*?['\"];?\\s*$")
                                .replacement("")
                                .flags(List.of("MULTILINE"))
                                .build()
                ))
                .build());

        // Rust preset
        PRESETS.put("rust", FileTypeNormalizer.fileTypeNormalizerBuilder()
                .extensions(List.of("rs"))
                .removeDoubleSpaces(true)
                .removeTabs(true)
                .removeNewLines(false)
                .patterns(List.of(
                        NormalizerPattern.normalizerPatternBuilder()
                                .pattern("^use\\s+.*?;\\s*$")
                                .replacement("")
                                .flags(List.of("MULTILINE"))
                                .build()
                ))
                .build());

        // Go preset
        PRESETS.put("go", FileTypeNormalizer.fileTypeNormalizerBuilder()
                .extensions(List.of("go"))
                .removeDoubleSpaces(true)
                .removeTabs(true)
                .removeNewLines(false)
                .patterns(List.of(
                        // Single line imports
                        NormalizerPattern.normalizerPatternBuilder()
                                .pattern("^import\\s+\"[^\"]+\"\\s*$")
                                .replacement("")
                                .flags(List.of("MULTILINE"))
                                .build(),
                        // Multi-line import blocks
                        NormalizerPattern.normalizerPatternBuilder()
                                .pattern("^import\\s*\\([^)]*\\)\\s*$")
                                .replacement("")
                                .flags(List.of("MULTILINE", "DOTALL"))
                                .build()
                ))
                .build());

        // Python preset
        PRESETS.put("python", FileTypeNormalizer.fileTypeNormalizerBuilder()
                .extensions(List.of("py"))
                .removeDoubleSpaces(true)
                .removeTabs(true)
                .removeNewLines(false)
                .patterns(List.of(
                        NormalizerPattern.normalizerPatternBuilder()
                                .pattern("^import\\s+.*$")
                                .replacement("")
                                .flags(List.of("MULTILINE"))
                                .build(),
                        NormalizerPattern.normalizerPatternBuilder()
                                .pattern("^from\\s+.*import.*$")
                                .replacement("")
                                .flags(List.of("MULTILINE"))
                                .build()
                ))
                .build());
    }

    private NormalizerPresets() {
        // Utility class
    }

    /**
     * Gets a preset by name.
     * The "auto" preset returns all language presets.
     *
     * @param presetName the preset name (e.g., "java", "ts", "auto")
     * @return a collection of normalizers for the preset
     * @throws IllegalArgumentException if the preset name is unknown
     */
    public static Collection<FileTypeNormalizer> getPreset(String presetName) {
        if (presetName == null || presetName.isEmpty()) {
            throw new IllegalArgumentException("Preset name cannot be null or empty");
        }

        String name = presetName.toLowerCase();

        if ("auto".equals(name)) {
            // Return all presets
            return new ArrayList<>(PRESETS.values());
        }

        FileTypeNormalizer preset = PRESETS.get(name);
        if (preset == null) {
            throw new IllegalArgumentException("Unknown preset: " + presetName +
                    ". Available presets: auto, " + String.join(", ", getAvailablePresets()));
        }

        return Collections.singletonList(preset);
    }

    /**
     * Gets a list of available preset names (excluding "auto").
     *
     * @return list of preset names
     */
    public static List<String> getAvailablePresets() {
        List<String> presets = new ArrayList<>(PRESETS.keySet());
        Collections.sort(presets);
        return presets;
    }

    /**
     * Checks if a preset exists.
     *
     * @param presetName the preset name
     * @return true if the preset exists (including "auto")
     */
    public static boolean hasPreset(String presetName) {
        if (presetName == null || presetName.isEmpty()) {
            return false;
        }
        String name = presetName.toLowerCase();
        return "auto".equals(name) || PRESETS.containsKey(name);
    }
}
