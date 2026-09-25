import json
import os
import shutil
import subprocess
import sys
import tempfile
import unittest
from datetime import datetime, timedelta, timezone
from pathlib import Path
from unittest import mock

TOOLS = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS))
import repair
import sync_report

FIXTURES = Path(__file__).resolve().parent / "fixtures"
NIGHT = datetime(2026, 9, 25, 22, 30, tzinfo=timezone.utc)
DAY = datetime(2026, 9, 25, 12, 17, tzinfo=timezone.utc)
ISSUE = {"number": 7, "createdAt": "2026-09-25T03:40:00Z", "body": ""}


def attempt(date, created, user=repair.BOT):
    return {"user": user, "created_at": created, "body": f"<!-- repair-attempt date={date} -->\nRepair attempt: done."}


def report_with_items():
    report = sync_report.new_report("afa485d231cb21bbf41a69eb2a3c408f612d8490")
    report["held_back"] = [{
        "swift": "Moblin/Various/Model.swift",
        "kotlin": "app/src/main/java/com/moblin/android/various/Model.kt",
        "reason": "compile",
        "ported_from": "c861d40e79a6198cbded1604ce80c3de3707b035",
        "errors": ["5: Unresolved reference 'missingHelper'."],
        "hooks": [],
    }]
    report["hooks"] = ["MISSING H4.1 (T4.json) media/haishinkit/media/video/VideoUnit.kt [fun attachDefault]: "
                       "neither the old nor the new line was found"]
    return report


class DecideSuite(unittest.TestCase):
    def test_no_issue(self):
        self.assertEqual(repair.decide(NIGHT, "schedule", None, []), (False, "there is no open needs-repair issue"))

    def test_busy_wins_even_when_forced(self):
        run, reason = repair.decide(NIGHT, "workflow_dispatch", ISSUE, [], force=True, busy="a sync is running")
        self.assertFalse(run)
        self.assertEqual(reason, "a sync is running")

    def test_night_run(self):
        self.assertTrue(repair.decide(NIGHT, "schedule", ISSUE, [])[0])
        self.assertTrue(repair.decide(datetime(2026, 9, 26, 3, 35, tzinfo=timezone.utc), "workflow_run", ISSUE, [])[0])

    def test_daytime_waits_for_the_night(self):
        comments = [attempt("2026-09-25", "2026-09-25T04:20:00Z")]
        run, reason = repair.decide(DAY, "schedule", ISSUE, comments)
        self.assertFalse(run)
        self.assertIn("daytime", reason)

    def test_daytime_retries_after_a_day_without_attempts(self):
        issue = dict(ISSUE, createdAt="2026-09-24T03:40:00Z")
        comments = [attempt("2026-09-24", "2026-09-24T04:20:00Z")]
        self.assertTrue(repair.decide(DAY, "schedule", issue, comments)[0])

    def test_cap_of_two_attempts_in_24_hours(self):
        comments = [attempt("2026-09-25", "2026-09-25T00:20:00Z"), attempt("2026-09-25", "2026-09-25T03:40:00Z")]
        run, reason = repair.decide(NIGHT, "schedule", ISSUE, comments)
        self.assertFalse(run)
        self.assertIn("at most 2", reason)
        self.assertIn("2026-09-26 00:20 UTC", reason)
        self.assertFalse(repair.decide(NIGHT, "workflow_dispatch", ISSUE, comments)[0])
        self.assertTrue(repair.decide(NIGHT, "workflow_dispatch", ISSUE, comments, force=True)[0])
        self.assertFalse(repair.decide(NIGHT + timedelta(hours=1), "schedule", ISSUE, comments)[0])
        self.assertTrue(repair.decide(NIGHT + timedelta(hours=2), "schedule", ISSUE, comments)[0])

    def test_the_cap_does_not_reset_at_midnight(self):
        comments = [attempt("2026-09-25", "2026-09-25T20:17:00Z"), attempt("2026-09-25", "2026-09-25T23:10:00Z")]
        after_midnight = datetime(2026, 9, 26, 0, 17, tzinfo=timezone.utc)
        self.assertFalse(repair.decide(after_midnight, "schedule", ISSUE, comments)[0])
        self.assertFalse(repair.decide(after_midnight + timedelta(hours=4), "schedule", ISSUE, comments)[0])
        self.assertTrue(repair.decide(datetime(2026, 9, 26, 20, 17, tzinfo=timezone.utc), "schedule", ISSUE,
                                      comments)[0])

    def test_waits_for_the_usage_limit_reset(self):
        until = NIGHT + timedelta(hours=3)
        comments = [{"user": repair.BOT, "created_at": "2026-09-25T20:20:00Z",
                     "body": f"<!-- repair-limit until={until.isoformat()} -->\nClaude's usage limit was reached"}]
        run, reason = repair.decide(NIGHT, "schedule", ISSUE, comments)
        self.assertFalse(run)
        self.assertIn("usage limit", reason)
        self.assertEqual(repair.repair_state(comments, NIGHT)["recent_attempts"], 0)
        self.assertTrue(repair.decide(until + timedelta(minutes=1), "schedule", ISSUE, comments)[0])

    def test_backs_off_after_four_attempts_that_do_not_fix_the_same_items(self):
        def failed(created, key="k1", body=""):
            return {"user": repair.BOT, "created_at": created,
                    "body": f"<!-- repair-attempt date={created[:10]} report={key} -->\nRepair attempt: failed.{body}"}

        comments = [failed("2026-09-20T21:00:00Z"), failed("2026-09-21T01:00:00Z"), failed("2026-09-22T21:00:00Z"),
                    failed("2026-09-23T01:00:00Z")]
        self.assertEqual(repair.repair_state(comments, NIGHT, "k1")["failed_in_a_row"], 4)
        run, reason = repair.decide(datetime(2026, 9, 23, 22, 0, tzinfo=timezone.utc), "schedule", ISSUE, comments,
                                    report_key="k1")
        self.assertFalse(run)
        self.assertIn("4 attempts in a row", reason)
        self.assertIn("2026-09-24 01:00 UTC", reason)
        self.assertTrue(repair.decide(datetime(2026, 9, 24, 1, 5, tzinfo=timezone.utc), "schedule", ISSUE, comments,
                                      report_key="k1")[0])
        self.assertTrue(repair.decide(datetime(2026, 9, 23, 22, 0, tzinfo=timezone.utc), "schedule", ISSUE, comments,
                                      report_key="k2")[0])
        more = comments + [failed("2026-09-24T01:05:00Z")]
        self.assertEqual(repair.repair_state(more, NIGHT, "k1")["backoff_until"],
                         datetime(2026, 9, 26, 1, 5, tzinfo=timezone.utc))
        many = more + [failed(f"2026-10-{day:02d}T01:00:00Z") for day in range(1, 6)]
        self.assertEqual(repair.repair_state(many, NIGHT, "k1")["backoff_until"],
                         datetime(2026, 10, 12, 1, 0, tzinfo=timezone.utc))
        pushed = comments[:3] + [failed("2026-09-23T01:00:00Z", body="\n" + repair.PUSHED_MARKER)]
        self.assertEqual(repair.repair_state(pushed, NIGHT, "k1")["failed_in_a_row"], 0)
        limit = "\n<!-- repair-limit until=2026-09-23T05:00Z -->"
        limited = comments[:3] + [failed("2026-09-23T01:00:00Z", body=limit)]
        self.assertEqual(repair.repair_state(limited, NIGHT, "k1")["failed_in_a_row"], 3)

    def test_markers_from_other_users_are_ignored(self):
        comments = [attempt("2026-09-25", "2026-09-25T00:20:00Z", user="someone"),
                    attempt("2026-09-25", "2026-09-25T03:40:00Z", user="someone")]
        self.assertTrue(repair.decide(NIGHT, "schedule", ISSUE, comments)[0])


class BusySuite(unittest.TestCase):
    def test_running_sync_and_earlier_repair(self):
        class Runs:
            def __init__(self, runs):
                self.data = runs

            def runs(self, workflow):
                return self.data[workflow]

        idle = {"android.yml": [{"databaseId": 1, "status": "completed"}],
                "repair.yml": [{"databaseId": 9, "status": "in_progress"}, {"databaseId": 5, "status": "completed"}]}
        self.assertIsNone(repair.busy_reason(Runs(idle), "9"))
        syncing = dict(idle, **{"android.yml": [{"databaseId": 2, "status": "in_progress"}]})
        self.assertIn("sync", repair.busy_reason(Runs(syncing), "9"))
        earlier = dict(idle, **{"repair.yml": [{"databaseId": 8, "status": "queued"},
                                               {"databaseId": 9, "status": "queued"}]})
        self.assertIn("earlier repair", repair.busy_reason(Runs(earlier), "9"))
        self.assertIsNone(repair.busy_reason(Runs(earlier), "8"))
        pushing = dict(idle, **{"android.yml": [{"databaseId": 3, "status": "in_progress", "event": "push"}]})
        self.assertIsNone(repair.busy_reason(Runs(pushing), "9"))


class PromptSuite(unittest.TestCase):
    def test_prompt_has_rules_and_report(self):
        text = repair.assemble_prompt(report_with_items(), {"number": 7, "body": ""})
        self.assertTrue(text.startswith("# Repair the Android port"))
        self.assertIn("# The report of issue #7", text)
        self.assertIn("`Moblin/Various/Model.swift` -> `app/src/main/java/com/moblin/android/various/Model.kt`", text)
        self.assertIn("missingHelper", text)
        self.assertIn("MISSING H4.1", text)
        self.assertIn("afa485d231cb21bbf41a69eb2a3c408f612d8490", text)
        self.assertLess(len(text), 100000)

    def test_prompt_falls_back_to_the_issue_text(self):
        issue = {"number": 8, "body": sync_report.MARKER + "\nThe sync stopped: command failed"}
        text = repair.assemble_prompt(sync_report.new_report(), issue)
        self.assertIn("The sync stopped: command failed", text)
        self.assertNotIn(sync_report.MARKER, text)

    def test_huge_report_is_truncated(self):
        report = report_with_items()
        report["hooks"] = [f"MISSING H{index} " + "x" * 200 for index in range(2000)]
        report["held_back"] = report["held_back"] * 500
        self.assertLess(len(repair.assemble_prompt(report, {"number": 7, "body": ""})), 50000)

    def test_prompt_command_dry_run(self):
        directory = Path(tempfile.mkdtemp())
        try:
            report = directory / "report.json"
            sync_report.save(report_with_items(), report)
            output = directory / "prompt.md"
            result = subprocess.run([sys.executable, str(TOOLS / "repair.py"), "prompt", "--report", str(report),
                                     "--issue", "7", "--out", str(output)], capture_output=True, text=True,
                                    encoding="utf-8")
            self.assertEqual(result.returncode, 0, result.stderr)
            text = output.read_text(encoding="utf-8")
            self.assertIn("Do not write code comments", text)
            self.assertIn("# The report of issue #7", text)
        finally:
            shutil.rmtree(directory, ignore_errors=True)


class VerifySuite(unittest.TestCase):
    def test_diff_problems(self):
        entries = [("M", "app/src/main/java/com/moblin/android/various/Model.kt"),
                   ("D", "app/src/test/java/com/moblin/android/various/ModelSuite.kt"),
                   ("M", ".github/workflows/android.yml"), ("M", "tools/known_test_failures.json"),
                   ("A", "tools/pbswift_helper.py")]
        diff = "\n".join([
            "--- a/app/src/main/java/com/moblin/android/various/Model.kt",
            "+++ b/app/src/main/java/com/moblin/android/various/Model.kt",
            "+    // explain the change",
            "+    val url = \"https://example.com\"",
            "+++ b/tools/pbswift_helper.py",
            "+#!/usr/bin/env python3",
            "+# helper",
            "+++ b/app/src/test/java/com/moblin/android/various/SettingsSuite.kt",
            "+    @Ignore",
        ])
        problems = repair.diff_problems(entries, diff)
        self.assertEqual(len(problems), 6, problems)
        self.assertTrue(any("unit test was deleted" in problem for problem in problems))
        self.assertTrue(any(".github/workflows/android.yml" in problem for problem in problems))
        self.assertTrue(any("known_test_failures" in problem for problem in problems))
        self.assertTrue(any("// explain the change" in problem for problem in problems))
        self.assertTrue(any("# helper" in problem for problem in problems))
        self.assertTrue(any("disabled" in problem for problem in problems))
        self.assertEqual(repair.diff_problems([("M", "tools/sync.py")], "+++ b/tools/sync.py\n+    run(port)"), [])

    def test_test_results(self):
        directory = Path(tempfile.mkdtemp())
        try:
            (directory / "TEST-a.xml").write_text(
                '<testsuite name="a" tests="3"><testcase classname="com.a.RecorderSuite" name="recordsAudioAndVideo">'
                '<failure message="boom"/></testcase><testcase classname="com.a.RecorderSuite" name="ok"/>'
                '<testcase classname="com.a.Other" name="broken[1]"><error message="x"/></testcase></testsuite>',
                encoding="utf-8")
            total, failures = repair.test_results(directory)
            self.assertEqual(total, 3)
            self.assertEqual(failures, {"com.a.RecorderSuite.recordsAudioAndVideo": "com.a.RecorderSuite",
                                        "com.a.Other.broken[1]": "com.a.Other"})
        finally:
            shutil.rmtree(directory, ignore_errors=True)

    def test_known_failures_file(self):
        known = json.loads((TOOLS / "known_test_failures.json").read_text(encoding="utf-8"))["tests"]
        self.assertEqual(len(known), 5)
        self.assertEqual(sum(1 for name in known if ".RecorderSuite." in name), 4)
        self.assertIn("com.moblin.android.media.haishinkit.mpeg.MpegTsReaderSuite.ffmpegAudioOnlyPeriodicBeep", known)

    def test_step_outputs_use_a_delimiter_for_multi_line_values(self):
        directory = Path(tempfile.mkdtemp())
        try:
            path = directory / "output"
            with mock.patch.dict(os.environ, {"GITHUB_OUTPUT": str(path)}):
                repair.write_outputs({"go": "true", "prompt": "line one\nline two"})
            lines = path.read_text(encoding="utf-8").splitlines()
            self.assertEqual(lines[0], "go=true")
            self.assertTrue(lines[1].startswith("prompt<<EOF_"))
            self.assertEqual(lines[2:4], ["line one", "line two"])
            self.assertEqual(lines[4], lines[1].split("<<")[1])
        finally:
            shutil.rmtree(directory, ignore_errors=True)


class InsideClaudeSuite(unittest.TestCase):
    def test_workflow_commands_refuse_to_run_inside_the_claude_step(self):
        environment = dict(os.environ, CLAUDE_CODE_ACTION="1")
        result = subprocess.run([sys.executable, str(TOOLS / "repair.py"), "finish", "--base", "HEAD"],
                                capture_output=True, text=True, encoding="utf-8", env=environment)
        self.assertEqual(result.returncode, 1)
        self.assertIn("not from inside the Claude session", result.stderr)

    def test_sync_does_not_push_or_touch_the_issue_inside_the_claude_step(self):
        import sync

        printed = []
        with mock.patch.dict(os.environ, {"CLAUDE_CODE_ACTION": "1"}), mock.patch("builtins.print"):
            self.assertFalse(sync.push())
            self.assertIsNone(sync_report.publish_or_print(sync_report.new_report("abc"), True, log=printed.append))
        self.assertIn("left to the workflow", printed[0])


class PushSuite(unittest.TestCase):
    def push(self, pushes, verified):
        import sync

        commands = []

        def run(command, cwd=None, check=True, capture=False):
            commands.append(" ".join(str(part) for part in command[1:3]))
            code = 0
            if command[1] == "push":
                code = pushes.pop(0)
            return subprocess.CompletedProcess(command, code, stdout="main\n", stderr="")

        with mock.patch.object(sync, "run", run), mock.patch("builtins.print"), \
                mock.patch.dict(os.environ, {"CLAUDE_CODE_ACTION": ""}):
            result = sync.push(verify=lambda: verified)
        return result, commands

    def test_a_rebased_result_that_does_not_build_is_not_pushed(self):
        result, commands = self.push([1, 0], verified=False)
        self.assertFalse(result)
        self.assertEqual(commands, ["rev-parse --abbrev-ref", "push -q", "pull --rebase"])

    def test_a_rebased_result_that_builds_is_pushed(self):
        result, commands = self.push([1, 0], verified=True)
        self.assertTrue(result)
        self.assertEqual(commands, ["rev-parse --abbrev-ref", "push -q", "pull --rebase", "push -q"])


class LimitCommandSuite(unittest.TestCase):
    def test_limit_comment_marks_the_reset_and_counts_as_an_attempt(self):
        posted = []
        outputs = {}

        class Args:
            execution_file = str(FIXTURES / "execution-limit.json")
            outcome = "failure"
            issue = "7"
            comment = "123"
            attempt = "1"
            report_key = "abc123"

        with (
            mock.patch.object(repair, "utcnow", return_value=datetime(2026, 9, 25, 13, 0, tzinfo=timezone.utc)),
            mock.patch.object(repair, "post", lambda github, issue, comment, body: posted.append(body)),
            mock.patch.object(repair, "write_outputs", outputs.update),
            mock.patch.object(sync_report.GitHub, "available", staticmethod(lambda: False)),
            mock.patch("builtins.print"),
        ):
            repair.limit(Args())
        self.assertEqual(outputs["limited"], "true")
        self.assertEqual(outputs["until"], "2026-09-25T15:00:00+00:00")
        self.assertIn("Repair attempt 1 of 2 in 24 hours", posted[0])
        self.assertIn("<!-- repair-attempt date=2026-09-25 report=abc123 -->", posted[0])
        comments = [{"user": repair.BOT, "created_at": "2026-09-25T13:00:00Z", "body": posted[0]}]
        state = repair.repair_state(comments, datetime(2026, 9, 25, 13, 5, tzinfo=timezone.utc))
        self.assertEqual(state["recent_attempts"], 1)
        self.assertEqual(state["limited_until"], datetime(2026, 9, 25, 15, 0, tzinfo=timezone.utc))
        self.assertFalse(repair.decide(datetime(2026, 9, 25, 14, 0, tzinfo=timezone.utc), "workflow_dispatch", ISSUE,
                                       comments)[0])
        self.assertTrue(repair.decide(datetime(2026, 9, 25, 15, 1, tzinfo=timezone.utc), "workflow_dispatch", ISSUE,
                                      comments)[0])

    def test_a_reset_time_in_the_past_still_waits(self):
        outputs = {}

        class Args:
            execution_file = str(FIXTURES / "execution-limit.json")
            outcome = "failure"
            issue = None
            comment = None
            attempt = None
            report_key = None

        now = datetime(2026, 9, 25, 16, 0, tzinfo=timezone.utc)
        with (
            mock.patch.object(repair, "utcnow", return_value=now),
            mock.patch.object(repair, "write_outputs", outputs.update),
            mock.patch.object(sync_report.GitHub, "available", staticmethod(lambda: False)),
            mock.patch("builtins.print"),
        ):
            repair.limit(Args())
        self.assertGreaterEqual(repair.parse_time(outputs["until"]), now + timedelta(minutes=30))


class FakeIssues:
    def __init__(self, comments, issue=ISSUE):
        self.issue = issue
        self.existing = comments
        self.posted = []

    def open_issue(self):
        return self.issue

    def comments(self, number):
        return self.existing

    def comment(self, number, body):
        self.posted.append(body)
        return 99


class StartSuite(unittest.TestCase):
    def start(self, github, force=False):
        outputs = {}

        class Args:
            pass

        Args.force = force
        with (
            mock.patch.object(repair, "utcnow", return_value=NIGHT),
            mock.patch.object(sync_report, "GitHub", lambda: github),
            mock.patch.object(repair, "write_outputs", outputs.update),
            mock.patch.object(sync_report, "write_step_summary", lambda text: None),
            mock.patch("builtins.print"),
        ):
            repair.start(Args())
        return outputs

    def test_start_checks_the_cap_again(self):
        capped = [attempt("2026-09-25", "2026-09-25T21:00:00Z"), attempt("2026-09-25", "2026-09-25T22:00:00Z")]
        github = FakeIssues(capped)
        self.assertEqual(self.start(github), {"go": "false"})
        self.assertEqual(github.posted, [])
        outputs = self.start(github, force=True)
        self.assertEqual(outputs["go"], "true")
        self.assertIn("Repair attempt 3 of 2", github.posted[0])

    def test_start_posts_the_attempt_and_the_prompt(self):
        github = FakeIssues([attempt("2026-09-25", "2026-09-25T21:00:00Z")])
        outputs = self.start(github)
        self.assertEqual(outputs["go"], "true")
        self.assertEqual(outputs["attempt"], 2)
        self.assertEqual(outputs["comment"], 99)
        self.assertIn("# Repair the Android port", outputs["prompt"])
        key = sync_report.failure_key(sync_report.load())
        self.assertEqual(outputs["key"], key)
        self.assertTrue(github.posted[0].startswith(f"<!-- repair-attempt date=2026-09-25 report={key} -->"))


class TestsGateSuite(unittest.TestCase):
    KNOWN = "com.moblin.android.media.haishinkit.media.RecorderSuite.recordsAudioAndVideo"

    def gate(self, first, again=None, at_base=(), has_changes=True):
        return repair.tests_gate(
            None, has_changes, runner=lambda: first,
            rerun=lambda tests: {name: tests[name] for name in (again if again is not None else tests)},
            at_base=lambda tests: set(at_base) & set(tests),
        )

    def test_only_known_failures(self):
        result = self.gate((100, {self.KNOWN: "RecorderSuite"}, "There were failing tests", False))
        self.assertTrue(result["ok"], result)

    def test_new_failure_blocks(self):
        result = self.gate((100, {"a.B.c": "a.B"}, "There were failing tests", False))
        self.assertFalse(result["ok"])
        self.assertIn("new failure: a.B.c", result["detail"])

    def test_flaky_failure_does_not_block(self):
        result = self.gate((100, {"a.B.c": "a.B"}, "There were failing tests", False), again=[])
        self.assertTrue(result["ok"])
        self.assertIn("passed when run again: a.B.c", result["detail"])

    def test_failure_that_fails_on_main_too_does_not_block(self):
        result = self.gate((100, {"a.B.c": "a.B"}, "There were failing tests", False), at_base=["a.B.c"])
        self.assertTrue(result["ok"])
        self.assertIn("fails on main too", result["detail"])

    def test_crashed_test_process_blocks(self):
        output = "Execution failed for task ':app:testDebugUnitTest'.\n> Process 'Gradle Test Executor 3' finished " \
                 "with non-zero exit value 134"
        self.assertFalse(self.gate((90, {}, output, False))["ok"])
        self.assertFalse(self.gate((90, {}, "BUILD FAILED", False))["ok"])
        self.assertFalse(self.gate((0, {}, "", False))["ok"])


class FinishSuite(unittest.TestCase):
    def finish(self, base_report, report, meaningful=()):
        closed = []
        posted = []

        class Args:
            base = "abc"
            issue = "7"
            comment = "5"
            attempt = "1"
            report_key = "abc123"
            outcome = "success"
            execution_file = str(FIXTURES / "execution-success.json")

        class GitHub:
            @staticmethod
            def available():
                return True

            def open_issue(self):
                return {"number": 7}

            def close_issue(self, number, comment):
                closed.append(number)

            def edit_comment(self, comment_id, body):
                posted.append(body)

        gates = [{"name": "changes", "ok": True, "detail": ""}]
        with (
            mock.patch.object(sync_report, "GitHub", GitHub),
            mock.patch.object(repair, "report_at", lambda commit: base_report),
            mock.patch.object(repair, "drop_stray_files", lambda: []),
            mock.patch.object(repair, "verify", lambda base, previous: (gates, report, list(meaningful))),
            mock.patch("builtins.print"),
            mock.patch.object(sync_report, "write_step_summary", lambda text: None),
        ):
            repair.finish(Args())
        return closed, posted

    def test_closes_when_main_already_has_nothing_to_report(self):
        closed, posted = self.finish(report_with_items(), sync_report.new_report("abc"))
        self.assertEqual(closed, [7])
        self.assertIn("closing", posted[0])

    def test_stays_open_after_a_stopped_sync_without_changes(self):
        closed, posted = self.finish(sync_report.new_report("abc"), sync_report.new_report("abc"))
        self.assertEqual(closed, [])
        self.assertIn("stays open", posted[0])

    def test_no_changes_with_items_left(self):
        closed, posted = self.finish(report_with_items(), report_with_items())
        self.assertEqual(closed, [])
        self.assertIn("Claude made no changes", posted[0])


if __name__ == "__main__":
    unittest.main()
