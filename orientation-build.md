<!-- FIXED HEADER: not content. Do not edit, trim, or count toward the size limit. -->
> **Orientation snapshot, not a specification.** Describes the code as it currently is; the code wins on any disagreement. See CLAUDE.md § Orientation files.
<!-- END FIXED HEADER -->

# Some Googly Eyes — build orientation

This document is an orientation to the repository's build environment: its entry points, module layout, version authorities, dependency baselines, and packaging flow. It describes the setup as it exists. It is not build history, a troubleshooting log, release documentation, or a conversation.

`build-env/` is a reference snapshot of the repository's build environment: every checked-in Gradle build input including the wrapper, plus the agent instructions, the build-runner script, the Git settings, and outlines of the three orientation files. It preserves repository-relative paths so that files can be compared or restored without guessing where they belong. The active files at the repository root and under `common/`, `fabric/`, `forge/`, `neoforge/`, and `gradle/` remain authoritative; Gradle does not read the copies. Keep the snapshot and this description synchronized whenever an active file it covers changes.

The snapshot holds verbatim copies of the manually maintained files that define or launch this Gradle build (Gradle scripts, Gradle properties, wrapper launchers, and wrapper files), the agent instructions (`CLAUDE.md`, `AGENTS.md`, `code-review.md`, `code-conventions.md`), `gradlews.ps1`, which runs the standard Gradle task sequence and reports each step's result, and the Git settings (`.gitignore`, `.gitattributes`). The orientation entries are outlines, not copies: each holds its file's fixed header followed by every heading in order, with no body text, so it records the files' format without duplicating content that changes with the code. It should not contain caches, generated output, IDE state, run directories, resolved dependency JARs, mod source or resources, GameTest fixtures, loader manifests, or release notes. The wrapper JAR is the sole binary because it is itself a checked-in build launcher.

## Snapshot contents

Each copy sits at `build-env/` followed by its active path.

| Kind | Active paths |
| --- | --- |
| Gradle build | `settings.gradle`, `build.gradle`, `gradle.properties`, `common/build.gradle`, `fabric/build.gradle`, `fabric/gradle.properties`, `forge/build.gradle`, `forge/gradle.properties`, `neoforge/build.gradle`, `neoforge/gradle.properties` |
| Gradle wrapper | `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar` |
| Agent instructions | `CLAUDE.md`, `AGENTS.md`, `code-review.md`, `code-conventions.md` |
| Build runner | `gradlews.ps1` |
| Git settings | `.gitignore`, `.gitattributes` |
| Orientation outlines | `orientation-player.md`, `orientation-code.md`, `orientation-build.md` |

## Build shape

The project is a Groovy-DSL Gradle build with `common`, `fabric`, `forge`, and `neoforge` subprojects; `settings.gradle` includes the loaders named by `enabled_platforms`. The root build applies Architectury Loom and the Architectury plugin to each subproject and establishes shared Minecraft mappings and Java settings. The build declares no Maven publication; the modules expose no stable public API and nothing consumes them through Maven. `common` is transformed for Fabric, Forge, and NeoForge; each loader module bundles its transformed common output with Shadow and then remaps the resulting production JAR. Architectury is build-time only: the plugin supplies `@ExpectPlatform` and its transformation, and no loader requires Architectury API at runtime.

`common` compiles the GeckoLib render integration against the compile-only `geckolib-common` artifact from GeckoLib's Cloudsmith Maven; the loader modules have no GeckoLib dependency of their own. GeckoLib is optional at runtime and is not bundled. Architectury's injected annotations supply `@ExpectPlatform`.

`common` declares the access widener `common/src/main/resources/somegoogly.accesswidener` through `loom.accessWidenerPath`; every loader points its own `loom.accessWidenerPath` at common's. Forge and NeoForge, which read access transformers rather than wideners, also name that widener in `atAccessWideners` so their production JARs carry the converted rules.

Only Fabric has Mixins. Fabric Loom uses the legacy Mixin annotation processor and writes the fixed `somegoogly.refmap.json` refmap named by `somegoogly.mixins.json`; Fabric's `loom.mods` entry also lists common's source set. `common` has no Mixins, so no loader configures a common refmap.

Each loader module compiles against common through the `common` configuration. Fabric also places it on its runtime and development classpaths; Forge and NeoForge runs receive common only through their `developmentForge`/`developmentNeoForge` configurations, which Architectury's runtime transformer rewrites for `@ExpectPlatform`, and not through `loom.mods`, because a second, untransformed copy would split its packages across two modules. Fabric, Forge, and NeoForge each call the root `configureGameTests` helper, which creates a `gametest` source set over the shared assertions and structure fixture under `common/src/gametest` and registers `gametestJavadoc`; each loader adds its own wrappers and the `somegoogly_gametest` development mod's metadata, and wires that source set into a `runGameTestServer` run. Fabric clears only its development GameTest world before a GameTest server run and writes a JUnit-style report to `fabric/build/gametest-report.xml`. Loader metadata is expanded from the root `sharedModProperties` map plus each loader's version ranges.

On Windows, `gradlew.bat` is the normal entry point; `gradlew` is the POSIX launcher. The wrapper selects the Gradle distribution, while the launcher selects its host JVM from the machine's Java configuration. The root build declares a Java 21 toolchain, which Gradle applies by convention to compilation, Javadoc, and Gradle-launched Java executions. Every module also produces a sources JAR.

Each subproject has the Java plugin's standard production-source `javadoc` task. Each loader also has a `gametestJavadoc` task over its complete GameTest source set, including the shared package-private assertions under `common/src/gametest/java`. The root `generateDocs` task derives its loader list from `enabled_platforms`, depends on every module's `javadoc` and each loader's `gametestJavadoc`, and synchronizes their HTML output into the committed `docs/javadoc/<module>/` and `docs/javadoc/<loader>-gametest/` trees; separate sections are required because loader modules contain classes with overlapping fully qualified names. `docs/index.html` is the hand-maintained landing page and `docs/.nojekyll` makes GitHub Pages serve the generated files unchanged. Neither file is a Gradle build input, and generated documentation is intentionally excluded from `build-env/`. The build does not produce Javadoc JARs.

The root `collectJars` task copies each enabled loader's already-built production jar (`<archivesName>-<version>.jar` in its `build/libs`, not the dev, dev-shadow, or sources jars) into `../jars`, the directory shared by all version checkouts, for installing into a local launcher. It builds nothing and skips jars that have not been built; it overwrites same-named jars and leaves other versions' jars alone. `gradlews.ps1` runs `clean build`, `runGameTestServer`, `generateDocs`, and `collectJars` in sequence and reports each step's exit code.

## Exact build versions

| Component | Exact version or coordinate | Build role |
| --- | --- | --- |
| Gradle | `9.5.1` (`gradle-9.5.1-bin.zip`) | Wrapper-selected build engine |
| Architectury Loom | `1.17.493` | Minecraft development, mappings, runs, transforms, and remapping |
| Architectury Gradle plugin | `3.5.170` | Common/Fabric/Forge/NeoForge project organization and `@ExpectPlatform` |
| GradleUp Shadow plugin | `9.4.3` | Bundles transformed common output into loader JARs |
| Java toolchain level | `21` | Compilation, Javadoc, and Java execution |
| Minecraft | `1.21.1` | Compile and runtime target |
| Mojang mappings | Official mappings for `1.21.1` | Base mapping layer; no separate mapping version is declared |
| Parchment mappings | `org.parchmentmc.data:parchment-1.21.1:2024.11.17@zip` | Layer over the official mappings |
| Forge | `net.minecraftforge:forge:1.21.1-52.1.2` | Exact Forge compile and development-run baseline |
| NeoForge | `net.neoforged:neoforge:21.1.34` | Exact NeoForge compile and development-run baseline |
| Fabric Loader | `net.fabricmc:fabric-loader:0.16.14` | Fabric loader dependency |
| Fabric API | `net.fabricmc.fabric-api:fabric-api:0.102.1+1.21.1` | Fabric runtime and development API |
| GeckoLib | `software.bernie.geckolib:geckolib-common-1.21.1:4.7.4` | Compile-only optional render integration in common |
| JSR 305 annotations | `com.google.code.findbugs:jsr305:3.0.2` | Compile-only nullability annotations, declared once for every module because Loom does not expose Minecraft's copy to common |

The Java setting is exact only at the language/toolchain-major level. The repository does not pin a JDK vendor, distribution, or patch release, and it does not pin the host JVM that runs Gradle. Gradle core plugins such as `base` and `java` use Gradle `9.5.1` and therefore have no separate declared version.

## Artifact and runtime version declarations

These values do not select build tools, but they are versioned inputs consumed by resource expansion and are relevant when reproducing the produced artifacts.

Each loader expands its metadata (`fabric.mod.json`, `META-INF/mods.toml`, `META-INF/neoforge.mods.toml`) from the root `sharedModProperties`, which include `mod_homepage` and the source (`<homepage>/tree/<minecraft_version>`) and issue-tracker (`<homepage>/issues`) URLs derived from it.

| Subject | Declaration |
| --- | --- |
| Some Googly Eyes artifact | `0.8.2` |
| Fabric, Forge, and NeoForge GameTest support mods | `0.8.2` |
| Minecraft compatibility | exactly `1.21.1`; Forge and NeoForge syntax `[1.21.1]`, Fabric syntax `=1.21.1` |
| Forge compatibility | `[52.1.2,53)` |
| Forge JavaFML loader compatibility | `[52,53)` |
| NeoForge compatibility | `[21.1.34,21.2)` |
| NeoForge JavaFML loader compatibility | `[1,)` |
| Fabric Loader compatibility | `>=0.16.14` |
| Fabric Java compatibility | `>=21` |
| Fabric API runtime declaration | `>=0.102.1+1.21.1`; compilation uses `0.102.1+1.21.1` |
| GeckoLib | optional on every loader; `4.7.4` or newer when present |

## Resolution and version authorities

`gradle.properties` is the authority for the Minecraft, mapping, loader, API, compatibility, integration, and mod versions. The root `build.gradle` pins the three external Gradle plugins and JSR 305, and `gradle/wrapper/gradle-wrapper.properties` pins Gradle itself. The loader scripts consume the root properties rather than restating dependency versions.

Plugin resolution uses Fabric Maven, Architectury Maven, Forge Maven, and the Gradle Plugin Portal. Every module declares Parchment Maven and GeckoLib's Cloudsmith Maven, and the NeoForge module also declares NeoForge Maven; Loom supplies its standard Minecraft repositories. There is no Gradle version catalog, dependency-lock state, dependency-verification metadata, exact JDK distribution, or wrapper-distribution checksum in the repository. Consequently, the table above records every exact version deliberately declared by the build, but it is not a lock of every transitive artifact selected by Gradle and Loom.
