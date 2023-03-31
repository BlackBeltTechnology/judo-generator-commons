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

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;

/**
 * It reperesents the generation ignore. This is used to keep files, which cannot be overwrite within
 * a generation. The format is same as .gitignore
 */
@Slf4j
public class GeneratorIgnore {
    public static final String GENERATOR_IGNORE_FILE = ".generator-ignore";
    private static final String separator = System.getProperty("file.separator");
    private List<String> globs = Collections.emptyList();
    private final Path targetPath;

    public GeneratorIgnore(Path targetPath) {
        this.targetPath = targetPath.normalize();
        try {
            globs = Files.readAllLines(Paths.get(targetPath.toString(), GeneratorIgnore.GENERATOR_IGNORE_FILE), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.info("Generator ignore file(" + GENERATOR_IGNORE_FILE + ") not found at target path(" + targetPath + "), all sources will be generated.");
        }
    }

    public List<String> getGlobs() {
        return globs;
    }

    public boolean shouldExcludeFile(Path absolutePath) {
        Path relativePath = Paths.get(absolutePath.normalize().toString().replace(targetPath.toString() + separator, ""));
        return globs.stream().anyMatch((glob) -> {
            final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);
            return pathMatcher.matches(relativePath);
        });
    }
}
