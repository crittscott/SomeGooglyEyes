# Minecraft 26.1.1 port status

The source adaptation targets exact Minecraft 26.1.1 on Fabric, Forge, and NeoForge. The completed local 26.1 implementation supplies the production and GameTest source baseline. No 26.1.1 compilation, GameTest run, client launch, or dedicated-server launch has been performed. The owner runs Gradle, Git, and GitHub operations.

## Dependency baseline

| Component | Pin |
| --- | --- |
| Minecraft | `26.1.1` |
| Java | `25` |
| Gradle | `9.5.1` |
| Architectury Loom, no remap | `1.17.493` |
| Architectury plugin | `3.5.170` |
| Shadow | `9.4.3` |
| Fabric Loader | `0.19.5` |
| Fabric API | `0.145.4+26.1.1` |
| Forge | `26.1.1-63.0.2` |
| NeoForge | `26.1.1.15-beta` |
| GeckoLib compile-only API | `com.geckolib:geckolib-{common,fabric,neoforge}-26.1:5.5` |
| NightConfig | `3.8.3` |
| MixinExtras compile-only API | `0.5.3` |
| JSpecify | `1.0.0` |

Fabric and NeoForge pins follow the neighboring SomeStacks 26.1.1 build. Forge's coordinate is published on its [official downloads page](https://files.minecraftforge.net/net/minecraftforge/forge/index_26.1.1.html). Runtime Minecraft compatibility is exact; Forge and JavaFML ranges are `[63.0.2,64)` and `[63,64)`, and NeoForge's range is `[26.1.1.15-beta,26.1.2)`.

## Source and packaging

Production and GameTest Java sources match the completed 26.1 baseline. This includes Fabric API renames, recipe codecs and assembly, extracted held-eye display context, skull model-state capture, HUD extraction, render pipelines, and GeckoLib package changes. Mojang's [26.1.1 release notes](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-1-1) list a chat-reporting hotfix; no additional gameplay or rendering source changes have been established for this patch.

The Java 25 build uses unobfuscated Minecraft with no mappings, remap tasks, or refmaps. Production Shadow JARs bundle transformed common code; Fabric nests NightConfig and injects the access widener, while Forge and NeoForge convert it to access transformers. Their launch tasks depend on common's development JAR. Shared Mixin configurations retain `JAVA_21` for Forge support, and Fabric's own configuration declares `JAVA_25`. The combined pack covers resource format 84 and data format 101.1; GameTest fixtures use data format 101.1.

All eye-definition files remain untouched, including their version selectors. Eye-state keys, item component IDs, configuration formats, and protocol version 17 retain the 26.1 implementation. No new migration layer is introduced. Loading a real 26.1 save remains an owner verification step.

## GeckoLib limitation

The publisher's [26.1.1 release query](https://api.modrinth.com/v2/project/geckolib/version?game_versions=%5B%2226.1.1%22%5D) returned no releases. Maven metadata for exact-26.1.1 common, Fabric, NeoForge, and Forge coordinates returned 404. Published 26.1/5.5 common, Fabric, and NeoForge POMs and Gradle module metadata were checked; their Java API variants target Java 25 and have no transitive dependencies. Those artifacts remain compile-only and are neither bundled nor added to development runtime classpaths. Forge has no GeckoLib listener or dependency.

This establishes the compile-time artifact baseline, not runtime support for 26.1.1. A compatible GeckoLib installation and compatible entity mod must be established before testing the optional integration. Do not substitute a 26.1.2 artifact or modify another mod's compatibility declarations to force loading.

## Static verification

Static checks passed: 931 JSON/pack-metadata files and four expanded loader TOML manifests parsed; all 15 Mixin class references resolve to source files; production manifests require exact Minecraft 26.1.1; orientation files meet their size limits; and all build-env inputs and orientation outlines match their active files. A file-set and SHA-256 comparison confirms that all 647 eye definitions are unchanged. All 223 production and GameTest Java source files match the completed 26.1 baseline.

These checks do not establish Gradle resolution, Java compilation, Mixin injection success, or runtime behavior. No archives, Minecraft or loader sources, bytecode, Loom caches, or Git state were inspected. Generated Javadoc has not been refreshed.

## Owner verification

Use a Java 25 Gradle host JVM and the checked-in wrapper. Run these from this checkout, addressing failures before proceeding:

1. `clean build` for all three loaders.
2. Fabric, Forge, and NeoForge GameTest servers: 128 Fabric scenarios and 129 each for Forge and NeoForge.
3. Physical client and dedicated-server startup for each loader, including production JAR loading independently of development launches. Check required Mixins and access rules, especially skull submission and special-item argument extraction.
4. Functional checks for crafting, eye application and harvesting, protection cancellation, joins, configuration reload, picker export, and persistence of mobs, items, and placed heads in a 26.1 save.
5. Visual checks for mob eyes, held items in both hands, glow, inventory and world display contexts, placed and item-form heads, picker HUD/gizmo/previews, resource reload, and adult/baby attachments. Eye-placement changes belong to the owner.
6. GeckoLib-absent startup on each loader; GeckoLib-present integration checks on Fabric and NeoForge only after a compatible installation is established.
7. `generateDocs`, then `collectJars`, after the preceding checks pass.
