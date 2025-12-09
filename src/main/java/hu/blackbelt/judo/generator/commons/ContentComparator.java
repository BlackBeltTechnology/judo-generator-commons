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

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Utility for comparing file content with normalization support.
 * Used for whitespace-tolerant file comparison during checksum validation.
 */
public class ContentComparator {

    private final FileNormalizerRegistry registry;

    /**
     * Creates a content comparator with the given registry.
     *
     * @param registry the normalizer registry
     */
    public ContentComparator(FileNormalizerRegistry registry) {
        this.registry = registry;
    }

    /**
     * Compares two byte arrays after normalizing their content.
     *
     * @param filePath the file path (used to determine the normalizer)
     * @param contentA first content
     * @param contentB second content
     * @return true if normalized content is equal, false otherwise
     */
    public boolean compareNormalized(String filePath, byte[] contentA, byte[] contentB) {
        if (contentA == null && contentB == null) {
            return true;
        }
        if (contentA == null || contentB == null) {
            return false;
        }

        Optional<FileTypeNormalizer> normalizerOpt = registry.getNormalizerForFile(filePath);
        if (normalizerOpt.isEmpty()) {
            // No normalizer configured - fall back to exact comparison
            return java.util.Arrays.equals(contentA, contentB);
        }

        FileTypeNormalizer normalizer = normalizerOpt.get();
        String normalizedA = normalize(normalizer, contentA);
        String normalizedB = normalize(normalizer, contentB);

        return normalizedA.equals(normalizedB);
    }

    /**
     * Normalizes content using the appropriate normalizer for the file type.
     *
     * @param filePath the file path (used to determine the normalizer)
     * @param content the content to normalize
     * @return the normalized content as a string
     */
    public String normalize(String filePath, byte[] content) {
        if (content == null) {
            return null;
        }

        Optional<FileTypeNormalizer> normalizerOpt = registry.getNormalizerForFile(filePath);
        if (normalizerOpt.isEmpty()) {
            return new String(content, StandardCharsets.UTF_8);
        }

        return normalize(normalizerOpt.get(), content);
    }

    /**
     * Normalizes content using the given normalizer.
     *
     * @param normalizer the normalizer to use
     * @param content the content to normalize
     * @return the normalized content as a string
     */
    private String normalize(FileTypeNormalizer normalizer, byte[] content) {
        String contentStr = new String(content, StandardCharsets.UTF_8);
        return normalizer.normalize(contentStr);
    }

    /**
     * Checks if a normalizer is available for the given file path.
     *
     * @param filePath the file path
     * @return true if a normalizer is configured, false otherwise
     */
    public boolean hasNormalizer(String filePath) {
        return registry.getNormalizerForFile(filePath).isPresent();
    }
}
