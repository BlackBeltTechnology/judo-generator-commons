# User Guide

Getting started guide for developers using JUDO Generator Commons.

## Introduction

JUDO Generator Commons is a template-based code generation framework that transforms meta-models into source code. It uses Handlebars templates with SpringEL expressions to provide flexible, powerful code generation.

## Prerequisites

- Java 21 or later
- Maven 3.9.4 or later
- Understanding of Handlebars template syntax
- Basic knowledge of Spring Expression Language (optional)

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>hu.blackbelt.judo.generator</groupId>
    <artifactId>judo-generator-commons</artifactId>
    <version>${judo-generator-commons.version}</version>
</dependency>
```

## Basic Usage

### 1. Create Template Descriptor

Create a YAML file describing your templates:

```yaml
# templates.yaml
templates:
  - name: entityTemplate
    pathExpression: "#self.name + '.java'"
    templateName: templates/Entity.java.hbs
    factoryExpression: "#model.getEntities()"
```

### 2. Create Handlebars Template

```handlebars
{{! templates/Entity.java.hbs }}
package {{package}};

public class {{name}} {
    {{#each properties}}
    private {{type}} {{name}};
    {{/each}}
}
```

### 3. Configure and Run Generation

```java
// Load model and templates
GeneratorModel model = GeneratorModel.loadYamlURL(templateYamlUrl);

// Create context
ModelGeneratorContext context = ModelGenerator.createGeneratorContext(
    templateLoader,
    urlResolver,
    model,
    helperClasses,
    valueResolvers,
    null
);

// Configure parameters
GeneratorParameter param = GeneratorParameter.builder()
    .generatorContext(context)
    .targetDirectory(outputDir)
    .performExecutor(this::generate)
    .validateChecksum(true)
    .build();

// Run generation
ModelGenerator.generateToDirectory(param);
```

## Template Descriptor Format

### Basic Template

```yaml
templates:
  - name: uniqueId           # Required: unique identifier
    pathExpression: "..."    # Required: output file path (SpringEL)
    templateName: path/to.hbs  # Handlebars template path
```

### Advanced Template

```yaml
templates:
  - name: actorService
    pathExpression: "#actorType.name + '/Service.java'"
    templateName: templates/Service.java.hbs
    actorTypeBased: true     # Iterate over actor types
    factoryExpression: "#model.getServices()"  # Collection to iterate
    conditionExpression: "#self.isPublic()"    # Skip if false
    permission: rwxr-xr-x    # POSIX file permissions
    templateContext:         # Additional variables
      - name: prefix
        expression: "#model.getPrefix()"
```

### Global Settings

```yaml
permission: rw-r--r--        # Default permissions

templateContext:             # Global variables
  - name: version
    expression: "'1.0.0'"

templates:
  # ... template definitions
```

## Expressions

### Available Variables

| Variable | Description |
|----------|-------------|
| `#model` | Your model object |
| `#self` | Current iteration item |
| `#actorType` | Current actor (when actorTypeBased=true) |

### Expression Examples

```yaml
# String concatenation
pathExpression: "#self.name + '/' + #self.type + '.java'"

# Method calls
factoryExpression: "#model.findByType('entity')"

# Conditionals
conditionExpression: "#self.isPublic() && #self.hasProperties()"

# Ternary
pathExpression: "#self.isTest() ? 'test/' : 'main/' + #self.name + '.java'"
```

## Helper Functions

### Using Built-in Helpers

```yaml
pathExpression: "#upperCase(#self.name) + '.java'"
```

### Creating Custom Helpers

```java
@TemplateHelper
public class MyHelpers extends StaticMethodValueResolver {
    
    public static String plural(Object obj) {
        String name = obj.toString();
        return name.endsWith("s") ? name : name + "s";
    }
}
```

Register helpers by including them in the helper classes collection when creating the context.

## File Management

### Generator Ignore

Create `.generator-ignore` to prevent overwriting files:

```
# Keep custom implementations
src/main/java/custom/**

# Keep manual configs
config/*.properties
```

### Checksum Ignore

Create `.generator-checksum-ignore` to allow regenerating modified files:

```
# Always regenerate these
generated/config/*.xml
```

### Permissions

Set file permissions in YAML:

```yaml
permission: rwxr-xr-x   # Owner: rwx, Group: r-x, Other: r-x
```

| Character | Meaning |
|-----------|---------|
| `r` | Read |
| `w` | Write |
| `x` | Execute |
| `-` | No permission |

## Template Overrides

Override templates without modifying originals:

1. Create override file with `.override.hbs` suffix:
   ```
   override/templates/Entity.java.override.hbs
   ```

2. Configure loader chain (override first):
   ```java
   ChainedURLTemplateLoader.createFromURIs(
       Arrays.asList(overrideUri, baseUri),
       "templates"
   );
   ```

## Whitespace-Tolerant Comparison

Handle formatter-modified files:

```yaml
fileNormalizers:
  - preset: auto        # Auto-detect languages
  
  # Or specific
  - preset: java
  - preset: ts
  
  # Or custom
  - extensions: [java]
    removeDoubleSpaces: true
    removeTabs: true
```

## Troubleshooting

### File keeps getting overwritten

Add to `.generator-ignore`:
```
path/to/file.java
```

### Checksum mismatch error

Options:
1. Revert file: `git checkout -- path/to/file.java`
2. Add to ignore: `.generator-checksum-ignore`
3. Reset checksums: `ModelGenerator.resetChecksums(param)`

### Template not found

- Check `templateName` path is correct
- Verify template exists in loader's search path
- Check for typos in file extension (`.hbs`)

### Helper method not working

Verify:
- Class has `@TemplateHelper` annotation
- Method is `public static`
- Method has 0 or 1 parameter
- Class extends `StaticMethodValueResolver`

## Next Steps

- Explore the [API Reference](api-reference.md) for detailed method documentation
- See [component documentation](../components/) for deep dives
- Check tests in `src/test/java` for working examples
