#!/usr/bin/env python3
import argparse
import hashlib
import json
import os
import shutil
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
REPORT = HERE / "sync-report.json"
LABEL = "needs-repair"
BOT = "github-actions[bot]"
MARKER = "<!-- moblin-android-needs-repair -->"
PBSWIFT_PREFIX = "app/src/main/java/com/moblin/android/integrations/tesla/protobuf/"
SECTIONS = ("sync_error", "build", "pbswift", "held_back", "port_failures", "hooks", "hand_ported", "effects")
MAX_ATTEMPTS_PER_DAY = 2
ATTEMPT_WINDOW_HOURS = 24
NIGHT_START_HOUR = 20
NIGHT_END_HOUR = 6
MAX_BODY = 60000
MAX_TEXT = 3000
REASONS = {
    "compile": "compile errors",
    "hook": "a hook went missing",
    "dependency": "other code stopped compiling with it",
    "fallback": "the build failed in a way no single file explained",
    "stale": "not ported again yet",
}


def new_report(upstream=None):
    return {
        "upstream": upstream,
        "sync_error": None,
        "build": [],
        "pbswift": None,
        "held_back": [],
        "port_failures": [],
        "hooks": [],
        "hand_ported": [],
        "effects": [],
    }


def load(path=REPORT):
    report = new_report()
    if Path(path).exists():
        try:
            report.update(json.loads(Path(path).read_text(encoding="utf-8")))
        except ValueError:
            pass
    return report


def save(report, path=REPORT):
    Path(path).write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8", newline="\n")


def is_empty(report):
    return not any(report.get(key) for key in SECTIONS)


def count(report):
    total = 0
    for key in SECTIONS:
        value = report.get(key)
        if isinstance(value, list) and key != "build":
            total += len(value)
        elif value:
            total += 1
    return total


def failure_key(report):
    parts = [key for key in ("sync_error", "build", "pbswift") if report.get(key)]
    parts += [f"held_back:{item.get('swift')}" for item in report.get("held_back") or []]
    parts += [f"port_failures:{item.get('swift')}" for item in report.get("port_failures") or []]
    for key in ("hooks", "hand_ported", "effects"):
        parts += [f"{key}:{line.split(': ')[0]}" for line in report.get(key) or []]
    return hashlib.sha256("\n".join(sorted(set(parts))).encode("utf-8")).hexdigest()[:12]


def short(commit):
    return commit[:10] if commit else "unknown"


def tail(text, limit=MAX_TEXT):
    text = (text or "").strip()
    return text if len(text) <= limit else "..." + text[-limit:]


def stale_files(inventory, state, reverted=None, previous=None):
    by_swift = {}
    for info in (reverted or {}).values():
        for swift in info.get("swift", []):
            by_swift[swift] = info
    earlier = {item["swift"]: item for item in (previous or {}).get("held_back", []) if item.get("swift")}
    held = []
    failures = []
    current = set()
    for entry in inventory.get("files", []):
        if entry["tier"] == "skip":
            continue
        path = entry["path"]
        current.add(path)
        saved = state.get(path)
        if saved is not None and saved.get("status") != "ok":
            error = tail(str(saved.get("error", "")), 600)
            failures.append({"swift": path, "kotlin": entry["kotlin_path"], "error": error})
            continue
        if saved is not None and saved.get("sha256") == entry["sha256"]:
            continue
        info = by_swift.get(path) or earlier.get(path) or {}
        held.append({
            "swift": path,
            "kotlin": entry["kotlin_path"],
            "reason": info.get("reason", "not ported yet" if saved is None else "stale"),
            "ported_from": (saved or {}).get("moblin_commit"),
            "errors": info.get("errors", []),
            "hooks": info.get("hooks", []),
        })
    for path in sorted(set(state) - current):
        info = by_swift.get(path) or earlier.get(path) or {}
        held.append({
            "swift": path,
            "kotlin": state[path].get("kotlin_path"),
            "reason": "removed upstream, but the Kotlin is still needed",
            "ported_from": state[path].get("moblin_commit"),
            "errors": info.get("errors", []),
            "hooks": info.get("hooks", []),
        })
    held.sort(key=lambda item: item["swift"])
    failures.sort(key=lambda item: item["swift"])
    return held, failures


def hook_findings():
    import check_hooks
    import postprocess

    results, problems, _ = check_hooks.collect()
    lines = []
    for result in results:
        label = postprocess.hook_label(result["hook"])
        if result["state"] == "missing":
            lines.append(f"MISSING {label}: {result['detail']}")
        elif result["state"] == "pending":
            lines.append(f"NOT APPLIED {label} in {', '.join(result['pending'][:3])}")
    lines += [f"INVALID {problem}" for problem in problems]
    return lines


def collect(upstream, inventory, state, reverted=None, previous=None, pbswift=None, build=None, hand_ported=(),
            effects=(), hooks=None, sync_error=None):
    report = new_report(upstream)
    report["held_back"], report["port_failures"] = stale_files(inventory, state, reverted, previous)
    report["hooks"] = list(hook_findings() if hooks is None else hooks)
    report["hand_ported"] = list(hand_ported)
    report["effects"] = list(effects)
    report["build"] = list(build or [])
    report["sync_error"] = sync_error
    protobuf_reverts = sorted(rel for rel in (reverted or {}) if rel.startswith(PBSWIFT_PREFIX))
    if pbswift:
        report["pbswift"] = tail(pbswift)
    elif protobuf_reverts:
        report["pbswift"] = ("The Kotlin that tools/pbswift.py generated did not compile, so the previous files were "
                             "kept: " + ", ".join(protobuf_reverts))
    return report


def code_block(lines):
    return "```\n" + "\n".join(lines) + "\n```"


def to_markdown(report, limit=MAX_BODY):
    parts = []
    if report.get("sync_error"):
        parts.append("### The sync stopped\n\n" + code_block([tail(report["sync_error"])]))
    if report.get("build"):
        parts.append("### The app does not build\n\nThe app did not build even with every change of this sync held "
                     "back, so the sync committed nothing but `tools/sync-report.json` and the files below are not "
                     "ported yet. The build output:\n\n" + code_block(report["build"][:40]))
    if report.get("pbswift"):
        parts.append("### Tesla protobufs\n\n`tools/pbswift.py` failed, so the previously generated Kotlin in "
                     "`integrations/tesla/protobuf/` was kept. Teach `tools/pbswift.py` the new construct.\n\n"
                     + code_block([report["pbswift"]]))
    held = report.get("held_back") or []
    if held:
        lines = [f"### Held back at the previous version ({len(held)})", "",
                 "These Kotlin files still match the Swift they were last ported from, so the app builds. "
                 "The next sync ports them again.", ""]
        for item in held:
            lines.append(f"- `{item['swift']}` -> `{item['kotlin']}`: {REASONS.get(item['reason'], item['reason'])}, "
                         f"last ported from `{short(item.get('ported_from'))}`")
            details = item.get("errors", [])[:5] + item.get("hooks", [])[:3]
            if details:
                lines.append("  " + code_block(details).replace("\n", "\n  "))
        parts.append("\n".join(lines))
    failures = report.get("port_failures") or []
    if failures:
        lines = [f"### Translation failed ({len(failures)})", ""]
        lines += [f"- `{item['swift']}` -> `{item['kotlin']}`: {item['error']}" for item in failures]
        parts.append("\n".join(lines))
    for key, title in (("hooks", "Hooks"), ("hand_ported", "Swift files ported by hand changed upstream"),
                       ("effects", "Effects checks")):
        if report.get(key):
            parts.append(f"### {title} ({len(report[key])})\n\n" + code_block(report[key][:60]))
    text = "\n\n".join(parts) if parts else "Nothing to report."
    if len(text) > limit:
        text = text[:limit] + "\n\n... (truncated, see tools/sync-report.json)"
    return text


def run_url():
    server = os.environ.get("GITHUB_SERVER_URL", "https://github.com")
    repository = os.environ.get("GITHUB_REPOSITORY")
    run_id = os.environ.get("GITHUB_RUN_ID")
    return f"{server}/{repository}/actions/runs/{run_id}" if repository and run_id else None


def issue_title(report):
    return f"Android port needs repair ({count(report)} items, upstream {short(report.get('upstream'))})"


def issue_body(report, url=None):
    header = (f"{MARKER}\nThe sync with eerimoq/moblin at `{short(report.get('upstream'))}` committed everything that "
              "builds. The items below need a fix in the Android port.")
    if url:
        header += f" Sync run: {url}"
    footer = (
        "---\n\nAutomatic repair: `.github/workflows/repair.yml` works on this issue with Claude after every sync and "
        f"every 4 hours, preferably between {NIGHT_START_HOUR:02d}:00 and {NIGHT_END_HOUR:02d}:00 UTC, with at most "
        f"{MAX_ATTEMPTS_PER_DAY} attempts in any {ATTEMPT_WINDOW_HOURS} hours, runs that hit Claude's usage limit "
        "included. After 4 attempts in a row that do not fix the same items it waits 1, 2, 4 and then 7 days between "
        "attempts, until the items change or a repair pushes. When the usage limit is reached it stops without "
        "changes, comments here when the limit resets and waits until then. It pushes only when check_hooks, pbswift "
        "--check, the build and the unit tests pass. The issue closes by itself when a sync or a repair leaves "
        "nothing to report. The machine-readable report is `tools/sync-report.json`."
    )
    room = MAX_BODY - len(header) - len(footer) - 10
    return f"{header}\n\n{to_markdown(report, room)}\n\n{footer}\n"


def write_step_summary(text):
    path = os.environ.get("GITHUB_STEP_SUMMARY")
    if path:
        with open(path, "a", encoding="utf-8") as summary:
            summary.write(text.rstrip() + "\n\n")


class GitHub:
    def __init__(self, repository=None):
        self.repository = repository or os.environ.get("GITHUB_REPOSITORY")

    @staticmethod
    def available():
        return bool(shutil.which("gh") and (os.environ.get("GH_TOKEN") or os.environ.get("GITHUB_TOKEN"))
                    and os.environ.get("GITHUB_REPOSITORY"))

    def gh(self, *arguments, stdin=None, check=True):
        result = subprocess.run(["gh", *arguments], input=stdin, capture_output=True, text=True, encoding="utf-8",
                                errors="replace")
        if check and result.returncode != 0:
            raise RuntimeError(f"gh {arguments[0]} failed: {result.stderr.strip()[-500:]}")
        return result.stdout

    def api(self, path, method="GET", fields=None):
        arguments = ["api", "-X", method, f"repos/{self.repository}/{path}"]
        stdin = None
        if fields is not None:
            arguments += ["--input", "-"]
            stdin = json.dumps(fields)
        output = self.gh(*arguments, stdin=stdin)
        return json.loads(output) if output.strip() else None

    def open_issue(self):
        return own_issue(self.api(f"issues?labels={LABEL}&state=open&sort=created&direction=asc&per_page=100") or [])

    def ensure_label(self):
        self.gh("label", "create", LABEL, "--repo", self.repository, "--color", "d93f0b", "--force",
                "--description", "The automatic sync with eerimoq/moblin left something to repair")

    def create_issue(self, title, body):
        return self.api("issues", "POST", {"title": title, "body": body, "labels": [LABEL]})["number"]

    def edit_issue(self, number, title, body):
        self.api(f"issues/{number}", "PATCH", {"title": title, "body": body})

    def close_issue(self, number, comment):
        self.comment(number, comment)
        self.api(f"issues/{number}", "PATCH", {"state": "closed", "state_reason": "completed"})

    def comment(self, number, body):
        return self.api(f"issues/{number}/comments", "POST", {"body": body})["id"]

    def edit_comment(self, comment_id, body):
        self.api(f"issues/comments/{comment_id}", "PATCH", {"body": body})

    def comments(self, number):
        output = self.gh("api", "--paginate", f"repos/{self.repository}/issues/{number}/comments", "--jq",
                         ".[] | {id, body, created_at, user: .user.login}")
        return [json.loads(line) for line in output.splitlines() if line.strip()]

    def runs(self, workflow):
        try:
            output = self.gh("run", "list", "--repo", self.repository, "--workflow", workflow, "--limit", "20",
                             "--json", "databaseId,status,event")
        except RuntimeError as error:
            print(f"warning: {error}")
            return []
        return json.loads(output or "[]")


def own_issue(issues):
    for issue in issues:
        if "pull_request" in issue or (issue.get("user") or {}).get("login") != BOT:
            continue
        return {"number": issue["number"], "title": issue.get("title", ""), "createdAt": issue["created_at"],
                "url": issue.get("html_url", ""), "body": issue.get("body") or ""}
    return None


def mark_published():
    path = os.environ.get("GITHUB_OUTPUT")
    if path:
        with open(path, "a", encoding="utf-8") as output:
            output.write("published=true\n")


def publish(report, github=None, url=None, log=print):
    github = github or GitHub()
    issue = github.open_issue()
    if is_empty(report):
        if issue:
            github.close_issue(issue["number"], f"Nothing is left to report for upstream "
                               f"`{short(report.get('upstream'))}`, closing.{' ' + url if url else ''}")
            log(f"closed issue #{issue['number']}")
        return None
    github.ensure_label()
    title = issue_title(report)
    body = issue_body(report, url)
    if issue:
        github.edit_issue(issue["number"], title, body)
        log(f"updated issue #{issue['number']}")
        return issue["number"]
    number = github.create_issue(title, body)
    log(f"opened issue #{number}")
    return number


def publish_or_print(report, enabled, log=print):
    if not is_empty(report):
        log("\n" + to_markdown(report))
    if not enabled:
        return None
    if os.environ.get("CLAUDE_CODE_ACTION") == "1":
        log("inside the Claude repair session, so the needs-repair issue is left to the workflow")
        return None
    if not GitHub.available():
        log("gh, GITHUB_TOKEN or GITHUB_REPOSITORY is missing, so the needs-repair issue is not updated")
        return None
    number = publish(report, url=run_url(), log=log)
    mark_published()
    return number


def items_line(report):
    if is_empty(report):
        return "nothing to report"
    names = [key.replace("_", " ") for key in SECTIONS if report.get(key)]
    return f"{count(report)} items ({', '.join(names)})"


def main():
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    parser = argparse.ArgumentParser(
        description="Print tools/sync-report.json as Markdown, or open the needs-repair issue for a sync that stopped "
        "before it could write its report."
    )
    parser.add_argument("--sync-failed", metavar="MESSAGE", default=None,
                        help="open or update the needs-repair issue with this message (needs gh and GITHUB_TOKEN)")
    args = parser.parse_args()
    if args.sync_failed is None:
        print(to_markdown(load()))
        return
    report = new_report(load().get("upstream"))
    report["sync_error"] = args.sync_failed
    try:
        publish_or_print(report, True)
    except Exception as error:
        sys.exit(f"could not update the needs-repair issue: {error}")


if __name__ == "__main__":
    main()
