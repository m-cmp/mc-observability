# Project Management Guide

This document contains information about project management tools and configurations used in MC-O11Y Trigger.

## Spotless Code Formatting

### Overview

Spotless is a code formatter that ensures consistent code style across the project. It's configured to automatically format Java code using Google Java Format with AOSP style.

### Configuration

```gradle
spotless {
    java {
        importOrder(
            'java|javax|jakarta',
            'org.springframework',
            'lombok',
            '',
            'org.junit|org.mockito',
            '\\#',
            '\\#org.junit'
        )
        
        googleJavaFormat('1.17.0').aosp()
        
        formatAnnotations()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}
```

### Usage

```bash
# Check code formatting
./gradlew spotlessCheck

# Apply code formatting
./gradlew spotlessApply

# Automatic formatting on compile
./gradlew compileJava  # Runs spotlessApply first
```

### Features

- **Import Ordering**: Organizes imports in a consistent order
- **Google Java Format**: Uses AOSP (Android Open Source Project) style
- **Annotation Formatting**: Proper annotation placement
- **Cleanup**: Removes unused imports, trailing whitespace
- **Line Endings**: Ensures files end with newline

## REST Docs Plugin

### Overview

The project uses Spring REST Docs with API Spec generation to create comprehensive API documentation. This approach generates documentation from actual tests, ensuring accuracy and up-to-date documentation.

### Configuration

```gradle
plugins {
    id 'org.asciidoctor.jvm.convert' version '3.3.2'
    id 'com.epages.restdocs-api-spec' version '0.18.2'
}

dependencies {
    testImplementation 'org.springframework.restdocs:spring-restdocs-mockmvc'
    testImplementation 'com.epages:restdocs-api-spec-mockmvc:0.18.2'
}

ext {
    set('snippetsDir', file("build/generated-snippets"))
}
```

### OpenAPI Generation

```gradle
openapi3 {
    setServer('localhost:8080')
    title = 'MC-O11Y Trigger API'
    description = 'Multi-Cloud Observability Trigger API for managing alert policies, notification channels, and alert history'
    version = '1.0.0'
    format = 'yaml'
    outputFileNamePrefix = 'mc-o11y-trigger-api'
}
```

### Usage

```bash
# Run tests to generate documentation snippets
./gradlew test

# Generate AsciiDoc documentation
./gradlew asciidoctor

# Generate OpenAPI specification
./gradlew openapi3

# Build everything (includes documentation)
./gradlew build
```

### Features

- **Test-Driven Documentation**: Documentation generated from actual API tests
- **OpenAPI 3.0 Support**: Generates industry-standard API specifications
- **Multiple Formats**: Supports both AsciiDoc and YAML/JSON output
- **MockMVC Integration**: Works seamlessly with Spring Boot test framework
- **Automatic Generation**: Documentation updates automatically with code changes

### Documentation Location

- **Generated Snippets**: `build/generated-snippets/`
- **AsciiDoc Output**: `build/docs/asciidoc/`
- **OpenAPI Spec**: `build/api-spec/openapi3.yaml`

## Development Workflow

### Code Quality Checks

1. **Before Committing**: Always run `./gradlew spotlessCheck` to ensure code formatting
2. **Auto-formatting**: Use `./gradlew spotlessApply` to automatically fix formatting issues
3. **Documentation**: Run tests to generate up-to-date API documentation

### Build Process

The build process includes automatic code formatting:

```gradle
# Compilation depends on code formatting
tasks.named('compileJava') {
    dependsOn 'spotlessApply'
}

# Test generates API documentation snippets
tasks.named('test') {
    outputs.dir snippetsDir
    useJUnitPlatform()
}

# Documentation generation depends on tests
tasks.named('asciidoctor') {
    inputs.dir snippetsDir
    dependsOn test
}
```

### Best Practices

- **Consistent Formatting**: All code should follow the configured Spotless rules
- **Test Documentation**: Write comprehensive API tests that serve as documentation
- **Build Verification**: Ensure both formatting and documentation generation pass before deployment