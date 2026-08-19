# Some Googly Eyes Build Environment

This records the toolchain and dependency versions this mod's Gradle build actually uses, as part of
converting it from a single Forge module (ForgeGradle) to a dual-loader Forge/Fabric mod built with
Architectury Loom. See [as-built.md](as-built.md) for the runtime architecture; that document still
describes the pre-conversion, single-module code and predates this build-environment work.

The versions below come from `working-build-env/`, a known-working dual-loader reference copied from
a different mod, Some Stacks, and proven on other mod conversions on this machine. `working-build-env/`
is a template that was adapted, not copied blindly: its `mods.toml` and `fabric.mod.json` still carry
stale branding from an even earlier template mod and were deliberately not carried over (see
Environment traps).

## Status

**Confirmed working:** the root `common`/`forge`/`fabric` skeleton syncs cleanly end to end —
`:common`, `:forge`, and `:fabric` all configure, all three remap successfully, and Loom generates its
usual per-loader run-task scaffolding (`generateLog4jConfig`, `generateDLIConfig`, `configureLaunch`,
`downloadAssets`, `configureClientLaunch`, `ideaSyncTask`), ending in `BUILD SUCCESSFUL`. IntelliJ has
since generated four working run configurations from that sync: `Minecraft Client (:forge)`,
`Minecraft Server (:forge)`, `Minecraft Client (:fabric)`, `Minecraft Server (:fabric)`. Per-loader dev
run directories (`forge/run/`, `fabric/run/`) now exist on disk, created by that sync.

**Not yet done:** no code has moved. `src/main` is still the entire pre-conversion, flat, Forge-only
tree — unreferenced by any of the three subprojects. Specifically still pending: the GameTest
source-set relocation, the access-transformer/Access-Widener rework, GeckoLib's dependency wiring, real
`mods.toml`/`fabric.mod.json` content, the actual `src/main` → `common`/`forge` code split, and cleanup
of the four now-stale ForgeGradle-era run configs under `.idea/runConfigurations/` (`runClient.xml`,
`runServer.xml`, `runGameTestServer.xml`, `runData.xml` — still present, dated before this conversion
started, and no longer reference any task that exists in the build).

## Toolchain

Confirmed active via the successful sync (Architectury Plugin and Loom both printed their versions and
configured without error):

| Tool | Active declaration |
| --- | --- |
| Gradle | Wrapper pinned in `gradle/wrapper/gradle-wrapper.properties` → **9.5.1** |
| Java toolchain | Language version 17; source/target 17; `--release 17` |
| IntelliJ Gradle JVM | `.idea/gradle.xml` → `gradleJvm="21"` |
| Architectury Loom | `1.17.491` |
| Architectury Plugin | `3.5.169` |
| Shadow (`com.gradleup.shadow`) | `9.4.3` |
| Mixin patched by Loom | transitive |

This mod has zero authored Mixins and the conversion hasn't required adding any — the Mixin dependency
arrives transitively through Loom's Forge patching regardless of whether the mod itself uses Mixin.

This machine resolves `java` on the command line through:

```text
C:\Users\Dad\AppData\Local\Programs\Eclipse Adoptium\jdk-17.0.15.6-hotspot
```

It also has Temurin 21.0.2+13, Oracle JDK 17.0.9+11, and a Java 8 JRE installed. IntelliJ uses JDK 21
to run Gradle itself, while the project's Java toolchain still forces compilation and every Gradle
`JavaExec` task — including Minecraft development runs — onto Java 17. Command-line wrapper invocations
use the Java 17 `JAVA_HOME`. There is no global `gradle` command; the wrapper is the build entry point.

`gradle.properties` gives Gradle 3 GiB with `org.gradle.jvmargs=-Xmx3G` and disables the persistent
daemon with `org.gradle.daemon=false`. Loom and the Architectury Plugin are pinned to the numbered
versions above rather than moving snapshot aliases, matching the proven reference.

## Minecraft and library versions

Confirmed active — the sync's log showed Loom merging and remapping mappings/mods against these exact
pins (`:merging mappings (InstallerTools, srg + mojmap)`, `:remapped 1 mods (intermediary -> named)` for
`:common`, `:remapped 56 mods` for `:fabric`, `:remapped 1 mods (srg -> named)` for `:forge`):

| Component | Build version | Declared runtime constraint |
| --- | --- | --- |
| Minecraft | **1.20.1** | exactly `[1.20.1]` on Forge; exactly `1.20.1` on Fabric |
| Mappings | Mojang official plus Parchment **2023.09.03-1.20.1** | development only |
| Forge | **1.20.1-47.4.10** | Forge `[47.4.10,48)`; FML `[47,48)` |
| Fabric Loader | **0.19.3** | `>=0.19.3` |
| Fabric API | **0.92.11+1.20.1** | `>=0.92.11+1.20.1` |
| Architectury API | **9.2.14** | `>=9.2.14` / `[9.2.14,)` |
| JSR-305 | **3.0.2** | compile-only annotation dependency |

This tightens both the Minecraft and Forge ranges relative to this mod's pre-conversion pins (which
were the looser `[1.20.1,1.21)` / `[47,)` / Forge `47.4.0`), matching the proven reference's own
reasoning: this mod's client rendering hooks reach deep enough into Minecraft/Forge internals that they
are not guaranteed stable across the 1.20.x line, so the narrower range is the right choice here too, if
anything more so than for the mod it was proven on.

There is no JUnit row: unlike the proven reference, this mod has no `common`-module JUnit tests today
(only the 67 Forge GameTests). Add a JUnit row here only once a real common-module test suite exists —
`common/build.gradle` already carries the JUnit BOM/Jupiter/platform-launcher dependencies and
`useJUnitPlatform()` wiring from the reference, ready and unused until then.

### GeckoLib — not in the proven reference at all, and not yet wired in

GeckoLib is this mod's one genuine cross-loader optional compile dependency, and Some Stacks doesn't
have an analog for it. Not yet added to `forge/build.gradle` or `fabric/build.gradle`:

| Component | Current Forge pin | Fabric equivalent |
| --- | --- | --- |
| GeckoLib | `software.bernie.geckolib:geckolib-forge-1.20.1:4.7.4` (`compileOnly fg.deobf(...)`, pre-conversion) | **TBD** — verify the matching `geckolib-fabric-1.20.1` release against GeckoLib's own published listing before pinning it |

GeckoLib does publish matching Forge/Fabric artifacts under the same version number (confirmed against
another dual-loader mod's pins), so a same-numbered Fabric release almost certainly exists — but the
exact coordinate should be verified, not guessed. Also verify what replaces `fg.deobf(...)` under Loom:
GeckoLib's own artifacts may already be pre-remapped, in which case plain `compileOnly`/`modCompileOnly`
is enough; confirm rather than assume.

### Everything else needs no build-environment entry

Citadel, LLibrary, Alex's Mobs, Exotic Birds, and the rest of the datapack-integrated mods
(Ars Nouveau, Autumnity, Hamsters, Ice and Fire, Immersive Engineering, Mowzie's Mobs, Simply Cats,
Twilight Forest) are all reached through reflection or class-name matching in this mod's code, with no
compile-time dependency at all. They need no version pin, no repository declaration, and no dependency
row in either loader's `build.gradle` — only GeckoLib does.

## Module and packaging setup

The three-subproject skeleton now exists and syncs cleanly:

```text
common/   shared source and resources; transformed into each loader artifact
forge/    Forge entry points and integration
fabric/   Fabric entry points and integration
```

Both loader modules compile against `common` through Architectury's `common` configuration and bundle
its transformed production output through `shadowBundle`. Development runs instead group the common
and loader source sets into one logical mod under `loom.mods.main`. The target production artifacts are:

```text
forge/build/libs/somegoogly-forge-<version>.jar
fabric/build/libs/somegoogly-fabric-<version>.jar
```

The `*-dev-shadow.jar` and `*-sources.jar` files in those directories will be development artifacts,
not release JARs. Neither has been produced yet — no code or resources exist in any subproject to build.

Today's entire `src/main` tree is still flat, Forge-only, and unreferenced by any subproject — this
conversion has only stood up the build environment around it so far. This document intentionally does
not enumerate which existing packages become `common` vs. stay Forge-only; that is an implementation-
level exercise, not an environment plan. The one asymmetry worth flagging here: unlike a typical
Architectury migration, which splits *existing* dual-loader code, essentially all of `fabric/src/main`
will be new code — there is no existing Fabric networking, config, or entity-persistent-data
implementation to port, since this mod has never run on Fabric.

## IntelliJ run configurations

The sync generated four working run configurations under `.idea/runConfigurations/`:
`Minecraft_Client___forge__forge.xml`, `Minecraft_Server___forge__forge.xml`,
`Minecraft_Client___fabric__fabric.xml`, `Minecraft_Server___fabric__fabric.xml`. All four launch
through `dev.architectury.transformer.TransformerRuntime`; Forge uses `BootstrapLauncher`, Fabric uses
Knot. Per-loader dev run directories (`forge/run/`, `fabric/run/`) were created alongside them.

No `Game Test Server` run configuration exists yet for either loader — expected, since neither
`forge/build.gradle` nor `fabric/build.gradle` has the GameTest source set or `loom.runs.gameTestServer`
block yet (see below). That's the next piece of run-configuration work, not a sync problem.

The four ForgeGradle-era configs — `runClient.xml`, `runServer.xml`, `runGameTestServer.xml`,
`runData.xml` — are still sitting in `.idea/runConfigurations/`, untouched by this conversion. They
predate it and reference Gradle tasks (`:prepareRunGameTestServerCompile`, the
`forgegametestserveruserdev` launch target, etc.) that no longer exist in this build. They have not
been deleted; do that once the Loom-generated configs above are confirmed to actually launch. `runData`
in particular should not be recreated in any form — this mod has no data generation
(`GatherDataEvent`/`DataGenerator` are unused, and there is no `src/generated`), so it was vestigial
ForgeGradle template cruft even before the conversion.

### GameTest source-set relocation is required, not optional

This mod's 67 GameTests currently live in ordinary main source under
`src/main/java/com/github/crittscott/somegoogly/gametest/`, run pre-conversion by ForgeGradle's built-in
`gameTestServer` run with `forge.enabledGameTestNamespaces=somegoogly`. Loom's `forge()`/`fabric()`
require the separate-dev-mod trick the proven reference uses instead: a shared `common/src/gametest`
source set, each loader's own `somegoogly_gametest` `loom.mods` entry, a stub `mods.toml`/
`fabric.mod.json`, a `pack.mcmeta`, and a Base64-encoded empty structure fixture
(`forge/src/gametest/fixtures/somegoogly_empty.nbt.b64`, and the Fabric equivalent) decoded at build
time into `data/somegoogly/structures/somegoogly_empty.nbt`. Concretely: the existing `gametest`
package must be physically relocated out of `src/main/java` into `common/src/gametest/java` before the
Loom run configurations can discover it — a launch that finds zero tests can otherwise look like a
clean run. Fabric's GameTest run additionally passes `-Dfabric-api.gametest`.

## Environment traps

- **Confirmed: any deviation from `working-build-env/`'s root `settings.gradle` can break the sync
  outright, even a seemingly-unrelated addition.** Adding the
  `org.gradle.toolchains.foojay-resolver-convention` plugin and reordering
  `pluginManagement.repositories` (putting `gradlePluginPortal()` first instead of last) broke
  `:common`'s Loom extension configuration with a `Gson 2.9.1` `ReflectionAccessFilter`
  `IllegalAccessException` while calculating the `minecraftJarConfiguration` property — a failure deep
  inside Loom's own bundled Gson usage, unrelated to anything in `common/build.gradle`'s actual content.
  Reverting `settings.gradle` to match the reference exactly (no foojay plugin; repos in the order
  fabric → architectury → forge → `gradlePluginPortal()` last) fixed it immediately. The root cause
  wasn't fully isolated between the two changes, and it may be sensitive to this machine's exact
  JDK/Gson/Loom combination rather than a documented incompatibility — but don't reintroduce either
  change without retesting a full `:common`/`:forge`/`:fabric` sync from a clean state.
- **Compile-only dependencies from `common` do not automatically reach loader compilation.**
  Architectury's `common` and `shadowBundle` configurations carry common output, not all of its
  dependency declarations. Fabric will need to redeclare JSR-305 for `javax.annotation.Nullable`;
  Forge receives it transitively through its own dependency graph.
- **Development must expose common and loader output as one logical mod.** Each loader's
  `loom.mods.main` needs both source sets. Adding common as a separate runtime mod can produce
  duplicate loading or Forge JPMS split-package failures; production bundling belongs in
  `shadowBundle`.
- **The Forge `gametest` source set needs main output on both classpaths.** Main's dependency
  classpath alone does not contain the mod's own compiled classes; explicit `sourceSets.main.output`
  additions in `forge/build.gradle` are required, matching the proven reference.
- **Forge scans GameTests only from registered Loom mod output.** The `somegoogly_gametest`
  `loom.mods` entry, its `mods.toml`, stub `@Mod` class, and `pack.mcmeta` are all part of making the
  custom source set visible to FML and making its structure resource load.
- **The generated GameTest NBT is not a source file.** Edit the Base64 fixture under
  `*/src/gametest/fixtures`; the decoded file under `*/build/generated` is disposable build output.
- **Forge and Fabric register GameTest classes differently.** Forge discovers test methods by
  scanning the loaded mod for classes annotated `@GameTestHolder`; Fabric requires each class to
  implement `FabricGameTest` and to be listed under a `fabric-gametest` entrypoint in the dev-mod's
  own `fabric.mod.json`. A new Fabric GameTest class left off that list silently does not run.
- **`working-build-env/` is an inactive reference tree.** Changing files there does not change the
  root build.
- **The access transformer needs rework on both loaders, not a straight copy.** This mod's whole
  model-attachment/resolver architecture depends on
  `src/main/resources/META-INF/accesstransformer.cfg`, which widens `ModelPart.children`,
  `AgeableListModel`/`AgeableHierarchicalModel`'s baby-scale fields, `RabbitModel`/`LlamaModel`'s
  per-part fields, and `LivingEntityRenderer.layers`. Two problems the proven reference never had to
  solve, since it has no AT at all:
  - The file's own header notes its entries are named by **SRG id** because ForgeGradle applies the
    AT to the SRG-mapped jar. Loom's Forge patching pipeline may apply ATs to the
    **official-named (Mojmap)** jar instead — verify this before assuming the file can be reused
    as-is; it may need rewriting to official member names.
  - **Fabric has no access-transformer equivalent.** The Fabric analog is an Access Widener — a
    different file format and mappings namespace, declared via `loom.accessWidenerPath` and
    `fabric.mod.json`'s `"accessWidener"` key — and it will need to be authored from scratch to grant
    the same widened access, or this mod's resolvers simply won't work on Fabric.
- **GeckoLib is the one dependency this mod has that the proven reference doesn't.** Confirm the
  Fabric-side artifact/version and the Loom-side equivalent of `fg.deobf(...)` before pinning it (see
  above) — don't assume it behaves like a dependency-free reflection integration the way Citadel/
  LLibrary/Alex's Mobs do.
- **`working-build-env/`'s own `mods.toml`/`fabric.mod.json` are unfinished templates, not a
  copy-paste source.** They still carry branding from an earlier template mod — `issueTrackerURL`,
  `displayURL`, `logoFile`, and even the Fabric entrypoint class name all say "SomeBuckets," not
  "Some Stacks" or "Some Googly Eyes." Write real `somegoogly` values instead of copying these files
  verbatim.
- **Each loader gets its own dev run directory, confirmed.** `forge/run/` and `fabric/run/` now exist,
  created by the sync's `downloadAssets`/`configureLaunch` tasks. The old single shared `run/` directory
  from the pre-conversion ForgeGradle setup still exists alongside them but is no longer used by either
  loader; its dev-world state and `run/server.properties` were not carried over.

## Build and test commands

Use the wrapper from the repository root in PowerShell:

```powershell
.\gradlew build
.\gradlew :forge:build
.\gradlew :fabric:build
.\gradlew :forge:runGameTestServer
.\gradlew :fabric:runGameTestServer
```

The root `build` will cover the configured subproject builds. There is no `:common:test` command
until a real common-module JUnit suite exists — this mod currently verifies server and shared logic
entirely through its GameTests, not JUnit. `runGameTestServer` isn't runnable on either loader yet;
that needs the GameTest source-set relocation above first.
