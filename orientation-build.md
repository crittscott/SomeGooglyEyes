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

This checkout targets Minecraft 26.3. The Groovy-DSL Gradle build has `common`, `fabric`, `forge`, and `neoforge` subprojects; `settings.gradle` includes the loaders named by `enabled_platforms`. The root applies Architectury Loom’s `dev.architectury.loom-no-remap` plugin and the Architectury plugin to each subproject. Minecraft is unobfuscated, so there are no mapping dependencies or remapping tasks. Each loader bundles Architectury-transformed common output into its production Shadow JAR; the loader-only JAR has the `dev` classifier. `assemble` includes the production JAR, and loader sources JARs include common’s source files. Architectury is build-time only, supplying `@ExpectPlatform` transformation without a runtime Architectury API dependency. No Maven publication is declared.

`common` uses Fabric Loader and MixinExtras as compile-only annotation/injection dependencies. GeckoLib is compile-only in common and every loader, using `com.geckolib:geckolib-<module>-26.3:5.5.7` from its Cloudsmith Maven. It is optional at runtime, loaded in development runs, and not bundled.

Fabric bundles NightConfig core and TOML as nested dependencies through Loom's `include` configuration; the parser is an implementation dependency during development. Every module declares Maven Central, which supplies these modules and the shared annotation dependency.

`common` declares `common/src/main/resources/somegoogly.accesswidener` in the `official` namespace through `loom.accessWidenerPath`, and every loader references it. Fabric injects the widener into its production Shadow JAR with `loom.injectAccessWidener`. Forge and NeoForge convert it on their Shadow tasks through `loom.getForge().convertAccessWideners` and `loom.getNeoForge().convertAccessWideners`. Development uses the declared widener directly.

The shared Mixin configurations declare `JAVA_21` from `mixin_java_version`, which Forge’s Mixin recognizes; Fabric’s own config declares `JAVA_25` from `java_version`. The build toolchain and game runtime remain Java 25. All configs use unobfuscated Mojang names without refmaps or a legacy Mixin annotation processor. Forge registers the common config through `loom.forge.mixinConfig`; NeoForge registers it through its metadata; Fabric lists the common config and `somegoogly-fabric.mixins.json` in `fabric.mod.json`. MixinExtras supports context capture during item-model extraction and skull submission.

Forge and NeoForge copy common’s `pack.mcmeta` into their own processed resources so loader-owned data is recognized in development, retaining one copy in the production Shadow JAR. The combined pack declares `min_format` `[97, 1]` and `max_format` `[121, 0]`, encompassing client resource format 97.1 and data format 121.0 without legacy pack metadata. Shared GameTest fixtures declare `[121, 0]` for both limits. Picker exports derive both data-format numbers from the running game. Both `pack.mcmeta` files take their formats from `gradle.properties` through `processResources` expansion.

Each loader module compiles against common through the `common` configuration. Fabric also places it on its runtime and development classpaths; Forge and NeoForge runs receive common only through their `developmentForge`/`developmentNeoForge` configurations, which Architectury's runtime transformer rewrites for `@ExpectPlatform`, and not through `loom.mods`, because a second, untransformed copy would split its packages across two modules. Forge and NeoForge’s `runClient`, `runServer`, and `runGameTestServer` tasks depend on common’s `jar`, which also processes common resources before the runtime transformer loads that development JAR. Fabric, Forge, and NeoForge each call the root `configureGameTests` helper, which creates a `gametest` source set over the shared assertions and structure fixture under `common/src/gametest` and registers `gametestJavadoc`; each loader adds its own wrappers and the `somegoogly_gametest` development mod's metadata, and wires that source set into a `runGameTestServer` run. Fabric discovers its non-static annotated methods through `fabric-gametest` entrypoints; Forge and NeoForge register 187 test functions apiece with `DeferredRegister` and load their corresponding test-instance resources from their own GameTest resource trees. Every loader clears its development GameTest world before a GameTest server run, and Fabric writes a JUnit-style report to `fabric/build/gametest-report.xml`. Loader metadata is expanded from the root `sharedModProperties` map plus each loader's version ranges.

On Windows, `gradlew.bat` is the normal entry point; `gradlew` is the POSIX launcher. The wrapper selects the Gradle distribution, while the launcher selects its host JVM from the machine's Java configuration. The root build declares a Java 25 toolchain from `java_version`, which also sets Fabric's minimum Java; which Gradle applies by convention to compilation, Javadoc, and Gradle-launched Java executions. Compilation and Javadoc read sources as UTF-8. Every module also produces a sources JAR; loader source archives include common's main sources to match their bundled production implementation. The IDE and Gradle host JVM must also support Java 25.

Each subproject has the Java plugin's standard production-source `javadoc` task. Each loader also has a `gametestJavadoc` task over its complete GameTest source set, including the shared package-private assertions under `common/src/gametest/java`. The root `generateDocs` task derives its loader list from `enabled_platforms`, depends on every module's `javadoc` and each loader's `gametestJavadoc`, and synchronizes their HTML output into the committed `docs/javadoc/<module>/` and `docs/javadoc/<loader>-gametest/` trees; separate sections are required because loader modules contain classes with overlapping fully qualified names. `docs/index.html` is the hand-maintained landing page and `docs/.nojekyll` makes GitHub Pages serve the generated files unchanged. Neither file is a Gradle build input, and generated documentation is intentionally excluded from `build-env/`. The build does not produce Javadoc JARs.

The root `collectJars` task copies each enabled loader's already-built production jar (`<archivesName>-<version>.jar` in its `build/libs`, not the dev, dev-shadow, or sources jars) into `../jars`, the directory shared by all version checkouts, for installing into a local launcher. It builds nothing and skips jars that have not been built; it overwrites same-named jars and leaves other versions' jars alone. `gradlews.ps1` runs `clean build`, `runGameTestServer`, `generateDocs`, and `collectJars` in sequence and reports each step's exit code.

## Exact build versions

| Component | Exact version or coordinate | Build role |
| --- | --- | --- |
| Gradle | `9.5.1` (`gradle-9.5.1-bin.zip`) | Wrapper-selected build engine |
| Architectury Loom | `1.17.493`, `dev.architectury.loom-no-remap` | Unobfuscated Minecraft development, runs, and transforms |
| Architectury Gradle plugin | `3.5.170` | Common/Fabric/Forge/NeoForge project organization and `@ExpectPlatform` |
| GradleUp Shadow plugin | `9.4.3` | Bundles transformed common output into loader JARs |
| Java toolchain level | `25` | Compilation, Javadoc, and Java execution |
| Minecraft | `26.3` | Compile and runtime target |
| Forge | `net.minecraftforge:forge:26.3-66.1.0` | Exact Forge compile and development-run baseline |
| NeoForge | `net.neoforged:neoforge:26.3.0.76` | Exact NeoForge compile and development-run baseline |
| Fabric Loader | `net.fabricmc:fabric-loader:0.19.5` | Fabric loader dependency; also supplies the common annotation dependency |
| Fabric API | `net.fabricmc.fabric-api:fabric-api:0.163.0+26.3` | Fabric runtime and development API |
| GeckoLib | `com.geckolib:geckolib-{common,fabric,forge,neoforge}-26.3:5.5.7` | Compile-only optional render integration in common and every loader; development runs also load the loader artifacts (`runtimeOnly`), never published |
| NightConfig | `com.electronwill.night-config:{core,toml}:3.8.3` | Bundled Fabric TOML parser |
| MixinExtras | `io.github.llamalad7:mixinextras-common:0.5.3` | Compile-only context and operation injection API; loaders provide runtime support |
| JSpecify annotations | `org.jspecify:jspecify:1.0.0` | Compile-only nullability annotations, declared once for every module |

The Java setting is exact only at the language/toolchain-major level. The repository does not pin a JDK vendor, distribution, or patch release, and it does not pin the host JVM that runs Gradle. Gradle core plugins such as `base` and `java` use Gradle `9.5.1` and therefore have no separate declared version.

## Artifact and runtime version declarations

These values do not select build tools, but they are versioned inputs consumed by resource expansion and are relevant when reproducing the produced artifacts.

Each loader expands its metadata (`fabric.mod.json`, `META-INF/mods.toml`, `META-INF/neoforge.mods.toml`) from the root `sharedModProperties`, which include `mod_homepage` and the source (`<homepage>/tree/<minecraft_version>`) and issue-tracker (`<homepage>/issues`) URLs derived from it.

| Subject | Declaration |
| --- | --- |
| Some Googly Eyes artifact | `0.8.3` |
| Fabric, Forge, and NeoForge GameTest support mods | `0.8.3` |
| Minecraft compatibility | exactly `26.3`; Forge and NeoForge syntax `[26.3]`, Fabric syntax `=26.3` |
| Forge compatibility | `[66.1.0,67)` |
| Forge JavaFML loader compatibility | `[66,67)` |
| NeoForge compatibility | `[26.3.0.76,26.3.1)` |
| NeoForge JavaFML loader compatibility | `[1,)` |
| Fabric Loader compatibility | `>=0.19.5` |
| Fabric Java compatibility | `>=25` |
| Fabric API runtime declaration | `>=0.163.0+26.3`; compilation uses `0.163.0+26.3` |
| GeckoLib | metadata permits optional `5.5.7` or newer on every loader |

## Resolution and version authorities

`gradle.properties` is the authority for the Minecraft, mapping, loader, API, compatibility, integration, configuration-library, and mod versions. It also holds the Java toolchain level and the JSpecify and external Gradle plugin versions; `settings.gradle` applies the plugin versions through `pluginManagement`. The root `build.gradle` derives the Minecraft, Forge, and NeoForge metadata ranges from the exact versions, and `gradle/wrapper/gradle-wrapper.properties` pins Gradle itself. The loader scripts consume the root properties rather than restating dependency versions.

Plugin resolution uses Fabric Maven, Architectury Maven, Forge Maven, and the Gradle Plugin Portal. Every module declares Maven Central and GeckoLib’s Cloudsmith Maven, and the NeoForge module also declares NeoForge Maven; Loom supplies its standard Minecraft repositories. There is no Gradle version catalog, dependency-lock state, dependency-verification metadata, exact JDK distribution, or wrapper-distribution checksum in the repository. Consequently, the table above records every exact version deliberately declared by the build, but it is not a lock of every transitive artifact selected by Gradle and Loom.
