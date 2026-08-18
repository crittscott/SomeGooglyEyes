# Rabbit — reference case for the "custom EntityModel wrap" bug family

`RabbitModel` (`net.minecraft.client.model.RabbitModel`) extends `EntityModel<T>` directly. It is not a
`HierarchicalModel` and not an `AgeableListModel`/`QuadrupedModel`, so neither `HierarchicalResolver` nor
`AgeableListResolver` claims it. It falls through to `ChildMapResolver`, the reflection catch-all, which
names its 12 top-level `ModelPart` fields positionally in declaration order: `leftRearFoot`(0),
`rightRearFoot`(1), `leftHaunch`(2), `rightHaunch`(3), `body`(4), `leftFrontLeg`(5), `rightFrontLeg`(6),
`head`(7), `rightEar`(8), `leftEar`(9), `tail`(10), `nose`(11). The shipped `rabbit.json` attaches to
`#11` (`nose`), which does track head pitch/yaw the same as `head` does (`setupAnim` sets
`nose.xRot`/`nose.yRot` identically to `head`'s), so the eye does rotate with the head look direction —
the problem is *where* that rotation happens, not whether it happens.

## The bug

`RabbitModel.renderToBuffer` is fully hand-written (not inherited from `AgeableListModel` or
`HierarchicalModel`), and — unlike every other model checked in this audit — it wraps **every** render,
adult included, in its own scale/translate that lives entirely outside the part tree:

```java
if (this.young) {
   pPoseStack.pushPose();
   pPoseStack.scale(0.56666666F, 0.56666666F, 0.56666666F);
   pPoseStack.translate(0.0F, 1.375F, 0.125F);
   // head, leftEar, rightEar, nose render here
   pPoseStack.popPose();
   pPoseStack.pushPose();
   pPoseStack.scale(0.4F, 0.4F, 0.4F);
   pPoseStack.translate(0.0F, 2.25F, 0.0F);
   // legs, haunches, body, tail render here
   pPoseStack.popPose();
} else {
   pPoseStack.pushPose();
   pPoseStack.scale(0.6F, 0.6F, 0.6F);
   pPoseStack.translate(0.0F, 1.0F, 0.0F);
   // every part, head and body groups together, renders here
   pPoseStack.popPose();
}
```

`ChildMapResolver` (and `ModelPartTreeResolver`, which it's built on) has no concept of this wrap at all —
it just replays `nose.translateAndRotate(poseStack)` directly, in raw unscaled mesh-space, with no ancestor
scale or translate. This is the same *class* of bug as the cow baby issue (an ancestor transform applied
outside the captured part tree), but strictly worse:

- **It's not baby-only.** The adult branch has its own wrap (`scale(0.6) + translate(0,1,0)`) that's just
  as invisible to us as the baby ones. Adult rabbits are mispositioned too — confirmed in-game.
- **It's not a single resolver's problem.** `AgeableListResolver`'s fix (replaying the wrap for
  `AgeableListModel`) doesn't apply here since `RabbitModel` isn't in that family at all.

## What a fix needs

Something that recognizes `RabbitModel` specifically (by `instanceof`, the same way `AgeableListResolver`
recognizes `AgeableListModel`) and replays the correct wrap before descending to the resolved part,
choosing between the three wraps (`young` head-group, `young` body-group, adult) based on which group the
resolved root falls in and the model's current `young` state — checked at *apply* time, not resolve time,
for the same reason `AgeableListResolver` does (the model instance is a shared singleton across every
rabbit, baby and adult, and `Resolvers.ATTACHMENTS` caches the resolved chain per (model, token)).

Because `RabbitModel`'s three wraps split parts into different groups than `AgeableListModel`'s
head/body split (baby: `{head, leftEar, rightEar, nose}` vs `{legs, haunches, body, tail}`; adult: all
parts together), this can't reuse `AgeableListResolver`'s `headWrap`/`bodyWrap` machinery as-is — it needs
its own resolver (or its own root-grouping logic) that knows which of Rabbit's fields fall in which group
for which wrap.

## Other mobs with this same "custom `EntityModel` with a hand-rolled wrap" shape

Found during this audit (see `vanilla-mobs.md`): `LlamaModel` (`llama`/`trader_llama` — baby-only wrap,
adults are wrap-free like `AgeableListModel`'s adults), `CamelModel` (`camel` — baby-only wrap, but this
one *does* extend `HierarchicalModel`, so it's a `HierarchicalResolver` gap, not a `ChildMapResolver` one),
and `SnifferModel` via `AgeableHierarchicalModel` (`sniffer` — same `HierarchicalResolver` gap as Camel,
but through a proper shared base class instead of Camel's hand-rolled copy). Each needs its own targeted
fix; see their respective files.
