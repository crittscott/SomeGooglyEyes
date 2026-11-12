# Report: Build-environment applied to 1.21.11

Applied 2026-11-11 by hand-porting 1.21.8's changes. Nothing was compiled, built, or tested; edits were checked by reading only.

## Finding 2 — Forge access-transformer conversion wired into `remapJar` by hand
- forge/build.gradle and build-env/forge/build.gradle (still identical): `loom { forge { } }` sets `convertAccessWideners = true` and `extraAccessWideners.add loom.accessWidenerPath.get().asFile.name`, with 1.21.8's comment, ahead of `mixinConfig`; the `remapJar` `atAccessWideners` line and its comment are removed.
- Deviation from 1.21.8: its MixinExtras `localRuntime` line was not brought in; it is now the only difference from 1.21.8's forge/build.gradle.
- NeoForge unchanged by design.
- orientation-build.md:35, platform-notes.md:93, and build-env/platform-notes.md:93 carry 1.21.8's text (their CRLF line endings kept). build-env/orientation-build.md does not mention the widener; unchanged.

## Findings 3 and 5
None (3 rejected by code-review-decisions.md:20; 5 already fixed).

## Recalled API facts
Architectury Loom's `convertAccessWideners`/`extraAccessWideners` populate `remapJar`'s `atAccessWideners`; 1.21.8 builds with this setup.

## Checks needed
- After building: the production Forge jar still contains `META-INF/accesstransformer.cfg` with the same rules; launch production Forge in MultiMC and look for access errors.

## Clash points
orientation-build.md:35 and platform-notes.md:93 (plus the build-env copy). Any later edit to forge/build.gradle must keep build-env/forge/build.gradle identical.
