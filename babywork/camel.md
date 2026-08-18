# Camel — NOT COVERED (baby only)

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
