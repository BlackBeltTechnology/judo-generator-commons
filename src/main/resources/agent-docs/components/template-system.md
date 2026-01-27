# Template System

The template system handles Handlebars template processing and SpringEL expression evaluation.

## Overview

The template system provides:
- Handlebars template compilation and execution
- SpringEL expression evaluation for paths, conditions, and factories
- Template loading with override support
- Context variable management

## Key Files

| File | Purpose |
|------|---------|
| `src/main/java/hu/blackbelt/judo/generator/commons/TemplateEvaulator.java` | Expression evaluator |
| `src/main/java/hu/blackbelt/judo/generator/commons/ChainedURLTemplateLoader.java` | Template loader with overrides |
| `src/main/java/hu/blackbelt/judo/generator/commons/GeneratorModel.java` | YAML descriptor loader |
| `src/main/java/hu/blackbelt/judo/generator/commons/GeneratorTemplate.java` | Single template config |

## Handlebars Integration

### Configuration

Handlebars is configured in `ModelGeneratorContext.createHandlebars()`:
- UTF-8 charset
- High-concurrency template cache
- Conditional and String helpers built-in
- Custom "times" helper for iteration
- Pretty printing enabled
- Infinite loops allowed

### Template Syntax

```handlebars
{{! Comment }}
{{variableName}}
{{#if condition}}
  {{value}}
{{/if}}
{{#each collection}}
  {{this.property}}
{{/each}}
{{helperMethod argument}}
```

## SpringEL Expressions

### Expression Types

| Property | Purpose | Example |
|----------|---------|---------|
| `pathExpression` | Output file path | `"#actorType.name + '/Actor.java'"` |
| `factoryExpression` | Collection to iterate | `"#model.getAllActors()"` |
| `conditionExpression` | Skip if false | `"#actorType.isPublic()"` |

### Context Variables

| Variable | Available When | Meaning |
|----------|---------------|---------|
| `#self` | Always | Context-dependent (see below) |
| `#model` | Always | Model object from GeneratorParameter |
| `#actorType` | actorTypeBased=true | Current actor type |

### Self Resolution

| Situation | `#self` refers to |
|-----------|------------------|
| actorTypeBased=true | Current actor type |
| In factoryExpression iteration | Current collection element |
| Neither | The model |

## Template Loading

### ChainedURLTemplateLoader

Supports parent-child chaining for template overrides:

```
base/templates/Actor.java.hbs        → Original
override/templates/Actor.java.override.hbs → Override (used if exists)
```

Features:
- Override mechanism via `.override.hbs` suffix
- Path ordering prevents infinite loops
- URL-based resource loading
- Parent loader fallback

### Creating Loader Chain

```java
ChainedURLTemplateLoader loader = ChainedURLTemplateLoader.createFromURIs(
    Arrays.asList(overrideUri, baseUri),
    "templates"
);
```

## YAML Template Descriptor

### Structure

```yaml
permission: rwxr-xr-x  # Global default

templateContext:       # Global variables
  - name: globalVar
    expression: "#someExpression"

templates:
  - name: templateId
    pathExpression: "#actorType.name + '/Actor.java'"
    templateName: templates/Actor.java.hbs
    actorTypeBased: true
    permission: rw-r--r--
    conditionExpression: "#actorType.isPublic()"
    factoryExpression: "#model.getAllActors()"
    templateContext:
      - name: localVar
        expression: "#self.getSomething()"
    copy: false
    exclude: false
```

### Properties

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `name` | String | Yes | Template identifier |
| `pathExpression` | SpringEL | Yes | Output file path |
| `templateName` | String | No | Template file (null for inline) |
| `actorTypeBased` | Boolean | No | Iterate over actors |
| `factoryExpression` | SpringEL | No | Collection source |
| `conditionExpression` | SpringEL | No | Generation condition |
| `permission` | String | No | POSIX permissions |
| `copy` | Boolean | No | Binary copy mode |
| `exclude` | Boolean | No | Skip generation |

## Common Patterns

### Template Override

To customize a generated file without modifying the original:

1. Create file with `.override.hbs` suffix
2. Place in override template directory
3. Configure loader with override path first

### Conditional Generation

```yaml
templates:
  - name: publicActor
    pathExpression: "#actorType.name + '/Public.java'"
    conditionExpression: "#actorType.isPublic()"
    templateName: templates/PublicActor.java.hbs
```

### Factory Iteration

```yaml
templates:
  - name: allEntities
    pathExpression: "#self.name + '.java'"
    factoryExpression: "#model.getEntities()"
    templateName: templates/Entity.java.hbs
```
