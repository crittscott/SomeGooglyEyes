# Forge 1.21.1 Port Log

This file is append-only reduced audit history. Do not read it during normal execution; use
`forge-1.21.1-port-status.md` for current state.

No verification commands or stage completions have been recorded. Forge is waiting on NeoForge.

## Entry format

### YYYY-MM-DD — Stage N — Work unit

- Command: exact command, or `none` for a documentation-only stage result.
- Result: exit state, duration, and reduced error/test count.
- Delta: what changed from the preceding result.
- Attempts: failed post-edit attempts used for this unit.
- Decision: durable implementation or handoff decision.
