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

import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class ModelGeneratorTest {
    static final String TMP_DIR_PREFIX = "modelGeneratorTestTarget";
    Path tmpTargetDir;
    Collection<GeneratedFile> generatedFileCollecton;

    @BeforeEach
    public void setUp() throws Exception {
        String newLine = System.getProperty("line.separator");
        tmpTargetDir = Files.createTempDirectory(Paths.get("target"), TMP_DIR_PREFIX);

        generatedFileCollecton = ImmutableList.<GeneratedFile>builder()
                .add(GeneratedFile.builder().path("level1/file1").content("level1/file1".getBytes(StandardCharsets.UTF_8)).build())
                .add(GeneratedFile.builder().path("level1/file2").content("level1/file2".getBytes(StandardCharsets.UTF_8)).build())
                .add(GeneratedFile.builder().path("level1/level2/file3").content("level1/level2/file3".getBytes(StandardCharsets.UTF_8)).build())
                .build();
    }

    private void generateFiles() {

    }

    @Test
    void testFilesWritten() {
        ModelGenerator.writeDirectory(generatedFileCollecton, tmpTargetDir.toFile(), ModelGenerator.GENERATED_FILES);

        Path generatedFile = absolutePathFor(ModelGenerator.GENERATED_FILES);
        Path file1 = absolutePathFor("level1", "file1");
        Path file2 = absolutePathFor("level1", "file2");
        Path file3 = absolutePathFor("level1", "level2", "file3");

        assertTrue(generatedFile.toFile().exists());
        assertTrue(file1.toFile().exists());
        assertTrue(file2.toFile().exists());
        assertTrue(file3.toFile().exists());

        Collection<GeneratorFileEntry> entries = ModelGenerator.readGeneratedFiles(tmpTargetDir.toFile(), ModelGenerator.GENERATED_FILES);
        Collection<GeneratorFileEntry> fileSystemEntries = ModelGenerator.readFilesystemEntries(tmpTargetDir.toFile(), entries);

        assertEquals(3, entries.size());
        assertEquals(3, fileSystemEntries.size());

        assertThat(entries, equalTo(fileSystemEntries));
    }

    @Test
    void testGeneratedFileModification() throws IOException {
        ModelGenerator.writeDirectory(generatedFileCollecton, tmpTargetDir.toFile(), ModelGenerator.GENERATED_FILES);

        Path file1 = absolutePathFor("level1", "file1");
        Files.write(file1, "level1/file1Modified".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () ->
                ModelGenerator.writeDirectory(generatedFileCollecton, tmpTargetDir.toFile(), ModelGenerator.GENERATED_FILES));

        writeFile(tmpTargetDir.toFile(), GeneratorIgnore.GENERATOR_IGNORE_FILE, ImmutableList.of("level1/file1"));

        ModelGenerator.writeDirectory(generatedFileCollecton, tmpTargetDir.toFile(), ModelGenerator.GENERATED_FILES);

        absolutePathFor(GeneratorIgnore.GENERATOR_IGNORE_FILE).toFile().delete();

        assertThrows(IllegalStateException.class, () ->
                ModelGenerator.writeDirectory(generatedFileCollecton, tmpTargetDir.toFile(), ModelGenerator.GENERATED_FILES));

        file1.toFile().delete();

        ModelGenerator.writeDirectory(generatedFileCollecton, tmpTargetDir.toFile(), ModelGenerator.GENERATED_FILES);
    }

    @Test
    void testGeneratedFileChanges() throws IOException {
        ModelGenerator.writeDirectory(generatedFileCollecton, tmpTargetDir.toFile(), ModelGenerator.GENERATED_FILES);

        Map<String, GeneratedFile> generatedFileMap = generatedFileCollecton.stream()
                .collect(Collectors.toMap(GeneratedFile::getPath, v -> v));

        generatedFileMap.get("level1/file1").setContent("level1/file1Modified".getBytes(StandardCharsets.UTF_8));
        Path file1 = absolutePathFor("level1", "file1");

        ModelGenerator.writeDirectory(generatedFileCollecton, tmpTargetDir.toFile(), ModelGenerator.GENERATED_FILES);

        assertEquals("level1/file1Modified", Files.readAllLines(file1).get(0));

        Collection<GeneratorFileEntry> entries = ModelGenerator.readGeneratedFiles(tmpTargetDir.toFile(), ModelGenerator.GENERATED_FILES);
        Collection<GeneratorFileEntry> fileSystemEntries = ModelGenerator.readFilesystemEntries(tmpTargetDir.toFile(), entries);

        assertEquals(3, entries.size());
        assertEquals(3, fileSystemEntries.size());

        assertThat(entries, equalTo(fileSystemEntries));
    }

    @Test
    void testGeneratedFileRemove() {
        ModelGenerator.writeDirectory(generatedFileCollecton, tmpTargetDir.toFile(), ModelGenerator.GENERATED_FILES);

        ModelGenerator.writeDirectory(generatedFileCollecton.stream()
                .filter(f -> !f.getPath().equals("level1/file1")).collect(Collectors.toList()), tmpTargetDir.toFile(),
                ModelGenerator.GENERATED_FILES);

        Path generatedFile = absolutePathFor(ModelGenerator.GENERATED_FILES);
        Path file1 = absolutePathFor("level1", "file1");
        Path file2 = absolutePathFor("level1", "file2");
        Path file3 = absolutePathFor("level1", "level2", "file3");

        assertTrue(generatedFile.toFile().exists());
        assertFalse(file1.toFile().exists());
        assertTrue(file2.toFile().exists());
        assertTrue(file3.toFile().exists());

        Collection<GeneratorFileEntry> entries = ModelGenerator.readGeneratedFiles(tmpTargetDir.toFile(), ModelGenerator.GENERATED_FILES);
        Collection<GeneratorFileEntry> fileSystemEntries = ModelGenerator.readFilesystemEntries(tmpTargetDir.toFile(), entries);

        assertEquals(2, entries.size());
        assertEquals(2, fileSystemEntries.size());

        assertThat(entries, equalTo(fileSystemEntries));
    }


    @Test
    void testIgnore() throws IOException {
        writeFile(tmpTargetDir.toFile(), GeneratorIgnore.GENERATOR_IGNORE_FILE, ImmutableList.of("level1/file1"));

        ModelGenerator.writeDirectory(generatedFileCollecton, tmpTargetDir.toFile(), ModelGenerator.GENERATED_FILES);

        Path generatedFile = absolutePathFor(ModelGenerator.GENERATED_FILES);
        Path file1 = absolutePathFor("level1", "file1");
        Path file2 = absolutePathFor("level1", "file2");
        Path file3 = absolutePathFor("level1", "level2", "file3");

        assertTrue(generatedFile.toFile().exists());
        assertFalse(file1.toFile().exists());
        assertTrue(file2.toFile().exists());
        assertTrue(file3.toFile().exists());

        Collection<GeneratorFileEntry> entries = ModelGenerator.readGeneratedFiles(tmpTargetDir.toFile(), ModelGenerator.GENERATED_FILES);
        Collection<GeneratorFileEntry> fileSystemEntries = ModelGenerator.readFilesystemEntries(tmpTargetDir.toFile(), entries);

        assertEquals(3, entries.size());
        assertEquals(2, fileSystemEntries.size());
    }

    Path absolutePathFor(String... relativePath) {
        return Paths.get(tmpTargetDir.toString(), relativePath);
    }

    void writeFile(File targetDirectory, String generatedFileName, List<String> lines) throws IOException {
        Files.write(Paths.get(targetDirectory.getAbsolutePath(), generatedFileName),
                String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
    }

}
