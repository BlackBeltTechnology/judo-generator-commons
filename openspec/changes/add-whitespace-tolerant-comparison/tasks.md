# Tasks: Add Whitespace-Tolerant File Comparison

## 1. Core Classes

- [x] 1.1 Create `NormalizerPattern.java` - POJO for regex pattern with flags and replacement
- [x] 1.2 Create `FileTypeNormalizer.java` - Normalizer configuration with boolean flags and patterns
  - `extensions: List<String>`
  - `removeDoubleSpaces: boolean`
  - `removeTabs: boolean`
  - `removeNewLines: boolean`
  - `patterns: List<NormalizerPattern>`
- [x] 1.3 Create `FileNormalizerRegistry.java` - Registry managing normalizers by extension
- [x] 1.4 Create `ContentComparator.java` - Utility for comparing normalized content
- [x] 1.5 Create `NormalizerPresets.java` - Built-in preset definitions for common languages

## 2. YAML Configuration

- [x] 2.1 Add `fileNormalizers` field to `GeneratorModel.java`
- [x] 2.2 Create Jackson deserializer support for normalizer configuration (including `preset` field)
- [x] 2.3 Add `FileNormalizerRegistry` initialization from `GeneratorModel`
- [x] 2.4 Implement preset resolution (expand `preset: java` to full configuration)

## 3. Integration

- [x] 3.1 Pass `FileNormalizerRegistry` to `writeDirectory()` method
- [x] 3.2 Add fallback normalized comparison in checksum mismatch handler
- [x] 3.3 Implement normalization order (line endings → boolean flags → regex patterns)
- [x] 3.4 Implement logic to skip file write when normalized content matches
- [x] 3.5 Update checksum entry when normalized match detected
- [x] 3.6 Add file extension extraction utility
- [x] 3.7 Add INFO level logging when checksum is auto-updated due to normalized match

## 4. Testing

- [x] 4.1 Unit tests for `NormalizerPattern` (regex compilation, replacement)
- [x] 4.2 Unit tests for `FileTypeNormalizer` boolean flags:
  - `removeDoubleSpaces` - multiple spaces to single
  - `removeTabs` - tab removal (replace with empty string)
  - `removeNewLines` - newline removal
  - Combined flags
- [x] 4.3 Unit tests for `FileTypeNormalizer` regex patterns
- [x] 4.4 Unit tests for `FileNormalizerRegistry` (extension mapping)
- [x] 4.5 Unit tests for `ContentComparator` (full comparison flow)
- [x] 4.6 Unit tests for normalization order
- [x] 4.7 Unit tests for `NormalizerPresets` (all presets: auto, java, ts, js, rust, go, python)
- [x] 4.8 Unit tests for preset YAML configuration parsing
- [x] 4.9 Integration test: checksum mismatch with equivalent normalized content
- [x] 4.10 Integration test: checksum mismatch with different normalized content
- [x] 4.11 Integration test: file timestamp preservation
- [x] 4.12 Integration test: verify INFO log on checksum auto-update

## 5. Documentation

- [x] 5.1 Add YAML configuration examples to README.md
- [x] 5.2 Update AGENTS.md with new feature documentation
- [x] 5.3 Document all preset configurations (java, ts, js, rust, go, python)

## Dependencies

- Task 2.x depends on Task 1.x (configuration requires POJOs)
- Task 3.x depends on Task 2.x (integration requires configuration support)
- Task 4.x can partially run in parallel with Task 3.x
- Task 5.x can start after Task 3.x is complete
