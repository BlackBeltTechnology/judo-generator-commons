## Context

JNG-6395 allows `-` characters in JSL `model` declarations. The model name flows through many downstream generators that produce Java identifiers, JavaScript identifiers, environment variable names, and file names. `judo-generator-commons` is the lowest module on the JUDO dependency graph that is already a transitive dependency of every code-generation consumer (archetypes, SDK core, UI React, UI E2E, tatami workflow), so it is the natural home for a shared sanitization helper.

The grammar work in `judo-meta-jsl` (Phase 3 of the plan) guarantees that JSL identifiers fed to this helper are ASCII-only — `[A-Za-z0-9_-]` plus the qualifier separator `::`. We therefore do not need a Unicode-aware implementation.

## Goals / Non-Goals

**Goals:**
- A single, discoverable utility class that defines the canonical transform from a JSL identifier to each downstream target naming convention.
- Deterministic, round-trippable mappings — given the same input, every call site in every module produces the same output, and a human can predict the mapping by reading the rules in this document.
- Null-safe and empty-safe behaviour: `null` in → `null` out, empty string in → empty string out. Callers never need to wrap calls in null guards.
- Pure functions with no external state: easy to test, easy to call from Handlebars / SpringEL helpers via `StaticMethodValueResolver`.

**Non-Goals:**
- Unicode awareness. JSL grammar restricts inputs to ASCII; supporting Unicode would invite locale-dependent casing bugs without a real-world use case.
- Bi-directional mapping (no `fromJavaIdentifier(String)`). The transform is intentionally lossy (e.g. `-` and `/` both collapse to `_`).
- Smart casing (camelCase / PascalCase). Predictability beats prettiness here; downstream templates that want camelCase already have their own helpers.

## Decisions

### D1 — Static methods on a final utility class

`NameSanitizer` is `final` with a `private` constructor and only `public static` methods. Rationale:

- The transforms are pure functions of their input. There is no state worth instantiating.
- Static methods integrate cleanly with the project's `StaticMethodValueResolver` pattern (see `openspec/project.md`), which is how Handlebars / SpringEL templates discover helpers.
- Matches the style of existing utility classes in this module (e.g. `StringHelper`, `UriHelper`).

Alternative considered: instance methods on an interface with a default implementation. Rejected because there is no foreseeable second implementation, and DI would complicate template usage.

### D2 — One class, not one-class-per-target

All five transforms live on `NameSanitizer`. Rationale: a single discovery point ("where do I sanitize names?") is more important than separating by target. The class is small (< 200 lines including Javadoc and reserved-word tables), so cohesion is not an issue.

### D3 — Reserved-word handling: trailing underscore suffix

When the sanitized result equals a Java (or, for `toJsIdentifier`, JavaScript) reserved word, suffix `_`: `class` → `class_`, `for` → `for_`.

Rationale:
- Predictable and reversible by humans reading generated code.
- A trailing underscore is the canonical convention in JavaPoet and most code generators.
- Avoids prefix-based schemes (`_class`) that could collide with the leading-digit rule.

Reserved-word lists are kept as `Set<String>` constants on the class. We include the full Java SE 21 reserved-word list (50 entries) and the JavaScript ES2022 reserved-word list (47 entries). These lists are static; if a future JSL feature adds new collisions, the lists can be updated without changing the algorithm.

### D4 — Qualified-name splitting belongs on the helper

The helper provides `toJavaPackage(String qualifiedName)` that splits on `::`, sanitizes each segment with `toJavaIdentifier`, lowercases, and joins with `.`. Callers do **not** need to split themselves.

Rationale:
- Single source of truth: every consumer would otherwise duplicate the `::`-split logic.
- The transform from JSL qualified name to Java package is conceptually one operation; exposing it as one method matches the mental model.
- `toJavaIdentifier(String)` remains available for callers who have already split (e.g. processing a single segment from a parser AST).

For `toEnvVarName` and `toFileNameSafe` the `::` separator collapses to `__` (a single underscore would lose the boundary information).

### D5 — `toJsIdentifier` uses underscore mapping, not camelCase

The plan listed camelCase as a "decide on review" item. We pick **underscore** (`my-app` → `my_app`, not `myApp`).

Rationale:
- Deterministic round-trip with the Java mapping (same algorithm, different reserved-word list).
- camelCase requires a boundary heuristic (does `MY-app` become `mYApp`, `MyApp`, or `myApp`?), and any choice would surprise some users.
- React/TypeScript templates that want camelCase already have a dedicated helper for the conversion. Pre-camelCasing here would force them to undo it.

### D6 — `toFileNameSafe` keeps dashes

Dashes are valid in POSIX filenames and are the original JSL syntax. `toFileNameSafe("my-app")` returns `"my-app"` unchanged. We only replace characters that are unsafe on Linux, macOS, or Windows: `/ \ : * ? " < > |` and ASCII control characters (`\u0000`–`\u001F`). The qualifier `::` collapses to `__`.

### D7 — No new Maven dependencies

The implementation uses only `java.lang`, `java.util`, and `java.util.regex`. No Guava, no Apache Commons. Keeps the module's footprint small and avoids version-clash surprises in downstream consumers.

## Risks / Trade-offs

- **Risk:** The lossy transform can produce collisions (`my-app` and `my_app` both map to `my_app`). → **Mitigation:** documented in Javadoc. Downstream call sites that need uniqueness (e.g. generating distinct Java classes) must enforce uniqueness at a higher level; the helper is not responsible for collision detection.
- **Risk:** Future JSL grammar additions (e.g. Unicode identifiers) would force a rewrite. → **Mitigation:** the helper has a small surface area and is fully covered by unit tests; a future rewrite is a contained change.
- **Trade-off:** Underscore mapping for JS identifiers diverges from common JS conventions (camelCase). → Accepted: predictability is the higher priority; templates can post-process.

## Migration Plan

Not applicable — this is a new utility with no existing consumers. Downstream adoption happens in subsequent phases of JNG-6395 (see plan §5).
