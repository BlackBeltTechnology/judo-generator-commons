## 1. Agent Docs Directory Structure

- [x] 1.1 Create `src/main/resources/agent-docs/` directory
- [x] 1.2 Create `src/main/resources/agent-docs/index.md` with navigation links to all components and guides
- [x] 1.3 Create `src/main/resources/agent-docs/components/` directory
- [x] 1.4 Create `src/main/resources/agent-docs/guides/` directory

## 2. Component Documentation

- [x] 2.1 Create `agent-docs/components/model-generator.md` (Overview, Key Files, Usage Examples, Common Patterns)
- [x] 2.2 Create `agent-docs/components/template-system.md` (Handlebars integration, ChainedURLTemplateLoader, template loading)
- [x] 2.3 Create `agent-docs/components/helpers.md` (Helper creation, @TemplateHelper, StaticMethodValueResolver, context access)
- [x] 2.4 Create `agent-docs/components/checksum-validation.md` (Checksums, .generated-files, validation workflow)
- [x] 2.5 Create `agent-docs/components/generator-ignore.md` (GLOB patterns, .generator-ignore, .generator-checksum-ignore)

## 3. Audience Guides

- [x] 3.1 Create `agent-docs/guides/ai-assistant.md` (Quick reference, common patterns, do's/don'ts for AI assistants)
- [x] 3.2 Create `agent-docs/guides/user-guide.md` (Getting started, configuration, examples for human developers)
- [x] 3.3 Create `agent-docs/guides/api-reference.md` (Detailed API documentation, method signatures)

## 4. Claude Skills Setup

- [x] 4.1 Create `.claude/skills/` directory structure
- [x] 4.2 Create `.claude/skills/generate-helper/skill.md` (Skill for creating new Handlebars helper classes)
- [x] 4.3 Create `.claude/skills/generate-helper/README.md` (Human-readable documentation for the skill)

## 5. Marketplace Catalog

- [x] 5.1 Create `claude-marketplace.json` at project root with name, description, and plugins array
- [x] 5.2 Add `generate-helper` skill entry to the marketplace catalog

## 6. Build Configuration

- [x] 6.1 Configure Maven to copy `.claude/skills/` to `claude-skills/` in the JAR
- [x] 6.2 Verify `agent-docs/` is packaged in JAR (should work by default from resources)
- [x] 6.3 Test JAR contents include both `agent-docs/` and `claude-skills/` directories

## 7. AGENTS.md Integration

- [x] 7.1 Add "Modular Documentation" section to AGENTS.md explaining new structure
- [x] 7.2 Add cross-references from AGENTS.md to modular component files
- [x] 7.3 Update CLAUDE.md to reference the new documentation structure

## 8. Documentation and Testing

- [x] 8.1 Update README.adoc with agent-docs and marketplace system explanation
- [x] 8.2 Add consumer project usage instructions (how to extract skills from JAR)
- [x] 8.3 Verify all relative links between documentation files work correctly
- [ ] 8.4 Test skill invocation by running `/generate-helper` command
