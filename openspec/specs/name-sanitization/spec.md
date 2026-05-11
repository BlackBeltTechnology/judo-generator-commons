# name-sanitization Specification

## Purpose
TBD - created by archiving change jng-6395-model-name-dash-support. Update Purpose after archive.
## Requirements
### Requirement: Null and empty inputs are preserved

`NameSanitizer` SHALL be null-safe and empty-safe across every transform. A `null` input MUST return `null`, and an empty input MUST return an empty string. Callers MUST be able to invoke any method without wrapping it in a null guard.

#### Scenario: null input to toJavaIdentifier
- **WHEN** `NameSanitizer.toJavaIdentifier(null)` is called
- **THEN** the return value is `null`

#### Scenario: empty input to toJavaIdentifier
- **WHEN** `NameSanitizer.toJavaIdentifier("")` is called
- **THEN** the return value is `""`

#### Scenario: null input to toJavaPackage
- **WHEN** `NameSanitizer.toJavaPackage(null)` is called
- **THEN** the return value is `null`

#### Scenario: null input to toJsIdentifier
- **WHEN** `NameSanitizer.toJsIdentifier(null)` is called
- **THEN** the return value is `null`

#### Scenario: null input to toEnvVarName
- **WHEN** `NameSanitizer.toEnvVarName(null)` is called
- **THEN** the return value is `null`

#### Scenario: null input to toFileNameSafe
- **WHEN** `NameSanitizer.toFileNameSafe(null)` is called
- **THEN** the return value is `null`

### Requirement: toJavaIdentifier produces a valid Java identifier

`NameSanitizer.toJavaIdentifier(String)` SHALL replace every character not in `[A-Za-z0-9_]` with `_`. If the resulting string begins with a digit, it MUST be prefixed with `_`. If the resulting string is a Java SE 21 reserved word, it MUST be suffixed with `_`.

#### Scenario: dashed identifier is converted to underscored identifier
- **WHEN** `NameSanitizer.toJavaIdentifier("my-app")` is called
- **THEN** the return value is `"my_app"`

#### Scenario: identifier starting with a digit is prefixed with underscore
- **WHEN** `NameSanitizer.toJavaIdentifier("1abc")` is called
- **THEN** the return value is `"_1abc"`

#### Scenario: Java reserved word is suffixed with underscore
- **WHEN** `NameSanitizer.toJavaIdentifier("class")` is called
- **THEN** the return value is `"class_"`

#### Scenario: multiple unsafe characters are each replaced
- **WHEN** `NameSanitizer.toJavaIdentifier("a.b-c d")` is called
- **THEN** the return value is `"a_b_c_d"`

### Requirement: toJavaPackage splits qualified names on `::`

`NameSanitizer.toJavaPackage(String)` SHALL split its input on the JSL qualifier separator `::`, lowercase each segment, sanitize each segment with the same rules as `toJavaIdentifier`, and join the segments with `.`.

#### Scenario: two-segment qualified name with a dashed leaf
- **WHEN** `NameSanitizer.toJavaPackage("Acme::my-app")` is called
- **THEN** the return value is `"acme.my_app"`

#### Scenario: three-segment qualified name with mixed casing and a dashed leaf
- **WHEN** `NameSanitizer.toJavaPackage("MY::sub::dash-here")` is called
- **THEN** the return value is `"my.sub.dash_here"`

#### Scenario: single segment passes through lowercased
- **WHEN** `NameSanitizer.toJavaPackage("MyApp")` is called
- **THEN** the return value is `"myapp"`

### Requirement: toJsIdentifier produces a valid JavaScript identifier

`NameSanitizer.toJsIdentifier(String)` SHALL apply the same character replacement and leading-digit rules as `toJavaIdentifier`, but check the result against the ECMAScript reserved-word list (suffixing `_` on collision). The mapping MUST be deterministic: no camelCasing or other boundary inference.

#### Scenario: dashed identifier is converted to underscored identifier
- **WHEN** `NameSanitizer.toJsIdentifier("my-app")` is called
- **THEN** the return value is `"my_app"`

#### Scenario: identifier starting with a digit is prefixed with underscore
- **WHEN** `NameSanitizer.toJsIdentifier("1abc")` is called
- **THEN** the return value is `"_1abc"`

#### Scenario: JavaScript reserved word is suffixed with underscore
- **WHEN** `NameSanitizer.toJsIdentifier("function")` is called
- **THEN** the return value is `"function_"`

### Requirement: toEnvVarName produces a POSIX environment variable name

`NameSanitizer.toEnvVarName(String)` SHALL replace the JSL qualifier separator `::` with `__`, replace every other character not in `[A-Za-z0-9_]` with `_`, and uppercase the result. If the result starts with a digit, it MUST be prefixed with `_`.

#### Scenario: dashed identifier becomes uppercase underscored
- **WHEN** `NameSanitizer.toEnvVarName("my-app")` is called
- **THEN** the return value is `"MY_APP"`

#### Scenario: qualified dashed name preserves segment boundary
- **WHEN** `NameSanitizer.toEnvVarName("acme::my-app::v2")` is called
- **THEN** the return value is `"ACME__MY_APP__V2"`

#### Scenario: leading digit is prefixed with underscore
- **WHEN** `NameSanitizer.toEnvVarName("1abc")` is called
- **THEN** the return value is `"_1ABC"`

### Requirement: toFileNameSafe produces a POSIX-safe filename and keeps dashes

`NameSanitizer.toFileNameSafe(String)` SHALL replace the JSL qualifier separator `::` with `__`, replace every character in the set `{ /, \, :, *, ?, ", <, >, | }` and every ASCII control character (`\u0000`–`\u001F`) with `_`, and preserve every other character (including dashes) unchanged.

#### Scenario: dashes are preserved
- **WHEN** `NameSanitizer.toFileNameSafe("acme::my-app")` is called
- **THEN** the return value is `"acme__my-app"`

#### Scenario: forward slash is replaced
- **WHEN** `NameSanitizer.toFileNameSafe("with/slash")` is called
- **THEN** the return value is `"with_slash"`

#### Scenario: multiple unsafe characters are each replaced
- **WHEN** `NameSanitizer.toFileNameSafe("a*b?c|d")` is called
- **THEN** the return value is `"a_b_c_d"`

