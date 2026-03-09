# Contributing to JUDO Generator Commons

Thank you for your interest in contributing to the JUDO Generator Commons project. This guide explains how to set up your development environment, submit issues, and create pull requests.

## Development Environment

### Prerequisites

Make sure your development environment meets these requirements:

| Requirement | Version |
|-------------|---------|
| **Java JDK** | 21 or higher |
| **Maven** | 3.9.4 or higher |
| **Git** | Any recent version |

For additional requirements and general JUDO ecosystem setup, see the parent project's [CONTRIBUTING guide](https://github.com/BlackBeltTechnology/judo-community/blob/develop/CONTRIBUTING.adoc).

### Project Setup

```bash
# Clone the repository
git clone git@github.com:BlackBeltTechnology/judo-generator-commons.git
cd judo-generator-commons

# Build the project
mvn clean install

# Run tests only
mvn clean test
```

## Code Structure

This is a single-module Maven project packaged as an OSGi bundle. All source code lives under `src/main/java/hu/blackbelt/judo/generator/commons/`.

The main components are:

- **ModelGenerator** -- Central orchestrator for the generation workflow
- **ModelGeneratorContext** -- Holds Handlebars engine, SpringEL context, and registered helpers
- **GeneratorModel / GeneratorTemplate** -- YAML descriptor model and individual template configuration
- **TemplateEvaulator** -- Evaluates SpringEL expressions for paths, conditions, and factory iteration
- **ChainedURLTemplateLoader** -- Hierarchical template loading with override support

For a complete architecture overview, see the [README](README.md) and [AGENTS.md](AGENTS.md).

## Submission Guidelines

### Submitting an Issue

Before submitting an issue, search the [issue tracker](https://github.com/BlackBeltTechnology/judo-generator-commons/issues) to see if the problem has already been reported or resolved.

To help us reproduce and fix bugs efficiently, include:

- Output of `java -version` and `mvn -version`
- Your `pom.xml` or `.flattened-pom.xml` (when applicable)
- A minimal reproduction case that demonstrates the failure

We will ask for a minimal reproduction to save maintainer time and ensure we are fixing the correct problem.

File new issues using the [issue form](https://github.com/BlackBeltTechnology/judo-generator-commons/issues/new/choose).

### Submitting a Pull Request

This project follows [GitHub's standard forking model](https://guides.github.com/activities/forking/). To contribute:

1. Fork the repository
2. Create a feature branch from `develop`
3. Make your changes
4. Run the full test suite: `mvn clean test`
5. Submit a pull request against the `develop` branch

## Commands

### Run Tests

```bash
mvn clean test
```

### Run Full Build

```bash
mvn clean install
```

### Run with Code Coverage

```bash
mvn clean test jacoco:report
```

The coverage report is generated at `target/site/jacoco/index.html`.

## Code Conventions

- Use **Lombok** annotations (`@Builder`, `@Slf4j`, `@Getter`) to minimize boilerplate
- Helper classes must be annotated with `@TemplateHelper` and extend `StaticMethodValueResolver`
- Helper methods must be `public static` with 0 or 1 parameter
- Use `ThreadLocalContextHolder` with `@ContextAccessor` for accessing template parameters in static helpers
- Follow existing code style -- no auto-formatting or import reorganization

## Git Workflow

- **Main branch:** `develop`
- **Feature branches:** `feature/JNG-NUMBER_short_summary`
- **Release branches:** `release/X.Y.Z`
- **Bugfix branches:** `bugfix/JNG-NUMBER_short_summary`

> **Important:** Every commit must reference a JIRA ticket number (e.g. `JNG-123`).

For the complete branching model and CI/CD workflow, see [.github/CIFLOW.md](.github/CIFLOW.md).
