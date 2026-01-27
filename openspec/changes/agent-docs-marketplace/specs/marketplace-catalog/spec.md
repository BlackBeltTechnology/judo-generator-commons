## ADDED Requirements

### Requirement: Marketplace catalog file format
The marketplace catalog SHALL be a JSON file named `claude-marketplace.json` at the project root with the following structure:
```json
{
  "name": "<project-marketplace-name>",
  "description": "<marketplace description>",
  "plugins": [
    {
      "name": "<skill-name>",
      "source": "<relative-path-to-skill>",
      "description": "<skill description>"
    }
  ]
}
```

#### Scenario: Valid marketplace file structure
- **WHEN** `claude-marketplace.json` is read
- **THEN** it SHALL contain `name`, `description`, and `plugins` fields at the root level

#### Scenario: Plugin entry structure
- **WHEN** a plugin is listed in the `plugins` array
- **THEN** it SHALL have `name`, `source`, and `description` fields

### Requirement: Marketplace name convention
The marketplace `name` field SHALL follow the pattern `<project-name>-marketplace` using kebab-case.

#### Scenario: Marketplace name for judo-generator-commons
- **WHEN** the marketplace is created for judo-generator-commons
- **THEN** the name SHALL be `judo-generator-commons-marketplace`

### Requirement: Plugin source path resolution
The `source` field SHALL be a relative path from the project root to the skill directory.

#### Scenario: Local skill source path
- **WHEN** a skill is located at `.claude/skills/generate-helper/`
- **THEN** the source SHALL be `./.claude/skills/generate-helper`

### Requirement: Consumer project installation
Consumer projects SHALL install marketplace skills by:
1. Adding the provider project as a Maven dependency
2. Copying or symlinking the skill from the dependency's JAR or source

#### Scenario: Consumer adds dependency
- **WHEN** a consumer project wants to use judo-generator-commons skills
- **THEN** it SHALL add judo-generator-commons as a Maven dependency

#### Scenario: Consumer references skill
- **WHEN** a consumer project references an installed skill
- **THEN** it MAY add the skill to its own `.claude/skills/` directory

### Requirement: Marketplace discovery
Consumer projects MAY create their own `claude-marketplace.json` that references skills from dependencies.

#### Scenario: Consumer marketplace references dependency skill
- **WHEN** a consumer creates its marketplace catalog
- **THEN** it MAY include entries pointing to skills extracted from dependency JARs

### Requirement: Skills packaged in JAR
Skills defined in `.claude/skills/` SHALL be packaged in the JAR under `claude-skills/` for consumer access.

#### Scenario: Skills available in JAR
- **WHEN** the JAR is built
- **THEN** skills from `.claude/skills/` SHALL be available at `claude-skills/` in the JAR

#### Scenario: Consumer extracts skill from JAR
- **WHEN** a consumer project needs a skill from a dependency
- **THEN** it MAY extract the skill from `claude-skills/<skill-name>/skill.md` in the dependency JAR

### Requirement: Marketplace validation
The marketplace catalog SHALL be valid JSON and all referenced skill paths SHALL exist.

#### Scenario: Valid JSON format
- **WHEN** `claude-marketplace.json` is parsed
- **THEN** it SHALL be valid JSON without syntax errors

#### Scenario: Referenced skills exist
- **WHEN** a plugin source path is specified
- **THEN** the path SHALL point to an existing skill directory containing `skill.md`
