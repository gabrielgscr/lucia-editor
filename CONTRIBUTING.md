# Contributing to Lucia Editor

First of all, thank you for your interest in contributing to Lucia Editor.

Lucia Editor is a desktop text editor project, and contributions are welcome in areas such as
bug fixes, documentation, tests, syntax highlighting, editor features, performance, usability,
and developer tooling.

## Ground Rules

By contributing to this repository, you agree that:

1. You have the right to submit the contribution.
2. Your contribution is your original work, or you have the legal right to submit it.
3. You understand that contributions will be distributed under the same license as the
   project unless explicitly stated otherwise.
4. You are not submitting code, content, or assets that violate third-party rights.

## Types of Contributions Welcome

We welcome contributions such as:

- bug fixes
- test improvements
- syntax highlighting improvements
- editor usability improvements
- keyboard shortcut enhancements
- file handling improvements
- documentation improvements
- performance optimizations
- refactoring that improves maintainability without altering behavior
- examples and sample editor workflows

## Before You Start

Please review the following before contributing:

- README.md
- LICENSE
- NOTICE
- TRADEMARKS.md
- open issues and discussions, if available

For large changes, new features, or architectural modifications, please open an issue first
to discuss the proposal before investing significant work.

## How to Contribute

### 1. Fork and branch

Create a fork and a topic branch with a clear name, for example:

- `fix/file-open-null-check`
- `feature/minimap-support`
- `docs/shortcut-reference-update`

### 2. Keep changes focused

Please keep pull requests as small and focused as reasonably possible.
Avoid mixing unrelated refactors with bug fixes or feature work.

### 3. Follow project conventions

Please follow the repository’s coding, naming, and structural conventions.

General expectations:
- write clear and maintainable code
- prefer readability over cleverness
- avoid unnecessary dependencies
- keep public APIs stable unless the change is discussed
- add comments only where they improve clarity
- update documentation when behavior changes

### 4. Include tests when appropriate

If your change affects editor behavior, rendering, syntax highlighting, file handling, or user interaction,
please include tests where feasible.

Examples:
- editor behavior tests
- syntax highlighting tests
- regression tests for reported bugs
- UI interaction tests where applicable

### 5. Write a good pull request

Please include:
- what changed
- why it changed
- how it was tested
- whether it introduces breaking changes
- screenshots or recordings for UI/editor changes, if relevant

## Coding Guidelines

Recommended practices:

- use descriptive names
- prefer small, cohesive methods and classes
- avoid hidden side effects
- preserve backward compatibility where possible
- document non-obvious behavior
- keep editor behavior intuitive and user-focused

If the project defines additional style rules, formatter settings, or analyzer rules, those
rules take precedence.

## License Headers (Recommended)

For new source files, prefer including a short SPDX header to make licensing explicit
at file level, for example:

`SPDX-License-Identifier: MPL-2.0`

If a file uses a different license, clearly indicate it in that file and ensure it is
compatible with project policies.

## Commit Messages

Use clear commit messages.

Examples:
- `Fix crash when opening empty file`
- `Improve syntax highlighting performance`
- `Add regression test for tab indentation`

## Documentation Changes

Documentation improvements are welcome, including:
- setup instructions
- editor shortcuts
- configuration options
- troubleshooting steps
- architecture notes

## Issue Reporting

When reporting a bug, please include as much of the following as possible:

- operating system and version
- application version or commit hash
- steps to reproduce
- expected behavior
- actual behavior
- screenshots, logs, or sample files if applicable

## Security Issues

Please do not disclose security-sensitive issues publicly without prior contact.
If you discover a vulnerability, report it privately to:

- Gabriel González
- ggonzalez@ticodevscr.com

Include enough detail to reproduce and assess the issue.

## Legal

By submitting a contribution, you agree that your contribution may be incorporated into the
project and distributed under the project license.

You also agree not to submit code, assets, or text copied from incompatible sources.

## Trademark Reminder

Contributing code does not grant permission to use the project’s trademarks, logos, or brand
identity outside the terms described in `TRADEMARKS.md`.

## Code of Conduct

Please be respectful and constructive in discussions, reviews, and issue reports.

Expected behavior:
- be professional
- assume good intent
- focus on technical merit
- avoid personal attacks
- welcome constructive feedback

Project maintainers reserve the right to moderate discussions and reject contributions that
do not align with the project’s technical direction, legal requirements, or community standards.
