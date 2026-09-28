import re
import sys
import unittest
from pathlib import Path

TOOLS = Path(__file__).resolve().parent.parent
ROOT = TOOLS.parent
sys.path.insert(0, str(TOOLS))
import ui_crawl


class RecordingDevice(ui_crawl.Device):
    def __init__(self):
        super().__init__("adb", "serial")
        self.calls = []

    def run(self, *args, timeout=30):
        self.calls.append(args)
        return ""


class UiCrawlPackageSuite(unittest.TestCase):
    def test_package_is_the_application_id(self):
        gradle = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertEqual(re.search(r'applicationId = "([^"]+)"', gradle).group(1), ui_crawl.PACKAGE)

    def test_launch_starts_the_launcher_activity_of_the_package(self):
        manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        namespace = re.search(r'namespace = "([^"]+)"', (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8"))
        activity = re.search(r'<activity\s+android:name="([^"]+)"', manifest).group(1)
        if activity.startswith("."):
            activity = namespace.group(1) + activity
        device = RecordingDevice()
        device.launch()
        self.assertEqual(device.calls, [("shell", "am", "start", "-n", f"{ui_crawl.PACKAGE}/{activity}")])


if __name__ == "__main__":
    unittest.main()
