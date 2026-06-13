# SomeGoogly test datapack

Verifies the datapack-driven eye config: per-entity override + the `enabled:false` kill-switch,
synced server→client.

## What it does
- `data/minecraft/eyes/zombie.json` — overrides zombies to **big red** eyes (vs the default small
  black ones). Tests geometry override.
- `data/minecraft/eyes/cow.json` — `"enabled": false`. Tests the server-authoritative disable.

## Install (MultiMC, single-player)
Copy this whole `somegoogly-test-datapack` folder into your world save's datapacks folder:

```
…/MultiMC/instances/1.20.1/.minecraft/saves/<your-world>/datapacks/somegoogly-test-datapack/
```

The folder must contain `pack.mcmeta` at its top level (it does).

## Apply
- Easiest: add the folder while the world is **closed**, then load the world — datapacks in the
  folder are enabled automatically.
- If the world is already open: run `/reload`. If it doesn't take, run
  `/datapack enable "file/somegoogly-test-datapack"` then `/reload`.

## Expect
- **Zombies:** big red eyes, immediately after `/reload` (geometry is client-side; existing zombies
  update live).
- **Cows:** no eyes, immediately after `/reload` (the client honors `enabled:false`; newly spawned
  cows are also gated server-side).
- Remove the folder + `/reload` to revert to the mod's built-in defaults.
