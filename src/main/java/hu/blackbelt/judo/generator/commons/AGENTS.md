# hu.blackbelt.judo.generator.commons

Per-file record (partial: rows added per change).

| File | Purpose |
|---|---|
| `BufferedSlf4jLogger.java` | Epsilon-free copy of `epsilon-runtime-execution` `…execution.impl.BufferedSlf4jLogger`. Buffers slf4j calls; level set from wrapped logger (`LogLevel.determinateLogLevel`); `String.format` formatting; `flush()`/`close()` replay to wrapped logger then clear (2nd close no-op). `@Slf4j` fallback `log`. Behaviour-identical, incl. `warn(fmt, args...)` gated on `isInfoEnabled`. See change: yaml-pipeline-generators |
| `LogLevel.java` | Enum ERROR..TRACE; `getMatchingLogLevels`, `determinateLogLevel(Logger)`. Copy of epsilon `LogLevel`. See change: yaml-pipeline-generators |
| `StaticMethodValueResolver.java` | Handlebars `ValueResolver` base of `@TemplateHelper` classes: `resolve(context, name)` invokes the public static helper method `name` (0 args, or 1 arg assignable from the context class, superclass walk). Resolved method cached per context class + name (`Optional.empty()` = no match); mismatch warning still logged on every unresolved call. See change: JNG-6454. |
