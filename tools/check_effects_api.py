#!/usr/bin/env python3
import argparse
import fnmatch
import hashlib
import json
import re
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
sys.path.insert(0, str(HERE))
import inventory as inventory_tool
import port
import postprocess

KOTLIN_ROOT = postprocess.KOTLIN_ROOT
PLATFORM_DIR = KOTLIN_ROOT / "platform"
INVENTORY = HERE / "inventory.json"
BASELINE = HERE / "platform_replaced_api.json"
GAPS = HERE / "effects_known_gaps.json"
PACKAGE_RESOLVED = "Moblin.xcodeproj/project.xcworkspace/xcshareddata/swiftpm/Package.resolved"
PINNED_PACKAGES = ["metalpetal", "vrmkit", "ayagamiswift", "swiftcube"]

TYPE_PATTERNS = [
    r"(?:CI|MTI|MTL|MTK|VN|SCN|VRM|WK|MK)[A-Z]\w*",
    r"AVAssetReader\w*",
    r"AVSpeech\w*",
    r"SDAnimatedImage\w*",
    r"SC3DLut",
    r"LutEntry",
    r"SwiftCubeError",
    r"ImageRenderer",
    r"UIGraphicsImageRenderer\w*",
    r"Chart",
    r"SectorMark",
]
TYPE_RE = re.compile(r"\b(?:" + "|".join(TYPE_PATTERNS) + r")\b")
MOTION_RE = re.compile(r"\bCM[A-Z]\w*\b")
CORE_MEDIA_PREFIXES = (
    "CMTime", "CMSample", "CMFormat", "CMVideo", "CMAudio", "CMBlock", "CMClock", "CMBuffer", "CMMetadata",
    "CMItem", "CMPersistent", "CMTag", "CMIO", "CMSimpleQueue", "CMMemoryPool", "CMSync", "CMText", "CMMuxed",
    "CMAttachment", "CMClosedCaption", "CMPixelFormat", "CMMediaType", "CMSubtitle", "CMTimebase",
)
MEMBER_RECEIVERS = {
    "CIImage", "MTIImage", "MTILayer", "CIContext", "WKWebView", "WKWebViewConfiguration", "SCNNode", "SCNRenderer",
    "SCNCamera", "VRMNode", "MKMapSnapshotter", "AVAssetReader", "AVAssetReaderTrackOutput", "AVSpeechSynthesizer",
    "CMMotionManager", "SC3DLut", "ImageRenderer",
}
MEMBER_FILTER_RE = re.compile(r"^MTI\w*Filter$")
KOTLIN_ANY_MEMBERS = {"equals", "hashCode", "toString", "copy"}
CHECK_NAMES = ("types", "members", "skipped", "pins", "detection", "kernels", "shaders", "boundary")
DETECTION = {
    "file": "Moblin/Media/HaishinKit/Media/Video/VideoUnit.swift",
    "function": "detectObjects(detectionJob:completion:)",
    "header": r"func\s+detectObjects\s*\(\s*detectionJob\s*:",
    "replaced_by": "com.moblin.android.platform.vision.VisionDetector.detect",
    "owner": "E9",
}


def load_json(path, default):
    if path.exists():
        return json.loads(path.read_text(encoding="utf-8-sig"))
    return default


def save_json(path, data):
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8", newline="\n")


def load_inventory():
    if not INVENTORY.exists():
        sys.exit(f"{INVENTORY} is missing. Run python tools/inventory.py first.")
    return load_json(INVENTORY, None)


def upstream_root(argument, inventory=None):
    if argument is not None:
        root = Path(argument).resolve()
    else:
        inventory = inventory or load_inventory()
        root = Path(inventory["root"]).resolve()
    if not (root / "Moblin").is_dir():
        sys.exit(f"no Moblin directory under {root}; pass --moblin <upstream checkout>")
    return root


def read_text(path):
    return path.read_text(encoding="utf-8", errors="replace").replace("\r\n", "\n")


def blank(segment, fill):
    return "".join(ch if ch == "\n" else fill for ch in segment)


def skip_swift_block_comment(text, i):
    depth = 0
    j = i
    while j < len(text):
        if text.startswith("/*", j):
            depth += 1
            j += 2
        elif text.startswith("*/", j):
            depth -= 1
            j += 2
            if depth == 0:
                return j
        else:
            j += 1
    return len(text)


def skip_interpolation(text, j):
    depth = 0
    while j < len(text):
        c = text[j]
        if c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
            if depth == 0:
                return j + 1
        elif c == '"':
            j = swift_string_end(text, j)
            continue
        elif c == "\n":
            return j
        j += 1
    return len(text)


def swift_string_end(text, i):
    hashes = 0
    while i + hashes < len(text) and text[i + hashes] == "#":
        hashes += 1
    j = i + hashes
    if text.startswith('"""', j):
        closing = '"""' + "#" * hashes
        k = j + 3
        while k < len(text):
            if text[k] == "\\" and hashes == 0:
                k += 2
                continue
            if text.startswith(closing, k):
                return k + len(closing)
            k += 1
        return len(text)
    closing = '"' + "#" * hashes
    k = j + 1
    while k < len(text):
        c = text[k]
        if c == "\\" and hashes == 0:
            if k + 1 < len(text) and text[k + 1] == "(":
                k = skip_interpolation(text, k + 1)
                continue
            k += 2
            continue
        if text.startswith(closing, k):
            return k + len(closing)
        if c == "\n":
            return k
        k += 1
    return len(text)


def mask_swift(text):
    pieces = []
    start = 0
    i = 0
    n = len(text)
    while i < n:
        c = text[i]
        if c == "/" and text.startswith("//", i):
            end = text.find("\n", i)
            end = n if end == -1 else end
            pieces.append(text[start:i])
            pieces.append(" " * (end - i))
            i = start = end
        elif c == "/" and text.startswith("/*", i):
            end = skip_swift_block_comment(text, i)
            pieces.append(text[start:i])
            pieces.append(blank(text[i:end], " "))
            i = start = end
        elif c == '"' or (c == "#" and re.match(r'#+"', text[i:i + 12])):
            end = swift_string_end(text, i)
            segment = text[i:end]
            hashes = len(segment) - len(segment.lstrip("#"))
            triple = segment.startswith('"""', hashes)
            opening = hashes + (3 if triple else 1)
            closing = ('"""' if triple else '"') + "#" * hashes
            closing_length = len(closing) if len(segment) >= opening + len(closing) and segment.endswith(closing) else 0
            body_end = len(segment) - closing_length
            pieces.append(text[start:i])
            pieces.append(segment[:opening] + blank(segment[opening:body_end], "x") + segment[body_end:])
            i = start = end
        else:
            i += 1
    pieces.append(text[start:])
    return "".join(pieces)


def matching_brace(masked, index):
    depth = 0
    for j in range(index, len(masked)):
        c = masked[j]
        if c == "{":
            depth += 1
        elif c == "}":
            depth -= 1
            if depth == 0:
                return j
    return len(masked) - 1


def swift_sources(root, inventory, include_skip=False):
    for entry in inventory["files"]:
        if entry["tier"] == "skip" and not include_skip:
            continue
        path = root / entry["path"]
        if path.exists():
            yield entry["path"], path


SWIFT_DECL_RE = re.compile(
    r"(?P<attributes>(?:@\w+(?:\([^()\n]*\))?\s+)*)"
    r"(?P<modifiers>(?:(?:public|internal|open|final|private|fileprivate|private\(set\)|fileprivate\(set\)|"
    r"internal\(set\)|public\(set\)|static|class(?=\s+(?:func|var|let|override|final|static))|override|required|"
    r"convenience|mutating|nonmutating|lazy|weak|unowned|nonisolated|dynamic|indirect|@MainActor)\s+)*)"
    r"(?P<keyword>class|struct|enum|protocol|extension|actor|func|init[?!]?|deinit|var|let|typealias|case|subscript)"
    r"(?=[\s(<{:?!])"
)
SWIFT_TYPE_KEYWORDS = ("class", "struct", "enum", "protocol", "extension", "actor")


def swift_header_end(masked, start, keyword):
    parens = 0
    i = start
    while i < len(masked):
        c = masked[i]
        if c in "([":
            parens += 1
        elif c in ")]":
            parens -= 1
        elif parens <= 0:
            if c == "{":
                return i, i
            if c == "=" and keyword in ("var", "let", "case", "typealias") and masked[i + 1:i + 2] != "=":
                return i, None
            if c == ";":
                return i, None
            if c == "\n":
                rest = masked[i + 1:].lstrip()
                if rest.startswith(("{", "->", "throws", "rethrows", "async", "where", ".", ":", ",")):
                    i += 1
                    continue
                return i, None
        i += 1
    return len(masked), None


def swift_declarations(text):
    masked = mask_swift(text)
    result = []
    stack = []
    pending = None
    parens = 0
    statement_start = True
    i = 0
    while i < len(masked):
        c = masked[i]
        if statement_start:
            if c in " \t\r\n;":
                i += 1
                continue
            statement_start = False
            if all(block["type"] for block in stack):
                match = SWIFT_DECL_RE.match(masked, i)
                if match:
                    keyword = match.group("keyword")
                    modifiers = match.group("modifiers").split()
                    hidden = any(block["hidden"] for block in stack) or "private" in modifiers or "fileprivate" in modifiers
                    end, brace = swift_header_end(masked, match.end(), keyword)
                    if not hidden:
                        signature = re.sub(r"\s+", " ", text[match.start("modifiers"):end]).strip()
                        result.append("    " * len(stack) + signature)
                    if brace is not None:
                        pending = (brace, keyword in SWIFT_TYPE_KEYWORDS, hidden)
                    i = end
                    continue
        if c in "([":
            parens += 1
        elif c in ")]":
            parens = max(0, parens - 1)
        elif c == "{":
            if pending is not None and pending[0] == i:
                stack.append({"type": pending[1], "hidden": pending[2], "parens": parens})
            else:
                stack.append({"type": False, "hidden": True, "parens": parens})
            pending = None
            parens = 0
            statement_start = True
        elif c == "}":
            if stack:
                parens = stack.pop()["parens"]
            statement_start = True
        elif c in "\n;" and parens == 0:
            statement_start = True
        i += 1
    return result


def swift_function_text(text, header_pattern):
    masked = mask_swift(text)
    match = re.search(header_pattern, masked)
    if not match:
        return None
    brace = masked.find("{", match.end())
    if brace == -1:
        return None
    end = matching_brace(masked, brace)
    line_start = text.rfind("\n", 0, match.start()) + 1
    body = text[line_start:end + 1]
    return "\n".join(line.rstrip() for line in body.split("\n"))


def sha256(text):
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def load_gaps():
    data = load_json(GAPS, {"gaps": []})
    gaps = data.get("gaps", []) if isinstance(data, dict) else []
    problems = []
    valid = []
    for index, gap in enumerate(gaps):
        label = f"{GAPS.name} #{index + 1}"
        if not isinstance(gap, dict):
            problems.append(f"{label}: an entry must be an object")
            continue
        missing = [key for key in ("check", "name", "owner", "milestone") if not str(gap.get(key, "")).strip()]
        if missing:
            problems.append(f"{label} ({gap.get('name', '?')}): needs {', '.join(missing)}")
            continue
        if gap["check"] not in CHECK_NAMES:
            problems.append(f"{label} ({gap['name']}): check must be one of {', '.join(CHECK_NAMES)}")
            continue
        if "files" in gap and not isinstance(gap["files"], list):
            problems.append(f"{label} ({gap['name']}): files must be a list of path prefixes")
            continue
        valid.append(gap)
    return valid, problems


def gap_for(finding, gaps):
    for gap in gaps:
        if gap["check"] != finding["check"] or not fnmatch.fnmatchcase(finding["name"], gap["name"]):
            continue
        files = gap.get("files")
        if files and not (finding.get("file") and finding["file"].startswith(tuple(files))):
            continue
        return gap
    return None


def report(findings, checks, title, verbose=False):
    gaps, problems = load_gaps()
    gaps_in_scope = [gap for gap in gaps if gap["check"] in checks]
    open_findings = []
    covered = []
    used = set()
    for finding in findings:
        gap = gap_for(finding, gaps_in_scope)
        if gap is None:
            open_findings.append(finding)
        else:
            covered.append((finding, gap))
            used.add(id(gap))
    for check in checks:
        group = [finding for finding in open_findings if finding["check"] == check]
        if not group:
            continue
        print(f"{check}: {len(group)} findings")
        by_name = {}
        for finding in group:
            by_name.setdefault((finding["name"], finding["message"]), []).append(finding.get("file"))
        for (name, message), files in sorted(by_name.items()):
            where = sorted({file for file in files if file})
            suffix = ""
            if where:
                suffix = " (" + ", ".join(where[:4]) + (f" and {len(where) - 4} more" if len(where) > 4 else "") + ")"
            print(f"  {name}: {message}{suffix}")
    if covered:
        print(f"known gaps: {len(covered)} findings are listed in {GAPS.name}")
        if verbose:
            for finding, gap in covered:
                where = f" ({finding['file']})" if finding.get("file") else ""
                print(f"  {finding['check']} {finding['name']}{where}: {gap['owner']}, {gap['milestone']}")
    unused = [gap for gap in gaps_in_scope if id(gap) not in used]
    for gap in unused:
        print(f"  note: known gap {gap['check']} {gap['name']} ({gap['owner']}, {gap['milestone']}) matches no current finding")
    for problem in problems:
        print(f"  INVALID {problem}")
    print(f"{title}: {len(open_findings)} open findings, {len(covered)} known gaps")
    return 1 if open_findings or problems else 0


def kotlin_files(directory):
    if not directory.exists():
        return []
    return sorted(directory.rglob("*.kt"))


KOTLIN_DECLARED_RE = re.compile(
    r"\b(?:class|interface|object|typealias)\s+(\w+)|\bfun\s+(?:<[^>\n]*>\s*)?(\w+)\s*\(|\b(?:val|var)\s+(\w+)\s*[:=]"
)


def platform_declared_names():
    names = set()
    for path in kotlin_files(PLATFORM_DIR):
        masked = postprocess.mask_kotlin(read_text(path))
        for match in KOTLIN_DECLARED_RE.finditer(masked):
            names.update(group for group in match.groups() if group)
    return names


def swift_declared_types(inventory):
    names = set()
    for entry in inventory["files"]:
        names.update(entry.get("declared_types", []))
    return names


IMPORT_LINE_RE = re.compile(r"^\s*(?:@\w+\s+)*import\s")


def type_findings(root, inventory):
    declared = platform_declared_names()
    own = swift_declared_types(inventory)
    findings = []
    for rel, path in swift_sources(root, inventory):
        text = read_text(path)
        masked = mask_swift(text)
        motion = bool(re.search(r"^\s*import\s+CoreMotion\b", masked, re.M))
        seen = set()
        for line in masked.split("\n"):
            if IMPORT_LINE_RE.match(line):
                continue
            seen.update(TYPE_RE.findall(line))
            if motion:
                seen.update(name for name in MOTION_RE.findall(line) if not name.startswith(CORE_MEDIA_PREFIXES))
        for name in sorted(seen - own):
            if name not in declared:
                findings.append({"check": "types", "name": name, "file": rel,
                                 "message": "no declaration under platform/**"})
    return findings


SUPER_SPLIT_RE = re.compile(r",(?![^<>()]*[>)])")
CLASS_SIGNATURE_RE = re.compile(r"\b(class|interface|object)\s+(\w+)")
MEMBER_SIGNATURE_RE = re.compile(r"\b(?:fun|val|var)\s+(?:<[^>]*>\s*)?(?:([\w.<>?*, ]+?)\.)?(`?\w+`?)\s*(?:[(:=<]|$)")
EXTENSION_LINE_RE = re.compile(
    r"^(?:(?:public|internal|private|inline|operator|infix|suspend|@\w+(?:\([^)]*\))?)\s+)*(?:fun|val|var)\s+"
    r"(?:<[^>\n]*>\s*)?([A-Z]\w*(?:\.[A-Z]\w*)*)(?:<[^>\n]*>)?\??\.(\w+)"
)
FACTORY_RE = re.compile(r"\bfun\s+(\w+)\s*\(\s*\)\s*:\s*(\w+)")


def split_header(signature):
    depth = 0
    for index, c in enumerate(signature):
        if c in "(<":
            depth += 1
        elif c == ")" or (c == ">" and signature[index - 1:index] != "-"):
            depth -= 1
        elif c == ":" and depth == 0:
            return signature[:index], signature[index + 1:]
    return signature, ""


def kotlin_model():
    classes = {}
    extensions = {}
    for path in kotlin_files(PLATFORM_DIR):
        text = read_text(path)
        stack = []
        for depth, signature in port.api_declarations(text):
            while stack and stack[-1][0] >= depth:
                stack.pop()
            type_match = CLASS_SIGNATURE_RE.search(signature)
            if type_match and not re.match(r"^(?:[\w@]+\s+)*(?:fun|val|var)\s", signature):
                name = type_match.group(2)
                if "companion object" in signature and signature.rstrip().endswith("companion object"):
                    stack.append((depth, None))
                    continue
                info = classes.setdefault(name, {"members": set(), "supers": set()})
                head, supers = split_header(signature[type_match.end():])
                info["members"].update(re.findall(r"\b(?:val|var)\s+(\w+)", head))
                for part in SUPER_SPLIT_RE.split(supers):
                    simple = re.sub(r"<.*$|\(.*$", "", part.strip()).split(".")[-1].strip()
                    if simple:
                        info["supers"].add(simple)
                stack.append((depth, name))
                continue
            if signature.startswith("companion object"):
                stack.append((depth, None))
                continue
            member = MEMBER_SIGNATURE_RE.search(signature)
            if not member:
                continue
            receiver, name = member.group(1), member.group(2).strip("`")
            if receiver:
                simple = re.sub(r"<.*$", "", receiver).rstrip("?").split(".")[-1]
                extensions.setdefault(simple, set()).add(name)
            elif stack and stack[-1][1] is not None and stack[-1][0] == depth - 1:
                classes[stack[-1][1]]["members"].add(name)
    for path in kotlin_files(ROOT / "app/src/main/java"):
        if path.is_relative_to(PLATFORM_DIR):
            continue
        for line in read_text(path).split("\n"):
            match = EXTENSION_LINE_RE.match(line)
            if match:
                extensions.setdefault(match.group(1).split(".")[-1], set()).add(match.group(2))
    factories = {}
    coreimage = PLATFORM_DIR / "coreimage"
    for path in kotlin_files(coreimage):
        for match in FACTORY_RE.finditer(postprocess.mask_kotlin(read_text(path))):
            factories.setdefault(match.group(1), match.group(2))
    return classes, extensions, factories


def kotlin_has_member(type_name, member, classes, extensions, seen=None):
    seen = seen or set()
    if type_name in seen:
        return False
    seen.add(type_name)
    if member in KOTLIN_ANY_MEMBERS:
        return True
    if member in extensions.get(type_name, ()):
        return True
    info = classes.get(type_name)
    if info is None:
        return False
    if member in info["members"]:
        return True
    return any(kotlin_has_member(parent, member, classes, extensions, seen) for parent in info["supers"])


SWIFT_EXTENSION_RE = re.compile(r"\bextension\s+(\w+)\b[^{\n]*\{")
SWIFT_EXTENSION_MEMBER_RE = re.compile(r"\b(?:func|var|let)\s+(\w+)")
BINDING_TYPE_RE = re.compile(r"(?<![\w.])([a-z_]\w*)\s*:\s*(?:inout\s+|@escaping\s+)?([A-Z]\w*)\b")
BINDING_INIT_RE = re.compile(r"\b(?:let|var)\s+([a-z_]\w*)\s*(?::\s*[\w.<>?!\[\] ]+)?=\s*(?:try[?!]?\s+)?(?:await\s+)?([A-Z]\w*)\s*\(")
BINDING_FACTORY_RE = re.compile(r"\b(?:let|var)\s+([a-z_]\w*)\s*(?::\s*[\w.<>?!&\[\] ]+)?=\s*CIFilter\.(\w+)\s*\(")
INIT_CHAIN_RE = re.compile(r"\b([A-Z]\w*)\s*\(([^()]|\([^()]*\))*\)\s*[?!]?\s*\.\s*([a-z_]\w*)")


def swift_extension_members(root, inventory):
    members = {}
    for _, path in swift_sources(root, inventory, include_skip=True):
        masked = mask_swift(read_text(path))
        for match in SWIFT_EXTENSION_RE.finditer(masked):
            end = matching_brace(masked, match.end() - 1)
            members.setdefault(match.group(1), set()).update(
                SWIFT_EXTENSION_MEMBER_RE.findall(masked[match.end():end])
            )
    return members


def is_receiver(type_name):
    return type_name in MEMBER_RECEIVERS or bool(MEMBER_FILTER_RE.match(type_name)) or type_name.startswith("CIFilter.")


def member_findings(root, inventory):
    classes, extensions, factories = kotlin_model()
    swift_members = swift_extension_members(root, inventory)
    findings = []
    for rel, path in swift_sources(root, inventory):
        masked = mask_swift(read_text(path))
        bindings = {}
        for match in BINDING_TYPE_RE.finditer(masked):
            bindings.setdefault(match.group(1), set()).add(match.group(2))
        for match in BINDING_INIT_RE.finditer(masked):
            if match.group(2) != "CIFilter":
                bindings.setdefault(match.group(1), set()).add(match.group(2))
        for match in BINDING_FACTORY_RE.finditer(masked):
            bindings.setdefault(match.group(1), set()).add("CIFilter." + match.group(2))
        uses = set()
        for name, types in bindings.items():
            if len(types) != 1:
                continue
            type_name = next(iter(types))
            if not is_receiver(type_name):
                continue
            pattern = re.compile(r"(?<![\w.])(?:self\.)?" + re.escape(name) + r"\s*[?!]?\s*\.\s*([a-z_]\w*)")
            for use in pattern.finditer(masked):
                uses.add((type_name, use.group(1)))
        for match in INIT_CHAIN_RE.finditer(masked):
            if is_receiver(match.group(1)):
                uses.add((match.group(1), match.group(3)))
        for type_name, member in sorted(uses):
            kotlin_type = type_name
            if type_name.startswith("CIFilter."):
                factory = type_name.split(".", 1)[1]
                kotlin_type = factories.get(factory)
                if kotlin_type is None and "CIFilter" not in classes:
                    continue
                if kotlin_type is None:
                    findings.append({"check": "members", "name": type_name + "()", "file": rel,
                                     "message": "no CIFilter factory with this name"})
                    continue
            if kotlin_type not in classes:
                continue
            if member in swift_members.get(type_name, ()) or member in swift_members.get(kotlin_type, ()):
                continue
            if not kotlin_has_member(kotlin_type, member, classes, extensions):
                findings.append({"check": "members", "name": f"{kotlin_type}.{member}", "file": rel,
                                 "message": "no member or extension with this name"})
    unique = {}
    for finding in findings:
        unique.setdefault((finding["name"], finding["file"]), finding)
    return list(unique.values())


def skipped_declarations(root):
    result = {}
    for rel in sorted(inventory_tool.PLATFORM_REPLACED):
        path = root / rel
        result[rel] = swift_declarations(read_text(path)) if path.exists() else None
    return result


def skipped_findings(root, baseline):
    findings = []
    stored = baseline.get("declarations", {})
    for rel, current in skipped_declarations(root).items():
        if current is None:
            if rel in stored:
                findings.append({"check": "skipped", "name": rel, "file": rel,
                                 "message": "the platform-replaced file was removed upstream"})
            continue
        if rel not in stored:
            findings.append({"check": "skipped", "name": rel, "file": rel,
                             "message": "not in platform_replaced_api.json; review it and run with --update"})
            continue
        before = stored[rel]
        for line in [line for line in before if line not in current]:
            findings.append({"check": "skipped", "name": rel, "file": rel, "message": f"removed: {line.strip()}"})
        for line in [line for line in current if line not in before]:
            findings.append({"check": "skipped", "name": rel, "file": rel, "message": f"added: {line.strip()}"})
    return findings


def package_revisions(root):
    path = root / PACKAGE_RESOLVED
    if not path.exists():
        return None
    data = load_json(path, {})
    pins = data.get("pins") or data.get("object", {}).get("pins", [])
    revisions = {}
    for pin in pins:
        identity = (pin.get("identity") or pin.get("package") or "").lower()
        revisions[identity] = pin.get("state", {}).get("revision")
    return revisions


def pin_findings(root, baseline):
    revisions = package_revisions(root)
    if revisions is None:
        return [{"check": "pins", "name": PACKAGE_RESOLVED, "message": "Package.resolved is missing"}]
    findings = []
    for name, expected in sorted(baseline.get("packages", {}).items()):
        current = revisions.get(name)
        if current != expected:
            findings.append({"check": "pins", "name": name,
                             "message": f"pinned {str(expected)[:12]} in the effects plan, upstream has {str(current)[:12]}"})
    return findings


def detection_hash(root):
    path = root / DETECTION["file"]
    if not path.exists():
        return None
    body = swift_function_text(read_text(path), DETECTION["header"])
    return sha256(body) if body is not None else None


def detection_findings(root, baseline):
    stored = baseline.get("functions", [])
    findings = []
    current = detection_hash(root)
    entry = next((item for item in stored if item.get("function") == DETECTION["function"]), None)
    if current is None:
        findings.append({"check": "detection", "name": DETECTION["function"], "file": DETECTION["file"],
                         "message": "the function was not found"})
    elif entry is None:
        findings.append({"check": "detection", "name": DETECTION["function"], "file": DETECTION["file"],
                         "message": "no stored hash; review it and run with --update"})
    elif entry.get("sha256") != current:
        findings.append({"check": "detection", "name": DETECTION["function"], "file": DETECTION["file"],
                         "message": f"changed upstream; bring {entry.get('replaced_by')} in step ({entry.get('owner')}), "
                                    "then run with --update"})
    return findings


def update_baseline(root, baseline):
    baseline["declarations"] = {rel: lines for rel, lines in skipped_declarations(root).items() if lines is not None}
    revisions = package_revisions(root) or {}
    baseline["packages"] = {name: revisions.get(name) for name in PINNED_PACKAGES}
    current = detection_hash(root)
    baseline["functions"] = [{
        "file": DETECTION["file"],
        "function": DETECTION["function"],
        "sha256": current,
        "replaced_by": DETECTION["replaced_by"],
        "owner": DETECTION["owner"],
    }]
    save_json(BASELINE, baseline)


def main():
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    parser = argparse.ArgumentParser(
        description="Check that every Apple type and member the effects use has a shim under platform/**, and "
        "that the upstream code the platform layer replaces has not changed. Exits with 1 on a finding that "
        "tools/effects_known_gaps.json does not list."
    )
    parser.add_argument("--moblin", type=Path, default=None, help="upstream checkout (default: the inventory root)")
    parser.add_argument("--check", nargs="+", choices=["types", "members", "skipped", "pins", "detection"],
                        default=["types", "members", "skipped", "pins", "detection"])
    parser.add_argument("--update", action="store_true",
                        help="store the current skipped-file declarations, package pins and detection hash in "
                        "tools/platform_replaced_api.json after reviewing a change")
    parser.add_argument("--verbose", action="store_true", help="also list the findings that are known gaps")
    args = parser.parse_args()
    inventory = load_inventory()
    root = upstream_root(args.moblin, inventory)
    baseline = load_json(BASELINE, {})
    if args.update:
        update_baseline(root, baseline)
        print(f"wrote {BASELINE}")
        return
    findings = []
    if "types" in args.check:
        findings += type_findings(root, inventory)
    if "members" in args.check:
        findings += member_findings(root, inventory)
    if "skipped" in args.check:
        findings += skipped_findings(root, baseline)
    if "pins" in args.check:
        findings += pin_findings(root, baseline)
    if "detection" in args.check:
        findings += detection_findings(root, baseline)
    sys.exit(report(findings, args.check, "check_effects_api", args.verbose))


if __name__ == "__main__":
    main()
