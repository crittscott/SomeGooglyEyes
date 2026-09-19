# TODO

## 2. Bug: parent box scale is not canceled for reflected box families

Separate from the Uranus work; affects Citadel today and would affect Uranus identically.

**What happens.** `AdvancedModelBox.render` applies its own `translateAndRotate` (which ends in
`scale(scaleX, scaleY, scaleZ)`) and then, when `scaleChildren` is false - the default - multiplies
in the *inverse* scale before descending to its children, so a parent's scale does not reach them.
`ReflectedBoxResolver.BoxChain.apply` replays `translateAndRotate` for every box from the root down
and never cancels anything, so an eye attached below a scaled ancestor inherits a scale the real
render path discards.

**Where.** `ReflectedBoxResolver` (chain apply and `applyTransform`), for both the Citadel and Uranus
families. Verified present in both libraries' `AdvancedModelBox`.

**When it shows.** Only when an ancestor in the chain has scale != 1 *and* `scaleChildren` is false.
Concrete case in Ice and Fire CE: `HippogryphModel` sets `Head.setShouldScaleChildren(false)` with
`Head` scaled 1.5 for babies, so anything attached below `Head` is drawn 1.5x too large and displaced
along the chain. Cases that set `setShouldScaleChildren(true)` (Hippocampus, Amphithere babies) are
correct as-is, and a scaled *leaf* is also correct, since its own scale genuinely applies.

**Shape of the fix.** `applyTransform` needs to know whether a box is the last link in the chain: for
every non-terminal box whose `scaleChildren` is false, apply `translateAndRotate` and then the
inverse of `scaleX/scaleY/scaleZ` (clamped as the library does, `1/max(scale, 0.0001)`). That means
two more reflective handles per family (the three scale fields and the `scaleChildren` flag) and
passing a terminal flag through `BoxChain.apply`. Worth doing after Uranus lands, so both families
get it from one change.
