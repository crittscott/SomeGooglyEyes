# Picker HUD and multiplayer checks

Use MultiMC with a disposable world. Repeat loader-specific checks on Fabric and Forge. These checks require a game client; source review and dedicated-server GameTests do not establish their results.

## HUD and attachments

1. Enable the picker without a target. Check the no-target hint, including rebound lock keys.
2. Choose an adult and a baby mob. Check target, weighted variant, age, part, and eye-count rows; cycle parts and variants.
3. Create an unsaved eye, save it, select it, and deselect it. Check that the selected/unsaved rows are yellow, ordinary eye rows are white/gray, and headers are green with white values.
4. With a language resource pack, reorder the positional arguments of `somegoogly.picker.hud.target_row`. Reload resources and confirm the values follow the translated order and retain their styles. Check row spacing and panel bounds with the pack's font and at different GUI scales.
5. Exercise a hierarchical mob, an ageable mob, GeckoLib bones, and a reflected model when the integrations are installed. Select and export parts, then reload the exported definitions and confirm the eyes remain attached.
6. Compare mob-pose azimuths with eye-authoring azimuths. Inspect a Googly Eye item in the hand, inventory, and a frame.
7. With Twilight Forest installed, inspect Ur-Ghast, Alpha Yeti, and Knight Phantom trophies and their head items. Each should still show eyes on the face, including after resource reload.

## Dedicated-server multiplayer

1. Join with matching mods on both endpoints. Check definition/settings synchronization before editing. Test missing-mod and network-version-mismatch refusals in both directions; Fabric's JOIN-time channel check remains unverified until this succeeds.
2. Have two players observe an eyed mob. Trigger expressions and anger changes, then let one player start tracking mid-expression. Check matching progress, expiration, and rendering after looking away and back.
3. Reload unchanged and changed eye definitions/settings. Check that unchanged data does not reset eye motion and changed definitions reach both players.
4. As a creative operator, freeze and release a target and export an adult and baby definition remotely. Check that each authored age replaces only that age. Lose operator or creative status while editing and verify that release still restores the mob's AI.
5. Break and replace eyed vanilla and optional-mod heads. Check retained eye appearance and placement; plain heads must drop without eye data and stack with plain items.
