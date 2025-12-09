# Proposal: Add Real-World Test Cases to ModelGeneratorTest

## Summary

Enhance the `ModelGeneratorTest` class with real-world test scenarios that simulate actual code generation workflows, including multi-file projects with realistic directory structures, content patterns, and edge cases encountered in production use.

## Problem Statement

The current `ModelGeneratorTest` contains basic unit tests with synthetic content (`"level1/file1"` as file content). While these tests verify core functionality, they don't adequately represent real-world scenarios:

1. **Synthetic content**: Test files contain path names as content rather than realistic code
2. **Missing file type diversity**: No tests with actual Java, TypeScript, or configuration files
3. **Limited directory structures**: Simple `level1/level2` hierarchy vs. realistic `src/main/java/...` structures
4. **No formatter simulation**: Missing tests that simulate IDE/build tool formatting changes
5. **No multi-actor scenarios**: Actor-based generation not tested with realistic discriminators

## Proposed Solution

Add a new test class `ModelGeneratorRealWorldTest` with realistic scenarios:

### Test Categories

1. **Java Project Generation**
   - Generate realistic Java class files with imports, annotations, methods
   - Verify package directory structure (`src/main/java/com/example/...`)
   - Test with formatted vs. unformatted content variations

2. **TypeScript/React Project Generation**
   - Generate component files with imports and JSX
   - Verify modern frontend directory conventions
   - Test with common formatter modifications (Prettier-style)

3. **Multi-File Generation with Dependencies**
   - Generate related files (entity + repository + service)
   - Verify consistent checksums across related files
   - Test partial regeneration scenarios

4. **Configuration File Generation**
   - YAML, JSON, properties files
   - Test with whitespace-sensitive content
   - Verify line ending handling (CRLF vs LF)

5. **Actor-Based Generation**
   - Multiple actors with separate output directories
   - Verify actor-specific checksum files
   - Test actor filtering with predicates

6. **Edge Cases**
   - Empty files
   - Large files (100KB+)
   - Files with special characters in paths
   - Unicode content

## Benefits

- Increased confidence in production readiness
- Better documentation through example-based tests
- Regression detection for real-world scenarios
- Easier onboarding for new contributors

## Scope

- **In scope**: New test class with realistic scenarios, test resources if needed
- **Out of scope**: Changes to production code, new features

## Dependencies

- None (test-only change)
