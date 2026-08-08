# Code Review: Magic Numbers and Magic Strings

Scope: `src/main/java/com/github/crittscott/somegoogly/**` and `src/main/resources/assets/somegoogly/lang/en_us.json`. The `othersourcecode/` tree (vendored reference sources for Citadel, GeckoLib, etc.) is out of scope — it isn't this mod's code.

Overall the codebase is disciplined about this: physics tuning (`GooglyTracker`), eye placement defaults (`EyePlacement`), and server config (`ServerConfig`) all use well-named, well-commented constants and Forge's config API rather than inline literals. The findings below are the exceptions to that pattern.

---

## 1. Numeric literals

### 1.1 The same "20 block reach" value defined twice, independently
- `src/main/java/com/github/crittscott/somegoogly/command/GooglyAdminCommand.java:57`
  ```java
  private static final double REACH = 20.0;
  ```
- `src/main/java/com/github/crittscott/somegoogly/command/SpawnAllCommand.java:68`
  ```java
  /** Reach of the {@code /sg spawn} placement raytrace (matches the admin command's target reach). */
  private static final double SPAWN_REACH = 20.0;
  ```
  The comment on `SPAWN_REACH` explicitly documents that it must track `GooglyAdminCommand`'s `REACH`, but nothing enforces that — they're two separate `private` constants in two separate classes. If one is tuned, the other silently drifts out of sync with no compiler or test signal. Worth promoting to one shared constant (e.g. on `LookTarget`, which both commands' reach concept is adjacent to).

  Note: `EyeInspectIndicator.java:38` also defines `REACH = 16.0D`, but that one is deliberately different (commented as intentionally shorter than crosshair pick range), so it isn't part of this duplication — just flagging it for contrast.

### 1.2 The percent-roll divisor `100` is hand-repeated in three files
- `src/main/java/com/github/crittscott/somegoogly/event/ServerEventHandler.java:46`
  ```java
  hasGooglyEyes = random.nextFloat() < (percent / 100F);
  ```
- `src/main/java/com/github/crittscott/somegoogly/eye/behavior/ServerBehaviorScheduler.java:104`
  ```java
  if (state == null || RANDOM.nextInt(100) >= ServerConfig.GROW_ON_HIT_PERCENT.get()) {
  ```
- `src/main/java/com/github/crittscott/somegoogly/event/EyeItemInteractions.java:160`
  ```java
  if (mob.getRandom().nextInt(100) >= ServerConfig.HARVEST_ON_KILL_PERCENT.get()) {
  ```
  All three implement the same "roll a configured percent chance" pattern, each spelling out the `100` scale itself, and using two different techniques (`nextFloat() < p/100F` vs. `nextInt(100) >= p`). Every `ServerConfig` percent field is documented and range-checked as `0..100`, so a shared helper (e.g. `ServerConfig.rollPercent(RandomSource random, IntValue percent)`) would remove the repeated literal and the inconsistency in technique at the same time.

### 1.3 Ticks-per-second (`20`) used inline for a cooldown countdown
- `src/main/java/com/github/crittscott/somegoogly/picker/PickerExportService.java:74`
  ```java
  int seconds = (COOLDOWN_TICKS - (now - last) + 19) / 20;
  ```
  This converts remaining ticks to a round-up second count using the vanilla 20-ticks-per-second constant, spelled out as bare `19`/`20` with no named constant. Low risk (Minecraft's tick rate is effectively fixed and this is the only site doing the conversion), but the two magic numbers together implement a non-obvious round-up-division idiom that would benefit from a name or at least an inline comment.

### 1.4 Tracker eviction threshold `10` has no named constant
- `src/main/java/com/github/crittscott/somegoogly/event/ClientEventHandler.java:149`
  ```java
  if (idle > 10) {
  ```
  This is the only place the literal is actually used, but two other files' comments describe it in prose as a fixed fact: `ClientEventHandler.java:39` ("evicted by the 10-tick sweep") and `GooglyTracker.java:26` ("evict once it's more than 10 ticks stale"). Extracting a constant (e.g. `EVICT_AFTER_TICKS`) would let those comments reference `{@link #EVICT_AFTER_TICKS}` instead of repeating the number in prose, so the three descriptions can't drift apart.

### 1.5 Minor: enchantment cost numbers
- `src/main/java/com/github/crittscott/somegoogly/enchant/OptometristEnchantment.java:29-34`
  ```java
  public int getMaxCost(int level) { return getMinCost(level) + 30; }
  public int getMinCost(int level) { return 15; }
  ```
  Single-use, self-contained, and low risk — flagging only because `15` and `30` carry real tuning meaning (enchantment table cost) and aren't named. Not a high priority; a senior reviewer could reasonably leave this as-is given it's only read in one place.

---

## 2. String literals (identifiers / keys / mod-id)

### 2.1 The mod-id string `"somegoogly"` is hardcoded instead of using `SomeGoogly.MOD_ID`
`SomeGoogly.java:40` defines `public static final String MOD_ID = "somegoogly";` and most of the codebase uses it correctly (`ModItems`, `ModCreativeTabs`, `NetworkHandler`, etc.). Three production sites retype the literal instead:
- `src/main/java/com/github/crittscott/somegoogly/command/GooglyAdminCommand.java:111`
  ```java
  : new ResourceLocation("somegoogly", id);
  ```
- `src/main/java/com/github/crittscott/somegoogly/eye/behavior/AbstractEyeBehavior.java:12`
  ```java
  this.id = new ResourceLocation("somegoogly", name);
  ```
- `src/main/java/com/github/crittscott/somegoogly/client/render/GooglyEyeRenderer.java:36`
  ```java
  private static final ResourceLocation TEX = new ResourceLocation("somegoogly", "textures/model/modelgooglyeye.png");
  ```
  This is exactly the kind of typo-fragile duplication the review is meant to catch: every behavior id and the eye texture's `ResourceLocation` are built from a retyped literal rather than `SomeGoogly.MOD_ID`. A rename of the mod id (unlikely, but the class exists precisely to make that a one-line change) would silently miss these three sites.

  (GameTest files also retype `"somegoogly"` — e.g. `PickerExportGameTests.java:50`, `SerializationGameTests.java:101` — but those are test-only per this project's testing conventions and lower priority.)

### 2.2 `"minecraft:player"` built two different ways
- `src/main/java/com/github/crittscott/somegoogly/command/SpawnAllCommand.java:60`
  ```java
  private static final ResourceLocation PLAYER = new ResourceLocation("minecraft", "player");
  ```
- `src/main/java/com/github/crittscott/somegoogly/event/ClientEventHandler.java:54`
  ```java
  if (!ClientConfig.isEntityDisabled(new ResourceLocation("minecraft", "player"))) {
  ```
  Minor — `"minecraft"` as a namespace is about as stable as constants get — but it's the same value constructed as a named constant in one file and an inline literal in another, which is inconsistent style rather than a real risk.

### 2.3 The config/codec field name `"enabled"` is retyped by hand in tests
`HeadInfo.java` defines the `enabled` field twice via `Codec.BOOL.fieldOf("enabled")` (lines 127 and 244) — that's the legitimate schema-definition site. Several GameTest files build or inspect NBT/JSON using the same key as a raw string with no shared constant:
- `gametest/PickerExportGameTests.java:84` — `garbage.putBoolean("enabled", true);`
- `gametest/SerializationGameTests.java:180` — `config.putBoolean("enabled", true);`
- `gametest/ConfigGameTests.java:137-142` — `"enabled": true` inside inline JSON fixtures

  This is standard practice for tests that exercise an external-facing JSON/NBT contract (per this repo's GameTest-only testing convention, test code is expected to mirror the shipped format rather than reach into internals), so it's lower priority than 2.1 — but if `HeadInfo`'s field name ever changes, none of these raw-string call sites would fail to compile, only fail at runtime.

---

## 3. Localization (en_us.json coverage)

`src/main/resources/assets/somegoogly/lang/en_us.json` exists and covers: the enchantment name, both item names, the creative tab name, the four keybinding categories/names, and the five `EyeInspectIndicator` action-bar messages (all correctly routed through `Component.translatable`, confirmed in `EyeInspectIndicator.java:76-85` and `ModCreativeTabs.java:28`).

### 3.1 Item tooltip text bypasses localization entirely (real gameplay-facing gap)
`src/main/java/com/github/crittscott/somegoogly/item/EyeItemProperties.java:25-33` — shared by both `GooglyEyeItem` and `SlimyEyeItem`, so this runs for any player who hovers either item:
```java
public static void appendTooltip(ItemStack stack, List<Component> tooltip) {
    AppearanceOverride props = get(stack);
    props.iris().ifPresent(color ->
            tooltip.add(Component.literal("Iris: #" + String.format("%06X", color.toRgb24())).withStyle(ChatFormatting.GRAY)));
    props.cornea().ifPresent(color ->
            tooltip.add(Component.literal("Cornea: #" + String.format("%06X", color.toRgb24())).withStyle(ChatFormatting.GRAY)));
    props.glow().ifPresent(glow ->
            tooltip.add(Component.literal("Glow: " + glow).withStyle(ChatFormatting.GRAY)));
}
```
"Iris:", "Cornea:", and "Glow:" are hardcoded English literals via `Component.literal`, not `Component.translatable`. This is the one clear case where ordinary (non-admin, non-creative-only) gameplay text is not routed through `en_us.json`, even though the lang file already exists and is used correctly elsewhere in the mod. Fixing it would mean adding keys like `somegoogly.tooltip.iris`/`cornea`/`glow` and using `Component.translatable(key, hexString)`.

### 3.2 Command and packet feedback is entirely `Component.literal` (lower priority — admin/creative tooling)
Every `/sg`, `/sg admin`, and picker network-packet feedback message is built with `Component.literal`, never `Component.translatable`:
- `command/GooglyClientCommands.java` (multiple, e.g. lines 62-80, 137)
- `command/GooglyAdminCommand.java:115,148,174,178,184`
- `command/SpawnAllCommand.java` (multiple, e.g. lines 192, 286-365)
- `picker/PickerPermissions.java:31`
- `client/picker/PickerInput.java:27`
- `network/PickerExportPacket.java:68`, `PickerFreezePacket.java:65`, `PickerMobPosePacket.java:86,91,99`, `PickerSpawnPacket.java:46`, `PickerSpawnAllPacket.java:51`

  All of these gate on operator permission (`requires(src -> src.hasPermission(2))`) or creative mode (`PickerPermissions`), i.e. they're mod-author/admin tooling rather than ordinary player-facing text — that's a reasonable reason to deprioritize localizing them, and it may be a deliberate choice given these are debug-oriented commands. Flagging for completeness since the task asks whether all user-facing strings are routed through the lang file, but this category carries much less risk/urgency than 3.1.

### 3.3 The picker HUD's text is entirely hardcoded, and not even via `Component`
`src/main/java/com/github/crittscott/somegoogly/client/picker/PickerHud.java` builds its whole overlay ("Googly Eye Picker", "Look at a mob and choose it (V).", "Target: …", "Part: … cycle", "Eyes (…):", "none") as raw Java `String`s drawn directly with `graphics.drawString(font, line.text, ...)` — bypassing both `Component` and the lang file. Same caveat as 3.2: this is a creative-mode authoring HUD, not standard gameplay UI, so it's a low-priority finding, but it's the most complete example of end-user-visible text with zero localization plumbing.

---

## Summary by priority

| Priority | Finding | Location(s) |
|---|---|---|
| High | Item tooltip text not localized | `EyeItemProperties.java:28-32` |
| Medium | `"somegoogly"` mod-id retyped instead of `SomeGoogly.MOD_ID` | `GooglyAdminCommand.java:111`, `AbstractEyeBehavior.java:12`, `GooglyEyeRenderer.java:36` |
| Medium | Duplicate `20.0` reach constant, documented as coupled but not shared | `GooglyAdminCommand.java:57`, `SpawnAllCommand.java:68` |
| Low-Medium | Repeated `100` percent-roll divisor, inconsistent technique | `ServerEventHandler.java:46`, `ServerBehaviorScheduler.java:104`, `EyeItemInteractions.java:160` |
| Low | Ticks-per-second (`19`/`20`) inline in cooldown math | `PickerExportService.java:74` |
| Low | Eviction threshold `10` un-named, referenced only in prose | `ClientEventHandler.java:149` (comments in `ClientEventHandler.java:39`, `GooglyTracker.java:26`) |
| Low | Enchantment cost numbers un-named | `OptometristEnchantment.java:29-34` |
| Low | `"minecraft:player"` built inconsistently (constant vs. inline) | `SpawnAllCommand.java:60`, `ClientEventHandler.java:54` |
| Low | `"enabled"` codec key retyped in test fixtures | `ConfigGameTests.java:137-142`, `PickerExportGameTests.java:84`, `SerializationGameTests.java:180` |
| Low (by design choice) | Admin/picker command feedback and picker HUD not localized | `command/*`, `network/Picker*Packet.java`, `PickerHud.java` |
