# JEI Integration — Working Notes

Saved conversation summary. Covers the brewing-recipe fix (done) and the eye_modifier
crafting-recipe assessment (pending a decision).

---

## Background: the problem

A client debug.log (`:v/googly/d` filtered view) surfaced this DEBUG line during a client run:

```
[mezz.jei.forge.platform.BrewingRecipeMaker]: Can't handle brewing recipe class:
class com.github.crittscott.somegoogly.potion.ModPotions$1
```

Cause: the googly-eyes brew is a custom Forge `IBrewingRecipe` (anonymous class `ModPotions$1`)
because it copies the eye item's appearance NBT onto the output — something the static
`BrewingRecipe` class can't do. JEI's brewing scanner only introspects vanilla and the static
Forge `BrewingRecipe`, so it silently skips the custom one and the brew is invisible in JEI.

The rest of that filtered log was clean: mod loads, **86/86 eye configs loaded with zero parse
failures**, network channel `somegoogly:main` v`3` accepted both directions. The JEI line was the
only real finding.

---

## DONE: brewing recipe now shows in JEI

### Code
- **`client/compat/jei/SomeGooglyJeiPlugin.java`** (new) — `@JeiPlugin` that registers a
  representative entry in JEI's brewing category: `awkward splash + googly eye → googly-eyes splash`,
  via `IVanillaRecipeFactory.createBrewingRecipe(...)`. Referenced only by JEI's annotation scan, so
  it never loads when JEI is absent (keeps JEI a true soft dependency). The displayed eye/output
  carry no appearance override; the real brew still copies whatever the eye item holds.
- **`potion/ModPotions.java`** — added `public static ItemStack representativeSplash()` routing
  through the existing private `newGooglySplash()`, so the JEI-shown output can't drift from the real
  brew or the creative-tab entry.

### Build wiring
- **build.gradle** — added BlameJared maven (`https://maven.blamejared.com`) and JEI deps:
  `common-api` + `forge-api` as `compileOnly`, `forge` as `runtimeOnly`.
- **gradle.properties** — `jei_version=15.3.0.4`.
- **mods.toml** — optional client-side `jei` dependency, `ordering="AFTER"`.

### Spec
- **living-spec.md** — §9.4 notes the custom `IBrewingRecipe`; new **§9.6 JEI integration**
  documents the plugin and flags the eye_modifier gap (see below) as Partial.

### Verification
- `./gradlew compileJava` succeeds; JEI 15.3.0.4 resolved cleanly.
- API note: JEI 15.3.0.4 has **no `uid` overload** of `createBrewingRecipe` — only the 3-arg form
  `createBrewingRecipe(List<ItemStack> ingredients, ItemStack potionInput, ItemStack potionOutput)`.
- Remaining build warnings are all pre-existing `FMLJavaModLoadingContext.get()` deprecations.
- NOT yet done: launching the client to visually confirm the entry appears in JEI's brewing tab.

---

## PENDING DECISION: eye_modifier crafting recipe in JEI

### Why it's also missing
`EyeModifierRecipe extends CustomRecipe`, registered via `SimpleCraftingRecipeSerializer`
(`ModRecipes.EYE_MODIFIER`) — a **special recipe with no JSON and no fixed ingredients/output**.
That's exactly the class JEI's crafting category skips. Can't be fixed with data; needs display-only
recipes registered through the JEI plugin. (Real crafting keeps running through `EyeModifierRecipe`.)

The recipe: exactly 1 `googly_eye` + 1 modifier ingredient →
- any of 16 vanilla **dyes** → set iris color
- **glowstone dust** → glow on
- **redstone** → glow off
- **cobweb** → strip all overrides back to plain

### Recommended approach: synthetic display recipes (no custom category)
All transforms are "1 eye + 1 ingredient → 1 eye" in a shapeless grid = the **vanilla crafting
category**. So register display-only `ShapelessRecipe` instances into `RecipeTypes.CRAFTING` from the
plugin. Do NOT build a bespoke `IRecipeCategory` (~200 lines of UI for no benefit).

Key win: outputs computed by reusing `EyeModifier.apply(EMPTY, ingredientStack)` +
`GooglyEyeItem.create(...)`, so displayed output stays correct if a modifier changes, and the eye
item already GUI-renders tinted/centered so examples look right.

### Work breakdown (~60 lines, two files + spec)
1. **Interface change (~20 lines):** add `List<ItemStack> displayIngredients()` to `EyeModifier`,
   implemented by each of the 4 modifiers (16 dyes / glowstone / redstone / cobweb). Avoids
   hardcoding the ingredient set in the plugin and keeps the design's single-source-of-truth ("a new
   appearance edit is a new entry in MODIFIERS, not a new recipe class") — a future modifier then
   shows in JEI for free.
2. **Plugin (~40 lines):** iterate `EyeModifier.MODIFIERS`; for each display ingredient compute
   `output = create(modifier.apply(EMPTY, ingredient))`, build
   `ShapelessRecipe(synthetic-id, "", category, output, [Ingredient.of(eye), Ingredient.of(ingredient)])`,
   collect, and `registration.addRecipes(RecipeTypes.CRAFTING, list)`.
   (`RecipeTypes.CRAFTING` is `RecipeType<CraftingRecipe>` in 1.20.1 — direct, no `RecipeHolder`.)
3. **Spec (~2 lines):** flip §9.6's eye_modifier note from "not registered (Partial)" to Implemented.

### Caveats to decide up front
- **~19 entries** (16 dyes + 3). Accurate and browsable but 16 dye pages. Alternative: collapse dye
  to one entry using the `forge:dyes` tag with a single representative tinted output (e.g. red) — 1
  page vs 16, at the cost of not showing every color. Recommendation: keep all 16 (the tinted
  outputs are the point).
- **Glow on/off look identical as icons** (glow is a render pass, invisible in a static GUI slot).
  Distinguishable only via the tooltip, which already prints `Glow: true/false`.
- **Cobweb (strip) needs a decorated input** — with a blank input eye, input and output look the
  same; use a tinted+glowing example input so "strips to plain" is visible (~3 extra lines).
- No new dependency/API risk — same JEI artifact already wired.

### Open question for implementation
- All 16 dyes + decorated cobweb input, **or** the collapsed single-dye-entry variant?
