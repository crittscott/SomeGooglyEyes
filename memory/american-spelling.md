---
name: american-spelling
description: User wants American spelling in code (color, not colour)
metadata:
  type: feedback
---

In this repo, use American spelling in Java identifiers and comments: `color`, not `colour`. The user flagged the British `getCorneaColours`/`irisColours` style and asked for `color`.

**Why:** The data JSON field names are already American (`corneaColors`, `irisColors`); the British spellings in some Java method/var names were inconsistent drift.

**How to apply:** Name new symbols with `color`; when rewriting existing British-spelled identifiers, switch them to American. See [[eye-property-unification]].
