<!-- FIXED HEADER: not content. Do not edit, trim, or count toward the size limit. -->
> **Orientation snapshot, not a specification.** Describes the code as it currently is; the code wins on any disagreement. See CLAUDE.md § Orientation files.
<!-- END FIXED HEADER -->

# Some Googly Eyes — build orientation

This document is an orientation to the repository's build environment: its entry points, module layout, version authorities, dependency baselines, and packaging flow. It describes the setup as it exists. It is not build history, a troubleshooting log, release documentation, or a conversation.

`build-env/` is a reference snapshot of the repository's build environment: every checked-in Gradle build input including the wrapper, plus the agent instructions, the build-runner script, the Git settings, and outlines of the three orientation files. It preserves repository-relative paths so that files can be compared or restored without guessing where they belong. The active files at the repository root and under `common/`, `fabric/`, `forge/`, `neoforge/`, and `gradle/` remain authoritative; Gradle does not read the copies. Keep the snapshot and this description synchronized whenever an active file it covers changes.

The snapshot holds verbatim copies of the manually maintained files that define or launch this Gradle build (Gradle scripts, Gradle properties, wrapper launchers, and wrapper files), the agent instructions (`CLAUDE.md`, `AGENTS.md`, `code-review.md`, `code-conventions.md`, `platform-notes.md`), `gradlews.ps1`, which runs the standard Gradle task sequence and reports each step's result, and the Git settings (`.gitignore`, `.gitattributes`). The orientation entries are outlines, not copies: each holds its file's fixed header followed by every heading in order, with no body text, so it records the files' format without duplicating content that changes with the code. It should not contain caches, generated output, IDE state, run directories, resolved dependency JARs, mod source or resources, GameTest fixtures, loader manifests, or release notes. The wrapper JAR is the sole binary because it is itself a checked-in build launcher.

## Snapshot contents

Each copy sits at `build-env/` followed by its active path.

| Kind | Active paths |
| --- | --- |
| Gradle build | `settings.gradle`, `build.gradle`, `gradle.properties`, `common/build.gradle`, `fabric/build.gradle`, `fabric/gradle.properties`, `forge/build.gradle`, `forge/gradle.properties`, `neoforge/build.gradle`, `neoforge/gradle.properties` |
| Gradle wrapper | `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar` |
| Agent instructions | `CLAUDE.md`, `AGENTS.md`, `code-review.md`, `code-conventions.md`, `platform-notes.md` |
| Build runner | `gradlews.ps1` |
| Git settings | `.gitignore`, `.gitattributes` |
| Orientation outlines | `orientation-player.md`, `orientation-code.md`, `orientation-build.md` |

## Build shape

This checkout targets Minecraft 1.21.11. The project is a Groovy-DSL Gradle build with `common`, `fabric`, `forge`, and `neoforge` subprojects; `settings.gradle` includes the loaders named by `enabled_platforms`. The root build applies Architectury Loom and the Architectury plugin to each subproject and establishes shared Minecraft mappings and Java settings. The build declares no Maven publication; the modules expose no stable public API and nothing consumes them through Maven. `common` is transformed for Fabric, Forge, and NeoForge; each loader module bundles its transformed common output with Shadow and then remaps the resulting production JAR. Architectury is build-time only: the plugin supplies `@ExpectPlatform` and its transformation, and no loader requires Architectury API at runtime.

`common` depends on Fabric Loader only for the Mixin annotations its common Mixins use. It also compiles the GeckoLib render integration against the compile-only `geckolib-common` artifact from GeckoLib's Cloudsmith Maven, and each loader module compiles its GeckoLib render-layer listener against that loader's compile-only GeckoLib artifact; GeckoLib is optional at runtime, loaded in development runs, and not bundled. `geckolib_minecraft` selects its Minecraft 1.21.11 artifacts. NeoForge development runs and GameTests load Heads 3.1.0 via `modLocalRuntime`, pinned by the Modrinth version id in `heads_version` and resolved through an exclusive Modrinth Maven repository. Heads is optional, never compiled against or bundled.

Fabric bundles NightConfig core and TOML as nested dependencies through Loom's `include` configuration; the parser is an implementation dependency during development. Every module declares Maven Central, which supplies these modules and the shared annotation dependency.

`common` declares the access widener `common/src/main/resources/somegoogly.accesswidener` through `loom.accessWidenerPath`; every loader points its own `loom.accessWidenerPath` at common's. Forge and NeoForge, which read access transformers rather than wideners, also name that widener in `atAccessWideners` so their production JARs carry the converted rules.

Fabric Loom uses the legacy Mixin annotation processor. `somegoogly-fabric.mixins.json` names `somegoogly.refmap.json`, and the merged `somegoogly-common.mixins.json` names `somegoogly-common.refmap.json` for Fabric’s intermediary names. Forge registers the common config through `loom.forge.mixinConfig`, and NeoForge registers it through mod metadata. Both run on Mojang names and exclude the common refmap from their shadow JARs.

Forge and NeoForge copy common's `pack.mcmeta` into their own processed resources so loader-owned data is recognized in development. Their shadow tasks retain one copy of that shared metadata. It declares `min_format` `[64, 0]` and `max_format` `[94, 1]`, covering client resource-pack format 75.0 and data-pack format 94.1. The lower bound stays below both legacy thresholds (resource 65, data 82), so both readers accept the required legacy `pack_format` 94 and `supported_formats` 64 through 94. Shared GameTest fixtures declare `[94, 1]` for both limits. Picker exports derive both major and minor format numbers from the running game. Both `pack.mcmeta` files take their formats from `gradle.properties` through `processResources` expansion. The Mixin configs' Java level is expanded the same way: `mixin_java_version` for common's and the Forge-family configs, `java_version` for Fabric's.

Each loader module compiles against common through the `common` configuration. Fabric also places it on its runtime and development classpaths; Forge and NeoForge runs receive common only through their `developmentForge`/`developmentNeoForge` configurations, which Architectury's runtime transformer rewrites for `@ExpectPlatform`, and not through `loom.mods`, because a second, untransformed copy would split its packages across two modules. Fabric, Forge, and NeoForge each call the root `configureGameTests` helper, which creates a `gametest` source set over the shared assertions and structure fixture under `common/src/gametest` and registers `gametestJavadoc`; each loader adds its own wrappers and the `somegoogly_gametest` development mod's metadata, and wires that source set into a `runGameTestServer` run. Fabric discovers its non-static annotated methods through `fabric-gametest` entrypoints; Forge registers 188 test functions and NeoForge registers 190 with `DeferredRegister` and load their corresponding test-instance resources from their own GameTest resource trees. Every loader clears its development GameTest world before a GameTest server run, and Fabric writes a JUnit-style report to `fabric/build/gametest-report.xml`. Loader metadata is expanded from the root `sharedModProperties` map plus each loader's version ranges.

On Windows, `gradlew.bat` is the normal entry point; `gradlew` is the POSIX launcher. The wrapper selects the Gradle distribution, while the launcher selects its host JVM from the machine's Java configuration. The root build declares a Java 21 toolchain from `java_version`, which also sets Fabric's minimum Java; which Gradle applies by convention to compilation, Javadoc, and Gradle-launched Java executions. Compilation and Javadoc read sources as UTF-8. Every module also produces a sources JAR; loader source archives include common's main sources to match their bundled production implementation.

Each subproject has the Java plugin's standard production-source `javadoc` task. Each loader also has a `gametestJavadoc` task over its complete GameTest source set, including the shared package-private assertions under `common/src/gametest/java`. The root `generateDocs` task derives its loader list from `enabled_platforms`, depends on every module's `javadoc` and each loader's `gametestJavadoc`, and synchronizes their HTML output into the committed `docs/javadoc/<module>/` and `docs/javadoc/<loader>-gametest/` trees; separate sections are required because loader modules contain classes with overlapping fully qualified names. `docs/index.html` is the hand-maintained landing page and `docs/.nojekyll` makes GitHub Pages serve the generated files unchanged. Neither file is a Gradle build input, and generated documentation is intentionally excluded from `build-env/`. The build does not produce Javadoc JARs.

The root `collectJars` task copies each enabled loader's already-built production jar (`<archivesName>-<version>.jar` in its `build/libs`, not the dev, dev-shadow, or sources jars) into `../jars`, the directory shared by all version checkouts, for installing into a local launcher. It builds nothing and skips jars that have not been built; it overwrites same-named jars and leaves other versions' jars alone. `gradlews.ps1` runs `clean build`, `runGameTestServer`, `generateDocs`, and `collectJars` in sequence and reports each step's exit code. The root `cleanDev` task empties every module's `build/` and `run/` without removing the directories, and every `clean` empties `build/` rather than removing it, so the Dropbox ignore marks that `../dropbox-ignore.ps1` puts on those directories survive.

## Exact build versions

| Component | Exact version or coordinate | Build role |
| --- | --- | --- |
| Gradle | `9.5.1` (`gradle-9.5.1-bin.zip`) | Wrapper-selected build engine |
| Architectury Loom | `1.17.493` | Minecraft development, mappings, runs, transforms, and remapping |
| Architectury Gradle plugin | `3.5.170` | Common/Fabric/Forge/NeoForge project organization and `@ExpectPlatform` |
| GradleUp Shadow plugin | `9.4.3` | Bundles transformed common output into loader JARs |
| Java toolchain level | `21` | Compilation, Javadoc, and Java execution |
| Minecraft | `1.21.11` | Compile and runtime target |
| Mojang mappings | Official mappings for `1.21.11` | Base mapping layer; no separate mapping version is declared |
| Parchment mappings | `org.parchmentmc.data:parchment-1.21.11:2025.12.20@zip` | Layer over the official mappings |
| Forge | `net.minecraftforge:forge:1.21.11-61.1.5` | Exact Forge compile and development-run baseline |
| NeoForge | `net.neoforged:neoforge:21.11.48` | Exact NeoForge compile and development-run baseline |
| Fabric Loader | `net.fabricmc:fabric-loader:0.18.4` | Fabric loader dependency; also supplies the common annotation dependency |
| Fabric API | `net.fabricmc.fabric-api:fabric-api:0.141.6+1.21.11` | Fabric runtime and development API |
| GeckoLib | `software.bernie.geckolib:geckolib-{common,fabric,forge,neoforge}-1.21.11:5.4.5` | Compile-only optional render integration in common and each loader; built for Minecraft 1.21.11; development runs also load the loader artifacts (`modLocalRuntime`), never published |
| NightConfig | `com.electronwill.night-config:{core,toml}:3.8.3` | Bundled Fabric TOML parser |
| JSpecify annotations | `org.jspecify:jspecify:1.0.0` | Compile-only nullability annotations, declared once for every module |

The Java setting is exact only at the language/toolchain-major level. The repository does not pin a JDK vendor, distribution, or patch release, and it does not pin the host JVM that runs Gradle. Gradle core plugins such as `base` and `java` use Gradle `9.5.1` and therefore have no separate declared version.

## Artifact and runtime version declarations

These values do not select build tools, but they are versioned inputs consumed by resource expansion and are relevant when reproducing the produced artifacts.

Each loader expands its metadata (`fabric.mod.json`, `META-INF/mods.toml`, `META-INF/neoforge.mods.toml`) from the root `sharedModProperties`, which include `mod_homepage` and the source (`<homepage>/tree/<minecraft_version>`) and issue-tracker (`<homepage>/issues`) URLs derived from it.

| Subject | Declaration |
| --- | --- |
| Some Googly Eyes artifact | `0.8.3` |
| Fabric, Forge, and NeoForge GameTest support mods | `0.8.3` |
| Minecraft compatibility | exactly `1.21.11`; Forge and NeoForge syntax `[1.21.11]`, Fabric syntax `=1.21.11` |
| Forge compatibility | `[61.1.5,62)` |
| Forge JavaFML loader compatibility | `[61,62)` |
| NeoForge compatibility | `[21.11.48,21.12)` |
| NeoForge JavaFML loader compatibility | `[1,)` |
| Fabric Loader compatibility | `>=0.18.4` |
| Fabric Java compatibility | `>=21` |
| Fabric API runtime declaration | `>=0.141.6+1.21.11`; compilation uses `0.141.6+1.21.11` |
| GeckoLib | optional on every loader; `5.4.5` or newer when present |

## Resolution and version authorities

`gradle.properties` is the authority for the Minecraft, mapping, loader, API, compatibility, integration, configuration-library, and mod versions. It also holds the Java toolchain level and the JSpecify and external Gradle plugin versions; `settings.gradle` applies the plugin versions through `pluginManagement`. The root `build.gradle` derives the Minecraft, Forge, and NeoForge metadata ranges from the exact versions, and `gradle/wrapper/gradle-wrapper.properties` pins Gradle itself. The loader scripts consume the root properties rather than restating dependency versions.

Plugin resolution uses Fabric Maven, Architectury Maven, Forge Maven, and the Gradle Plugin Portal. Every module declares Maven Central, Parchment Maven, and GeckoLib's Cloudsmith Maven, and the NeoForge module also declares NeoForge Maven; Loom supplies its standard Minecraft repositories. There is no Gradle version catalog, dependency-lock state, dependency-verification metadata, exact JDK distribution, or wrapper-distribution checksum in the repository. Consequently, the table above records every exact version deliberately declared by the build, but it is not a lock of every transitive artifact selected by Gradle and Loom.
