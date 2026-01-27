## ADDED Requirements

### Requirement: Modular documentation directory structure
The system SHALL organize agent documentation in a `src/main/resources/agent-docs/` directory with the following structure:
- `components/` - Per-component documentation (one file per major component)
- `guides/` - Audience-specific guides (ai-assistant, user, api-reference)
- `index.md` - Entry point that lists all available documentation

#### Scenario: Component documentation exists for each major component
- **WHEN** the JAR is built
- **THEN** `agent-docs/components/` SHALL contain documentation files for: ModelGenerator, TemplateSystem, Helpers, ChecksumValidation, GeneratorIgnore

#### Scenario: Audience guides are organized separately
- **WHEN** an AI assistant reads the documentation
- **THEN** `agent-docs/guides/ai-assistant.md` SHALL provide AI-specific instructions and quick reference
- **THEN** `agent-docs/guides/user-guide.md` SHALL provide human-readable usage documentation
- **THEN** `agent-docs/guides/api-reference.md` SHALL provide detailed API documentation

### Requirement: Documentation packaged in JAR
The agent-docs directory SHALL be included in the built JAR artifact under the path `agent-docs/`.

#### Scenario: Documentation available in published artifact
- **WHEN** a consumer project adds judo-generator-commons as a Maven dependency
- **THEN** the consumer SHALL be able to read `agent-docs/index.md` from the classpath

#### Scenario: Maven resources include agent-docs
- **WHEN** Maven builds the project
- **THEN** contents of `src/main/resources/agent-docs/` SHALL be copied to the JAR under `agent-docs/`

### Requirement: Index file provides navigation
The `agent-docs/index.md` file SHALL list all available documentation with:
- Component documentation links
- Guide documentation links  
- Brief description of each document's purpose

#### Scenario: Index file structure
- **WHEN** an AI assistant reads `agent-docs/index.md`
- **THEN** the file SHALL contain a "Components" section listing all component docs
- **THEN** the file SHALL contain a "Guides" section listing all audience guides

### Requirement: Component documentation format
Each component documentation file SHALL follow a consistent format:
- Overview section explaining the component's purpose
- Key classes/files section with file paths
- Usage examples section
- Common patterns section
- Troubleshooting section (if applicable)

#### Scenario: ModelGenerator component doc structure
- **WHEN** reading `agent-docs/components/model-generator.md`
- **THEN** the file SHALL contain sections: Overview, Key Files, Usage Examples, Common Patterns

### Requirement: Cross-reference between documents
Documentation files SHALL use relative links to reference other documents within the agent-docs structure.

#### Scenario: Component doc links to guide
- **WHEN** a component doc needs to reference the AI assistant guide
- **THEN** it SHALL use a relative link like `../guides/ai-assistant.md`
