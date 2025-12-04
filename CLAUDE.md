# JUDO Generator Commons - AI Assistant Instructions

## CRITICAL: Always Read This First

**BEFORE doing any work on this project, you MUST read** `@/AGENTS.md` **completely.**

The AGENTS.md file contains:
- Complete project architecture and design patterns
- Core component descriptions with file references
- Template system implementation details
- Generation workflow and expression evaluation
- Key features (checksums, ignore patterns, permissions)
- Testing strategy and development guidelines
- Common tasks and troubleshooting guide
- Quick reference cards for daily use

**DO NOT proceed with any coding task until you have thoroughly read and understood AGENTS.md.**

---

<!-- OPENSPEC:START -->
# OpenSpec Instructions

These instructions are for AI assistants working in this project.

Always open `@/openspec/AGENTS.md` when the request:
- Mentions planning or proposals (words like proposal, spec, change, plan)
- Introduces new capabilities, breaking changes, architecture shifts, or big performance/security work
- Sounds ambiguous and you need the authoritative spec before coding

Use `@/openspec/AGENTS.md` to learn:
- How to create and apply change proposals
- Spec format and conventions
- Project structure and guidelines

Keep this managed block so 'openspec update' can refresh the instructions.

<!-- OPENSPEC:END -->

---

## Quick Start for AI Assistants

1. **Read** `@/AGENTS.md` - Complete project documentation
2. **Check** `@/README.adoc` - User-facing documentation
3. **Review** `@/openspec/AGENTS.md` - OpenSpec workflow (if planning changes)
4. **Explore** `@/pom.xml` - Build configuration and dependencies
5. **Examine** `src/main/java/hu/blackbelt/judo/generator/commons/` - Core implementation

## Project Quick Facts

- **Technology**: Java 21, Maven, OSGi Bundle
- **Purpose**: Template-based code generation framework
- **Main Components**: ModelGenerator, GeneratorContext, TemplateEvaluator
- **Template Engine**: Handlebars.java with SpringEL expressions
- **Key Features**: Checksum validation, generator ignore, incremental generation

## Development Rules

1. **Always extend** `StaticMethodValueResolver` for helper classes
2. **Always annotate** helpers with `@TemplateHelper`
3. **Use ThreadLocal** for template parameter access in helpers (`@ContextAccessor`)
4. **Follow YAML format** exactly for template descriptors
5. **Maintain checksums** - never bypass validation without good reason
6. **Test thoroughly** - checksum, ignore patterns, parallel generation

## Common Pitfalls to Avoid

❌ **DON'T**: Create helpers without `@TemplateHelper` annotation
✅ **DO**: Annotate and extend StaticMethodValueResolver

❌ **DON'T**: Access template parameters directly in static helpers
✅ **DO**: Use ThreadLocalContextHolder with @ContextAccessor

❌ **DON'T**: Ignore checksum validation errors
✅ **DO**: Understand why checksums mismatch, fix properly

❌ **DON'T**: Modify generated files manually without adding to .generator-ignore
✅ **DO**: Use ignore patterns or checksum-ignore appropriately

❌ **DON'T**: Guess SpringEL expression syntax
✅ **DO**: Read Spring Expression Language documentation

## File Location Reference

**Core Files** (see AGENTS.md for complete list):
- `/src/main/java/.../ModelGenerator.java` - Main orchestrator
- `/src/main/java/.../ModelGeneratorContext.java` - State container
- `/src/main/java/.../GeneratorModel.java` - Template collection
- `/src/main/java/.../TemplateEvaulator.java` - Expression evaluator

**Tests**:
- `/src/test/java/.../ModelGeneratorTest.java` - Main test suite
- `/src/test/java/.../GeneratorIgnoreTest.java` - Ignore pattern tests
- `/src/test/java/.../ChecksumIgnoreTest.java` - Checksum tests

**Documentation**:
- `/AGENTS.md` - **← START HERE** - Complete AI assistant guide
- `/README.adoc` - User documentation
- `/CONTRIBUTING.adoc` - Contribution guidelines
- `/openspec/AGENTS.md` - OpenSpec workflow

---

**Remember**: AGENTS.md is your primary resource. Read it first, reference it often.