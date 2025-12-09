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

import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for normalizing file content of specific file types.
 * Used for whitespace-tolerant file comparison during checksum validation.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(builderMethodName = "fileTypeNormalizerBuilder")
public class FileTypeNormalizer {

    /**
     * Preset name (e.g., "auto", "java", "ts", "js", "rust", "go", "python").
     * If set, the normalizer configuration is loaded from built-in presets.
     */
    private String preset;

    /**
     * File extensions this normalizer applies to (e.g., ["java", "kt"]).
     */
    @Builder.Default
    private List<String> extensions = new ArrayList<>();

    /**
     * If true, collapse multiple consecutive spaces to a single space.
     */
    @Builder.Default
    private boolean removeDoubleSpaces = false;

    /**
     * If true, remove tab characters (replace with empty string).
     */
    @Builder.Default
    private boolean removeTabs = false;

    /**
     * If true, remove newline characters.
     */
    @Builder.Default
    private boolean removeNewLines = false;

    /**
     * Custom regex patterns for language-specific normalization.
     */
    @Builder.Default
    private List<NormalizerPattern> patterns = new ArrayList<>();

    /**
     * Normalizes the given content according to this normalizer's configuration.
     *
     * Normalization order:
     * 1. Line ending normalization (CRLF → LF) - always applied first
     * 2. Boolean flag normalizers (tabs → double spaces → newlines)
     * 3. Custom regex patterns (in configured order)
     *
     * @param content the content to normalize
     * @return the normalized content
     */
    public String normalize(String content) {
        if (content == null) {
            return null;
        }

        String result = content;

        // 1. Always normalize line endings first (CRLF → LF)
        result = result.replace("\r\n", "\n");

        // 2. Apply boolean flag normalizers in order: tabs → double spaces → newlines
        if (removeTabs) {
            result = result.replace("\t", "");
        }
        if (removeDoubleSpaces) {
            result = result.replaceAll("  +", " ");
        }
        if (removeNewLines) {
            result = result.replace("\n", "");
        }

        // 3. Apply custom regex patterns in order
        if (patterns != null) {
            for (NormalizerPattern pattern : patterns) {
                result = pattern.apply(result);
            }
        }

        return result;
    }
}
