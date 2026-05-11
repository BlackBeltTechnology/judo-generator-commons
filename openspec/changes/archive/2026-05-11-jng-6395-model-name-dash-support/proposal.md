## Why

JIRA JNG-6395 introduces dash (`-`) characters as valid characters in JSL `model` declarations (e.g. `model my-app;`). Every downstream JUDO module — archetypes, SDK code generation, React/Playwright templates, workflow file naming — concatenates the model name into a Java package segment, a JS identifier, an environment variable, or a filesystem path. Without a single shared utility, each consumer would copy-paste its own regex and the rules would drift over time, producing inconsistent identifiers across the pipeline. We need one canonical helper, owned by `judo-generator-commons` (the lowest module in the dependency chain that is reachable from every consumer).

## What Changes

- Add a new utility class `hu.blackbelt.judo.generator.commons.NameSanitizer` to this module's main source set. The class is `final`, has a `private` no-arg constructor, and exposes only static, null-safe methods:
  - `toJavaIdentifier(String)` — replace every char not in `[A-Za-z0-9_]` with `_`; prefix `_` if the result starts with a digit; suffix `_` if the result is a Java reserved word.
  - `toJavaPackage(String)` — split on `::`, lowercase each segment, run it through `toJavaIdentifier`, join with `.`.
  - `toJsIdentifier(String)` — same sanitation rules as `toJavaIdentifier` but using the JavaScript reserved-word list; deterministic underscore mapping (no camelCasing) so the transform round-trips.
  - `toEnvVarName(String)` — uppercase, replace `::` with `__`, replace every other non `[A-Z0-9_]` char with `_`.
  - `toFileNameSafe(String)` — POSIX-safe filename: keep dashes, replace `::` with `__`, replace `/ \ : * ? " < > |` and control characters with `_`.
- Add JUnit 5 unit tests (`NameSanitizerTest`) covering every method: dashed names, `::`-qualified names, leading-digit names, reserved-word names, null and empty inputs.
- No breaking changes — additive only. No new Maven dependencies; only Java SE is used.

## Capabilities

### New Capabilities
- `name-sanitization`: rules for turning arbitrary JSL identifiers (including dashed and qualified names) into Java identifiers, Java package paths, JavaScript identifiers, environment variable names, and POSIX-safe file names.

### Modified Capabilities

_None._ This change is additive.

## Impact

- New code: `src/main/java/hu/blackbelt/judo/generator/commons/NameSanitizer.java`, `src/test/java/hu/blackbelt/judo/generator/commons/NameSanitizerTest.java`.
- No changes to existing public APIs.
- No new Maven dependencies.
- Downstream JUDO modules (`judo-tatami-jsl`, the archetype templates, `judo-psm-generator-sdk-core`, `judo-ui-react-template`, `judo-ui-e2e-template`) will adopt this helper in subsequent phases of JNG-6395.
