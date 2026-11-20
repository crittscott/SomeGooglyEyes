# Client test plan (draft)

What the Fabric client tests should check, and what each one does. Every test is judged by screenshots in the review app. Sources: `orientation-player.md` and the four manual checklists (`framed-eyes-testing.md`, `inventory-eyes-testing.md`, `boat-eyes-testing.md`, `third-person-eyes-testing.md`). Per-mob layout auditing is out of scope; `/sg spawnall` covers it.

Tags: **[DS]** needs a dedicated server. **[File]** the result is a file, not something on screen; a screenshot can show only its in-game consequence. **[Launch]** needs a client setting in place before the game starts. **[Mod]** needs another mod in the test run. Tests needing a second player or a protection mod are out of scope.

## Common setup

Unless a test says otherwise:

- **World:** a fresh superflat creative world with cheats on; noon, clear weather, daylight and weather cycles off, mob spawning off. Each test uses its own area so earlier tests leave nothing in view.
- **Camera:** placed with `/tp` to an exact position and rotation. The HUD is hidden for world shots and shown when the test is about the HUD, hotbar, chat, or a screen.
- **Subtitles:** on, so a sound shows up in a screenshot as its subtitle.
- **Eyes:** given with `/sg admin eyes true`, colored with `/sg admin tint`, unless applying or harvesting is what the test checks; then a Slimy Eye or shears are used as a player would.
- **Timing:** ticks are frozen with `/tick freeze` and advanced with `/tick step`, so each screenshot falls at a known moment. Ambient expressions are off unless the test is about them; a specific expression is forced with `/sg admin behavior <id>` or a single-expression pool with a short interval.
- **Server settings:** written to `somegoogly-server.toml`, then applied with `/reload`; restored after the test.
- **Mobs:** summoned with `NoAI` when they should hold still; with AI when the test needs their behavior.
- **Test datapack:** the tests carry their own datapack for definitions vanilla doesn't have (paired boat eyes, a one-eye layout, alternate cow layouts, a stick in `c:tools/shear`).

## 1. Eyes on carriers

### 1.1 Mobs
- **1.1.1** An eyed adult mob shows its configured eyes, iris/cornea colors, and glow.
  - A still cow gets eyes with a red iris. Shots from the front and a three-quarter angle: eyes sit where the cow definition puts them, with red irises.
- **1.1.2** A baby with a baby definition shows the baby layout; a baby without one shows none.
  - With spawn chance 100, summon an adult and baby of a species with a baby definition, and a baby of one without. Shot of all three: adult and baby layouts differ; the third baby has no eyes.
- **1.1.3** A baby without a baby definition may gain eyes on growing up.
  - With spawn chance 100, summon a baby of the species from 1.1.2 set to grow up in a few ticks. Shots before and after: no eyes, then adult eyes.
- **1.1.4** A weighted-variant mob keeps the same variant across save/reload.
  - Give eyes to a mob whose definition has several variants. Shot; save and quit, reopen, same camera, shot: same arrangement both times.
- **1.1.5** A sheared sheep's body eyes sit on the bare body and return to the wool when it regrows.
  - An eyed sheep on grass. Shot; shear it with plain shears; shot; let it eat grass until the wool regrows; shot. Eyes on the wool, then on the bare body, then on the wool again.
- **1.1.6** Glowing eyes stay visible in darkness; non-glowing eyes darken.
  - Two eyed cows in a closed unlit room at midnight, one with glow on and one off. Shot: one cow's eyes bright, the other's dark like the cow.
- **1.1.7** An invisible mob shows no eyes.
  - An eyed cow given invisibility. Shot: no cow and no eyes.
- **1.1.8** A GeckoLib-rendered mob shows eyes. [Mod]
  - Summon an eyed mob from a GeckoLib-based mod. Shot: eyes placed on its model.

### 1.2 Players
- **1.2.1** Own eyes visible in both F5 views, not in first person.
  - Apply a Slimy Eye to yourself. Shots in first person, F5 back, and F5 front: no eyes in first person; eyes on the face from the front.

### 1.3 Placed heads
- **1.3.1** Each eligible vanilla head shows eyes laid out and colored by its mob's definition.
  - Place skeleton, wither skeleton, zombie, creeper, piglin, and player heads on the floor and on a wall; apply a Slimy Eye to each. Shot of each row from the front: each head has its mob's eye layout.
- **1.3.2** A dragon head never gets eyes.
  - Place a dragon head; use a Slimy Eye on it in survival. Shot with hotbar: no eyes, item count unchanged.
- **1.3.3** A worn head shows no eyes.
  - Put an eyed head item on your own head. F5 front shot: the head is worn and has no eyes.

### 1.4 Boats
- **1.4.1** Every boat variant starts eyeless, even with spawn chance at 100.
  - With spawn chance 100, place every wood's boat and chest boat plus the bamboo raft and chest raft in a row on water. Shot: none has eyes.
- **1.4.2** An eyed boat shows two outward eyes near the front, one per side, on every hull and cargo variant, occupied and empty.
  - Apply a Slimy Eye to each boat from 1.4.1. Shots from each side and from the front, empty; then with a villager seated in each, again. Two eyes on every hull, near the front, one per side, facing out.
- **1.4.3** Eyes don't clip or gap against the hull; paddles, passengers, chest, and water masking look normal.
  - Close-ups of the eye area of an oak chest boat and a bamboo raft with a passenger, on water. Eyes flush with the hull; paddles, passenger, and chest normal; no water inside the hull.
- **1.4.4** Eyed boats cull at the same distance and screen edge as eyeless ones.
  - An eyed and an eyeless boat side by side. Shots stepping the camera away to render distance, and turning so the boats sit at the screen edge: both vanish at the same point.

## 2. Pupil physics

- **2.1** Mob pupils settle toward world down, lag on movement and turning, and respond to head motion.
  - A still eyed cow. Shot at rest; push it sideways and shoot a few ticks later; turn its head and shoot. Pupils at the bottom, then trailing the motion, then swinging with the turn.
- **2.2** Camera pitch or F5 view alone never moves the gravity direction.
  - A frozen scene with an eyed cow, boat, framed eye, and your own held eye. Shots looking level, up, down, and in F5: pupils point the same way relative to the world in every shot.
- **2.3** Boat pupils stay attached and correct while rowing, turning, rocking, and in bubble columns.
  - Ride an eyed boat. Shots while rowing forward, rowing back, turning each way, just after hitting the hull, and in an up and a down bubble column. Eyes stay on the hull; pupils hang toward world down and lag changes in motion.
- **2.4** Framed loose eyes settle by placement and rotation.
  - Frames on each wall direction, the floor, and the ceiling, each with an eye at every rotation. One shot per surface after settling: wall pupils at the bottom whatever the rotation; floor and ceiling pupils vary with rotation.
- **2.5** Third-person hand eyes settle and lag.
  - Hold eyes in both hands, F5 front and back. Shots while still, swinging, sneaking, walking, swimming, and with the main arm switched to left: pupils toward world down, lagging motion; an eye held flat shows no consistent pupil direction.
- **2.6** GUI pupil down follows the icon's screen pose.
  - Inventory open with eyes in several slots. Shots at GUI scale 2, after changing to scale 4, and after resizing the window: pupils at the bottom of each icon every time.
- **2.7** First-person held loose eye reacts to movement.
  - Hold an eye in first person. Shot at rest; walk forward and stop; shot just after stopping: pupil at rest, then swung forward.
- **2.8** Placed head eyes settle under gravity.
  - Eyed heads on the floor facing each direction and on walls. Shot: pupils settled toward world down on every head.

## 3. Ambient expressions

- **3.1** Each pool expression plays on a mob.
  - An eyed cow. For each of blink, cross-eye, side-eye, and stare: set the pool to that alone with a short fixed interval, step to the middle of the first expression, shoot. Each shot shows that expression.
- **3.2** Placed heads play synchronized idle expressions; paired layouts cross-eye.
  - Eyed heads with a paired layout. Blink-only pool: shot mid-blink, both eyes of each head closing together. Cross-eye-only pool: shot, paired eyes crossing.
- **3.3** Loose eyes never cross-eye.
  - Cross-eye-only pool, short interval. Loose eyes in a frame, the hotbar, and both hands (F5). Shots across several intervals: no loose eye ever crosses.
- **3.4** Identical copies animate independently.
  - Blink-only pool with a random interval. Identical eyes in four neighboring frames, four hotbar slots, both hands, and two eyed boats. Several shots: in at least one, copies are not all in the same state.
- **3.5** Bundled boat eyes never cross; a paired layout does; a one-eye layout doesn't.
  - Cross-eye-only pool. Three eyed boats: bundled layout, test-datapack paired layout, test-datapack one-eye layout. Shots over several intervals: only the paired boat crosses.
- **3.6** With an interval shorter than the longest expression, playback doesn't overlap and expressions release their overlays.
  - Interval shorter than the longest expression; an eyed cow and a framed eye. A run of shots across three intervals: each shows one expression or none, and eyes return to normal between them.
- **3.7** After eviction, the current expression is reconstructed without replaying a backlog.
  - An eyed cow, framed eye, and eyed boat with a short-interval pool. Turn away long enough for eviction, turn back, shoot on three consecutive ticks: eyes in a plausible current state, not flicking through several expressions.
- **3.8** Pausing freezes world animation; GUI icons keep animating.
  - Blink-only pool. An eyed cow, framed eye, eyed boat, and held eye in F5. Pause mid-blink; shots at the pause and after waiting: world eyes identical in both. Shots of a pausing screen showing an eye item: its icon changes.

## 4. Triggered expressions

Mobs keep their AI where the behavior needs it.

- **4.1** Player damage causes a bulge; non-player damage doesn't.
  - Hit chance 100. Punch one eyed cow; damage another with `/damage` from a non-player source. Shot mid-reaction: the first bulging, the second not.
- **4.2** Milking a cow, mooshroom, and goat causes a bulge.
  - Use a bucket on each eyed animal. Shot after each: that animal bulging.
- **4.3** Shearing a sheep causes a bulge.
  - Shear an eyed sheep. Shot: bulging.
- **4.4** Love pupils on each affection event.
  - Eyed animals: feed two cows wheat; give two villagers beds and food until they breed; leave an iron golem near a baby villager until it offers a flower; feed an ocelot fish until it trusts; feed a wolf bones until tamed. Shot at each event: pupils twice normal size.
- **4.5** Swirl after a villager trade and a wandering-trader trade.
  - Eyed villager and wandering trader each with a fixed offer. Complete a trade with each; shot after closing the screen: swirling.
- **4.6** Swirl on heal; a second heal within the cooldown doesn't swirl.
  - Damage an eyed cow, heal it with instant health; shot: swirl. Heal again within the cooldown; shot: no swirl.
- **4.7** Sleeping: villagers lower tan lids, other sleepers squint, nothing else plays.
  - At night, an eyed villager asleep in a bed; by day, an eyed fox asleep. Shots: villager with tan lids, fox squinting. Force a blink on the fox; shot: no blink.
- **4.8** A ramming goat squints.
  - An eyed goat with a nearby target. Step until it charges; shot mid-charge: squinting.
- **4.9** Bow aim closes the eye away from the bow arm.
  - In survival with resistance, let an eyed skeleton draw on you; shot mid-draw. Give yourself eyes and draw a bow in F5 front, right-handed and then left-handed; shots. In each, the eye away from the bow arm is closed.
- **4.10** A swelling creeper's pupils rattle faster toward the blast.
  - Ignite an eyed creeper. Shots on consecutive ticks early and late in the fuse: pupils jumping more each tick late than early.
- **4.11** Angry or attacking mobs' pupils turn red, and return when calm.
  - In survival, an eyed zombie targeting you; an eyed wolf you hit. Shots: red pupils. Switch to creative and wait for them to lose interest; shots: normal pupils.
- **4.12** A screaming enderman's pupils throb.
  - In survival, look at an eyed enderman. Shots on consecutive ticks while it screams: pupil size changing.
- **4.13** Only one expression at a time; squint, wink, rattle, red, and throb layer over it.
  - Force a blink on a swelling creeper and a stare on an aiming skeleton; shots: both effects visible together. Force two expressions on a cow back to back; shot: only one showing.
- **4.14** Boats get no hurt, heal, anger, or sleep reactions.
  - Hit an eyed boat; shot: no bulge.
- **4.15** First-person held loose eye reacts to hurt and heal.
  - Hold an eye in first person. Take damage; shot mid-reaction. Heal with instant health; shot mid-reaction.

## 5. Getting eyes

- **5.1** A direct shears kill drops one eye and costs one durability; an indirect kill doesn't; nothing drops with `doMobLoot` off.
  - Harvest chance 100, survival. Kill an eyed cow (lowered to 1 health) with shears; shot with hotbar: eye on the ground, durability bar down one. Kill another by other means; shot: no eye. Set `doMobLoot` off and kill a third with shears; shot: no eye.
- **5.2** A `c:tools/shear` item works like shears.
  - With the test datapack putting a stick in `c:tools/shear`, kill an eyed cow with the stick. Shot: eye on the ground.
- **5.3** A harvested eye keeps effective colors and glow.
  - Harvest from a cow with a blue iris and glow on. Put the eye in a frame in a dark room. Shot: blue, glowing eye.
- **5.4** Optometrist on an eyed mob: eyes gone, mob unharmed, one eye drops, one durability, shearing sound.
  - Survival, Optometrist shears, an eyed cow. Shots before and after use, with HUD: cow without eyes and no damage flash, one eye on the ground, durability down one, shearing subtitle.
- **5.5** Plain shears keep their usual interactions.
  - Use plain shears on an eyed sheep and an eyed boat. Shots: sheep sheared, eyes kept; you seated in the boat, eyes kept.
- **5.6** Optometrist on an eyed placed head pops one eye.
  - Shot: head eyeless, one eye on the ground.
- **5.7** Optometrist on an eyed boat, either hand: eyes gone, one eye drops, one durability, no boarding or cargo opening.
  - Main hand on an eyed boat, then offhand (sneaking, empty main hand) on an eyed chest boat. Shots: eyes gone, an eye on the ground, durability down, not seated, no chest screen.
- **5.8** Removing your own eyes: Optometrist cleanly; plain shears cost a hit.
  - Survival, PvP off, eyes on yourself. Sneak-use Optometrist on air; F5 shot with HUD: eyes gone, health unchanged. Re-apply; repeat with plain shears: eyes gone, health down.
- **5.9** Denied attempts change nothing.
  - Use Optometrist on an eyed cow in spectator; on an eyed head in adventure mode; on an eyed cow just beyond reach. Shots with HUD: eyes, counts, durability, and ground unchanged.

## 6. Applying eyes

- **6.1** A Slimy Eye on an eyeless eligible mob gives its eyes the item's appearance, squelches, and consumes one item in survival, none in creative.
  - Survival, a green-iris Slimy Eye on an eyeless cow. Shot with HUD: green eyes, squelch subtitle, count down one. Creative, another cow: count unchanged.
- **6.2** An eyed mob, an ineligible mob, and an unsupported life stage refuse it.
  - Survival, use a Slimy Eye on an eyed cow, a mob with no definition, and a baby of a species without a baby definition. Shots with HUD: nothing changes, count unchanged.
- **6.3** Sneak-use on air applies it to yourself.
  - F5 front shot: eyes on your face.
- **6.4** Applying to an eyeless placed head works with build permission and is refused without it.
  - Survival on one head: eyes appear. Adventure mode on another: refused, count unchanged. Shot of both.
- **6.5** Applying to each boat variant, either hand, with any appearance.
  - Ordinary eye on an oak boat (main hand), dyed eye on a chest boat (offhand, sneaking), glowing eye on a raft, glow-off eye on a chest raft. Shots: correct appearance on each, not seated, no chest screen. A Slimy Eye on an already eyed boat: shot, count unchanged.
- **6.6** Spectators can't apply.
  - Shot: eyeless cow unchanged.

## 7. Crafting

Results are shown in the crafting grid and, where glow matters, placed in a frame in a dark room.

- **7.1** Dye sets iris color.
  - Craft an eye with red dye. Shot of the result: red iris.
- **7.2** Glowstone forces glow on; redstone forces it off; cobweb clears all overrides.
  - Craft from a glowing red eye: with redstone, with cobweb; from a plain eye: with glowstone. Frame shots in the dark: red not glowing; plain not glowing; plain glowing.
- **7.3** Modifiers stack.
  - Red dye, then glowstone. Frame shot in the dark: red and glowing.
- **7.4** Slime keeps appearance and custom name.
  - Name a red eye in an anvil, craft with a slimeball. Shot of the tooltip and icon: Slimy Eye with the name and red iris.
- **7.5** The Slimy Eye recipe unlocks on obtaining a Googly Eye.
  - New survival world. Recipe book shots before and after picking up an eye: locked, then listed.
- **7.6** Round trip keeps appearance.
  - Harvest from a red-iris cow; add glowstone; slime it; apply to a new cow; harvest again. Shots of each cow and item: red and, after glowstone, glowing throughout.
- **7.7** The creative tab contents; creative eyes have no overrides.
  - Shot of the tab: Googly Eye, Slimy Eye, Optometrist book. Frame a creative eye; shot: default appearance.

## 8. Eye items on display

### 8.1 Item frames
- **8.1.1** Loose framed eyes: 3x diameter, normal thickness, colors, and lighting; glow stays during expressions.
  - White, dyed, and glowing eyes in regular and glow frames, at noon and at midnight, with a blink-only pool. Shots of each set, including one mid-blink: large eyes, colors right, glow kept while blinking.
- **8.1.2** An invisible frame's eye keeps animating with no frame visible.
  - Make occupied frames on a wall, floor, and ceiling invisible. Shots mid-blink: eyes visible and blinking, no wood or backing.
- **8.1.3** Swapping contents leaves nothing behind.
  - In one frame: eye, then a different-colored eye, Slimy Eye, diamond, nothing, then the frame broken and replaced with the first eye. Shot after each: only the current item, in its own appearance.
- **8.1.4** Eyed head items in frames, every orientation.
  - Each eligible head item in frames on walls, floor, and ceiling at several rotations, with a blink then cross-eye pool. Shots: eyes in configured places at normal size, right colors, settled, blinking, paired crossing.
- **8.1.5** Replacing a framed head leaves no prior layout.
  - Replace a framed eyed head with another species, another appearance, a plain head, and a loose eye. Shot after each: only the current item's eyes.

### 8.2 Inventory and GUI
- **8.2.1** Copies in hotbar, inventory, chest, and creative slots animate independently.
  - Random-interval blink pool. Identical eyes in hotbar, inventory, and an open chest; then in the creative menu. Several shots each: copies not all in step.
- **8.2.2** Moving stacks doesn't restart icons on count changes; appearance changes show at once; the cursor item keeps its own expression.
  - With a blink pool, move, split, merge, shift-click, and drag eye stacks; swap one for a different color. Shot after each action: icons continue mid-expression, the swapped icon shows the new color, the carried item has its own state.
- **8.2.3** Icons keep normal size and vanilla overlays.
  - A stack of 64 eyes and single eyes in the hotbar, one selected, one hovered, mid-blink. Shot: normal icon size, count, tooltip, highlight, selected slot, nothing outside the icon.
- **8.2.4** Changing views leaks no layout or colors.
  - Scroll creative entries away and back; close a chest and open another; disconnect and rejoin; open a new world. Shot after each: icons correct for their items.
- **8.2.5** Other items render normally.
  - Plain heads, Slimy Eyes, and a diamond in slots. Shot: ordinary icons.
- **8.2.6** A broken eyed head's item tooltip lists its eyes.
  - Break an eyed head in survival, hover its item. Shot: tooltip lists the eyes.

### 8.3 Hands
- **8.3.1** An equipped mob animates its held eye items on its own.
  - A still zombie holding an eye in its main hand and an eyed head in its offhand, blink pool. Shots: both animate, not in step.
- **8.3.2** Hand changes leave no stale layout.
  - In F5, swap hands, change slot, change count, swap for another color, empty the hands, walk out of render range and back, and reconnect. Shot after each: hands show only current items in their own appearance.
- **8.3.3** First-person head items keep resting pupils.
  - Hold an eyed head in first person and walk. Shot: centered, still pupils.

### 8.4 Ground and worn
- **8.4.1** Dropped eyes and heads keep centered, static pupils.
  - Drop an eye and an eyed head. Shots several ticks apart: pupils centered and unchanged.

### 8.5 Context isolation
- **8.5.1** Each display context stays independent.
  - Matching eyes in a frame, the hotbar, both hands, and on the ground, blink pool. Toggle first/third person several times. Shots in each view: each copy in its own state; nothing in the world mirrors a GUI icon.

## 9. Target check

Sneak and aim with an eye item; shots show the reported verdict.

- **9.1** Each verdict.
  - With a Googly Eye at an eyeless cow (can receive), an eyed cow (has eyes), a baby whose species has only an adult definition (other life stage), and a mob with no definition (no configuration). Repeat one with a Slimy Eye.
- **9.2** Boats and heads.
  - An eyeless boat and an eyed head.
- **9.3** No checks through walls or past reach.
  - A cow behind a wall, and a cow just past reach: no verdict for either.
- **9.4** Client hide settings don't change the verdict. [Launch]
  - In a run started with `disableGooglyEyes` on, check an eyed cow: "has eyes".
- **9.5** Disabling the definition and reloading changes the verdict.
  - Check an eyeless cow; enable a test datapack that disables the cow definition and reload; check again: "can receive", then "no configuration".

## 10. Spawning and lifecycle

- **10.1** `globalPercent` 100 gives every eligible spawn eyes; 0 gives none.
  - Summon five cows at 100, five at 0. Shot: all eyed, none eyed.
- **10.2** `entityOverrides`: exact ID beats a wildcard; otherwise the first matching wildcard.
  - Global 0, overrides `minecraft:cow,100`, `minecraft:c*,0`, `minecraft:*,100`. Summon a cow, chicken, and pig. Shot: cow and pig eyed, chicken not.
- **10.4** Players never get eyes naturally, and lose them on death.
  - At 100, join a new world; F5 shot: no eyes. Give yourself eyes, die, respawn; F5 shot: no eyes.
- **10.5** Conversions keep eyes.
  - An eyed zombie villager cured; an eyed villager struck by lightning; an eyed zombie left underwater until it converts. Shots after each: villager, witch, and drowned with eyes.
- **10.6** A player leaving the End keeps eyes.
  - With eyes on, go to the End and leave through an exit portal, skipping the credits. F5 shot: eyes still on.
- **10.7** Save, quit, reopen, chunk reload, and reconnect keep eyes and appearance.
  - A scene with a tinted eyed cow, yourself eyed, an eyed head, an eyed boat, a framed eye, and eyes in the hotbar. Shot; save and quit, reopen; shot; teleport far away and back; shot. All three identical in eyes and colors.
- **10.8** Head items restore eyes; an eyed mob's own head keeps them.
  - Break an eyed head in survival and place it again; shot: eyes back. A charged creeper kills an eyed zombie; place the dropped head; shot: eyes on it.
- **10.9** A broken eyed boat places an eyeless boat, with no extra eye.
  - Break an eyed boat, chest boat, and raft in survival, and one in creative. Shots of the drops: boat items and cargo only. Place each; shot: eyeless.
- **10.10** Changing worlds or dimensions leaks no eye state.
  - With an eyed cow, framed eye, eyed boat, and held eyes in view, go to the Nether and back; then quit and open a different world at the same coordinates. Shots: correct eyes after returning; none in the new world.
- **10.11** A 1.21.4 world opens with eyed mobs, items, and settings intact.
  - Open a world saved by the 1.21.4 release, kept with the tests, containing a tinted eyed mob, eye items, and non-default settings. Shots: same eyes and colors, items intact, settings in effect.

## 11. Multiplayer

- **11.1** A joining client, or one entering tracking range, sees saved eyes and appearance. [DS]
  - On a dedicated server, apply a tinted Slimy Eye to a cow; disconnect and reconnect; shot. Walk out of tracking range and back; shot. Eyes and colors right both times.
- **11.2** A `googlyEyesEnabled` change reaches connected clients immediately. [DS]
  - While watching an eyed cow, the server turns the master switch off and back on. Shots: eyes gone, then back, without reconnecting.
- **11.3** Multiplayer menus don't pause the server. [DS]
  - Watch a walking cow, open the pause menu for a few seconds, close it. Shots before and after: the cow has moved.

## 12. Configuration

### 12.1 Server
- **12.1.1** Master switch off stops everything; saved eyes return.
  - Master switch off. Shots: eyed mob, head, and boat without eyes; framed and hotbar eyes with centered static pupils. Try a Slimy Eye, a shears kill, a hit at 100 percent, and an angry zombie; shots: none take effect. Switch on; shot: eyes back as before.
- **12.1.2** Each behavior switch off disables only its behavior.
  - For each switch, turn it off and repeat that behavior's 4.x trigger. Shot: no reaction. Repeat one other behavior in the same state; shot: it still works.
- **12.1.3** Ambient off or an empty pool stops idle expressions, not physics.
  - Ambient off with a short interval: shots across several intervals of an eyed cow, framed eye, and head show no expressions; push the cow; shot: pupils lag. Repeat with an empty pool.
- **12.1.4** A cross-eye-only pool animates paired heads, not loose eyes; blink resumes both.
  - Cross-eye-only pool: shots of a paired head crossing and a framed loose eye not. Blink-only pool: shots of both blinking.
- **12.1.5** Interval, hit, harvest, and cooldown settings take effect.
  - Interval fixed at 40 ticks: shots 40 ticks apart, each mid-expression. Hit chance 0: punch; shot, no bulge. Harvest chance 0: shears kill; shot, no eye. Heal cooldown 20: two heals 30 ticks apart; shots, both swirl.
- **12.1.6** Fabric applies edits at world start and on `/reload`.
  - Edit a setting while playing; shot showing the old behavior; `/reload`; shot showing the new. Edit, quit, reopen; shot showing the new.
- **12.1.7** Fabric keeps a malformed TOML file and fills in missing keys. [File]
  - Start a world with a malformed file, then with one missing keys. Shots: the world loads with defaults in effect for the malformed file and with existing values kept for the partial one. The `.bak` and the filled-in file need a file check.

### 12.2 Client
- **12.2.1** `disableGooglyEyes` hides all eyes; loose items go static. [Launch]
  - Shots of the 10.7 scene: no eyes on mobs, heads, or boats; loose items with centered static pupils.
- **12.2.2** `disabledEntities` hides listed mobs, their heads, and boats by per-wood ID. [Launch]
  - Run with `minecraft:zombie` and `minecraft:oak_boat` listed. Shot: eyed zombie and zombie head without eyes, eyed oak boat without eyes; eyed cow and birch boat with eyes.
- **12.2.3** `disabledMods` hides a namespace's mobs and heads. [Launch]
  - Run with `minecraft` listed. Shot: no vanilla mob or head shows eyes.

## 13. Reload

- **13.1** A changed definition on `/reload` changes loaded eyes on the client.
  - An eyed cow, eyed head, and eyed boat in view. Swap the test datapack to alternate layouts and reload. Shots before and after: new positions, no trace of the old ones.
- **13.2** Disabling a definition hides those eyes; re-enabling brings them back.
  - Disable the cow definition and reload; shot: no eyes. Re-enable and reload; shot: same eyes and colors as before.
- **13.3** A resource reload keeps all eyes rendering.
  - The 10.7 scene, F3+T. Shots before and after: identical.

## 14. Picker and `/sg` commands

- **14.1** Non-operators and non-creative players are refused.
  - In a world without cheats, and in survival with cheats: run a `/sg` command and press `K`. Shots of chat: refused each time.
- **14.2** `K` toggles the picker; configured mobs show eyes regardless of spawn chance; HUD and gizmo show state.
  - Spawn chance 0, an eyeless cow. Press `K`; shot with HUD: cow shows eyes, picker HUD shown. Press `K`; shot: picker off, no eyes.
- **14.3** Choosing freezes a mob; releasing or disconnecting restores its AI.
  - Choose a walking cow with `V`; shots 100 ticks apart: it hasn't moved; HUD and gizmo shown. Release; shots: it moves. Choose with `/sg choose`, disconnect, rejoin; shots: it moves.
- **14.4** Part selection.
  - Press `]` twice, `[` once, then `/sg part` and `/sg list parts`. Shot after each: HUD and chat show the selected part.
- **14.5** Creating and editing an eye.
  - `/sg create`, then `move`, `rot`, `posrot` with `~` in one component, and `properties` setting colors and glow. Shot after each: the preview eye changes as commanded, with centered pupils.
- **14.6** Saving and managing eyes; unsaved edits discarded.
  - Save, dupe, select, delete, list eyes; shot after each. Edit an eye, select another without saving, return; shot: edit gone.
- **14.7** Variant management.
  - Create a second variant, switch between them, set a weight, delete one. Shot after each: HUD and chat show the change.
- **14.8** Adult and baby edits coexist.
  - Edit and save an adult cow, then a baby cow. Shot of both: each with its own edit.
- **14.9** `/sg export` writes and reloads; repeats and oversized definitions are refused. [File]
  - Export; shot: other cows now show the edited layout. Export again at once; shot: refusal in chat. Build a definition too large and export; shot: refusal.
- **14.10** `/sg exportall` writes files and leaves the world unchanged. [File]
  - Shots before and after: same world, chat reports the output folder.
- **14.11** `/sg spawn`, its suggestions, and mob moves.
  - With `minecraft:pig` excluded: shot of the suggestion list without pig. Spawn a cow; shot: frozen cow at the target. `/sg mob move` and `rot`; shots: moved and turned.
- **14.12** `/sg spawnall` gating and its skip report.
  - With `allowSpawnAll` off: shot of the refusal. On, in a disposable area, `/sg spawnall minecraft`: shots of the grid and the skip report.
- **14.13** Admin commands on the aimed mob.
  - On a cow: eyes on and off, tint iris and cornea, tint clear, glow on, off, and config, behavior by id and random. Shot after each.
- **14.14** Rebound picker keys work.
  - Rebind the toggle key from `K` to `J` in Controls; press `J`. Shot: picker on.

## 15. Other mods

Not implemented in the Fabric 1.21.8 tests: this checkout supports neither the Heads mod nor Twilight Forest on Fabric. 1.1.8 runs only when the launch names a GeckoLib-rendered mob with `-Dsomegoogly.clientgametest.geckolibMob=<entity id>` and its mod is in the run.

- **15.1** Heads-mod heads get eyes where this checkout supports them. [Mod]
  - Place supported Heads-mod heads and apply Slimy Eyes. Shot: eyes on each.
- **15.2** Twilight Forest mobs and heads, where supported. [Mod]
  - Summon eyed Twilight Forest mobs and place their heads with eyes. Shots: eyes on each.
