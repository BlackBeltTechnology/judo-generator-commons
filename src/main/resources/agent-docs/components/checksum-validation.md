# Checksum Validation

The checksum system detects manual modifications to generated files and prevents accidental overwrites.

## Overview

Checksum validation:
- Tracks MD5 hashes of generated files
- Detects when files have been manually modified
- Prevents accidental overwriting of customizations
- Supports whitespace-tolerant comparison for formatted files

## Key Files

| File | Purpose |
|------|---------|
| `src/main/java/hu/blackbelt/judo/generator/commons/ChecksumUtil.java` | MD5 calculation |
| `src/main/java/hu/blackbelt/judo/generator/commons/ChecksumIgnore.java` | Ignore patterns |
| `src/main/java/hu/blackbelt/judo/generator/commons/ContentComparator.java` | Normalized comparison |
| `src/main/java/hu/blackbelt/judo/generator/commons/FileNormalizerRegistry.java` | Normalizer registry |

## Index Files

Generated file checksums are stored in index files:

| File | Purpose |
|------|---------|
| `.generated-files` | Common (non-actor) files |
| `.generated-files-[actor]` | Actor-specific files |

### Format

```
path/to/file1.java,a1b2c3d4e5f6...
path/to/file2.java,f6e5d4c3b2a1...
```

Each line contains: `relative-path,md5-checksum`

## Validation Workflow

```
1. Read saved checksums from .generated-files
2. Calculate current filesystem checksums
3. For each file:
   ├─ Checksums match → File unchanged, proceed
   ├─ File in .generator-checksum-ignore → Allow overwrite
   ├─ Checksums differ:
   │   ├─ Normalizer configured → Compare normalized content
   │   │   ├─ Normalized match → Update checksum, proceed
   │   │   └─ Normalized differ → Error
   │   └─ No normalizer → Error
4. Write files and update checksums
```

## Checksum Ignore

The `.generator-checksum-ignore` file allows overwriting modified files:

```
# Allow overwriting these patterns
src/main/custom/*.java
config/**/*.properties
```

### Format

Same GLOB patterns as `.generator-ignore`:
- `*` matches any characters except `/`
- `**` matches any characters including `/`
- `?` matches single character

## Whitespace-Tolerant Comparison

For files modified by formatters (Prettier, IDE auto-format), configure normalizers:

### YAML Configuration

```yaml
fileNormalizers:
  # Auto-detect all supported languages
  - preset: auto
  
  # Or specific presets
  - preset: java
  - preset: ts
  
  # Or custom configuration
  - extensions: [java, kt]
    removeDoubleSpaces: true
    removeTabs: true
    patterns:
      - pattern: "^import\\s+.*?;\\s*$"
        flags: [MULTILINE]
```

### Built-in Presets

| Preset | Extensions | Description |
|--------|------------|-------------|
| `auto` | All below | Auto-detect by extension |
| `java` | .java | Remove imports, collapse whitespace |
| `ts` | .ts, .tsx | Remove imports, collapse whitespace |
| `js` | .js, .jsx, .mjs, .cjs | Remove imports, collapse whitespace |
| `rust` | .rs | Remove use statements |
| `go` | .go | Remove imports |
| `python` | .py | Remove imports |

### Normalization Order

1. Line endings: CRLF → LF (always)
2. Boolean flags: tabs → double spaces → newlines
3. Custom regex patterns (in configured order)

## Checksum Operations

### Reset Checksums

Clear validation state after intentional manual edits:

```java
ModelGenerator.resetChecksums(param);
```

This removes `.generated-files` index, allowing fresh generation.

### Recalculate Checksums

Update checksums to match current filesystem without regenerating:

```java
ModelGenerator.recalculateChecksumToDirectory(param);
```

Useful after manual edits you want to "accept".

### Clean Generated Files

Remove all files tracked in checksum index:

```java
ModelGenerator.cleanGeneratedFromChecksum(param);
```

## Common Patterns

### Accepting Manual Changes

After intentionally editing a generated file:

1. **Option A:** Add to `.generator-checksum-ignore`
   ```
   src/main/java/CustomizedFile.java
   ```

2. **Option B:** Recalculate checksums
   ```java
   ModelGenerator.recalculateChecksumToDirectory(param);
   ```

3. **Option C:** Reset and regenerate
   ```java
   ModelGenerator.resetChecksums(param);
   ModelGenerator.generateToDirectory(param);
   ```

### Handling Formatter Changes

If IDE or CI formatter modifies whitespace:

```yaml
fileNormalizers:
  - preset: auto  # Handles common languages
```

This allows checksum validation to pass despite whitespace differences.

## Troubleshooting

**Issue:** `IllegalStateException: Generated file modified`
- File was edited after generation
- Solutions:
  - Revert: `git checkout -- path/to/file`
  - Delete: `rm path/to/file`
  - Ignore: Add to `.generator-checksum-ignore`
  - Accept: Run `recalculateChecksumToDirectory()`
  - Reset: Run `resetChecksums()`

**Issue:** Checksum mismatch after formatting
- Configure `fileNormalizers` in generator model
- Use appropriate preset for your language
- Check that normalizer is applied to correct extensions

**Issue:** `.generated-files` keeps changing
- Check for non-deterministic generation (timestamps, UUIDs)
- Ensure consistent line endings (configure Git)
- Verify helpers produce consistent output
