#!/usr/bin/env python3
import argparse
import json
import os
import re
import sys
import threading
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
K = ROOT / "app/src/main/java/com/moblin/android"
STATE = HERE / "refine-state.json"
PROVIDERS = {
    "deepseek": ("https://api.deepseek.com/anthropic", "deepseek-flash", "DEEPSEEK_API_KEY"),
    "anthropic": (None, "claude-opus-5", "ANTHROPIC_API_KEY"),
}
TOP_LEVEL_RE = re.compile(r"^(?:@\w+\s+)*(?:(?:private|internal|public|data|sealed|enum|open|abstract)\s+)*(?:fun|class|object|val|var|interface)\s+(?:<[^>]*>\s*)?(?:[\w.]+\.)?(\w+)", re.M)


def load_dotenv():
    env = ROOT / ".env"
    if env.exists():
        for line in env.read_text(encoding="utf-8-sig").splitlines():
            if "=" in line and not line.strip().startswith("#"):
                key, value = line.split("=", 1)
                os.environ.setdefault(key.strip(), value.strip().strip("\"'"))


def library_api():
    text = (K / "platform/swiftui/SwiftUI.kt").read_text(encoding="utf-8")
    lines = text.split("\n")
    sigs = []
    i = 0
    while i < len(lines):
        line = lines[i]
        if line.startswith("@Composable") and i + 1 < len(lines):
            i += 1
            line = lines[i]
        if re.match(r"^(fun|val|class) ", line):
            sig = line
            j = i
            while sig.count("(") > sig.count(")") and j + 1 < len(lines):
                j += 1
                sig += " " + lines[j].strip()
            sigs.append(re.sub(r"\s*\{\s*$", "", sig))
            i = j
        i += 1
    return "\n".join(sigs)


def symbols():
    text = (K / "platform/SystemImages.kt").read_text(encoding="utf-8")
    return " ".join(sorted(set(re.findall(r'"([^"]+)" to Icons', text))))


def signatures(path, limit=60):
    if not path.exists():
        return []
    out = []
    for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        if re.match(r"^(@Composable\s+)?(fun|class|object|val|var|data class|enum class|sealed class) ", line) and not line.startswith("private"):
            out.append(re.sub(r"\s*\{.*$", "", line.strip()))
        if len(out) >= limit:
            break
    return out


def dependency_api(entry, by_path):
    parts = []
    for dep in entry.get("deps", []):
        other = by_path.get(dep)
        if not other or other["tier"] == "skip":
            continue
        sigs = signatures(ROOT / other["kotlin_path"])
        if sigs:
            parts.append(f"// {other['kotlin_package']}\n" + "\n".join(sigs))
    return "\n\n".join(parts)[:40000]


SYSTEM = """You are refining the Jetpack Compose translation of one SwiftUI file from Moblin, an iOS IRL streaming app, so that on Android it looks and behaves exactly like the SwiftUI original on iOS.

You receive the SwiftUI source, the current Kotlin translation, the component library and the declarations of the files it depends on. Return the refined Kotlin file.

Rules:
- Keep every top-level declaration: same names, same parameters and defaults, same order. Other files call them.
- Keep all behaviour: every model call, state change, binding and navigation target in the current Kotlin stays. Only the presentation changes, plus fixes where the Kotlin clearly deviates from the Swift (a call replaced by Unit or TODO while the Kotlin declaration exists, a value read once instead of observed).
- Build settings pages, forms and lists from the component library below exactly where the SwiftUI uses Form, Section, NavigationLink, Toggle, Picker, Label, Button in a Form, Slider and .sheet. Never use Scaffold, TopAppBar, Material Switch, ExposedDropdownMenu, Card, ListItem or filled Material buttons for those.
- Keep calls into com.moblin.android.platform, AppDelegate.context and any other Android-specific code in the current Kotlin.
- No comments. Do not invent APIs; call only declarations you can see here or that exist in Compose foundation, Material 3 (for AlertDialog, Text, Icon only) and the Kotlin standard library.

Respond with exactly two fenced blocks: a ```json block {"notes": ["..."]} with at most 5 notes, then a ```kotlin block with the whole file starting with the package line.
"""


def fenced(text):
    blocks, language, buffer = [], None, []
    for line in text.split("\n"):
        stripped = line.strip()
        if language is None:
            if stripped.startswith("```"):
                language, buffer = stripped[3:].strip().lower(), []
        elif stripped == "```":
            blocks.append((language, "\n".join(buffer)))
            language = None
        else:
            buffer.append(line)
    return blocks


def refine_one(client, model, system, entry, by_path, upstream):
    swift = (upstream / entry["path"]).read_text(encoding="utf-8", errors="replace")
    target = ROOT / entry["kotlin_path"]
    kotlin = target.read_text(encoding="utf-8", errors="replace")
    user = "\n\n".join([
        f"Swift file: {entry['path']}",
        f"Kotlin file: {entry['kotlin_path']} (package {entry['kotlin_package']})",
        "Declarations of the files this file depends on:\n" + dependency_api(entry, by_path),
        "SwiftUI source:\n```swift\n" + swift + "\n```",
        "Current Kotlin:\n```kotlin\n" + kotlin + "\n```",
    ])
    started = time.time()
    with client.messages.stream(model=model, max_tokens=200000, system=system,
                                messages=[{"role": "user", "content": user}]) as stream:
        message = stream.get_final_message()
    text = "".join(b.text for b in message.content if b.type == "text")
    new = next((body for language, body in fenced(text) if language in ("kotlin", "kt")), None)
    if not new or not new.lstrip().startswith("package "):
        raise RuntimeError("no kotlin block")
    before = {n for n in TOP_LEVEL_RE.findall(kotlin)}
    after = {n for n in TOP_LEVEL_RE.findall(new)}
    lost = sorted(before - after)
    public_lost = [n for n in lost if re.search(r"^(?!private)(?:@\w+\s+)*(?:fun|class|object|val|var)\s+(?:<[^>]*>\s*)?(?:[\w.]+\.)?" + n + r"\b", kotlin, re.M)]
    if public_lost:
        raise RuntimeError("lost declarations: " + ", ".join(public_lost))
    target.write_text(new.rstrip() + "\n", encoding="utf-8", newline="\n")
    return round(time.time() - started, 1), message.usage.input_tokens, message.usage.output_tokens


def main():
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    load_dotenv()
    parser = argparse.ArgumentParser(description="Refine SwiftUI translations to match iOS, using the SwiftUI-style components.")
    parser.add_argument("--include", nargs="+", default=["Moblin/View/Utils/", "Moblin/View/Settings/", "Moblin/View/ControlBar/QuickButton/"])
    parser.add_argument("--exclude", nargs="+", default=["Moblin/View/Settings/SettingsView.swift"])
    parser.add_argument("--provider", default="deepseek", choices=sorted(PROVIDERS))
    parser.add_argument("--workers", type=int, default=8)
    parser.add_argument("--limit", type=int, default=0)
    parser.add_argument("--force", action="store_true")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    import anthropic
    base_url, model, key_env = PROVIDERS[args.provider]
    options = {"api_key": os.environ[key_env]}
    if base_url:
        options["base_url"] = base_url
    client = anthropic.Anthropic(**options)

    inventory = json.loads((HERE / "inventory.json").read_text(encoding="utf-8"))
    upstream = Path(inventory["root"])
    by_path = {e["path"]: e for e in inventory["files"]}
    state = json.loads(STATE.read_text(encoding="utf-8")) if STATE.exists() else {}
    todo = []
    for entry in sorted(inventory["files"], key=lambda e: (e["wave"], e["path"])):
        if entry["tier"] == "skip" or not entry["path"].startswith(tuple(args.include)):
            continue
        if entry["path"].startswith(tuple(args.exclude)):
            continue
        target = ROOT / entry["kotlin_path"]
        if not target.exists():
            continue
        text = target.read_text(encoding="utf-8", errors="replace")
        if "import com.moblin.android.platform.swiftui" in text and not args.force:
            continue
        if state.get(entry["path"], {}).get("status") == "ok" and not args.force:
            continue
        todo.append(entry)
    if args.limit:
        todo = todo[: args.limit]
    print(f"{len(todo)} files to refine with {model}")
    if args.dry_run:
        for entry in todo:
            print("  " + entry["path"])
        return

    system = SYSTEM + "\n\n" + (HERE / "prompts/ui.md").read_text(encoding="utf-8") + \
        "\n\n# Component library (package com.moblin.android.platform.swiftui, import com.moblin.android.platform.swiftui.*)\n\n" + \
        library_api() + "\n\n# SF Symbol names that SystemImage maps\n\n" + symbols()
    lock = threading.Lock()
    done = 0

    def work(entry):
        try:
            return entry, refine_one(client, model, system, entry, by_path, upstream), None
        except Exception as exc:
            return entry, None, str(exc)

    by_wave = {}
    for entry in todo:
        by_wave.setdefault(entry["wave"], []).append(entry)
    for wave in sorted(by_wave):
        with ThreadPoolExecutor(max_workers=args.workers) as pool:
            for future in as_completed([pool.submit(work, e) for e in by_wave[wave]]):
                entry, result, error = future.result()
                with lock:
                    done += 1
                    if error:
                        state[entry["path"]] = {"status": "error", "error": error}
                        print(f"[{done}/{len(todo)}] FAIL {entry['path']}: {error[:150]}")
                    else:
                        state[entry["path"]] = {"status": "ok", "seconds": result[0], "tokens_in": result[1], "tokens_out": result[2]}
                        print(f"[{done}/{len(todo)}] ok   {entry['path']} ({result[0]}s)")
                    STATE.write_text(json.dumps(state, indent=2), encoding="utf-8", newline="\n")


if __name__ == "__main__":
    main()
