#!/usr/bin/env python3
"""Apply a token-migration map to the shipped eye configs in place.

Two-step migration (see at-usage.md). Step 1: run `/sg migratetokens` in a dev client that has every
content mod loaded; it writes `<gameDir>/somegoogly-migration/token-migration.json`, mapping each entity's
legacy attach tokens to their new canonical path tokens (e.g. `#0` -> `head`). Step 2: run this script,
which rewrites only the `attachPoint` string values under `src/main/resources/data/**/eyes/*.json`,
leaving every other byte (formatting, numbers, version/age metadata) untouched.

The rewrite is a literal replacement of `"attachPoint": "<old>"` -> `"attachPoint": "<new>"` scoped to the
file for the matching entity (file `data/<ns>/eyes/<path>.json` -> entity id `<ns>:<path>`), so it is
deterministic and reviewable in a diff. Idempotent: re-running with the same map is a no-op once applied.

Usage:
    python tools/migrate_attach_tokens.py <path-to-token-migration.json> [--data DIR] [--dry-run]

`--data` defaults to `src/main/resources/data` relative to the repo root (the script's parent's parent).
"""

import argparse
import json
import sys
from pathlib import Path


def entity_id_for(eyes_file: Path, data_root: Path) -> str:
    """data/<ns>/eyes/<path...>.json -> '<ns>:<path...>' (path may be nested)."""
    rel = eyes_file.relative_to(data_root)
    namespace = rel.parts[0]
    # parts: <ns>/eyes/<path...>.json -> drop <ns> and 'eyes', join the rest, strip .json
    after_eyes = rel.parts[2:]
    path = "/".join(after_eyes)
    if path.endswith(".json"):
        path = path[: -len(".json")]
    return f"{namespace}:{path}"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("mapping", type=Path, help="token-migration.json produced by /sg migratetokens")
    parser.add_argument("--data", type=Path, default=None, help="data/ root (default: src/main/resources/data)")
    parser.add_argument("--dry-run", action="store_true", help="report changes without writing")
    args = parser.parse_args()

    repo_root = Path(__file__).resolve().parent.parent
    data_root = args.data if args.data else repo_root / "src" / "main" / "resources" / "data"
    if not data_root.is_dir():
        print(f"error: data root not found: {data_root}", file=sys.stderr)
        return 2

    mapping = json.loads(args.mapping.read_text(encoding="utf-8"))
    unresolved = mapping.pop("_unresolved", [])
    if unresolved:
        print(f"note: {len(unresolved)} entit(ies) were unresolved at dump time (their mod wasn't loaded):")
        for entity in unresolved:
            print(f"  - {entity}")

    files_changed = 0
    tokens_changed = 0
    missing = []
    for entity_id, token_map in mapping.items():
        # Locate the file: <ns>:<path> -> data/<ns>/eyes/<path>.json
        namespace, _, path = entity_id.partition(":")
        eyes_file = data_root / namespace / "eyes" / (path + ".json")
        if not eyes_file.is_file():
            missing.append(entity_id)
            continue

        text = eyes_file.read_text(encoding="utf-8")
        original = text
        local_changed = 0
        for old, new in token_map.items():
            needle = f'"attachPoint": "{old}"'
            replacement = f'"attachPoint": "{new}"'
            count = text.count(needle)
            if count:
                text = text.replace(needle, replacement)
                local_changed += count

        if text != original:
            files_changed += 1
            tokens_changed += local_changed
            verb = "would update" if args.dry_run else "updated"
            print(f"{verb} {eyes_file.relative_to(repo_root)} ({local_changed} token(s): {token_map})")
            if not args.dry_run:
                eyes_file.write_text(text, encoding="utf-8")

    if missing:
        print(f"warning: {len(missing)} mapped entit(ies) had no config file under {data_root}:")
        for entity in missing:
            print(f"  - {entity}")

    summary = "Dry run:" if args.dry_run else "Done:"
    print(f"{summary} {tokens_changed} token(s) across {files_changed} file(s).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
