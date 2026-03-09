# Generation Engine Specification

## Purpose

The generation engine is the central orchestrator that drives the entire code generation pipeline. It coordinates YAML descriptor loading, template evaluation, file writing, and checksum tracking through `ModelGenerator`, `ModelGeneratorContext`, `GeneratorParameter`, and `GeneratorResult`.

## Architecture

The engine centers on `ModelGenerator`, which exposes static methods for the complete generation lifecycle. `ModelGeneratorContext` holds the configured Handlebars engine, SpringEL evaluation context, and registered helpers. `GeneratorParameter` bundles all inputs (context, target directory, executor function, validation flags) using the Builder pattern. `GeneratorResult` collects generated files in thread-safe collections (`ConcurrentHashMap` for discriminator-based files, `CopyOnWriteArrayList` for common files).

Key classes:
- `ModelGenerator` -- static orchestrator methods
- `ModelGeneratorContext` -- engine state (Handlebars, SpringEL, helpers, value resolvers)
- `GeneratorParameter` -- generation configuration (Builder pattern)
- `GeneratorResult` -- thread-safe result container
- `GeneratedFile` -- single file output (path, content bytes, POSIX permissions, condition flag)

## Requirements

### Requirement: Generate files to a target directory

The engine SHALL accept a `GeneratorParameter` and produce output files in the configured target directory by invoking the `performExecutor` function.

#### Scenario: Basic generation run
- **GIVEN** a `GeneratorParameter` with a valid `generatorContext`, `targetDirectoryResolver`, and `performExecutor`
- **WHEN** `ModelGenerator.generateToDirectory(param)` is called
- **THEN** the `performExecutor` is invoked and the returned `GeneratorResult` files are written to the target directory

### Requirement: Support discriminator-based (actor) generation

The engine SHALL support writing separate file sets per discriminator (actor type), each tracked by its own `.generated-files-[discriminator]` index file.

#### Scenario: Actor-based generation produces per-actor directories
- **GIVEN** a `GeneratorResult` with entries in `generatedByDiscriminator`
- **WHEN** `ModelGenerator.generateToDirectory(param)` completes
- **THEN** each discriminator's files are written to their respective target directories via `discriminatorTargetDirectoryResolver`

### Requirement: Create generator context from configuration

The engine SHALL create a fully configured `ModelGeneratorContext` from a template loader, URL resolver, generator model, helper classes, value resolvers, and context accessor class.

#### Scenario: Context creation with helpers
- **GIVEN** a `ChainedURLTemplateLoader`, a `GeneratorModel` loaded from YAML, and a set of `@TemplateHelper` classes
- **WHEN** `ModelGenerator.createGeneratorContext(loader, resolver, model, helpers, valueResolvers, contextAccessor)` is called
- **THEN** the returned `ModelGeneratorContext` has a configured Handlebars engine with registered helpers and a SpringEL `StandardEvaluationContext` with registered helper functions

### Requirement: Support ZIP output

The engine SHALL be able to produce a ZIP archive of generated files instead of writing to disk.

#### Scenario: Generate files as ZIP
- **GIVEN** a valid `GeneratorParameter`
- **WHEN** `ModelGenerator.getGeneratedFilesAsZip(param)` is called
- **THEN** a byte array containing a valid ZIP archive of all generated files is returned

### Requirement: Reset and recalculate checksums

The engine SHALL provide methods to reset all checksums (deleting `.generated-files` index) and to recalculate checksums from existing files on disk.

#### Scenario: Reset checksums
- **GIVEN** a target directory with an existing `.generated-files` index
- **WHEN** `ModelGenerator.resetChecksums(targetDir)` is called
- **THEN** the `.generated-files` index file is deleted

#### Scenario: Recalculate checksums
- **GIVEN** a target directory with generated files and a `.generated-files` index
- **WHEN** `ModelGenerator.recalculateChecksumToDirectory(targetDir)` is called
- **THEN** the `.generated-files` index is updated with current MD5 checksums of all tracked files

### Requirement: Evaluate YAML template descriptors

The engine SHALL load YAML template descriptors via `GeneratorModel.loadYamlURL()` and deserialize them into `GeneratorModel` with `GeneratorTemplate` entries, supporting template override merging via `overrideTemplates()`.

#### Scenario: Load YAML descriptor
- **GIVEN** a valid YAML file URL containing template definitions
- **WHEN** `GeneratorModel.loadYamlURL(url)` is called
- **THEN** a `GeneratorModel` with correctly populated `templates` collection is returned

#### Scenario: Override templates
- **GIVEN** a base `GeneratorModel` and an override `GeneratorModel` with a template having `exclude: true`
- **WHEN** `baseModel.overrideTemplates(overrideModel)` is called
- **THEN** the matching template in the base model is replaced by the override definition
