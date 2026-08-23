# Some Googly Eyes Build Environment

This document records the working Architectury Loom build environment. It describes the final build
layout and the compatible tool versions in use. Runtime architecture is covered by `as-built.md`.

## Version set

These versions come from the active wrapper, Gradle scripts, properties, and loader metadata.

| Component | Active version or constraint |
| --- | --- |
| Gradle wrapper | 9.5.1 |
| Java compilation and development runs | Java 17 |
| Current command-line JDK | Eclipse Temurin 17.0.15+6 |
| IntelliJ Gradle JVM | JDK 21 |
| IntelliJ project language level | Java 17 |
| Minecraft | exactly 1.20.1 |
| Architectury Loom | 1.17.491 |
| Architectury Gradle plugin | 3.5.169 |
| Architectury API | 9.2.14; runtime minimum 9.2.14 |
| Forge compile dependency | 1.20.1-47.4.0 (experimental floor) |
| Forge runtime range | `[47.4.0,48)` (experimental floor) |
| FML runtime range | `[47,48)` |
| Fabric Loader | 0.19.3; runtime minimum 0.19.3 |
| Fabric API | 0.92.11+1.20.1; runtime minimum the same version |
| Mappings | Mojang official plus Parchment 2023.09.03 for Minecraft 1.20.1 |
| Shadow plugin | 9.4.3 |
| GeckoLib | 4.7.4, compile-only and optional at runtime |
| JSR-305 | 3.0.2, compile-only |
| JUnit BOM | 5.10.2 |
| Mod version | 0.8.1 |

The Gradle runtime JVM and compilation toolchain are separate. IntelliJ runs Gradle with JDK 21, but
every subproject requests a Java 17 toolchain, sets source and target compatibility to 17, and compiles
with `--release 17`. Command-line Gradle can run under the current Java 17 installation.

Minecraft is exact because the renderer integration, Access Widener, Access Transformer, and Fabric
Mixins refer to 1.20.1 internals. Forge compilation is pinned to the minimum Forge version accepted at
runtime. Fabric compilation likewise uses the declared minimum Loader and API versions.

The Forge floor is experimental at 47.4.0, lowered from a prior 47.4.10 baseline to reach modpacks
(e.g. All the Mods 9) pinned to earlier 47.4.x builds. The mod uses no Forge API added within the
47.4.x line, so the lower floor is expected to work, but it has not been verified against every patch
between 47.4.0 and 47.4.10.

## Project layout

The Gradle project includes exactly three modules:

```text
common/   shared production code, resources, and GameTest assertion logic
forge/    Forge adapters, metadata, access transformation, and GameTest wrappers
fabric/   Fabric adapters, metadata, Mixins, and GameTest wrappers
```

The root `src/` tree is not part of any active source set.

`settings.gradle` names the root project `somegoogly` and includes `common`, `forge`, and `fabric`.
Plugin resolution uses this repository order:

1. Fabric Maven
2. Architectury Maven
3. Forge Maven
4. Gradle Plugin Portal

The root build applies Loom, the Architectury plugin, Maven Publish, layered Mojang/Parchment mappings,
and the Java 17 toolchain to every module. Gradle receives a 3 GiB maximum heap and does not use the
daemon.

## Common module

The common module declares both target platforms through Architectury and uses
`common/src/main/resources/somegoogly.accesswidener` while compiling shared Minecraft code. Its
dependencies are:

- Fabric Loader 0.19.3 for portable environment annotations;
- Architectury API 9.2.14 as a mod compile-only dependency;
- JSR-305 3.0.2 as a compile-only dependency;
- JUnit Jupiter through BOM 5.10.2 for its conventional test source set.

The production common JAR excludes the Access Widener. Fabric copies the canonical file into its mod
JAR beside `fabric.mod.json`; Forge supplies equivalent access through Forge patches and
`META-INF/accesstransformer.cfg`.

Common resource processing expands `mod_id` in `pack.mcmeta`. Shared assets, recipes, language,
datapack eye definitions, and the GameTest structure fixture are production resources.

## Loader modules and packaging

Both loader modules apply Shadow and use Architectury's platform-specific Loom setup. Each has:

- a resolvable, non-consumable `common` configuration for common development output;
- compile and runtime classpaths extended from `common`;
- its Architectury development configuration extended from `common`;
- a `shadowBundle` containing the transformed common production artifact;
- a remapped release JAR built from the shadowed loader and common output.

This arrangement allows Architectury to transform common `@ExpectPlatform` calls in development and
in packaged artifacts. Common and loader packages remain disjoint for Forge's module layer.

### Forge

Forge depends on `net.minecraftforge:forge:1.20.1-47.4.0` and
`architectury-forge:9.2.14`. GeckoLib's Forge 1.20.1 artifact at 4.7.4 is compile-only. Runtime
metadata requires the configured Minecraft, Forge, FML, and Architectury ranges.

`forge/gradle.properties` sets `loom.platform=forge`. Main resource processing expands
`META-INF/mods.toml`; GameTest resource processing independently expands the development mod's
metadata.

### Fabric

Fabric depends on Fabric Loader 0.19.3, Fabric API 0.92.11+1.20.1, and
`architectury-fabric:9.2.14`. GeckoLib's Fabric 1.20.1 artifact at 4.7.4 is mod compile-only. JSR-305
is redeclared because common compile-only dependencies do not propagate to loader compilation.

Main resource processing expands `fabric.mod.json` and copies the common Access Widener into the
Fabric resources. Metadata declares exact Minecraft 1.20.1, the minimum Loader/API versions, the
Access Widener, `somegoogly.mixins.json`, and GeckoLib as a suggestion.

## GameTest source sets

Each loader defines a `gametest` source set containing common assertion logic and loader-specific
wrappers. Its classpaths include the corresponding main source-set output and dependencies. There are
77 shared public assertion methods and 78 wrapper test methods per loader; the additional wrapper test
exercises loader-integrated behavior.

Forge exposes the source set as the `somegoogly_gametest` development mod and defines a
`gameTestServer` Forge run. Fabric supplies its own development-mod metadata, lists all wrapper entry
points, enables `fabric-api.gametest`, and defines its Fabric `gameTestServer` run.

## Access configuration

`common/src/main/resources/somegoogly.accesswidener` is the canonical access declaration for shared
code. `forge/src/main/resources/META-INF/accesstransformer.cfg` supplies the Forge access not already
provided by Forge patches. Together they cover the vanilla model parts, renderer collections and
layers, age-dependent model transforms, and render factory used by shared rendering.

The Access Widener uses named Mojang/Parchment members. The Forge Access Transformer uses the SRG
member names expected by the Forge toolchain. The two files represent equivalent effective access,
not identical textual declarations.

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

## `working-build-env` snapshot

`working-build-env/` is an exact-path snapshot of every checked-in file that configures, launches, or
supplies metadata and access rules to the active build:

```text
build.gradle
settings.gradle
gradle.properties
gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
common/build.gradle
common/src/main/resources/pack.mcmeta
common/src/main/resources/somegoogly.accesswidener
forge/build.gradle
forge/gradle.properties
forge/src/main/resources/META-INF/mods.toml
forge/src/main/resources/META-INF/accesstransformer.cfg
forge/src/gametest/resources/META-INF/mods.toml
fabric/build.gradle
fabric/src/main/resources/fabric.mod.json
fabric/src/main/resources/somegoogly.mixins.json
fabric/src/gametest/resources/fabric.mod.json
```

Files in this snapshot are copies, not build inputs. The active files at the repository root and
under the three modules remain authoritative.
