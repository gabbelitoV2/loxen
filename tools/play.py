#!/usr/bin/env python3
import argparse
import json
import os
import re
import subprocess
import sys
import time
from pathlib import Path

API = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications"
SCOPE = "https://www.googleapis.com/auth/androidpublisher"
TIMEOUT = 60
RETRIES = 3
RETRY_DELAY = 10
VERSION_CODE_BASE = 1000
MOBLIN_COMMITS = "https://api.github.com/repos/eerimoq/moblin/compare/{old}...{new}"
NOTES_URL = "https://api.deepseek.com/anthropic/v1/messages"
NOTES_MODEL = "deepseek-flash"
NOTES_LIMIT = 500
NOTES_LANGUAGE = "en-US"
TRANSLATIONS = {"sv-SE": "Swedish"}
DEFAULT_NOTES = "• Bug fixes and improvements."
DEFAULT_TRANSLATIONS = {"sv-SE": "• Buggfixar och förbättringar."}
SYNC = re.compile(r"^Sync with eerimoq/moblin ([0-9a-f]{7,40})")
REPAIR = re.compile(r"^Repair the Android port after the sync")
NOTES_SYSTEM = """You write the "What's new" text of a Google Play release of Loxen, an Android app for IRL live
streaming that is ported from Moblin, an iOS app.
You get Loxen's own changes and the changes from Moblin that this release brings in, one per line.
Write at most 6 lines of at most 80 characters, each starting with "• ", most important first, in plain
English for streamers.
Only mention changes users notice: features, fixes and changed behaviour. Leave out tests, tooling, builds,
refactoring, translations, version bumps and anything that only concerns Apple devices (iPhone, iPad, Mac, Apple
Watch, widgets, Live Activities).
The change lines are written for developers; most of them still describe something users notice, so explain
those in plain words. Do not invent anything that is not in the changes. Keep the whole text under 450
characters. Only if really none of the changes is visible to users, write exactly: • Bug fixes and improvements."""
TRANSLATE_SYSTEM = """You translate the "What's new" text of a Google Play release of Loxen, an Android app for IRL live
streaming, into {language}. Keep every line, its order and its leading "• ". Write natural everyday {language} for
streamers, at most 80 characters per line and under 480 characters in all; shorten the wording rather than dropping a
line. Keep names as they are (Loxen, Moblin, Samsung, VRM, PNGTuber, RTMP, SRT, RIST, WHIP, OBS, Twitch, YouTube,
Kick). Answer with the translated lines only."""


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


def retried(request, delay=RETRY_DELAY):
    for attempt in range(RETRIES):
        response = request()
        if response.status_code < 500 or attempt == RETRIES - 1:
            return response
        time.sleep(delay * (attempt + 1))
    return response


def highest_version_code(session, package, delay=RETRY_DELAY):
    response = retried(lambda: session.post(f"{API}/{package}/edits", json={}, timeout=TIMEOUT), delay)
    if response.status_code == 404:
        raise AppMissing(error_message(response))
    edit = checked(response, "create an edit")["id"]
    try:
        tracks = checked(retried(lambda: session.get(f"{API}/{package}/edits/{edit}/tracks", timeout=TIMEOUT), delay),
                         "list the tracks")
        bundles = checked(retried(lambda: session.get(f"{API}/{package}/edits/{edit}/bundles", timeout=TIMEOUT),
                                  delay), "list the bundles")
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


def decide(package, version_code, event, service_account_json, session_factory=None, delay=RETRY_DELAY):
    manual = event == "workflow_dispatch"
    if not service_account_json:
        return manual, False, ("PLAY_SERVICE_ACCOUNT_JSON is not set, so nothing is uploaded to Google Play"
                               + (". The signed app bundle is attached to this run." if manual else ".")), 0
    try:
        highest = highest_version_code((session_factory or session_for)(service_account_json), package, delay)
    except AppMissing as error:
        return manual, False, (f"{package} is not in Google Play Console yet ({error}). Create the app and upload the "
                               "first app bundle by hand, see README.md."), 0
    if highest >= version_code:
        return manual, False, (f"versionCode {version_code} is not higher than {highest} on Google Play, so main has "
                               "not changed since the last upload."), highest
    return True, True, f"versionCode {version_code} is new (highest on Google Play: {highest}), uploading.", highest


def git(*arguments, cwd=None):
    return subprocess.run(["git", *arguments], cwd=cwd, capture_output=True, text=True, encoding="utf-8",
                          errors="replace", check=True).stdout


def uploaded_commit(version_code, cwd=None, limit=500):
    target = version_code - VERSION_CODE_BASE
    if target <= 0:
        return None
    for commit in git("rev-list", "--first-parent", f"--max-count={limit}", "HEAD", cwd=cwd).split():
        count = int(git("rev-list", "--count", commit, cwd=cwd))
        if count == target:
            return commit
        if count < target:
            return None
    return None


def subjects_since(commit, cwd=None):
    revisions = [f"{commit}..HEAD"] if commit else ["--max-count=20", "HEAD"]
    return [line for line in git("log", "--no-merges", "--format=%s", *revisions, cwd=cwd).splitlines() if line]


def synced_moblin_commit(revision, cwd=None):
    subject = git("log", "-1", "--format=%s", "--grep=^Sync with eerimoq/moblin ", revision, cwd=cwd).strip()
    match = SYNC.match(subject)
    return match.group(1) if match else None


def split_subjects(subjects):
    own = [subject for subject in subjects if not SYNC.match(subject) and not REPAIR.match(subject)]
    synced = [SYNC.match(subject).group(1) for subject in subjects if SYNC.match(subject)]
    return own, (synced[0] if synced else None)


def moblin_subjects(old, new, get=None):
    if get is None:
        import requests

        get = requests.get
    headers = {"Accept": "application/vnd.github+json"}
    if os.environ.get("GITHUB_TOKEN"):
        headers["Authorization"] = f"Bearer {os.environ['GITHUB_TOKEN']}"
    response = get(MOBLIN_COMMITS.format(old=old, new=new), headers=headers, timeout=TIMEOUT)
    if response.status_code >= 400:
        raise PlayError(f"compare eerimoq/moblin {old}...{new}: HTTP {response.status_code}")
    return [commit["commit"]["message"].splitlines()[0] for commit in response.json().get("commits", [])]


def completed(system, user, api_key, post=None):
    if post is None:
        import requests

        post = requests.post
    response = post(
        NOTES_URL,
        headers={"x-api-key": api_key, "anthropic-version": "2023-06-01", "content-type": "application/json"},
        json={"model": NOTES_MODEL, "max_tokens": 2000, "temperature": 0, "thinking": {"type": "disabled"},
              "system": system, "messages": [{"role": "user", "content": user}]},
        timeout=TIMEOUT,
    )
    if response.status_code >= 400:
        raise PlayError(f"ask DeepSeek: HTTP {response.status_code}")
    blocks = response.json().get("content", [])
    text = "".join(block.get("text", "") for block in blocks if block.get("type") == "text").strip()
    if not text:
        raise PlayError("the model returned no text")
    return text


def summarized(own, moblin, api_key, post=None):
    user = ("Loxen changes:\n" + ("\n".join(own) or "(none)") + "\n\nChanges from Moblin:\n"
            + ("\n".join(moblin) or "(none)"))
    return completed(NOTES_SYSTEM, user, api_key, post=post)


def translated_notes(text, languages, api_key, post=None):
    translations = {}
    for language in languages:
        if language not in TRANSLATIONS:
            continue
        if text == DEFAULT_NOTES:
            translations[language] = DEFAULT_TRANSLATIONS[language]
            continue
        if not api_key:
            continue
        try:
            system = TRANSLATE_SYSTEM.format(language=TRANSLATIONS[language])
            answer = completed(system, text, api_key, post=post)
            lines = [line.strip() for line in answer.splitlines() if line.strip().startswith("• ")]
            if not lines:
                raise PlayError(f"the translation has no lines starting with •: {answer[:100]}")
            translations[language] = fitted("\n".join(lines))
        except Exception as error:
            print(f"::warning::Release notes in {language}: {error}, Google Play shows the English ones")
    return translations


def listing_languages(session, package):
    edit = checked(retried(lambda: session.post(f"{API}/{package}/edits", json={}, timeout=TIMEOUT)),
                   "create an edit")["id"]
    try:
        listings = checked(retried(lambda: session.get(f"{API}/{package}/edits/{edit}/listings", timeout=TIMEOUT)),
                           "list the store listings")
    finally:
        session.delete(f"{API}/{package}/edits/{edit}", timeout=TIMEOUT)
    return {listing["language"] for listing in listings.get("listings", []) if "language" in listing}


def notes_languages(package, service_account_json, session_factory=None):
    wanted = [NOTES_LANGUAGE, *TRANSLATIONS]
    if not service_account_json or not package:
        return wanted
    try:
        listed = listing_languages((session_factory or session_for)(service_account_json), package)
    except Exception as error:
        print(f"::warning::Release notes: {error}, writing only {NOTES_LANGUAGE}")
        return [NOTES_LANGUAGE]
    return [language for language in wanted if language == NOTES_LANGUAGE or language in listed]


def short(subject, limit=110):
    sentence = re.split(r"(?<=[a-z0-9)])[.;:] ", subject, maxsplit=1)[0].rstrip(".")
    return sentence if len(sentence) <= limit else sentence[: limit - 1].rstrip() + "…"


def plain_notes(own, moblin):
    lines = [f"• {short(subject)}." for subject in own[:5]]
    if moblin:
        lines.append(f"• {len(moblin)} changes from Moblin.")
    return "\n".join(lines)


def fitted(text, limit=NOTES_LIMIT):
    lines = [line.rstrip() for line in text.strip().splitlines() if line.strip()]
    kept = []
    for line in lines:
        if len("\n".join(kept + [line])) > limit:
            break
        kept.append(line)
    if not kept and lines:
        kept = [lines[0][: limit - 1].rstrip() + "…"]
    return "\n".join(kept) or DEFAULT_NOTES


def release_notes(previous_version_code, api_key, cwd=None, get=None, post=None):
    commit = uploaded_commit(previous_version_code, cwd=cwd) if previous_version_code else None
    own, new_moblin = split_subjects(subjects_since(commit, cwd=cwd))
    moblin = []
    old_moblin = synced_moblin_commit(commit, cwd=cwd) if commit else None
    if new_moblin and old_moblin and new_moblin != old_moblin:
        try:
            moblin = moblin_subjects(old_moblin, new_moblin, get=get)
        except Exception as error:
            print(f"::warning::Release notes: {error}")
    if not own and not moblin:
        return DEFAULT_NOTES
    if api_key:
        try:
            return fitted(summarized(own, moblin, api_key, post=post))
        except Exception as error:
            print(f"::warning::Release notes: {error}, using the commit subjects")
    return fitted(plain_notes(own, moblin))


def write_notes(directory, notes):
    path = Path(directory)
    path.mkdir(parents=True, exist_ok=True)
    for language, text in notes.items():
        (path / f"whatsnew-{language}").write_text(text, encoding="utf-8")


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
    notes_parser = subparsers.add_parser("notes", help="write the release notes (What's new) to "
                                                       "DIRECTORY/whatsnew-<language>, in English and translated to "
                                                       "the store listing's other languages; DEEPSEEK_API_KEY writes "
                                                       "them, without it they list the commit subjects in English")
    notes_parser.add_argument("--previous-version-code", type=int, default=0,
                              help="the highest versionCode on Google Play before this upload")
    notes_parser.add_argument("--package", default="", help="limit the languages to the store listing's")
    notes_parser.add_argument("--directory", required=True)
    args = parser.parse_args(argv)
    if args.command == "notes":
        api_key = os.environ.get("DEEPSEEK_API_KEY", "").strip()
        try:
            text = release_notes(args.previous_version_code, api_key)
        except Exception as error:
            print(f"::warning::Release notes: {error}")
            text = DEFAULT_NOTES
        languages = notes_languages(args.package, os.environ.get("PLAY_SERVICE_ACCOUNT_JSON", "").strip())
        notes = {NOTES_LANGUAGE: text, **translated_notes(text, languages, api_key)}
        write_notes(args.directory, notes)
        summary = "\n\n".join(f"{language}:\n\n{notes[language]}" for language in notes)
        print(summary)
        append("GITHUB_STEP_SUMMARY", f"## What's new\n\n{summary}\n")
        return 0
    try:
        build, upload, reason, previous = decide(args.package, args.version_code, args.event,
                                                 os.environ.get("PLAY_SERVICE_ACCOUNT_JSON", "").strip())
    except PlayError as error:
        print(f"::error::Google Play: {error}. Check that the service account has access to the app in Play Console "
              "(Users and permissions) and that the Google Play Android Developer API is enabled.")
        return 1
    print(f"::notice::{reason}")
    append("GITHUB_OUTPUT", f"build={str(build).lower()}\nupload={str(upload).lower()}\nprevious={previous}\n")
    append("GITHUB_STEP_SUMMARY", f"## Google Play\n\n{reason}\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
