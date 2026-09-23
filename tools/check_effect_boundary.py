#!/usr/bin/env python3
import argparse
import re
import sys
from datetime import datetime, timezone
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import check_effects_api as api
import port
import regen_effects

ROOT = api.ROOT
KOTLIN_ROOT = api.KOTLIN_ROOT
SNAPSHOT = HERE / "effect_boundary_snapshot.json"
NAMED_FILES = [
    KOTLIN_ROOT / "media/haishinkit/media/video/VideoEffect.kt",
    KOTLIN_ROOT / "media/haishinkit/media/video/VideoEffectsProcessor.kt",
]
PACKAGE_RE = re.compile(r"^package\s+([\w.]+)", re.M)
IMPORT_RE = re.compile(r"^import\s+([\w.]+)(?:\s+as\s+(\w+))?\s*$", re.M)
TOP_LEVEL_TYPE_RE = re.compile(
    r"^(?:(?:public|internal|private|open|abstract|final|data|sealed|enum|annotation|value|inline|fun)\s+)*"
    r"(?:class|interface|object|typealias)\s+(\w+)", re.M
)
DECLARATION_RE = re.compile(
    r"^(?:@\w+\s+)?(?:(?:public|open|abstract|final|data|sealed|enum|inner|annotation|value|inline|operator|infix|"
    r"suspend|const|lateinit|tailrec|external|actual|expect|companion)\s+)*"
    r"(?P<keyword>fun\s+interface|fun|class|interface|object|val|var|typealias|constructor)\b(?P<rest>.*)$"
)
RECEIVER_NAME_RE = re.compile(r"^\s*(?:<[^>]*>\s*)?(?:(?P<receiver>[\w.<>?*, ]+?)\.)?(?P<name>`?\w+`?)")
TYPE_NAME_RE = re.compile(r"^\s*(?P<name>\w+)")
IDENTIFIER_RE = re.compile(r"(?<![\w.])([A-Z]\w*)\b")


def effect_files():
    files = set(api.kotlin_files(KOTLIN_ROOT / "videoeffects"))
    files.update(path for path in NAMED_FILES if path.exists())
    inventory = regen_effects.load_inventory()
    for entry in regen_effects.regenerated_entries(inventory):
        path = ROOT / entry["kotlin_path"]
        if path.exists():
            files.add(path)
    return sorted(files)


def package_types():
    types = {}
    for path in api.kotlin_files(ROOT / "app/src/main/java"):
        text = api.read_text(path)
        package = PACKAGE_RE.search(text)
        if package:
            types.setdefault(package.group(1), set()).update(TOP_LEVEL_TYPE_RE.findall(text))
    return types


def imports_of(text):
    imports = {}
    for match in IMPORT_RE.finditer(text):
        fqn, alias = match.group(1), match.group(2)
        imports[alias or fqn.rsplit(".", 1)[-1]] = fqn
    return imports


def qualify(signature, imports, package, types):
    def replace(match):
        name = match.group(1)
        if name in imports:
            return imports[name]
        if name in types.get(package, ()):
            return f"{package}.{name}"
        return name
    return IDENTIFIER_RE.sub(replace, signature)


def declaration_key(signature, owners):
    match = DECLARATION_RE.match(signature)
    if match is None:
        if signature.endswith(";"):
            return ".".join(owners + ["<entries>"]), None
        return None, None
    keyword = " ".join(match.group("keyword").split())
    rest = match.group("rest")
    if keyword == "constructor":
        return ".".join(owners + ["<init>"]), None
    if keyword in ("class", "interface", "object", "fun interface", "typealias"):
        name = TYPE_NAME_RE.match(rest)
        if name is None:
            name_text = "Companion"
        else:
            name_text = name.group("name")
        key = ".".join(owners + [name_text])
        return key, (name_text if keyword != "typealias" else None)
    name = RECEIVER_NAME_RE.match(rest)
    if name is None:
        return None, None
    parts = list(owners)
    if name.group("receiver"):
        parts.append(re.sub(r"<.*$", "", name.group("receiver")).rstrip("?").split(".")[-1])
    parts.append(name.group("name").strip("`"))
    return ".".join(parts), None


def declarations(files, types):
    result = {}
    for path in files:
        text = api.read_text(path)
        package_match = PACKAGE_RE.search(text)
        package = package_match.group(1) if package_match else ""
        imports = imports_of(text)
        stack = []
        for depth, signature in port.api_declarations(text):
            while stack and stack[-1][0] >= depth:
                stack.pop()
            owners = [name for _, name in stack]
            key, type_name = declaration_key(signature, owners)
            if key is None:
                continue
            entry = result.setdefault(key, {"packages": set(), "signatures": set(), "files": set()})
            entry["packages"].add(package)
            entry["signatures"].add(qualify(signature, imports, package, types))
            entry["files"].add(path.relative_to(ROOT).as_posix())
            if type_name is not None:
                stack.append((depth, type_name))
    return result


def serializable(result):
    return {
        key: {
            "packages": sorted(value["packages"]),
            "signatures": sorted(value["signatures"]),
            "files": sorted(value["files"]),
        }
        for key, value in sorted(result.items())
    }


def take_snapshot():
    files = effect_files()
    data = {
        "generated": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "files": {path.relative_to(ROOT).as_posix(): api.sha256(api.read_text(path)) for path in files},
        "declarations": serializable(declarations(files, package_types())),
    }
    api.save_json(SNAPSHOT, data)
    print(f"stored {len(data['declarations'])} declarations of {len(files)} effect files in {SNAPSHOT}")


def stale_files():
    snapshot = api.load_json(SNAPSHOT, None)
    if snapshot is None:
        return None
    stored = snapshot.get("files", {})
    current = {path.relative_to(ROOT).as_posix(): api.sha256(api.read_text(path)) for path in effect_files()}
    return sorted(rel for rel in set(stored) | set(current) if stored.get(rel) != current.get(rel))


def outside_files(effect_paths):
    effect_set = {path.resolve() for path in effect_paths}
    for directory in regen_effects.SOURCE_DIRS:
        for path in api.kotlin_files(directory):
            if path.resolve() not in effect_set:
                yield path


def refers_to(text, name, packages, file_package, imports):
    for package in packages:
        if file_package == package or f"{package}.{name}" in text or imports.get(name) == f"{package}.{name}":
            return True
        if re.search(r"^import\s+" + re.escape(package) + r"\.\*\s*$", text, re.M):
            return True
    return False


def reference_lines(key, before, texts):
    parts = key.split(".")
    name = parts[-1]
    if name in ("<init>", "<entries>", "Companion"):
        name = parts[-2] if len(parts) > 1 else name
        parts = parts[:-1]
    packages = before["packages"]
    member = len(parts) > 1
    owner = parts[0]
    if member:
        pattern = re.compile(r"(?:\.|::)\s*" + re.escape(name) + r"\b")
    else:
        pattern = re.compile(r"(?<![\w.])" + re.escape(name) + r"\b")
    found = []
    for path, text in texts.items():
        if member:
            if owner not in text:
                continue
        else:
            package_match = PACKAGE_RE.search(text)
            file_package = package_match.group(1) if package_match else ""
            if not refers_to(text, name, packages, file_package, imports_of(text)):
                continue
        for number, line in enumerate(text.split("\n"), 1):
            if line.startswith(("import ", "package ")):
                continue
            if pattern.search(line):
                found.append(f"{path.relative_to(ROOT).as_posix()}:{number}: {line.strip()[:160]}")
    return found


def diff():
    snapshot = api.load_json(SNAPSHOT, None)
    if snapshot is None:
        sys.exit(f"{SNAPSHOT} is missing. Run python tools/check_effect_boundary.py --snapshot before the regeneration.")
    files = effect_files()
    current = serializable(declarations(files, package_types()))
    texts = {path: api.read_text(path) for path in outside_files(files)}
    findings = []
    for key, before in snapshot["declarations"].items():
        after = current.get(key)
        if after is not None and after["signatures"] == before["signatures"]:
            continue
        references = reference_lines(key, before, texts)
        if not references:
            continue
        findings.append({
            "check": "boundary",
            "name": key,
            "file": before["files"][0] if before["files"] else None,
            "message": "removed" if after is None else "signature changed",
            "before": before["signatures"],
            "after": after["signatures"] if after else [],
            "references": references,
        })
    gaps, problems = api.load_gaps()
    boundary_gaps = [gap for gap in gaps if gap["check"] == "boundary"]
    open_findings = [finding for finding in findings if api.gap_for(finding, boundary_gaps) is None]
    covered = len(findings) - len(open_findings)
    for finding in open_findings:
        print(f"{finding['name']}: {finding['message']}")
        for signature in finding["before"]:
            print(f"  before: {signature}")
        for signature in finding["after"]:
            print(f"  after:  {signature}")
        print("  referenced by:")
        for line in finding["references"][:20]:
            print(f"    {line}")
        if len(finding["references"]) > 20:
            print(f"    ... and {len(finding['references']) - 20} more")
    for problem in problems:
        print(f"  INVALID {problem}")
    print(f"check_effect_boundary: {len(open_findings)} changed declarations referenced outside the effects, "
          f"{covered} known gaps (snapshot of {snapshot['generated']})")
    return 1 if open_findings or problems else 0


def main():
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    parser = argparse.ArgumentParser(
        description="Snapshot the public declarations of the effect files before the regeneration, and list "
        "afterwards each changed declaration that a file outside the effects references."
    )
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--snapshot", action="store_true", help=f"store the declarations in {SNAPSHOT.name}")
    mode.add_argument("--diff", action="store_true", help="compare with the snapshot; exits with 1 on a finding")
    args = parser.parse_args()
    if args.snapshot:
        take_snapshot()
        return
    sys.exit(diff())


if __name__ == "__main__":
    main()
