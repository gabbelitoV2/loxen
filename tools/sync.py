#!/usr/bin/env python3
import argparse
import json
import os
import subprocess
import sys
import traceback
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
sys.path.insert(0, str(HERE))
import degrade
import sync_report

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
        sys.exit(f"command failed with exit code {result.returncode}: " + " ".join(str(part) for part in command))
    return result


def git(*arguments, capture=True, check=True):
    return run(["git", "-C", UPSTREAM, *arguments], capture=capture, check=check)


def load_state():
    return json.loads(STATE.read_text(encoding="utf-8")) if STATE.exists() else {}


def save_state(state):
    STATE.write_text(json.dumps(state, indent=2, ensure_ascii=False), encoding="utf-8", newline="\n")


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
        save_state(state)
    return removed


def gradle(*tasks):
    env = dict(os.environ)
    for candidate in [Path(r"C:\Program Files\Java\jdk-21"), Path(r"C:\Program Files\Java\jdk-17")]:
        if "JAVA_HOME" not in env and (candidate / "bin").exists():
            env["JAVA_HOME"] = str(candidate)
    gradlew = ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew")
    print(f"$ gradlew {' '.join(tasks)}", flush=True)
    result = subprocess.run(
        [str(gradlew), *tasks, "-q"], cwd=ROOT, env=env, text=True, encoding="utf-8", errors="replace",
        capture_output=True,
    )
    return result.returncode == 0, result.stdout + result.stderr


def builds():
    ok, output = gradle(":app:compileDebugUnitTestKotlin", ":app:assembleDebug")
    if not ok:
        print(sync_report.tail(output, 2000))
    return ok


def push(verify=builds):
    if os.environ.get("CLAUDE_CODE_ACTION") == "1":
        print("not pushing from inside the Claude repair session; the repair workflow pushes after verifying")
        return False
    branch = run(["git", "rev-parse", "--abbrev-ref", "HEAD"], capture=True, check=False).stdout.strip()
    if branch in ("", "HEAD"):
        branch = "main"
    for _ in range(3):
        if run(["git", "push", "-q", "origin", f"HEAD:{branch}"], check=False).returncode == 0:
            return True
        if run(["git", "pull", "--rebase", "-q", "origin", branch], check=False).returncode != 0:
            run(["git", "rebase", "--abort"], check=False)
            return False
        if verify is not None and not verify():
            print(f"{branch} moved and the rebased result does not build, so nothing is pushed")
            return False
    return False


def set_aside_changes(target):
    run(["git", "stash", "push", "--include-untracked", "-q", "-m", f"sync {target[:10]}: does not build"])
    print("the changes of this sync are in git stash (git stash list), nothing of them is committed")


EFFECT_CHECKS = ["check_kernels.py", "check_effects_api.py"]


def warn_effect_checks():
    findings = []
    for script in EFFECT_CHECKS:
        if not (HERE / script).exists():
            continue
        result = run([sys.executable, HERE / script, "--moblin", UPSTREAM], check=False, capture=True)
        output = (result.stdout + result.stderr).rstrip()
        if result.returncode == 0:
            print(f"{script}: no findings")
            continue
        print(f"warning: {script} reported findings (the sync goes on; add the shim member or an entry in "
              "tools/effects_known_gaps.json before the next effects merge):")
        for line in output.splitlines():
            print("  " + line)
            findings.append(f"{script}: {line}")
    return findings


def warn_hand_ported():
    result = run([sys.executable, HERE / "check_hand_ported.py"], check=False, capture=True)
    output = (result.stdout + result.stderr).rstrip()
    if result.returncode == 0:
        print("check_hand_ported.py: no findings")
        return []
    print("warning: Swift files that are ported by hand changed upstream (the sync goes on; update their Kotlin, "
          "then run python tools/check_hand_ported.py --update):")
    findings = []
    for line in output.splitlines():
        print("  " + line)
        if not line.startswith("check_hand_ported: "):
            findings.append(line)
    return findings


def regenerate_protobufs(check=False):
    command = [sys.executable, HERE / "pbswift.py", "--moblin", UPSTREAM]
    if check:
        command.append("--check")
    result = run(command, check=False, capture=True)
    output = (result.stdout + result.stderr).rstrip()
    print(output)
    return result.returncode == 0, output


def build_lines(result):
    lines = []
    for rel, file_errors in sorted(result["errors"].items()):
        lines += [f"{rel}:{line}" for line in degrade.format_errors(file_errors)]
    return lines[:40] or sync_report.tail(result["output"]).splitlines()[-40:]


def port_and_build(args, state, inventory, target, previous):
    snapshot = degrade.Snapshot(ROOT, state)
    removed = remove_deleted_files(state, inventory)
    for path in removed:
        print(f"removed {path}")
    protobufs_ok, protobufs_output = regenerate_protobufs()

    port = [sys.executable, HERE / "port.py", "--tier", "all", "--incremental", "--provider", args.provider,
            "--workers", str(args.workers)]
    if args.model:
        port += ["--model", args.model]
    run(port)

    if not args.no_build:
        fix = [sys.executable, HERE / "fix.py", "--provider", args.provider, "--rounds", str(args.fix_rounds),
               "--workers", str(args.workers), "--effort", "medium"]
        run(fix, check=False)
    run([sys.executable, HERE / "postprocess.py"], check=False)
    state = load_state()

    reverted = {}
    build = []
    build_ok = True
    if not args.no_build:
        result = degrade.stabilize(
            snapshot, state, lambda: gradle(":app:compileDebugUnitTestKotlin"), degrade.missing_hooks,
            degrade.dependencies(inventory), save_state,
        )
        reverted = result["reverted"]
        if reverted:
            print(f"held back {len(reverted)} files at their previous version so that the app builds")
            run([sys.executable, HERE / "port.py", "--report-only"], check=False)
        if result["ok"]:
            build_ok, output = gradle(":app:assembleDebug")
            if not build_ok:
                build = sync_report.tail(output).splitlines()[-40:]
        else:
            build_ok = False
            build = build_lines(result)
        print(f"assembleDebug: {'ok' if build_ok else 'FAILED'}")
        for line in build[:20]:
            print("  " + line)
    hooks = sync_report.hook_findings()
    print(f"hooks: {len(hooks)} problems")
    for line in hooks:
        print("  " + line)
    effects = warn_effect_checks()
    hand_ported = warn_hand_ported()
    report = sync_report.collect(
        target, inventory, state, reverted=reverted, previous=previous,
        pbswift=None if protobufs_ok else protobufs_output, build=build, hand_ported=hand_ported,
        effects=effects, hooks=hooks,
    )
    return report, build_ok


def publish(report, enabled):
    try:
        sync_report.publish_or_print(report, enabled)
    except Exception as error:
        print(f"could not update the needs-repair issue: {error}")


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
    parser.add_argument("--issue", action="store_true",
                        help="open, update or close the needs-repair GitHub issue with gh (needs GITHUB_TOKEN)")
    args = parser.parse_args()

    state = load_state()
    base = base_commit_of(state)
    previous = sync_report.load()
    target = None
    try:
        ensure_upstream(base)
        target = git("rev-parse", args.ref).stdout.strip()
        print(f"ported from {base[:10] if base else 'unknown'}, upstream {args.ref} is {target[:10]}")
        if base == target and not args.dry_run and sync_report.is_empty(previous):
            print("already up to date")
            publish(previous, args.issue)
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
            regenerate_protobufs(check=True)
            warn_effect_checks()
            warn_hand_ported()
            return
        report, build_ok = port_and_build(args, state, inventory, target, previous)
    except (SystemExit, Exception) as stop:
        if isinstance(stop, SystemExit) and (stop.code in (None, 0) or args.dry_run):
            raise
        report = sync_report.new_report(target)
        report["sync_error"] = str(stop.code) if isinstance(stop, SystemExit) else traceback.format_exc()
        publish(report, args.issue)
        raise

    if args.commit and not build_ok:
        print("the app does not build even with every change of this sync held back, so only "
              "tools/sync-report.json is committed")
        set_aside_changes(target)
        report["held_back"], report["port_failures"] = sync_report.stale_files(inventory, load_state(),
                                                                              previous=report)
    sync_report.save(report)
    if args.commit:
        run(["git", "add", "-A"])
        message = f"Sync with eerimoq/moblin {target[:10]}."
        if not sync_report.is_empty(report):
            message = (f"Sync with eerimoq/moblin {target[:10]}, {sync_report.count(report)} items need repair "
                       "(tools/sync-report.json).")
        if not build_ok:
            message = (f"Sync with eerimoq/moblin {target[:10]}: the app does not build, so only the report "
                       "(tools/sync-report.json) is committed.")
        run(["git", "commit", "-q", "-m", message], check=False)
        if args.push and not push(verify=builds if build_ok else None):
            report = sync_report.new_report(target)
            report["sync_error"] = ("git push failed: main moved during the sync and the sync commit did not rebase "
                                    "cleanly or did not build on top of it. Nothing was pushed; the next sync tries "
                                    "again.")
            publish(report, args.issue)
            sys.exit(report["sync_error"])
    print(f"sync report: {sync_report.items_line(report)}")
    sync_report.write_step_summary("## Sync report\n\n" + sync_report.to_markdown(report))
    publish(report, args.issue)
    if not build_ok:
        sys.exit(1)


if __name__ == "__main__":
    main()
