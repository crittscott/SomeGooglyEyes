from __future__ import annotations

import argparse
import unicodedata
from pathlib import Path

ALLOWED_CONTROLS = {"\t", "\n", "\r"}


def is_normal_ascii(ch: str) -> bool:
    return ch in ALLOWED_CONTROLS or 32 <= ord(ch) <= 126


def display_char(ch: str) -> str:
    if ch == "\t":
        return r"\t"
    if ch == "\n":
        return r"\n"
    if ch == "\r":
        return r"\r"
    return ch


def format_char_report(ch: str) -> str:
    code = ord(ch)
    name = unicodedata.name(ch, "<unnamed>")
    return f"U+{code:04X} dec={code} char={display_char(ch)!r} {name}"


def scan_file(path: Path, union: set[str] | None = None) -> int:
    try:
        text = path.read_text(encoding="utf-8")
    except UnicodeDecodeError as exc:
        print(f"{path}: invalid UTF-8 at byte {exc.start}: {exc.reason}")
        text = path.read_bytes().decode("utf-8", errors="replace")

    count = 0
    for line_no, line in enumerate(text.splitlines(keepends=True), start=1):
        for col_no, ch in enumerate(line, start=1):
            if is_normal_ascii(ch):
                continue

            if union is not None:
                union.add(ch)
            else:
                print(f"{path}:{line_no}:{col_no}: {format_char_report(ch)}")
            count += 1

    return count


def main() -> int:
    parser = argparse.ArgumentParser(
        description="List non-normal-ASCII characters in Java files under cwd."
    )
    parser.add_argument(
        "root",
        nargs="?",
        default=".",
        help="Root directory to scan; defaults to cwd.",
    )
    parser.add_argument(
        "-u",
        "--union",
        action="store_true",
        help="Show each distinct suspicious character only once.",
    )
    args = parser.parse_args()

    root = Path(args.root)
    total = 0
    union: set[str] | None = set() if args.union else None
    files = sorted(root.rglob("*.java"))

    for path in files:
        total += scan_file(path, union)

    if union is not None:
        for ch in sorted(union, key=ord):
            print(format_char_report(ch))

    print(f"\nScanned {len(files)} Java file(s); found {total} suspicious character(s).")
    return 1 if total else 0


if __name__ == "__main__":
    raise SystemExit(main())
