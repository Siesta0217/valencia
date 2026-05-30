---
type: project
status: active
repo: C:\Users\Siesta\nofall-mod
stack:
  - Java 21
  - Gradle
  - Fabric Loom
  - Minecraft 1.21.11
  - Fabric Loader 0.19.2
created: 2026-05-30
---

# nofall-mod / Valencia

## Goal

Maintain the Valencia Fabric client mod for Lunar Client / Minecraft 1.21.11.

## Current Shape

- Gradle project using `fabric-loom` and Java 21.
- Main source folder: `src/main/java/com/valencia`.
- Resource metadata: `src/main/resources/fabric.mod.json`.
- Mixin config: `src/main/resources/valencia.mixins.json`.
- Release artifacts are under `releases`.
- `build.gradle` finalizes `build` with a `deploy` task that copies the remapped jar into the Lunar Client Fabric mods folder when it exists.

## Important Files

- `build.gradle`: Fabric Loom setup, Java version, jar packaging, deploy task.
- `gradle.properties`: Minecraft, loader, Fabric, mod version, archive name.
- `src/main/java/com/valencia/NoFallMod.java`: client entrypoint in `fabric.mod.json`.
- `src/main/java/com/valencia/ModConfig.java`: likely central config/keybind persistence.
- `src/main/java/com/valencia/ClickGuiScreen.java`: UI surface for module settings.
- `src/main/java/com/valencia/mixin`: behavior injection points.
- `README.md`: feature list and release/build notes, but some text appears mojibake/encoding-corrupted.

## Commands

```powershell
.\gradlew.bat assemble
.\gradlew.bat build
.\gradlew.bat remapJar
.\gradlew.bat deploy
```

Use `assemble` for a lighter local build. Remember that `build` currently runs `deploy` afterward because of `build.finalizedBy("deploy")`.

## AI Rules

- Read the relevant module and its mixin together before editing behavior.
- Keep changes scoped to the requested module unless shared config/keybind/UI code must change.
- Do not rewrite the mojibake text in `README.md` unless the task is explicitly documentation cleanup.
- Prefer Gradle tasks already present in the repo instead of inventing new build commands.
- Check `git status --short` before edits and do not revert user changes.
- When changing gameplay behavior, note the affected module, mixin, keybind, config field, and visible UI control.

## Verification Checklist

- Compile with `.\gradlew.bat assemble`.
- If resources or mixins changed, confirm `fabric.mod.json` and `valencia.mixins.json` still match class names.
- If keybinds or config changed, inspect `ModConfig.java`, `Keybinds.java`, and `ClickGuiScreen.java` together.
- If rendering changed, inspect both the renderer class and the related mixin/HUD entrypoint.

## Known Issues / Watchpoints

- README contains encoding-corrupted Chinese text. Treat it as user content until asked to repair it.
- `gradle.properties` contains `yarn_mappings` and `fabric_version`, but `build.gradle` currently uses official Mojang mappings and only declares Fabric Loader.
- `build` auto-deploys if the Lunar Client mods directory exists, which can surprise automated runs.

## Useful Agent Prompts

- [[Codex Repo Fix]]
- [[Debug Root Cause]]

## Next Actions

- Add a clean project-specific skill from this note when the workflow stabilizes.
- Add one debug note after the next non-trivial bug fix.

