# Contributing

Repository is hosted on GitHub: [Leawind/Resonator].

## Setup Development Environment

### Code Formatting

Use `google-java-format` to format java code.

1. Install [google-java-format] plugin for Intellij IDEA.
1. Navagate to `IDEA Settings` -> `Other Settings`
1. Enable checkbox `Enable google-java-format`
1. Select `Code Style` to `Default Google Java Style`

Format other languages with EditorConfig.

1. Navagate to `IDEA Settings` -> `Editor` -> `Code Style`
1. Enable checkbox `Enable EditorConfig suppport`

[Leawind/Resonator]: https://github.com/Leawind/Resonator
[google-java-format]: https://plugins.jetbrains.com/plugin/8527-google-java-format

## Coding Style

### Nullability

Use `@javax.annotation.Nullable` and `@javax.annotation.NonNull` annotations to
specify nullability.

If no nullabality annotation, it is considered non-null
