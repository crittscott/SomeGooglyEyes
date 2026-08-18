# Llama — NOT COVERED (baby only)

`LlamaModel` extends `EntityModel<T>` directly, same as `RabbitModel` (see `rabbit.md` for the general
shape of this bug family). Used by both `llama` and `trader_llama` (`TraderLlama extends Llama`, no model
override) — see `trader_llama.md`.

Unlike Rabbit, Llama's **adult** render path has no extra wrap at all — it just renders every part
directly, so adult llamas are correctly positioned by `ChildMapResolver` today. The bug is baby-only, but
it's a three-way split (not head/body like `AgeableListModel`):

```java
public void renderToBuffer(...) {
   if (this.young) {
      pPoseStack.pushPose();
      pPoseStack.scale(0.71428573F, 0.64935064F, 0.7936508F);
      pPoseStack.translate(0.0F, 1.3125F, 0.22F);
      this.head.render(...);                                    // head only
      pPoseStack.popPose();
      pPoseStack.pushPose();
      pPoseStack.scale(0.625F, 0.45454544F, 0.45454544F);
      pPoseStack.translate(0.0F, 2.0625F, 0.0F);
      this.body.render(...);                                    // body only
      pPoseStack.popPose();
      pPoseStack.pushPose();
      pPoseStack.scale(0.45454544F, 0.41322312F, 0.45454544F);
      pPoseStack.translate(0.0F, 2.0625F, 0.0F);
      // legs + chest group
      pPoseStack.popPose();
   } else {
      // every part, no wrap
   }
}
```

Three independent (non-uniform — X/Y/Z scale differ) wraps, one per group (head alone; body alone; the
four legs + two chest boxes together), each only active when `young`. A fix needs a Llama-specific
resolver (or grouping logic) analogous to `AgeableListResolver.headWrap`/`bodyWrap`, but with three groups
instead of two, non-uniform scale, and gated on `young` at apply time the same way.
