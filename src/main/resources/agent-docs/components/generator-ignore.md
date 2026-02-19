# Generator Ignore

The generator ignore system prevents overwriting developer-modified files using GLOB patterns.

## Overview

Generator ignore:
- Excludes files from generation based on patterns
- Uses same syntax as `.gitignore`
- Supports directory-level patterns
- Works alongside checksum validation

## Key Files

| File | Purpose |
|------|---------|
| `src/main/java/hu/blackbelt/judo/generator/commons/GeneratorIgnore.java` | Pattern matching |
| `src/main/java/hu/blackbelt/judo/generator/commons/ChecksumIgnore.java` | Checksum override |
| `src/main/java/hu/blackbelt/judo/generator/commons/GitIgnoreSynchronizer.java` | GitIgnore sync |

## Configuration Files

| File | Purpose |
|------|---------|
| `.generator-ignore` | Exclude files from generation |
| `.generator-checksum-ignore` | Allow overwriting modified files |

Both files use identical GLOB pattern syntax.

## Pattern Syntax

### Basic Patterns

```
# Exact file
src/main/CustomFile.java

# All files with extension
*.log
*.tmp

# Directory (trailing slash)
target/
build/
```

### Wildcards

| Pattern | Matches |
|---------|---------|
| `*` | Any characters except `/` |
| `**` | Any characters including `/` |
| `?` | Single character |

### Examples

```
# All Java files in any directory
**/*.java

# All files in docs directory (recursive)
docs/**

# Files starting with "test"
test*

# Single character wildcard
file?.txt  # Matches file1.txt, fileA.txt
```

### Exception Patterns

Use `!` prefix to negate a pattern:

```
# Ignore all generated files
generated/*

# But keep important.txt
!generated/important.txt
```

## Directory Hierarchy

Patterns are loaded from multiple levels:

```
project/
├── .generator-ignore      # Root patterns
├── src/
│   ├── .generator-ignore  # src/ patterns
│   └── main/
│       └── .generator-ignore  # src/main/ patterns
```

Patterns from all levels are combined, with more specific (deeper) patterns taking precedence.

## Behavior

### What Happens to Ignored Files

- File still tracked in `.generated-files` index
- Checksum validation still performed
- File writing is skipped
- Existing file preserved

### Difference from Checksum Ignore

| Aspect | `.generator-ignore` | `.generator-checksum-ignore` |
|--------|--------------------|-----------------------------|
| File tracked | Yes | Yes |
| Checksum validated | Yes | No |
| File written | No | Yes (overwritten) |
| Use case | Preserve manual edits | Allow regeneration |

## GitIgnore Synchronization

The generator can auto-update `.gitignore` with generated files:

### Markers

```
# JUDO GENERATOR BLOCK START
/generated/file1.java
/generated/file2.java
# JUDO GENERATOR BLOCK END
```

### Behavior

- Preserves existing `.gitignore` content
- Replaces only the JUDO block
- Adds block if not present
- Keeps generated files out of version control

## Common Patterns

### Ignore Custom Implementations

```
# .generator-ignore
src/main/java/custom/**
src/main/resources/override/**
```

### Ignore Generated Tests

```
# .generator-ignore
src/test/generated/**
```

### Allow Checksum Override for Config

```
# .generator-checksum-ignore
config/**/*.properties
application.yml
```

### Preserve Manual SQL

```
# .generator-ignore
src/main/resources/db/custom/*.sql
```

## Implementation Details

### Pattern Caching

`GeneratorIgnore` uses `LoadingCache` with:
- Maximum 200 entries
- Per-path pattern compilation
- Automatic eviction

### Pattern Loading

```java
GeneratorIgnore ignore = new GeneratorIgnore();
boolean excluded = ignore.shouldExcludeFile(filePath);
```

The loader:
1. Walks directory tree upward from file
2. Loads `.generator-ignore` at each level
3. Combines patterns
4. Caches result

## Troubleshooting

**Issue:** File keeps getting overwritten
- Check file path matches pattern exactly
- Verify `.generator-ignore` is in correct directory
- Test pattern: more specific paths need more specific patterns
- Remember: patterns are case-sensitive

**Issue:** Pattern not matching
- Use `**` for recursive matching
- Check trailing slashes for directories
- Verify no conflicting negation patterns
- Test with simpler pattern first

**Issue:** GitIgnore block duplicated
- Ensure only one JUDO GENERATOR BLOCK exists
- Check for manual copies of the block
- Let synchronizer manage the block

**Issue:** Checksum error despite ignore
- `.generator-ignore` doesn't bypass checksum validation
- Use `.generator-checksum-ignore` to skip validation
- Or use both files for complete exclusion
