#!/usr/bin/env python3
"""Strip the dead `sideOffset` key from SomeGoogly eye datapack JSON files.

`sideOffset` was always a no-op duplicate of position.x and has been removed from the
data model. This walks the eye config JSON tree and deletes every `sideOffset` key,
preserving the existing 2-space indentation and key order. Old files keep parsing
without it (the codec field is optional), so this is purely a cleanup.

Usage:
    python scripts/strip_side_offset.py            # rewrite files in place
    python scripts/strip_side_offset.py --check     # report only, non-zero exit if any remain

By default it scans src/main/resources/data and somegoogly-test-datapack relative to the
repo root (the script's parent's parent). Pass explicit paths to override.
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

KEY = "sideOffset"
DEFAULT_ROOTS = [
    Path("src/main/resources/data"),
    Path("somegoogly-test-datapack"),
]


def strip(node):
    """Recursively remove KEY from dicts/lists. Returns (new_node, removed_count)."""
    removed = 0
    if isinstance(node, dict):
        new = {}
        for k, v in node.items():
            if k == KEY:
                removed += 1
                continue
            child, child_removed = strip(v)
            new[k] = child
            removed += child_removed
        return new, removed
    if isinstance(node, list):
        new_list = []
        for item in node:
            child, child_removed = strip(item)
            new_list.append(child)
            removed += child_removed
        return new_list, removed
    return node, removed


def iter_json_files(roots: list[Path]) -> list[Path]:
    files: list[Path] = []
    for root in roots:
        if root.is_file() and root.suffix == ".json":
            files.append(root)
        elif root.is_dir():
            files.extend(sorted(root.rglob("*.json")))
    return files


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("paths", nargs="*", type=Path,
                        help="files or dirs to process (default: the eye data roots)")
    parser.add_argument("--check", action="store_true",
                        help="don't write; exit 1 if any sideOffset keys remain")
    args = parser.parse_args()

    repo_root = Path(__file__).resolve().parent.parent
    if args.paths:
        roots = [p if p.is_absolute() else repo_root / p for p in args.paths]
    else:
        roots = [repo_root / r for r in DEFAULT_ROOTS]

    files = iter_json_files(roots)
    if not files:
        print("No JSON files found under:", *[str(r) for r in roots], sep="\n  ")
        return 0

    changed = 0
    total_removed = 0
    for path in files:
        try:
            original = path.read_text(encoding="utf-8")
            data = json.loads(original)
        except (OSError, json.JSONDecodeError) as e:
            print(f"SKIP (unreadable/invalid): {path} -- {e}", file=sys.stderr)
            continue

        stripped, removed = strip(data)
        if removed == 0:
            continue

        total_removed += removed
        changed += 1
        if args.check:
            print(f"would strip {removed} sideOffset key(s): {path}")
            continue

        # Match the existing formatting: 2-space indent, trailing newline.
        new_text = json.dumps(stripped, indent=2, ensure_ascii=False) + "\n"
        path.write_text(new_text, encoding="utf-8")
        print(f"stripped {removed} sideOffset key(s): {path}")

    verb = "would change" if args.check else "changed"
    print(f"\n{verb} {changed} file(s), {total_removed} key(s) total "
          f"(scanned {len(files)} file(s)).")
    if args.check and total_removed > 0:
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
