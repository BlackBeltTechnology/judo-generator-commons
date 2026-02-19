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
 * Registry for managing file type normalizers by file extension.
 * Supports built-in presets and custom normalizer configurations.
 */
public class FileNormalizerRegistry {

    private final Map<String, FileTypeNormalizer> normalizers = new HashMap<>();

    /**
     * Creates an empty registry.
     */
    public FileNormalizerRegistry() {
    }

    /**
     * Creates a registry from a collection of normalizer configurations.
     * Handles preset expansion automatically.
     *
     * @param normalizerConfigs the normalizer configurations
     * @return a configured registry
     */
    public static FileNormalizerRegistry fromConfigs(Collection<FileTypeNormalizer> normalizerConfigs) {
        FileNormalizerRegistry registry = new FileNormalizerRegistry();
        if (normalizerConfigs != null) {
            for (FileTypeNormalizer config : normalizerConfigs) {
                if (config.getPreset() != null && !config.getPreset().isEmpty()) {
                    // Expand preset
                    Collection<FileTypeNormalizer> presetNormalizers = NormalizerPresets.getPreset(config.getPreset());
                    for (FileTypeNormalizer presetNormalizer : presetNormalizers) {
                        registry.register(presetNormalizer);
                    }
                } else {
                    registry.register(config);
                }
            }
        }
        return registry;
    }

    /**
     * Registers a normalizer for all its configured extensions.
     *
     * @param normalizer the normalizer to register
     */
    public void register(FileTypeNormalizer normalizer) {
        if (normalizer != null && normalizer.getExtensions() != null) {
            for (String extension : normalizer.getExtensions()) {
                String ext = extension.toLowerCase();
                if (ext.startsWith(".")) {
                    ext = ext.substring(1);
                }
                normalizers.put(ext, normalizer);
            }
        }
    }

    /**
     * Gets the normalizer for a given file extension.
     *
     * @param extension the file extension (with or without leading dot)
     * @return an Optional containing the normalizer, or empty if none configured
     */
    public Optional<FileTypeNormalizer> getNormalizer(String extension) {
        if (extension == null) {
            return Optional.empty();
        }
        String ext = extension.toLowerCase();
        if (ext.startsWith(".")) {
            ext = ext.substring(1);
        }
        return Optional.ofNullable(normalizers.get(ext));
    }

    /**
     * Gets the normalizer for a file by extracting its extension from the path.
     *
     * @param filePath the file path
     * @return an Optional containing the normalizer, or empty if none configured
     */
    public Optional<FileTypeNormalizer> getNormalizerForFile(String filePath) {
        String extension = extractExtension(filePath);
        return getNormalizer(extension);
    }

    /**
     * Checks if this registry has any normalizers configured.
     *
     * @return true if normalizers are configured, false otherwise
     */
    public boolean hasNormalizers() {
        return !normalizers.isEmpty();
    }

    /**
     * Extracts the file extension from a file path.
     *
     * @param filePath the file path
     * @return the extension (without leading dot), or null if none
     */
    public static String extractExtension(String filePath) {
        if (filePath == null) {
            return null;
        }
        int lastDot = filePath.lastIndexOf('.');
        int lastSeparator = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        if (lastDot > lastSeparator && lastDot < filePath.length() - 1) {
            return filePath.substring(lastDot + 1).toLowerCase();
        }
        return null;
    }
}
