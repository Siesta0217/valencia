---
type: reference
topic: fabric-gradle
created: 2026-05-30
---

# Fabric Gradle Notes

## Repo Facts

- Fabric Loom plugin: `1.10-SNAPSHOT`
- Java release: `21`
- Minecraft version: `1.21.11`
- Fabric Loader: `0.19.2`
- Archive base name: `valencia`
- Current mod version: `1.7.13`

## Local Build Commands

```powershell
.\gradlew.bat assemble
.\gradlew.bat remapJar
.\gradlew.bat build
```

## Deploy Behavior

`build.gradle` defines:

```groovy
build.finalizedBy("deploy")
```

That means `.\gradlew.bat build` attempts to deploy after building. Use `assemble` when you only want compilation/output checks.

