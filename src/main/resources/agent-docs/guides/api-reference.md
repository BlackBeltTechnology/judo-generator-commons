# API Reference

Detailed API documentation for JUDO Generator Commons.

## ModelGenerator

Central orchestrator for code generation.

**Package:** `hu.blackbelt.judo.generator.commons`

### Static Methods

#### generateToDirectory

```java
public static void generateToDirectory(GeneratorParameter generatorParameter)
```

Main entry point for code generation.

| Parameter | Type | Description |
|-----------|------|-------------|
| generatorParameter | GeneratorParameter | Configuration for generation |

**Throws:** `IllegalStateException` if checksum validation fails

**Behavior:**
1. Executes `performExecutor` to generate files
2. Writes actor-based files to discriminator directories
3. Writes common files to target directory
4. Validates and updates checksums

---

#### createGeneratorContext

```java
public static ModelGeneratorContext createGeneratorContext(
    ChainedURLTemplateLoader templateLoader,
    URLResolver uriResourceResolver,
    GeneratorModel generatorModel,
    Collection<Class<?>> helpers,
    Collection<Class<? extends ValueResolver>> valueResolvers,
    Class<?> contextAccessor
)
```

Creates a configured generator context.

| Parameter | Type | Description |
|-----------|------|-------------|
| templateLoader | ChainedURLTemplateLoader | Template loading chain |
| uriResourceResolver | URLResolver | URI resource resolution |
| generatorModel | GeneratorModel | Loaded template model |
| helpers | Collection<Class<?>> | Helper classes |
| valueResolvers | Collection<Class<? extends ValueResolver>> | Handlebars value resolvers |
| contextAccessor | Class<?> | Context accessor class (nullable) |

**Returns:** Configured `ModelGeneratorContext`

---

#### resetChecksums

```java
public static void resetChecksums(GeneratorParameter generatorParameter)
```

Clears all checksum validation state.

**Use case:** After intentional manual edits to generated files.

---

#### recalculateChecksumToDirectory

```java
public static void recalculateChecksumToDirectory(GeneratorParameter generatorParameter)
```

Updates checksums to match current filesystem without regenerating.

**Use case:** Accept manual changes without regenerating files.

---

#### cleanGeneratedFromChecksum

```java
public static void cleanGeneratedFromChecksum(GeneratorParameter generatorParameter)
```

Removes all files tracked in the checksum index.

**Use case:** Clean generated files before fresh generation.

---

## GeneratorParameter

Configuration container for generation (Builder pattern).

### Builder Methods

```java
GeneratorParameter.builder()
    .generatorContext(context)           // Required
    .targetDirectory(dir)                // Required
    .performExecutor(executor)           // Required
    .validateChecksum(true)              // Default: true
    .extraContextVariables(map)          // Optional
    .actorTypePredicate(predicate)       // Optional
    .actorTypeTargetDirectoryResolver(resolver)  // For actor-based
    .build();
```

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| generatorContext | ModelGeneratorContext | Yes | Engine configuration |
| targetDirectory | File | Yes | Output directory |
| performExecutor | Function<GeneratorParameter, GeneratorResult> | Yes | Generation logic |
| validateChecksum | Boolean | No | Enable checksum validation (default: true) |
| extraContextVariables | Map<String, Object> | No | Additional SpringEL variables |
| actorTypePredicate | Predicate<ActorType> | No | Filter actor types |
| actorTypeTargetDirectoryResolver | Function<ActorType, File> | No | Per-actor output directory |

---

## GeneratorModel

Template collection loaded from YAML.

### Static Methods

#### loadYamlURL

```java
public static GeneratorModel loadYamlURL(URL yamlUrl)
```

Loads template model from YAML file.

| Parameter | Type | Description |
|-----------|------|-------------|
| yamlUrl | URL | URL to YAML descriptor |

**Returns:** Loaded `GeneratorModel`

---

#### overrideTemplates

```java
public GeneratorModel overrideTemplates(GeneratorModel override)
```

Merges templates from another model.

| Parameter | Type | Description |
|-----------|------|-------------|
| override | GeneratorModel | Model with override templates |

**Returns:** New merged `GeneratorModel`

**Behavior:**
- Templates with same `name` are replaced
- New templates are added
- Existing templates not in override are preserved

---

## GeneratorTemplate

Single template configuration.

### Properties

| Property | Type | Description |
|----------|------|-------------|
| name | String | Unique template identifier |
| pathExpression | String | SpringEL expression for output path |
| templateName | String | Handlebars template path |
| actorTypeBased | Boolean | Iterate over actor types |
| factoryExpression | String | SpringEL for collection iteration |
| conditionExpression | String | SpringEL boolean for conditional generation |
| permission | String | POSIX permission string |
| copy | Boolean | Binary copy mode (no template processing) |
| exclude | Boolean | Exclude from generation (for overrides) |
| templateContext | List<TemplateSpringELExpression> | Additional variables |

---

## ChainedURLTemplateLoader

Template loader with override support.

### Static Methods

#### createFromURIs

```java
public static ChainedURLTemplateLoader createFromURIs(
    List<URI> uris,
    String prefix
)
```

Creates loader chain from URIs.

| Parameter | Type | Description |
|-----------|------|-------------|
| uris | List<URI> | URIs to search (first has highest priority) |
| prefix | String | Template path prefix |

**Returns:** Configured loader chain

**Override behavior:**
- Template `foo.hbs` can be overridden by `foo.override.hbs`
- First URI with matching template wins

---

## TemplateHelperFinder

Discovers helper classes by annotation.

### Static Methods

#### collectHelpers

```java
public static Collection<Class<?>> collectHelpers(
    String packageName,
    ClassLoader classLoader
)
```

Scans for `@TemplateHelper` annotated classes.

| Parameter | Type | Description |
|-----------|------|-------------|
| packageName | String | Package to scan |
| classLoader | ClassLoader | ClassLoader for scanning |

**Returns:** Collection of helper classes

---

## ThreadLocalContextHolder

Thread-safe context storage for helpers.

### Static Methods

#### bindContext

```java
public static void bindContext(Map<String, ?> context)
```

Binds context to current thread.

---

#### getVariable

```java
public static Object getVariable(String name)
```

Retrieves variable from thread-local context.

| Parameter | Type | Description |
|-----------|------|-------------|
| name | String | Variable name |

**Returns:** Variable value or null

---

#### clearContext

```java
public static void clearContext()
```

Clears thread-local context.

---

## Annotations

### @TemplateHelper

Marks a class as a Handlebars/SpringEL helper.

```java
@TemplateHelper
public class MyHelper extends StaticMethodValueResolver {
    // public static methods become helpers
}
```

**Requirements:**
- Class must have default constructor
- Should extend `StaticMethodValueResolver`
- Methods must be `public static`
- Methods take 0 or 1 parameter

---

### @ContextAccessor

Marks a helper class that needs context access.

```java
@TemplateHelper
@ContextAccessor
public class MyHelper extends StaticMethodValueResolver {
    
    public static void bindContext(Map<String, ?> context) {
        ThreadLocalContextHolder.bindContext(context);
    }
}
```

**Requirements:**
- Must also have `@TemplateHelper`
- Must implement `bindContext(Map<String, ?> context)` method

---

## ChecksumUtil

MD5 checksum utilities.

### Static Methods

#### getMD5

```java
public static String getMD5(byte[] content)
```

Calculates MD5 hash of content.

| Parameter | Type | Description |
|-----------|------|-------------|
| content | byte[] | Content to hash |

**Returns:** 32-character lowercase hex string

---

#### getMD5

```java
public static String getMD5(Path file)
```

Calculates MD5 hash of file.

| Parameter | Type | Description |
|-----------|------|-------------|
| file | Path | File to hash |

**Returns:** 32-character lowercase hex string

---

## GeneratorIgnore

GLOB-based file exclusion.

### Methods

#### shouldExcludeFile

```java
public boolean shouldExcludeFile(Path filePath)
```

Checks if file matches ignore patterns.

| Parameter | Type | Description |
|-----------|------|-------------|
| filePath | Path | File to check |

**Returns:** `true` if file should be excluded

**Behavior:**
- Loads patterns from `.generator-ignore` files
- Walks directory tree upward
- Combines patterns from all levels
- Caches results (max 200 entries)

---

## ContentComparator

Whitespace-tolerant content comparison.

### Methods

#### areEquivalent

```java
public boolean areEquivalent(byte[] content1, byte[] content2, String extension)
```

Compares content after normalization.

| Parameter | Type | Description |
|-----------|------|-------------|
| content1 | byte[] | First content |
| content2 | byte[] | Second content |
| extension | String | File extension for normalizer lookup |

**Returns:** `true` if normalized content matches

---

## FileNormalizerRegistry

Registry for file normalizers.

### Methods

#### forExtension

```java
public Optional<FileTypeNormalizer> forExtension(String extension)
```

Gets normalizer for file extension.

| Parameter | Type | Description |
|-----------|------|-------------|
| extension | String | File extension (without dot) |

**Returns:** Optional normalizer

---

#### registerPreset

```java
public void registerPreset(String preset)
```

Registers built-in preset.

| Parameter | Type | Description |
|-----------|------|-------------|
| preset | String | Preset name: auto, java, ts, js, rust, go, python |
