import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

TOOLS = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS))
import play

PACKAGE = "com.loxen.app"
EDITS = f"{play.API}/{PACKAGE}/edits"


class Response:
    def __init__(self, status_code, body):
        self.status_code = status_code
        self.body = body
        self.text = str(body)

    def json(self):
        return self.body


class Session:
    def __init__(self, tracks=None, bundles=None, insert_status=200, list_status=200):
        self.tracks = tracks or {}
        self.bundles = bundles or {}
        self.insert_status = insert_status
        self.list_status = list_status
        self.calls = []

    def post(self, url, json=None, timeout=None):
        self.calls.append(("POST", url))
        if self.insert_status != 200:
            return Response(self.insert_status, {"error": {"message": "Package not found: com.loxen.app."}})
        return Response(200, {"id": "edit1"})

    def get(self, url, timeout=None):
        self.calls.append(("GET", url))
        if self.list_status != 200:
            return Response(self.list_status, {"error": {"message": "The caller does not have permission"}})
        return Response(200, self.tracks if url.endswith("/tracks") else self.bundles)

    def delete(self, url, timeout=None):
        self.calls.append(("DELETE", url))
        return Response(204, {})


def tracks(*codes_per_track):
    return {"tracks": [{"track": f"track{index}", "releases": [{"versionCodes": [str(code) for code in codes]}]}
                       for index, codes in enumerate(codes_per_track)]}


class HighestVersionCodeSuite(unittest.TestCase):
    def test_takes_the_highest_code_of_every_track_and_bundle(self):
        session = Session(tracks(("1110",), ("1105", "1112")), {"bundles": [{"versionCode": 1111}]})
        self.assertEqual(play.highest_version_code(session, PACKAGE), 1112)
        self.assertEqual(session.calls[0], ("POST", EDITS))
        self.assertEqual(session.calls[-1], ("DELETE", f"{EDITS}/edit1"))

    def test_a_new_app_without_releases_has_zero(self):
        self.assertEqual(play.highest_version_code(Session({"tracks": [{"track": "internal"}]}, {}), PACKAGE), 0)

    def test_a_missing_app_is_reported(self):
        with self.assertRaises(play.AppMissing):
            play.highest_version_code(Session(insert_status=404), PACKAGE)

    def test_the_edit_is_deleted_when_listing_fails(self):
        session = Session(list_status=403)
        with self.assertRaisesRegex(play.PlayError, "HTTP 403: The caller does not have permission"):
            play.highest_version_code(session, PACKAGE)
        self.assertEqual(session.calls[-1], ("DELETE", f"{EDITS}/edit1"))


class DecideSuite(unittest.TestCase):
    def decide(self, event, session, version_code=1112, key='{"type": "service_account"}'):
        return play.decide(PACKAGE, version_code, event, key, session_factory=lambda key: session)

    def test_uploads_a_new_version_code(self):
        build, upload, _ = self.decide("workflow_run", Session(tracks(("1111",))))
        self.assertEqual((build, upload), (True, True))

    def test_skips_when_main_did_not_change(self):
        build, upload, reason = self.decide("workflow_run", Session(tracks(("1112",))))
        self.assertEqual((build, upload), (False, False))
        self.assertIn("not higher than 1112", reason)

    def test_a_manual_run_still_builds_the_bundle(self):
        self.assertEqual(self.decide("workflow_dispatch", Session(tracks(("1112",))))[:2], (True, False))

    def test_without_the_service_account_only_a_manual_run_builds(self):
        self.assertEqual(self.decide("workflow_run", None, key="")[:2], (False, False))
        build, upload, reason = self.decide("workflow_dispatch", None, key="")
        self.assertEqual((build, upload), (True, False))
        self.assertIn("attached to this run", reason)

    def test_a_missing_app_is_skipped(self):
        build, upload, reason = self.decide("workflow_run", Session(insert_status=404))
        self.assertEqual((build, upload), (False, False))
        self.assertIn("upload the first app bundle by hand", reason)


class MainSuite(unittest.TestCase):
    def test_writes_the_outputs(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "output"
            summary = Path(directory) / "summary"
            environment = {"GITHUB_OUTPUT": str(output), "GITHUB_STEP_SUMMARY": str(summary),
                           "PLAY_SERVICE_ACCOUNT_JSON": ""}
            with mock.patch.dict(os.environ, environment), mock.patch("builtins.print"):
                code = play.main(["decide", "--package", PACKAGE, "--version-code", "1112", "--event",
                                  "workflow_dispatch"])
            self.assertEqual(code, 0)
            self.assertEqual(output.read_text(encoding="utf-8"), "build=true\nupload=false\n")
            self.assertIn("PLAY_SERVICE_ACCOUNT_JSON is not set", summary.read_text(encoding="utf-8"))

    def test_a_play_error_fails_the_step(self):
        session = Session(list_status=403)
        with (
            mock.patch.dict(os.environ, {"PLAY_SERVICE_ACCOUNT_JSON": "{}", "GITHUB_OUTPUT": ""}),
            mock.patch.object(play, "session_for", lambda key: session),
            mock.patch("builtins.print") as printed,
        ):
            code = play.main(["decide", "--package", PACKAGE, "--version-code", "1112", "--event", "workflow_run"])
        self.assertEqual(code, 1)
        self.assertIn("::error::", printed.call_args[0][0])


class KeyMaterialSuite(unittest.TestCase):
    def test_upload_keys_and_the_service_account_are_never_committed(self):
        for path in ("upload-keystore.jks", "app/upload-keystore.jks", "app/build/upload-key/upload-keystore.jks",
                     "release.keystore", "upload-keystore.properties", "play-service-account.json",
                     "tools/play-service-account.json"):
            result = subprocess.run(["git", "check-ignore", "-q", "--no-index", path], cwd=TOOLS.parent)
            self.assertEqual(result.returncode, 0, f"{path} is not ignored by git")


if __name__ == "__main__":
    unittest.main()
