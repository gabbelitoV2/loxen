#!/usr/bin/env python3
import argparse
import hashlib
import json
import re
import sys
from datetime import datetime, timezone
from pathlib import Path

HERE = Path(__file__).resolve().parent

MEDIA = {
    "AVFoundation", "AVKit", "VideoToolbox", "CoreMedia", "CoreVideo", "Metal", "MetalKit",
    "MetalPerformanceShaders", "CoreImage", "Vision", "AudioToolbox", "CoreAudio", "Accelerate",
    "CoreML", "ARKit", "SceneKit", "SpriteKit",
}
UI = {
    "SwiftUI", "UIKit", "AppKit", "WidgetKit", "AppIntents", "Intents", "IntentsUI",
    "StoreKit", "SafariServices", "WebKit", "MessageUI", "PhotosUI", "Photos", "QuickLook", "TipKit",
}
PLATFORM = {
    "Network", "CoreBluetooth", "CoreLocation", "CryptoKit", "GameController", "Speech",
    "NaturalLanguage", "CoreMotion", "LocalAuthentication", "AuthenticationServices", "Security",
    "SystemConfiguration", "CoreTelephony", "libsrt", "CoreHaptics", "MediaPlayer", "NetworkExtension",
    "ActivityKit",
}
APPLE_ONLY = {
    "WatchConnectivity", "HealthKit", "HomeKit", "CarPlay", "ReplayKit", "ExternalAccessory",
    "MultipeerConnectivity", "CoreNFC", "CoreSpotlight", "MusicKit",
    "BackgroundTasks", "CallKit", "PushKit", "CloudKit", "ShazamKit",
}
NEUTRAL = {"Foundation", "Combine", "os", "OSLog"}
SKIP_DIRS = (
    "Moblin Watch/", "Moblin Widget/", "Moblin Live Activity/", "Moblin Mac/", "Moblin Screen Recording/",
)
SHARED_DIRS = ("Moblin Watch/Shared/", "Moblin Live Activity/Shared/")
EXCLUDE_DIRS = ("Moblin/Integrations/Tesla/Protobuf/",)
PLATFORM_REPLACED = {
    "Moblin/VideoEffects/VTuber/Live2DRenderer.swift",
    "Moblin/VideoEffects/Blur/BlurKernel.swift",
    "Moblin/VideoEffects/Blur/BlurFilter.swift",
}
KOTLIN_KEYWORDS = {
    "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "interface",
    "is", "null", "object", "package", "return", "super", "this", "throw", "true", "try", "typealias",
    "typeof", "val", "var", "when", "while",
}
TIER_ORDER = ["logic", "platform", "test", "media", "ui", "apple_only", "skip"]

DECL_RE = re.compile(
    r"^(?:@\w+(?:\([^)\n]*\))?\s+)*(?:(?:public|private|internal|fileprivate|open|final|indirect)\s+)*"
    r"(?:class|struct|enum|protocol|actor|typealias)\s+([A-Za-z_]\w*)",
    re.M,
)
PRIVATE_DECL_RE = re.compile(
    r"^(?:@\w+(?:\([^)\n]*\))?\s+)*(?:(?:public|internal|open|final|indirect)\s+)*(?:private|fileprivate)\s+"
    r"(?:(?:open|final|indirect)\s+)*(?:class|struct|enum|protocol|actor|typealias)\s+([A-Za-z_]\w*)",
    re.M,
)
FUNC_RE = re.compile(
    r"^(?:@\w+(?:\([^)\n]*\))?\s+)*(?:(?:public|internal|open)\s+)?func\s+([A-Za-z_]\w*)", re.M
)
GLOBAL_RE = re.compile(r"^(?:@MainActor\s+)?(?:(?:public|internal)\s+)?(?:let|var)\s+([A-Za-z_]\w*)", re.M)
IMPORT_RE = re.compile(
    r"^\s*import\s+(?:(?:class|struct|enum|protocol|func|var|let|typealias)\s+)?([A-Za-z_]\w*)", re.M
)
IDENT_RE = re.compile(r"\b[A-Za-z_]\w*\b")


def classify(rel, imports):
    if rel in PLATFORM_REPLACED:
        return "skip"
    if rel.startswith(SKIP_DIRS) and not rel.startswith(SHARED_DIRS):
        return "skip"
    if rel.startswith("MoblinTests/"):
        return "test"
    if imports & APPLE_ONLY and not (imports - APPLE_ONLY - NEUTRAL):
        return "apple_only"
    if imports & MEDIA:
        return "media"
    if imports & UI:
        return "ui"
    if imports & PLATFORM:
        return "platform"
    return "logic"


def kotlin_target(rel):
    parts = list(Path(rel).parts)
    name = Path(rel).stem
    if parts[0] == "MoblinTests":
        base = "app/src/test/java"
        parts = parts[1:]
    else:
        base = "app/src/main/java"
    if parts and parts[0] == "Moblin":
        parts = parts[1:]
    segments = []
    for part in parts[:-1]:
        segment = re.sub(r"[^a-z0-9]", "", part.lower())
        if not segment:
            continue
        if segment[0].isdigit() or segment in KOTLIN_KEYWORDS:
            segment = "_" + segment
        segments.append(segment)
    package = ".".join(["com", "moblin", "android"] + segments)
    path = "/".join([base] + package.split(".") + [name + ".kt"])
    return package, path


def scan(root):
    entries = []
    for path in sorted(root.rglob("*.swift")):
        rel = path.relative_to(root).as_posix()
        if any(part.startswith(".") for part in Path(rel).parts):
            continue
        if rel.startswith(EXCLUDE_DIRS) or "node_modules" in Path(rel).parts:
            continue
        text = path.read_text(encoding="utf-8", errors="replace")
        imports = sorted(set(IMPORT_RE.findall(text)))
        package, kotlin_path = kotlin_target(rel)
        entries.append(
            {
                "path": rel,
                "lines": text.count("\n") + 1,
                "sha256": hashlib.sha256(text.encode("utf-8")).hexdigest(),
                "imports": imports,
                "tier": classify(rel, set(imports)),
                "declared_types": list(dict.fromkeys(DECL_RE.findall(text))),
                "private_types": list(dict.fromkeys(PRIVATE_DECL_RE.findall(text))),
                "declared_functions": list(dict.fromkeys(FUNC_RE.findall(text))),
                "declared_globals": list(dict.fromkeys(GLOBAL_RE.findall(text))),
                "identifiers": set(IDENT_RE.findall(text)),
                "kotlin_package": package,
                "kotlin_path": kotlin_path,
            }
        )
    return entries


def resolve_deps(entries):
    owner = {}
    for entry in entries:
        if entry["tier"] == "skip":
            continue
        for name in entry["declared_types"] + entry["declared_functions"] + entry["declared_globals"]:
            owner.setdefault(name, entry["path"])
    for entry in entries:
        own = set(entry["declared_types"] + entry["declared_functions"] + entry["declared_globals"])
        deps = set()
        for ident in entry.pop("identifiers"):
            target = owner.get(ident)
            if target and target != entry["path"] and ident not in own:
                deps.add(target)
        entry["deps"] = sorted(deps)


def assign_waves(entries):
    active = {e["path"]: set(e["deps"]) for e in entries if e["tier"] != "skip"}
    for deps in active.values():
        deps.intersection_update(active)
    remaining = set(active)
    result = {}
    wave = 0
    while remaining:
        ready = {p for p in remaining if not (active[p] & remaining)}
        if not ready:
            lowest = min(len(active[p] & remaining) for p in remaining)
            ready = {p for p in remaining if len(active[p] & remaining) == lowest}
        for path in ready:
            result[path] = wave
        remaining -= ready
        wave += 1
    for entry in entries:
        entry["wave"] = result.get(entry["path"], -1)


def summarize(entries):
    rows = {}
    for entry in entries:
        row = rows.setdefault(entry["tier"], [0, 0])
        row[0] += 1
        row[1] += entry["lines"]
    print(f"{'tier':<12}{'files':>8}{'lines':>10}")
    for tier in TIER_ORDER:
        if tier in rows:
            print(f"{tier:<12}{rows[tier][0]:>8}{rows[tier][1]:>10}")
    print(f"{'total':<12}{sum(r[0] for r in rows.values()):>8}{sum(r[1] for r in rows.values()):>10}")


def main():
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    parser = argparse.ArgumentParser(description="Inventory Moblin's Swift code before porting it to Kotlin.")
    upstream = HERE.parent / ".upstream"
    default_root = upstream if (upstream / "Moblin").is_dir() else HERE.parent.parent / "moblin"
    parser.add_argument("--moblin", type=Path, default=default_root)
    parser.add_argument("--out", type=Path, default=HERE / "inventory.json")
    args = parser.parse_args()
    root = args.moblin.resolve()
    if not (root / "Moblin").is_dir():
        parser.error(f"no Moblin directory under {root}")
    entries = scan(root)
    resolve_deps(entries)
    assign_waves(entries)
    entries.sort(key=lambda e: (e["wave"], e["path"]))
    inventory = {
        "root": str(root),
        "generated": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "files": entries,
    }
    args.out.write_text(json.dumps(inventory, indent=2, ensure_ascii=False), encoding="utf-8", newline="\n")
    summarize(entries)
    print(f"\nwrote {args.out}")


if __name__ == "__main__":
    main()
