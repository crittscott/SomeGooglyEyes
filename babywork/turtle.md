Covered by AgeableListResolver (TurtleModel extends QuadrupedModel). Has a baby form, fixed by the
head/body wrap replay.

Minor unrelated caveat found while auditing: `TurtleModel.renderToBuffer` adds its own extra
`translate(0, -0.08, 0)` around the whole render while an adult turtle is actively carrying an egg
(`!young && hasEgg()`), on top of whatever `QuadrupedModel`/`AgeableListModel` already does. Not replicated
by our resolver — a small (0.08 block), situational, egg-only offset, not an age/baby issue. Not fixed;
noted for later if it turns out to matter visually.
