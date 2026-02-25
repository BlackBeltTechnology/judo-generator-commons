# File Management Specification

## Purpose

The file management subsystem handles writing generated files to disk with POSIX permissions, excluding files via `.generator-ignore` patterns, and synchronizing the project's `.gitignore` with generated file paths. It ensures that developer-modified files are protected and that generated output integrates cleanly with version control.

## Architecture

The subsystem consists of:

- `GeneratorIgnore` -- Reads `.generator-ignore` files containing glob patterns, walks the directory tree upward to collect patterns from all levels, and determines whether a file should be excluded from writing. Uses `LoadingCache` with 200-entry maximum for pattern caching.
- `GitIgnoreSynchronizer` -- Manages a delimited block (`# JUDO GENERATOR BLOCK START` / `# JUDO GENERATOR BLOCK END`) within the project's `.gitignore` file, adding generated file paths while preserving all other existing content.
- `GeneratedFile` -- Value object representing a file to be written (path, content bytes, POSIX permissions set, condition flag).
- File writing logic in `ModelGenerator` -- Handles directory creation, POSIX permission application, and fallback for non-POSIX systems.

## Requirements

### Requirement: Exclude files via generator-ignore patterns

`GeneratorIgnore` SHALL read `.generator-ignore` files containing glob patterns and prevent matching files from being written to disk during generation, while still tracking them in the checksum index.

#### Scenario: File matches ignore pattern
- **GIVEN** a `.generator-ignore` file containing `src/main/CustomFile.java`
- **WHEN** the generator produces a file at `src/main/CustomFile.java`
- **THEN** the file is NOT written to disk but remains in the `.generated-files` index

#### Scenario: Glob pattern matching
- **GIVEN** a `.generator-ignore` file containing `**/*.php`
- **WHEN** `generatorIgnore.shouldExcludeFile(path)` is called for `output/views/page.php`
- **THEN** `true` is returned

#### Scenario: Directory pattern matching
- **GIVEN** a `.generator-ignore` file containing `config/**`
- **WHEN** `generatorIgnore.shouldExcludeFile(path)` is called for `config/database.yml`
- **THEN** `true` is returned

#### Scenario: Hierarchical pattern resolution
- **GIVEN** a `.generator-ignore` file in both the root and a subdirectory
- **WHEN** a file under the subdirectory is checked
- **THEN** patterns from both the subdirectory and root `.generator-ignore` files are combined

### Requirement: Apply POSIX file permissions

The engine SHALL apply POSIX file permissions to generated files when a permission string is specified. Permissions use the standard 9-character format (`rwxrwxrwx`).

#### Scenario: Template-level permission applied
- **GIVEN** a `GeneratorTemplate` with `permission: rwxr-xr-x`
- **WHEN** the generated file is written to a POSIX filesystem
- **THEN** the file has owner=rwx, group=r-x, other=r-x permissions

#### Scenario: Model-level permission as fallback
- **GIVEN** a `GeneratorModel` with `permission: rw-r--r--` and a template with no permission set
- **WHEN** the generated file is written
- **THEN** the model-level `rw-r--r--` permission is applied

#### Scenario: Non-POSIX filesystem fallback
- **GIVEN** a non-POSIX filesystem (e.g. Windows)
- **WHEN** a generated file with POSIX permissions is written
- **THEN** the file is written without error and permissions are silently skipped

### Requirement: Create parent directories automatically

The engine SHALL create any missing parent directories when writing a generated file.

#### Scenario: Nested directory creation
- **GIVEN** a generated file with path `deep/nested/dir/Output.java` where `deep/` does not exist
- **WHEN** the file is written
- **THEN** all parent directories `deep/nested/dir/` are created

### Requirement: Synchronize .gitignore with generated files

`GitIgnoreSynchronizer` SHALL maintain a block in `.gitignore` delimited by `# JUDO GENERATOR BLOCK START` and `# JUDO GENERATOR BLOCK END` markers, containing paths of all generated files.

#### Scenario: Add generated files block
- **GIVEN** a `.gitignore` file without a JUDO generator block
- **WHEN** `gitIgnoreSynchronizer.addGeneratedFiles(entries)` is called
- **THEN** a new block with start/end markers is appended containing the generated file paths

#### Scenario: Update existing block
- **GIVEN** a `.gitignore` file with an existing JUDO generator block
- **WHEN** `gitIgnoreSynchronizer.addGeneratedFiles(entries)` is called with a different set of files
- **THEN** only the content between the markers is replaced; all other `.gitignore` entries are preserved

#### Scenario: Preserve existing gitignore content
- **GIVEN** a `.gitignore` with custom entries above and below the JUDO generator block
- **WHEN** the generator block is updated
- **THEN** all custom entries remain unchanged

### Requirement: Support binary file copying

When a template is configured with `copy: true`, the engine SHALL copy the file content as raw bytes without Handlebars template processing.

#### Scenario: Binary file copy
- **GIVEN** a `GeneratorTemplate` with `copy: true` and a binary file (e.g. an image)
- **WHEN** the file is generated
- **THEN** the binary content is copied verbatim to the output path without template rendering
