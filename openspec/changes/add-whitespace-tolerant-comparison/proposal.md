# Change: Add Whitespace-Tolerant File Comparison

## Why

The current checksum validation approach has a fundamental timing problem in the build pipeline:

1. **Problem**: After generation, compilers/formatters (e.g., Prettier, IDE auto-format) modify generated files by reformatting whitespace and reordering imports
2. **Current workaround**: Checksum regeneration is called after formatting
3. **Issue**: When builds fail before checksum regeneration, the checksums are never updated, causing false checksum mismatch errors on subsequent builds
4. **Result**: Developers must manually reset checksums or delete files, disrupting workflow

A whitespace-tolerant comparison as a fallback when checksums don't match would solve this by:
- Detecting semantically equivalent files despite formatting differences
- Preserving existing files when content is logically the same (avoiding compiler recompilation)
- Auto-updating checksums when semantic equivalence is confirmed

## What Changes

- Add configurable content normalizers per file type (based on extension)
- Introduce boolean flags for common whitespace normalizers:
  - `removeDoubleSpaces` - Collapse multiple consecutive spaces to single space
  - `removeTabs` - Remove tab characters (replace with empty string)
  - `removeNewLines` - Remove newline characters
- Introduce regexp-based patterns for stripping language-specific content (e.g., import statements)
- Add fallback comparison when checksum mismatch is detected
- Update checksum automatically when normalized content matches
- Only touch target files when actual content changes (preserve timestamps for compilers)
- New YAML configuration section for file type normalizers
- Built-in language presets for common languages: `java`, `ts`, `js`, `rust`, `go`, `python`
- `auto` preset that detects the appropriate normalizer based on target file extension
- INFO level logging when checksum is auto-updated due to normalized match

## Impact

- Affected specs: New capability `file-comparison` (no existing specs to modify)
- Affected code:
  - `ModelGenerator.java` - Add fallback comparison logic in `writeDirectory()`
  - `ChecksumUtil.java` - Add normalized content comparison utilities
  - `GeneratorModel.java` - Add file normalizer configuration
  - New `FileNormalizer.java` - Normalizer pattern registry
  - New `FileTypeNormalizer.java` - Per-extension normalizer configuration
  - New `NormalizerPresets.java` - Built-in preset definitions
- Affected documentation:
  - `README.md` - Add YAML configuration examples
  - `AGENTS.md` - Document new feature and presets

## Non-Goals

- Full semantic parsing of source files (we use regexp patterns, not AST)
- Support for binary file comparison (existing checksum-only approach remains)
- Changing the default checksum algorithm (MD5)
