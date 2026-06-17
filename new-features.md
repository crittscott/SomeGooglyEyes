# Eye System Notes

## 0. TODO
Babies vs Adults

## 1. Config Helpers

### Mob preview / testing command

DONE

~~Add a command that generates every eye-compatible mob for testing.~~


## 2. Eyes as Items

### Harvesting eyes

Use diamond shears to remove or harvest eyes from a mob.

Behavior:

* Eye item drops at the mob’s position.

### Adding eyes to mobs

Eye potion can add eyes to an eyeless mob.

Rules:

* Player does not choose the eye placement.
* Placement is automatic/random.

### Eye-bearing heads

If an eyed mob drops its head, the dropped head should include the mob’s eyes.

Compatibility:

* Integrate with the Heads mod.

### Eye color

Eyes can be recolored with dye.

Possible methods:

* Apply dye directly to a mob.
* Recolor eyes in a crafting table.

### Eye effects

Redstone can make eyes glow.

Cobweb clears all eye effects.

Glowstone + diamond applied to eyes makes them give off light.

Spider eyes grant night vision.

Rules:

* Light and night vision are mutually exclusive.
* Spider eyes are the only case where more than two eyes are needed.

Spider eye layout:

* Two front eyes.
* Two top eyes.
* One eye on each side.
* Two back eyes.

## 3. Responses to Events

### Anger

Eyes may change color when the mob becomes angry.

### Villager upgrade

Villager eyes swirl during an upgrade.

### Villager damage reaction

Villager eyes temporarily grow larger when hit by a player.

### Staring

Add a stare behavior.

Behavior:

* Pupils move to the center.
* Pupils stop tracking movement.

### Blinking and winking

Possible idle animations:

* Blink.
* Wink.

## 4. Model Complexity

### Eye size

Eyes should generally match the size of the mob.

Oversized eyes:

* Too-large eyes can only fit on top of the head.

### Armor slot / visual behavior

Add eyes to an “armor” slot.

Behavior:

* Show a helmet-like image in the slot.
* Allow leggings as the actual item/equipment behavior.
* Possible Easter egg.

### Eyebrows

Consider eyebrow expressions.

Possible expressions:

* Surprised.
* Angry.
* Sad.
* Refusing trade / “not trading.”
* Spock eyebrow.
