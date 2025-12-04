# Project Context

## Purpose

JUDO Generator Commons is a template-based code generation framework that transforms meta-models into production-ready code using Handlebars templates combined with Spring Expression Language (SpringEL).

**Primary Goals:**
- Provide robust foundation for generator engine development
- Enable checksum validation to protect against overwrites
- Support incremental generation for efficiency
- Offer flexible ignore patterns for customization
- Manage file permissions and template inheritance

## Tech Stack

**Core Technologies:**
- Java 21
- Maven 3.9.4
- OSGi Bundle packaging
- Handlebars.java 4.4.0
- Spring Expression Language 6.2.7
- Jackson YAML 2.17.2

**Build & Testing:**
- JUnit 5 (Jupiter)
- Mockito 3.0.0
- Hamcrest 2.1
- JaCoCo (code coverage)
- SonarQube integration

**Key Dependencies:**
- Google Guava 30.0-jre
- SLF4J 2.0.16 + Logback 1.5.12
- ClassGraph 4.8.152 (annotation scanning)

## Project Conventions

### Code Style

**Java Conventions:**
- Use Lombok annotations to minimize boilerplate (`@Builder`, `@Value`, `@Slf4j`, `@NonNull`)
- Functional style preferred (Java 8+ streams, Functions, Predicates)
- Immutability: final fields, ImmutableList, Value objects
- Public static methods for helper classes
- Extend `StaticMethodValueResolver` for helper base classes

**Naming:**
- Classes: PascalCase
- Methods: camelCase
- Constants: UPPER_SNAKE_CASE
- Packages: lowercase, no underscores

**Documentation:**
- Javadoc for public APIs
- Inline comments for complex logic
- README.md/AGENTS.md for architecture and guides

### Architecture Patterns

**Design Patterns Used:**
1. **Builder Pattern**: Configuration objects (GeneratorParameter, GeneratorModel, GeneratorTemplate)
2. **Strategy Pattern**: Function-based extensibility (targetDirectoryResolver, performExecutor)
3. **Template Method**: Static methods define workflow (generateToDirectory, writeDirectory)
4. **Factory Pattern**: Context creation (createGeneratorContext, generatorModelBuilder)
5. **Value Object Pattern**: Immutable data carriers (GeneratedFile, GeneratorFileEntry)

**Key Architectural Decisions:**
- Template loading with override mechanism (`.override.hbs` files)
- Checksum-based incremental generation (MD5 validation)
- GLOB-based ignore patterns (`.generator-ignore`, `.generator-checksum-ignore`)
- ThreadLocal for helper context access (thread-safe parallel builds)
- SpringEL for expressions, Handlebars for templates

### Testing Strategy

**Approach:**
- Unit tests with JUnit 5
- Hamcrest matchers for readable assertions
- Temporary directories for file system tests
- @BeforeEach for fixture setup
- Guava ImmutableList for test data

**Coverage:**
- Core generation workflow (ModelGeneratorTest)
- Ignore pattern matching (GeneratorIgnoreTest, ChecksumIgnoreTest)
- Checksum validation and regeneration
- File operations and permissions
- GitIgnore synchronization

**Requirements:**
- All public APIs must have tests
- Test helpers independently
- Test failure scenarios (checksum mismatch, invalid patterns)
- Integration tests for full generation workflow

### Git Workflow

**Branching Strategy:**
- `develop` - Main development branch
- Feature branches: `feature/description`
- Bugfix branches: `bugfix/description`
- Release branches: `release/version`

**Commit Conventions:**
- Conventional Commits format: `type(scope): description`
- Types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`
- Include impact summary for major changes
- Reference issue numbers: `#123`

**Example:**
```
docs: Add comprehensive documentation and AI assistant guides

This commit introduces extensive documentation improvements...

- AGENTS.md: 800+ line comprehensive guide
- README.md: GitHub-compatible documentation
- Enhanced README.adoc with examples

Fixes #123
```

## Domain Context

### Template-Based Code Generation

This project is a **meta-framework** for building code generators. It doesn't generate specific code types; instead, it provides the infrastructure for:

1. **Loading template configurations** from YAML
2. **Evaluating expressions** using SpringEL
3. **Applying templates** using Handlebars
4. **Managing generated files** with checksums
5. **Supporting customization** through overrides and ignores

### Meta-Model Integration

The framework is used by three meta-model projects:
- **judo-meta-esm**: Entity State Machine generator
- **judo-meta-pam**: Platform Abstract Model generator
- **judo-meta-ui**: User Interface generator

Each meta-model project has:
- `generator-engine`: Binds meta-model to framework
- `generator-maven-project`: Maven plugin integration
- `generator-maven-plugin-test`: Test examples

### Generation Workflow Concepts

**Actor-Based Generation:**
- Templates can be called once per "actor type" (domain entity)
- Supports filtering with predicates
- Separate output directories per actor

**Expression Evaluation:**
- `#self`: Context-dependent (actor, iteration element, or model)
- `#model`: Always the model object
- `#actorType`: Current actor (when actorTypeBased=true)
- Helper methods: `#methodName()`

**File Management:**
- `.generated-files`: Index of non-actor files with checksums
- `.generated-files-[actor]`: Index per actor
- `.generator-ignore`: GLOB patterns to exclude files
- `.generator-checksum-ignore`: Allow overwriting modified files
- `.gitignore`: Synchronized with JUDO block markers

## Important Constraints

### Technical Constraints

1. **Java Version**: Must be Java 21 or higher
2. **Maven Version**: Requires Maven 3.9.4+
3. **OSGi Compatibility**: Bundle packaging required
4. **Thread Safety**: Helper methods must be thread-safe (use ThreadLocal)
5. **POSIX Permissions**: Only work on POSIX filesystems (gracefully degrade on Windows)

### Design Constraints

1. **Helper Method Signatures**: Maximum 1 parameter, must be public static
2. **Template Override Naming**: Must use `.override.hbs` extension
3. **Checksum Algorithm**: MD5 (cannot change without migration)
4. **GLOB Pattern Syntax**: Must follow .gitignore conventions
5. **Expression Language**: SpringEL only (no other expression languages)

### Performance Constraints

1. **Caching Required**: Use LoadingCache for expensive operations
2. **Parallel Generation**: Framework handles concurrency automatically
3. **Incremental Generation**: Must leverage checksum-based skipping
4. **Template Cache**: HighConcurrencyTemplateCache is mandatory

### Business Constraints

1. **License**: Eclipse Public License 2.0 (must maintain)
2. **Backward Compatibility**: Checksum files must remain compatible
3. **Documentation**: AGENTS.md and README must stay synchronized
4. **Test Coverage**: Critical paths must have tests

## External Dependencies

### Maven Central Dependencies

**Core:**
- `com.github.jknack:handlebars:4.4.0`
- `org.springframework:spring-expression:6.2.7`
- `com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.17.2`
- `io.github.classgraph:classgraph:4.8.152`
- `com.google.guava:guava:30.0-jre`

**OSGi:**
- `org.osgi:org.osgi.core:6.0.0`
- `org.osgi:osgi.cmpn:6.0.0`

**Testing:**
- `org.junit.jupiter:junit-jupiter:5.5.1`
- `org.hamcrest:hamcrest:2.1`
- `org.mockito:mockito-core:3.0.0`

### External Systems

**GitHub:**
- Repository: https://github.com/BlackBeltTechnology/judo-generator-commons
- Issue Tracker: https://github.com/BlackBeltTechnology/judo-generator-commons/issues
- CI/CD: GitHub Actions workflows

**Maven Repositories:**
- Maven Central (releases)
- JUDO Nexus: https://nexus.judo.technology/ (snapshots)

**Documentation Sites:**
- Handlebars.java: https://github.com/jknack/handlebars.java
- Spring EL: https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#expressions

### Related JUDO Projects

**Meta-Model Projects:**
- judo-meta-esm: https://github.com/BlackBeltTechnology/judo-meta-esm
- judo-meta-pam: https://github.com/BlackBeltTechnology/judo-meta-pam
- judo-meta-ui: https://github.com/BlackBeltTechnology/judo-meta-ui

**Community:**
- judo-community: https://github.com/BlackBeltTechnology/judo-community

## OpenSpec Workflow

This project uses OpenSpec for spec-driven development. See `openspec/AGENTS.md` for:
- Creating change proposals
- Implementing approved changes
- Archiving deployed changes
- Spec format and validation

**Key Files:**
- `openspec/project.md`: This file (project context)
- `openspec/AGENTS.md`: OpenSpec workflow guide
- `openspec/specs/`: Current specifications
- `openspec/changes/`: Proposed changes
- `openspec/changes/archive/`: Completed changes
