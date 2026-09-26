import re
import shutil
import sys
import tempfile
import unittest
from pathlib import Path

TOOLS = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS))
import port


def entry(path, **fields):
    return {
        "status": "ok",
        "sha256": path,
        "kotlin_path": f"app/src/main/java/{Path(path).stem}.kt",
        "tier": "ui",
        **fields,
    }


class PortReportSuite(unittest.TestCase):
    def setUp(self):
        self.directory = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.directory, True)

    def report(self, state):
        inventory = {"files": [{"path": path, "tier": "ui", "sha256": path} for path in state]}
        port.write_report(self.directory, inventory, state)
        text = (self.directory / "PORT-REPORT.md").read_text(encoding="utf-8")
        return re.sub(r"^Generated .*$", "Generated", text, flags=re.MULTILINE)

    def section(self, text, heading):
        start = text.index(heading + "\n")
        end = text.find("\n## ", start + len(heading))
        return text[start:] if end < 0 else text[start:end]

    def test_classified_notes_are_listed_apart_from_manual_work(self):
        state = {
            "Moblin/B.swift": entry("Moblin/B.swift", unsupported=["gap"], apple_only=["HealthKit"]),
            "Moblin/A.swift": entry("Moblin/A.swift", unsupported=[], needs_user=["StoreKit"]),
            "Moblin/C.swift": entry("Moblin/C.swift", unsupported=[]),
        }
        text = self.report(state)
        manual = self.section(text, "## Needs manual work")
        self.assertIn("- Moblin/B.swift\n  - gap", manual)
        self.assertNotIn("HealthKit", manual)
        self.assertNotIn("Moblin/A.swift", manual)
        classified = self.section(text, "## Apple only / needs Gabriel")
        self.assertIn("### Apple only\n\n- Moblin/B.swift\n  - HealthKit", classified)
        self.assertIn("### Needs Gabriel\n\n- Moblin/A.swift\n  - StoreKit", classified)
        self.assertLess(classified.index("### Apple only"), classified.index("### Needs Gabriel"))
        self.assertNotIn("Moblin/C.swift\n", classified)

    def test_report_only_is_the_same_every_time(self):
        state = {
            "Moblin/B.swift": entry("Moblin/B.swift", unsupported=["gap"], apple_only=["HealthKit", "Watch"]),
            "Moblin/A.swift": entry("Moblin/A.swift", unsupported=[], needs_user=["StoreKit"]),
        }
        reordered = dict(reversed(list(state.items())))
        self.assertEqual(self.report(state), self.report(reordered))

    def test_no_classified_section_without_classified_notes(self):
        text = self.report({"Moblin/A.swift": entry("Moblin/A.swift", unsupported=["gap"])})
        self.assertNotIn("Apple only", text)

    def test_a_new_port_keeps_the_classification(self):
        previous = entry("Moblin/A.swift", unsupported=["gap"], apple_only=["HealthKit"], needs_user=["StoreKit"])
        unsupported, classified = port.classified_notes(["HealthKit", "new gap", "StoreKit"], previous)
        self.assertEqual(["new gap"], unsupported)
        self.assertEqual({"apple_only": ["HealthKit"], "needs_user": ["StoreKit"]}, classified)
        self.assertEqual((["gap"], {}), port.classified_notes(["gap"], None))


class ShimGuidanceSuite(unittest.TestCase):
    def test_weather_files_are_not_told_that_weather_is_missing(self):
        for swift_path in (
            "Moblin/Various/Model/ModelVariables.swift",
            "Moblin/Various/Model/ModelRemoteControl.swift",
            "Moblin/Various/Managers/WeatherManager.swift",
        ):
            glossary = port.shim_glossary(swift_path)
            self.assertIn("com.moblin.android.platform.weatherkit", glossary, swift_path)
            self.assertNotIn("WeatherKit has no counterpart", glossary, swift_path)


if __name__ == "__main__":
    unittest.main()
