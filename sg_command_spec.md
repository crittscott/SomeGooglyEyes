# `/sg` Command System — Specification for Googly Eyes Mod

**Mod:** Googly Eyes | **MC version:** 1.20.1 | **Loader:** Forge  
**Purpose:** Interactive in-game eye placement with config export

---

## Overview

The `/sg` command system enables creative-mode placement of googly eyes on mobs. Because eye placement is inherently visual — mobs vary wildly in shape, scale, and part structure — a CLI-based workflow inside the game is far preferable to external config editing.

The system is built on **Brigadier** (Minecraft's command framework) with two small extensions described below.

---

## Command Reference

Base command: `/sg`

### Mob Selection

```
/sg ch[oose]
```
Selects the mob currently in the player's crosshair.

```
/sg un[choose]
```
Clears the current mob selection.

---

### Part Selection

```
/sg pa[rt] <name|number|none>
```
Sets the coordinate frame for eye placement to the named or numbered model part. Part names/numbers are discovered via the existing keyboard shortcut that cycles through model parts. `none` clears the part selection.

---

### Eye Creation and Positioning

```
/sg cr[eate] <x> <y> <z>
```
Creates a new eye at position `(x, y, z)` relative to the selected part's coordinate origin. Units: blocks.

```
/sg mv <x> <y> <z>
/sg mo[ve] <x> <y> <z>
```
Sets the eye's **absolute** position relative to the part origin. Coordinates support `~` as a no-op (leave that axis unchanged):

```
/sg mv 0.1 ~ ~     ← sets x to 0.1, y and z unchanged
/sg mv ~ ~ 0.05    ← sets z to 0.05, x and y unchanged
```

```
/sg ro[t] <inclination> <azimuth>
```
Sets the eye's orientation.
- `inclination`: angle from the part's `+y` axis (degrees)
- `azimuth`: angle from the part's `+x` axis (degrees)
- Both support `~` (leave unchanged)

---

### Eye List Management

```
/sg sa[ve]
```
Appends the current eye (position, rotation, part, properties) to the session eye list.

```
/sg se[lect] <number>
```
Loads eye `<number>` from the list for further adjustment.

```
/sg de[lete] <number>
```
Removes eye `<number>` from the list.

```
/sg li[st] pa[rts]
/sg li[st] ey[es]
```
Lists all model parts of the current mob, or all eyes in the current list.

---

### Eye Properties

```
/sg pr[operties] ...
```
Sets visual properties of the current eye. Parameters TBD, covering at minimum:
- Iris size
- Pupil size  
- Iris color / pupil color

---

### Export and Utilities

```
/sg ex[port]
```
Writes the eye list to the standard mod JSON config format.

```
/sg spawnall
```
Spawns all mobs without AI (for visual inspection). No short form — the full word is required to prevent accidental execution.

---

## Command Chaining

Multiple commands may be issued on a single line, separated by commas. Commas do not appear in any command syntax, so they serve unambiguously as separators. Commands execute left to right:

```
/sg se 2, mv 0.2 0.2 ~, ro 45 ~, sa
```

This selects eye 2, moves it, adjusts its inclination, and saves — all in one line.

---

## Abbreviation Rules

Each command accepts either a short form or the full word. The short forms are the minimum unambiguous prefix shown in brackets above (e.g. `ch` or `choose`, `mv` or `move`). No intermediate lengths are required.

---

## Implementation Notes for Claude Code

### Framework: Brigadier

The command system is implemented using **Brigadier**, Minecraft/Forge's standard command framework. Most commands map directly onto Brigadier primitives.

#### Straightforward Brigadier (no issues)

The following are standard Brigadier literal + argument patterns and require no special handling:

| Command | Brigadier types |
|---|---|
| `ch`, `un`, `sa`, `ex`, `spawnall` | Literal only |
| `cr x y z`, `mv x y z` | Literal + `FloatArgumentType` × 3 |
| `rot incl azi` | Literal + `FloatArgumentType` × 2 |
| `se <n>`, `de <n>` | Literal + `IntegerArgumentType` |
| `li pa`, `li ey` | Nested literals |
| `pa <name/number/none>` | Literal + `StringArgumentType.word()` with manual dispatch |

#### Non-trivial: `~` no-op syntax in `mv` and `rot`

Brigadier has no built-in argument type for "float or `~`". A **custom `ArgumentType<Optional<Float>>`** is required. It parses either a floating-point number (returning `Optional.of(value)`) or the literal `~` character (returning `Optional.empty()`), which the executor interprets as "leave this axis unchanged."

This requires:
1. `MaybeFloatArgumentType implements ArgumentType<Optional<Float>>`
2. Registration via `ArgumentTypeInfo` and the Forge argument type registry
3. Approximately 1–2 hours of work total.

#### Non-trivial: Abbreviation aliases

Brigadier matches literals exactly — there is no prefix-matching built in. The cleanest approach is to **register both the short form and full word as separate literals pointing to the same executor**:

```java
Commands.literal("mv").then(...)
Commands.literal("move").then(...)  // identical subtree
```

This is verbose but mechanical, preserves tab-completion for both forms, and requires no runtime string dispatching. A shared builder method per command keeps it from being unwieldy.

#### Non-trivial: Comma-chained commands

Brigadier parses one command per invocation. Chaining is implemented as a **pre-dispatch shim** in the command event handler:

1. Intercept the raw input string before Brigadier sees it
2. Split on `,`, trim whitespace from each fragment
3. Prepend `sg ` to each fragment if not present
4. Dispatch each through the Brigadier dispatcher in sequence

This is approximately 20 lines of code and lives outside Brigadier entirely.

### Effort Summary

| Feature | Effort |
|---|---|
| Fixed-argument commands | Trivial |
| `~` no-op argument type | ~1–2 hrs (custom `ArgumentType`) |
| Abbreviation aliases | Low (mechanical repetition) |
| `pa name/number/none` parsing | Minimal (string arg + switch) |
| Comma chaining | Low (~20 lines, pre-dispatch) |

The implementation is approximately **80% standard Brigadier**. No part of the design fights the framework.
