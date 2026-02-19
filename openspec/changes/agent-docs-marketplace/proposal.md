## Why

The AGENTS.md file contains comprehensive documentation for AI assistants, but it's monolithic and locked within this project. Other JUDO projects (judo-meta-esm, judo-meta-pam, judo-meta-ui, judo-runtime-core) need similar AI-consumable documentation but have no standardized way to:
1. Structure their own agent documentation
2. Share reusable Claude Code skills across projects
3. Discover and install skills from dependencies

This change creates a modular agent documentation system with a marketplace for Claude Code skills, enabling the entire JUDO ecosystem to benefit from AI-assisted development.

## What Changes

- Split AGENTS.md into modular, component-focused documentation files stored in a dedicated directory structure
- Create audience-specific documentation layers (AI assistant guide, user guide, API reference)
- Package agent-docs inside the existing JAR for dependency-based consumption
- Define a Claude Code skills format for this project
- Create a local marketplace catalog format (`claude-marketplace.json`) for skill discovery
- Establish conventions for consumer projects to install and use skills via Maven dependencies

## Capabilities

### New Capabilities
- `agent-docs-structure`: Modular documentation structure with per-component and per-audience organization, packaged in JAR
- `claude-skills-format`: Claude Code skill definition format and directory structure for this project
- `marketplace-catalog`: Local catalog format (`claude-marketplace.json`) for discovering and referencing skills from dependencies

### Modified Capabilities
<!-- No existing specs are being modified -->

## Impact

**Code Changes:**
- New directory: `src/main/resources/agent-docs/` for packaged documentation
- New directory: `.claude/skills/` for Claude Code skills (project-local)
- New file: `claude-marketplace.json` at project root for skill catalog

**Build Changes:**
- Maven resources configuration to include `agent-docs/` in JAR
- Documentation for consumer projects on extracting/referencing docs from dependencies

**Documentation Changes:**
- AGENTS.md will be refactored into smaller, focused files
- New README section explaining the agent-docs and marketplace system

**Consumer Project Impact:**
- Projects depending on judo-generator-commons can access agent-docs from the JAR
- Consumer projects can reference skills by adding Maven dependency and configuring their local marketplace

**No Breaking Changes:**
- Existing AGENTS.md content preserved (just reorganized)
- No API changes
- No behavior changes to generation framework
