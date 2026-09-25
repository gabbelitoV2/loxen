#!/usr/bin/env python3
import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import postprocess


def collect(tasks=None):
    hooks, problems, warnings = postprocess.load_hooks()
    if tasks:
        wanted = set(tasks)
        hooks = [hook for hook in hooks if hook["_task"] in wanted or Path(hook["_source"]).stem in wanted]
    contents = {}
    root = postprocess.KOTLIN_ROOT
    if root.exists():
        for path in sorted(root.rglob("*.kt")):
            if postprocess.is_pbswift_output(path):
                continue
            rel = path.relative_to(root).as_posix()
            contents[rel] = path.read_text(encoding="utf-8", errors="replace")
    return postprocess.apply_hooks(contents, hooks, apply=False), problems, warnings


def main():
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    parser = argparse.ArgumentParser(
        description="Check that every one-line hook in tools/hooks/*.json is in place in the generated Kotlin files. "
        "Exits with 1 when a hook is missing, not applied yet or invalid."
    )
    parser.add_argument("--task", nargs="+", default=None, help="check only the hooks of these tasks, for example T4")
    parser.add_argument("--verbose", action="store_true", help="list every hook, not only the problems")
    args = parser.parse_args()

    results, problems, warnings = collect(args.task)
    counts = postprocess.report_hooks(results, problems, warnings, args.verbose, pending_label="not applied")
    if counts["missing"] or counts["pending"] or problems:
        if counts["missing"]:
            print("A missing hook means the generated line under it changed. Re-derive it, see tools/hooks/README.md.")
        if counts["pending"]:
            print("Run python tools/postprocess.py to apply the hooks that are not applied yet.")
        sys.exit(1)


if __name__ == "__main__":
    main()
