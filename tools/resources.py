#!/usr/bin/env python3
import argparse
import json
import re
import shutil
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
ASSETS = ROOT / "app/src/main/assets"
VERSION = ROOT / "app/moblin-version.properties"
MARKETING_VERSION = re.compile(r"^[ \t]*MARKETING_VERSION[ \t]*=[ \t]*(\d+(?:\.\d+)*)[ \t]*(?://.*)?$", re.MULTILINE)


def mirror_bundle(source, target):
    copied = 0
    if target.exists():
        shutil.rmtree(target)
    for path in sorted(source.rglob("*")):
        if path.is_file():
            destination = target / path.relative_to(source)
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(path, destination)
            copied += 1
    return copied


def best_image(imageset):
    contents = imageset / "Contents.json"
    if not contents.exists():
        return None
    images = json.loads(contents.read_text(encoding="utf-8")).get("images", [])
    candidates = []
    for image in images:
        filename = image.get("filename")
        if not filename or not (imageset / filename).exists():
            continue
        if image.get("appearances"):
            continue
        scale = image.get("scale", "1x").rstrip("x")
        size = image.get("size", "0x0").split("x")[0]
        try:
            rank = float(scale) * 1000 + float(size)
        except ValueError:
            rank = 0
        candidates.append((rank, imageset / filename))
    return max(candidates)[1] if candidates else None


def mirror_asset_catalog(catalog, target):
    copied = 0
    if target.exists():
        shutil.rmtree(target)
    target.mkdir(parents=True)
    for imageset in sorted(catalog.iterdir()):
        if imageset.suffix not in (".imageset", ".appiconset"):
            continue
        image = best_image(imageset)
        if image is None:
            continue
        shutil.copyfile(image, target / (imageset.stem + image.suffix.lower()))
        copied += 1
    return copied


LOOSE_RESOURCE_SUFFIXES = (".js",)


def mirror_loose_resources(app, target):
    copied = {}
    for path in sorted(app.rglob("*")):
        if not path.is_file() or path.suffix not in LOOSE_RESOURCE_SUFFIXES:
            continue
        parts = path.relative_to(app).parts
        if any(part.endswith((".bundle", ".xcassets")) or part == "node_modules" or part.startswith(".") for part in parts):
            continue
        if path.name in copied:
            print(f"warning: {path.relative_to(app).as_posix()} has the same name as {copied[path.name]}, not copied")
            continue
        target.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(path, target / path.name)
        copied[path.name] = path.relative_to(app).as_posix()
    return copied


def marketing_version(config):
    files = sorted(config.glob("*.xcconfig"), key=lambda path: (path.name != "Base.xcconfig", path.name))
    for path in files:
        match = MARKETING_VERSION.search(path.read_text(encoding="utf-8"))
        if match:
            return match.group(1)
    return None


def mirror_version(config, target):
    version = marketing_version(config) if config.is_dir() else None
    if version is None:
        return None
    text = f"MARKETING_VERSION={version}\n"
    if not target.exists() or target.read_text(encoding="utf-8") != text:
        target.write_text(text, encoding="utf-8", newline="\n")
    return version


def main():
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    parser = argparse.ArgumentParser(description="Mirror Moblin's bundled resources into the Android assets.")
    parser.add_argument("--moblin", type=Path, default=ROOT / ".upstream")
    args = parser.parse_args()
    app = args.moblin / "Moblin"
    for bundle in sorted(app.glob("*.bundle")):
        count = mirror_bundle(bundle, ASSETS / bundle.name)
        print(f"{bundle.name}: {count} files")
    catalog = app / "Assets.xcassets"
    if catalog.is_dir():
        count = mirror_asset_catalog(catalog, ASSETS / "Assets")
        print(f"Assets.xcassets: {count} images")
    loose = mirror_loose_resources(app, ASSETS)
    print(f"loose resources: {len(loose)} files ({', '.join(sorted(loose))})")
    version = mirror_version(args.moblin / "Config", VERSION)
    print(f"MARKETING_VERSION: {version or 'not found, ' + VERSION.name + ' kept'}")


if __name__ == "__main__":
    main()
