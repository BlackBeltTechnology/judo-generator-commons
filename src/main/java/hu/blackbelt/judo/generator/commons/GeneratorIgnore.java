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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * It represents the generation ignore. This is used to keep files, which cannot be overwrite within
 * a generation. The format is same as .gitignore
 */
@Slf4j
public class GeneratorIgnore {
    public static final String GENERATOR_IGNORE_FILE = ".generator-ignore";
    private static final String separator = System.getProperty("file.separator");

    CacheLoader<Path, List<String>> loader = new CacheLoader<Path, List<String>>() {
        @Override
        public List<String> load(Path key) {
            if (key.resolve(GENERATOR_IGNORE_FILE).toFile().exists()) {
                try {
                    List<String> globs = Files.readAllLines(key.resolve(GeneratorIgnore.GENERATOR_IGNORE_FILE), StandardCharsets.UTF_8);
                    return globs;
                } catch (IOException e) {
                    throw new RuntimeException("Could not read file: " + key.resolve(GENERATOR_IGNORE_FILE).toFile().getAbsolutePath(), e);
                }
            }
            return Collections.emptyList();
        }
    };

    LoadingCache<Path, List<String>> cache = CacheBuilder.newBuilder().maximumSize(200).build(loader);

    private final Path rootPath;

    public GeneratorIgnore(Path rootPath) {
        this.rootPath = rootPath.normalize();
    }

    public boolean shouldExcludeFile(Path absolutePath) {
        if (!isChildPath(rootPath, absolutePath)) {
            throw new IllegalArgumentException(absolutePath.toFile().getAbsolutePath() + " is not part of " + rootPath.toFile().getAbsolutePath());
        }
        Path currentPath = absolutePath.getParent();
        boolean match = false;
        while (!match && isChildPath(rootPath, currentPath)) {
            try {
                List<String> globs = cache.get(currentPath);
                Path relativePath = Paths.get(absolutePath.normalize().toString().replace(currentPath.toString() + separator, ""));
                match = globs.stream().anyMatch((glob) -> {
                    final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);
                    return pathMatcher.matches(relativePath);
                });
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
            currentPath = currentPath.getParent();
        }
        return match;
    }


    private boolean isChildPath(Path possibleParent, Path maybeChild) {
        File fileToTest = maybeChild.toFile();
        while (fileToTest != null) {
            if (fileToTest.equals(possibleParent.toFile())) {
                return true;
            }
            fileToTest = fileToTest.getParentFile();
        }
        return false;
    }
}
