<!-- FIXED HEADER: not content. Do not edit, trim, or count toward the size limit. -->
> **Orientation snapshot, not a specification.** Describes the code as it currently is; the code wins on any disagreement. See CLAUDE.md § Orientation files.
<!-- END FIXED HEADER -->

# Some Googly Eyes — build orientation

Active build files are authoritative.

`build-env/` mirrors the inputs below at their active relative paths; Gradle reads the active files. Keep it synchronized. Orientation copies contain only fixed headers and headings. Exclude caches, generated output, IDE/run state, dependencies, mod sources/resources, tests, manifests, and release notes; the wrapper JAR is the sole binary.

## Snapshot contents

Copies preserve active paths under `build-env/`.

| Kind | Active paths |
| --- | --- |
| Gradle build | `settings.gradle`, `build.gradle`, `gradle.properties`, `gradle/code-analysis.gradle`, `common/build.gradle`, `fabric/build.gradle`, `fabric/gradle.properties`, `forge/build.gradle`, `forge/gradle.properties` |
| Analysis rules | `config/pmd/trial.xml` |
| Gradle wrapper | `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar` |
| Shared agent instructions | `CLAUDE.md`, `AGENTS.md`, `code-review.md`, `code-conventions.md`, `code-review-decisions.md` |
| Platform facts | `platform-notes.md` from this checkout |
| Build runner | `gradlews.ps1` |
| Git settings | `.gitignore`, `.gitattributes` |
| Orientation outlines | `orientation-player.md`, `orientation-code.md`, `orientation-build.md` |

## Build shape

The Groovy-DSL build has `common` plus the loaders in `enabled_platforms` (`fabric`, `forge`). Root applies Architectury Loom/Architectury, mappings, and Java settings to subprojects. Loaders Shadow-bundle transformed common output, then remap production JARs. There is no Maven publication or stable public API. Architectury supplies `@ExpectPlatform` and its transformation at build time; Architectury API is not required at runtime.

`common` uses Fabric Loader for `@Environment` and Mixin annotations only. GeckoLib has no common artifact here: common compiles against `geckolib-fabric` from Cloudsmith; loaders use their own artifact. All use non-transitive `modCompileOnly`. GeckoLib is optional, loaded in development, and never bundled.

All modules use common's `src/main/resources/somegoogly.accesswidener` through `loom.accessWidenerPath`. Forge's `loom.forge` enables `convertAccessWideners` and lists it in `extraAccessWideners`; remapping writes SRG-named access-transformer rules into the production JAR.

All modules use the legacy Mixin processor. Loaders write `somegoogly.refmap.json` (Fabric intermediary, Forge SRG); common writes intermediary `somegoogly-common.refmap.json`. Common's resource config, `somegoogly-common.mixins.json`, omits the refmap for Mojang-named development. Architectury adds it in production; `FixForgeMixin` rewrites it to Forge SRG. Fabric lists common's config beside `somegoogly-fabric.mixins.json` in `fabric.mod.json` and common's sources in `loom.mods`; Forge registers it beside `somegoogly-forge.mixins.json` through `loom.forge.mixinConfig`.

Each loader module compiles against common through the `common` configuration. Fabric also places it on its runtime and development classpaths; Forge runs receive common only through its `developmentForge` configuration, which Architectury's runtime transformer rewrites for `@ExpectPlatform`, and not through `loom.mods`, because a second, untransformed copy would split its packages across two modules. Fabric and Forge each call the root `configureGameTests` helper, which creates a `gametest` source set over the shared assertions under `common/src/gametest` and registers `gametestJavadoc`; each loader adds its own wrappers and the `somegoogly_gametest` development mod's metadata, and wires that source set into a `runGameTestServer` run. Every loader clears its development GameTest world before a GameTest server run, and Fabric writes a JUnit-style report to `fabric/build/gametest-report.xml`. Loader metadata is expanded from the root `sharedModProperties` map plus each loader's version ranges.

On Windows, use `gradlew.bat`; `gradlew` is the POSIX launcher. The wrapper selects Gradle; the machine's Java configuration selects its host JVM. That host, including IntelliJ's Gradle JVM, must be Java 21 or newer for google-java-format. The Java 17 toolchain from `java_version` controls ordinary compilation, Javadoc, and game execution, and sets Fabric's minimum Java. Error Prone overrides only its compilation tasks. Sources and Javadoc use UTF-8. Every module produces a sources JAR; each loader includes common's main sources before remapping.

Each subproject has a production `javadoc` task. Each loader's `gametestJavadoc` includes the shared package-private GameTest assertions. Root `generateDocs` derives loaders from `enabled_platforms` and synchronizes these tasks' HTML into committed `docs/javadoc/<module>/` and `docs/javadoc/<loader>-gametest/` trees, separating overlapping class names. `docs/index.html` is the hand-maintained landing page; `docs/.nojekyll` makes GitHub Pages serve generated files unchanged. Generated docs are not build inputs, are excluded from `build-env/`, and have no Javadoc JARs.

`collectJars` copies enabled loaders' existing `build/libs/<archivesName>-<version>.jar` production files to shared `../jars` for local launchers. It builds nothing, skips missing files, overwrites matching names, and keeps other versions. `gradlews.ps1` runs `clean build`, `runGameTestServer`, `generateDocs`, and `collectJars`, reporting exit codes. `cleanDev` empties each module's `build/` and `run/`; `clean` empties `build/`. Both preserve directories and their `../dropbox-ignore.ps1` marks.

Forge owns its native loot declarations. Native `processResources` copies common's version-specific `pack.mcmeta` into the development resource root; `shadowJar` excludes duplicates only for that metadata. Fabric uses `implementation` plus Loom `include` for NightConfig core and TOML, so development and production both have the parser. Both `pack.mcmeta` files take their formats from `gradle.properties` through `processResources` expansion. The Mixin configs' Java level is expanded the same way: `mixin_java_version` for common's and the Forge-family configs, `java_version` for Fabric's.

The root project applies Spotless to the Java sources under `common/src`, `fabric/src`, and `forge/src`, every source set included. It formats code and javadoc with google-java-format in AOSP style (4-space indent), reorders imports, trims trailing whitespace, ends files with a newline, and honors `// spotless:off` / `// spotless:on`, which keep the `/sg` Brigadier trees in their hand layout. `spotlessApply` rewrites the files; `spotlessCheck` reports unformatted files but is not attached to `check`, so formatting never fails a build.

Analysis covers all source sets. `-Pquality=compiler` enables javac lint on Java 17; `-Pquality=errorprone` adds Error Prone defaults; `-Pquality=nullaway` also adds NullAway. Checker modes use JDK 21 with `--release 17`; ordinary builds and game runs use Java 17. Checks are advisory, with no `-Werror` or rewriting. Local exceptions follow `code-conventions.md`; unknown modes fail configuration.

Capture with `.\gradlew.bat compileJava compileGametestJava -Pquality=nullaway --rerun-tasks --console=plain *> nullaway.log`. Checker modes add the Error Prone plugin, core processor and compile-only contract annotations. Only NullAway mode adds its processor. Mixin processing and refmaps remain enabled; `test`/`gametest` are marked as test code. Mode changes invalidate compilation. `errorprone_max_warnings` caps diagnostics. Checker dependencies are not bundled.

NullAway checks `com.github.crittscott.somegoogly` with non-null defaults and existing `javax.annotation.Nullable`. It honors dependency nullability and verifies JetBrains predicate `@Contract` annotations with `CheckContracts`; unannotated APIs remain optimistic. JSpecify mode is off.

Root `analysisReview` runs standalone advisory `pmdReview`/`cpdReview` from `gradle/code-analysis.gradle`, outside `check`/`build`. PMD uses Gradle's `Pmd` task, `config/pmd/trial.xml`, each source set's compile classpath/output, and Java 17; dependencies may compile, but no game launches. Shared GameTests retain each loader context. HTML/XML and count/timing/source summaries go to `build/reports/pmd/<module>/<sourceSet>.*`; empty sets skip. Seven rules use default metric thresholds. Findings are advisory; XML processing/configuration errors fail. Type-resolution diagnostics remain in the log.

CPD scans physical module `src/main/java` files once, separately from `src/test/java`/`src/gametest/java`. Exact and approximate passes use `cpd_minimum_tokens`; approximate ignores identifiers/literals. Java 17 runs CPD without compilation. XML/text reports with counts, timing, options, and sources go to `build/reports/cpd/<production|tests>-<exact|approximate>.*`. Findings do not fail; tool errors do. Explicit reviews rerun. Capture `.\gradlew.bat analysisReview --continue --console=plain *> analysis-review.log`; assess completed tasks only and retain diagnostics. Tool dependencies stay in analysis-only configurations.

Agent review applies conventions/accepted decisions and compares compiler/NullAway reviews. Retain each finding and reason: change justified, retain deliberately, false positive, already reported, or needs more evidence. Group findings with loader context. Scores alone do not justify splitting classes; wiring may repeat. Record proposed changes, rule/CPD recommendations, runtimes, and limitations before edits.

## Exact build versions

| Component | Exact version or coordinate | Build role |
| --- | --- | --- |
| Gradle | `9.5.1` (`gradle-9.5.1-bin.zip`) | Wrapper-selected build engine |
| Architectury Loom | `1.17.493` | Minecraft development, mappings, runs, transforms, and remapping |
| Architectury Gradle plugin | `3.5.170` | Common/Fabric/Forge project organization and `@ExpectPlatform` |
| GradleUp Shadow plugin | `9.4.3` | Bundles transformed common output into loader JARs |
| Spotless Gradle plugin | `8.12.0` | Runs the source formatter |
| Error Prone Gradle plugin | `5.2.0` | Optional compiler integration |
| Error Prone core and annotations | `2.53.0` | Default analysis checks and compile-only library contracts |
| NullAway | `0.14.4` | Optional nullability checker on the processor path |
| PMD and CPD | `7.24.0` | Advisory analysis tools; CPD starts at `100` tokens |
| Error Prone compiler level | `21`, `--release 17` | Analysis compilation only; Minecraft runs on Java 17 |
| google-java-format | `2.0.0`, AOSP style | Java source and javadoc formatting; runs in the Gradle host JVM |
| Java toolchain level | `17` | Ordinary compilation, Javadoc, and Java execution |
| Minecraft | `1.20.1` | Compile and runtime target |
| Mojang mappings | Official mappings for `1.20.1` | Base mapping layer; no separate mapping version is declared |
| Parchment mappings | `org.parchmentmc.data:parchment-1.20.1:2023.09.03@zip` | Layer over the official mappings |
| Forge | `net.minecraftforge:forge:1.20.1-47.4.0` | Exact Forge compile and development-run baseline |
| Fabric Loader | `net.fabricmc:fabric-loader:0.16.14` | Fabric loader dependency |
| Fabric API | `net.fabricmc.fabric-api:fabric-api:0.92.11+1.20.1` | Fabric runtime and development API |
| GeckoLib | `software.bernie.geckolib:geckolib-<loader>-1.20.1:4.7.4` | Compile-only optional render integration: Fabric's artifact in common, each loader's own in that loader; development runs also load it (`modLocalRuntime`), never published |
| Heads | `maven.modrinth:heads:X7aPmbm0` (Heads 1.1.3 for Forge 1.20.1) | Forge development runs and GameTests only (`modLocalRuntime`); never compiled against or published |
| Twilight Forest | `curse.maven:the-twilight-forest-227639:5468648` (Twilight Forest 4.3.2508 for Forge 1.20.1) | Forge development runs and GameTests only (`modLocalRuntime`); never compiled against or published |
| NightConfig | `com.electronwill.night-config:{core,toml}:3.8.3` (`nightconfig_version`) | Fabric TOML parser, nested in its production JAR |
| JSR 305 annotations | `com.google.code.findbugs:jsr305:3.0.2` | Compile-only nullability annotations, declared once for every module because Loom does not expose Minecraft's copy to common |

The Java setting is exact only at the language/toolchain-major level. The repository does not pin a JDK vendor, distribution, or patch release, and it does not pin the host JVM that runs Gradle. Gradle core plugins such as `base` and `java` use Gradle `9.5.1` and therefore have no separate declared version.

## Artifact and runtime version declarations

These values do not select build tools, but they are versioned inputs consumed by resource expansion and are relevant when reproducing the produced artifacts.

Each loader expands its metadata (`fabric.mod.json`, `META-INF/mods.toml`) from the root `sharedModProperties`, which include `mod_homepage` and the source (`<homepage>/tree/<minecraft_version>`) and issue-tracker (`<homepage>/issues`) URLs derived from it.

| Subject | Declaration |
| --- | --- |
| Some Googly Eyes artifact | `0.8.3` |
| Fabric and Forge GameTest support mods | `0.8.3` |
| Minecraft compatibility | exactly `1.20.1`; Forge syntax `[1.20.1]`, Fabric syntax `=1.20.1` |
| Forge compatibility | `[47.4.0,48)` |
| Forge JavaFML loader compatibility | `[47,48)` |
| Fabric Loader compatibility | `>=0.16.14` |
| Fabric Java compatibility | `>=17` |
| Fabric API runtime declaration | `>=0.92.11+1.20.1`; compilation uses `0.92.11+1.20.1` |
| GeckoLib | optional on every loader; `4.7.4` or newer when present |
| Heads | optional on Forge, any version (`mods.toml`); never required |
| Twilight Forest | optional on Forge, any version (`mods.toml`); never required |

## Resolution and version authorities

`gradle.properties` owns target, dependency, tool, and mod versions, Java levels, the analysis warning cap, and the CPD token threshold. `settings.gradle` applies plugin versions through `pluginManagement`; root derives Minecraft/Forge metadata ranges. `gradle/wrapper/gradle-wrapper.properties` pins Gradle. Loader scripts consume root properties.

Plugin resolution uses Fabric Maven, Architectury Maven, Forge Maven, and the Gradle Plugin Portal. Every module declares Maven Central, Parchment Maven, and GeckoLib's Cloudsmith Maven; Forge also declares Modrinth Maven, exclusively for the `maven.modrinth` group, and CurseMaven, exclusively for the `curse.maven` group; Loom supplies its standard Minecraft repositories. There is no Gradle version catalog, dependency-lock state, dependency-verification metadata, exact JDK distribution, or wrapper-distribution checksum in the repository. Consequently, the table above records every exact version deliberately declared by the build, but it is not a lock of every transitive artifact selected by Gradle and Loom.
