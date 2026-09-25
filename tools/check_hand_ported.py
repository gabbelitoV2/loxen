#!/usr/bin/env python3
import argparse
import json
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
INVENTORY = HERE / "inventory.json"
BASELINE = HERE / "hand_ported.json"


def load_json(path):
    return json.loads(path.read_text(encoding="utf-8-sig")) if path.exists() else {}


def save_json(path, data):
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8", newline="\n")


def upstream_hashes(inventory):
    return {entry["path"]: entry["sha256"] for entry in inventory.get("files", [])}


def findings(inventory, baseline, root=ROOT):
    hashes = upstream_hashes(inventory)
    result = []
    for swift, entry in sorted(baseline.items()):
        kotlin = entry["kotlin"]
        current = hashes.get(swift)
        if current is None:
            result.append(f"{swift}: removed upstream, so {kotlin} has no Swift counterpart any more")
        elif current != entry["sha256"]:
            result.append(f"{swift}: changed upstream; bring {kotlin} in step by hand, then run "
                          "python tools/check_hand_ported.py --update")
        if not (root / kotlin).exists():
            result.append(f"{kotlin}: missing, it is the hand port of {swift}")
    return result


def update(inventory, baseline):
    hashes = upstream_hashes(inventory)
    for swift, entry in baseline.items():
        if swift in hashes:
            entry["sha256"] = hashes[swift]
    return baseline


def main():
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    parser = argparse.ArgumentParser(
        description="Check that the Swift files ported by hand (tools/hand_ported.json) have not changed upstream "
        "since their Kotlin was last brought in step. Reads the hashes from tools/inventory.json. Exits with 1 "
        "on a finding."
    )
    parser.add_argument("--update", action="store_true",
                        help="store the current upstream hashes after bringing the Kotlin files in step")
    args = parser.parse_args()
    if not INVENTORY.exists():
        sys.exit(f"{INVENTORY} is missing. Run python tools/inventory.py first.")
    inventory = load_json(INVENTORY)
    baseline = load_json(BASELINE)
    if args.update:
        save_json(BASELINE, update(inventory, baseline))
        print(f"wrote {BASELINE}")
        return
    problems = findings(inventory, baseline)
    for line in problems:
        print(line)
    print(f"check_hand_ported: {len(baseline)} hand-ported files, {len(problems)} findings")
    if problems:
        sys.exit(1)


if __name__ == "__main__":
    main()
