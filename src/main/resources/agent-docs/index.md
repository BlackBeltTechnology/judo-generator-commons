# JUDO Generator Commons - Agent Documentation

This directory contains modular documentation for AI assistants and developers working with the JUDO Generator Commons framework.

## Components

Detailed documentation for each major component:

| Component | Description |
|-----------|-------------|
| [Model Generator](components/model-generator.md) | Central orchestrator for generation workflow |
| [Template System](components/template-system.md) | Handlebars integration and template loading |
| [Helpers](components/helpers.md) | Helper creation, @TemplateHelper, context access |
| [Checksum Validation](components/checksum-validation.md) | Checksum management and validation |
| [Generator Ignore](components/generator-ignore.md) | GLOB patterns for file exclusion |

## Guides

Audience-specific documentation:

| Guide | Audience | Description |
|-------|----------|-------------|
| [AI Assistant Guide](guides/ai-assistant.md) | AI Assistants | Quick reference, patterns, do's/don'ts |
| [User Guide](guides/user-guide.md) | Developers | Getting started, configuration, examples |
| [API Reference](guides/api-reference.md) | All | Detailed API documentation |

## Quick Start

**For AI Assistants:** Start with the [AI Assistant Guide](guides/ai-assistant.md) for quick reference, then dive into specific [components](components/) as needed.

**For Developers:** Start with the [User Guide](guides/user-guide.md) for setup and configuration, refer to [API Reference](guides/api-reference.md) for detailed method signatures.

## Project Overview

JUDO Generator Commons is a template-based code generation framework that:
- Transforms meta-models into code using Handlebars templates
- Evaluates SpringEL expressions for dynamic paths and conditions
- Manages file checksums to detect manual modifications
- Supports ignore patterns for selective generation
- Provides a helper system for custom template functions
