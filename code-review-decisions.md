# Code review decisions

Settled review policy and accepted behavior for this project. Read this before reviewing or implementing a finding. These decisions override general preferences in the review prompt, CLAUDE.md, and code-conventions.md where they conflict. A new, demonstrated defect may justify revisiting a decision; preference, speculation, and a different architecture do not.

## Review scope

- Preserve review findings, their original numbering, and follow-up items for cross-version handoff. Add a verdict to each assessed finding; when fixed, describe the implemented fix, relevant invariants, and verification status. Retain rejected findings with their reasons and qualify superseded claims explicitly rather than deleting or silently rewriting them. The verdict distinguishes the original review from the current code. This retention policy follows the owner's instruction and supersedes the earlier removal policy.
- Trust our code, Minecraft, loaders, and other mods unless a relevant behavior is known to be unreliable or unguaranteed. Filesystem operations and reflective integration boundaries can need failure handling. A client or server across the network remains untrusted.
- Keep effort proportional to a Minecraft mod. A theoretical nested rendering call or a variant roll changed to an exact endpoint through /data is not an actionable defect without a realistic consequence.
- A working deliberate choice is not a finding merely because another implementation is more conventional. General principles do not override the specific decisions below.

## Development and build

- Manual play-testing uses MultiMC, not the development runServer environment. The GameTest development world is disposable and may be deleted by the test helper.
- Keep build environments as close across Minecraft versions as reasonable. Inactive cross-version Gradle branches, plugin declarations, pack-format fields, and separate Mixin Java settings are acceptable consequences.
- Keep `pack.mcmeta` in common. Each Forge-family module copies it into its own resources and de-duplicates it in `shadowJar`, so no loader module carries a hand-maintained copy. Do not propose moving it into the loader modules.
- Keep optional lightning-conversion injections in their existing shared Mixin; splitting them solely to make every injection required is unnecessary.

## Commands and player experience

- There is one command root: /sg. Keep the client/server routing needed to support it. Do not propose a second root to simplify the code.
- Code complexity may support a simpler player interface. Visual authoring and /sg admin commands may require a player looking at the target. Selectors, server-console use, command blocks, and datapack execution are not requirements for these visual tools.
- Held-eye hurt/heal reactions should be visible to every viewer. The existing held-eye grow trigger on any damage is intentional; it need not match mobs' player-caused-damage gate.
- Preserve the existing protection policy for eye interactions with players; do not invent an additional attack-event/PvP-claim policy.

## Rendering and integration

- The current aim-wink side is correct. Do not raise it again from an assumed model-axis convention.
- A single GeckoLib frame field is acceptable without a demonstrated nested-draw case.
- The held/item iris scale is a fixed choice for its appearance in the hand, independent of the mob-eye default. Equal values do not imply a shared tuning rule.
- Preserve working GUI item sizing and pause-independent GUI animation. An untested claim about a model transform is not a reason to rewrite rendering.
- Keep Twilight Forest, Heads, Citadel, and related optional integration support in common where it shares the existing pipeline, even if only Forge can use it in this checkout.
- Keep GeckoLib null-name checks. Do not remove third-party integration guards on an unverified contract.
- A model failure must leave other models a chance to succeed. Scope recoverable reflection failures to the affected model or attachment; family-wide unavailability is appropriate only when the shared API itself cannot load.

## Data, loot, and networking

- Preserve the working bundled eye-definition labels and placements, including entries whose labels name another Minecraft version. Do not retag, re-export, or delete them based only on the label.
- Preserve nearest-version fallback selection. This is intentional functionality, not obsolete compatibility.
- Keep the working Fabric persistent-data representation. A typed-attachment rewrite is not requested.
- Preserve saved keys and supported numeric color input forms. A reader accepting more in-range numeric types than its writer emits does not itself prove legacy support or a defect.
- Head and self shearing may remove eyes and wear shears even if current definitions provide no eye to drop.
- Keep the shared vanilla CopyNbtFunction operation for placed-head loot on both loaders. Move shears-on-kill eye drops into the normal loot pipeline; that remains an implementation direction, not a claim that it is complete.
- Do not add required dependencies, including Forge Config API Port.
- Keep the shared packet-size reserve, including the extra framing byte on Fabric. A separate loader-specific limit is unnecessary.
- Preserve the present requirement that both endpoints have the mod while multiplayer behavior is verified. Dedicated-server multiplayer testing has not yet happened; Fabric's JOIN-time channel check is unverified, not a proven bug.

## Tests and logging

- Leave eyedMobDropsAnEyedHead, its TODO, and its commented-out loader wrappers unchanged and disabled. Investigating or reviving this test is deferred.
- Ignore the out-of-range-color manual reproduction issue; do not spend review effort rewriting that test.
- Keep Fabric protection-ordering checks manual; do not add permanent flag-gated refusal listeners.
- Startup logs should report whether optional Twilight Forest Mixins applied, without making the integration required.
- Effective server-config summaries should include behavior master switches.

## Evidence and remaining work

The decisions above do not prove an unexamined API contract. The individual reports retain implementation follow-up and explicitly unverified checks: released Forge 0.8.2 saved-data shape, Forge mooshroom conversion-event coverage, native block-entity persistence equivalence, and Fabric multiplayer channel timing. Preserve working behavior until permitted source or an in-game check justifies a change. The unanswered Correctness question about client-level unload remains in that report.
