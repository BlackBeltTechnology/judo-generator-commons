## ADDED Requirements

### Requirement: Skills directory structure
Claude Code skills SHALL be stored in `.claude/skills/` directory with one subdirectory per skill containing:
- `skill.md` - The skill definition file (Claude Code slash command)
- `README.md` - Human-readable documentation for the skill (optional)

#### Scenario: Single skill structure
- **WHEN** a skill named "generate-helper" is created
- **THEN** it SHALL be located at `.claude/skills/generate-helper/skill.md`

#### Scenario: Multiple skills coexist
- **WHEN** multiple skills are defined
- **THEN** each skill SHALL have its own subdirectory under `.claude/skills/`

### Requirement: Skill definition format
Each `skill.md` file SHALL be a valid Claude Code slash command file containing:
- A descriptive header explaining when to use the skill
- Step-by-step instructions for the AI to follow
- Input/output specifications
- Examples where applicable

#### Scenario: Skill file is valid Claude Code format
- **WHEN** a user invokes `/skill-name` in Claude Code
- **THEN** the content of `skill.md` SHALL be loaded as the command prompt

#### Scenario: Skill contains usage guidance
- **WHEN** reading a skill.md file
- **THEN** it SHALL contain a section explaining when and how to use the skill

### Requirement: Skill naming convention
Skill names SHALL use kebab-case (lowercase with hyphens) matching their directory name.

#### Scenario: Skill name matches directory
- **WHEN** a skill directory is named `create-template`
- **THEN** the skill SHALL be invocable as `/create-template`

#### Scenario: Invalid skill names rejected
- **WHEN** a skill directory uses camelCase or underscores
- **THEN** the marketplace validator SHALL report an error

### Requirement: Skills reference agent-docs
Skills MAY reference agent-docs for context by using the pattern `@agent-docs/<path>` in their instructions.

#### Scenario: Skill references component documentation
- **WHEN** a skill needs context about the template system
- **THEN** it MAY include `@agent-docs/components/template-system.md` to load that context

### Requirement: Skill metadata in marketplace
Each skill registered in the marketplace catalog SHALL have:
- `name` - The skill identifier (kebab-case)
- `source` - Relative path to the skill directory
- `description` - Brief description of what the skill does

#### Scenario: Skill listed in marketplace
- **WHEN** a skill is added to the marketplace
- **THEN** it SHALL have name, source, and description fields defined
