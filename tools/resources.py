#!/usr/bin/env python3
import argparse
import json
import shutil
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
ASSETS = ROOT / "app/src/main/assets"


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


if __name__ == "__main__":
    main()
