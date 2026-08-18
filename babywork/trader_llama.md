# Trader Llama — FIXED (baby only)

Same model as `llama` — `TraderLlama extends Llama` with no model override, so it uses `LlamaModel`
directly. See `llama.md` for the full mechanism and the fix landed (`RabbitLlamaResolver`), which covers
this mob automatically since it recognizes the model class rather than the entity type.

`trader_llama.json`'s `attachPoint`s were renamed `#0`/`#1` → `head`/`body`, same as `llama.json`.
