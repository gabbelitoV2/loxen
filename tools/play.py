#!/usr/bin/env python3
import argparse
import json
import os
import sys

API = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications"
SCOPE = "https://www.googleapis.com/auth/androidpublisher"
TIMEOUT = 60


class PlayError(Exception):
    pass


class AppMissing(PlayError):
    pass


def session_for(service_account_json):
    from google.auth.transport.requests import AuthorizedSession
    from google.oauth2 import service_account

    try:
        info = json.loads(service_account_json)
    except json.JSONDecodeError as error:
        raise PlayError(f"PLAY_SERVICE_ACCOUNT_JSON is not JSON: {error.msg}") from None
    credentials = service_account.Credentials.from_service_account_info(info, scopes=[SCOPE])
    return AuthorizedSession(credentials)


def error_message(response):
    try:
        return response.json()["error"]["message"]
    except (ValueError, KeyError, TypeError):
        return response.text[:300]


def checked(response, what):
    if response.status_code >= 400:
        raise PlayError(f"{what}: HTTP {response.status_code}: {error_message(response)}")
    return response.json()


def highest_version_code(session, package):
    response = session.post(f"{API}/{package}/edits", json={}, timeout=TIMEOUT)
    if response.status_code == 404:
        raise AppMissing(error_message(response))
    edit = checked(response, "create an edit")["id"]
    try:
        tracks = checked(session.get(f"{API}/{package}/edits/{edit}/tracks", timeout=TIMEOUT), "list the tracks")
        bundles = checked(session.get(f"{API}/{package}/edits/{edit}/bundles", timeout=TIMEOUT), "list the bundles")
    finally:
        session.delete(f"{API}/{package}/edits/{edit}", timeout=TIMEOUT)
    codes = [
        int(code)
        for track in tracks.get("tracks", [])
        for release in track.get("releases", [])
        for code in release.get("versionCodes", [])
    ]
    codes += [int(bundle["versionCode"]) for bundle in bundles.get("bundles", []) if "versionCode" in bundle]
    return max(codes, default=0)


def decide(package, version_code, event, service_account_json, session_factory=None):
    manual = event == "workflow_dispatch"
    if not service_account_json:
        return manual, False, ("PLAY_SERVICE_ACCOUNT_JSON is not set, so nothing is uploaded to Google Play"
                               + (". The signed app bundle is attached to this run." if manual else "."))
    try:
        highest = highest_version_code((session_factory or session_for)(service_account_json), package)
    except AppMissing as error:
        return manual, False, (f"{package} is not in Google Play Console yet ({error}). Create the app and upload the "
                               "first app bundle by hand, see README.md.")
    if highest >= version_code:
        return manual, False, (f"versionCode {version_code} is not higher than {highest} on Google Play, so main has "
                               "not changed since the last upload.")
    return True, True, f"versionCode {version_code} is new (highest on Google Play: {highest}), uploading."


def append(variable, text):
    path = os.environ.get(variable)
    if path:
        with open(path, "a", encoding="utf-8") as file:
            file.write(text)


def main(argv=None):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    parser = argparse.ArgumentParser(description="Decide whether the Play workflow builds and uploads the app bundle. "
                                                 "Reads the service account key from PLAY_SERVICE_ACCOUNT_JSON.")
    subparsers = parser.add_subparsers(dest="command", required=True)
    decide_parser = subparsers.add_parser("decide", help="write build=true|false and upload=true|false to "
                                                         "GITHUB_OUTPUT")
    decide_parser.add_argument("--package", required=True)
    decide_parser.add_argument("--version-code", type=int, required=True)
    decide_parser.add_argument("--event", required=True, help="github.event_name")
    args = parser.parse_args(argv)
    try:
        build, upload, reason = decide(args.package, args.version_code, args.event,
                                       os.environ.get("PLAY_SERVICE_ACCOUNT_JSON", "").strip())
    except PlayError as error:
        print(f"::error::Google Play: {error}. Check that the service account has access to the app in Play Console "
              "(Users and permissions) and that the Google Play Android Developer API is enabled.")
        return 1
    print(f"::notice::{reason}")
    append("GITHUB_OUTPUT", f"build={str(build).lower()}\nupload={str(upload).lower()}\n")
    append("GITHUB_STEP_SUMMARY", f"## Google Play\n\n{reason}\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
