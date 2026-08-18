# Llama — FIXED (baby only)

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

## Fix landed

`RabbitLlamaResolver` (`client/render/resolver/RabbitLlamaResolver.java`) replays all three wraps as
no-ops when `!young`, matching the adult's wrap-free render exactly. `LlamaModel`'s 8 fields needed new
`accesstransformer.cfg` entries.

`llama.json`'s `attachPoint`s were renamed from the old positional `#0`/`#1` (`ChildMapResolver`'s naming)
to `head`/`body` (`RabbitLlamaResolver`'s canonical names) — required for the eyes to attach at all under
the new resolver. Unlike Rabbit, the eye **position** values were left as-is on purpose: adult llamas were
already correctly positioned before this fix (adult has no wrap), so the same numbers still apply to
adults post-fix, and now also correctly scale for babies for the first time — worth an in-game check on a
baby llama to confirm, but not expected to need re-authoring.
