# Some Googly Eyes Build Environment

This document records the working Architectury Loom build environment. It describes the final build
layout and the compatible tool versions in use. Runtime architecture is covered by `as-built.md`.

## Version set

These versions come from the active wrapper, Gradle scripts, properties, and loader metadata.

| Component | Active version or constraint |
| --- | --- |
| Gradle wrapper | 9.5.1 |
| Java compilation and development runs | Java 21 |
| Minecraft | exactly 1.21.1 |
| Architectury Loom | 1.17.491 |
| Architectury Gradle plugin | 3.5.169 |
| Architectury API | 13.0.8; compile-time annotations and transformation only |
| Forge build target | 1.21.1-52.1.2; completed artifact |
| Forge runtime range in metadata | `[52.1.2,53)` |
| FML runtime range in metadata | `[52,53)` |
| NeoForge build target | 21.1.34; completed artifact |
| NeoForge runtime range in metadata | `[21.1.34,22)` |
| Fabric Loader | 0.15.11; runtime minimum 0.15.11 |
| Fabric API | 0.102.1+1.21.1; runtime minimum the same version |
| Mappings | Mojang official plus Parchment 2024.11.17 for Minecraft 1.21.1 |
| Shadow plugin | 9.4.3 |
| GeckoLib | 4.7.4, compile-only and optional at runtime |
| JSR-305 | 3.0.2, compile-only |
| Mod version | 0.8.2 |

Every subproject requests a Java 21 toolchain, sets source and target compatibility to 21, compiles
with `--release 21`, and uses Java 21 for development runs, independent of whatever JVM launches
Gradle itself.

Minecraft is exact because the renderer integration, Access Widener, Access Transformer, and Fabric
Mixins refer to 1.21.1 internals. Every loader compiles and runs against the minimum versions declared
in its metadata. Architectury API supplies compile-time annotations and transformation only; no loader
requires it at runtime.

## Project layout

The Gradle project includes exactly four modules:

```text
common/   shared production code, resources, and GameTest assertion logic
fabric/   completed Fabric adapters, metadata, Mixins, GameTests, and packaging
forge/    completed Forge adapters, metadata, access rules, GameTests, and packaging
neoforge/ completed NeoForge adapters, metadata, access rules, GameTests, and packaging
```

`settings.gradle` names the root project `somegoogly` and includes `common`, `forge`, `fabric`, and
`neoforge`.
Plugin resolution uses this repository order:

1. Fabric Maven
2. Architectury Maven
3. Forge Maven
4. NeoForge Maven
5. Gradle Plugin Portal

The root build applies Loom, the Architectury plugin, Maven Publish, layered Mojang/Parchment mappings,
and the Java 21 toolchain to every module. Gradle receives a 3 GiB maximum heap and does not use the
daemon.

## Common module

The common module declares Fabric, Forge, and NeoForge as transform targets and uses
`common/src/main/resources/somegoogly.accesswidener` while compiling shared Minecraft code. Its
dependencies are:

- Architectury API 13.0.8 as a mod compile-only dependency;
- JSR-305 3.0.2 as a compile-only dependency.

The module has no JUnit test source set; automated verification is the GameTest suite in
`common/src/gametest` driven per loader.

The production common JAR excludes the Access Widener. Fabric copies the canonical 1.21.1 file into
its mod JAR beside `fabric.mod.json`. NeoForge and Forge copy the canonical Forge-family
`gradle/accesstransformer.cfg` into their processed resources; its 36 rules mirror the Access Widener.

Common resource processing expands `mod_id` in `pack.mcmeta`. Shared assets, recipes, language,
datapack eye definitions, and the GameTest structure fixture are production resources.

## Loader modules and packaging

All three loader modules apply Shadow and use Architectury's platform-specific Loom setup. Each has:

- a resolvable, non-consumable `common` configuration for common development output;
- a compile classpath extended from `common`;
- its Architectury development configuration extended from `common`;
- a `shadowBundle` containing the transformed common production artifact;
- a remapped release JAR built from the shadowed loader and common output.

Fabric and NeoForge also extend their raw runtime classpaths from `common`. Forge deliberately does
not: its development transformation already supplies common code, and adding the raw common JAR
would expose the same packages through two modules. Architectury transforms common
`@ExpectPlatform` calls in development and packaged artifacts. All three production and packaging
paths are verified.

### Forge

Forge's build target is `net.minecraftforge:forge:1.21.1-52.1.2`; GeckoLib's Forge 1.21.1 artifact
at 4.7.4 is compile-only. Architectury API 13 has no Forge platform artifact for Minecraft 1.21.1,
so Forge uses native content registration, networking, tracking, configuration, events, and client
integration behind the common project-owned seams. Architectury remains only a build-time
`@ExpectPlatform` transformer.

Main resource processing expands `META-INF/mods.toml`; GameTest resource processing independently
expands the development mod's metadata. Forge contains 13 production Java files and 17 GameTest
files. Its verified release artifact is `forge/build/libs/somegoogly-forge-0.8.2.jar`.

### Fabric

Fabric depends on Fabric Loader 0.15.11 and Fabric API 0.102.1+1.21.1. GeckoLib's Fabric 1.21.1
artifact at 4.7.4 is mod compile-only. JSR-305 is redeclared because common compile-only dependencies
do not propagate to loader compilation.

Main resource processing expands `fabric.mod.json` and copies the common Access Widener into the
Fabric resources. Metadata declares exact Minecraft 1.21.1, Java 21, the minimum Loader/API versions,
the Access Widener, `somegoogly.mixins.json`, GeckoLib 4.7.4 or newer as a suggestion, and older
installed GeckoLib versions as incompatible.

The verified release artifact is `fabric/build/libs/somegoogly-fabric-0.8.2.jar`.

### NeoForge

NeoForge targets `net.neoforged:neoforge:21.1.34` and the GeckoLib NeoForge 1.21.1 artifact at 4.7.4.
Architectury supplies build-time transformation only; GeckoLib is compile-only and metadata-optional
on the physical client. Main resource processing expands `neoforge.mods.toml` and copies the canonical
Forge-family Access Transformer containing the 36 rules required by the shared client code.

The module contains 13 production Java files covering bootstrap, native configuration, server
events, platform services, client registration/access, and the soft-loaded GeckoLib bridge. Common
resources are packaged through the transformed common artifact. The verified release artifact is
`neoforge/build/libs/somegoogly-neoforge-0.8.2.jar`.

The final release artifacts were verified by path and file metadata only:

| Loader | Artifact | Size | Last written (UTC-4) |
| --- | --- | ---: | --- |
| Fabric | `fabric/build/libs/somegoogly-fabric-0.8.2.jar` | 742,506 bytes | 2026-09-17 22:15:11 |
| NeoForge | `neoforge/build/libs/somegoogly-neoforge-0.8.2.jar` | 738,791 bytes | 2026-09-17 22:15:14 |
| Forge | `forge/build/libs/somegoogly-forge-0.8.2.jar` | 741,769 bytes | 2026-09-17 22:15:13 |

## GameTest source sets

Fabric, NeoForge, and Forge each define a `gametest` source set combining 98 shared public assertions
with 99 loader wrappers; the additional test exercises that loader's entity persistence through a
save/load round trip. Each uses a separate `somegoogly_gametest` development mod and exposes all 17
holders. All three dedicated servers discover and pass all 99 required tests and exit cleanly.

## Access configuration

`common/src/main/resources/somegoogly.accesswidener` is the canonical 1.21.1 access declaration used
by shared compilation and Fabric runtime. `gradle/accesstransformer.cfg` is the canonical Forge-family
translation copied into both Forge and NeoForge resources. The two representations cover the same 36
renderer collections and layers, render-type factory, model-part children, age-dependent model
transforms, and rabbit and llama parts referenced by current source.

## Wrapper files

The project uses the checked-in Gradle wrapper:

```text
gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
```

`gradle-wrapper.properties` downloads `gradle-9.5.1-bin.zip`, validates its URL, uses a 10-second
network timeout, and stores distributions beneath `GRADLE_USER_HOME/wrapper/dists`.

The checked-in wrapper file fingerprints are:

| File | SHA-256 |
| --- | --- |
| `gradlew` | `D8231D345AB33433AB7B2C0720D5BEB416C8D5C6789DBC01AD122B63BC2CAE0D` |
| `gradlew.bat` | `BDECF875B6868CBCBD36A1F85EEDF0832F358FF28092C5797ED645F7EDCE77D9` |
| `gradle-wrapper.jar` | `CB0DA6751C2B753A16AC168BB354870EBB1E162E9083F116729CEC9C781156B8` |

## `build-env` copy

`build-env/` is a copy of the active build scripts, kept so the build configuration can be reviewed
or reconstructed without the full source tree. Every file in it must match its root or module
counterpart byte-for-byte:

```text
build.gradle
settings.gradle
gradle.properties
gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.properties
common/build.gradle
fabric/build.gradle
forge/build.gradle
forge/gradle.properties
neoforge/build.gradle
neoforge/gradle.properties
```

`gradle-wrapper.jar` is deliberately absent: `gradlew` together with `gradle-wrapper.properties`
(pinned to `gradle-9.5.1-bin.zip`) regenerate it through `gradle wrapper`. The files at the
repository root and under `common`, `fabric`, `forge`, and `neoforge` are authoritative on any
disagreement.
