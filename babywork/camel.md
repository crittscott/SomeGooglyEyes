# Camel — FIXED (baby only)

`CamelModel` extends `HierarchicalModel<T>` directly (not `AgeableHierarchicalModel` — see `sniffer.md`
for the mob that does use that shared base). It hand-rolls its own baby wrap instead:

```java
public void renderToBuffer(...) {
   if (this.young) {
      pPoseStack.pushPose();
      pPoseStack.scale(0.45F, 0.45F, 0.45F);
      pPoseStack.translate(0.0F, 1.834375F, 0.0F);
      this.root().render(...);
      pPoseStack.popPose();
   } else {
      this.root().render(...);
   }
}
```

`HierarchicalResolver` always walks `root()` directly with no wrap (that's correct for the plain
`HierarchicalModel` default `renderToBuffer`, which every other Hierarchical mob in this audit relies on
unmodified). Camel overrides it to add this baby-only scale+translate, so `HierarchicalResolver` silently
reconstructs the adult transform for a baby camel — same bug class as the cow head wrap
(`AgeableListModel`), just reimplemented ad hoc on top of `HierarchicalModel` instead of going through
`AgeableHierarchicalModel`.

Single group here (unlike Llama's three or `AgeableListModel`'s two) — the whole `root()` tree gets one
scale+translate when young. A fix needs `HierarchicalResolver` (or a Camel-specific wrapper) to recognize
`CamelModel` specifically and replay this wrap, gated on `young` at apply time, the same reasoning as
`AgeableListResolver`'s existing fix (the model instance is shared across every camel, baby and adult, and
`resolve()` is cached per (model, token)).

## Fix landed

`HierarchicalResolver.youngWrap` (`client/render/resolver/HierarchicalResolver.java`) recognizes
`CamelModel` by `instanceof` and replays the literal constants above as a no-op when `!young`. No new AT
entries needed (the constants are inline literals, not fields). `camel.json`'s `attachPoint`s
(`root/body/head`, `root/body`) were already `HierarchicalResolver`'s own naming, so no token rename was
needed. Adult camels were already correctly positioned before this fix; worth an in-game check on a baby
camel to confirm the position still looks right now that it's properly scaled, but not expected to need
re-authoring.
