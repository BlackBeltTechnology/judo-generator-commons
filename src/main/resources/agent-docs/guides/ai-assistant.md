# AI Assistant Guide

Quick reference for AI assistants working with JUDO Generator Commons.

## Quick Facts

| Aspect | Value |
|--------|-------|
| Language | Java 21 |
| Build | Maven 3.9.4 |
| Template Engine | Handlebars 4.4.0 |
| Expression Language | SpringEL 6.2.7 |
| Package | `hu.blackbelt.judo.generator.commons` |

## Do's and Don'ts

### Do

- **Read AGENTS.md first** - Contains comprehensive project documentation
- **Extend StaticMethodValueResolver** when creating helpers
- **Use @TemplateHelper annotation** on all helper classes
- **Use ThreadLocalContextHolder** for context access in helpers
- **Follow YAML format exactly** for template descriptors
- **Test checksum scenarios** when modifying generation logic

### Don't

- **Don't create helpers without @TemplateHelper** - They won't be discovered
- **Don't access template params directly** in static helpers - Use ThreadLocal
- **Don't ignore checksum validation errors** - Understand and fix properly
- **Don't modify generated files** without adding to .generator-ignore
- **Don't guess SpringEL syntax** - Check Spring documentation

## Common Tasks

### Creating a New Helper

```java
@TemplateHelper
public class MyHelper extends StaticMethodValueResolver {
    public static String myMethod(Object obj) {
        return obj.toString().transform();
    }
}
```

### Adding Context Access

```java
@TemplateHelper
@ContextAccessor
public class MyHelper extends StaticMethodValueResolver {
    
    public static void bindContext(Map<String, ?> context) {
        ThreadLocalContextHolder.bindContext(context);
    }
    
    public static synchronized String getValue(Object key) {
        return (String) ThreadLocalContextHolder.getVariable(key.toString());
    }
}
```

### Adding a Template

```yaml
templates:
  - name: myTemplate
    pathExpression: "#self.name + '.java'"
    templateName: templates/MyTemplate.java.hbs
    factoryExpression: "#model.getEntities()"
    conditionExpression: "#self.isActive()"
```

## Key Locations

| What | Where |
|------|-------|
| Main orchestrator | `ModelGenerator.java` |
| Configuration | `GeneratorParameter.java` |
| Template config | `GeneratorTemplate.java` |
| Helper base | `StaticMethodValueResolver.java` |
| Context holder | `ThreadLocalContextHolder.java` |
| Tests | `src/test/java/.../ModelGeneratorTest.java` |

## Expression Quick Reference

### Variables

| Variable | Meaning |
|----------|---------|
| `#self` | Current item (actor, collection element, or model) |
| `#model` | The model object |
| `#actorType` | Current actor (when actorTypeBased=true) |

### Common Expressions

```yaml
# Path with actor name
pathExpression: "#actorType.name + '/Service.java'"

# Iterate collection
factoryExpression: "#model.getAllEntities()"

# Conditional generation
conditionExpression: "#self.isPublic() && !#self.isAbstract()"

# Use helper
pathExpression: "#upperCase(#self.name) + '.java'"
```

## Error Quick Reference

| Error | Cause | Solution |
|-------|-------|----------|
| `Generated file modified` | Manual edit detected | Revert, delete, or add to .generator-ignore |
| `Helper not found` | Missing annotation or wrong signature | Check @TemplateHelper, public static, 0-1 params |
| `SpelEvaluationException` | Bad expression syntax | Check variable names, method calls |
| `Template not found` | Wrong templateName path | Verify path in template loader |

## Build Commands

```bash
# Build
mvn clean install

# Test
mvn test

# Skip tests
mvn clean install -DskipTests

# Run specific test
mvn test -Dtest=ModelGeneratorTest
```

## File Patterns

### Ignore Files

| File | Purpose |
|------|---------|
| `.generator-ignore` | Skip file writing |
| `.generator-checksum-ignore` | Allow overwriting modified files |
| `.generated-files` | Checksum index |

### Pattern Examples

```
# Exact file
src/main/Custom.java

# All Java files
**/*.java

# Directory
target/

# Exception
generated/*
!generated/keep.txt
```

## Related Documentation

- [Model Generator](../components/model-generator.md) - Orchestration details
- [Template System](../components/template-system.md) - Templates and expressions
- [Helpers](../components/helpers.md) - Creating custom helpers
- [Checksum Validation](../components/checksum-validation.md) - Checksum management
- [Generator Ignore](../components/generator-ignore.md) - File exclusion patterns
