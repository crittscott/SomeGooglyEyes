# Code Review: Over-Guarding

Scope: `src/main/java/com/github/crittscott/somegoogly/**`, reviewed for defensive code (null checks,
try/catch, instanceof/type checks, bounds checks) that guards against conditions Minecraft/Forge or the
mod's own code already guarantees cannot occur. Per project philosophy, guarding against Minecraft/Forge's
own hypothetical internal bugs, or against data the mod itself produces and controls, is unnecessary
complexity. Guarding real boundaries (network, disk, config, player input, third-party mod reflection) is
legitimate and is *not* flagged here.

Three confirmed findings, in descending order of how many call sites they affect.

---

## 1. `Resolvers.forModel()` null checks — the method can never return null

**File:** `src/main/java/com/github/crittscott/somegoogly/client/render/resolver/Resolvers.java`

```java
private static final List<EyeAttachmentResolver> ALL = List.of(
        new HierarchicalResolver(),
        new AgeableListResolver(),
        new CitadelResolver(),
        new LLibraryResolver(),
        new ChildMapResolver()
);
...
public static EyeAttachmentResolver forModel(EntityModel<?> model) {
    for (EyeAttachmentResolver r : ALL) {
        if (r.handles(model)) {
            return r;
        }
    }
    return null;
}
```

`ChildMapResolver`, the last entry in `ALL`, is documented and implemented as an unconditional catch-all:

`src/main/java/com/github/crittscott/somegoogly/client/render/resolver/ChildMapResolver.java:39-41`
```java
@Override
public boolean handles(EntityModel<?> model) {
    return true; // catch-all; the named-model resolvers are tried first
}
```

Since the loop in `forModel` always reaches (at worst) `ChildMapResolver`, which matches every
`EntityModel<?>` unconditionally, `forModel()` can never actually return `null` for any non-null model —
the `return null;` at the end of the loop is unreachable. Every caller passes a model obtained directly
from a live renderer (`this.getParentModel()`, `ler.getModel()`, `living.getModel()`), never null.

This dead-code null check is duplicated at four call sites:

- `src/main/java/com/github/crittscott/somegoogly/client/render/LayerGooglyEyes.java:50-53`
  ```java
  EyeAttachmentResolver resolver = Resolvers.forModel(model);
  if (resolver == null) {
      return;
  }
  ```
- `src/main/java/com/github/crittscott/somegoogly/client/picker/PickerLayer.java:41-44`
  ```java
  EyeAttachmentResolver resolver = Resolvers.forModel(model);
  if (resolver == null) {
      return;
  }
  ```
- `src/main/java/com/github/crittscott/somegoogly/client/picker/PickerState.java:295-296`
  ```java
  EyeAttachmentResolver resolver = Resolvers.forModel(vanillaModel);
  if (resolver != null) {
      tokens = resolver.enumerateParts(vanillaModel);
      ...
  ```
  (Here the dead branch is the *positive* case: the `if (resolver != null)` block is not a redundant
  early-return but it does mean the subsequent GeckoLib-enumeration fallback right below it is
  unreachable for any `LivingEntityRenderer`-backed model, since `resolver` is never null there.)
- `src/main/java/com/github/crittscott/somegoogly/client/picker/PickerExporter.java:199-202`
  ```java
  EyeAttachmentResolver resolver = Resolvers.forModel(model);
  if (resolver == null) {
      return null;
  }
  ```

**Why it's over-guarding:** the "no resolver found" case these four sites all defend against is foreclosed
by `Resolvers.ALL`'s own composition — a fact established entirely by this mod's own code, not by any
external or unreliable interface. This isn't a defect (the guards are harmless), but it's dead code
propagated to four places instead of being either removed or, if the catch-all's unconditional `true` is
ever meant to become conditional in the future, documented as intentionally defensive.

---

## 2. `SomeGoogly.clientEventHandler` null checks — unreachable after mod construction

**File:** `src/main/java/com/github/crittscott/somegoogly/SomeGoogly.java`

`clientEventHandler` is assigned exactly once, synchronously, inside the constructor's client-only branch:

```java
public static ClientEventHandler clientEventHandler;

public SomeGoogly() {
    ...
    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
        ...
        MinecraftForge.EVENT_BUS.register(clientEventHandler = new ClientEventHandler());
        ...
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::addLayers);
        ...
    });
    ...
}

private void addLayers(EntityRenderersEvent.AddLayers event) {
    if (clientEventHandler != null) {
        clientEventHandler.addLayers();
    }
}
```

`DistExecutor.unsafeRunWhenOn` runs its supplier synchronously when on the matching physical side, so on a
physical client the assignment to `clientEventHandler` completes before the constructor returns — i.e.
before FML fires any later lifecycle event (`AddLayers`, world load, network connection, etc.). On a
physical dedicated server, this whole branch never runs at all, but neither does any of the code that reads
`clientEventHandler` (each such site is itself only reachable via `Dist.CLIENT`-gated code or client-only
render classes). So by the time anything reads `clientEventHandler`, it is either guaranteed non-null
(client) or the reading code never executes (server).

The `addLayers()` null check above is one instance. Two more appear in packet handlers, both reached only
via `DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)` — i.e. only after the same client-side constructor
branch has already run:

`src/main/java/com/github/crittscott/somegoogly/network/EyeConfigSyncPacket.java:86-92`
```java
DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
    ClientEyeConfigs.replaceAll(packet.configs);
    if (SomeGoogly.clientEventHandler != null) {
        SomeGoogly.clientEventHandler.clearTrackers();
    }
})
```

`src/main/java/com/github/crittscott/somegoogly/network/EyeBehaviorTriggerPacket.java:70-74`
```java
private static void play(EyeBehaviorTriggerPacket packet) {
    EyeBehavior behavior = EyeBehaviors.byId(packet.behaviorId);
    if (behavior == null || SomeGoogly.clientEventHandler == null) {
        return; // unknown id (build skew) or no client handler yet
    }
```

The comment `"no client handler yet"` implies a believed startup race, but there is none: these handlers
only run after a client has fully joined a world/server, which is always long after mod construction (and
hence `clientEventHandler` assignment) completes.

This is confirmed by two sibling call sites in render code that read `SomeGoogly.clientEventHandler`
**without** any null check — proving the codebase itself treats it as always non-null once rendering is
possible:

- `src/main/java/com/github/crittscott/somegoogly/client/render/LayerGooglyEyes.java:55`
  ```java
  GooglyTracker tracker = SomeGoogly.clientEventHandler.getGooglyTracker(living, helper);
  ```
- `src/main/java/com/github/crittscott/somegoogly/client/compat/GooglyGeoLayer.java:111`
  ```java
  frame.tracker = SomeGoogly.clientEventHandler.getGooglyTracker(living, helper);
  ```

Both of these run only after `addLayers()` has already successfully called
`clientEventHandler.addLayers()` to register the very layer that is now rendering — which could not have
happened if `clientEventHandler` were null. So the three null checks above are guarding a state that
cannot arise, and are inconsistent with the rest of the codebase's own treatment of the field.

---

## 3. `EyeItemInteractions.onLivingDrops` — redundant `helper == null` check

**File:** `src/main/java/com/github/crittscott/somegoogly/event/EyeItemInteractions.java:164-167`

```java
HeadInfo helper = helperFor(mob);
if (helper == null || !helper.hasConfig()) {
    return; // no geometry to sample an appearance from
}
```

`helperFor` (same file, lines 97-103) is declared to return `HeadInfo` (not `@Nullable HeadInfo`) and
always does:

```java
private static HeadInfo helperFor(LivingEntity mob) {
    ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
    return HeadInfo.serverHelper(type, mob);
}
```

`HeadInfo.serverHelper` (`src/main/java/com/github/crittscott/somegoogly/eye/HeadInfo.java:427-431`)
unconditionally constructs and returns a `HeadInfo`:

```java
public static HeadInfo serverHelper(ResourceLocation entityName, LivingEntity entity) {
    RuntimeConfig config = ServerEyeConfigs.get(entityName, entity);
    int variant = chooseVariantIndex(config, EyeState.getVariantRoll(entity));
    return new HeadInfo(config, variant);
}
```

The private constructor (`HeadInfo.java:45-48`) never returns `null` and has no failure path — a `null`
`config` argument is handled internally (`heads` becomes `null`, and `hasConfig()` correctly reports
`false`), but the `HeadInfo` *object itself* is always constructed. So `helper == null` can never be true.

This is confirmed by the sibling call in the same file, `harvest()` (lines 76-81), which calls the same
`helperFor` and correctly checks only `hasConfig()`, not nullity:

```java
private static void harvest(...) {
    HeadInfo helper = helperFor(mob);
    if (!helper.hasConfig()) {
        return; // shouldn't happen while hasEyes is true, but guard anyway
    }
```

(That `!helper.hasConfig()` check itself *is* legitimate, worth noting explicitly since it looks similar:
a mob's `hasGooglyEyes` flag is decided once at spawn and never re-evaluated, so a datapack `/reload` that
disables or removes that entity's config after spawn leaves a mob with `hasEyes()==true` but no usable
config — a real, reachable state, not a hypothetical one. Only the `helper == null` half of the
`onLivingDrops` check is the redundant part.)

---

## Boundary-crossing checks that are correctly guarded (not flagged)

For contrast, the following defensive patterns were reviewed and found to be legitimate, not over-guarding:

- All network packet `decode`/`handle` methods (`network/*.java`) validate wire data (NBT size quotas,
  registry lookups, sender null-checks via `PickerPermissions.creative`) — genuine boundary, correctly
  guarded.
- `EyeConfigReloadListener`, `PickerExportService`, `ServerConfig` validate datapack JSON / client-authored
  NBT / hand-editable TOML config respectively — genuine boundaries, correctly guarded.
- The reflection-based model resolvers (`CitadelResolver`, `LLibraryResolver`, `ChildMapResolver`,
  `ReflectedBoxResolver`) and GeckoLib/Exotic Birds compat classes wrap third-party mod internals in
  broad `catch (Throwable)` — legitimate, since those are genuinely unreliable external interfaces whose
  shape isn't guaranteed by any contract.
- `SpawnAllCommand`/`PickerExporter` wrap modded `EntityType#create()` calls in `try/catch` — legitimate,
  since a modded factory can throw or (per its own contract) return `null`.

No under-guarded boundary-crossing gaps were found during this pass — the network/disk/config surfaces
inspected all had matching validation.
