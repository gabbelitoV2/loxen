#!/usr/bin/env python3
import argparse
import json
import os
import re
import subprocess
import sys
import threading
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import port
import postprocess

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
ERROR_RE = re.compile(r"^e: file:///(.+?):(\d+):(\d+) (.*)$")
JDK_CANDIDATES = [
    Path(r"C:\Program Files\Java\jdk-21"),
    Path(r"C:\Program Files\Java\jdk-17"),
    Path(r"C:\Program Files\Android\Android Studio\jbr"),
]

FIX_SYSTEM = """You are fixing Kotlin compile errors in an Android port of the Moblin iOS app. The Kotlin file was machine translated from the Swift file shown for reference. You receive the current Kotlin file and the compiler errors for it.

Rules
- Return the complete corrected Kotlin file in a single ```kotlin block and nothing else. Start with the package line.
- Change only what the errors require. Keep every other line byte-identical, including formatting and declaration order.
- Do not write comments. TODO("reason") is the only allowed marker for something that cannot be implemented.
- Never use UByteArray, UByte, UShort, UInt or ULong. Use ByteArray, Byte, Short, Int and Long with explicit masking (x.toInt() and 0xff) where the Swift code used unsigned types.
- When a symbol is unresolved and the glossary lists it, add the import. When it is a member of a type declared in another file, do not redeclare the type; adapt the call to what the errors say exists.
- When a symbol is private in another file, do not copy it; change the call site to an equivalent or, if the symbol is a small helper, declare a private copy in this file.
- When a symbol comes from an iOS-only or missing library and cannot exist here, replace the use with TODO("...") in the smallest possible scope.
- Keep public signatures unchanged unless an error is about the signature itself. When a call does not match a declaration listed under the dependencies, change the call, not the declaration.
- @Composable functions may take `model: Model = LocalModel.current` and `onNavigate: (String) -> Unit = LocalOnNavigate.current`; keep those defaults, and add them when a composable needs the model or navigation and callers do not pass it.
- Observable state on model classes is `val name = MutableStateFlow(...)`. Read it in composables with `val x by name.collectAsState()` and write it with `name.value = ...`. When the dependency declarations show a property as a plain val or var instead, access it directly without .value or collectAsState()."""


def find_java_home():
    env = os.environ.get("JAVA_HOME")
    if env and Path(env, "bin", "java.exe").exists():
        return env
    for candidate in JDK_CANDIDATES:
        if (candidate / "bin" / "java.exe").exists():
            return str(candidate)
    return None


def compile_kotlin(task="compileDebugKotlin"):
    env = dict(os.environ)
    java_home = find_java_home()
    if java_home:
        env["JAVA_HOME"] = java_home
    gradlew = ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew")
    result = subprocess.run(
        [str(gradlew), f":app:{task}", "-q"], cwd=ROOT, env=env,
        capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=1800,
    )
    return result.stdout + result.stderr


def parse_errors(output):
    errors = {}
    current = None
    for line in output.splitlines():
        match = ERROR_RE.match(line)
        if match:
            path = Path(match.group(1))
            current = (path, int(match.group(2)), match.group(4))
            errors.setdefault(path, []).append([current[1], current[2]])
        elif current and line.startswith("    ") and errors[current[0]]:
            errors[current[0]][-1][1] += " " + line.strip()
    return errors


def load_inventory():
    inventory = json.loads((HERE / "inventory.json").read_text(encoding="utf-8"))
    by_kotlin = {ROOT / e["kotlin_path"]: e for e in inventory["files"]}
    by_path = {e["path"]: e for e in inventory["files"]}
    return inventory, by_kotlin, by_path


def build_prompt(kotlin_path, kotlin_source, errors, entry, swift_source, glossary, dependencies):
    parts = [f"Kotlin file: {kotlin_path.relative_to(ROOT).as_posix()}"]
    if entry:
        parts.append(f"Kotlin package: {entry['kotlin_package']}")
    parts.append("Compiler errors (line: message):\n" + "\n".join(f"{line}: {message}" for line, message in errors))
    if glossary:
        parts.append("Types, functions and globals from other files and their packages:\n" + "\n".join(glossary))
    if dependencies:
        parts.append("Current Kotlin declarations in the files this file depends on. Match these exactly:\n" + dependencies)
    parts.append("```kotlin\n" + kotlin_source + "\n```")
    if swift_source:
        parts.append("Original Swift for reference:\n```swift\n" + swift_source + "\n```")
    return "\n\n".join(parts)


def fix_one(backend, kotlin_path, errors, by_kotlin, by_path, moblin_root):
    kotlin_source = kotlin_path.read_text(encoding="utf-8", errors="replace")
    entry = by_kotlin.get(kotlin_path)
    swift_source = None
    glossary = []
    dependencies = ""
    if entry:
        swift_file = moblin_root / entry["path"]
        if swift_file.exists():
            swift_source = swift_file.read_text(encoding="utf-8", errors="replace")
        glossary = port.glossary_for(entry, by_path)
        dependencies = port.dependency_signatures(entry, by_path, ROOT)
    prompt = build_prompt(kotlin_path, kotlin_source, errors, entry, swift_source, glossary, dependencies)
    started = time.time()
    text, tokens_in, tokens_out = backend.complete(FIX_SYSTEM, prompt)
    blocks = port.fenced_blocks(text)
    kotlin = next((body for language, body in blocks if language in ("kotlin", "kt")), None)
    if kotlin is None or not kotlin.lstrip().startswith("package "):
        raise port.PortError("the response had no kotlin block")
    kotlin, _ = postprocess.process_file(kotlin.rstrip() + "\n", set())
    kotlin_path.write_text(kotlin, encoding="utf-8", newline="\n")
    return round(time.time() - started, 1), tokens_in, tokens_out


def main():
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    port.load_dotenv(ROOT / ".env")
    parser = argparse.ArgumentParser(description="Feed Kotlin compile errors back to the LLM until the project compiles.")
    parser.add_argument("--provider", choices=sorted(port.PROVIDERS), default="anthropic")
    parser.add_argument("--backend", choices=["auto", "api", "cli"], default="auto")
    parser.add_argument("--model", default=None)
    parser.add_argument("--effort", default="high")
    parser.add_argument("--workers", type=int, default=4)
    parser.add_argument("--rounds", type=int, default=3)
    parser.add_argument("--max-files", type=int, default=0)
    parser.add_argument("--task", default="compileDebugKotlin")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    if args.model is None:
        args.model = port.PROVIDERS[args.provider]["model"]

    inventory, by_kotlin, by_path = load_inventory()
    moblin_root = Path(inventory["root"])
    if not find_java_home():
        sys.exit("No JDK 17+ found. Set JAVA_HOME.")

    backend = None
    previous_count = None
    for round_number in range(1, args.rounds + 1):
        postprocess.run(False)
        print(f"round {round_number}: compiling...")
        output = compile_kotlin(args.task)
        errors = parse_errors(output)
        count = sum(len(v) for v in errors.values())
        print(f"round {round_number}: {count} errors in {len(errors)} files")
        if count == 0:
            print("the project compiles")
            return
        if previous_count is not None and count >= previous_count:
            print("no improvement since the last round, stopping")
            (ROOT / "build-errors.log").write_text(output, encoding="utf-8", newline="\n")
            return
        previous_count = count
        def wave_of(path):
            entry = by_kotlin.get(path)
            return entry["wave"] if entry else 1_000_000

        files = sorted(errors.items(), key=lambda item: (wave_of(item[0]), -len(item[1])))
        if args.max_files:
            files = files[: args.max_files]
        if args.dry_run:
            for path, file_errors in files:
                print(f"  wave {wave_of(path):>3}  {len(file_errors):>4}  {path.relative_to(ROOT).as_posix()}")
            (ROOT / "build-errors.log").write_text(output, encoding="utf-8", newline="\n")
            return
        if backend is None:
            backend = port.pick_backend(args)
            print(f"backend {backend.name} ({args.provider}), model {args.model}, effort {args.effort}, {args.workers} workers")
        lock = threading.Lock()
        finished = 0

        def work(item):
            path, file_errors = item
            try:
                return path, fix_one(backend, path, file_errors, by_kotlin, by_path, moblin_root), None
            except Exception as exc:
                return path, None, str(exc)

        waves = {}
        for item in files:
            waves.setdefault(wave_of(item[0]), []).append(item)
        for wave in sorted(waves):
            with ThreadPoolExecutor(max_workers=args.workers) as pool:
                for future in as_completed([pool.submit(work, item) for item in waves[wave]]):
                    path, result, error = future.result()
                    with lock:
                        finished += 1
                        rel = path.relative_to(ROOT).as_posix()
                        if error:
                            print(f"  [{finished}/{len(files)}] FAIL {rel}: {error[:200]}")
                        else:
                            print(f"  [{finished}/{len(files)}] ok   {rel} ({result[0]}s)")
    print("compiling after the last round...")
    output = compile_kotlin(args.task)
    errors = parse_errors(output)
    count = sum(len(v) for v in errors.values())
    (ROOT / "build-errors.log").write_text(output, encoding="utf-8", newline="\n")
    print(f"{count} errors in {len(errors)} files remain, see build-errors.log")


if __name__ == "__main__":
    main()
