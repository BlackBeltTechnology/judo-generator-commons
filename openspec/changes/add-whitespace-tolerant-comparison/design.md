# Design: Whitespace-Tolerant File Comparison

## Context

The JUDO Generator Commons framework generates code files that are often processed by external tools (formatters, IDEs, compilers) which modify whitespace and reorder imports. The current checksum-based validation detects these as modifications, causing false positives when builds fail before checksum regeneration occurs.

**Stakeholders:**
- Generator engine developers
- Build pipeline maintainers
- Developers using generated code

**Constraints:**
- Must remain backward compatible with existing `.generated-files` format
- Must not significantly impact generation performance
- Must support multiple programming languages with different formatting conventions
- Configuration must integrate with existing YAML template descriptor format

## Goals / Non-Goals

**Goals:**
- Provide fallback comparison when checksums don't match
- Support simple boolean flags for common whitespace normalizations
- Support configurable regex patterns for language-specific normalization (e.g., imports)
- Preserve file timestamps when content is semantically equivalent
- Automatically update checksums when normalized content matches
- Keep existing checksum-first approach for performance

**Non-Goals:**
- Full AST-based semantic comparison (too complex, language-specific)
- Binary file semantic comparison
- Changing the checksum algorithm
- Auto-detecting normalizer patterns (explicit configuration required)

## Decisions

### Decision 1: Boolean Flags for Common Whitespace Normalizers

**What:** Provide simple boolean flags for frequently needed whitespace normalizations

**Flags:**
- `removeDoubleSpaces: true` - Collapse `  +` (multiple spaces) to single space
- `removeTabs: true` - Remove tab characters (replace with empty or space)
- `removeNewLines: true` - Remove/normalize newline characters

**Why:**
- Simple and intuitive configuration
- No regex knowledge required for common cases
- Covers 90% of whitespace formatting differences
- Easy to enable/disable per file type

**YAML Example:**
```yaml
fileNormalizers:
  - extensions: [java, kt]
    removeDoubleSpaces: true
    removeTabs: true
    removeNewLines: true
    patterns:
      - pattern: "^import\\s+.*?;\\s*$"
        flags: [MULTILINE]
  - extensions: [ts, tsx]
    removeDoubleSpaces: true
    removeTabs: true
    removeNewLines: true
    patterns:
      - pattern: "^import\\s+.*?['\"];?\\s*$"
        flags: [MULTILINE]
```

### Decision 2: Regexp-Based Patterns for Language-Specific Content

**What:** Use configurable regular expressions to strip language-specific non-semantic content (e.g., imports)

**Why:** 
- Language-agnostic approach
- Simple to configure for specific needs
- Fast execution compared to AST parsing
- Complements boolean flags for edge cases

**Alternatives considered:**
- AST-based parsing: Too complex, requires per-language parsers
- Diff algorithms: Doesn't solve the import reordering problem

### Decision 3: Extension-Based File Type Mapping

**What:** Map file extensions to normalizer configurations

**Why:**
- Simple and intuitive mapping
- Covers common use cases (`.java`, `.ts`, `.tsx`, `.kt`)
- Easy to configure in YAML

### Decision 4: Normalization Order

**What:** Apply normalizations in a consistent order

**Order:**
1. Line ending normalization (CRLF → LF) - always applied first
2. Boolean flag normalizers (in order: tabs → double spaces → newlines)
3. Custom regex patterns (in configured order)

**Why:**
- Predictable behavior
- Line endings must be normalized before other operations
- Boolean flags handle common cases before custom patterns

### Decision 5: Fallback Strategy (Checksum First, Then Normalize)

**What:** Only perform normalized comparison when checksum mismatch is detected

**Why:**
- Preserves performance for unchanged files (checksum match = skip)
- Normalized comparison only happens when needed
- No overhead for files that haven't been reformatted

**Flow:**
```
1. Calculate filesystem checksum
2. Compare with saved checksum
3. If match → skip (no changes)
4. If mismatch:
   a. Load filesystem file content
   b. Load generated file content (in-memory)
   c. Apply normalizers to both (boolean flags + patterns)
   d. Compare normalized content
   e. If match → keep existing file, update checksum
   f. If mismatch → proceed with existing logic (error or overwrite)
```

### Decision 6: Timestamp Preservation

**What:** Only write files when actual content changes (not just formatting)

**Why:**
- Prevents unnecessary compiler recompilation
- Build tools often use timestamps for incremental builds
- Reduces build times significantly

**Implementation:**
- When normalized content matches, don't touch the file at all
- Only update the `.generated-files` checksum entry
- When content truly differs, write file normally

### Decision 7: YAML Configuration Location

**What:** Add `fileNormalizers` section to the existing generator model YAML

**Why:**
- Keeps all generation configuration in one place
- Follows existing pattern for global configuration
- Can be overridden in template-specific YAML files

### Decision 8: Language Presets

**What:** Provide built-in preset configurations for common languages

**Presets:**
- `auto` - Automatically detect and apply preset based on file extension
- `java` - Java files (.java)
- `ts` - TypeScript files (.ts, .tsx)
- `js` - JavaScript files (.js, .jsx, .mjs, .cjs)
- `rust` - Rust files (.rs)
- `go` - Go files (.go)
- `python` - Python files (.py)

**YAML Example:**
```yaml
fileNormalizers:
  # Simplest configuration - auto-detect for all supported extensions
  - preset: auto
```

```yaml
fileNormalizers:
  # Or use specific presets
  - preset: java
  - preset: ts
  - preset: go
  # Can still use explicit configuration alongside presets
  - extensions: [kt]
    removeDoubleSpaces: true
    removeTabs: true
```

**Preset Definitions:**

| Preset | Extensions | removeDoubleSpaces | removeTabs | removeNewLines | Import Pattern |
|--------|------------|-------------------|------------|----------------|----------------|
| auto | (all below) | (per extension) | (per extension) | (per extension) | (per extension) |
| java | java | true | true | false | `^import\s+.*?;\s*$` |
| ts | ts, tsx | true | true | false | `^import\s+.*?['"];?\s*$` |
| js | js, jsx, mjs, cjs | true | true | false | `^import\s+.*?['"];?\s*$` |
| rust | rs | true | true | false | `^use\s+.*?;\s*$` |
| go | go | true | true | false | `^import\s+.*$` (multiline block support) |
| python | py | true | true | false | `^(import\s+.*\|from\s+.*import.*)$` |

**Auto Preset Behavior:**
- When `preset: auto` is configured, the system registers all language presets
- At comparison time, the file extension is used to look up the appropriate normalizer
- Files with unrecognized extensions are skipped (no normalization applied)

**Why:**
- Reduces configuration boilerplate for common languages
- `auto` provides zero-configuration experience for most projects
- Provides sensible defaults based on language conventions
- Users can still override or extend with explicit configuration

### Decision 9: Logging Level

**What:** Use INFO level logging when checksums are auto-updated due to normalized content match

**Why:**
- Provides visibility into when normalization fallback is used
- Helps developers understand why checksums changed
- INFO level is appropriate as it's an actionable event (checksum was updated)

**Log message format:**
```
INFO: File '{}' has equivalent normalized content, updating checksum
```

## Component Design

### New Classes

```
FileTypeNormalizer
├── extensions: List<String>
├── removeDoubleSpaces: boolean (default: false)
├── removeTabs: boolean (default: false)
├── removeNewLines: boolean (default: false)
├── patterns: List<NormalizerPattern>
└── normalize(String content): String

NormalizerPattern
├── pattern: String (regex)
├── replacement: String (default: "")
└── flags: List<PatternFlag> (MULTILINE, DOTALL, etc.)

FileNormalizerRegistry
├── normalizers: Map<String, FileTypeNormalizer>
├── register(FileTypeNormalizer): void
└── getNormalizer(String extension): Optional<FileTypeNormalizer>

ContentComparator
├── registry: FileNormalizerRegistry
├── compareNormalized(String ext, byte[] a, byte[] b): boolean
└── normalize(String ext, byte[] content): String
```

### Normalization Implementation

```java
public String normalize(String content) {
    String result = content;
    
    // 1. Always normalize line endings first
    result = result.replace("\r\n", "\n");
    
    // 2. Apply boolean flag normalizers
    if (removeTabs) {
        result = result.replace("\t", "");
    }
    if (removeDoubleSpaces) {
        result = result.replaceAll("  +", " ");
    }
    if (removeNewLines) {
        result = result.replace("\n", "");
    }
    
    // 3. Apply custom regex patterns in order
    for (NormalizerPattern pattern : patterns) {
        result = pattern.apply(result);
    }
    
    return result;
}
```

### Integration Points

1. **GeneratorModel.java**: Add `fileNormalizers` field, deserialize from YAML
2. **ModelGenerator.writeDirectory()**: Add normalized comparison fallback
3. **ChecksumUtil.java**: Add `getNormalizedMD5()` method (optional optimization)

## Risks / Trade-offs

### Risk: Aggressive Normalization

**Risk:** Enabling all boolean flags might make semantically different files appear equal

**Mitigation:** 
- Flags are opt-in (default: false)
- Document when each flag is appropriate
- Combine with regex patterns for precision

### Risk: Performance Impact

**Risk:** Reading and normalizing files on every mismatch could slow builds

**Mitigation:**
- Only trigger on checksum mismatch (rare case)
- Boolean flags use simple string operations (fast)
- Cache compiled Pattern instances for regex

### Risk: Import Order Significance

**Risk:** In some cases, import order matters (e.g., CSS imports, side-effect imports)

**Mitigation:**
- Import stripping is optional (via patterns, not boolean flags)
- Document when to use/avoid import normalization
- Users configure patterns for their specific needs

## Migration Plan

1. **Phase 1: Core Implementation**
   - Add FileNormalizer classes with boolean flags
   - Add YAML configuration support
   - No behavior change without configuration

2. **Phase 2: Integration**
   - Integrate into writeDirectory() logic
   - Add fallback comparison flow
   - Update checksum on semantic match

3. **Phase 3: Documentation**
   - Document YAML configuration
   - Provide example configurations for Java, TypeScript, Kotlin
   - Update AGENTS.md with new feature

**Rollback:** Remove `fileNormalizers` from YAML to disable feature (backward compatible)

## Resolved Questions

1. **Q:** Should `removeTabs` replace with empty string or single space?
   **Decision:** Empty string.

2. **Q:** Should normalized comparison apply to `.generator-checksum-ignore` files?
   **Decision:** No, checksum-ignore files already bypass validation entirely.

3. **Q:** Should we provide preset configurations (e.g., `preset: java`)?
   **Decision:** Yes, provide presets for: `js`, `ts`, `java`, `rust`, `go`, `python`.

4. **Q:** What happens if normalization results in empty content for both files?
   **Decision:** They are considered equal (no special handling needed).

5. **Q:** What level of logging is expected when normalized comparison is used?
   **Decision:** INFO level logs when a checksum is auto-updated due to normalized match.
