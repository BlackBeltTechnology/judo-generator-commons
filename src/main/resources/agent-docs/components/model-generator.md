# Model Generator

The ModelGenerator is the central orchestrator for the code generation workflow.

## Overview

ModelGenerator provides static methods that coordinate the entire generation process:
- Template evaluation and file generation
- Checksum validation and management
- File writing with permission handling
- Actor-based directory organization

## Key Files

| File | Purpose |
|------|---------|
| `src/main/java/hu/blackbelt/judo/generator/commons/ModelGenerator.java` | Central orchestrator |
| `src/main/java/hu/blackbelt/judo/generator/commons/ModelGeneratorContext.java` | State container |
| `src/main/java/hu/blackbelt/judo/generator/commons/GeneratorParameter.java` | Configuration builder |
| `src/main/java/hu/blackbelt/judo/generator/commons/GeneratorResult.java` | Result container |

## Usage Examples

### Basic Generation

```java
ModelGeneratorContext context = ModelGenerator.createGeneratorContext(
    templateLoader,
    urlResolver,
    generatorModel,
    helperClasses,
    valueResolvers,
    contextAccessorClass
);

GeneratorParameter param = GeneratorParameter.builder()
    .generatorContext(context)
    .targetDirectory(targetDir)
    .performExecutor(p -> generateResult(p))
    .validateChecksum(true)
    .build();

ModelGenerator.generateToDirectory(param);
```

### Checksum Operations

```java
// Reset all checksums (after intentional manual edits)
ModelGenerator.resetChecksums(param);

// Recalculate checksums without regenerating
ModelGenerator.recalculateChecksumToDirectory(param);

// Clean generated files based on checksum index
ModelGenerator.cleanGeneratedFromChecksum(param);
```

## Common Patterns

### Entry Points

| Method | Purpose |
|--------|---------|
| `generateToDirectory()` | Main generation entry point |
| `resetChecksums()` | Clear validation state |
| `recalculateChecksumToDirectory()` | Refresh checksums |
| `cleanGeneratedFromChecksum()` | Remove tracked files |

### Generation Flow

```
1. generateToDirectory(GeneratorParameter)
   ├─ Execute performExecutor → GeneratorResult
   ├─ Write actor-based files (per discriminator)
   └─ Write common files
   
2. writeDirectory()
   ├─ Load ignore patterns
   ├─ Load checksum ignore patterns
   ├─ Validate checksums
   ├─ Write files with permissions
   └─ Update checksum index
```

### Builder Pattern

GeneratorParameter uses Lombok @Builder:

```java
GeneratorParameter.builder()
    .generatorContext(context)        // Required
    .targetDirectory(dir)             // Required
    .performExecutor(executor)        // Required
    .validateChecksum(true)           // Optional, default true
    .extraContextVariables(map)       // Optional
    .build();
```

## Troubleshooting

**Issue:** `IllegalStateException: Generated file modified`
- File was manually edited after generation
- Solutions: revert file, add to `.generator-ignore`, or run `resetChecksums()`

**Issue:** Files not being generated
- Check `conditionExpression` in template YAML
- Verify ignore patterns don't exclude the file
- Enable DEBUG logging for detailed output

**Issue:** Wrong target directory
- Verify `targetDirectory` in GeneratorParameter
- For actor-based files, check `actorTypeTargetDirectoryResolver`
