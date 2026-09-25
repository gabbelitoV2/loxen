#!/usr/bin/env python3
import argparse
import json
import os
import re
import secrets
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ElementTree
from datetime import datetime, timedelta, timezone
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
sys.path.insert(0, str(HERE))
import sync_report
import usage_limit

PROMPT = HERE / "prompts" / "repair.md"
KNOWN_FAILURES = "tools/known_test_failures.json"
TEST_RESULTS = ROOT / "app/build/test-results/testDebugUnitTest"
EXECUTION_FILE = "claude-execution-output.json"
BOT = sync_report.BOT
ATTEMPT_RE = re.compile(r"<!-- repair-attempt date=(\d{4}-\d{2}-\d{2})(?: report=(\w+))? -->")
LIMIT_RE = re.compile(r"<!-- repair-limit until=(\S+) -->")
PUSHED_MARKER = "<!-- repair-pushed -->"
FREE_ATTEMPTS = 4
LONGEST_BACKOFF = timedelta(days=7)
ACTIVE = {"queued", "in_progress", "waiting", "pending", "requested"}
TEST_CRASH_RE = re.compile(r"Could not complete execution for Gradle Test Executor|finished with non-zero exit value|"
                           r"Test process encountered an unexpected problem")
GUARDED = (
    ".github/", ".gitmodules", "tools/repair.py", "tools/sync_report.py", "tools/usage_limit.py",
    "tools/known_test_failures.json", "tools/check_hooks.py", "tools/prompts/repair.md", "tools/tests/",
)
IGNORED_CHANGES = {"tools/inventory.json", "PORT-REPORT.md", "tools/sync-report.json"}
PROMPT_REPORT_LIMIT = 40000
UNKNOWN_RESET_WAIT = timedelta(hours=4)
MINIMUM_RESET_WAIT = timedelta(minutes=30)
DAYTIME_RETRY = timedelta(hours=24)


def utcnow():
    return datetime.now(timezone.utc)


def parse_time(text):
    moment = datetime.fromisoformat(text.replace("Z", "+00:00"))
    return moment if moment.tzinfo else moment.replace(tzinfo=timezone.utc)


def stockholm(moment):
    zone = usage_limit.zone_named("Europe/Stockholm")
    if zone is timezone.utc:
        return f"{moment:%Y-%m-%d %H:%M} UTC"
    return f"{moment:%Y-%m-%d %H:%M} UTC ({moment.astimezone(zone):%H:%M} in Stockholm)"


def is_night(now):
    return now.hour >= sync_report.NIGHT_START_HOUR or now.hour < sync_report.NIGHT_END_HOUR


def failed_in_a_row(attempts, report_key):
    streak = 0
    for _, key, pushed, limited in sorted(attempts, key=lambda attempt: attempt[0], reverse=True):
        if pushed or key != report_key:
            break
        if not limited:
            streak += 1
    return streak


def repair_state(comments, now, report_key=None):
    window = timedelta(hours=sync_report.ATTEMPT_WINDOW_HOURS)
    attempts = []
    last = None
    limited_until = None
    for comment in comments:
        if comment.get("user") != BOT:
            continue
        body = comment.get("body") or ""
        attempt = ATTEMPT_RE.search(body)
        untils = LIMIT_RE.findall(body)
        if not attempt and not untils:
            continue
        created = parse_time(comment["created_at"])
        last = max(last, created) if last else created
        if attempt:
            attempts.append((created, attempt.group(2), PUSHED_MARKER in body, bool(untils)))
        for until in untils:
            try:
                moment = parse_time(until)
            except ValueError:
                continue
            limited_until = max(limited_until, moment) if limited_until else moment
    recent = sorted(created for created, _, _, _ in attempts if now - created < window)
    cap = sync_report.MAX_ATTEMPTS_PER_DAY
    next_attempt = recent[-cap] + window if len(recent) >= cap else None
    streak = failed_in_a_row(attempts, report_key) if report_key else 0
    backoff_until = None
    if streak >= FREE_ATTEMPTS:
        latest = max(created for created, _, _, _ in attempts)
        backoff_until = latest + min(timedelta(days=2 ** (streak - FREE_ATTEMPTS)), LONGEST_BACKOFF)
    return {"recent_attempts": len(recent), "next_attempt": next_attempt, "last": last,
            "limited_until": limited_until, "failed_in_a_row": streak, "backoff_until": backoff_until}


def blocked_reason(state, now):
    if state["limited_until"] and now < state["limited_until"]:
        return f"waiting for Claude's usage limit to reset at {stockholm(state['limited_until'])}"
    if state["next_attempt"] and now < state["next_attempt"]:
        return (f"{state['recent_attempts']} repair attempts in the last {sync_report.ATTEMPT_WINDOW_HOURS} hours "
                f"already, at most {sync_report.MAX_ATTEMPTS_PER_DAY}; the next one can start after "
                f"{stockholm(state['next_attempt'])}")
    if state["backoff_until"] and now < state["backoff_until"]:
        return (f"{state['failed_in_a_row']} attempts in a row did not fix the same items; the next one can start "
                f"after {stockholm(state['backoff_until'])}")
    return None


def decide(now, event, issue, comments, force=False, busy=None, report_key=None):
    if issue is None:
        return False, "there is no open needs-repair issue"
    if busy:
        return False, busy
    if force:
        return True, "forced by hand"
    state = repair_state(comments, now, report_key)
    blocked = blocked_reason(state, now)
    if blocked:
        return False, blocked
    if event == "workflow_dispatch":
        return True, "started by hand"
    if is_night(now):
        return True, "night hours"
    last = state["last"] or parse_time(issue["createdAt"])
    if now - last >= DAYTIME_RETRY:
        return True, "no repair attempt for 24 hours"
    return False, (f"daytime; the repair runs between {sync_report.NIGHT_START_HOUR:02d}:00 and "
                   f"{sync_report.NIGHT_END_HOUR:02d}:00 UTC unless nothing was tried for 24 hours")


def busy_reason(github, own_run_id):
    for run in github.runs("android.yml"):
        if run.get("status") in ACTIVE and run.get("event") != "push":
            return "a sync is running; the repair is triggered again when it completes"
    for run in github.runs("repair.yml"):
        if run.get("status") in ACTIVE and int(run.get("databaseId", 0)) < int(own_run_id or 0):
            return "an earlier repair run is still going"
    return None


def write_outputs(values):
    path = os.environ.get("GITHUB_OUTPUT")
    lines = []
    for key, value in values.items():
        value = str(value)
        if "\n" in value:
            delimiter = f"EOF_{secrets.token_hex(8)}"
            lines.append(f"{key}<<{delimiter}\n{value}\n{delimiter}")
        else:
            lines.append(f"{key}={value}")
    if path:
        with open(path, "a", encoding="utf-8") as output:
            output.write("\n".join(lines) + "\n")
    else:
        for key, value in values.items():
            if key != "prompt":
                print(f"{key}={value}")


def git(*arguments, check=False):
    result = subprocess.run(["git", *arguments], cwd=ROOT, capture_output=True, text=True, encoding="utf-8",
                            errors="replace")
    if check and result.returncode != 0:
        raise RuntimeError(f"git {' '.join(arguments)} failed: {result.stderr.strip()}")
    return result


def file_at(commit, path):
    result = git("show", f"{commit}:{path}")
    return result.stdout if result.returncode == 0 else None


def report_at(commit):
    text = file_at(commit, "tools/sync-report.json") if commit else None
    if text is None:
        return sync_report.load()
    report = sync_report.new_report()
    report.update(json.loads(text))
    return report


def known_failures(base):
    text = file_at(base, KNOWN_FAILURES) if base else None
    if text is None:
        path = ROOT / KNOWN_FAILURES
        text = path.read_text(encoding="utf-8") if path.exists() else "{}"
    return set(json.loads(text).get("tests", []))


def assemble_prompt(report, issue=None, template=None):
    template = template if template is not None else PROMPT.read_text(encoding="utf-8")
    number = f" #{issue['number']}" if issue else ""
    if sync_report.is_empty(report) and issue and issue.get("body"):
        details = issue["body"].replace(sync_report.MARKER, "").strip()[:PROMPT_REPORT_LIMIT]
    else:
        details = sync_report.to_markdown(report, PROMPT_REPORT_LIMIT)
    report_part = (
        f"# The report of issue{number}\n\n"
        f"Upstream commit: `{report.get('upstream') or 'unknown'}` (checked out in `.upstream/`). "
        "The machine-readable report is `tools/sync-report.json`.\n\n" + details
    )
    return template.rstrip() + "\n\n" + report_part + "\n"


def gate(args):
    now = utcnow()
    github = sync_report.GitHub()
    issue = github.open_issue()
    busy = busy_reason(github, os.environ.get("GITHUB_RUN_ID"))
    if args.mode == "verify-only":
        run, reason = (False, busy) if busy else (True, "verify only")
    else:
        comments = github.comments(issue["number"]) if issue else []
        report_key = sync_report.failure_key(sync_report.load())
        run, reason = decide(now, args.event, issue, comments, args.force, busy, report_key)
    print(f"{'run' if run else 'skip'}: {reason}")
    sync_report.write_step_summary(f"## Repair gate\n\n{'Running' if run else 'Skipped'}: {reason}")
    write_outputs({"run": "true" if run else "false", "issue": issue["number"] if issue else "", "reason": reason})


def prompt(args):
    if args.report and not args.report.exists():
        sys.exit(f"{args.report} does not exist")
    report = sync_report.load(args.report) if args.report else sync_report.load()
    issue = {"number": args.issue, "body": ""} if args.issue else None
    text = assemble_prompt(report, issue)
    if args.out:
        Path(args.out).write_text(text, encoding="utf-8", newline="\n")
        print(f"wrote {args.out} ({len(text):,} characters)")
    else:
        print(text)


def latest_synced_commit():
    import sync

    state = sync.load_state()
    entries = [entry for entry in state.values() if entry.get("moblin_commit") and entry.get("ported_at")]
    return max(entries, key=lambda entry: entry["ported_at"])["moblin_commit"] if entries else None


def upstream(args):
    import sync

    report = sync_report.load()
    sync.ensure_upstream(None)
    commit = report.get("upstream") or latest_synced_commit() or sync.git("rev-parse", "origin/main").stdout.strip()
    sync.git("checkout", "--quiet", "--detach", commit)
    sync.run([sys.executable, HERE / "inventory.py", "--moblin", sync.UPSTREAM])
    print(f"upstream checked out at {commit}")


def attempt_header(date, number=None, report_key=None):
    count = (f" {number} of {sync_report.MAX_ATTEMPTS_PER_DAY} in {sync_report.ATTEMPT_WINDOW_HOURS} hours"
             if number else "")
    key = f" report={report_key}" if report_key else ""
    url = sync_report.run_url()
    return f"<!-- repair-attempt date={date}{key} -->\nRepair attempt{count}" + (f" ({url})" if url else "")


def start(args):
    now = utcnow()
    github = sync_report.GitHub()
    issue = github.open_issue()
    if issue is None:
        print("the needs-repair issue was closed in the meantime, nothing to do")
        write_outputs({"go": "false"})
        return
    report = sync_report.load()
    report_key = sync_report.failure_key(report)
    state = repair_state(github.comments(issue["number"]), now, report_key)
    blocked = None if args.force else blocked_reason(state, now)
    if blocked:
        print(f"not starting: {blocked}")
        sync_report.write_step_summary(f"## Repair\n\nNot started: {blocked}")
        write_outputs({"go": "false"})
        return
    number = state["recent_attempts"] + 1
    text = assemble_prompt(report, issue)
    header = attempt_header(now.date().isoformat(), number, report_key)
    comment_id = github.comment(issue["number"], header + ": started.")
    print(f"repair attempt {number} for issue #{issue['number']}, prompt of {len(text):,} characters")
    write_outputs({"go": "true", "issue": issue["number"], "comment": comment_id, "attempt": number,
                   "key": report_key, "prompt": text})


def execution_file(path=None):
    return Path(path) if path else Path(os.environ.get("RUNNER_TEMP", ".")) / EXECUTION_FILE


def post(github, issue, comment_id, body):
    if github is None or not issue:
        print(body)
        return
    if comment_id:
        github.edit_comment(comment_id, body)
    else:
        github.comment(issue, body)


def limit(args):
    now = utcnow()
    messages = usage_limit.load_messages(execution_file(args.execution_file))
    result = usage_limit.detect(messages, args.outcome == "failure", now)
    if not result["limited"]:
        print(f"no usage limit (Claude step {args.outcome})")
        write_outputs({"limited": "false"})
        return
    until = result["resets_at"] or now + UNKNOWN_RESET_WAIT
    until = max(until, now + MINIMUM_RESET_WAIT).astimezone(timezone.utc).replace(microsecond=0)
    known = "" if result["resets_at"] else " (the reset time was not given, so it waits 4 hours)"
    body = (
        f"{attempt_header(now.date().isoformat(), args.attempt, args.report_key)}: Claude's usage limit was "
        f"reached, so the repair stopped without pushing anything. It resets at {stockholm(until)}{known}; the "
        "repair waits until then. "
        f"This run counts as one of the {sync_report.MAX_ATTEMPTS_PER_DAY} attempts in "
        f"{sync_report.ATTEMPT_WINDOW_HOURS} hours.\n<!-- repair-limit until={until.isoformat()} -->\n\n"
        "Evidence: " + "; ".join(result["evidence"])[:500]
    )
    github = sync_report.GitHub() if sync_report.GitHub.available() else None
    post(github, args.issue, args.comment, body)
    print(f"usage limit reached, waiting until {until.isoformat()}")
    sync_report.write_step_summary("## Repair\n\n" + body)
    write_outputs({"limited": "true", "until": until.isoformat()})


def command_gate(name, command):
    result = subprocess.run([str(part) for part in command], cwd=ROOT, capture_output=True, text=True,
                            encoding="utf-8", errors="replace")
    output = result.stdout + result.stderr
    return {"name": name, "ok": result.returncode == 0, "detail": sync_report.tail(output, 1500)}, output


def gradle_gate(name, *tasks):
    import sync

    ok, output = sync.gradle(*tasks)
    return {"name": name, "ok": ok, "detail": "" if ok else sync_report.tail(output, 2500)}


def test_results(directory=TEST_RESULTS):
    total = 0
    failures = {}
    for path in sorted(Path(directory).glob("*.xml")):
        for case in ElementTree.parse(path).getroot().iter("testcase"):
            total += 1
            if case.find("failure") is not None or case.find("error") is not None:
                failures[f"{case.get('classname')}.{case.get('name')}"] = case.get("classname")
    return total, failures


def run_tests(classes=()):
    import sync

    shutil.rmtree(TEST_RESULTS, ignore_errors=True)
    filters = [part for name in classes for part in ("--tests", name)]
    ok, output = sync.gradle(":app:testDebugUnitTest", "--continue", *filters)
    total, failures = test_results()
    return total, failures, output, ok


def changed_paths(base):
    git("add", "-A")
    output = git("diff", "--cached", "--no-renames", "--name-status", base).stdout
    entries = []
    for line in output.splitlines():
        status, _, path = line.partition("\t")
        if path:
            entries.append((status, path))
    return entries


def added_lines(diff):
    path = None
    for line in diff.splitlines():
        if line.startswith("+++ "):
            path = line[6:] if line.startswith("+++ b/") else None
        elif line.startswith("+") and path:
            yield path, line[1:]


def diff_problems(entries, diff):
    problems = []
    for status, path in entries:
        if path.startswith(GUARDED) or path in GUARDED:
            problems.append(f"{path}: the repair must not change this file")
        if status.startswith("D") and path.startswith("app/src/test/"):
            problems.append(f"{path}: a unit test was deleted")
        if status.startswith("A") and not path.startswith(("app/", "tools/")):
            problems.append(f"{path}: new files belong under app/ or tools/")
    for path, line in added_lines(diff):
        stripped = line.strip()
        if path.endswith((".kt", ".kts")) and stripped.startswith(("//", "/*")):
            problems.append(f"{path}: comment added: {stripped[:120]}")
        if path.endswith(".py") and stripped.startswith("#") and not stripped.startswith("#!"):
            problems.append(f"{path}: comment added: {stripped[:120]}")
        if path.startswith("app/src/test/") and re.search(r"@(?:Ignore|Disabled)\b", line):
            problems.append(f"{path}: a unit test was disabled: {stripped[:120]}")
    return problems


def failing_at_base(tests):
    before = len(git("stash", "list").stdout.splitlines())
    git("stash", "push", "--include-untracked", "-q", "-m", "repair-verify")
    if len(git("stash", "list").stdout.splitlines()) == before:
        return set()
    try:
        _, failures, _, _ = run_tests(sorted(set(tests.values())))
    finally:
        git("stash", "pop", "-q", check=True)
    return set(tests) & set(failures)


def failing_again(tests):
    _, failures, _, _ = run_tests(sorted(set(tests.values())))
    return {name: classname for name, classname in tests.items() if name in failures}


def tests_gate(base, has_changes, runner=run_tests, rerun=failing_again, at_base=failing_at_base):
    known = known_failures(base)
    total, failures, output, ok = runner()
    if total == 0:
        return {"name": "unit tests", "ok": False, "detail": "no test results\n" + sync_report.tail(output, 2500)}
    if TEST_CRASH_RE.search(output) or (not ok and not failures):
        return {"name": "unit tests", "ok": False,
                "detail": "the test task failed without reporting every test (a crashed test process?)\n"
                + sync_report.tail(output, 2500)}
    unexpected = {name: classname for name, classname in failures.items() if name not in known}
    repeated = rerun(unexpected) if unexpected else {}
    flaky = sorted(set(unexpected) - set(repeated))
    pre_existing = at_base(repeated) if repeated and has_changes else set()
    new = sorted(set(repeated) - pre_existing)
    lines = [f"{total} tests, {len(failures)} failed, {len(set(failures) & known)} of them known failures"]
    lines += [f"new failure: {name}" for name in new]
    lines += [f"fails on main too (not caused by the repair): {name}" for name in sorted(pre_existing)]
    lines += [f"failed once, passed when run again: {name}" for name in flaky]
    return {"name": "unit tests", "ok": not new, "detail": "\n".join(lines)}


def verify(base, base_report):
    import sync

    entries = changed_paths(base)
    diff = git("diff", "--cached", "--no-renames", "-U0", base).stdout
    problems = diff_problems(entries, diff)
    gates = [{"name": "changes", "ok": not problems, "detail": "\n".join(problems[:40])}]
    hooks, _ = command_gate("check_hooks", [sys.executable, HERE / "check_hooks.py"])
    gates.append(hooks)
    protobufs, protobufs_output = command_gate(
        "pbswift --check", [sys.executable, HERE / "pbswift.py", "--moblin", sync.UPSTREAM, "--check"]
    )
    gates.append(protobufs)
    build = gradle_gate("assembleDebug", ":app:assembleDebug")
    gates.append(build)
    meaningful = [path for _, path in entries if path not in IGNORED_CHANGES]
    if build["ok"]:
        gates.append(tests_gate(base, bool(meaningful)))
    inventory = json.loads((HERE / "inventory.json").read_text(encoding="utf-8"))
    report = sync_report.collect(
        base_report.get("upstream"), inventory, sync.load_state(), previous=base_report,
        pbswift=None if protobufs["ok"] else protobufs_output,
        build=[] if build["ok"] else build["detail"].splitlines()[-40:],
        hand_ported=sync.warn_hand_ported(), effects=sync.warn_effect_checks(),
    )
    return gates, report, meaningful


def gates_markdown(gates):
    lines = ["| check | result |", "|---|---|"]
    lines += [f"| {gate['name']} | {'ok' if gate['ok'] else 'FAILED'} |" for gate in gates]
    for gate in gates:
        if gate["detail"] and (not gate["ok"] or gate["name"] == "unit tests"):
            lines += ["", f"{gate['name']}:", sync_report.code_block([gate["detail"]])]
    return "\n".join(lines)


def verify_only(args):
    base = args.base or git("rev-parse", "HEAD").stdout.strip()
    gates, report, _ = verify(base, report_at(base))
    text = "## Verification of main\n\n" + gates_markdown(gates) + "\n\nReport: " + sync_report.items_line(report)
    print(text)
    sync_report.write_step_summary(text)
    if not all(gate["ok"] for gate in gates):
        sys.exit(1)


def commit_and_push(message):
    import sync

    git("add", "-A")
    process = subprocess.run(["git", "-c", "core.hooksPath=/dev/null", "commit", "-q", "-F", "-"], cwd=ROOT,
                             input=message, capture_output=True, text=True, encoding="utf-8", errors="replace")
    if process.returncode != 0:
        raise RuntimeError(f"git commit failed: {process.stderr.strip()}")
    if not sync.push():
        return None
    return git("rev-parse", "HEAD").stdout.strip()


def drop_stray_files():
    output = git("ls-files", "--others", "--exclude-standard").stdout
    stray = [path for path in output.splitlines() if path and not path.startswith(("app/", "tools/"))]
    for path in stray:
        (ROOT / path).unlink(missing_ok=True)
    return stray


def finish(args):
    now = utcnow()
    github = sync_report.GitHub() if sync_report.GitHub.available() else None
    base_report = report_at(args.base)
    messages = usage_limit.load_messages(execution_file(args.execution_file))
    claude = usage_limit.summary(messages).strip()
    if not messages and args.outcome == "failure":
        claude = ("Claude did not start: the claude-code-action step failed before any output. Check the run log and "
                  "the CLAUDE_CODE_OAUTH_TOKEN secret.")
    stray = drop_stray_files()
    gates, report, meaningful = verify(args.base, base_report)
    header = attempt_header(now.date().isoformat(), args.attempt, args.report_key)
    claude_part = ("\n\nClaude's summary:\n\n" + sync_report.tail(claude, 2500)) if claude else ""
    if stray:
        claude_part += "\n\nScratch files left outside app/ and tools/ were removed: " + ", ".join(stray[:20])
    ok = all(gate["ok"] for gate in gates)
    if not ok:
        body = f"{header}: verification failed, nothing was pushed (Claude step {args.outcome}).\n\n"
        body += gates_markdown(gates) + claude_part
        post(github, args.issue, args.comment, body)
        sync_report.write_step_summary("## Repair\n\n" + body)
        return
    if not meaningful:
        if sync_report.is_empty(report) and not sync_report.is_empty(base_report):
            body = f"{header}: main verifies and nothing is left to report, closing."
            post(github, args.issue, args.comment, body)
            if github:
                sync_report.publish(report, github)
        elif sync_report.is_empty(report):
            body = (f"{header}: Claude made no changes (Claude step {args.outcome}) and main verifies. The issue "
                    "comes from a sync that stopped, so it stays open until the next sync finishes with nothing to "
                    f"report.{claude_part}")
            post(github, args.issue, args.comment, body)
        else:
            body = (f"{header}: Claude made no changes (Claude step {args.outcome}); "
                    f"{sync_report.items_line(report)} remain.{claude_part}")
            post(github, args.issue, args.comment, body)
        sync_report.write_step_summary("## Repair\n\n" + body)
        return
    before = sync_report.count(base_report)
    after = sync_report.count(report)
    sync_report.save(report)
    message = (f"Repair the Android port after the sync with eerimoq/moblin {sync_report.short(report.get('upstream'))}"
               f" ({before} report items before, {after} after).")
    if claude:
        message += "\n\n" + sync_report.tail(claude, 2000)
    sha = commit_and_push(message)
    if sha is None:
        body = (f"{header}: verification passed, but main moved and the repair did not rebase cleanly or did not "
                "build on top of it, so nothing was pushed.")
    elif sync_report.is_empty(report):
        body = f"{header}: pushed {sha}. Everything verifies and nothing is left to report, closing.\n{PUSHED_MARKER}"
    else:
        body = (f"{header}: pushed {sha}; {sync_report.items_line(report)} remain (the issue text is updated)."
                f"\n{PUSHED_MARKER}")
    body += "\n\n" + gates_markdown(gates) + claude_part
    post(github, args.issue, args.comment, body)
    if sha and github:
        sync_report.publish(report, github, sync_report.run_url())
    sync_report.write_step_summary("## Repair\n\n" + body)


def main():
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    parser = argparse.ArgumentParser(description="Drive the automatic repair of the Android port (.github/workflows/"
                                     "repair.yml).")
    commands = parser.add_subparsers(dest="command", required=True)
    command = commands.add_parser("gate", help="decide whether this workflow run should try a repair")
    command.add_argument("--event", default=os.environ.get("GITHUB_EVENT_NAME", "workflow_dispatch"))
    command.add_argument("--mode", choices=["repair", "verify-only"], default="repair")
    command.add_argument("--force", action="store_true")
    command.set_defaults(handler=gate)
    command = commands.add_parser("prompt", help="print the prompt Claude gets, for a dry run")
    command.add_argument("--report", type=Path, default=None)
    command.add_argument("--issue", default=None)
    command.add_argument("--out", type=Path, default=None)
    command.set_defaults(handler=prompt)
    command = commands.add_parser("upstream", help="check out the upstream commit of the report in .upstream")
    command.set_defaults(handler=upstream)
    command = commands.add_parser("start", help="post the attempt comment and write the prompt as a step output")
    command.add_argument("--force", action="store_true")
    command.set_defaults(handler=start)
    command = commands.add_parser("limit", help="stop and wait when Claude hit its usage limit")
    command.add_argument("--issue", default=None)
    command.add_argument("--comment", default=None)
    command.add_argument("--attempt", default=None)
    command.add_argument("--report-key", default=None)
    command.add_argument("--outcome", default="success")
    command.add_argument("--execution-file", default=None)
    command.set_defaults(handler=limit)
    command = commands.add_parser("verify", help="run the checks a repair must pass, without pushing")
    command.add_argument("--base", default=None)
    command.set_defaults(handler=verify_only)
    command = commands.add_parser("finish", help="verify, then commit, push and update the issue")
    command.add_argument("--base", required=True)
    command.add_argument("--issue", default=None)
    command.add_argument("--comment", default=None)
    command.add_argument("--attempt", default=None)
    command.add_argument("--report-key", default=None)
    command.add_argument("--outcome", default="success")
    command.add_argument("--execution-file", default=None)
    command.set_defaults(handler=finish)
    args = parser.parse_args()
    if os.environ.get("CLAUDE_CODE_ACTION") == "1" and args.command != "prompt":
        sys.exit(f"repair.py {args.command} is run by the workflow, not from inside the Claude session")
    args.handler(args)


if __name__ == "__main__":
    main()
