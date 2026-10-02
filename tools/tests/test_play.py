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
        build, upload, _, previous = self.decide("workflow_run", Session(tracks(("1111",))))
        self.assertEqual((build, upload, previous), (True, True, 1111))

    def test_skips_when_main_did_not_change(self):
        build, upload, reason, _ = self.decide("workflow_run", Session(tracks(("1112",))))
        self.assertEqual((build, upload), (False, False))
        self.assertIn("not higher than 1112", reason)

    def test_a_manual_run_still_builds_the_bundle(self):
        self.assertEqual(self.decide("workflow_dispatch", Session(tracks(("1112",))))[:2], (True, False))

    def test_without_the_service_account_only_a_manual_run_builds(self):
        self.assertEqual(self.decide("workflow_run", None, key="")[:2], (False, False))
        build, upload, reason, _ = self.decide("workflow_dispatch", None, key="")
        self.assertEqual((build, upload), (True, False))
        self.assertIn("attached to this run", reason)

    def test_a_missing_app_is_skipped(self):
        build, upload, reason, _ = self.decide("workflow_run", Session(insert_status=404))
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
            self.assertEqual(output.read_text(encoding="utf-8"), "build=true\nupload=false\nprevious=0\n")
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


class RetrySuite(unittest.TestCase):
    def test_a_server_error_is_retried(self):
        class Flaky(Session):
            def __init__(self):
                super().__init__(tracks(("1111",)))
                self.failures = 2

            def get(self, url, timeout=None):
                if url.endswith("/tracks") and self.failures:
                    self.failures -= 1
                    return Response(503, {"error": {"message": "The service is currently unavailable."}})
                return super().get(url, timeout)

        self.assertEqual(play.highest_version_code(Flaky(), PACKAGE, delay=0), 1111)

    def test_a_lasting_server_error_fails(self):
        class Down(Session):
            def get(self, url, timeout=None):
                return Response(503, {"error": {"message": "The service is currently unavailable."}})

        with self.assertRaisesRegex(play.PlayError, "HTTP 503"):
            play.highest_version_code(Down(), PACKAGE, delay=0)


class NotesRepository:
    def __init__(self, directory, subjects):
        self.path = Path(directory)
        subprocess.run(["git", "init", "-q", str(self.path)], check=True)
        for subject in subjects:
            self.commit(subject)

    def commit(self, subject):
        subprocess.run(["git", "-C", str(self.path), "-c", "user.name=Test", "-c", "user.email=test@example.com",
                        "commit", "-q", "--allow-empty", "-m", subject], check=True)

    def head(self, back=0):
        return play.git("rev-parse", f"HEAD~{back}", cwd=self.path).strip()


class Post:
    def __init__(self, text, status_code=200):
        self.text = text
        self.status_code = status_code
        self.requests = []

    def __call__(self, url, headers=None, json=None, timeout=None):
        self.requests.append(json)
        return Response(self.status_code, {"content": [{"type": "thinking", "thinking": "..."},
                                                       {"type": "text", "text": self.text}]})


def compare(subjects):
    def get(url, headers=None, timeout=None):
        get.url = url
        return Response(200, {"commits": [{"commit": {"message": f"{subject}\n\nBody."}} for subject in subjects]})

    return get


class ReleaseNotesSuite(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.repository = NotesRepository(self.directory.name, [
            "First commit.",
            "Sync with eerimoq/moblin aaaaaaa111.",
            "Uploaded commit.",
        ])

    def test_the_uploaded_commit_is_found_by_its_version_code(self):
        self.assertEqual(play.uploaded_commit(1003, cwd=self.repository.path), self.repository.head())
        self.repository.commit("Later commit.")
        self.assertEqual(play.uploaded_commit(1003, cwd=self.repository.path), self.repository.head(1))
        self.assertEqual(play.uploaded_commit(1001, cwd=self.repository.path), self.repository.head(3))
        self.assertIsNone(play.uploaded_commit(1009, cwd=self.repository.path))
        self.assertIsNone(play.uploaded_commit(0, cwd=self.repository.path))

    def test_the_model_gets_loxen_changes_and_the_moblin_commits_since_the_last_upload(self):
        self.repository.commit("The main screen hides the system bars; they come back on a swipe.")
        self.repository.commit("Sync with eerimoq/moblin bbbbbbb222, 2 items need repair (tools/sync-report.json).")
        self.repository.commit("Repair the Android port after the sync with eerimoq/moblin bbbbbbb222.")
        get = compare(["Add a scoreboard widget.", "Version 35.6.0."])
        post = Post("• Full screen main view.\n• New scoreboard widget.")
        notes = play.release_notes(1003, "key", cwd=self.repository.path, get=get, post=post)
        self.assertEqual(notes, "• Full screen main view.\n• New scoreboard widget.")
        self.assertTrue(get.url.endswith("/compare/aaaaaaa111...bbbbbbb222"))
        self.assertEqual(post.requests[0]["thinking"], {"type": "disabled"})
        content = post.requests[0]["messages"][0]["content"]
        self.assertIn("The main screen hides the system bars", content)
        self.assertIn("Add a scoreboard widget.", content)
        self.assertNotIn("Repair the Android port", content)
        self.assertNotIn("Uploaded commit.", content)

    def test_without_the_model_the_notes_list_shortened_subjects(self):
        self.repository.commit("Recordings end where recording stopped: the recorder is stopped first, and so on.")
        self.repository.commit("Sync with eerimoq/moblin bbbbbbb222.")
        notes = play.release_notes(1003, "", cwd=self.repository.path, get=compare(["One.", "Two."]))
        self.assertEqual(notes, "• Recordings end where recording stopped.\n• 2 changes from Moblin.")

    def test_a_failing_model_falls_back_to_the_subjects(self):
        self.repository.commit("Fix the chat.")
        with mock.patch("builtins.print"):
            notes = play.release_notes(1003, "key", cwd=self.repository.path, post=Post("", status_code=500))
        self.assertEqual(notes, "• Fix the chat.")

    def test_an_empty_answer_falls_back_to_the_subjects(self):
        self.repository.commit("Fix the chat.")
        with mock.patch("builtins.print"):
            notes = play.release_notes(1003, "key", cwd=self.repository.path, post=Post(""))
        self.assertEqual(notes, "• Fix the chat.")

    def test_nothing_new_gives_the_default_text(self):
        self.repository.commit("Repair the Android port after the sync with eerimoq/moblin aaaaaaa111.")
        self.assertEqual(play.release_notes(1003, "key", cwd=self.repository.path), play.DEFAULT_NOTES)

    def test_notes_fit_in_googles_limit(self):
        text = "\n".join(f"• Change number {index} " + "x" * 60 for index in range(20))
        fitted = play.fitted(text)
        self.assertLessEqual(len(fitted), play.NOTES_LIMIT)
        self.assertTrue(fitted.endswith("x"))
        self.assertEqual(len(play.fitted("y" * 900)), play.NOTES_LIMIT)

    def test_the_command_writes_the_whatsnew_file_even_when_everything_fails(self):
        output = Path(self.directory.name) / "whatsnew"
        with (
            mock.patch.object(play, "release_notes", side_effect=RuntimeError("no git")),
            mock.patch.dict(os.environ, {"GITHUB_STEP_SUMMARY": ""}),
            mock.patch("builtins.print"),
        ):
            code = play.main(["notes", "--previous-version-code", "1143", "--directory", str(output)])
        self.assertEqual(code, 0)
        self.assertEqual((output / "whatsnew-en-US").read_text(encoding="utf-8"), play.DEFAULT_NOTES + "\n")


class KeyMaterialSuite(unittest.TestCase):
    def test_upload_keys_and_the_service_account_are_never_committed(self):
        for path in ("upload-keystore.jks", "app/upload-keystore.jks", "app/build/upload-key/upload-keystore.jks",
                     "release.keystore", "upload-keystore.properties", "play-service-account.json",
                     "tools/play-service-account.json"):
            result = subprocess.run(["git", "check-ignore", "-q", "--no-index", path], cwd=TOOLS.parent)
            self.assertEqual(result.returncode, 0, f"{path} is not ignored by git")


if __name__ == "__main__":
    unittest.main()
