# Sniffer — FIXED (baby only)

`SnifferModel` extends `AgeableHierarchicalModel<T>`, the one other vanilla mob using that base class
(everything else Hierarchical extends `HierarchicalModel` directly). `AgeableHierarchicalModel` is a
proper shared abstraction for exactly this bug — the `AgeableListModel` of the Hierarchical world — but
`HierarchicalResolver` doesn't know about it, so it has the same gap:

```java
// AgeableHierarchicalModel.renderToBuffer
if (this.young) {
   pPoseStack.pushPose();
   pPoseStack.scale(this.youngScaleFactor, this.youngScaleFactor, this.youngScaleFactor);
   pPoseStack.translate(0.0F, this.bodyYOffset / 16.0F, 0.0F);
   this.root().render(...);
   pPoseStack.popPose();
} else {
   this.root().render(...);
}
```

Simpler than Camel's version (uniform scale, single group, and it's a named/reusable base class rather
than a copy-pasted override) but the effect on `HierarchicalResolver` is identical: babies get the adult
transform, offset by whatever `youngScaleFactor`/`bodyYOffset` Sniffer's constructor passes in.

Fix options, in order of preference:
1. Extend `HierarchicalResolver` itself to check `instanceof AgeableHierarchicalModel` and replay this
   wrap when `young` — this is the "proper" fix since it's a real shared base class, and would
   automatically cover any future vanilla or modded mob built on it, not just Sniffer.
2. A Sniffer-specific wrapper, if reading `youngScaleFactor`/`bodyYOffset` off `AgeableHierarchicalModel`
   turns out to need its own access-transformer entries and the general case doesn't seem worth it for a
   single mob.

Either way: gate the wrap on `young` at apply time, not resolve time (`Resolvers.ATTACHMENTS` caches
`resolve()` per (model, token), and the model instance is shared across every sniffer, baby and adult) —
same reasoning as the existing `AgeableListResolver` fix.

## Fix landed

Took option 1: `HierarchicalResolver.youngWrap` recognizes `AgeableHierarchicalModel` by `instanceof` and
replays `youngScaleFactor`/`bodyYOffset` as a no-op when `!young`, so any future model on this base class
is covered automatically. New AT entries were needed for both fields (private, no getters); `young` itself
needed none (already public). `sniffer.json`'s `attachPoint` (`root/bone/body/head`) was already
`HierarchicalResolver`'s own naming, so no token rename was needed. Adult sniffers were already correctly
positioned before this fix; worth an in-game check on a baby sniffer to confirm, but not expected to need
re-authoring.
