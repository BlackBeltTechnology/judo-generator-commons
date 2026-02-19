# File Comparison Capability

## ADDED Requirements

### Requirement: File Type Normalizer Configuration

The system SHALL support configurable content normalizers per file type in the generator model YAML.

Normalizers SHALL be mapped to file extensions (e.g., `java`, `ts`, `tsx`, `kt`).

Each normalizer SHALL support boolean flags for common whitespace operations and optional regex patterns for language-specific normalization.

#### Scenario: Configure Java file normalizer
- **GIVEN** a generator model YAML with fileNormalizers section
- **WHEN** the YAML contains a normalizer for extension "java"
- **THEN** the normalizer SHALL be registered for .java files

#### Scenario: Multiple extensions per normalizer
- **GIVEN** a normalizer configured with extensions ["ts", "tsx"]
- **WHEN** a .ts or .tsx file is processed
- **THEN** the same normalizer settings SHALL be applied

### Requirement: Boolean Whitespace Normalizer Flags

The system SHALL support boolean flags for common whitespace normalization operations.

The following flags SHALL be supported:
- `removeDoubleSpaces` - Collapse multiple consecutive spaces to a single space
- `removeTabs` - Remove tab characters from content
- `removeNewLines` - Remove newline characters from content

All boolean flags SHALL default to `false` (disabled).

#### Scenario: Remove double spaces
- **GIVEN** a normalizer with `removeDoubleSpaces: true`
- **WHEN** content contains "hello    world" (multiple spaces)
- **THEN** the normalized content SHALL be "hello world" (single space)

#### Scenario: Remove tabs
- **GIVEN** a normalizer with `removeTabs: true`
- **WHEN** content contains tab characters
- **THEN** the tab characters SHALL be removed from normalized content

#### Scenario: Remove newlines
- **GIVEN** a normalizer with `removeNewLines: true`
- **WHEN** content contains newline characters
- **THEN** the newline characters SHALL be removed from normalized content

#### Scenario: Combine multiple flags
- **GIVEN** a normalizer with `removeDoubleSpaces: true`, `removeTabs: true`, and `removeNewLines: true`
- **WHEN** content contains tabs, multiple spaces, and newlines
- **THEN** all specified whitespace types SHALL be normalized

#### Scenario: Default flag values
- **GIVEN** a normalizer without explicit boolean flag settings
- **WHEN** normalization is applied
- **THEN** no whitespace normalization SHALL occur (flags default to false)

### Requirement: Regex Pattern Normalization

The system SHALL support optional regex patterns for language-specific content normalization.

Patterns SHALL support standard Java regex flags (MULTILINE, DOTALL, CASE_INSENSITIVE).

Patterns SHALL support custom replacement strings (default: empty string for removal).

#### Scenario: Remove import statements
- **GIVEN** a normalizer with pattern `^import\s+.*?;\s*$` and MULTILINE flag
- **WHEN** content contains `import java.util.List;`
- **THEN** the import statement SHALL be removed from normalized content

#### Scenario: Custom replacement string
- **GIVEN** a normalizer with pattern `\s+` and replacement ` `
- **WHEN** content contains multiple consecutive whitespace characters
- **THEN** the whitespace SHALL be replaced with a single space

#### Scenario: Pattern order preservation
- **GIVEN** a normalizer with multiple patterns
- **WHEN** normalization is applied
- **THEN** patterns SHALL be applied in the order they are configured

### Requirement: Normalization Order

The system SHALL apply normalizations in a consistent, defined order.

The normalization order SHALL be:
1. Line ending normalization (CRLF to LF)
2. Boolean flag normalizers (tabs, then double spaces, then newlines)
3. Custom regex patterns (in configured order)

#### Scenario: Consistent normalization order
- **GIVEN** a normalizer with boolean flags and regex patterns
- **WHEN** normalization is applied
- **THEN** line endings SHALL be normalized first
- **AND** boolean flags SHALL be applied second
- **AND** regex patterns SHALL be applied last

### Requirement: Fallback Normalized Comparison

The system SHALL perform normalized content comparison when checksum mismatch is detected.

The fallback comparison SHALL only occur for file types with configured normalizers.

#### Scenario: Checksum mismatch with equivalent normalized content
- **GIVEN** a generated file with checksum mismatch against saved checksum
- **AND** a normalizer configured for the file type
- **WHEN** the normalized content of both files is identical
- **THEN** the existing file SHALL be preserved (not overwritten)
- **AND** the checksum in .generated-files SHALL be updated to match the filesystem file

#### Scenario: Checksum mismatch with different normalized content
- **GIVEN** a generated file with checksum mismatch against saved checksum
- **AND** a normalizer configured for the file type
- **WHEN** the normalized content differs
- **THEN** the system SHALL proceed with existing checksum validation behavior

#### Scenario: No normalizer configured for file type
- **GIVEN** a generated file with checksum mismatch
- **AND** no normalizer configured for the file extension
- **WHEN** checksum validation occurs
- **THEN** the system SHALL use existing checksum-only validation behavior

### Requirement: File Timestamp Preservation

The system SHALL preserve file timestamps when normalized content matches.

The system SHALL only modify target files when actual content changes.

#### Scenario: Preserve timestamp on semantic equivalence
- **GIVEN** a generated file with checksum mismatch
- **AND** normalized content matches the existing file
- **WHEN** generation completes
- **THEN** the target file modification timestamp SHALL remain unchanged
- **AND** the checksum entry SHALL be updated

#### Scenario: Update timestamp on actual change
- **GIVEN** a generated file with checksum mismatch
- **AND** normalized content differs from the existing file
- **WHEN** generation completes
- **THEN** the target file SHALL be overwritten
- **AND** the modification timestamp SHALL be updated

### Requirement: Line Ending Normalization

The system SHALL always normalize line endings during content comparison.

Windows-style line endings (CRLF) SHALL be converted to Unix-style (LF) before any other normalization.

#### Scenario: CRLF to LF normalization
- **GIVEN** a filesystem file with CRLF line endings
- **AND** a generated file with LF line endings
- **WHEN** normalized comparison is performed
- **THEN** line endings SHALL be normalized to LF before other normalizations
- **AND** files with equivalent content SHALL be considered equal
