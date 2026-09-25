import os
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

TOOLS = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS))
import sync_report


class FakeGitHub:
    def __init__(self, issue=None):
        self.issue = issue
        self.calls = []

    def open_issue(self):
        return self.issue

    def ensure_label(self):
        self.calls.append(("label",))

    def create_issue(self, title, body):
        self.calls.append(("create", title, body))
        return 12

    def edit_issue(self, number, title, body):
        self.calls.append(("edit", number, title, body))

    def close_issue(self, number, comment):
        self.calls.append(("close", number, comment))


def report_with_items():
    report = sync_report.new_report("afa485d231cb21bbf41a69eb2a3c408f612d8490")
    report["pbswift"] = "pbswift: error: Moblin/Integrations/Tesla/Protobuf/vcsec.pb.swift:120: unknown construct"
    report["hand_ported"] = ["Moblin Live Activity/MoblinLiveActivityApp.swift: changed upstream"]
    return report


class ReportSuite(unittest.TestCase):
    def test_empty_report(self):
        report = sync_report.new_report("abc")
        self.assertTrue(sync_report.is_empty(report))
        self.assertEqual(sync_report.count(report), 0)
        self.assertEqual(sync_report.items_line(report), "nothing to report")

    def test_count_and_items_line(self):
        report = report_with_items()
        self.assertEqual(sync_report.count(report), 2)
        self.assertEqual(sync_report.items_line(report), "2 items (pbswift, hand ported)")

    def test_save_and_load(self):
        directory = Path(tempfile.mkdtemp())
        try:
            path = directory / "report.json"
            sync_report.save(report_with_items(), path)
            self.assertEqual(sync_report.load(path), report_with_items())
            self.assertTrue(path.read_text(encoding="utf-8").endswith("}\n"))
            self.assertEqual(sync_report.load(directory / "missing.json"), sync_report.new_report())
        finally:
            shutil.rmtree(directory, ignore_errors=True)

    def test_issue_body_stays_under_the_github_limit(self):
        report = report_with_items()
        report["effects"] = [f"check_effects_api.py: finding {index} " + "y" * 300 for index in range(3000)]
        body = sync_report.issue_body(report, "https://github.com/o/r/actions/runs/1")
        self.assertLess(len(body), 65536)
        self.assertTrue(body.startswith(sync_report.MARKER))
        self.assertIn("at most 2 attempts in any 24 hours, runs that hit Claude's usage limit included", body)
        self.assertIn("tools/sync-report.json", body)


class PublishSuite(unittest.TestCase):
    def test_opens_an_issue(self):
        github = FakeGitHub()
        self.assertEqual(sync_report.publish(report_with_items(), github, log=lambda *_: None), 12)
        self.assertEqual(github.calls[0], ("label",))
        self.assertEqual(github.calls[1][0], "create")
        self.assertIn("2 items, upstream afa485d231", github.calls[1][1])
        self.assertIn("Tesla protobufs", github.calls[1][2])

    def test_updates_the_open_issue(self):
        github = FakeGitHub({"number": 5})
        sync_report.publish(report_with_items(), github, log=lambda *_: None)
        self.assertEqual([call[0] for call in github.calls], ["label", "edit"])
        self.assertEqual(github.calls[1][1], 5)

    def test_closes_the_issue_when_nothing_is_left(self):
        github = FakeGitHub({"number": 5})
        sync_report.publish(sync_report.new_report("abc"), github, log=lambda *_: None)
        self.assertEqual([call[0] for call in github.calls], ["close"])

    def test_nothing_to_do_without_issue_and_items(self):
        github = FakeGitHub()
        sync_report.publish(sync_report.new_report("abc"), github, log=lambda *_: None)
        self.assertEqual(github.calls, [])

    def test_local_run_only_prints(self):
        printed = []
        with mock.patch.dict(os.environ, {"GH_TOKEN": "", "GITHUB_TOKEN": "", "GITHUB_REPOSITORY": ""}):
            self.assertIsNone(sync_report.publish_or_print(report_with_items(), True, log=printed.append))
        self.assertIn("Tesla protobufs", printed[0])
        self.assertIn("not updated", printed[1])

    def test_publishing_is_recorded_for_the_workflow(self):
        directory = Path(tempfile.mkdtemp())
        try:
            output = directory / "output"
            environment = {"GITHUB_OUTPUT": str(output), "GH_TOKEN": "x", "GITHUB_REPOSITORY": "o/r"}
            with (
                mock.patch.dict(os.environ, environment),
                mock.patch.object(sync_report.GitHub, "available", staticmethod(lambda: True)),
                mock.patch.object(sync_report, "publish", lambda report, url=None, log=None: 3),
            ):
                self.assertEqual(sync_report.publish_or_print(report_with_items(), True, log=lambda *_: None), 3)
            self.assertEqual(output.read_text(encoding="utf-8"), "published=true\n")
        finally:
            shutil.rmtree(directory, ignore_errors=True)

    def test_only_issues_opened_by_the_workflow_are_used(self):
        issues = [
            {"number": 1, "user": {"login": "someone"}, "created_at": "2026-09-25T03:00:00Z", "body": "hijack"},
            {"number": 2, "user": {"login": sync_report.BOT}, "created_at": "2026-09-25T03:10:00Z",
             "pull_request": {}},
            {"number": 3, "user": {"login": sync_report.BOT}, "created_at": "2026-09-25T03:20:00Z", "body": None,
             "title": "Android port needs repair", "html_url": "https://github.com/o/r/issues/3"},
        ]
        issue = sync_report.own_issue(issues)
        self.assertEqual(issue, {"number": 3, "title": "Android port needs repair", "createdAt": "2026-09-25T03:20:00Z",
                                 "url": "https://github.com/o/r/issues/3", "body": ""})
        self.assertIsNone(sync_report.own_issue(issues[:2]))

    def test_open_issue_asks_for_open_labelled_issues(self):
        github = sync_report.GitHub("o/r")
        with mock.patch.object(github, "gh", return_value="[]") as gh:
            self.assertIsNone(github.open_issue())
        arguments = gh.call_args[0]
        self.assertEqual(arguments[:3], ("api", "-X", "GET"))
        self.assertIn("repos/o/r/issues?labels=needs-repair&state=open", arguments[3])


class CountSuite(unittest.TestCase):
    def test_build_output_counts_as_one_item(self):
        report = sync_report.new_report("abc")
        report["build"] = [f"line {index}" for index in range(40)]
        self.assertEqual(sync_report.count(report), 1)

    def test_failure_key_names_the_items_not_their_messages(self):
        report = report_with_items()
        report["held_back"] = [{"swift": "Moblin/A.swift", "errors": ["1: Unresolved reference 'x'."]}]
        report["hooks"] = ["MISSING H1 (T1.json) various/A.kt [fun a]: neither the old nor the new line was found"]
        again = report_with_items()
        again["upstream"] = "c" * 40
        again["pbswift"] = "pbswift: error: another line"
        again["held_back"] = [{"swift": "Moblin/A.swift", "errors": ["7: Type mismatch."]}]
        again["hooks"] = ["MISSING H1 (T1.json) various/A.kt [fun a]: file not found"]
        self.assertEqual(sync_report.failure_key(report), sync_report.failure_key(again))
        again["held_back"].append({"swift": "Moblin/B.swift"})
        self.assertNotEqual(sync_report.failure_key(report), sync_report.failure_key(again))
        self.assertEqual(len(sync_report.failure_key(sync_report.new_report())), 12)


class CommandSuite(unittest.TestCase):
    def test_sync_failed_without_github_prints_the_report(self):
        environment = dict(os.environ, GH_TOKEN="", GITHUB_TOKEN="", GITHUB_REPOSITORY="", GITHUB_OUTPUT="")
        result = subprocess.run([sys.executable, str(TOOLS / "sync_report.py"), "--sync-failed", "timed out"],
                                capture_output=True, text=True, encoding="utf-8", env=environment)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("### The sync stopped", result.stdout)
        self.assertIn("timed out", result.stdout)


if __name__ == "__main__":
    unittest.main()
