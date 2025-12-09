# Tasks: Add Real-World Test Cases to ModelGeneratorTest

## 1. Create Test Class Structure

- [x] 1.1 Create `ModelGeneratorRealWorldTest.java` with standard JUnit 5 setup
- [x] 1.2 Add content constants for Java, TypeScript, and config files
- [x] 1.3 Add helper methods for common test operations

## 2. Java Project Tests

- [x] 2.1 Test: Generate Java entity with standard Maven directory structure
- [x] 2.2 Test: Generate multiple related Java files (entity, repository, service)
- [x] 2.3 Test: Regenerate Java files after content change

## 3. TypeScript/Frontend Tests

- [x] 3.1 Test: Generate TypeScript React component with imports
- [x] 3.2 Test: Generate multiple frontend files (component, hook, types)

## 4. Formatter Simulation Tests

- [x] 4.1 Test: Detect checksum mismatch after simulated formatter changes
- [x] 4.2 Test: Normalized comparison accepts formatter whitespace changes
- [x] 4.3 Test: Normalized comparison rejects actual content changes

## 5. Actor-Based Generation Tests

- [x] 5.1 Test: Generate files for multiple actors with separate directories
- [x] 5.2 Test: Actor-specific checksum files are maintained correctly

## 6. Configuration File Tests

- [x] 6.1 Test: Generate YAML configuration files
- [x] 6.2 Test: Generate JSON configuration files with proper formatting

## 7. Edge Case Tests

- [x] 7.1 Test: Handle empty file generation
- [x] 7.2 Test: Handle large file generation (100KB+)
- [x] 7.3 Test: Handle files with special characters in paths
- [x] 7.4 Test: Handle Unicode content in generated files

## 8. Verification

- [x] 8.1 Run all tests and verify they pass
- [x] 8.2 Verify test coverage with `mvn test jacoco:report`

## Dependencies

- Tasks 2.x-7.x depend on Task 1.x (test class structure)
- Task 8.x depends on all other tasks being complete
