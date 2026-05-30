---
type: skill-draft
status: draft
target: claude-code
created: 2026-05-30
---

# repo-conventions Skill Draft

This can later become `.claude/skills/repo-conventions/SKILL.md`.

```md
---
name: repo-conventions
description: Use when working in the Valencia / nofall-mod repository. Provides build commands, project layout, verification rules, and editing constraints.
---

# Valencia Repo Conventions

Before editing, inspect `git status --short`.

## Project

- Java 21 Gradle project using Fabric Loom.
- Main package: `com.valencia`.
- Source root: `src/main/java/com/valencia`.
- Resources: `src/main/resources`.
- Client entrypoint: `com.valencia.NoFallMod`.

## Commands

```powershell
.\gradlew.bat assemble
.\gradlew.bat build
.\gradlew.bat remapJar
```

Prefer `assemble` for normal verification. Use `build` only when deploy side effects are acceptable.

## Editing Rules

- Read module class and related mixin together.
- Keep feature behavior, config, keybind, and ClickGUI controls consistent.
- Do not rewrite README mojibake unless asked.
- If changing mixin names, update `valencia.mixins.json`.
- If changing entrypoints or metadata, update `fabric.mod.json`.

## Final Response

Report changed files, verification command, verification result, and any remaining risk.
```

