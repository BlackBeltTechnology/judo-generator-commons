# hu.blackbelt.judo.generator.commons (tests)

Per-file record (partial: rows added per change).

| File | Purpose |
|---|---|
| `BufferedSlf4jLoggerTest.java` | Pins Epsilon behaviour of `BufferedSlf4jLogger`: only enabled levels, call order, `String.format`, throwable carried, nothing before close, 2nd close no-op (TP-33). See change: yaml-pipeline-generators |
