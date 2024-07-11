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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * It represents the generation ignore. This is used to keep files, which cannot be overwrite within
 * a generation. The format is same as .gitignore
 */
@Slf4j
public class GitIgnoreSynchronizer {
    public static final String GIT_IGNORE = ".gitignore";
    private static final String separator = System.getProperty("file.separator");

    public static final String GENEARATOR_AREA_START = "# JUDO GENERATOR BLOCK START";
    public static final String GENEARATOR_AREA_END = "# JUDO GENERATOR BLOCK END";


    CacheLoader<Path, List<String>> loader = new CacheLoader<Path, List<String>>() {
        @Override
        public List<String> load(Path key) {
            if (key.resolve(GIT_IGNORE).toFile().exists()) {
                try {
                    List<String> gitignoreLines = Files.readAllLines(key.resolve(GitIgnoreSynchronizer.GIT_IGNORE), StandardCharsets.UTF_8);
                    return gitignoreLines;
                } catch (IOException e) {
                    throw new RuntimeException("Could not read file: " + key.resolve(GIT_IGNORE).toFile().getAbsolutePath(), e);
                }
            }
            return Collections.emptyList();
        }
    };

    LoadingCache<Path, List<String>> cache = CacheBuilder.newBuilder().maximumSize(200).build(loader);

    private final Path rootPath;

    public GitIgnoreSynchronizer(Path rootPath) {
        this.rootPath = rootPath.normalize();
    }


    public void addGeneratedFiles(Collection<GeneratorFileEntry> entries) {
        try {
            List<String> gitignoreLines = cache.get(rootPath);
            List<String> newGitignoreLines = new ArrayList<>();

            GeneratorIgnore generatorIgnore = new GeneratorIgnore(rootPath);

            if (entries.size() == 0) {
                return;
            }
            List<GeneratorFileEntry> filesToIgnore = entries.stream()
                    .filter(f -> !generatorIgnore.shouldExcludeFile(new File(rootPath.toFile(), f.getPath()).toPath()))
                    .toList();

            if (filesToIgnore.size() == 0) {
                return;
            }

            int readIndex = 0;
            // Read lines before start marker or all lines
            boolean startMarkerFound = false;
            while (readIndex < gitignoreLines.size() && !startMarkerFound) {
                if (gitignoreLines.get(readIndex).equals(GENEARATOR_AREA_START)) {
                    startMarkerFound = true;
                } else {
                    newGitignoreLines.add(gitignoreLines.get(readIndex));
                }
                readIndex++;
            }

            // If startMarker found ignoring lines until end marker or end of file
            if (startMarkerFound) {
                boolean endMarkerFound = false;
                while (readIndex < gitignoreLines.size() && !endMarkerFound) {
                    if (gitignoreLines.get(readIndex).equals(GENEARATOR_AREA_END)) {
                        endMarkerFound = true;
                    }
                    readIndex++;
                }
            }

            // Appending makrers and lines to be ignored
            newGitignoreLines.add(GENEARATOR_AREA_START);
            filesToIgnore.stream()
                    .forEach(entry -> {
                newGitignoreLines.add(entry.getPath());
            });
            newGitignoreLines.add(GENEARATOR_AREA_END);

            while (readIndex < gitignoreLines.size()) {
                newGitignoreLines.add(gitignoreLines.get(readIndex));
                readIndex++;
            }
            Files.write(rootPath.resolve(GIT_IGNORE), newGitignoreLines);

            // Invalidate cache
            cache.refresh(rootPath);
        } catch (IOException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
