import json
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

TOOLS = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS))
import sync
import sync_report

TARGET = "b" * 40


class SyncCommitSuite(unittest.TestCase):
    def setUp(self):
        self.directory = Path(tempfile.mkdtemp())
        self.inventory = self.directory / "inventory.json"
        self.inventory.write_text(json.dumps({"files": [
            {"path": "Moblin/A.swift", "tier": "logic", "kotlin_path": "app/src/main/java/A.kt", "sha256": "new"},
        ]}), encoding="utf-8")
        self.commands = []
        self.saved = []
        self.published = []
        self.pushes = []
        self.set_aside = []

    def tearDown(self):
        shutil.rmtree(self.directory, ignore_errors=True)

    def main(self, report, build_ok, state_after_set_aside=None):
        states = [{"Moblin/A.swift": {"status": "ok", "sha256": "old", "moblin_commit": "a" * 40}}]
        if state_after_set_aside is not None:
            states.append(state_after_set_aside)

        def git(*arguments, capture=True, check=True):
            return subprocess.CompletedProcess(arguments, 0, stdout=TARGET + "\n", stderr="")

        def run(command, cwd=None, check=True, capture=False):
            self.commands.append([str(part) for part in command])
            return subprocess.CompletedProcess(command, 0, stdout="", stderr="")

        def push(verify=None):
            self.pushes.append(verify)
            return True

        with (
            mock.patch.object(sys, "argv", ["sync.py", "--commit", "--push"]),
            mock.patch.object(sync, "load_state", lambda: states[-1] if len(self.set_aside) else states[0]),
            mock.patch.object(sync, "INVENTORY", self.inventory),
            mock.patch.object(sync, "ensure_upstream", lambda base: None),
            mock.patch.object(sync, "git", git),
            mock.patch.object(sync, "run", run),
            mock.patch.object(sync, "port_and_build", lambda *arguments: (report, build_ok)),
            mock.patch.object(sync, "set_aside_changes", self.set_aside.append),
            mock.patch.object(sync, "push", push),
            mock.patch.object(sync, "publish", lambda report, enabled: self.published.append(report)),
            mock.patch.object(sync_report, "load", lambda: sync_report.new_report("a" * 40)),
            mock.patch.object(sync_report, "save", self.saved.append),
            mock.patch.object(sync_report, "write_step_summary", lambda text: None),
            mock.patch("builtins.print"),
        ):
            try:
                sync.main()
            except SystemExit as stop:
                return stop.code
        return None

    def commit_message(self):
        return next(command[-1] for command in self.commands if command[:2] == ["git", "commit"])

    def test_a_tree_that_does_not_build_is_set_aside_and_only_the_report_is_committed(self):
        report = sync_report.new_report(TARGET)
        report["build"] = ["e: Something.kt:1:1 broken"]
        report["held_back"] = [{"swift": "Moblin/A.swift", "kotlin": "app/src/main/java/A.kt", "reason": "compile",
                                "ported_from": "a" * 40, "errors": ["1: broken"], "hooks": []}]
        code = self.main(report, False, state_after_set_aside={"Moblin/A.swift": {"status": "ok", "sha256": "old"}})
        self.assertEqual(code, 1)
        self.assertEqual(self.set_aside, [TARGET])
        self.assertIn("only the report", self.commit_message())
        self.assertEqual(self.pushes, [None])
        saved = self.saved[-1]
        self.assertEqual(saved["build"], ["e: Something.kt:1:1 broken"])
        self.assertEqual([item["reason"] for item in saved["held_back"]], ["compile"])
        self.assertEqual(saved["held_back"][0]["errors"], ["1: broken"])
        self.assertIs(self.published[-1], saved)

    def test_a_tree_that_builds_is_committed_and_rebuilt_after_a_rebase(self):
        report = sync_report.new_report(TARGET)
        code = self.main(report, True)
        self.assertIsNone(code)
        self.assertEqual(self.set_aside, [])
        self.assertEqual(self.commit_message(), f"Sync with eerimoq/moblin {TARGET[:10]}.")
        self.assertEqual(self.pushes, [sync.builds])
        self.assertTrue(sync_report.is_empty(self.published[-1]))


if __name__ == "__main__":
    unittest.main()
