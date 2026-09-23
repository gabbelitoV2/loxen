#!/usr/bin/env python3
import argparse
import difflib
import json
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
sys.path.insert(0, str(HERE))
STATE = HERE / "port-state.json"
INVENTORY = HERE / "inventory.json"
REQUESTS = HERE / "regen_effects_requests.diff"
SOURCE_DIRS = [ROOT / "app/src/main/java", ROOT / "app/src/test/java", ROOT / "app/src/androidTest/java"]
KEPT = {
    "Moblin/VideoEffects/Text/TextEffectFormatter.swift",
    "Moblin/VideoEffects/Text/TextFormatStringLoader.swift",
}
PHASES = [
    ("A", 1, 1, [
        "Moblin/Various/Detection.swift",
        "Moblin/VideoEffects/Dewarp360/Dewarp360Filter.swift",
    ]),
    ("B", 1, 2, [
        "Moblin/Media/HaishinKit/Media/Video/VideoEffect.swift",
        "Moblin/VideoEffects/EffectUtils.swift",
        "Moblin/VideoEffects/ShapeEffect.swift",
    ]),
    ("C", None, 1, [
        "Moblin/Media/HaishinKit/Media/Video/VideoEffectsProcessor.swift",
        "Moblin/VideoEffects/",
    ]),
    ("D", None, 1, [
        "Moblin/Various/MediaPlayer.swift",
        "Moblin/Various/Model/ModelFaceBackgroundImage.swift",
        "Moblin/Various/Model/ModelReplay.swift",
        "Moblin/Various/Model/ModelMediaPlayer.swift",
        "MoblinTests/Moblin/VideoEffects/",
    ]),
]


def load_json(path, default):
    if path.exists():
        return json.loads(path.read_text(encoding="utf-8"))
    return default


def load_inventory():
    if not INVENTORY.exists():
        sys.exit(f"{INVENTORY} is missing. Run python tools/inventory.py first.")
    return load_json(INVENTORY, None)


def phase_entries(inventory):
    usable = [entry for entry in inventory["files"] if entry["tier"] != "skip" and entry["path"] not in KEPT]
    taken = set()
    phases = []
    for name, workers, runs, prefixes in PHASES:
        entries = []
        for prefix in prefixes:
            if prefix.endswith("/"):
                matched = [entry for entry in usable if entry["path"].startswith(prefix)]
            else:
                matched = [entry for entry in usable if entry["path"] == prefix]
                if not matched:
                    sys.exit(f"phase {name}: {prefix} is not in the inventory, or it is skipped or kept")
            for entry in matched:
                if entry["path"] not in taken:
                    taken.add(entry["path"])
                    entries.append(entry)
        entries.sort(key=lambda entry: (entry["wave"], entry["path"]))
        phases.append({"name": name, "workers": workers, "runs": runs, "entries": entries})
    return phases


def regenerated_entries(inventory):
    return [entry for phase in phase_entries(inventory) for entry in phase["entries"]]


def find_entries(paths, inventory):
    by_path = {entry["path"]: entry for entry in inventory["files"]}
    result = []
    for path in paths:
        wanted = path.strip().replace("\\", "/").removeprefix("./")
        entry = by_path.get(wanted)
        if entry is None:
            matches = [other for key, other in by_path.items() if key.endswith("/" + wanted)]
            if len(matches) != 1:
                sys.exit(f"{path} matches {len(matches)} Swift files in the inventory")
            entry = matches[0]
        if entry["tier"] == "skip":
            sys.exit(f"{entry['path']} is in the skip tier")
        if entry["path"] in KEPT:
            sys.exit(f"{entry['path']} is a kept boundary file and is never re-translated")
        result.append(entry)
    return result


def run(command, check=False):
    print("$ " + " ".join(str(part) for part in command), flush=True)
    result = subprocess.run([str(part) for part in command], cwd=ROOT)
    if check and result.returncode != 0:
        sys.exit(f"command failed with exit code {result.returncode}")
    return result.returncode


def model_options(args):
    options = ["--provider", args.provider, "--backend", args.backend, "--effort", args.effort]
    if args.model:
        options += ["--model", args.model]
    return options


def port_command(args, entries, workers):
    return ([sys.executable, HERE / "port.py", "--tier", "all", "--force", "--no-postprocess", "--workers", str(workers)]
            + model_options(args) + ["--include"] + [entry["path"] for entry in entries])


def now_iso():
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


def report_run(label, entries, started):
    state = load_json(STATE, {})
    failures = []
    print(f"\n{label}: raw port output before postprocess")
    for entry in entries:
        saved = state.get(entry["path"], {})
        kotlin = ROOT / entry["kotlin_path"]
        if saved.get("status") != "ok" or saved.get("ported_at", "") < started:
            reason = saved.get("error") if saved.get("status") == "error" else "not written by this run"
            print(f"  FAIL {entry['path']}: {reason}")
            failures.append(entry["path"])
            continue
        todos = []
        if kotlin.exists():
            for number, line in enumerate(kotlin.read_text(encoding="utf-8", errors="replace").split("\n"), 1):
                if "TODO(" in line:
                    todos.append((number, line.strip()))
        unsupported = saved.get("unsupported", [])
        if not unsupported and not todos:
            print(f"  ok   {entry['path']}")
            continue
        failures.append(entry["path"])
        print(f"  TODO {entry['path']} -> {entry['kotlin_path']}")
        for item in unsupported:
            print(f"         unsupported: {item}")
        for number, line in todos:
            print(f"         line {number}: {line[:160]}")
    return failures


def postprocess_entries(entries):
    written = [str(ROOT / entry["kotlin_path"]) for entry in entries if (ROOT / entry["kotlin_path"]).exists()]
    if written:
        run([sys.executable, HERE / "postprocess.py", "--only", *written])


def run_port(args, label, entries, workers, runs, dry_run):
    failures = []
    for number in range(1, runs + 1):
        run_label = label if runs == 1 else f"{label}, run {number} of {runs}"
        command = port_command(args, entries, workers)
        if dry_run:
            print(f"\n{run_label}: {len(entries)} files")
            print("$ " + " ".join(str(part) for part in command[:10]) + " --include ...")
            for entry in entries:
                print(f"    wave {entry['wave']:>3}  {entry['tier']:<6} {entry['path']}")
            continue
        started = now_iso()
        print(f"\n=== {run_label}: {len(entries)} files ===", flush=True)
        run(command)
        failures = report_run(run_label, entries, started)
        postprocess_entries(entries)
    return failures


def kotlin_sources():
    for directory in SOURCE_DIRS:
        if directory.exists():
            yield from sorted(directory.rglob("*.kt"))


def keep_fixes_in_owned_files(args, owned):
    run([sys.executable, HERE / "postprocess.py"])
    before = {path: path.read_text(encoding="utf-8", errors="replace") for path in kotlin_sources()
              if path.resolve() not in owned}
    fix = [sys.executable, HERE / "fix.py", "--rounds", str(args.fix_rounds), "--workers", str(args.workers or 4)]
    run(fix + model_options(args))
    run([sys.executable, HERE / "postprocess.py"])
    diffs = []
    for path, text in before.items():
        if not path.exists():
            continue
        now = path.read_text(encoding="utf-8", errors="replace")
        if now == text:
            continue
        rel = path.relative_to(ROOT).as_posix()
        diffs.append("".join(difflib.unified_diff(
            text.splitlines(keepends=True), now.splitlines(keepends=True), f"a/{rel}", f"b/{rel}"
        )))
        path.write_text(text, encoding="utf-8", newline="\n")
        print(f"  discarded fix.py change in {rel} (not a regenerated file; it becomes a request to its owner)")
    if diffs:
        REQUESTS.write_text("".join(diffs), encoding="utf-8", newline="\n")
        print(f"{len(diffs)} proposed changes to files outside the regeneration are in {REQUESTS}")
    elif REQUESTS.exists():
        REQUESTS.unlink()


def main():
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    parser = argparse.ArgumentParser(
        description="Re-translate the effect files against the effect shims in the phases of the effects plan "
        "(section 2.3 step 4), then run fix.py, check_hooks.py and check_effect_boundary.py --diff."
    )
    parser.add_argument("--phase", nargs="+", choices=[name for name, *_ in PHASES], default=None,
                        help="run only these phases")
    parser.add_argument("--only", nargs="+", default=None, metavar="SWIFT_PATH",
                        help="re-translate only these Swift files (one worker, one run)")
    parser.add_argument("--workers", type=int, default=None, help="workers for phases C and D (port.py's default)")
    parser.add_argument("--provider", default="anthropic")
    parser.add_argument("--backend", choices=["auto", "api", "cli"], default="auto")
    parser.add_argument("--model", default=None)
    parser.add_argument("--effort", default="high")
    parser.add_argument("--fix-rounds", type=int, default=3)
    parser.add_argument("--no-fix", action="store_true", help="skip fix.py; the checks still run")
    parser.add_argument("--dry-run", action="store_true", help="print the phases and commands and stop")
    parser.add_argument("--skip-snapshot-check", action="store_true",
                        help="start a full run although effect files changed since the boundary snapshot")
    args = parser.parse_args()
    if args.phase and args.only:
        parser.error("--phase and --only cannot be combined")

    inventory = load_inventory()
    phases = phase_entries(inventory)
    owned = {(ROOT / entry["kotlin_path"]).resolve() for phase in phases for entry in phase["entries"]}
    if not args.phase and not args.only and not args.dry_run and not args.skip_snapshot_check:
        import check_effect_boundary
        stale = check_effect_boundary.stale_files()
        if stale is None:
            sys.exit("Take the boundary snapshot first: python tools/check_effect_boundary.py --snapshot")
        if stale:
            print("effect files changed since the boundary snapshot:")
            for rel in stale[:20]:
                print(f"  {rel}")
            sys.exit("Run python tools/check_effect_boundary.py --snapshot again before the regeneration (never after "
                     "it has started), or pass --skip-snapshot-check.")
    failures = []
    if args.only:
        entries = find_entries(args.only, inventory)
        failures += run_port(args, "only", entries, 1, 1, args.dry_run)
    else:
        for phase in phases:
            if args.phase and phase["name"] not in args.phase:
                continue
            workers = phase["workers"] or args.workers or 2
            failures += run_port(args, f"phase {phase['name']}", phase["entries"], workers, phase["runs"], args.dry_run)
    if args.dry_run:
        print(f"\nthen: postprocess.py, fix.py{' (skipped)' if args.no_fix else ''}, check_hooks.py, "
              "check_effect_boundary.py --diff")
        return

    if not args.no_fix:
        keep_fixes_in_owned_files(args, owned)
    hooks = run([sys.executable, HERE / "check_hooks.py"])
    boundary = run([sys.executable, HERE / "check_effect_boundary.py", "--diff"])
    print("\nsummary")
    print(f"  files with TODO( or unsupported items, or not ported: {len(failures)}")
    for path in failures:
        print(f"    {path}")
    print(f"  check_hooks.py: {'ok' if hooks == 0 else 'FAILED'}")
    print(f"  check_effect_boundary.py --diff: {'ok' if boundary == 0 else 'FAILED'}")
    if failures or hooks or boundary:
        sys.exit(1)


if __name__ == "__main__":
    main()
