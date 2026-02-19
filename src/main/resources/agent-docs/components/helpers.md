# Helpers

The helper system enables custom functions in Handlebars templates and SpringEL expressions.

## Overview

Helpers are static methods that:
- Transform values in templates (`{{upperCase name}}`)
- Provide utility functions in expressions (`#upperCase(#name)`)
- Access template context via ThreadLocal

## Key Files

| File | Purpose |
|------|---------|
| `src/main/java/hu/blackbelt/judo/generator/commons/StaticMethodValueResolver.java` | Base class for helpers |
| `src/main/java/hu/blackbelt/judo/generator/commons/TemplateHelperFinder.java` | Annotation scanner |
| `src/main/java/hu/blackbelt/judo/generator/commons/ThreadLocalContextHolder.java` | Context access |
| `src/main/java/hu/blackbelt/judo/generator/commons/annotations/TemplateHelper.java` | Helper annotation |
| `src/main/java/hu/blackbelt/judo/generator/commons/annotations/ContextAccessor.java` | Context accessor annotation |

## Creating a Helper

### Basic Helper

```java
@TemplateHelper
public class StringHelper extends StaticMethodValueResolver {
    
    public static String upperCase(Object obj) {
        return obj != null ? obj.toString().toUpperCase() : "";
    }
    
    public static String camelToSnake(Object obj) {
        return CaseFormat.LOWER_CAMEL.to(
            CaseFormat.LOWER_UNDERSCORE, 
            obj.toString()
        );
    }
}
```

### Requirements

**MUST:**
- Annotate class with `@TemplateHelper`
- Extend `StaticMethodValueResolver`
- Methods must be `public static`
- Methods take 0 or 1 parameter
- Methods return non-void

**SHOULD:**
- Use descriptive method names (these become template functions)
- Handle null inputs gracefully
- Return empty string rather than null

## Using Helpers

### In Templates

```handlebars
{{upperCase actorName}}
{{camelToSnake propertyName}}
{{#if (isPublic entity)}}
  {{entity.name}} is public
{{/if}}
```

### In SpringEL

```yaml
pathExpression: "#upperCase(#actorType.name) + '.java'"
conditionExpression: "#isPublic(#entity)"
```

## Context Access

For accessing template parameters in parallel builds, use `@ContextAccessor`:

```java
@TemplateHelper
@ContextAccessor
public class ConfigHelper extends StaticMethodValueResolver {
    
    // Required method for context binding
    public static void bindContext(Map<String, ?> context) {
        ThreadLocalContextHolder.bindContext(context);
    }
    
    // Access context variables (must be synchronized)
    public static synchronized String getApiPrefix(Object obj) {
        return (String) ThreadLocalContextHolder.getVariable("apiPrefix");
    }
    
    public static synchronized String getConfig(Object key) {
        return (String) ThreadLocalContextHolder.getVariable(key.toString());
    }
}
```

### Why ThreadLocal?

- Helpers are static methods (class-level, not instance)
- Generation runs in parallel (multiple threads)
- Template parameters vary per generation context
- ThreadLocal provides thread-safe, per-context storage

### Context Accessor Requirements

1. Annotate with `@ContextAccessor`
2. Implement `bindContext(Map<String, ?> context)` method
3. Use `synchronized` on methods accessing ThreadLocal
4. Access via `ThreadLocalContextHolder.getVariable(name)`

## Helper Discovery

Helpers are auto-discovered by `TemplateHelperFinder`:

```java
Collection<Class<?>> helpers = TemplateHelperFinder.collectHelpers(
    packageName,      // Package to scan (e.g., "com.example.helpers")
    classLoader       // ClassLoader to use
);
```

Features:
- Uses ClassGraph for annotation scanning
- Filters by package prefix
- Supports custom ClassLoader

## Built-in Helpers

### String Helpers

| Method | Description | Example |
|--------|-------------|---------|
| `upperCase` | To uppercase | `"hello"` → `"HELLO"` |
| `lowerCase` | To lowercase | `"HELLO"` → `"hello"` |
| `capitalize` | Capitalize first | `"hello"` → `"Hello"` |
| `camelToSnake` | CamelCase to snake_case | `"myVar"` → `"my_var"` |

### URI Helpers

| Method | Description |
|--------|-------------|
| `pathOf` | Extract path from URI |
| `schemeOf` | Extract scheme from URI |

## Common Patterns

### Null-Safe Helper

```java
public static String safeTrim(Object obj) {
    if (obj == null) return "";
    return obj.toString().trim();
}
```

### Collection Helper

```java
public static int size(Object obj) {
    if (obj instanceof Collection) {
        return ((Collection<?>) obj).size();
    }
    return 0;
}
```

### Conditional Helper

```java
public static boolean isEmpty(Object obj) {
    if (obj == null) return true;
    if (obj instanceof String) return ((String) obj).isEmpty();
    if (obj instanceof Collection) return ((Collection<?>) obj).isEmpty();
    return false;
}
```

## Troubleshooting

**Issue:** Helper method not found
- Verify `@TemplateHelper` annotation on class
- Verify method is `public static`
- Check method has 0 or 1 parameter
- Ensure package is in scan path

**Issue:** Context variable returns null
- Verify `@ContextAccessor` annotation
- Implement `bindContext()` method
- Use `synchronized` on accessor methods
- Check variable name spelling

**Issue:** Race conditions in parallel generation
- Use `ThreadLocalContextHolder` for context access
- Mark context-accessing methods as `synchronized`
- Avoid static mutable state in helpers
