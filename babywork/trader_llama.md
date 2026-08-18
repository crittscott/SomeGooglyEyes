# Trader Llama — NOT COVERED (baby only)

Same model as `llama` — `TraderLlama extends Llama` with no model override, so it uses `LlamaModel`
directly. See `llama.md` for the full mechanism and the fix needed. Whatever fix is written for llama
covers this mob automatically once it recognizes the model class rather than the entity type.
