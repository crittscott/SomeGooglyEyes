#!/usr/bin/env python3
"""Migrate eye-config JSONs to the two-angle orientation (inclination + azimuth).

The eye model now stores orientation as two angles — `"inclination"` (from the part's +Y axis) and
`"azimuth"` (from its +X axis), in degrees — replacing every earlier scheme: the original Euler triple
(`yRotation`/`xRotation`/`zRotation`) and the interim `"rotation": [x, y, z, w]` quaternion.

All bundled eyes have identity orientation (every old Euler value is 0.0), so this simply strips the
dead rotation keys and writes the default forward-facing angles (inclination 90, azimuth 270 = facing
-Z, the same direction identity produced). It does NOT decompose non-identity rotations — there are
none in the data; if that ever changes, this would flatten them to forward.

Idempotent: an eye already on inclination/azimuth (and free of old keys) is left untouched; existing
angle values are preserved while any stale rotation keys are removed. Re-runnable.

Usage:
    python migrate_rotation.py [repo_root]

repo_root defaults to the first ancestor of this script (or the current directory) that contains
src/main/resources/data.
"""
import glob
import json
import os
import sys


def find_root(start):
    d = os.path.abspath(start)
    while True:
        if os.path.isdir(os.path.join(d, "src", "main", "resources", "data")):
            return d
        parent = os.path.dirname(d)
        if parent == d:
            return None
        d = parent


def resolve_root():
    if len(sys.argv) > 1:
        return os.path.abspath(sys.argv[1])
    for start in (os.path.dirname(os.path.abspath(__file__)), os.getcwd()):
        root = find_root(start)
        if root:
            return root
    return None


# --- file transform ----------------------------------------------------------------------------

OLD_ROTATION_KEYS = ("yRotation", "xRotation", "zRotation", "rotation")
NEW_KEYS = ("inclination", "azimuth")
DEFAULT_INCLINATION = 90.0
DEFAULT_AZIMUTH = 270.0


def convert_eye(eye):
    has_old = any(k in eye for k in OLD_ROTATION_KEYS)
    has_new = all(k in eye for k in NEW_KEYS)
    if has_new and not has_old:
        return False  # already migrated

    # Preserve any already-present angles; otherwise default to forward (valid: source is identity).
    inclination = eye.get("inclination", DEFAULT_INCLINATION)
    azimuth = eye.get("azimuth", DEFAULT_AZIMUTH)

    rebuilt = {}
    placed = False
    for k, v in eye.items():
        if k in OLD_ROTATION_KEYS or k in NEW_KEYS:
            continue
        rebuilt[k] = v
        if k == "sideOffset":  # the rotation fields lived right after sideOffset
            rebuilt["inclination"] = inclination
            rebuilt["azimuth"] = azimuth
            placed = True
    if not placed:
        rebuilt["inclination"] = inclination
        rebuilt["azimuth"] = azimuth
    eye.clear()
    eye.update(rebuilt)
    return True


def process(path):
    with open(path, "rb") as f:
        raw = f.read()
    newline = b"\r\n" if b"\r\n" in raw else b"\n"
    data = json.loads(raw.decode("utf-8"))

    changed = 0
    for entry in data.get("entries", []) or []:
        for head in entry.get("heads", []) or []:
            for eye in head.get("eyes", []) or []:
                if convert_eye(eye):
                    changed += 1
    if changed == 0:
        return 0

    text = json.dumps(data, indent=2, ensure_ascii=False) + "\n"
    out = text.encode("utf-8")
    if newline != b"\n":
        out = out.replace(b"\n", newline)
    with open(path, "wb") as f:
        f.write(out)
    return changed


def main():
    root = resolve_root()
    if not root:
        print("Could not find src/main/resources/data — pass the repo root as an argument.")
        sys.exit(1)

    pattern = os.path.join(root, "src", "main", "resources", "data", "*", "eyes", "*.json")
    files = sorted(glob.glob(pattern))
    if not files:
        print(f"No eye JSONs matched {pattern!r}.")
        sys.exit(1)

    changed_files = 0
    total = 0
    for path in files:
        n = process(path)
        if n:
            changed_files += 1
            total += n
            print(f"  ~{n}  {os.path.relpath(path, root)}")

    print(f"\nDone. Migrated {total} eye(s) across {changed_files}/{len(files)} file(s).")


if __name__ == "__main__":
    main()
