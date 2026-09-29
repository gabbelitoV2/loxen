import json
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


class MirrorLooseResourcesSuite(unittest.TestCase):
    def setUp(self):
        self.directory = Path(tempfile.mkdtemp())
        self.app = self.directory / "Moblin"
        self.target = self.directory / "assets"

    def tearDown(self):
        shutil.rmtree(self.directory, ignore_errors=True)

    def add(self, relative, data=b"x"):
        path = self.app / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)

    def test_mirrors_the_web_remote_control_flat_and_nothing_else(self):
        for relative in ("RemoteControl/Web/index.html", "RemoteControl/Web/volleyball.png",
                         "RemoteControl/Web/css/app.css", "RemoteControl/Web/js/app.mjs",
                         "RemoteControl/Web/js/vendor.mjs", "RemoteControl/Web/.DS_Store",
                         "VideoEffects/Browser/moblin.js", "Various/Foo.swift", "README.md", "Some/other.png",
                         "Some/node_modules/lib.js", "Alerts.bundle/sound.js"):
            self.add(relative)
        copied = resources.mirror_loose_resources(self.app, self.target)
        expected = ["app.css", "app.mjs", "index.html", "moblin.js", "vendor.mjs", "volleyball.png"]
        self.assertEqual(sorted(path.name for path in self.target.iterdir()), expected)
        self.assertEqual(sorted(copied), expected)
        self.assertEqual(copied["app.mjs"], "RemoteControl/Web/js/app.mjs")

    def test_keeps_loxens_favicon_instead_of_moblins(self):
        self.add("RemoteControl/Web/favicon.ico", b"moblin")
        self.add("RemoteControl/Web/index.html")
        self.target.mkdir()
        (self.target / "favicon.ico").write_bytes(b"loxen")
        copied = resources.mirror_loose_resources(self.app, self.target)
        self.assertNotIn("favicon.ico", copied)
        self.assertEqual((self.target / "favicon.ico").read_bytes(), b"loxen")

    def test_a_file_gone_from_the_source_is_removed_and_the_rest_is_kept(self):
        manifest = self.directory / "loose_resources.json"
        for relative in ("RemoteControl/Web/index.html", "RemoteControl/Web/js/chunk-1.mjs",
                         "RemoteControl/Web/favicon.ico", "VideoEffects/Browser/moblin.js"):
            self.add(relative)
        self.target.mkdir()
        (self.target / "favicon.ico").write_bytes(b"loxen")
        (self.target / "fonts").mkdir()
        resources.mirror_loose_resources(self.app, self.target, manifest)
        self.assertEqual(json.loads(manifest.read_text(encoding="utf-8")), ["chunk-1.mjs", "index.html", "moblin.js"])
        (self.app / "RemoteControl/Web/js/chunk-1.mjs").unlink()
        self.add("RemoteControl/Web/js/chunk-2.mjs")
        copied = resources.mirror_loose_resources(self.app, self.target, manifest)
        self.assertEqual(sorted(copied), ["chunk-2.mjs", "index.html", "moblin.js"])
        self.assertEqual(sorted(path.name for path in self.target.iterdir()),
                         ["chunk-2.mjs", "favicon.ico", "fonts", "index.html", "moblin.js"])
        self.assertEqual((self.target / "favicon.ico").read_bytes(), b"loxen")
        self.assertEqual(json.loads(manifest.read_text(encoding="utf-8")), ["chunk-2.mjs", "index.html", "moblin.js"])

    def test_a_first_run_without_a_manifest_removes_nothing(self):
        manifest = self.directory / "loose_resources.json"
        self.add("RemoteControl/Web/index.html")
        self.target.mkdir()
        (self.target / "old.mjs").write_bytes(b"x")
        resources.mirror_loose_resources(self.app, self.target, manifest)
        self.assertTrue((self.target / "old.mjs").is_file())
        self.assertEqual(json.loads(manifest.read_text(encoding="utf-8")), ["index.html"])

    def test_a_missing_upstream_removes_nothing_and_keeps_the_manifest(self):
        manifest = self.directory / "loose_resources.json"
        self.add("RemoteControl/Web/index.html")
        self.add("VideoEffects/Browser/moblin.js")
        resources.mirror_loose_resources(self.app, self.target, manifest)
        before = manifest.read_bytes()
        shutil.rmtree(self.app)
        copied = resources.mirror_loose_resources(self.app, self.target, manifest)
        self.assertEqual(copied, {})
        self.assertEqual(sorted(path.name for path in self.target.iterdir()), ["index.html", "moblin.js"])
        self.assertEqual(manifest.read_bytes(), before)

    def test_the_manifest_lists_what_the_real_web_app_mirrors(self):
        web = resources.ROOT / ".upstream/Moblin" / resources.WEB_APP
        if not web.is_dir():
            self.skipTest("no .upstream checkout")
        names = json.loads(resources.LOOSE_MANIFEST.read_text(encoding="utf-8"))
        self.assertEqual(names, sorted(names))
        self.assertNotIn("favicon.ico", names)
        for name in names:
            self.assertTrue((resources.ASSETS / name).is_file(), name)

    def test_the_real_web_app_is_mirrored(self):
        web = resources.ROOT / ".upstream/Moblin" / resources.WEB_APP
        if not web.is_dir():
            self.skipTest("no .upstream checkout")
        names = {path.name for path in web.rglob("*") if path.is_file() and not path.name.startswith(".")}
        names.discard("favicon.ico")
        missing = sorted(name for name in names if not (resources.ASSETS / name).is_file())
        self.assertEqual(missing, [])
        self.assertTrue((resources.ASSETS / "favicon.ico").is_file())


if __name__ == "__main__":
    unittest.main()
