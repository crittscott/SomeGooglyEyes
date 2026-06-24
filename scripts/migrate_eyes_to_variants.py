#!/usr/bin/env python3
"""One-off migration: wrap each eye-config entry's bare ``heads`` list into the
canonical ``variants`` form (a single weight-1.0 variant).

The mod no longer reads entry-level ``heads`` -- ``variants`` is the only shape.
Run this once over ``src/main/resources/data/*/eyes/**/*.json`` before the codec
change lands, otherwise unmigrated files silently lose their eyes (the codec
ignores unknown keys rather than erroring).

Idempotent: entries that already have ``variants`` (or have neither field, e.g.
a disabled stub) are left untouched.
"""

from __future__ import annotations

import json
import sys
from collections import OrderedDict
from pathlib import Path

# Repo root is the parent of this script's directory (scripts/).
DATA_ROOT = Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "data"

# Canonical entry key order, so diffs stay clean and readable.
ENTRY_KEY_ORDER = ["version", "age", "enabled", "variants"]


def migrate_entry(entry: dict) -> bool:
    """Wrap a bare ``heads`` into ``variants``. Returns True if changed."""
    if "variants" in entry or "heads" not in entry:
        return False
    heads = entry.pop("heads")
    entry["variants"] = [OrderedDict([("weight", 1.0), ("heads", heads)])]
    return True


def reorder_entry(entry: dict) -> OrderedDict:
    """Rebuild an entry in canonical key order; unknown keys keep their order at the end."""
    ordered = OrderedDict()
    for key in ENTRY_KEY_ORDER:
        if key in entry:
            ordered[key] = entry[key]
    for key, value in entry.items():
        if key not in ordered:
            ordered[key] = value
    return ordered


def migrate_file(path: Path) -> bool:
    with path.open(encoding="utf-8") as fh:
        doc = json.load(fh, object_pairs_hook=OrderedDict)

    entries = doc.get("entries")
    if not isinstance(entries, list):
        return False

    changed = False
    new_entries = []
    for entry in entries:
        if isinstance(entry, dict):
            if migrate_entry(entry):
                changed = True
            entry = reorder_entry(entry)
        new_entries.append(entry)
    doc["entries"] = new_entries

    if not changed:
        return False

    with path.open("w", encoding="utf-8", newline="\n") as fh:
        json.dump(doc, fh, ensure_ascii=False, indent=2)
        fh.write("\n")
    return True


def main() -> int:
    if not DATA_ROOT.is_dir():
        print(f"data root not found: {DATA_ROOT}", file=sys.stderr)
        return 1

    files = sorted(DATA_ROOT.glob("*/eyes/**/*.json"))
    changed = [p for p in files if migrate_file(p)]

    print(f"Scanned {len(files)} eye config files; migrated {len(changed)}.")
    for p in changed:
        print(f"  {p.relative_to(DATA_ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
