package hu.blackbelt.judo.generator.commons;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

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
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Real-world test scenarios for ModelGenerator that use realistic code content,
 * directory structures, and generation patterns encountered in production.
 */
@Slf4j
public class ModelGeneratorRealWorldTest {

    static final String TMP_DIR_PREFIX = "realWorldTestTarget";

    // ========== Java Content Constants ==========

    static final String JAVA_USER_ENTITY = """
        package com.example.model;

        import java.util.UUID;
        import javax.persistence.Entity;
        import javax.persistence.Id;
        import javax.persistence.Column;

        @Entity
        public class User {
            @Id
            private UUID id;

            @Column(nullable = false)
            private String name;

            @Column(unique = true)
            private String email;

            public UUID getId() { return id; }
            public void setId(UUID id) { this.id = id; }
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            public String getEmail() { return email; }
            public void setEmail(String email) { this.email = email; }
        }
        """;

    static final String JAVA_USER_REPOSITORY = """
        package com.example.repository;

        import com.example.model.User;
        import java.util.Optional;
        import java.util.UUID;

        public interface UserRepository {
            Optional<User> findById(UUID id);
            Optional<User> findByEmail(String email);
            User save(User user);
            void deleteById(UUID id);
        }
        """;

    static final String JAVA_USER_SERVICE = """
        package com.example.service;

        import com.example.model.User;
        import com.example.repository.UserRepository;
        import java.util.Optional;
        import java.util.UUID;

        public class UserService {
            private final UserRepository userRepository;

            public UserService(UserRepository userRepository) {
                this.userRepository = userRepository;
            }

            public Optional<User> getUser(UUID id) {
                return userRepository.findById(id);
            }

            public User createUser(String name, String email) {
                User user = new User();
                user.setId(UUID.randomUUID());
                user.setName(name);
                user.setEmail(email);
                return userRepository.save(user);
            }
        }
        """;

    static final String JAVA_USER_ENTITY_UPDATED = """
        package com.example.model;

        import java.util.UUID;
        import java.time.LocalDateTime;
        import javax.persistence.Entity;
        import javax.persistence.Id;
        import javax.persistence.Column;

        @Entity
        public class User {
            @Id
            private UUID id;

            @Column(nullable = false)
            private String name;

            @Column(unique = true)
            private String email;

            @Column
            private LocalDateTime createdAt;

            public UUID getId() { return id; }
            public void setId(UUID id) { this.id = id; }
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            public String getEmail() { return email; }
            public void setEmail(String email) { this.email = email; }
            public LocalDateTime getCreatedAt() { return createdAt; }
            public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        }
        """;

    // ========== TypeScript Content Constants ==========

    static final String TS_USER_TYPE = """
        export interface User {
          id: string;
          name: string;
          email: string;
          createdAt?: Date;
        }

        export type UserCreateInput = Omit<User, 'id' | 'createdAt'>;
        """;

    static final String TS_USER_CARD_COMPONENT = """
        import React from 'react';
        import { User } from '../types/User';

        interface UserCardProps {
          user: User;
          onSelect?: (user: User) => void;
        }

        export const UserCard: React.FC<UserCardProps> = ({ user, onSelect }) => {
          return (
            <div className="user-card" onClick={() => onSelect?.(user)}>
              <h3>{user.name}</h3>
              <p>{user.email}</p>
            </div>
          );
        };
        """;

    static final String TS_USE_USER_HOOK = """
        import { useState, useEffect } from 'react';
        import { User } from '../types/User';

        export function useUser(userId: string) {
          const [user, setUser] = useState<User | null>(null);
          const [loading, setLoading] = useState(true);
          const [error, setError] = useState<Error | null>(null);

          useEffect(() => {
            fetch(`/api/users/${userId}`)
              .then(res => res.json())
              .then(setUser)
              .catch(setError)
              .finally(() => setLoading(false));
          }, [userId]);

          return { user, loading, error };
        }
        """;

    // ========== Configuration Content Constants ==========

    static final String YAML_APPLICATION_CONFIG = """
        server:
          port: 8080
          host: localhost
          contextPath: /api

        database:
          url: jdbc:postgresql://localhost:5432/mydb
          username: admin
          password: ${DB_PASSWORD}
          pool:
            minSize: 5
            maxSize: 20

        logging:
          level:
            root: INFO
            com.example: DEBUG
        """;

    static final String JSON_PACKAGE_CONFIG = """
        {
          "name": "my-frontend-app",
          "version": "1.0.0",
          "private": true,
          "dependencies": {
            "react": "^18.2.0",
            "react-dom": "^18.2.0",
            "typescript": "^5.0.0"
          },
          "devDependencies": {
            "@types/react": "^18.2.0",
            "vite": "^5.0.0"
          },
          "scripts": {
            "dev": "vite",
            "build": "tsc && vite build",
            "preview": "vite preview"
          }
        }
        """;

    // ========== Actor-Based Content Constants ==========

    static final String JAVA_ADMIN_CONTROLLER = """
        package com.example.admin;

        import org.springframework.web.bind.annotation.RestController;
        import org.springframework.web.bind.annotation.RequestMapping;

        @RestController
        @RequestMapping("/admin")
        public class AdminController {
            // Admin-specific endpoints
        }
        """;

    static final String JAVA_USER_CONTROLLER = """
        package com.example.user;

        import org.springframework.web.bind.annotation.RestController;
        import org.springframework.web.bind.annotation.RequestMapping;

        @RestController
        @RequestMapping("/user")
        public class UserController {
            // User-specific endpoints
        }
        """;

    // ========== Test Instance Variables ==========

    Path tmpTargetDir;

    @BeforeEach
    void setUp() throws Exception {
        tmpTargetDir = Files.createTempDirectory(
            Paths.get("target"),
            TMP_DIR_PREFIX
        );
    }

    // ========== Helper Methods ==========

    Path absolutePathFor(String... relativePath) {
        return Paths.get(tmpTargetDir.toString(), relativePath);
    }

    void writeIgnoreFile(String fileName, List<String> patterns)
        throws IOException {
        Files.write(
            absolutePathFor(fileName),
            String.join("\n", patterns).getBytes(StandardCharsets.UTF_8)
        );
    }

    // ========== Java Project Tests (2.x) ==========

    @Test
    void testGenerateJavaEntityWithMavenStructure() {
        // Given: A Java entity file in standard Maven structure
        Collection<GeneratedFile> files = ImmutableList.of(
            GeneratedFile.builder()
                .path("src/main/java/com/example/model/User.java")
                .content(JAVA_USER_ENTITY.getBytes(StandardCharsets.UTF_8))
                .build()
        );

        // When: Writing the directory
        ModelGenerator.writeDirectory(
            files,
            tmpTargetDir.toFile(),
            ModelGenerator.GENERATED_FILES,
            true
        );

        // Then: File exists at correct location
        Path javaFile = absolutePathFor(
            "src",
            "main",
            "java",
            "com",
            "example",
            "model",
            "User.java"
        );
        assertTrue(
            Files.exists(javaFile),
            "Java entity file should be generated at standard Maven location"
        );

        // And: Content matches
        assertDoesNotThrow(() -> {
            String content = Files.readString(javaFile);
            assertThat(content, containsString("package com.example.model;"));
            assertThat(content, containsString("@Entity"));
            assertThat(content, containsString("public class User"));
        });

        // And: Checksum file is created
        Path checksumFile = absolutePathFor(ModelGenerator.GENERATED_FILES);
        assertTrue(
            Files.exists(checksumFile),
            "Checksum file should be created"
        );

        Collection<GeneratorFileEntry> entries =
            ModelGenerator.readGeneratedFiles(
                tmpTargetDir.toFile(),
                ModelGenerator.GENERATED_FILES
            );
        assertEquals(
            1,
            entries.size(),
            "Should have one entry in checksum file"
        );
    }

    @Test
    void testGenerateMultipleRelatedJavaFiles() {
        // Given: Multiple related Java files (entity, repository, service)
        Collection<GeneratedFile> files = ImmutableList.of(
            GeneratedFile.builder()
                .path("src/main/java/com/example/model/User.java")
                .content(JAVA_USER_ENTITY.getBytes(StandardCharsets.UTF_8))
                .build(),
            GeneratedFile.builder()
                .path(
                    "src/main/java/com/example/repository/UserRepository.java"
                )
                .content(JAVA_USER_REPOSITORY.getBytes(StandardCharsets.UTF_8))
                .build(),
            GeneratedFile.builder()
                .path("src/main/java/com/example/service/UserService.java")
                .content(JAVA_USER_SERVICE.getBytes(StandardCharsets.UTF_8))
                .build()
        );

        // When: Writing the directory
        ModelGenerator.writeDirectory(
            files,
            tmpTargetDir.toFile(),
            ModelGenerator.GENERATED_FILES,
            true
        );

        // Then: All files exist
        assertTrue(
            Files.exists(
                absolutePathFor(
                    "src",
                    "main",
                    "java",
                    "com",
                    "example",
                    "model",
                    "User.java"
                )
            ),
            "Entity file should exist"
        );
        assertTrue(
            Files.exists(
                absolutePathFor(
                    "src",
                    "main",
                    "java",
                    "com",
                    "example",
                    "repository",
                    "UserRepository.java"
                )
            ),
            "Repository file should exist"
        );
        assertTrue(
            Files.exists(
                absolutePathFor(
                    "src",
                    "main",
                    "java",
                    "com",
                    "example",
                    "service",
                    "UserService.java"
                )
            ),
            "Service file should exist"
        );

        // And: All are in checksum file
        Collection<GeneratorFileEntry> entries =
            ModelGenerator.readGeneratedFiles(
                tmpTargetDir.toFile(),
                ModelGenerator.GENERATED_FILES
            );
        assertEquals(
            3,
            entries.size(),
            "Should have three entries in checksum file"
        );
    }

    @Test
    void testRegenerateJavaFilesAfterContentChange() {
        // Given: Initial generation
        Collection<GeneratedFile> initialFiles = ImmutableList.of(
            GeneratedFile.builder()
                .path("src/main/java/com/example/model/User.java")
                .content(JAVA_USER_ENTITY.getBytes(StandardCharsets.UTF_8))
                .build()
        );

        ModelGenerator.writeDirectory(
            initialFiles,
            tmpTargetDir.toFile(),
            ModelGenerator.GENERATED_FILES,
            true
        );

        Path javaFile = absolutePathFor(
            "src",
            "main",
            "java",
            "com",
            "example",
            "model",
            "User.java"
        );

        // When: Regenerating with updated content (added createdAt field)
        Collection<GeneratedFile> updatedFiles = ImmutableList.of(
            GeneratedFile.builder()
                .path("src/main/java/com/example/model/User.java")
                .content(
                    JAVA_USER_ENTITY_UPDATED.getBytes(StandardCharsets.UTF_8)
                )
                .build()
        );

        ModelGenerator.writeDirectory(
            updatedFiles,
            tmpTargetDir.toFile(),
            ModelGenerator.GENERATED_FILES,
            true
        );

        // Then: File is updated
        assertDoesNotThrow(() -> {
            String content = Files.readString(javaFile);
            assertThat(
                content,
                containsString("private LocalDateTime createdAt;")
            );
            assertThat(
                content,
                containsString("import java.time.LocalDateTime;")
            );
        });
    }

    // ========== TypeScript/Frontend Tests (3.x) ==========

    @Test
    void testGenerateTypeScriptReactComponent() {
        // Given: TypeScript React component files
        Collection<GeneratedFile> files = ImmutableList.of(
            GeneratedFile.builder()
                .path("src/components/UserCard.tsx")
                .content(
                    TS_USER_CARD_COMPONENT.getBytes(StandardCharsets.UTF_8)
                )
                .build()
        );

        // When: Writing the directory
        ModelGenerator.writeDirectory(
            files,
            tmpTargetDir.toFile(),
            ModelGenerator.GENERATED_FILES,
            true
        );

        // Then: File exists
        Path tsxFile = absolutePathFor("src", "components", "UserCard.tsx");
        assertTrue(
            Files.exists(tsxFile),
            "TSX component file should be generated"
        );

        // And: Content is correct
        assertDoesNotThrow(() -> {
            String content = Files.readString(tsxFile);
            assertThat(content, containsString("import React from 'react';"));
            assertThat(content, containsString("export const UserCard"));
            assertThat(content, containsString("<div className=\"user-card\""));
        });
    }

    @Test
    void testGenerateMultipleFrontendFiles() {
        // Given: Multiple frontend files (types, component, hook)
        Collection<GeneratedFile> files = ImmutableList.of(
            GeneratedFile.builder()
                .path("src/types/User.ts")
                .content(TS_USER_TYPE.getBytes(StandardCharsets.UTF_8))
                .build(),
            GeneratedFile.builder()
                .path("src/components/UserCard.tsx")
                .content(
                    TS_USER_CARD_COMPONENT.getBytes(StandardCharsets.UTF_8)
                )
                .build(),
            GeneratedFile.builder()
                .path("src/hooks/useUser.ts")
                .content(TS_USE_USER_HOOK.getBytes(StandardCharsets.UTF_8))
                .build()
        );

        // When: Writing the directory
        ModelGenerator.writeDirectory(
            files,
            tmpTargetDir.toFile(),
            ModelGenerator.GENERATED_FILES,
            true
        );

        // Then: All files exist
        assertTrue(
            Files.exists(absolutePathFor("src", "types", "User.ts")),
            "Type definition file should exist"
        );
        assertTrue(
            Files.exists(absolutePathFor("src", "components", "UserCard.tsx")),
            "Component file should exist"
        );
        assertTrue(
            Files.exists(absolutePathFor("src", "hooks", "useUser.ts")),
            "Hook file should exist"
        );
    }

    // ========== Formatter Simulation Tests (4.x) ==========

    @Test
    void testDetectChecksumMismatchAfterFormatterChanges() throws IOException {
        // Given: Generated Java file
        String originalContent = """
            package com.example;

            import java.util.List;
            import java.util.Map;

            public class Service {
                private Map<String,List<String>> data;
            }
            """;

        Collection<GeneratedFile> files = ImmutableList.of(
            GeneratedFile.builder()
                .path("src/main/java/com/example/Service.java")
                .content(originalContent.getBytes(StandardCharsets.UTF_8))
                .build()
        );

        ModelGenerator.writeDirectory(
            files,
            tmpTargetDir.toFile(),
            ModelGenerator.GENERATED_FILES,
            true
        );

        // When: Formatter modifies the file (adds space after comma in generics)
        Path javaFile = absolutePathFor(
            "src",
            "main",
            "java",
            "com",
            "example",
            "Service.java"
        );
        String formattedContent = """
            package com.example;

            import java.util.List;
            import java.util.Map;

            public class Service {
                private Map<String, List<String>> data;
            }
            """;
        Files.write(
            javaFile,
            formattedContent.getBytes(StandardCharsets.UTF_8)
        );

        // Then: Regeneration without normalizer should throw
        assertThrows(
            IllegalStateException.class,
            () ->
                ModelGenerator.writeDirectory(
                    files,
                    tmpTargetDir.toFile(),
                    ModelGenerator.GENERATED_FILES,
                    true,
                    null
                ),
            "Should detect manual modification when checksum validation enabled"
        );
    }

    @Test
    void testNormalizedComparisonAcceptsFormatterWhitespaceChanges()
        throws IOException {
        // Given: Generated Java file with normalizer configured
        // The normalizer removes imports and collapses double spaces,
        // so both files should normalize to the same content
        String originalContent = """
            import java.util.List;
            import java.util.Map;

            public class Service {
                private String data;
            }
            """;

        FileNormalizerRegistry registry = FileNormalizerRegistry.fromConfigs(
            List.of(
                FileTypeNormalizer.fileTypeNormalizerBuilder()
                    .preset("java")
                    .build()
            )
        );

        Collection<GeneratedFile> files = ImmutableList.of(
            GeneratedFile.builder()
                .path("Service.java")
                .content(originalContent.getBytes(StandardCharsets.UTF_8))
                .build()
        );

        ModelGenerator.writeDirectory(
            files,
            tmpTargetDir.toFile(),
            ModelGenerator.GENERATED_FILES,
            true,
            registry
        );

        // When: Formatter modifies the file (reordered imports, added double spaces)
        // After normalization, both should match because imports are stripped
        // and double spaces are collapsed
        Path javaFile = absolutePathFor("Service.java");
        String formattedContent = """
            import java.util.Map;
            import java.util.List;

            public class Service {
                private  String data;
            }
            """;
        Files.write(
            javaFile,
            formattedContent.getBytes(StandardCharsets.UTF_8)
        );

        // Then: Regeneration with normalizer should succeed
        assertDoesNotThrow(
            () ->
                ModelGenerator.writeDirectory(
                    files,
                    tmpTargetDir.toFile(),
                    ModelGenerator.GENERATED_FILES,
                    true,
                    registry
                ),
            "Should accept whitespace-only changes with normalizer"
        );

        // And: File content is preserved (not overwritten)
        String afterContent = Files.readString(javaFile);
        assertEquals(
            formattedContent,
            afterContent,
            "Formatted content should be preserved"
        );
    }

    @Test
    void testNormalizedComparisonRejectsActualContentChanges()
        throws IOException {
        // Given: Generated Java file with normalizer configured
        String originalContent = """
            public class Service {
                private String name;
            }
            """;

        FileNormalizerRegistry registry = FileNormalizerRegistry.fromConfigs(
            List.of(
                FileTypeNormalizer.fileTypeNormalizerBuilder()
                    .preset("java")
                    .build()
            )
        );

        Collection<GeneratedFile> files = ImmutableList.of(
            GeneratedFile.builder()
                .path("Service.java")
                .content(originalContent.getBytes(StandardCharsets.UTF_8))
                .build()
        );

        ModelGenerator.writeDirectory(
            files,
            tmpTargetDir.toFile(),
            ModelGenerator.GENERATED_FILES,
            true,
            registry
        );

        // When: File is modified with actual content change (renamed class)
        Path javaFile = absolutePathFor("Service.java");
        String modifiedContent = """
            public class MyService {
                private String name;
            }
            """;
        Files.write(javaFile, modifiedContent.getBytes(StandardCharsets.UTF_8));

        // Then: Should throw even with normalizer
        assertThrows(
            IllegalStateException.class,
            () ->
                ModelGenerator.writeDirectory(
                    files,
                    tmpTargetDir.toFile(),
                    ModelGenerator.GENERATED_FILES,
                    true,
                    registry
                ),
            "Should reject actual content changes even with normalizer"
        );
    }

    // ========== Actor-Based Generation Tests (5.x) ==========

    @Test
    void testGenerateFilesForMultipleActors() {
        // Given: Files for two actors (admin and user)
        Collection<GeneratedFile> adminFiles = ImmutableList.of(
            GeneratedFile.builder()
                .path("AdminController.java")
                .content(JAVA_ADMIN_CONTROLLER.getBytes(StandardCharsets.UTF_8))
                .build()
        );

        Collection<GeneratedFile> userFiles = ImmutableList.of(
            GeneratedFile.builder()
                .path("UserController.java")
                .content(JAVA_USER_CONTROLLER.getBytes(StandardCharsets.UTF_8))
                .build()
        );

        // Create actor directories
        File adminDir = absolutePathFor("actor-admin").toFile();
        File userDir = absolutePathFor("actor-user").toFile();
        adminDir.mkdirs();
        userDir.mkdirs();

        // When: Writing to separate actor directories
        ModelGenerator.writeDirectory(
            adminFiles,
            adminDir,
            ModelGenerator.GENERATED_FILES + "-admin",
            true
        );
        ModelGenerator.writeDirectory(
            userFiles,
            userDir,
            ModelGenerator.GENERATED_FILES + "-user",
            true
        );

        // Then: Each actor has its files in its directory
        assertTrue(
            Files.exists(
                absolutePathFor("actor-admin", "AdminController.java")
            ),
            "Admin controller should be in admin directory"
        );
        assertTrue(
            Files.exists(absolutePathFor("actor-user", "UserController.java")),
            "User controller should be in user directory"
        );

        // And: Each has its own checksum file
        assertTrue(
            Files.exists(
                absolutePathFor("actor-admin", ".generated-files-admin")
            ),
            "Admin should have actor-specific checksum file"
        );
        assertTrue(
            Files.exists(
                absolutePathFor("actor-user", ".generated-files-user")
            ),
            "User should have actor-specific checksum file"
        );
    }

    @Test
    void testActorSpecificChecksumMaintenance() throws IOException {
        // Given: Files generated for two actors
        Collection<GeneratedFile> adminFiles = ImmutableList.of(
            GeneratedFile.builder()
                .path("AdminController.java")
                .content(JAVA_ADMIN_CONTROLLER.getBytes(StandardCharsets.UTF_8))
                .build()
        );

        Collection<GeneratedFile> userFiles = ImmutableList.of(
            GeneratedFile.builder()
                .path("UserController.java")
                .content(JAVA_USER_CONTROLLER.getBytes(StandardCharsets.UTF_8))
                .build()
        );

        File adminDir = absolutePathFor("actor-admin").toFile();
        File userDir = absolutePathFor("actor-user").toFile();
        adminDir.mkdirs();
        userDir.mkdirs();

        ModelGenerator.writeDirectory(
            adminFiles,
            adminDir,
            ModelGenerator.GENERATED_FILES + "-admin",
            true
        );
        ModelGenerator.writeDirectory(
            userFiles,
            userDir,
            ModelGenerator.GENERATED_FILES + "-user",
            true
        );

        // When: Modifying only admin actor's file
        Path adminFile = absolutePathFor("actor-admin", "AdminController.java");
        Files.write(adminFile, "// Modified".getBytes(StandardCharsets.UTF_8));

        // Then: Admin regeneration fails
        assertThrows(
            IllegalStateException.class,
            () ->
                ModelGenerator.writeDirectory(
                    adminFiles,
                    adminDir,
                    ModelGenerator.GENERATED_FILES + "-admin",
                    true
                ),
            "Admin actor regeneration should fail due to modification"
        );

        // But: User regeneration still succeeds
        assertDoesNotThrow(
            () ->
                ModelGenerator.writeDirectory(
                    userFiles,
                    userDir,
                    ModelGenerator.GENERATED_FILES + "-user",
                    true
                ),
            "User actor regeneration should succeed (unmodified)"
        );
    }

    // ========== Configuration File Tests (6.x) ==========

    @Test
    void testGenerateYamlConfigurationFiles() {
        // Given: YAML configuration file
        Collection<GeneratedFile> files = ImmutableList.of(
            GeneratedFile.builder()
                .path("src/main/resources/application.yaml")
                .content(
                    YAML_APPLICATION_CONFIG.getBytes(StandardCharsets.UTF_8)
                )
                .build()
        );

        // When: Writing the directory
        ModelGenerator.writeDirectory(
            files,
            tmpTargetDir.toFile(),
            ModelGenerator.GENERATED_FILES,
            true
        );

        // Then: File exists with correct content
        Path yamlFile = absolutePathFor(
            "src",
            "main",
            "resources",
            "application.yaml"
        );
        assertTrue(
            Files.exists(yamlFile),
            "YAML config file should be generated"
        );

        assertDoesNotThrow(() -> {
            String content = Files.readString(yamlFile);
            assertThat(content, containsString("server:"));
            assertThat(content, containsString("port: 8080"));
            assertThat(content, containsString("database:"));
            // Verify indentation is preserved
            assertThat(content, containsString("  pool:"));
            assertThat(content, containsString("    minSize: 5"));
        });
    }

    @Test
    void testGenerateJsonConfigurationFiles() {
        // Given: JSON configuration file
        Collection<GeneratedFile> files = ImmutableList.of(
            GeneratedFile.builder()
                .path("package.json")
                .content(JSON_PACKAGE_CONFIG.getBytes(StandardCharsets.UTF_8))
                .build()
        );

        // When: Writing the directory
        ModelGenerator.writeDirectory(
            files,
            tmpTargetDir.toFile(),
            ModelGenerator.GENERATED_FILES,
            true
        );

        // Then: File exists with valid JSON structure
        Path jsonFile = absolutePathFor("package.json");
        assertTrue(
            Files.exists(jsonFile),
            "JSON config file should be generated"
        );

        assertDoesNotThrow(() -> {
            String content = Files.readString(jsonFile);
            assertThat(
                content,
                containsString("\"name\": \"my-frontend-app\"")
            );
            assertThat(content, containsString("\"dependencies\":"));
            assertThat(content, containsString("\"react\": \"^18.2.0\""));
        });
    }

    // ========== Edge Case Tests (7.x) ==========

    @Test
    void testHandleEmptyFileGeneration() {
        // Given: An empty file
        Collection<GeneratedFile> files = ImmutableList.of(
            GeneratedFile.builder()
                .path("empty.txt")
                .content(new byte[0])
                .build()
        );

        // When: Writing the directory
        ModelGenerator.writeDirectory(
            files,
            tmpTargetDir.toFile(),
            ModelGenerator.GENERATED_FILES,
            true
        );

        // Then: Empty file is created
        Path emptyFile = absolutePathFor("empty.txt");
        assertTrue(Files.exists(emptyFile), "Empty file should be created");

        assertDoesNotThrow(() -> {
            assertEquals(0, Files.size(emptyFile), "File should be empty");
        });

        // And: Checksum is recorded
        Collection<GeneratorFileEntry> entries =
            ModelGenerator.readGeneratedFiles(
                tmpTargetDir.toFile(),
                ModelGenerator.GENERATED_FILES
            );
        assertEquals(
            1,
            entries.size(),
            "Empty file should be in checksum file"
        );
    }

    @Test
    void testHandleLargeFileGeneration() {
        // Given: A large file (100KB+)
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            largeContent
                .append("// Line ")
                .append(i)
                .append(
                    ": This is some generated content to make the file large.\n"
                );
        }
        String content = largeContent.toString();
        assertTrue(
            content.length() > 100_000,
            "Test content should be > 100KB"
        );

        Collection<GeneratedFile> files = ImmutableList.of(
            GeneratedFile.builder()
                .path("large-file.java")
                .content(content.getBytes(StandardCharsets.UTF_8))
                .build()
        );

        // When: Writing the directory
        ModelGenerator.writeDirectory(
            files,
            tmpTargetDir.toFile(),
            ModelGenerator.GENERATED_FILES,
            true
        );

        // Then: Large file is created with complete content
        Path largeFile = absolutePathFor("large-file.java");
        assertTrue(Files.exists(largeFile), "Large file should be created");

        assertDoesNotThrow(() -> {
            String readContent = Files.readString(largeFile);
            assertEquals(
                content,
                readContent,
                "Content should match completely"
            );
        });
    }

    @Test
    void testHandleSpecialCharactersInPaths() {
        // Given: Files with special characters in paths
        Collection<GeneratedFile> files = ImmutableList.of(
            GeneratedFile.builder()
                .path("my-component/user_service.java")
                .content(
                    "// File with hyphens and underscores".getBytes(
                        StandardCharsets.UTF_8
                    )
                )
                .build(),
            GeneratedFile.builder()
                .path("v2.0/api-v2.java")
                .content(
                    "// File with dots and hyphens".getBytes(
                        StandardCharsets.UTF_8
                    )
                )
                .build()
        );

        // When: Writing the directory
        ModelGenerator.writeDirectory(
            files,
            tmpTargetDir.toFile(),
            ModelGenerator.GENERATED_FILES,
            true
        );

        // Then: Files are created with special characters preserved
        assertTrue(
            Files.exists(absolutePathFor("my-component", "user_service.java")),
            "File with hyphen and underscore should exist"
        );
        assertTrue(
            Files.exists(absolutePathFor("v2.0", "api-v2.java")),
            "File with dots and hyphens should exist"
        );
    }

    @Test
    void testHandleUnicodeContent() {
        // Given: File with Unicode content
        String unicodeContent = """
            // Unicode test file
            // Japanese: こんにちは世界
            // Chinese: 你好世界
            // Emoji: 🚀 🎉 ✨
            // German: Größe, Äpfel, Übung
            // Russian: Привет мир

            public class UnicodeTest {
                String greeting = "Hello, 世界! 🌍";
            }
            """;

        Collection<GeneratedFile> files = ImmutableList.of(
            GeneratedFile.builder()
                .path("UnicodeTest.java")
                .content(unicodeContent.getBytes(StandardCharsets.UTF_8))
                .build()
        );

        // When: Writing the directory
        ModelGenerator.writeDirectory(
            files,
            tmpTargetDir.toFile(),
            ModelGenerator.GENERATED_FILES,
            true
        );

        // Then: File exists with correct Unicode content
        Path unicodeFile = absolutePathFor("UnicodeTest.java");
        assertTrue(Files.exists(unicodeFile), "Unicode file should be created");

        assertDoesNotThrow(() -> {
            String content = Files.readString(
                unicodeFile,
                StandardCharsets.UTF_8
            );
            assertThat(content, containsString("こんにちは世界"));
            assertThat(content, containsString("你好世界"));
            assertThat(content, containsString("🚀"));
            assertThat(content, containsString("Größe"));
            assertThat(content, containsString("Привет мир"));
        });
    }
}
