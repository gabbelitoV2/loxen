#!/usr/bin/env python3
import argparse
import json
import os
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
UPSTREAM = ROOT / ".upstream"
UPSTREAM_URL = "https://github.com/eerimoq/moblin.git"
STATE = HERE / "port-state.json"
INVENTORY = HERE / "inventory.json"


def run(command, cwd=ROOT, check=True, capture=False):
    print("$ " + " ".join(str(part) for part in command), flush=True)
    result = subprocess.run(
        [str(part) for part in command], cwd=cwd, text=True, encoding="utf-8", errors="replace",
        capture_output=capture,
    )
    if check and result.returncode != 0:
        if capture:
            print(result.stdout[-3000:])
            print(result.stderr[-3000:])
        sys.exit(f"command failed with exit code {result.returncode}")
    return result


def git(*arguments, capture=True, check=True):
    return run(["git", "-C", UPSTREAM, *arguments], capture=capture, check=check)


def load_state():
    return json.loads(STATE.read_text(encoding="utf-8")) if STATE.exists() else {}


def ensure_upstream(base_commit):
    if not (UPSTREAM / ".git").exists():
        run(["git", "clone", "--filter=blob:none", "--no-checkout", UPSTREAM_URL, UPSTREAM])
    git("fetch", "--quiet", "origin")
    if base_commit and git("cat-file", "-e", f"{base_commit}^{{commit}}", check=False).returncode != 0:
        sys.exit(f"base commit {base_commit} is not in {UPSTREAM_URL}")


def base_commit_of(state):
    commits = {entry.get("moblin_commit") for entry in state.values() if entry.get("moblin_commit")}
    if len(commits) == 1:
        return commits.pop()
    counts = {}
    for entry in state.values():
        commit = entry.get("moblin_commit")
        if commit:
            counts[commit] = counts.get(commit, 0) + 1
    return max(counts, key=counts.get) if counts else None


def remove_deleted_files(state, inventory):
    current = {entry["path"] for entry in inventory["files"] if entry["tier"] != "skip"}
    removed = []
    for path in sorted(set(state) - current):
        kotlin = ROOT / state[path]["kotlin_path"]
        if kotlin.exists():
            kotlin.unlink()
        removed.append(path)
        del state[path]
    if removed:
        STATE.write_text(json.dumps(state, indent=2, ensure_ascii=False), encoding="utf-8", newline="\n")
    return removed


def gradle(task):
    env = dict(os.environ)
    for candidate in [Path(r"C:\Program Files\Java\jdk-21"), Path(r"C:\Program Files\Java\jdk-17")]:
        if "JAVA_HOME" not in env and (candidate / "bin").exists():
            env["JAVA_HOME"] = str(candidate)
    gradlew = ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew")
    print(f"$ gradlew {task}", flush=True)
    result = subprocess.run(
        [str(gradlew), task, "-q"], cwd=ROOT, env=env, text=True, encoding="utf-8", errors="replace",
        capture_output=True,
    )
    errors = [line for line in (result.stdout + result.stderr).splitlines() if line.startswith("e: ")]
    return result.returncode == 0, errors


def main():
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    parser = argparse.ArgumentParser(description="Bring the Android port up to date with eerimoq/moblin.")
    parser.add_argument("--ref", default="origin/main", help="upstream ref to sync to")
    parser.add_argument("--provider", default="deepseek")
    parser.add_argument("--model", default=None)
    parser.add_argument("--workers", type=int, default=8)
    parser.add_argument("--fix-rounds", type=int, default=3)
    parser.add_argument("--dry-run", action="store_true", help="show what would be ported and stop")
    parser.add_argument("--no-build", action="store_true")
    parser.add_argument("--commit", action="store_true", help="commit the result in moblin-android")
    parser.add_argument("--push", action="store_true", help="push after committing")
    args = parser.parse_args()

    state = load_state()
    base = base_commit_of(state)
    ensure_upstream(base)
    target = git("rev-parse", args.ref).stdout.strip()
    print(f"ported from {base[:10] if base else 'unknown'}, upstream {args.ref} is {target[:10]}")
    if base == target and not args.dry_run:
        print("already up to date")
        return
    if base:
        log = git("log", "--oneline", f"{base}..{target}").stdout.strip().splitlines()
        print(f"{len(log)} new upstream commits")
        for line in log[:20]:
            print("  " + line)
    git("checkout", "--quiet", "--detach", target)

    run([sys.executable, HERE / "inventory.py", "--moblin", UPSTREAM])
    run([sys.executable, HERE / "resources.py", "--moblin", UPSTREAM])
    inventory = json.loads(INVENTORY.read_text(encoding="utf-8"))
    if args.dry_run:
        run([sys.executable, HERE / "port.py", "--tier", "all", "--provider", args.provider, "--dry-run"])
        return
    removed = remove_deleted_files(state, inventory)
    for path in removed:
        print(f"removed {path}")

    port = [sys.executable, HERE / "port.py", "--tier", "all", "--incremental", "--provider", args.provider,
            "--workers", str(args.workers)]
    if args.model:
        port += ["--model", args.model]
    run(port)

    build_ok = True
    if not args.no_build:
        fix = [sys.executable, HERE / "fix.py", "--provider", args.provider, "--rounds", str(args.fix_rounds),
               "--workers", str(args.workers), "--effort", "medium"]
        run(fix, check=False)
    run([sys.executable, HERE / "postprocess.py"], check=False)
    hooks = run([sys.executable, HERE / "check_hooks.py"], check=False, capture=True)
    print(hooks.stdout.rstrip())
    hooks_ok = hooks.returncode == 0
    if not args.no_build:
        build_ok, errors = gradle(":app:assembleDebug")
        print(f"assembleDebug: {'ok' if build_ok else 'FAILED'} ({len(errors)} compile errors)")
        for line in errors[:20]:
            print("  " + line)

    if not hooks_ok:
        print(hooks.stderr.rstrip())
        sys.exit("hooks are missing or not applied, stopping before the commit. Re-derive them as tools/hooks/README.md "
                 "describes, then run python tools/postprocess.py and python tools/check_hooks.py.")

    if args.commit:
        run(["git", "add", "-A"])
        message = f"Sync with eerimoq/moblin {target[:10]}."
        if not build_ok:
            message = f"Sync with eerimoq/moblin {target[:10]} (build failing)."
        run(["git", "commit", "-q", "-m", message], check=False)
        if args.push:
            run(["git", "push", "-q", "origin", "HEAD"])
    summary = [line for line in hooks.stdout.splitlines() if line.startswith("hooks: ")]
    if summary:
        print(summary[0])
    if not build_ok:
        sys.exit(1)


if __name__ == "__main__":
    main()
