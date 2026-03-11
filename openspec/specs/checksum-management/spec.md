# Checksum Management Specification

## Purpose

The checksum management subsystem tracks MD5 checksums of all generated files to detect manual modifications, prevent accidental overwrites, and support incremental generation. It provides both strict validation and configurable bypass via ignore patterns.

## Architecture

The subsystem consists of:

- `ChecksumUtil` -- Computes MD5 checksums for byte arrays and files using `MessageDigest` and `DigestInputStream`, returning zero-padded 32-character hex strings
- `GeneratorFileEntry` -- Immutable value object associating a file path with its MD5 checksum, serialized as `path,checksum` CSV format in `.generated-files` index files
- `ChecksumIgnore` -- Reads `.generator-checksum-ignore` files containing glob patterns and determines whether a file's checksum validation should be skipped, using `LoadingCache` with 200-entry maximum

Index files:
- `.generated-files` -- Tracks checksums for common (non-discriminator) generated files
- `.generated-files-[discriminator]` -- Tracks checksums for discriminator-specific (actor-based) generated files

## Requirements

### Requirement: Calculate MD5 checksums

`ChecksumUtil` SHALL compute MD5 checksums for byte arrays and file paths, returning a zero-padded 32-character lowercase hexadecimal string.

#### Scenario: Checksum of byte array
- **GIVEN** a byte array with known content
- **WHEN** `ChecksumUtil.getMD5(byteArray)` is called
- **THEN** the correct 32-character MD5 hex string is returned

#### Scenario: Checksum of file on disk
- **GIVEN** a file at a valid `Path`
- **WHEN** `ChecksumUtil.getMD5(path)` is called
- **THEN** the MD5 hex string matching the file's content is returned

### Requirement: Persist and load file checksums

`GeneratorFileEntry` SHALL serialize to and deserialize from `path,checksum` CSV format. The `.generated-files` index file SHALL contain one entry per line.

#### Scenario: Serialize entry
- **GIVEN** a `GeneratorFileEntry` with path `"src/Actor.java"` and checksum `"abc123..."`
- **WHEN** `toString()` is called
- **THEN** the result is `"src/Actor.java,abc123..."`

#### Scenario: Deserialize entry
- **GIVEN** a string `"src/Actor.java,abc123..."`
- **WHEN** `GeneratorFileEntry.fromString(line)` is called
- **THEN** a `GeneratorFileEntry` with the correct path and checksum is returned

### Requirement: Validate checksums on generation

During `writeDirectory()`, the engine SHALL compare the on-disk checksum of each tracked file against the saved checksum in the `.generated-files` index. If they differ (indicating manual modification), the engine SHALL throw an `IllegalStateException` unless the file matches a `.generator-checksum-ignore` pattern.

#### Scenario: Checksum match allows overwrite
- **GIVEN** a generated file whose on-disk checksum matches the saved checksum
- **WHEN** the generator writes a new version of the file
- **THEN** the file is overwritten and the index is updated

#### Scenario: Checksum mismatch throws error
- **GIVEN** a generated file that was manually edited (on-disk checksum differs from saved)
- **WHEN** the generator attempts to write
- **THEN** an `IllegalStateException` is thrown with a message indicating the file was modified

#### Scenario: Checksum mismatch bypassed by checksum-ignore
- **GIVEN** a manually edited generated file whose path matches a pattern in `.generator-checksum-ignore`
- **WHEN** the generator attempts to write
- **THEN** the file is overwritten without error

### Requirement: Support checksum ignore patterns

`ChecksumIgnore` SHALL read `.generator-checksum-ignore` files containing glob patterns (same syntax as `.gitignore`) and determine whether a file path should bypass checksum validation.

#### Scenario: File matches checksum ignore pattern
- **GIVEN** a `.generator-checksum-ignore` file containing `**/*.custom.java`
- **WHEN** `checksumIgnore.shouldExcludeFile(path)` is called for `src/Model.custom.java`
- **THEN** `true` is returned

#### Scenario: File does not match checksum ignore
- **GIVEN** a `.generator-checksum-ignore` file containing `**/*.custom.java`
- **WHEN** `checksumIgnore.shouldExcludeFile(path)` is called for `src/Model.java`
- **THEN** `false` is returned

### Requirement: Update index after generation

After writing files, the engine SHALL update the `.generated-files` index with new MD5 checksums for all generated files, including newly created files and excluding deleted files.

#### Scenario: New file added to index
- **GIVEN** a generation run that produces a new file `src/NewFile.java`
- **WHEN** generation completes
- **THEN** the `.generated-files` index contains an entry for `src/NewFile.java` with its MD5 checksum

#### Scenario: Deleted file removed from index
- **GIVEN** a generation run where a previously tracked file is no longer produced
- **WHEN** generation completes
- **THEN** the file is deleted from disk and its entry is removed from `.generated-files`
