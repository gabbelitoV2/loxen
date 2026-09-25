#!/usr/bin/env python3
import argparse
import difflib
import json
import os
import re
import shutil
import subprocess
import sys
import threading
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timezone
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import postprocess

PROMPTS = HERE / "prompts"
SHIMS = PROMPTS / "shims"
KOTLIN_PACKAGE_DIR = Path("app/src/main/java/com/moblin/android")
PLATFORM_API_LIMIT = 600
ALL_TIERS = ["logic", "platform", "test", "media", "ui", "apple_only"]
DEFAULT_TIERS = ["logic", "platform", "test"]
PROVIDERS = {
    "anthropic": {"base_url": None, "model": "claude-opus-5", "key_env": "ANTHROPIC_API_KEY"},
    "deepseek": {
        "base_url": "https://api.deepseek.com/anthropic",
        "model": "deepseek-flash",
        "key_env": "DEEPSEEK_API_KEY",
    },
}
PRICES = {
    "claude-opus-5": (5.0, 25.0),
    "claude-sonnet-5": (2.0, 10.0),
    "claude-haiku-4-5": (1.0, 5.0),
    "deepseek-flash": (0.15, 0.60),
    "deepseek-v4-pro": (0.66, 1.98),
}


class PortError(Exception):
    pass


def now_iso():
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


def estimate_tokens(text):
    return len(text) // 3 + 1


class ApiBackend:
    name = "api"

    def __init__(self, model, effort, provider):
        try:
            import anthropic
        except ImportError:
            sys.exit("The anthropic package is missing. Run: pip install anthropic")
        options = {}
        if provider["base_url"]:
            options["base_url"] = provider["base_url"]
        api_key = os.environ.get(provider["key_env"])
        if api_key:
            options["api_key"] = api_key
        self.client = anthropic.Anthropic(**options)
        self.model = model
        self.effort = effort
        self.fallbacks = provider["base_url"] is None

    def complete(self, system, user):
        kwargs = {
            "model": self.model,
            "max_tokens": 200000,
            "system": [{"type": "text", "text": system, "cache_control": {"type": "ephemeral"}}],
            "messages": [{"role": "user", "content": user}],
            "output_config": {"effort": self.effort},
        }
        if self.fallbacks:
            stream = self.client.beta.messages.stream(
                betas=["server-side-fallback-2026-07-01"], fallbacks="default", **kwargs
            )
        else:
            stream = self.client.messages.stream(**kwargs)
        with stream as s:
            message = s.get_final_message()
        if message.stop_reason == "refusal":
            raise PortError("the model refused to translate the file")
        if message.stop_reason == "max_tokens":
            raise PortError("the response was cut off at max_tokens")
        text = "".join(block.text for block in message.content if block.type == "text")
        usage = message.usage
        tokens_in = (
            usage.input_tokens
            + (usage.cache_read_input_tokens or 0)
            + (usage.cache_creation_input_tokens or 0)
        )
        return text, tokens_in, usage.output_tokens


class CliBackend:
    name = "cli"

    def __init__(self, model, effort):
        self.exe = shutil.which("claude.exe") or shutil.which("claude")
        if not self.exe:
            sys.exit("claude was not found in PATH. Install Claude Code or use --backend api.")
        self.model = model
        self.effort = effort

    def complete(self, system, user):
        prompt = "<instructions>\n" + system + "\n</instructions>\n\n" + user
        command = [
            self.exe, "-p", "--no-session-persistence", "--output-format", "text", "--tools", "",
            "--model", self.model, "--effort", self.effort,
        ]
        env = {key: value for key, value in os.environ.items() if key != "CLAUDECODE"}
        result = subprocess.run(
            command, input=prompt, capture_output=True, text=True, encoding="utf-8", errors="replace",
            env=env, timeout=3600,
        )
        if result.returncode != 0:
            detail = (result.stderr or result.stdout).strip()[-600:]
            raise PortError(detail or f"claude exited with code {result.returncode}")
        return result.stdout, estimate_tokens(prompt), estimate_tokens(result.stdout)


def load_json(path, default):
    if path.exists():
        return json.loads(path.read_text(encoding="utf-8"))
    return default


def save_json(path, data):
    tmp = path.with_suffix(".tmp")
    tmp.write_text(json.dumps(data, indent=2, ensure_ascii=False), encoding="utf-8", newline="\n")
    tmp.replace(path)


INCREMENTAL = (PROMPTS / "incremental.md").read_text(encoding="utf-8")


def load_prompts():
    system = (PROMPTS / "system.md").read_text(encoding="utf-8")
    tiers = {tier: (PROMPTS / f"{tier}.md").read_text(encoding="utf-8") for tier in ALL_TIERS}
    return system, tiers


SHIM_INTRO = (
    "The hand-written platform layer implements these Apple APIs under their Apple names, so a translation keeps "
    "the Swift calls. Translate every use listed below to the shim as described. Never replace them with TODO(), "
    "never declare stand-ins for them and never call the Android API underneath from generated code."
)


SCOPE_LINE_RE = re.compile(r"^<!--\s*scope:(.*?)-->$")
FRAGMENT_CACHE = {}


def shim_fragments():
    if "fragments" not in FRAGMENT_CACHE:
        fragments = []
        if SHIMS.exists():
            for path in sorted(SHIMS.glob("*.md"), key=lambda p: postprocess.natural_key(p.name)):
                if path.name.lower() == "readme.md":
                    continue
                text = path.read_text(encoding="utf-8-sig").strip()
                first, _, rest = text.partition("\n")
                match = SCOPE_LINE_RE.match(first.strip())
                scopes = None
                if match:
                    scopes = tuple(part.strip() for part in match.group(1).split(",") if part.strip())
                    text = rest.strip()
                if text:
                    fragments.append({"name": path.name, "scopes": scopes, "text": text})
        FRAGMENT_CACHE["fragments"] = fragments
    return FRAGMENT_CACHE["fragments"]


def fragment_applies(fragment, swift_path):
    return swift_path is None or fragment["scopes"] is None or swift_path.startswith(fragment["scopes"])


def shim_fragment_names(swift_path=None):
    return tuple(fragment["name"] for fragment in shim_fragments() if fragment_applies(fragment, swift_path))


def shim_glossary(swift_path=None):
    parts = [fragment["text"] for fragment in shim_fragments() if fragment_applies(fragment, swift_path)]
    if not parts:
        return ""
    return "# Apple API shims\n\n" + SHIM_INTRO + "\n\n" + "\n\n".join(parts)


API_DECLARATION_RE = re.compile(
    r"(?P<annotations>(?:@[\w.]+(?:\([^()]*\))?\s+)*)"
    r"(?P<modifiers>(?:(?:public|private|protected|internal|open|abstract|final|override|suspend|inline|operator|"
    r"infix|const|lateinit|data|sealed|enum|inner|annotation|value|tailrec|external|companion|actual|expect)\s+)*)"
    r"(?P<keyword>fun\s+interface|fun|class|interface|object|val|var|typealias|constructor)(?=[\s(<{:])"
)
API_TYPE_KEYWORDS = ("class", "interface", "object", "fun interface")
API_HIDDEN_MODIFIERS = {"private", "internal", "protected", "override"}
API_HEADER_ENDINGS = (",", "(", "[", ":", "->", ".", "&&", "||")
API_HEADER_STARTS = (":", "where ", "{", ")", "]", ",", "->", ".")


def api_valid_declaration(masked, match):
    keyword = " ".join(match.group("keyword").split())
    rest = masked[match.end():match.end() + 200].lstrip(" \t")
    if not rest:
        return False
    if keyword == "constructor":
        return rest[0] == "("
    if keyword == "object" and "companion" in match.group("modifiers").split():
        return rest[0] in "{:\n" or rest[0].isalpha() or rest[0] == "_"
    if keyword in ("fun", "val", "var"):
        return rest[0] == "<" or rest[0] == "`" or rest[0].isalpha() or rest[0] == "_"
    return rest[0] == "`" or rest[0].isalpha() or rest[0] == "_"


def api_header_continues(masked, newline):
    line_start = masked.rfind("\n", 0, newline) + 1
    if masked[line_start:newline].rstrip().endswith(API_HEADER_ENDINGS):
        return True
    following = masked[newline + 1:]
    stripped = following.lstrip()
    return stripped.startswith(API_HEADER_STARTS)


def api_header_end(masked, match):
    keyword = match.group("keyword")
    parens = 0
    i = match.end()
    while i < len(masked):
        c = masked[i]
        if c in "([":
            parens += 1
        elif c in ")]":
            parens -= 1
        elif parens <= 0:
            if c == "{":
                return i, i
            if c == "=" and keyword != "typealias" and postprocess.is_assignment(masked, i):
                return i, None
            if c == ";" or (c == "\n" and not api_header_continues(masked, i)):
                return i, None
        i += 1
    return len(masked), None


def api_initializer(masked, readable, equals):
    parens = 0
    i = equals + 1
    while i < len(masked):
        c = masked[i]
        if c in "([":
            parens += 1
        elif c in ")]":
            parens -= 1
        elif parens <= 0 and c in "{\n;":
            break
        i += 1
    value = re.sub(r"\s+", " ", readable[equals + 1:i]).strip()
    if not value:
        return ""
    if len(value) > 100:
        value = value[:100] + "..."
    return " = " + value


def api_enum_entries(masked, readable, start):
    entries = []
    parens = 0
    braces = 0
    segment_start = start
    i = start
    while i < len(masked):
        c = masked[i]
        if c in "([":
            parens += 1
        elif c in ")]":
            parens -= 1
        elif c == "{":
            braces += 1
        elif c == "}":
            if braces == 0 and parens <= 0:
                break
            braces -= 1
        elif parens <= 0 and braces == 0 and c in ",;":
            entries.append(readable[segment_start:i])
            segment_start = i + 1
            if c == ";":
                i += 1
                break
        i += 1
    else:
        i = len(masked)
    if masked[segment_start:i].strip() and (i >= len(masked) or masked[i] == "}"):
        entries.append(readable[segment_start:i])
    names = []
    for entry in entries:
        value = re.sub(r"\s+", " ", re.sub(r"^\s*(?:@\w+(?:\([^()]*\))?\s+)*", "", entry.split("{")[0])).strip()
        if value:
            names.append(value if len(value) <= 60 else value[:60] + "...")
    return i, names


def api_declarations(text):
    masked = postprocess.mask_kotlin(text)
    readable = "".join(" " if m == " " and t not in " \t" else t for m, t in zip(masked, text))
    result = []
    stack = []
    parens = 0
    i = 0
    statement_start = True
    body_brace = None
    enum_next = False
    while i < len(masked):
        c = masked[i]
        if statement_start:
            if c in " \t\r\n;":
                i += 1
                continue
            statement_start = False
            if all(block["type"] for block in stack):
                visible_level = all(block["visible"] for block in stack)
                if enum_next:
                    enum_next = False
                    end, names = api_enum_entries(masked, readable, i)
                    if names and visible_level:
                        result.append((len(stack), ", ".join(names) + ";"))
                    i = end
                    statement_start = True
                    continue
                match = API_DECLARATION_RE.match(masked, i)
                if match and api_valid_declaration(masked, match):
                    header_end, brace = api_header_end(masked, match)
                    modifiers = set(match.group("modifiers").split())
                    keyword = " ".join(match.group("keyword").split())
                    visible = visible_level and not (modifiers & API_HIDDEN_MODIFIERS)
                    if visible:
                        annotations = " ".join(
                            a for a in re.findall(r"@[\w.]+", match.group("annotations")) if a == "@Composable"
                        )
                        signature = readable[match.start("modifiers"):header_end]
                        if keyword in ("val", "var") and brace is None and header_end < len(masked) and masked[header_end] == "=":
                            head = masked[match.end():header_end]
                            if "const" in modifiers or ":" not in head:
                                signature += api_initializer(masked, readable, header_end)
                        signature = re.sub(r"\s+", " ", ((annotations + " ") if annotations else "") + signature).strip()
                        signature = re.sub(r"\s*,\s*\)", ")", re.sub(r"\(\s+", "(", signature))
                        signature = re.sub(r"\s+by\s+lazy\b.*$", "", signature)
                        result.append((len(stack), signature))
                    if brace is not None:
                        is_type = keyword in API_TYPE_KEYWORDS
                        body_brace = (brace, is_type, visible and is_type, is_type and "enum" in modifiers)
                    i = header_end
                    continue
        if c in "([":
            parens += 1
        elif c in ")]":
            parens = max(0, parens - 1)
        elif c == "{":
            if body_brace is not None and body_brace[0] == i:
                stack.append({"type": body_brace[1], "visible": body_brace[2], "parens": parens})
                enum_next = body_brace[3]
            else:
                stack.append({"type": False, "visible": False, "parens": parens})
                enum_next = False
            body_brace = None
            parens = 0
            statement_start = True
        elif c == "}":
            if stack:
                parens = stack.pop()["parens"]
            enum_next = False
        elif c in "\n;" and parens == 0:
            statement_start = True
        i += 1
    return result


def platform_api_files(out_dir, tier):
    package_dir = out_dir / KOTLIN_PACKAGE_DIR
    files = []
    sample = package_dir / "media/MediaSample.kt"
    if sample.exists():
        files.append(sample)
    platform = package_dir / "platform"
    if platform.exists():
        files += sorted(platform.rglob("*.kt"), key=lambda p: platform_api_rank(p.relative_to(platform), tier))
    return files


def platform_api_rank(relative, tier):
    parts = relative.parts
    group = parts[0] if len(parts) > 1 else ""
    order = ["", "core", "video", "avfoundation", "videotoolbox", "audio", "network", "ntp", "srt", "mp4", "uikit"]
    late = ["swiftui", "capture", "host"]
    if tier == "ui":
        order = ["", "swiftui", "uikit"] + [name for name in order if name not in ("", "uikit")]
        late = ["capture", "host"]
    if group in order:
        rank = order.index(group)
    elif group in late:
        rank = 100 + late.index(group)
    else:
        rank = 50
    return (rank, group, relative.as_posix())


API_NAME_RE = re.compile(r"\b(?:fun\s+interface|fun|class|interface|object|val|var|typealias)\s+(?:<[^>]*>\s*)?(?:[\w.<>?*, ]+\.)?(\w+)")
API_CACHE = {}


def platform_api_blocks(out_dir, tier):
    key = (str(out_dir), tier)
    if key not in API_CACHE:
        package_dir = out_dir / KOTLIN_PACKAGE_DIR
        blocks = []
        for path in platform_api_files(out_dir, tier):
            text = path.read_text(encoding="utf-8", errors="replace")
            declarations = api_declarations(text)
            if not declarations:
                continue
            package = re.search(r"^package ([\w.]+)", text, re.M)
            relative = path.relative_to(package_dir).as_posix()
            lines = [f"// {package.group(1) if package else ''} ({relative})"]
            lines += ["    " * depth + signature for depth, signature in declarations]
            names = set()
            for depth, signature in declarations:
                match = API_NAME_RE.search(signature)
                if depth == 0 and match:
                    names.add(match.group(1))
            blocks.append({"path": relative, "lines": lines, "names": names})
        API_CACHE[key] = blocks
    return API_CACHE[key]


def platform_api(out_dir, tier, limit=PLATFORM_API_LIMIT):
    lines = []
    omitted = []
    for block in platform_api_blocks(out_dir, tier):
        room = limit - len(lines)
        if room <= 1:
            omitted.append(block["path"])
            continue
        if len(block["lines"]) > room:
            lines += block["lines"][:room] + [""]
            omitted.append(block["path"] + " (in part)")
            continue
        lines += block["lines"] + [""]
    if not lines:
        return ""
    if omitted:
        lines.append(
            "// Left out for room: " + ", ".join(omitted) + ". When the Swift file uses one of their names, the "
            "request lists that file's declarations."
        )
    return (
        "# Platform API\n\n"
        "The current public declarations of the hand-written platform layer (every file under "
        "com.moblin.android.platform, and com.moblin.android.media.MediaSample). Members are indented under their "
        "type, bodies are left out. Import from the package in each file's header line and call these exactly as "
        "declared. Never redeclare them in a generated file.\n\n"
        "```kotlin\n" + "\n".join(lines).rstrip() + "\n```"
    )


def referenced_platform_api(out_dir, tier, source, limit=PLATFORM_API_LIMIT):
    listed = 0
    omitted = []
    for block in platform_api_blocks(out_dir, tier):
        room = limit - listed
        listed += len(block["lines"]) + 1
        if room > 1 and len(block["lines"]) <= room:
            continue
        omitted.append(block)
    chosen = set()
    text = source
    while text:
        found = [
            index for index, block in enumerate(omitted)
            if index not in chosen and any(re.search(r"\b" + re.escape(name) + r"\b", text) for name in block["names"])
        ]
        chosen.update(found)
        text = "\n".join(line for index in found for line in omitted[index]["lines"])
    return "\n\n".join("\n".join(block["lines"]) for index, block in enumerate(omitted) if index in chosen)


PROMPT_CACHE = {}


def system_prompt(system, tiers, tier, out_dir, incremental=False, swift_path=None):
    key = (tier, str(out_dir), shim_fragment_names(swift_path))
    if key not in PROMPT_CACHE:
        sections = [system.rstrip(), shim_glossary(swift_path), platform_api(out_dir, tier), tiers[tier].rstrip()]
        PROMPT_CACHE[key] = "\n\n".join(section for section in sections if section)
    prompt = PROMPT_CACHE[key]
    if incremental:
        prompt += "\n\n" + INCREMENTAL
    return prompt


def select_entries(inventory, args):
    tiers = ALL_TIERS if "all" in args.tier else args.tier
    entries = [e for e in inventory["files"] if e["tier"] in tiers]
    if args.include:
        entries = [e for e in entries if e["path"].startswith(tuple(args.include))]
    entries.sort(key=lambda e: (e["wave"], e["path"]))
    return entries


def needs_port(entry, state, force):
    previous = state.get(entry["path"])
    if force or previous is None:
        return True
    return previous.get("status") != "ok" or previous.get("sha256") != entry["sha256"]


def glossary_for(entry, by_path):
    rows = []
    for dep in entry["deps"]:
        other = by_path.get(dep)
        if other is None or other["tier"] == "skip":
            continue
        private_types = set(other.get("private_types", []))
        for name in other["declared_types"]:
            if name in private_types:
                continue
            rows.append(f"- {name} -> {other['kotlin_package']}")
        for name in other["declared_functions"]:
            rows.append(f"- fun {name}() -> {other['kotlin_package']}")
        for name in other["declared_globals"]:
            rows.append(f"- val {name} -> {other['kotlin_package']}")
    return rows


DECLARATION_RE = re.compile(
    r"^\s*(?:@\w+(?:\([^)]*\))?\s+)*(?:(?:public|internal|open|abstract|override|suspend|inline|operator|infix|const|lateinit|companion)\s+)*"
    r"(?:(?:fun|class|data class|sealed class|enum class|object|interface|val|var|typealias)\s|constructor\()"
)


ENUM_CLASS_RE = re.compile(r"\benum\s+class\b")
HIDDEN_MODIFIER_RE = re.compile(r"^(\s*(?:@\w+(?:\([^)]*\))?\s+)*)(?:private|protected)\s+")


def enum_entry_names(text, masked, line_offset):
    brace = masked.find("{", line_offset)
    if brace == -1 or "}" in masked[line_offset:brace] or len(ENUM_CLASS_RE.findall(masked[line_offset:brace])) != 1:
        return []
    readable = "".join(" " if m == " " and t not in " \t" else t for m, t in zip(masked, text))
    _, names = api_enum_entries(masked, readable, brace + 1)
    return names


def strip_declaration_body(signature):
    depth = 0
    for index, character in enumerate(signature):
        if character == "(":
            depth += 1
        elif character == ")":
            depth -= 1
        elif character == "{" and depth <= 0:
            return signature[:index].rstrip()
    return signature


def signatures(path, limit=80):
    if not path.exists():
        return []
    text = path.read_text(encoding="utf-8", errors="replace")
    raw_lines = text.splitlines(keepends=True)
    lines = [line.rstrip("\r\n") for line in raw_lines]
    offsets = []
    offset = 0
    for line in raw_lines:
        offsets.append(offset)
        offset += len(line)
    masked = None
    result = []
    index = 0
    hidden_indent = None
    while index < len(lines) and len(result) < limit:
        line = lines[index]
        line_offset = offsets[index]
        index += 1
        hidden = HIDDEN_MODIFIER_RE.match(line)
        if not DECLARATION_RE.match(HIDDEN_MODIFIER_RE.sub(r"\1", line, count=1) if hidden else line):
            continue
        indent = len(line) - len(line.lstrip())
        if hidden_indent is not None and indent > hidden_indent:
            continue
        hidden_indent = None
        if hidden:
            hidden_indent = indent
            continue
        signature = line.rstrip()
        while signature.count("(") > signature.count(")") and index < len(lines):
            signature += " " + lines[index].strip()
            index += 1
        signature = strip_declaration_body(signature)
        result.append(signature)
        if ENUM_CLASS_RE.search(signature):
            if masked is None:
                masked = postprocess.mask_kotlin(text)
            names = enum_entry_names(text, masked, line_offset)
            if names:
                result.append("    " + ", ".join(names) + ";")
    return result


def dependency_signatures(entry, by_path, out_dir):
    sections = []
    for dep in entry.get("deps", []):
        other = by_path.get(dep)
        if other is None or other["tier"] == "skip":
            continue
        lines = signatures(out_dir / other["kotlin_path"])
        if lines:
            sections.append(f"// {other['kotlin_package']} ({Path(other['kotlin_path']).name})\n" + "\n".join(lines))
    return "\n\n".join(sections)[:60000]


def build_user_prompt(entry, glossary, source, dependencies=""):
    parts = [
        f"Swift file: {entry['path']}",
        f"Tier: {entry['tier']}",
        f"Kotlin package: {entry['kotlin_package']}",
        f"Kotlin file: {entry['kotlin_path']}",
    ]
    if glossary:
        parts.append(
            "Types, top-level functions and globals from other files, with the Kotlin package they live in. "
            "Import them from there when used:\n" + "\n".join(glossary)
        )
    if dependencies:
        parts.append(
            "Current Kotlin declarations in the files this file depends on. Call them exactly as declared:\n"
            + dependencies
        )
    parts.append("```swift\n" + source + "\n```")
    return "\n\n".join(parts)


def fenced_blocks(text):
    blocks = []
    language = None
    buffer = []
    for line in text.split("\n"):
        stripped = line.strip()
        if language is None:
            if stripped.startswith("```"):
                language = stripped[3:].strip().lower()
                buffer = []
        elif stripped == "```":
            blocks.append((language, "\n".join(buffer)))
            language = None
        else:
            buffer.append(line)
    if language is not None and buffer:
        blocks.append((language, "\n".join(buffer)))
    return blocks


def parse_response(text):
    blocks = fenced_blocks(text)
    kotlin = next((body for language, body in blocks if language in ("kotlin", "kt")), None)
    if kotlin is None:
        raise PortError("the response had no kotlin block")
    if not kotlin.lstrip().startswith("package "):
        raise PortError("the kotlin block does not start with a package line")
    meta = {}
    json_body = next((body for language, body in blocks if language == "json"), None)
    if json_body:
        try:
            meta = json.loads(json_body)
        except json.JSONDecodeError:
            meta = {}
    return meta, kotlin


def git_head(root):
    result = subprocess.run(
        ["git", "-C", str(root), "rev-parse", "HEAD"], capture_output=True, text=True, encoding="utf-8"
    )
    return result.stdout.strip() if result.returncode == 0 else None


def swift_at(root, commit, path):
    if not commit:
        return None
    result = subprocess.run(
        ["git", "-C", str(root), "show", f"{commit}:{path}"],
        capture_output=True, text=True, encoding="utf-8", errors="replace",
    )
    return result.stdout if result.returncode == 0 else None


def changed_lines(old, new):
    count = 0
    for line in difflib.unified_diff(old.splitlines(), new.splitlines(), lineterm="", n=0):
        if line.startswith(("+", "-")) and not line.startswith(("+++", "---")):
            count += 1
    return count


def build_incremental_prompt(entry, glossary, old_source, source, kotlin, dependencies):
    swift_diff = "\n".join(
        difflib.unified_diff(
            old_source.splitlines(), source.splitlines(), f"a/{entry['path']}", f"b/{entry['path']}", lineterm=""
        )
    )
    parts = [
        f"Swift file: {entry['path']}",
        f"Tier: {entry['tier']}",
        f"Kotlin package: {entry['kotlin_package']}",
        f"Kotlin file: {entry['kotlin_path']}",
    ]
    if glossary:
        parts.append(
            "Types, top-level functions and globals from other files, with the Kotlin package they live in. "
            "Import them from there when used:\n" + "\n".join(glossary)
        )
    if dependencies:
        parts.append(
            "Current Kotlin declarations in the files this file depends on. Call them exactly as declared:\n"
            + dependencies
        )
    parts.append("Swift change:\n```diff\n" + swift_diff + "\n```")
    parts.append("New Swift file:\n```swift\n" + source + "\n```")
    parts.append("Current Kotlin file:\n```kotlin\n" + kotlin + "\n```")
    return "\n\n".join(parts)


def build_prompts(entry, root, out_dir, by_path, system, tiers, previous=None, incremental=False):
    source = (root / entry["path"]).read_text(encoding="utf-8", errors="replace")
    target = out_dir / entry["kotlin_path"]
    glossary = glossary_for(entry, by_path)
    dependencies = dependency_signatures(entry, by_path, out_dir)
    platform = referenced_platform_api(out_dir, entry["tier"], source)
    if platform:
        dependencies = "\n\n".join(part for part in (dependencies, platform) if part)
    old_source = None
    if incremental and previous and previous.get("status") == "ok" and target.exists():
        old_source = swift_at(root, previous.get("moblin_commit"), entry["path"])
    if old_source is not None:
        kotlin_before = target.read_text(encoding="utf-8", errors="replace")
        return {
            "mode": "incremental",
            "system": system_prompt(system, tiers, entry["tier"], out_dir, incremental=True, swift_path=entry["path"]),
            "user": build_incremental_prompt(entry, glossary, old_source, source, kotlin_before, dependencies),
            "kotlin_before": kotlin_before,
            "swift_changed": changed_lines(old_source, source),
        }
    return {
        "mode": "full",
        "system": system_prompt(system, tiers, entry["tier"], out_dir, swift_path=entry["path"]),
        "user": build_user_prompt(entry, glossary, source, dependencies),
    }


def port_one(backend, entry, root, out_dir, by_path, system, tiers, previous=None, incremental=False, commit=None):
    target = out_dir / entry["kotlin_path"]
    prompts = build_prompts(entry, root, out_dir, by_path, system, tiers, previous, incremental)
    started = time.time()
    mode = prompts["mode"]
    warning = None
    if mode == "incremental":
        kotlin_before = prompts["kotlin_before"]
        swift_changed = prompts["swift_changed"]
        prompt_system = prompts["system"]
        prompt_user = prompts["user"]
        limit = max(60, 5 * swift_changed + 20)
        text, tokens_in, tokens_out = backend.complete(prompt_system, prompt_user)
        meta, kotlin = parse_response(text)
        kotlin_changed = changed_lines(kotlin_before, kotlin)
        if kotlin_changed > limit:
            retry = (
                prompt_user
                + f"\n\nA previous answer changed {kotlin_changed} Kotlin lines although the Swift change only touches "
                f"{swift_changed} lines. Change only the Kotlin lines that correspond to the Swift diff."
            )
            text2, more_in, more_out = backend.complete(prompt_system, retry)
            tokens_in += more_in
            tokens_out += more_out
            meta2, kotlin2 = parse_response(text2)
            changed2 = changed_lines(kotlin_before, kotlin2)
            if changed2 < kotlin_changed:
                meta, kotlin, kotlin_changed = meta2, kotlin2, changed2
            if kotlin_changed > limit:
                warning = f"changed {kotlin_changed} Kotlin lines for {swift_changed} changed Swift lines"
    else:
        text, tokens_in, tokens_out = backend.complete(prompts["system"], prompts["user"])
        meta, kotlin = parse_response(text)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(kotlin.rstrip() + "\n", encoding="utf-8", newline="\n")
    result = {
        "status": "ok",
        "sha256": entry["sha256"],
        "moblin_commit": commit,
        "mode": mode,
        "tier": entry["tier"],
        "kotlin_path": entry["kotlin_path"],
        "notes": list(meta.get("notes", []))[:8],
        "unsupported": list(meta.get("unsupported", [])),
        "model": backend.model,
        "backend": backend.name,
        "tokens_in": tokens_in,
        "tokens_out": tokens_out,
        "seconds": round(time.time() - started, 1),
        "ported_at": now_iso(),
    }
    if warning:
        result["warning"] = warning
    return result


def find_entry(path, root, by_path):
    wanted = path.strip().replace("\\", "/")
    candidate = Path(wanted)
    if candidate.is_absolute():
        try:
            wanted = candidate.resolve().relative_to(root).as_posix()
        except ValueError:
            pass
    wanted = wanted.removeprefix("./")
    if wanted in by_path:
        return by_path[wanted]
    matches = [entry for key, entry in by_path.items() if key.endswith("/" + wanted)]
    if len(matches) == 1:
        return matches[0]
    if matches:
        sys.exit(f"{path} matches several Swift files: " + ", ".join(entry["path"] for entry in matches))
    sys.exit(f"{path} is not in the inventory")


def print_prompt(path, root, out_dir, by_path, state, system, tiers, incremental):
    entry = find_entry(path, root, by_path)
    if entry["tier"] == "skip":
        sys.exit(f"{entry['path']} is in the skip tier and is never ported")
    prompts = build_prompts(entry, root, out_dir, by_path, system, tiers, state.get(entry["path"]), incremental)
    included = shim_fragment_names(entry["path"])
    excluded = [fragment["name"] for fragment in shim_fragments() if fragment["name"] not in included]
    print(f"=== shim fragments: included {', '.join(included) or 'none'}; "
          f"left out by scope {', '.join(excluded) or 'none'} ===")
    print(f"=== system prompt ({prompts['mode']}, {len(prompts['system']):,} characters, "
          f"about {estimate_tokens(prompts['system']):,} tokens) ===")
    print(prompts["system"])
    print(f"\n=== user prompt ({len(prompts['user']):,} characters, about {estimate_tokens(prompts['user']):,} tokens) ===")
    print(prompts["user"])


def dry_run(todo, root, args, system, tiers, out_dir):
    per_tier = {}
    tokens_in = 0
    tokens_out = 0
    for entry in todo:
        source = (root / entry["path"]).read_text(encoding="utf-8", errors="replace")
        source_tokens = estimate_tokens(source)
        tokens_in += estimate_tokens(system_prompt(system, tiers, entry["tier"], out_dir, swift_path=entry["path"])) + source_tokens
        tokens_out += int(source_tokens * 1.1) + 200
        row = per_tier.setdefault(entry["tier"], [0, 0])
        row[0] += 1
        row[1] += entry["lines"]
    print(f"{'tier':<12}{'files':>8}{'lines':>10}")
    for tier in ALL_TIERS:
        if tier in per_tier:
            print(f"{tier:<12}{per_tier[tier][0]:>8}{per_tier[tier][1]:>10}")
    print(f"{'total':<12}{len(todo):>8}{sum(r[1] for r in per_tier.values()):>10}")
    print(f"\nestimated tokens in: {tokens_in:,}  out: {tokens_out:,}")
    price = PRICES.get(args.model)
    if price:
        cost = tokens_in / 1e6 * price[0] + tokens_out / 1e6 * price[1]
        if args.provider == "deepseek":
            print(f"estimated cost with {args.model}: ${cost:,.2f} off-peak, ${cost * 2:,.2f} during peak hours "
                  "(01:00-04:00 and 06:00-10:00 UTC on weekdays)")
        else:
            print(f"estimated API cost with {args.model}: ${cost:,.0f} (free with the cli backend)")
    if len(todo) <= 60:
        print()
        for entry in todo:
            print(f"  wave {entry['wave']:>3}  {entry['tier']:<10} {entry['path']} -> {entry['kotlin_path']}")


def write_report(out_dir, inventory, state):
    lines = ["# Port report", "", f"Generated {now_iso()}", "", "## Summary", ""]
    statuses = {}
    for entry in inventory["files"]:
        saved = state.get(entry["path"])
        if entry["tier"] == "skip":
            status = "skipped"
        elif saved is None:
            status = "pending"
        elif saved.get("status") != "ok":
            status = "error"
        elif saved.get("sha256") != entry["sha256"]:
            status = "stale"
        else:
            status = "done"
        statuses.setdefault(entry["tier"], {}).setdefault(status, 0)
        statuses[entry["tier"]][status] += 1
    lines.append("| tier | done | error | stale | pending | skipped |")
    lines.append("|---|---|---|---|---|---|")
    for tier in ALL_TIERS + ["skip"]:
        row = statuses.get(tier)
        if row:
            lines.append(
                f"| {tier} | {row.get('done', 0)} | {row.get('error', 0)} | {row.get('stale', 0)} | "
                f"{row.get('pending', 0)} | {row.get('skipped', 0)} |"
            )
    manual = [(p, s) for p, s in sorted(state.items()) if s.get("status") == "ok" and s.get("unsupported")]
    if manual:
        lines += ["", "## Needs manual work", ""]
        for path, saved in manual:
            lines.append(f"- {path}")
            for item in saved["unsupported"]:
                lines.append(f"  - {item}")
    errors = [(p, s) for p, s in sorted(state.items()) if s.get("status") == "error"]
    if errors:
        lines += ["", "## Errors", ""]
        for path, saved in errors:
            lines.append(f"- {path}: {saved.get('error', '')}")
    done = [(p, s) for p, s in sorted(state.items()) if s.get("status") == "ok"]
    if done:
        lines += ["", "## Ported files", "", "| Swift | Kotlin | model | seconds |", "|---|---|---|---|"]
        for path, saved in done:
            lines.append(f"| {path} | {saved['kotlin_path']} | {saved.get('model', '')} | {saved.get('seconds', '')} |")
    (out_dir / "PORT-REPORT.md").write_text("\n".join(lines) + "\n", encoding="utf-8", newline="\n")


def load_dotenv(path):
    if not path.exists():
        return
    for line in path.read_text(encoding="utf-8-sig").splitlines():
        line = line.strip().removeprefix("export ")
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        os.environ.setdefault(key.strip(), value.strip().strip("\"'"))


def pick_backend(args):
    provider = PROVIDERS[args.provider]
    backend = args.backend
    if backend == "auto":
        backend = "api" if os.environ.get(provider["key_env"]) or args.provider != "anthropic" else "cli"
    if backend == "cli" and args.provider != "anthropic":
        sys.exit(f"{args.provider} only works with --backend api")
    if backend == "api":
        if args.provider != "anthropic" and not os.environ.get(provider["key_env"]):
            sys.exit(f"Set the {provider['key_env']} environment variable to your {args.provider} API key")
        return ApiBackend(args.model, args.effort, provider)
    return CliBackend(args.model, args.effort)


def main():
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    load_dotenv(HERE.parent / ".env")
    parser = argparse.ArgumentParser(description="Translate Moblin's Swift files to Kotlin with an LLM.")
    parser.add_argument("--inventory", type=Path, default=HERE / "inventory.json")
    parser.add_argument("--moblin", type=Path, default=None)
    parser.add_argument("--out", type=Path, default=HERE.parent)
    parser.add_argument("--tier", nargs="+", default=DEFAULT_TIERS, choices=ALL_TIERS + ["all"])
    parser.add_argument("--include", nargs="+", default=[])
    parser.add_argument("--limit", type=int, default=0)
    parser.add_argument("--backend", choices=["auto", "api", "cli"], default="auto")
    parser.add_argument("--provider", choices=sorted(PROVIDERS), default="anthropic")
    parser.add_argument("--model", default=None)
    parser.add_argument("--effort", default="high")
    parser.add_argument("--workers", type=int, default=2)
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--force", action="store_true")
    parser.add_argument("--report-only", action="store_true")
    parser.add_argument("--incremental", action="store_true")
    parser.add_argument("--no-postprocess", action="store_true",
                        help="do not run tools/postprocess.py (rules and hooks) on the files written")
    parser.add_argument("--print-prompt", metavar="SWIFT_PATH", default=None,
                        help="print the system and user prompt for one Swift file and stop; with --incremental, "
                        "the incremental prompt when the file was ported before")
    args = parser.parse_args()
    if args.model is None:
        args.model = PROVIDERS[args.provider]["model"]

    if not args.inventory.exists():
        sys.exit(f"{args.inventory} is missing. Run python tools/inventory.py first.")
    inventory = load_json(args.inventory, None)
    root = (args.moblin or Path(inventory["root"])).resolve()
    out_dir = args.out.resolve()
    state_path = HERE / "port-state.json"
    state = load_json(state_path, {})
    by_path = {e["path"]: e for e in inventory["files"]}

    if args.report_only:
        write_report(out_dir, inventory, state)
        print(f"wrote {out_dir / 'PORT-REPORT.md'}")
        return

    system, tiers = load_prompts()
    if args.print_prompt:
        print_prompt(args.print_prompt, root, out_dir, by_path, state, system, tiers, args.incremental)
        return
    entries = select_entries(inventory, args)
    todo = [e for e in entries if needs_port(e, state, args.force)]
    print(f"{len(entries)} files selected, {len(entries) - len(todo)} already done, {len(todo)} to port")
    if args.limit:
        todo = todo[: args.limit]
        print(f"limited to {len(todo)}")
    if not todo:
        write_report(out_dir, inventory, state)
        return
    if args.dry_run:
        dry_run(todo, root, args, system, tiers, out_dir)
        return

    backend = pick_backend(args)
    print(f"backend {backend.name} ({args.provider}), model {args.model}, effort {args.effort}, {args.workers} workers")
    lock = threading.Lock()
    finished = 0

    commit = git_head(root)

    kotlin_before = {}

    def work(entry):
        target = out_dir / entry["kotlin_path"]
        before = target.read_text(encoding="utf-8", errors="replace") if target.exists() else None
        try:
            result = port_one(
                backend, entry, root, out_dir, by_path, system, tiers,
                previous=state.get(entry["path"]), incremental=args.incremental, commit=commit,
            )
            return entry, result, None, before
        except Exception as exc:
            return entry, None, str(exc), before

    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        futures = [pool.submit(work, entry) for entry in todo]
        try:
            for future in as_completed(futures):
                entry, result, error, before = future.result()
                with lock:
                    finished += 1
                    if error:
                        state[entry["path"]] = {
                            "status": "error",
                            "error": error,
                            "sha256": entry["sha256"],
                            "tier": entry["tier"],
                            "kotlin_path": entry["kotlin_path"],
                            "ported_at": now_iso(),
                        }
                        print(f"[{finished}/{len(todo)}] FAIL {entry['path']}: {error}")
                    else:
                        state[entry["path"]] = result
                        if result["mode"] == "incremental" and before is not None:
                            kotlin_before[out_dir / entry["kotlin_path"]] = before
                        extra = f", WARNING {result['warning']}" if result.get("warning") else ""
                        print(
                            f"[{finished}/{len(todo)}] ok   {entry['path']} -> {entry['kotlin_path']} "
                            f"({result['mode']}, {result['seconds']}s, {len(result['unsupported'])} TODO{extra})"
                        )
                    save_json(state_path, state)
        except KeyboardInterrupt:
            print("\ninterrupted, saving state for the files that finished")
            pool.shutdown(wait=False, cancel_futures=True)
            save_json(state_path, state)
            write_report(out_dir, inventory, state)
            raise SystemExit(130)

    written = [out_dir / e["kotlin_path"] for e in todo if state.get(e["path"], {}).get("status") == "ok"]
    if written and not args.no_postprocess:
        print(f"\npostprocessing the {len(written)} files written")
        postprocess.run(False, only=written, before=kotlin_before)
    write_report(out_dir, inventory, state)
    ok = sum(1 for e in todo if state.get(e["path"], {}).get("status") == "ok")
    print(f"\ndone: {ok} ok, {len(todo) - ok} failed. Report: {out_dir / 'PORT-REPORT.md'}")


if __name__ == "__main__":
    main()
