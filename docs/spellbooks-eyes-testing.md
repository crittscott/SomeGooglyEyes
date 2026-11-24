# Spellbooks summoned eyes

Manual client checks in MultiMC with NeoForge, Iron's Spells 'n Spellbooks 1.21.1-3.16.4, and its dependencies. These exercise private renderer delegation and client rendering, which the server GameTests cannot observe. Results have not been verified in game.

1. **Normal rendering.** Spawn a summoned skeleton and summoned zombie with `/sg spawn`. Enable the picker so its configured-eye preview bypasses natural eye rolls. After any rising animation finishes, both mobs show their configured eyes on their faces.
2. **Picker placement.** Choose each mob with `/sg choose`. Select its `head` part and move the draft eye. The coordinate axes and draft eye appear at the head and follow its rotation. Save and export a placement; reload it and confirm it appears at the same location.
3. **Rising animation.** Use Raise Dead to create each summon type. With eyes applied or the picker active, watch the eyes throughout the rising animation and afterward. Their placement remains attached to the face when rendering changes from GeckoLib to the internal vanilla renderer.
4. **Renderer reload.** Reload client resources with F3+T. Repeat the placement check on both mobs; eyes and axes remain visible without duplicate eyes.
5. **Other renderers.** Check a vanilla skeleton, vanilla zombie, and Spellbooks cultist. Their eye placement and picker axes retain their usual behavior.
