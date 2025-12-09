# Design: Add Real-World Test Cases to ModelGeneratorTest

## Overview

This document describes the design for adding realistic test scenarios to the ModelGeneratorTest suite. The tests will use actual code patterns and directory structures that mirror production usage.

## Design Decisions

### Decision 1: Separate Test Class

**Choice**: Create a new `ModelGeneratorRealWorldTest` class rather than extending `ModelGeneratorTest`

**Rationale**:
- Keeps existing tests focused on unit-level behavior
- Real-world tests are longer and more complex
- Allows different test lifecycle management
- Clear separation of concerns

### Decision 2: Inline Test Content

**Choice**: Define test content as inline strings rather than external resource files

**Rationale**:
- Tests are self-contained and readable
- No need to manage separate resource files
- Content is directly visible in test methods
- Easier to modify and understand test intent

### Decision 3: Content Patterns

**Choice**: Use realistic but minimal code snippets

**Rationale**:
- Long enough to be realistic (imports, class structure, methods)
- Short enough to be readable in tests
- Include patterns that formatters typically modify (spacing, imports order)

## Test Scenarios

### 1. Java Project Structure

```
target/
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── example/
│                   ├── model/
│                   │   └── User.java
│                   ├── repository/
│                   │   └── UserRepository.java
│                   └── service/
│                       └── UserService.java
├── .generated-files
└── .generator-ignore
```

**Test content example**:
```java
package com.example.model;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class User {
    @Id
    private UUID id;
    private String name;
    private String email;
    
    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
}
```

### 2. TypeScript/React Structure

```
target/
├── src/
│   ├── components/
│   │   └── UserCard.tsx
│   ├── hooks/
│   │   └── useUser.ts
│   └── types/
│       └── User.ts
└── .generated-files
```

**Test content example**:
```typescript
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
```

### 3. Formatter Modification Simulation

Test that simulates what happens when a code formatter modifies generated files:

**Original generated content**:
```java
import java.util.List;
import java.util.Map;

public class Service {
    private Map<String,List<String>> data;
}
```

**After formatter (Prettier/IDE)**:
```java
import java.util.List;
import java.util.Map;

public class Service {
    private Map<String, List<String>> data;
}
```

The test verifies:
1. Checksum mismatch is detected
2. With normalizer: files are recognized as equivalent
3. Without normalizer: error is thrown

### 4. Actor-Based Generation

Simulates generating files for multiple "actors" (e.g., different modules or tenants):

```
target/
├── actor-admin/
│   ├── AdminController.java
│   └── .generated-files-admin
├── actor-user/
│   ├── UserController.java
│   └── .generated-files-user
└── common/
    ├── BaseController.java
    └── .generated-files
```

### 5. Configuration Files

Test generation of various config file types:

```yaml
# application.yaml
server:
  port: 8080
  host: localhost

database:
  url: jdbc:postgresql://localhost:5432/mydb
  username: admin
```

```json
{
  "name": "my-app",
  "version": "1.0.0",
  "dependencies": {
    "react": "^18.0.0"
  }
}
```

## Test Method Structure

Each test method follows this pattern:

```java
@Test
void testScenarioName() throws Exception {
    // 1. Setup - Create temporary directory
    Path tmpDir = Files.createTempDirectory(Paths.get("target"), "realWorldTest");
    
    // 2. Define generated files with realistic content
    Collection<GeneratedFile> files = ImmutableList.of(
        GeneratedFile.builder()
            .path("src/main/java/com/example/Model.java")
            .content(JAVA_MODEL_CONTENT.getBytes(StandardCharsets.UTF_8))
            .build()
    );
    
    // 3. Execute generation
    ModelGenerator.writeDirectory(files, tmpDir.toFile(), 
        ModelGenerator.GENERATED_FILES, true);
    
    // 4. Verify results
    assertTrue(Files.exists(tmpDir.resolve("src/main/java/com/example/Model.java")));
    
    // 5. Simulate modification scenario (if applicable)
    // ...
    
    // 6. Re-run generation and verify behavior
    // ...
}
```

## Content Constants

Define reusable content constants at the class level:

```java
public class ModelGeneratorRealWorldTest {
    
    // Java content
    static final String JAVA_ENTITY = """
        package com.example.model;
        
        import java.util.UUID;
        import javax.persistence.Entity;
        
        @Entity
        public class User {
            private UUID id;
            private String name;
        }
        """;
    
    // TypeScript content
    static final String TS_COMPONENT = """
        import React from 'react';
        import { User } from '../types/User';
        
        export const UserCard: React.FC<{ user: User }> = ({ user }) => {
          return <div>{user.name}</div>;
        };
        """;
    
    // Config content
    static final String YAML_CONFIG = """
        server:
          port: 8080
        database:
          url: jdbc:postgresql://localhost/db
        """;
}
```

## Assertions

Use descriptive assertions that explain what's being verified:

```java
// Good
assertTrue(Files.exists(javaFile), 
    "Java entity file should be generated at standard Maven location");

assertEquals(expectedChecksum, actualChecksum,
    "Checksum should match after clean generation");

assertThrows(IllegalStateException.class, () -> ...,
    "Should detect manual modification when checksum validation enabled");

// Avoid
assertTrue(Files.exists(javaFile)); // No context on what's being tested
```

## Test Categories Summary

| Category | Test Count | Purpose |
|----------|------------|---------|
| Java Project | 3 | Standard Maven structure, multiple related files |
| TypeScript Project | 2 | Frontend conventions, component generation |
| Formatter Simulation | 3 | IDE/tool formatting changes |
| Actor-Based | 2 | Multi-actor scenarios with discriminators |
| Configuration | 2 | YAML, JSON, properties files |
| Edge Cases | 4 | Empty, large, unicode, special chars |

**Total: ~16 new test methods**
