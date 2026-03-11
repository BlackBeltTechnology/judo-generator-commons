# Template System Specification

## Purpose

The template system provides Handlebars template compilation, hierarchical template loading with override support, SpringEL expression evaluation, and automatic discovery of template helper classes. It enables downstream generators to define templates in YAML and extend functionality through custom helpers.

## Architecture

The template system consists of several collaborating components:

- `ChainedURLTemplateLoader` -- Loads templates from URL-based locations in a parent-child chain, supporting `.override.hbs` overrides
- `TemplateEvaulator` -- Evaluates SpringEL expressions for path resolution, factory iteration, and condition checks
- `TemplateHelperFinder` -- Scans the classpath for `@TemplateHelper` annotated classes using ClassGraph
- `StaticMethodValueResolver` -- Abstract base class for helpers that resolves static methods as Handlebars value resolvers with `LoadingCache` caching
- `ThreadLocalContextHolder` -- Provides thread-safe access to template parameters for static helper methods
- `GeneratorTemplate` -- Configuration for a single template (path expression, condition, factory, permissions)
- `TemplateSpringELExpression` -- Named SpringEL expression holder
- `StringHelper` -- Built-in helper with string transformation methods (upperCase, camelCaseToSnakeCase, etc.)
- `UriHelper` -- URI manipulation utilities for template resource resolution

## Requirements

### Requirement: Load templates with hierarchical override support

`ChainedURLTemplateLoader` SHALL load templates from URL-based locations and support parent-child chaining where child loaders override parent templates. A template named `X.override.hbs` in a child loader SHALL take precedence over `X.hbs` in a parent loader.

#### Scenario: Override template resolution
- **GIVEN** a parent loader with `templates/Actor.java.hbs` and a child loader with `templates/Actor.java.override.hbs`
- **WHEN** the template `templates/Actor.java` is requested
- **THEN** the content from `Actor.java.override.hbs` in the child loader is returned

#### Scenario: Fallback to parent template
- **GIVEN** a parent loader with `templates/Model.java.hbs` and a child loader without any override
- **WHEN** the template `templates/Model.java` is requested
- **THEN** the content from the parent loader's `Model.java.hbs` is returned

### Requirement: Create template loaders from URIs

`ChainedURLTemplateLoader.createFromURIs()` SHALL accept a collection of URIs and create a chained loader hierarchy from them.

#### Scenario: Chain creation from URIs
- **GIVEN** a list of two URIs pointing to template directories
- **WHEN** `ChainedURLTemplateLoader.createFromURIs(uris)` is called
- **THEN** a chained loader is returned where the first URI is the child (highest priority) and the second is the parent

### Requirement: Evaluate SpringEL expressions

`TemplateEvaulator` SHALL evaluate SpringEL expressions for `factoryExpression`, `pathExpression`, and `conditionExpression` against a `StandardEvaluationContext` with registered variables and helper functions.

#### Scenario: Path expression evaluation
- **GIVEN** a `GeneratorTemplate` with `pathExpression: "#actorType.name + '/Service.java'"`
- **WHEN** the path expression is evaluated with `#actorType` set to an object whose `name` returns `"UserActor"`
- **THEN** the result is `"UserActor/Service.java"`

#### Scenario: Factory expression returning collection
- **GIVEN** a `GeneratorTemplate` with `factoryExpression: "#model.getAllEntities()"`
- **WHEN** `getFactoryExpressionResultOrValue()` is called
- **THEN** the returned object is the collection from `model.getAllEntities()`

#### Scenario: Condition expression filtering
- **GIVEN** a `GeneratorTemplate` with `conditionExpression: "#actorType.isPublic()"`
- **WHEN** the condition is evaluated for an actor type where `isPublic()` returns false
- **THEN** the generation for that template is skipped

### Requirement: Auto-discover template helpers

`TemplateHelperFinder` SHALL scan the classpath for classes annotated with `@TemplateHelper` and return them as helper classes. It SHALL also find the single class annotated with `@ContextAccessor`.

#### Scenario: Discover helpers by annotation
- **GIVEN** classes `StringHelper` and `CustomHelper` annotated with `@TemplateHelper` on the classpath
- **WHEN** `TemplateHelperFinder.collectHelpersAsClass()` is called
- **THEN** both classes are included in the returned collection

#### Scenario: Find context accessor
- **GIVEN** a class `ConfigHelper` annotated with both `@TemplateHelper` and `@ContextAccessor`
- **WHEN** `TemplateHelperFinder.findContextAccessorAsClass()` is called
- **THEN** the `ConfigHelper` class is returned

### Requirement: Resolve static helper methods as template values

`StaticMethodValueResolver` SHALL resolve calls to public static methods on helper classes, caching method lookups via `LoadingCache` for performance.

#### Scenario: Static method resolution
- **GIVEN** a `StringHelper` class extending `StaticMethodValueResolver` with a `public static String upperCase(Object)` method
- **WHEN** `resolve(someObject, "upperCase")` is called on the resolver
- **THEN** the result of `StringHelper.upperCase(someObject)` is returned

### Requirement: Provide thread-safe context access for helpers

`ThreadLocalContextHolder` SHALL store context maps per-thread via `ThreadLocal`, enabling static helper methods to access template parameters safely during parallel generation.

#### Scenario: Thread-local context binding
- **GIVEN** a helper method running in a parallel stream
- **WHEN** `ThreadLocalContextHolder.bindContext(contextMap)` is called, then `getVariable("key")` is called
- **THEN** the value for `"key"` from the bound context map is returned, isolated from other threads
