# test-coverage Specification

## Purpose
TBD - created by archiving change add-real-world-test-cases. Update Purpose after archive.
## Requirements
### Requirement: Real-World Java Project Generation Tests

The test suite SHALL include tests that verify code generation for realistic Java project structures using standard Maven conventions.

#### Scenario: Generate Java entity with Maven directory structure

**Given** a generated file collection with a Java entity file at path `src/main/java/com/example/model/User.java`
**And** the file contains realistic Java code with package declaration, imports, and class definition
**When** `writeDirectory()` is called with checksum validation enabled
**Then** the file is created at the correct path
**And** the `.generated-files` index contains the file with correct checksum

#### Scenario: Generate multiple related Java files

**Given** a generated file collection with entity, repository, and service Java files
**When** `writeDirectory()` is called
**Then** all files are created in their respective packages
**And** all files are indexed in `.generated-files`

#### Scenario: Regenerate Java files after content change

**Given** Java files that were previously generated
**And** the generated content has been modified (e.g., new field added)
**When** `writeDirectory()` is called with the updated content
**Then** the files are updated with new content
**And** the checksums in `.generated-files` are updated

---

### Requirement: Real-World TypeScript Project Generation Tests

The test suite SHALL include tests that verify code generation for realistic TypeScript/React project structures.

#### Scenario: Generate TypeScript React component

**Given** a generated file collection with a React component at path `src/components/UserCard.tsx`
**And** the file contains realistic TypeScript code with imports and JSX
**When** `writeDirectory()` is called
**Then** the file is created at the correct path
**And** the content matches the expected TypeScript syntax

#### Scenario: Generate multiple frontend files

**Given** a generated file collection with component, hook, and type definition files
**When** `writeDirectory()` is called
**Then** all files are created following frontend conventions

---

### Requirement: Formatter Modification Simulation Tests

The test suite SHALL include tests that simulate code formatter modifications to generated files.

#### Scenario: Detect checksum mismatch after formatter changes

**Given** a Java file that was previously generated
**And** a code formatter has modified the file (whitespace changes only)
**When** `writeDirectory()` is called without a normalizer registry
**Then** an `IllegalStateException` is thrown indicating checksum mismatch

#### Scenario: Accept formatter changes with normalized comparison

**Given** a Java file that was previously generated
**And** a code formatter has modified the file (whitespace changes only)
**And** a `FileNormalizerRegistry` is configured for Java files
**When** `writeDirectory()` is called with the normalizer registry
**Then** the generation succeeds without error
**And** the file content is preserved (not overwritten)
**And** the checksum is updated to match the formatted file

#### Scenario: Reject actual content changes even with normalizer

**Given** a Java file that was previously generated
**And** the file has been modified with actual content changes (not just whitespace)
**And** a `FileNormalizerRegistry` is configured
**When** `writeDirectory()` is called with the normalizer registry
**Then** an `IllegalStateException` is thrown

---

### Requirement: Actor-Based Generation Tests

The test suite SHALL include tests for multi-actor generation scenarios.

#### Scenario: Generate files for multiple actors

**Given** a generation configuration with multiple actors (e.g., "admin", "user")
**And** each actor has its own target directory
**When** files are generated for each actor
**Then** each actor's files are created in the correct directory
**And** each actor has a separate `.generated-files-{actor}` index

#### Scenario: Actor-specific checksum maintenance

**Given** files previously generated for multiple actors
**And** one actor's file has been modified
**When** regeneration is attempted
**Then** only the modified actor's generation fails
**And** unmodified actors' generations succeed

---

### Requirement: Configuration File Generation Tests

The test suite SHALL include tests for configuration file generation.

#### Scenario: Generate YAML configuration files

**Given** a generated file collection with a YAML configuration file
**When** `writeDirectory()` is called
**Then** the file is created with correct YAML formatting
**And** whitespace is preserved correctly

#### Scenario: Generate JSON configuration files

**Given** a generated file collection with a JSON configuration file
**When** `writeDirectory()` is called
**Then** the file is created with valid JSON content

---

### Requirement: Edge Case Tests

The test suite SHALL include tests for edge cases in file generation.

#### Scenario: Handle empty file generation

**Given** a generated file with zero-byte content
**When** `writeDirectory()` is called
**Then** an empty file is created at the specified path
**And** the file is indexed with appropriate checksum

#### Scenario: Handle large file generation

**Given** a generated file with content larger than 100KB
**When** `writeDirectory()` is called
**Then** the file is created with complete content
**And** the checksum is calculated correctly

#### Scenario: Handle special characters in paths

**Given** a generated file with special characters in its path (spaces, hyphens, underscores)
**When** `writeDirectory()` is called
**Then** the file is created at the correct path with special characters preserved

#### Scenario: Handle Unicode content

**Given** a generated file containing Unicode characters (emojis, non-ASCII text)
**When** `writeDirectory()` is called
**Then** the file is created with correct Unicode encoding (UTF-8)
**And** the content is readable and matches the original

