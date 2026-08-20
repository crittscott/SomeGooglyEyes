# Some Googly Eyes Build Environment

This document describes the active Architectury Loom build. For runtime ownership and invariants, see
`as-built.md`; for the current verification state and phased continuation plan, see
`fabric-port-verification-handoff.md`.

## Status

The active Gradle project is a three-module Forge/Fabric build:

```text
common/   loader-neutral source, resources, and shared GameTest logic
forge/    Forge entry point, adapters, metadata, AT, and GameTest wrappers
fabric/   Fabric entry points, adapters, metadata/Mixins, and GameTest wrappers
```

The legacy root `src/` tree remains in the repository but is not a source set of any included Gradle
project. It is an inactive reference and must not receive live changes.

The current port is complete at source level. The user confirmed successful Forge and Fabric builds
for checkpoint commit `4039ada` on 2026-08-20. GameTests and systematic runtime parity checks remain;
see `fabric-port-verification-handoff.md`.

## Toolchain and pinned versions

| Component | Version / constraint |
| --- | --- |
| Java | 17 |
| Minecraft | exactly 1.20.1 |
| Architectury Loom | 1.17.491 |
| Architectury plugin | 3.5.169 |
| Architectury API | 9.2.14; runtime minimum 9.2.14 |
| Forge | 1.20.1-47.4.10; runtime range `[47.4.10,48)` |
| FML | `[47,48)` |
| Fabric Loader | 0.19.3 minimum |
| Fabric API | 0.92.11+1.20.1 minimum |
| Mappings | Mojang official plus Parchment 2023.09.03 for 1.20.1 |
| Shadow plugin | 9.4.3 |
| GeckoLib | 4.7.4, optional runtime / compile-only |
| JSR-305 | 3.0.2, compile-only |
| JUnit | 5.10.2 dependencies present; no JUnit suite |

Minecraft is deliberately exact on both loaders. The client renderer, Access Transformer/Access
Widener, and Mixins name Minecraft internals that are not stable across 1.20.x.

## Root build

`settings.gradle` includes only `common`, `forge`, and `fabric`. Its plugin repository order is a known
environment constraint and should remain:

1. Fabric Maven
2. Architectury Maven
3. Forge Maven
4. Gradle Plugin Portal

A previous attempt to add Foojay toolchain resolution and reorder these repositories caused Loom's
configuration to fail inside its bundled Gson path. Do not reintroduce either change without a clean
three-module sync test.

The root build applies Loom, Architectury, Maven Publish, Mojang mappings, Parchment, and the Java 17
toolchain to every subproject. `org.gradle.jvmargs=-Xmx3G`; the daemon is disabled.

## Common module

`common/src/main/java` compiles without Forge, Fabric API, GeckoLib, or Forge-only patched members. It
owns shared client rendering and picker code as well as gameplay code. Its canonical access widener
exposes the vanilla members that shared render code needs; Fabric applies that file directly and Forge
supplies equivalent access through patches or its Access Transformer. Common otherwise depends on Fabric Loader only for portable
environment annotations, Architectury API, and JSR-305.

`common/src/gametest/java` contains plain assertion logic. It is compiled as part of each loader's
custom `gametest` source set, not as common production code.

Common remains a transformed development dependency rather than a second source set of each loader
mod. Fabric resource processing copies the canonical common access widener beside `fabric.mod.json`,
because Fabric Loader resolves that declaration within the metadata-owning mod container. The common
JAR omits its redundant copy; Forge supplies runtime access through patches and its AT.

## Loader packaging

Each loader declares two relationships to common:

- `common(project(... namedElements))` places common output on the compile classpath;
- `shadowBundle project(... transformProduction<Loader>)` bundles the transformed common production
  output into the release artifact.

The compile and runtime classpaths plus `developmentForge`/`developmentFabric` extend from `common`.
This lets Architectury transform common's `@ExpectPlatform` calls in development. Loader source
packages are disjoint from common packages, avoiding split packages in Forge's Java module layer.

## Forge module

Forge retains loader-specific pieces only:

- `@Mod` entry point and FML/MinecraftForge event adapters;
- `ForgeConfigSpec` storage adapters;
- `@ExpectPlatform` implementations;
- Forge entity-tracking packet distribution;
- Forge optional-mod checks and item renderer attachment;
- `META-INF/accesstransformer.cfg`;
- Forge metadata and GameTest dev-mod wrappers.

Architectury API is a mandatory Forge runtime dependency in `mods.toml`. GeckoLib is `compileOnly` and
remains a soft runtime dependency.

The Forge `gametest` source set includes common test logic, Forge wrappers, main output, and main
runtime dependencies. Loom exposes it as `somegoogly_gametest`, whose stub `@Mod` and `mods.toml` make
Forge scan the wrapper classes.

## Fabric module

Fabric contains the main and client entry points, API callback adapters, config storage, platform
implementations, and four Mixins. Its metadata declares:

- exact Minecraft 1.20.1;
- minimum Loader, Fabric API, and Architectury API versions;
- `somegoogly.accesswidener`;
- `somegoogly.mixins.json`;
- GeckoLib as a suggestion, not a dependency.

Fabric redeclares JSR-305 because compile-only dependencies from common do not propagate to a loader's
compilation. It uses:

```groovy
modCompileOnly "software.bernie.geckolib:geckolib-fabric-1.20.1:4.7.4"
```

from GeckoLib's Cloudsmith Maven repository. The corresponding Forge artifact is
`geckolib-forge-1.20.1:4.7.4`.

The Fabric `gametest` source set includes common test logic, Fabric wrappers, and main output. Loom's
GameTest server passes `-Dfabric-api.gametest`. The dev mod lists every wrapper explicitly under the
`fabric-gametest` entrypoint; adding a wrapper class without adding it to that list silently omits it.

## Access Transformer and Access Widener

Common's `somegoogly.accesswidener` declares every non-public vanilla member used by common code.
Forge's patches or `META-INF/accesstransformer.cfg` must supply equivalent access. These expose:

- `ModelPart.children`;
- `EntityRenderDispatcher.renderers` and `playerRenderers` on Fabric;
- `AgeableListModel.headParts()` and `bodyParts()`;
- `AgeableListModel` baby scale/offset fields;
- `AgeableHierarchicalModel` baby fields;
- `LivingEntityRenderer.layers`;
- `LivingEntityRenderer.addLayer` on Fabric;
- the full `RenderType.create` factory on common and Fabric;
- Rabbit and Llama top-level model-part fields.

Shared users belong under `common/src/main/java`. Adding a widened member requires updating the common
AW and adding a Forge AT entry only when Forge does not already expose it.

## Resource processing

Common expands the mod id in `pack.mcmeta`. Each loader expands its own metadata placeholders. Each
GameTest source set separately expands its dev-mod metadata.

The production resources used by both loaders live under `common/src/main/resources`, including the
eye definitions, assets, recipes, language, and `data/somegoogly/structures/empty.nbt`. There is no
generated Base64 fixture in the active setup.

## Run configurations

Loom owns separate Forge and Fabric run directories and launch configurations. The old root `run/`
directory and pre-Loom IntelliJ configurations are legacy state, not inputs to the current runs.

Both loader build files define a `gameTestServer` run. A GameTest process which discovers zero tests is
not a pass; confirm 69 executions and success on each loader.

## Verification commands

From PowerShell at the repository root, when separately authorized:

```powershell
.\gradlew :common:compileJava
.\gradlew :forge:compileJava :fabric:compileJava
.\gradlew :forge:build :fabric:build
.\gradlew :forge:runGameTestServer
.\gradlew :fabric:runGameTestServer
```

The repository's `CLAUDE.md` currently forbids the agent from running these commands unless the user
explicitly requests a build/test pass. It also forbids decompiling, unarchiving dependencies, and
inspecting Gradle caches as a substitute.

There is no meaningful `:common:test` suite at present. Shared behavior is covered through 69 common
GameTest logic methods, each exposed by a thin wrapper on both loaders.

## Environment traps

- Do not edit the inactive root `src/` tree expecting a loader build to see the change.
- Keep GeckoLib types out of common; loader implementations own those optional API references.
- Keep loader source packages disjoint from common packages.
- Do not assume common compile-only dependencies propagate to loader compilation.
- Keep the standard Architectury common runtime and development configurations intact so platform
  substitutions run in development.
- Do not omit a Fabric GameTest wrapper from `fabric/src/gametest/resources/fabric.mod.json`.
- Do not add an empty version range to the Forge GameTest dev-mod dependency table; Forge interprets it
  as unsatisfiable.
- Do not copy metadata from `working-build-env/`; it is an inactive template with unrelated branding.
- Do not treat a successful Gradle configuration or artifact remap as runtime parity. Verify both
  dedicated servers, both GameTest runs, client rendering, persistence, networking, and picker flows.
