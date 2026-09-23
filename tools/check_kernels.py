#!/usr/bin/env python3
import argparse
import re
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import check_effects_api as api
import postprocess

KERNELS_DIR = api.PLATFORM_DIR / "coreimage/kernels"
KERNEL_LIBRARY = api.PLATFORM_DIR / "coreimage/CIKernel.kt"
METAL_KERNEL_RE = re.compile(
    r'\bextern\s+"C"\s+(?:\[\[\s*stitchable\s*\]\]\s+)?(?:[\w:<>]+\s+)+?(\w+)\s*\('
    r"|\[\[\s*stitchable\s*\]\]\s+(?:[\w:<>]+\s+)+?(\w+)\s*\("
)
MULTILINE_STRING_RE = re.compile(r'(#*)"""(.*?)"""\1', re.S)
PORT_OBJECT_RE = re.compile(r"\bobject\s+(\w+)\s*:[^{]*\bCIKernelPort\b[^{]*\{")
PORT_NAME_RE = re.compile(r"\bval\s+name\b[^=\n]*(?:=|get\(\)\s*=)\s*\"([^\"]*)\"")
PORT_GLSL_RE = re.compile(r"\bval\s+glsl\b[^=\n]*(?:=|get\(\)\s*=)\s*(\S)")


def mask_metal(text):
    text = re.sub(r"/\*.*?\*/", lambda m: " " * len(m.group(0)), text, flags=re.S)
    return re.sub(r"//[^\n]*", lambda m: " " * len(m.group(0)), text)


def metal_kernels(root):
    kernels = {}
    for path in sorted((root / "Moblin").rglob("*.metal")):
        rel = path.relative_to(root).as_posix()
        for match in METAL_KERNEL_RE.finditer(mask_metal(api.read_text(path))):
            kernels.setdefault(match.group(1) or match.group(2), rel)
    return kernels


def embedded_shaders(root):
    shaders = {}
    for path in sorted((root / "Moblin").rglob("*.swift")):
        rel = path.relative_to(root).as_posix()
        text = api.read_text(path)
        if "metal_stdlib" not in text:
            continue
        for match in MULTILINE_STRING_RE.finditer(text):
            if "#include <metal_stdlib>" in match.group(2):
                shaders.setdefault(rel, []).append(api.sha256(match.group(2)))
    return shaders


def kotlin_ports():
    ports = []
    for path in api.kotlin_files(KERNELS_DIR):
        text = api.read_text(path)
        masked = postprocess.mask_kotlin(text)
        for match in PORT_OBJECT_RE.finditer(masked):
            end = api.matching_brace(masked, match.end() - 1)
            body = text[match.end():end]
            name = PORT_NAME_RE.search(body)
            glsl = PORT_GLSL_RE.search(postprocess.mask_kotlin(body))
            ported = glsl is not None and not body[glsl.start(1):].startswith("null")
            ports.append({
                "object": match.group(1),
                "name": name.group(1) if name else None,
                "ported": ported,
                "file": path.relative_to(api.ROOT).as_posix(),
            })
    return ports


def registered_objects():
    if not KERNEL_LIBRARY.exists():
        return None
    masked = postprocess.mask_kotlin(api.read_text(KERNEL_LIBRARY))
    return set(re.findall(r"\b(\w+Port)\b", masked))


def kernel_findings(root):
    findings = []
    ports = kotlin_ports()
    registered = registered_objects()
    by_name = {}
    for port_object in ports:
        if port_object["name"] is None:
            findings.append({"check": "kernels", "name": port_object["object"], "file": port_object["file"],
                             "message": "CIKernelPort object without a literal name"})
            continue
        by_name.setdefault(port_object["name"], []).append(port_object)
    for kernel, rel in sorted(metal_kernels(root).items()):
        candidates = by_name.get(kernel, [])
        if not candidates:
            findings.append({"check": "kernels", "name": kernel, "file": rel,
                             "message": "no CIKernelPort object in platform/coreimage/kernels"})
            continue
        if not any(candidate["ported"] for candidate in candidates):
            findings.append({"check": "kernels", "name": kernel, "file": rel,
                             "message": "the CIKernelPort has no GLSL yet (glsl = null)"})
            continue
        if registered is None:
            findings.append({"check": "kernels", "name": kernel, "file": rel,
                             "message": "platform/coreimage/CIKernel.kt is missing, so the port is never registered"})
        elif not any(candidate["object"] in registered for candidate in candidates if candidate["ported"]):
            findings.append({"check": "kernels", "name": kernel, "file": rel,
                             "message": "the port is not in CIKernelLibrary's port list in CIKernel.kt"})
    return findings


def shader_findings(root, baseline):
    findings = []
    stored = baseline.get("shaders", {})
    current = embedded_shaders(root)
    for rel in sorted(set(stored) | set(current)):
        before = [entry["sha256"] for entry in stored.get(rel, {}).get("hashes", [])]
        after = current.get(rel, [])
        covered_by = stored.get(rel, {}).get("covered_by")
        if rel not in stored:
            findings.append({"check": "shaders", "name": rel, "file": rel,
                             "message": f"{len(after)} embedded Metal shader(s) that no platform replacement covers"})
        elif before != after:
            findings.append({"check": "shaders", "name": rel, "file": rel,
                             "message": f"embedded Metal shader changed upstream; update {covered_by}, then run "
                                        "with --update"})
    return findings


def update_baseline(root, baseline):
    stored = baseline.get("shaders", {})
    shaders = {}
    for rel, hashes in embedded_shaders(root).items():
        shaders[rel] = {
            "covered_by": stored.get(rel, {}).get("covered_by", "a platform replacement (fill in)"),
            "hashes": [{"sha256": value} for value in hashes],
        }
    baseline["shaders"] = shaders
    api.save_json(api.BASELINE, baseline)


def main():
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    parser = argparse.ArgumentParser(
        description="Check that every Metal kernel upstream has a registered GLSL port under "
        "platform/coreimage/kernels, and that no embedded Metal shader changed. Exits with 1 on a finding that "
        "tools/effects_known_gaps.json does not list."
    )
    parser.add_argument("--moblin", type=Path, default=None, help="upstream checkout (default: the inventory root)")
    parser.add_argument("--update", action="store_true",
                        help="store the current embedded shader hashes in tools/platform_replaced_api.json")
    parser.add_argument("--verbose", action="store_true", help="also list kernels, ports and known gaps")
    args = parser.parse_args()
    root = api.upstream_root(args.moblin)
    baseline = api.load_json(api.BASELINE, {})
    if args.update:
        update_baseline(root, baseline)
        print(f"wrote {api.BASELINE}")
        return
    if args.verbose:
        for kernel, rel in sorted(metal_kernels(root).items()):
            print(f"kernel {kernel} ({rel})")
        for port_object in kotlin_ports():
            state = "glsl" if port_object["ported"] else "glsl = null"
            print(f"port {port_object['object']} name={port_object['name']} {state} ({port_object['file']})")
        for rel, hashes in embedded_shaders(root).items():
            print(f"embedded shaders in {rel}: {len(hashes)}")
    findings = kernel_findings(root) + shader_findings(root, baseline)
    sys.exit(api.report(findings, ("kernels", "shaders"), "check_kernels", args.verbose))


if __name__ == "__main__":
    main()
