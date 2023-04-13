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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class GeneratorIgnoreTest {
    static final String TMP_DIR_PREFIX = "generatorIgnoreTestTarget";
    Path tmpTargetDir;
    Path rootIgnoreFilePath;
    Path level1IgnoreFilePath;

    Path level2IgnoreFilePath;

    Path level3IgnoreFilePath;

    GeneratorIgnore generatorIgnore;

    @BeforeEach
    public void setUp() throws Exception {
        String newLine = System.getProperty("line.separator");
        tmpTargetDir = Files.createTempDirectory(Paths.get("target"), TMP_DIR_PREFIX);

        rootIgnoreFilePath = Paths.get(tmpTargetDir.toString(), GeneratorIgnore.GENERATOR_IGNORE_FILE);
        level1IgnoreFilePath = Paths.get(tmpTargetDir.toString(), "level1", GeneratorIgnore.GENERATOR_IGNORE_FILE);
        level2IgnoreFilePath = Paths.get(tmpTargetDir.toString(), "level1", "level2", GeneratorIgnore.GENERATOR_IGNORE_FILE);
        level3IgnoreFilePath = Paths.get(tmpTargetDir.toString(), "level1", "level2", "level3", GeneratorIgnore.GENERATOR_IGNORE_FILE);
        level3IgnoreFilePath.getParent().toFile().mkdirs();

        Files.write(rootIgnoreFilePath, String.join(newLine,
    "**/*.php", "app.yaml", "folder-contents-to-ignore/**", "test/*/testing.txt", "level1/level2/level3/ignoredFromRoot")
                .getBytes(StandardCharsets.UTF_8));

        Files.write(level1IgnoreFilePath, String.join(newLine,
    "level2/**/ignoredFromLevel1")
                .getBytes(StandardCharsets.UTF_8));

        Files.write(level2IgnoreFilePath, String.join(newLine,
    "level3/ignoredFromLevel2")
                .getBytes(StandardCharsets.UTF_8));

        Files.write(level3IgnoreFilePath, String.join(newLine,
    "ignoredFromLevel3")
                .getBytes(StandardCharsets.UTF_8));

        generatorIgnore = new GeneratorIgnore(tmpTargetDir);
    }

    @Test
    void testTargetTempFolderCreation() {
        assertThat(tmpTargetDir.toFile().getPath(), startsWith("target"));
    }

    @Test
    void testIgnoreFileCreation() {
        File fileWithAbsolutePath = rootIgnoreFilePath.toFile();

        assertTrue(fileWithAbsolutePath.exists());
    }

    @Test
    void testShouldExcludeExplicitFileInRoot() {
        Path path1 = absolutePathFor("app.yaml");
        Path path2 = absolutePathFor("lol.yaml");
        Path ignoredFromRoot = absolutePathFor("level1", "level2", "level3", "ignoredFromRoot");

        assertTrue(generatorIgnore.shouldExcludeFile(path1));
        assertFalse(generatorIgnore.shouldExcludeFile(path2));
        assertTrue(generatorIgnore.shouldExcludeFile(ignoredFromRoot));
    }

    @Test
    void testShouldExcludeFileInAnyLevel() {
        Path path1 = absolutePathFor("first", "second", "third", "theFile.php");
        Path path2 = absolutePathFor("first", "second", "third", "theFile.pdf");

        assertTrue(generatorIgnore.shouldExcludeFile(path1));
        assertFalse(generatorIgnore.shouldExcludeFile(path2));
    }

    @Test
    void testShouldExcludeFilesOneLevelDeep() {
        Path path1 = absolutePathFor("test", "one", "testing.txt");
        Path path2 = absolutePathFor("test", "two", "testing.txt");
        Path path3 = absolutePathFor("test", "two", "two-two", "testing.txt");

        assertTrue(generatorIgnore.shouldExcludeFile(path1));
        assertTrue(generatorIgnore.shouldExcludeFile(path2));
        assertFalse(generatorIgnore.shouldExcludeFile(path3));
    }

    @Test
    void testShouldExcludeAllInFolder() {
        Path path1 = absolutePathFor("folder-contents-to-ignore", "one", "testing.txt");
        Path path2 = absolutePathFor("folder-contents-to-ignore", "testing.txt");
        Path path3 = absolutePathFor("folder-contents-to-ignore");
        Path path4 = absolutePathFor("test", "testing.txt");

        assertTrue(generatorIgnore.shouldExcludeFile(path1));
        assertTrue(generatorIgnore.shouldExcludeFile(path2));
        assertFalse(generatorIgnore.shouldExcludeFile(path3)); // only contents!
        assertFalse(generatorIgnore.shouldExcludeFile(path4));
    }

    @Test
    void testShouldExcludeFilesFromLevel1() {
        Path path1 = absolutePathFor("level1", "level2", "level3", "ignoredFromLevel1");
        Path path2 = absolutePathFor("level1", "level2", "ignoredFromLevel1");
        Path path3 = absolutePathFor("level1", "ignoredFromLevel1");

        assertTrue(generatorIgnore.shouldExcludeFile(path1));
        assertFalse(generatorIgnore.shouldExcludeFile(path2));
        assertFalse(generatorIgnore.shouldExcludeFile(path3));
    }

    @Test
    void testShouldExcludeFilesFromLevel2() {
        Path path1 = absolutePathFor("level1", "level2", "level3", "ignoredFromLevel2");
        Path path2 = absolutePathFor("level1", "level2", "ignoredFromLevel2");

        assertTrue(generatorIgnore.shouldExcludeFile(path1));
        assertFalse(generatorIgnore.shouldExcludeFile(path2));
    }

    @Test
    void testShouldExcludeFilesFromLevel3() {
        Path path1 = absolutePathFor("level1", "level2", "level3", "ignoredFromLevel3");

        assertTrue(generatorIgnore.shouldExcludeFile(path1));
    }

    Path absolutePathFor(String... relativePath) {
        return Paths.get(tmpTargetDir.toString(), relativePath);
    }
}
