# Code conventions

## Member ordering

Order members within a class/interface/record alphabetically, **to the extent the
Java language allows**. The exceptions below are the only places where another rule
overrides alphabetical — everything else sorts by name (case-insensitive).

This is an unreleased mod: there is no legacy wire format, save format, or public API
to preserve, so we order for readability and let the few real constraints win where
they apply.

### Member order within a type

```
1. static final constants          (alphabetical; dependency-order exception)
2. other static fields             (alphabetical; dependency-order exception)
3. instance fields                 (alphabetical; dependency-order exception)
4. static / instance init blocks   (placed by dependency, not alphabetical)
5. constructors                    (by arity, fewest parameters first)
6. nested enums                    (alphabetical)
7. nested records                  (alphabetical)
8. other nested classes/interfaces (alphabetical)
9. methods                         (alphabetical)
```

Within each group, sort alphabetically by name unless a rule below says otherwise.
Overloaded methods cluster naturally by shared name; order them by arity, then by
parameter type names.

### Rule: methods and nested types are always alphabetical

Java imposes no ordering on methods or nested type declarations, so these always sort
by name with no exceptions. This covers the bulk of any reordering.

### Exception: field initialization order

Fields (static **and** instance) initialize top-to-bottom. Alphabetical order breaks
when one field's initializer reads another field declared below it. The Java language
(JLS 8.3.3) splits this into two cases, and **constants are not blanket-exempt** from
either — the inlining of compile-time constants only matters for the second case.

- **Simple-name reference → compile error.** If a field's initializer reads a sibling
  field by its *simple name* (e.g. `... = create(MOD_ID)` referring to a `MOD_ID` field
  in the same class), that field must be declared *earlier*. A later declaration is a
  hard compile error (`illegal forward reference`) **even when the target is a `final`
  constant** — and even when the read is inside a lambda body (`() -> MOD_ID`). The one
  carve-out is a reference inside an *anonymous class* body, whose innermost enclosing
  class differs, so 8.3.3 does not apply there. Because the compiler rejects these, they
  can't ship silently; dependency order simply wins.
- **Qualified-name / cross-class reference → silent hazard.** A read through a qualified
  name (`ClassName.FIELD`) or from a different class is *not* restricted by 8.3.3, so it
  compiles. There, a compile-time constant is inlined and reads correctly regardless of
  order, but a non-constant object field silently yields `null`/`0` if it hasn't
  initialized yet.

When such a dependency exists, dependency order overrides alphabetical for *those fields
only*. Order the rest of the group alphabetically around them — in practice, a
"smallest name whose dependencies are already satisfied" (greedy topological) ordering.
Add a brief comment on any field that sits out of alphabetical order for this reason.

### Exception: enum constants and `ordinal()`

Within an enum, the constants are declared first (a language rule) and may be
alphabetized like anything else. But **do not serialize an enum by `ordinal()`** —
reordering the constants would silently change the format. Serialize by name or an
explicit, stable id instead, so the constants stay freely reorderable.

The same principle applies to any value whose meaning is derived from declaration order
(e.g. network packet discriminators): assign explicit, stable ids rather than deriving
them from order, so the source can be alphabetized without changing behavior.

### Exception: the main mod class header

The `@Mod` entry-point class (`SomeGoogly`) starts with its identity constants in a fixed,
**non-alphabetical** order:

```
MOD_ID
MOD_NAME
LOGGER
```

This is the conventional Forge idiom — `MOD_ID` first because `@Mod` and every registry
references it, `MOD_NAME` beside it, then `LOGGER`. Any other constants in the class follow
these three and are ordered normally (alphabetical, subject to the field-initialization
exception).

### Exception: deliberately grouped field blocks

Alphabetical is the default for fields, but a field block that is a **deliberate aligned
table** — comma-multi-declarations (`float prevX, x;`), `prev`/current animation-channel
pairs, or a run of one-per-line fields under explanatory section comments — may keep its
existing order when alphabetizing would scatter tightly-related declarations and orphan
their comments. Constants still precede non-constant fields (the group ordering holds); only
the order *within* the block is preserved. Used by e.g. `BehaviorInstance`, `GooglyTracker`,
and `PickerState`. When in doubt, alphabetize — reserve this for genuine tables.

### Notes

- Registration/wiring **statements** inside a method (e.g. the `DeferredRegister.register`,
  event-bus, and config calls in the `SomeGoogly` constructor) are not members and aren't
  alphabetized. They read top-to-bottom and are grouped by concern. Their call order is also
  behaviorally free: each merely subscribes a deferred registry/handler to a bus, and Forge
  fires the actual registration later in its own order (by registry type, event, and listener
  priority) — not in call order.
- Constructors are not sorted among methods; the constructor's name is the class name,
  which would drop it at an arbitrary alphabetical position. They get their own slot
  (group 5), ordered by arity.
- Non-constant fields (mutable statics, instance fields) are not "constants"; they fall
  into groups 2 and 3, still subject to the field-initialization exception.
