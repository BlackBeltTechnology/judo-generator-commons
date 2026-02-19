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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the whitespace-tolerant file comparison feature.
 */
@Slf4j
public class NormalizedComparisonTest {

    static final String TMP_DIR_PREFIX = "normalizedComparisonTestTarget";
    Path tmpTargetDir;
    FileNormalizerRegistry registry;

    @BeforeEach
    public void setUp() throws Exception {
        tmpTargetDir = Files.createTempDirectory(Paths.get("target"), TMP_DIR_PREFIX);

        // Set up registry with Java normalizer
        registry = FileNormalizerRegistry.fromConfigs(List.of(
                FileTypeNormalizer.fileTypeNormalizerBuilder()
                        .preset("java")
                        .build()
        ));
    }

    @Test
    void testChecksumMismatchWithEquivalentNormalizedContent() throws IOException {
        // Initial content
        String originalContent = "import java.util.List;\n\npublic class Foo {}";

        Collection<GeneratedFile> generatedFiles = ImmutableList.<GeneratedFile>builder()
                .add(GeneratedFile.builder()
                        .path("Foo.java")
                        .content(originalContent.getBytes(StandardCharsets.UTF_8))
                        .build())
                .build();

        // Write files initially
        ModelGenerator.writeDirectory(generatedFiles, tmpTargetDir.toFile(),
                ModelGenerator.GENERATED_FILES, true, registry);

        Path javaFile = tmpTargetDir.resolve("Foo.java");
        assertTrue(javaFile.toFile().exists());

        // Simulate formatter modifying the file (different import, more whitespace)
        String formattedContent = "import java.util.Map;\n\npublic class  Foo {}";
        Files.write(javaFile, formattedContent.getBytes(StandardCharsets.UTF_8));

        // Without normalizer registry, this should throw
        assertThrows(IllegalStateException.class, () ->
                ModelGenerator.writeDirectory(generatedFiles, tmpTargetDir.toFile(),
                        ModelGenerator.GENERATED_FILES, true, null));

        // With normalizer registry, it should pass because normalized content matches
        assertDoesNotThrow(() ->
                ModelGenerator.writeDirectory(generatedFiles, tmpTargetDir.toFile(),
                        ModelGenerator.GENERATED_FILES, true, registry));

        // Verify file was not overwritten (preserved formatted version)
        String afterContent = Files.readString(javaFile);
        assertEquals(formattedContent, afterContent);
    }

    @Test
    void testChecksumMismatchWithDifferentNormalizedContent() throws IOException {
        // Initial content
        String originalContent = "public class Foo {}";

        Collection<GeneratedFile> generatedFiles = ImmutableList.<GeneratedFile>builder()
                .add(GeneratedFile.builder()
                        .path("Foo.java")
                        .content(originalContent.getBytes(StandardCharsets.UTF_8))
                        .build())
                .build();

        // Write files initially
        ModelGenerator.writeDirectory(generatedFiles, tmpTargetDir.toFile(),
                ModelGenerator.GENERATED_FILES, true, registry);

        Path javaFile = tmpTargetDir.resolve("Foo.java");
        assertTrue(javaFile.toFile().exists());

        // Modify file with actual content change (not just formatting)
        String modifiedContent = "public class Bar {}";
        Files.write(javaFile, modifiedContent.getBytes(StandardCharsets.UTF_8));

        // Should throw even with normalizer because content truly differs
        assertThrows(IllegalStateException.class, () ->
                ModelGenerator.writeDirectory(generatedFiles, tmpTargetDir.toFile(),
                        ModelGenerator.GENERATED_FILES, true, registry));
    }

    @Test
    void testTimestampPreservationOnNormalizedMatch() throws IOException {
        // Initial content
        String originalContent = "import java.util.List;\n\npublic class Foo {}";

        Collection<GeneratedFile> generatedFiles = ImmutableList.<GeneratedFile>builder()
                .add(GeneratedFile.builder()
                        .path("Foo.java")
                        .content(originalContent.getBytes(StandardCharsets.UTF_8))
                        .build())
                .build();

        // Write files initially
        ModelGenerator.writeDirectory(generatedFiles, tmpTargetDir.toFile(),
                ModelGenerator.GENERATED_FILES, true, registry);

        Path javaFile = tmpTargetDir.resolve("Foo.java");

        // Simulate formatter modifying the file
        String formattedContent = "import java.util.Map;\n\npublic class  Foo {}";
        Files.write(javaFile, formattedContent.getBytes(StandardCharsets.UTF_8));

        // Record timestamp
        long timestampBefore = javaFile.toFile().lastModified();

        // Small delay to ensure timestamp would change if file was written
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // Ignore
        }

        // Write again with normalizer - should not overwrite file
        ModelGenerator.writeDirectory(generatedFiles, tmpTargetDir.toFile(),
                ModelGenerator.GENERATED_FILES, true, registry);

        // Timestamp should be preserved (file not rewritten)
        long timestampAfter = javaFile.toFile().lastModified();
        assertEquals(timestampBefore, timestampAfter, "File timestamp should be preserved when normalized content matches");
    }

    @Test
    void testNoNormalizerForFileType() throws IOException {
        // Content for a file type without normalizer configured
        String content = "some  content  with  spaces";

        Collection<GeneratedFile> generatedFiles = ImmutableList.<GeneratedFile>builder()
                .add(GeneratedFile.builder()
                        .path("file.txt")
                        .content(content.getBytes(StandardCharsets.UTF_8))
                        .build())
                .build();

        // Write files initially
        ModelGenerator.writeDirectory(generatedFiles, tmpTargetDir.toFile(),
                ModelGenerator.GENERATED_FILES, true, registry);

        Path txtFile = tmpTargetDir.resolve("file.txt");

        // Modify file slightly (adding spaces - would match with normalization)
        String modifiedContent = "some   content   with   spaces";
        Files.write(txtFile, modifiedContent.getBytes(StandardCharsets.UTF_8));

        // Should throw because no normalizer for .txt files
        assertThrows(IllegalStateException.class, () ->
                ModelGenerator.writeDirectory(generatedFiles, tmpTargetDir.toFile(),
                        ModelGenerator.GENERATED_FILES, true, registry));
    }

    @Test
    void testAutoPresetRegistersAllLanguages() throws IOException {
        FileNormalizerRegistry autoRegistry = FileNormalizerRegistry.fromConfigs(List.of(
                FileTypeNormalizer.fileTypeNormalizerBuilder()
                        .preset("auto")
                        .build()
        ));

        // Initial content for multiple file types
        Collection<GeneratedFile> generatedFiles = ImmutableList.<GeneratedFile>builder()
                .add(GeneratedFile.builder()
                        .path("Foo.java")
                        .content("import java.util.List;\npublic class Foo {}".getBytes(StandardCharsets.UTF_8))
                        .build())
                .add(GeneratedFile.builder()
                        .path("bar.ts")
                        .content("import { Foo } from './foo';\nexport class Bar {}".getBytes(StandardCharsets.UTF_8))
                        .build())
                .add(GeneratedFile.builder()
                        .path("baz.py")
                        .content("import os\ndef main(): pass".getBytes(StandardCharsets.UTF_8))
                        .build())
                .build();

        // Write files initially
        ModelGenerator.writeDirectory(generatedFiles, tmpTargetDir.toFile(),
                ModelGenerator.GENERATED_FILES, true, autoRegistry);

        // Modify files with equivalent normalized content
        Path javaFile = tmpTargetDir.resolve("Foo.java");
        Path tsFile = tmpTargetDir.resolve("bar.ts");
        Path pyFile = tmpTargetDir.resolve("baz.py");

        Files.write(javaFile, "import java.util.Map;\npublic class  Foo {}".getBytes(StandardCharsets.UTF_8));
        Files.write(tsFile, "import { Bar } from './bar';\nexport class  Bar {}".getBytes(StandardCharsets.UTF_8));
        Files.write(pyFile, "import sys\ndef main():  pass".getBytes(StandardCharsets.UTF_8));

        // All should pass with auto preset
        assertDoesNotThrow(() ->
                ModelGenerator.writeDirectory(generatedFiles, tmpTargetDir.toFile(),
                        ModelGenerator.GENERATED_FILES, true, autoRegistry));
    }

    @Test
    void testChecksumUpdatedOnNormalizedMatch() throws IOException {
        // Initial content
        String originalContent = "import java.util.List;\n\npublic class Foo {}";

        Collection<GeneratedFile> generatedFiles = ImmutableList.<GeneratedFile>builder()
                .add(GeneratedFile.builder()
                        .path("Foo.java")
                        .content(originalContent.getBytes(StandardCharsets.UTF_8))
                        .build())
                .build();

        // Write files initially
        ModelGenerator.writeDirectory(generatedFiles, tmpTargetDir.toFile(),
                ModelGenerator.GENERATED_FILES, true, registry);

        Path javaFile = tmpTargetDir.resolve("Foo.java");

        // Get original checksum from .generated-files
        Collection<GeneratorFileEntry> entriesBefore = ModelGenerator.readGeneratedFiles(
                tmpTargetDir.toFile(), ModelGenerator.GENERATED_FILES);
        String checksumBefore = entriesBefore.iterator().next().getChecksum();

        // Simulate formatter modifying the file
        String formattedContent = "import java.util.Map;\n\npublic class  Foo {}";
        Files.write(javaFile, formattedContent.getBytes(StandardCharsets.UTF_8));

        String filesystemChecksum = ChecksumUtil.getMD5(javaFile);
        assertNotEquals(checksumBefore, filesystemChecksum, "Filesystem checksum should differ after formatting");

        // Write again with normalizer
        ModelGenerator.writeDirectory(generatedFiles, tmpTargetDir.toFile(),
                ModelGenerator.GENERATED_FILES, true, registry);

        // Checksum in .generated-files should now match filesystem
        Collection<GeneratorFileEntry> entriesAfter = ModelGenerator.readGeneratedFiles(
                tmpTargetDir.toFile(), ModelGenerator.GENERATED_FILES);
        String checksumAfter = entriesAfter.iterator().next().getChecksum();

        assertEquals(filesystemChecksum, checksumAfter, "Checksum should be updated to match filesystem file");
    }
}
