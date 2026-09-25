import copy
import json
import re
import shutil
import sys
import tempfile
import unittest
from pathlib import Path

TOOLS = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS))
import degrade
import sync_report

FIXTURE = Path(__file__).resolve().parent / "fixtures" / "degrade"
MAIN = "app/src/main/java/com/moblin/android/"
SETTINGS = MAIN + "various/Settings.kt"
MODEL = MAIN + "various/Model.kt"
CHAT = MAIN + "various/Chat.kt"
RAID = MAIN + "various/Raid.kt"
CAMERA = MAIN + "platform/Camera.kt"
SUITE = "app/src/test/java/com/moblin/android/various/SettingsSuite.kt"
FUN_RE = re.compile(r"\bfun (\w+)\(")
CALL_RE = re.compile(r"^\s+(\w+)\(\)\s*$")


def load(name):
    return json.loads((FIXTURE / name).read_text(encoding="utf-8"))


def toy_build(root):
    sources = sorted(path for directory in degrade.SOURCE_DIRS for path in (root / directory).rglob("*.kt"))
    declared = {name for path in sources for name in FUN_RE.findall(path.read_text(encoding="utf-8"))}
    lines = []
    for path in sources:
        for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            call = CALL_RE.match(line)
            if call and call.group(1) not in declared:
                lines.append(f"e: {path.resolve().as_uri()}:{number}:5 Unresolved reference '{call.group(1)}'.")
    return not lines, "\n".join(lines)


class DegradeSuite(unittest.TestCase):
    def setUp(self):
        self.root = Path(tempfile.mkdtemp())
        shutil.copytree(FIXTURE / "base", self.root, dirs_exist_ok=True)
        self.inventory = load("inventory.json")
        self.snapshot = degrade.Snapshot(self.root, load("state-before.json"))
        self.saved = []

    def tearDown(self):
        shutil.rmtree(self.root, ignore_errors=True)

    def port(self, *files):
        for rel in files:
            target = self.root / rel
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(FIXTURE / "ported" / rel, target)

    def text(self, rel):
        return (self.root / rel).read_text(encoding="utf-8")

    def base_text(self, rel):
        return (FIXTURE / "base" / rel).read_text(encoding="utf-8")

    def stabilize(self, state, build=None, hooks=None):
        return degrade.stabilize(
            self.snapshot, state, build or (lambda: toy_build(self.root)), hooks or (lambda: {}),
            degrade.dependencies(self.inventory), lambda saved: self.saved.append(copy.deepcopy(saved)),
            log=lambda *_: None,
        )

    def test_holds_back_only_changed_files_that_break_the_build(self):
        self.port(SETTINGS, MODEL, CHAT, RAID, SUITE)
        state = load("state-after-port.json")
        result = self.stabilize(state)
        self.assertTrue(result["ok"])
        reasons = {rel: info["reason"] for rel, info in result["reverted"].items()}
        self.assertEqual(reasons, {MODEL: "compile", RAID: "compile", SUITE: "compile", SETTINGS: "dependency"})
        self.assertEqual(result["reverted"][MODEL]["errors"], ["5: Unresolved reference 'missingHelper'."])
        self.assertEqual(result["reverted"][SETTINGS]["swift"], ["Moblin/Various/Settings.swift"])
        for rel in (SETTINGS, MODEL, SUITE):
            self.assertEqual(self.text(rel), self.base_text(rel))
        self.assertFalse((self.root / RAID).exists())
        self.assertEqual(self.text(CHAT), (FIXTURE / "ported" / CHAT).read_text(encoding="utf-8"))
        self.assertEqual(self.text(CAMERA), self.base_text(CAMERA))
        before = load("state-before.json")
        after = load("state-after-port.json")
        for swift in ("Moblin/Various/Settings.swift", "Moblin/Various/Model.swift", "MoblinTests/SettingsSuite.swift"):
            self.assertEqual(state[swift], before[swift])
        self.assertNotIn("Moblin/Various/Raid.swift", state)
        self.assertEqual(state["Moblin/Various/Chat.swift"], after["Moblin/Various/Chat.swift"])
        self.assertEqual(self.saved[-1], state)
        self.assertTrue(toy_build(self.root)[0])

    def test_report_lists_held_back_files_so_the_next_sync_retries_them(self):
        self.port(SETTINGS, MODEL, CHAT, RAID, SUITE)
        state = load("state-after-port.json")
        result = self.stabilize(state)
        report = sync_report.collect("b" * 40, self.inventory, state, reverted=result["reverted"], hooks=[])
        held = {item["swift"]: item for item in report["held_back"]}
        self.assertEqual(sorted(held), ["Moblin/Various/Model.swift", "Moblin/Various/Raid.swift",
                                        "Moblin/Various/Settings.swift", "MoblinTests/SettingsSuite.swift"])
        self.assertEqual(held["Moblin/Various/Model.swift"]["ported_from"], "a" * 40)
        self.assertEqual(held["Moblin/Various/Raid.swift"]["reason"], "compile")
        self.assertIsNone(held["Moblin/Various/Raid.swift"]["ported_from"])
        self.assertEqual(held["Moblin/Various/Settings.swift"]["reason"], "dependency")
        self.assertEqual(report["port_failures"], [])
        self.assertFalse(sync_report.is_empty(report))
        self.assertIn("missingHelper", sync_report.to_markdown(report))
        again = sync_report.collect("b" * 40, self.inventory, state, previous=report, hooks=[])
        self.assertEqual(again["held_back"], report["held_back"])

    def test_nothing_is_held_back_when_the_port_builds(self):
        self.port(CHAT)
        state = load("state-after-port.json")
        result = self.stabilize(state)
        self.assertTrue(result["ok"])
        self.assertEqual(result["reverted"], {})
        self.assertEqual(self.saved, [])

    def test_changed_file_with_a_missing_hook_is_held_back(self):
        self.port(CHAT)
        state = load("state-after-port.json")
        label = "H1 (T1.json) various/Chat.kt [fun startChat]: neither the old nor the new line was found"

        def hooks():
            return {CHAT: [label]} if self.text(CHAT) != self.base_text(CHAT) else {}

        result = self.stabilize(state, hooks=hooks)
        self.assertTrue(result["ok"])
        self.assertEqual(result["reverted"][CHAT]["reason"], "hook")
        self.assertEqual(result["reverted"][CHAT]["hooks"], [label])
        self.assertEqual(self.text(CHAT), self.base_text(CHAT))
        self.assertEqual(state["Moblin/Various/Chat.swift"], load("state-before.json")["Moblin/Various/Chat.swift"])

    def test_unexplained_failure_holds_back_everything_this_sync_changed(self):
        self.port(SETTINGS, CHAT)
        state = load("state-after-port.json")

        def build():
            if self.snapshot.changed():
                return False, "FAILURE: Build failed with an exception.\n* What went wrong: resource linking failed"
            return True, ""

        result = self.stabilize(state, build=build)
        self.assertTrue(result["ok"])
        self.assertEqual({rel: info["reason"] for rel, info in result["reverted"].items()},
                         {SETTINGS: "fallback", CHAT: "fallback"})
        self.assertEqual(self.snapshot.changed(), [])

    def test_file_removed_upstream_comes_back_when_hand_written_code_still_uses_it(self):
        (self.root / CAMERA).write_text(
            "package com.moblin.android.platform\n\nfun startCamera() {\n    startChat()\n}\n", encoding="utf-8"
        )
        self.snapshot = degrade.Snapshot(self.root, load("state-before.json"))
        state = load("state-before.json")
        (self.root / CHAT).unlink()
        del state["Moblin/Various/Chat.swift"]
        result = self.stabilize(state)
        self.assertTrue(result["ok"])
        self.assertEqual(result["reverted"][CHAT]["reason"], "dependency")
        self.assertTrue((self.root / CHAT).exists())
        inventory = copy.deepcopy(self.inventory)
        inventory["files"] = [entry for entry in inventory["files"] if entry["path"] != "Moblin/Various/Chat.swift"]
        held, _ = sync_report.stale_files(inventory, state, result["reverted"])
        chat = next(item for item in held if item["swift"] == "Moblin/Various/Chat.swift")
        self.assertIn("removed upstream", chat["reason"])

    def test_gives_up_when_the_build_fails_without_changes_to_hold_back(self):
        state = load("state-before.json")
        result = self.stabilize(state, build=lambda: (False, "e: file:///x/Other.kt:1:1 Syntax error."))
        self.assertFalse(result["ok"])
        self.assertEqual(result["reverted"], {})

    def test_a_build_hiccup_without_kotlin_errors_is_retried_before_holding_anything_back(self):
        self.port(SETTINGS, CHAT)
        state = load("state-after-port.json")
        outcomes = [(False, "FAILURE: Could not resolve all files for configuration. Read timed out"), (True, "")]
        result = self.stabilize(state, build=lambda: outcomes.pop(0))
        self.assertTrue(result["ok"])
        self.assertEqual(result["reverted"], {})
        self.assertEqual(outcomes, [])
        self.assertEqual(self.text(CHAT), (FIXTURE / "ported" / CHAT).read_text(encoding="utf-8"))

    def test_after_the_targeted_rounds_everything_left_is_held_back(self):
        self.port(SETTINGS, MODEL, CHAT)
        state = load("state-after-port.json")
        builds = []

        def build():
            builds.append(1)
            changed = self.snapshot.changed()
            if not changed:
                return True, ""
            uri = (self.root / changed[0]).resolve().as_uri()
            return False, f"e: {uri}:1:1 Something new broke."

        result = degrade.stabilize(self.snapshot, state, build, lambda: {}, {}, lambda saved: None,
                                   log=lambda *_: None, targeted_rounds=2)
        self.assertTrue(result["ok"])
        reasons = sorted(info["reason"] for info in result["reverted"].values())
        self.assertEqual(reasons, ["compile", "fallback", "fallback"])
        self.assertEqual(self.snapshot.changed(), [])
        self.assertEqual(len(builds), 3)

    def test_the_last_round_is_confirmed_by_a_build(self):
        self.port(MODEL)
        state = load("state-after-port.json")
        result = degrade.stabilize(self.snapshot, state, lambda: toy_build(self.root), lambda: {}, {},
                                   lambda saved: None, log=lambda *_: None, max_rounds=1)
        self.assertTrue(result["ok"])
        self.assertEqual(list(result["reverted"]), [MODEL])


class ChooseRevertsSuite(unittest.TestCase):
    def test_errors_in_changed_files_win_over_dependencies(self):
        targets = degrade.choose_reverts(
            {"a.kt": [[1, "x"]], "b.kt": [[2, "Unresolved reference 'c'."]]}, {"a.kt", "c.kt"}, {},
            lambda rel: {"c"}, {}, False,
        )
        self.assertEqual(targets, {"a.kt": "compile"})

    def test_dependencies_from_the_inventory(self):
        targets = degrade.choose_reverts({"b.kt": [[2, "Type mismatch."]]}, {"c.kt", "d.kt"}, {}, lambda rel: set(),
                                         {"b.kt": {"c.kt"}}, False)
        self.assertEqual(targets, {"c.kt": "dependency"})

    def test_missing_hook_in_unchanged_file_is_only_reported(self):
        self.assertEqual(degrade.choose_reverts({}, {"a.kt"}, {"b.kt": ["H1"]}, lambda rel: set(), {}, True), {})


class ParseErrorsSuite(unittest.TestCase):
    def test_paths_are_made_relative_and_continuation_lines_joined(self):
        root = Path(tempfile.mkdtemp())
        try:
            uri = (root / "app/src/main/java/A.kt").resolve().as_uri()
            output = f"e: {uri}:3:7 Argument type mismatch:\n    actual type is 'Int'.\nw: warning\ne: {uri}:9:1 Oops."
            self.assertEqual(degrade.parse_errors(output, root),
                             {"app/src/main/java/A.kt": [[3, "Argument type mismatch: actual type is 'Int'."],
                                                         [9, "Oops."]]})
        finally:
            shutil.rmtree(root, ignore_errors=True)

    def test_dependencies_map_kotlin_paths(self):
        inventory = load("inventory.json")
        depends_on = degrade.dependencies(inventory)
        self.assertEqual(depends_on[MODEL], {SETTINGS})
        self.assertEqual(depends_on[SUITE], {SETTINGS})
        self.assertNotIn("", depends_on)


if __name__ == "__main__":
    unittest.main()
