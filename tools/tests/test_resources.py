import shutil
import sys
import tempfile
import unittest
from pathlib import Path

TOOLS = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS))
import resources


class MirrorVersionSuite(unittest.TestCase):
    def setUp(self):
        self.directory = Path(tempfile.mkdtemp())
        self.config = self.directory / "Config"
        self.config.mkdir()
        self.target = self.directory / "moblin-version.properties"

    def tearDown(self):
        shutil.rmtree(self.directory, ignore_errors=True)

    def write(self, name, text):
        (self.config / name).write_text(text, encoding="utf-8")

    def test_reads_the_marketing_version_from_base_xcconfig(self):
        self.write("Base.xcconfig", '#include "User.xcconfig"\n\nMARKETING_VERSION = 35.3.0\n'
                                    "CURRENT_PROJECT_VERSION = 290\n")
        self.write("Another.xcconfig", "MARKETING_VERSION = 1.0\n")
        self.assertEqual(resources.mirror_version(self.config, self.target), "35.3.0")
        self.assertEqual(self.target.read_text(encoding="utf-8"), "MARKETING_VERSION=35.3.0\n")

    def test_ignores_conditional_and_variable_settings(self):
        self.write("Base.xcconfig", "MARKETING_VERSION[sdk=macosx*] = 9.9.9\nMARKETING_VERSION = $(OTHER)\n"
                                    "MARKETING_VERSION = 36.0 // next\n")
        self.assertEqual(resources.mirror_version(self.config, self.target), "36.0")

    def test_falls_back_to_other_xcconfig_files(self):
        self.write("Base.xcconfig", "CURRENT_PROJECT_VERSION = 290\n")
        self.write("Moblin.xcconfig", "MARKETING_VERSION = 35.4.0\n")
        self.assertEqual(resources.mirror_version(self.config, self.target), "35.4.0")

    def test_keeps_the_previous_file_when_the_version_is_gone(self):
        self.target.write_text("MARKETING_VERSION=35.3.0\n", encoding="utf-8")
        self.write("Base.xcconfig", "CURRENT_PROJECT_VERSION = 290\n")
        self.assertIsNone(resources.mirror_version(self.config, self.target))
        self.assertIsNone(resources.mirror_version(self.directory / "missing", self.target))
        self.assertEqual(self.target.read_text(encoding="utf-8"), "MARKETING_VERSION=35.3.0\n")


class MirrorAssetCatalogSuite(unittest.TestCase):
    def setUp(self):
        self.directory = Path(tempfile.mkdtemp())
        self.catalog = self.directory / "Assets.xcassets"
        self.target = self.directory / "Assets"

    def tearDown(self):
        shutil.rmtree(self.directory, ignore_errors=True)

    def add(self, name):
        imageset = self.catalog / name
        imageset.mkdir(parents=True)
        (imageset / "image.png").write_bytes(b"png")
        (imageset / "Contents.json").write_text('{"images": [{"filename": "image.png", "scale": "1x"}]}',
                                                encoding="utf-8")

    def test_leaves_out_moblins_app_icons_and_mascot(self):
        for name in ("AppIcon.appiconset", "AppIconKing.appiconset", "AppIconNoBackground.imageset",
                     "AppIconKingNoBackground.imageset", "MoblinInMouth.imageset", "ObsLogo.imageset",
                     "AlertFace.imageset"):
            self.add(name)
        self.target.mkdir()
        (self.target / "AppIcon.png").write_bytes(b"old")
        self.assertEqual(resources.mirror_asset_catalog(self.catalog, self.target), 2)
        self.assertEqual(sorted(path.name for path in self.target.iterdir()), ["AlertFace.png", "ObsLogo.png"])


if __name__ == "__main__":
    unittest.main()
