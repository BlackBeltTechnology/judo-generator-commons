# Generate Helper Skill

Creates new Handlebars helper classes for JUDO Generator Commons.

## Overview

This skill guides the creation of template helper functions that can be used in:
- Handlebars templates: `{{helperName value}}`
- SpringEL expressions: `#helperName(#variable)`

## Usage

Invoke the skill with `/generate-helper` and describe what helper you need.

### Examples

```
/generate-helper
I need a helper to pluralize entity names
```

```
/generate-helper
Create a helper that converts dates to ISO format
```

```
/generate-helper
I need a context-aware helper that accesses the current model's prefix
```

## What Gets Created

1. **Helper class** in `src/main/java/hu/blackbelt/judo/generator/commons/`
   - Annotated with `@TemplateHelper`
   - Extends `StaticMethodValueResolver`
   - Public static methods for helper functions

2. **Test class** in `src/test/java/hu/blackbelt/judo/generator/commons/`
   - JUnit 5 tests
   - Coverage for normal cases and edge cases (null, empty, etc.)

## Helper Types

### Simple Helper

For transforming values without needing template context:

```java
@TemplateHelper
public class MyHelper extends StaticMethodValueResolver {
    public static String transform(Object obj) {
        return obj.toString().toUpperCase();
    }
}
```

### Context-Aware Helper

For accessing template parameters or model state:

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

## Requirements

Helpers must:
- Be annotated with `@TemplateHelper`
- Extend `StaticMethodValueResolver`
- Have `public static` methods
- Accept 0 or 1 parameter
- Handle null inputs gracefully
- Return non-void values

## Related Documentation

- [Helpers Component](../../src/main/resources/agent-docs/components/helpers.md)
- [AI Assistant Guide](../../src/main/resources/agent-docs/guides/ai-assistant.md)
