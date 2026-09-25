#!/usr/bin/env python3
import copy
import re
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
SOURCE_DIRS = ("app/src/main/java", "app/src/test/java")
KOTLIN_PREFIX = "app/src/main/java/com/moblin/android/"
PBSWIFT_PREFIX = KOTLIN_PREFIX + "integrations/tesla/protobuf/"
ERROR_RE = re.compile(r"^e: (?:file://)?(.+?\.kts?):(\d+):(\d+) (.*)$")
QUOTED_RE = re.compile(r"'([\w.]+)'")
DECLARATION_RE = re.compile(r"\b(?:class|interface|object|fun|val|var|typealias)\s+(?:<[^>\n]*>\s*)?(?:[\w.]+\.)?(\w+)")
WINDOWS_DRIVE_RE = re.compile(r"^/[A-Za-z]:/")
MAX_ERRORS_PER_FILE = 20
MAX_ERROR_LENGTH = 400


def relative_path(raw, root):
    if WINDOWS_DRIVE_RE.match(raw):
        raw = raw[1:]
    path = Path(raw)
    if not path.is_absolute():
        path = Path("/" + raw)
    try:
        return path.resolve().relative_to(Path(root).resolve()).as_posix()
    except ValueError:
        return path.as_posix()


def parse_errors(output, root=ROOT):
    errors = {}
    current = None
    for line in output.splitlines():
        match = ERROR_RE.match(line)
        if match:
            current = relative_path(match.group(1), root)
            errors.setdefault(current, []).append([int(match.group(2)), match.group(4).strip()])
        elif current and line.startswith("    ") and errors[current]:
            errors[current][-1][1] += " " + line.strip()
    return errors


def format_errors(file_errors):
    return [f"{line}: {message}"[:MAX_ERROR_LENGTH] for line, message in file_errors[:MAX_ERRORS_PER_FILE]]


def quoted_names(errors):
    names = set()
    for file_errors in errors.values():
        for _, message in file_errors:
            for quoted in QUOTED_RE.findall(message):
                names.add(quoted.rsplit(".", 1)[-1])
    return names


class Snapshot:
    def __init__(self, root, state, directories=SOURCE_DIRS):
        self.root = Path(root)
        self.directories = directories
        self.files = self.read()
        self.state = copy.deepcopy(state)

    def read(self):
        files = {}
        for directory in self.directories:
            base = self.root / directory
            if not base.exists():
                continue
            for path in base.rglob("*"):
                if path.is_file():
                    files[path.relative_to(self.root).as_posix()] = path.read_bytes()
        return files

    def changed(self):
        now = self.read()
        return sorted(rel for rel in set(self.files) | set(now) if self.files.get(rel) != now.get(rel))

    def declarations(self, rel):
        path = self.root / rel
        texts = [self.files.get(rel, b"").decode("utf-8", errors="replace")]
        if path.exists():
            texts.append(path.read_text(encoding="utf-8", errors="replace"))
        return {name for text in texts for name in DECLARATION_RE.findall(text)}

    def revert(self, rel, state):
        path = self.root / rel
        if rel in self.files:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(self.files[rel])
        elif path.exists():
            path.unlink()
        swift = sorted(
            {key for key, entry in state.items() if entry.get("kotlin_path") == rel}
            | {key for key, entry in self.state.items() if entry.get("kotlin_path") == rel}
        )
        for key in swift:
            if key in self.state:
                state[key] = copy.deepcopy(self.state[key])
            else:
                state.pop(key, None)
        return swift


def dependencies(inventory):
    by_path = {entry["path"]: entry for entry in inventory.get("files", [])}
    result = {}
    for entry in inventory.get("files", []):
        if entry["tier"] == "skip":
            continue
        kotlin = {
            by_path[dep]["kotlin_path"] for dep in entry.get("deps", [])
            if dep in by_path and by_path[dep]["tier"] != "skip"
        }
        result.setdefault(entry["kotlin_path"], set()).update(kotlin)
    return result


def choose_reverts(errors, changed, missing_hooks, declarations, depends_on, build_ok):
    targets = {}
    for rel in errors:
        if rel in changed:
            targets[rel] = "compile"
    for rel in missing_hooks:
        if rel in changed and rel not in targets:
            targets[rel] = "hook"
    if targets or build_ok:
        return targets
    names = quoted_names(errors)
    if names:
        for rel in sorted(changed):
            if names & declarations(rel):
                targets[rel] = "dependency"
    for rel in errors:
        for dependency in depends_on.get(rel, ()):
            if dependency in changed:
                targets.setdefault(dependency, "dependency")
    if targets:
        return targets
    return {rel: "fallback" for rel in changed}


def missing_hooks():
    import check_hooks
    import postprocess

    missing = {}
    results, _, _ = check_hooks.collect()
    for result in results:
        if result["state"] != "missing":
            continue
        hook = result["hook"]
        if postprocess.is_glob(hook["file"]):
            continue
        missing.setdefault(KOTLIN_PREFIX + hook["file"], []).append(
            f"{postprocess.hook_label(hook)}: {result['detail']}"
        )
    return missing


def build_once_more_on_hiccup(build, root, log):
    ok, output = build()
    errors = parse_errors(output, root)
    if not ok and not errors:
        log("the build failed without a Kotlin error, building once more before holding anything back")
        ok, output = build()
        errors = parse_errors(output, root)
    return ok, output, errors


def stabilize(snapshot, state, build, hook_problems, depends_on, save_state, log=print, max_rounds=20,
              targeted_rounds=8):
    reverted = {}
    for round_number in range(max_rounds):
        ok, output, errors = build_once_more_on_hiccup(build, snapshot.root, log)
        missing = hook_problems()
        changed = set(snapshot.changed())
        targets = choose_reverts(errors, changed, missing, snapshot.declarations, depends_on, ok)
        if not targets:
            break
        if round_number + 1 >= targeted_rounds and not ok:
            targets = {rel: "fallback" for rel in changed}
        for rel, reason in sorted(targets.items()):
            swift = snapshot.revert(rel, state)
            reverted[rel] = {
                "reason": reason,
                "swift": swift,
                "errors": format_errors(errors.get(rel, [])),
                "hooks": missing.get(rel, []),
            }
            log(f"held back {rel} ({reason}, {len(errors.get(rel, []))} errors)")
        save_state(state)
    else:
        ok, output, errors = build_once_more_on_hiccup(build, snapshot.root, log)
    return {"ok": ok, "errors": errors, "output": output, "reverted": reverted}
