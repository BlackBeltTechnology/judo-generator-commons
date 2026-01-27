## Context

The JUDO Generator Commons project has a comprehensive 700+ line AGENTS.md file that serves AI assistants effectively, but this monolithic approach creates barriers for the broader JUDO ecosystem:

**Current state:**
- Single AGENTS.md file with all documentation
- No mechanism for other JUDO projects to benefit from reusable skills
- Each project must create documentation from scratch
- No standardized way to share AI-consumable knowledge across dependencies

**Constraints:**
- Must maintain backward compatibility (existing AGENTS.md content preserved)
- Must work within Maven/JAR packaging for dependency-based distribution
- Claude Code skills must follow the `.claude/skills/` convention
- Documentation must be accessible both at development time and from JAR dependencies

## Goals / Non-Goals

**Goals:**
- Create a modular documentation structure that can be packaged in JARs
- Define a Claude Code skills format for this project
- Establish a marketplace catalog for skill discovery
- Enable consumer projects to access documentation and skills from dependencies
- Preserve all existing AGENTS.md content in the new structure

**Non-Goals:**
- Runtime skill execution or dynamic loading (this is development-time tooling)
- Central marketplace server or registry (local file-based only)
- Automatic skill installation or dependency resolution
- Breaking changes to the generation framework behavior
- Documentation for projects other than judo-generator-commons

## Decisions

### Decision 1: Documentation directory structure

**Choice:** Use `src/main/resources/agent-docs/` for packaged documentation

**Rationale:**
- Standard Maven convention ensures automatic JAR inclusion
- Accessible via classpath from consumer projects
- Separates AI documentation from main code
- No build configuration changes needed beyond standard resources

**Alternatives considered:**
- `docs/agent/` - Not automatically packaged in JAR
- `META-INF/agent-docs/` - Less discoverable, unconventional for documentation
- Root-level directory - Would require custom Maven resource configuration

### Decision 2: Component documentation granularity

**Choice:** Five core component files matching the architecture diagram sections

| Component File | Contents |
|---------------|----------|
| `model-generator.md` | ModelGenerator orchestration, entry points, workflow |
| `template-system.md` | Handlebars, template loading, ChainedURLTemplateLoader |
| `helpers.md` | Helper creation, @TemplateHelper, StaticMethodValueResolver |
| `checksum-validation.md` | Checksums, .generated-files, validation workflow |
| `generator-ignore.md` | GLOB patterns, .generator-ignore, .generator-checksum-ignore |

**Rationale:**
- Matches existing AGENTS.md architecture diagram
- Each file is focused and independently useful
- Enables targeted reading without loading entire documentation
- Natural decomposition based on functionality

**Alternatives considered:**
- One file per Java class - Too granular, creates navigation overhead
- Two files (core + utilities) - Still too large, loses modularity benefit
- Seven+ files (adding expressions, permissions, etc.) - Diminishing returns

### Decision 3: Skills directory location

**Choice:** `.claude/skills/` for project-local skills, packaged to `claude-skills/` in JAR

**Rationale:**
- `.claude/skills/` is Claude Code's standard convention
- Separate JAR path (`claude-skills/`) avoids polluting consumer's .claude directory
- Consumers explicitly copy skills they want, maintaining control

**Alternatives considered:**
- `src/main/resources/skills/` - Breaks Claude Code convention, confusing
- `.claude/skills/` with same path in JAR - Could conflict with consumer's skills
- Top-level `skills/` - Not recognized by Claude Code tooling

### Decision 4: Marketplace catalog format

**Choice:** JSON file at project root named `claude-marketplace.json`

```json
{
  "name": "judo-generator-commons-marketplace",
  "description": "Claude Code skills for JUDO code generation",
  "plugins": [
    {
      "name": "generate-helper",
      "source": "./.claude/skills/generate-helper",
      "description": "Create a new Handlebars helper class"
    }
  ]
}
```

**Rationale:**
- JSON is widely supported and easy to parse
- Project root makes it discoverable
- `plugins` array allows multiple skills
- Minimal required fields (name, source, description) keep it simple

**Alternatives considered:**
- YAML format - Less universal tooling support
- `package.json` style with `skills` field - Conflates with npm conventions
- `skills.json` in `.claude/` - Less discoverable for consumers

### Decision 5: Audience-specific guides structure

**Choice:** Three guides in `agent-docs/guides/`

| Guide | Audience | Focus |
|-------|----------|-------|
| `ai-assistant.md` | AI assistants | Quick reference, common patterns, do's/don'ts |
| `user-guide.md` | Human developers | Getting started, configuration, examples |
| `api-reference.md` | Both | Detailed API documentation, method signatures |

**Rationale:**
- AI assistants need concise, actionable instructions
- Human developers need narrative documentation
- API reference serves both audiences
- Clear separation prevents document bloat

**Alternatives considered:**
- Single combined guide - Loses audience optimization
- Four+ guides (adding troubleshooting, FAQ) - Scope creep, can be sections within guides

### Decision 6: Consumer installation approach

**Choice:** Manual extraction and local marketplace reference

**Workflow for consumers:**
1. Add judo-generator-commons as Maven dependency
2. Extract desired skills from `claude-skills/` in JAR
3. Place in local `.claude/skills/` directory
4. Optionally reference in local `claude-marketplace.json`

**Rationale:**
- Simple, no tooling required
- Consumer maintains full control
- Works with standard Maven/JAR workflow
- No magic or implicit behavior

**Alternatives considered:**
- Automatic skill injection - Too invasive, unexpected behavior
- Maven plugin for extraction - Over-engineering for v1
- Symlinks to JAR resources - Platform-dependent, fragile

## Risks / Trade-offs

### Risk: Documentation drift between AGENTS.md and modular files
**Impact:** Medium - Confusing if main AGENTS.md diverges from components
**Mitigation:** AGENTS.md becomes a thin entry point that references modular files. Add a "Modular Documentation" section to AGENTS.md explaining the new structure. Consider deprecating detailed content in AGENTS.md over time.

### Risk: Consumer confusion about skill installation
**Impact:** Low - Consumers may not understand how to use skills
**Mitigation:** Clear documentation in README and agent-docs explaining the workflow. Provide example commands for JAR extraction.

### Risk: Marketplace format may not align with future Claude Code features
**Impact:** Low - Format is simple and adaptable
**Mitigation:** Keep format minimal. JSON schema allows extension without breaking changes. Monitor Claude Code updates.

### Trade-off: Manual skill installation vs. automation
**Accepted:** Manual process is more work for consumers but provides clarity and control. Automation can be added later without breaking the manual approach.

### Trade-off: Documentation duplication during transition
**Accepted:** During transition, some content exists in both AGENTS.md and modular files. This is temporary; phased migration will consolidate.

## Open Questions

1. **Should AGENTS.md be fully deprecated after migration?**
   - Recommendation: Keep as entry point, remove detailed content
   - Decision can be made after observing adoption

2. **What initial skills should be created?**
   - Candidates: `generate-helper`, `create-template`, `debug-generation`
   - Should be determined based on most common AI assistant tasks

3. **How should consumer projects discover available skills?**
   - Current approach: Read marketplace JSON, manual lookup
   - Future enhancement: CLI tool for skill listing/installation
