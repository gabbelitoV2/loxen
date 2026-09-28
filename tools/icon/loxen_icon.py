#!/usr/bin/env python3
import argparse
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw

S = 2048
BG_TOP = (27, 42, 74)
BG_BOTTOM = (11, 18, 32)
AMBER = (245, 166, 35)
AMBER_DARK = (214, 128, 20)
CREAM = (255, 231, 190)
DARK = (27, 27, 30)
INNER_EAR = (255, 205, 140)
LIVE = (255, 59, 48)
IRIS = (255, 196, 64)
WHITE = (255, 255, 255)

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "app/src/main/res"
ASSETS = ROOT / "app/src/main/assets/Loxen"
PLAY_ICON = ROOT / "app/src/main/ic_launcher-playstore.png"
DENSITIES = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
FOREGROUND_SCALE = 0.62
OFFSET = (0, -40)


def background(size):
    image = Image.new("RGB", (size, size))
    draw = ImageDraw.Draw(image)
    for y in range(size):
        t = y / (size - 1)
        color = tuple(round(a + (b - a) * t) for a, b in zip(BG_TOP, BG_BOTTOM))
        draw.line([(0, y), (size, y)], fill=color)
    return image


def lynx(size, scale=1.0, offset=(0, 0), live=True):
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    k = size / 1000 * scale
    ox = size * (1 - scale) / 2 + offset[0] * k
    oy = size * (1 - scale) / 2 + offset[1] * k

    def p(points):
        return [(ox + x * k, oy + y * k) for x, y in points]

    def e(cx, cy, rx, ry):
        return [ox + (cx - rx) * k, oy + (cy - ry) * k, ox + (cx + rx) * k, oy + (cy + ry) * k]

    def rounded(points, radius, fill):
        pts = p(points)
        draw.polygon(pts, fill=fill)
        draw.line(pts + [pts[0]], fill=fill, width=max(1, round(radius * 2 * k)), joint="curve")
        for x, y in pts:
            r = radius * k
            draw.ellipse([x - r, y - r, x + r, y + r], fill=fill)

    rounded([(300, 400), (330, 185), (445, 335)], 26, AMBER_DARK)
    rounded([(700, 400), (670, 185), (555, 335)], 26, AMBER_DARK)
    rounded([(330, 378), (343, 250), (415, 340)], 16, INNER_EAR)
    rounded([(670, 378), (657, 250), (585, 340)], 16, INNER_EAR)
    rounded([(312, 190), (327, 105), (352, 188)], 16, DARK)
    rounded([(688, 190), (673, 105), (648, 188)], 16, DARK)
    head = [(500, 285), (655, 320), (760, 470), (800, 600), (748, 690), (660, 760),
            (500, 815), (340, 760), (252, 690), (200, 600), (240, 470), (345, 320)]
    rounded(head, 22, AMBER)
    rounded([(474, 335), (486, 405), (496, 335)], 8, AMBER_DARK)
    rounded([(504, 335), (514, 405), (526, 335)], 8, AMBER_DARK)
    draw.ellipse(e(500, 690, 150, 108), fill=CREAM)
    for cx in (408, 592):
        draw.ellipse(e(cx, 548, 62, 56), fill=DARK)
        draw.ellipse(e(cx, 550, 48, 44), fill=IRIS)
        draw.ellipse(e(cx, 552, 20, 34), fill=DARK)
        draw.ellipse(e(cx + 16, 532, 13, 13), fill=WHITE)
        draw.ellipse(e(cx - 18, 572, 6, 6), fill=WHITE)
    rounded([(470, 632), (530, 632), (500, 668)], 10, DARK)
    width = max(1, round(12 * k))
    draw.line(p([(500, 668), (500, 700)]), fill=DARK, width=width)
    draw.arc(e(468, 700, 34, 26), 15, 165, fill=DARK, width=width)
    draw.arc(e(532, 700, 34, 26), 15, 165, fill=DARK, width=width)
    if live:
        draw.ellipse(e(800, 235, 58, 58), fill=LIVE)
    return layer


def full_icon(scale=0.86):
    big = background(S).convert("RGBA")
    big.alpha_composite(lynx(S, scale=scale, offset=OFFSET))
    return big


def foreground():
    return lynx(S, scale=FOREGROUND_SCALE, offset=OFFSET)


def color_mask(layer, color):
    channels = layer.split()[:3]
    masks = [channel.point(lambda v, wanted=wanted: 255 if v == wanted else 0) for channel, wanted in zip(channels, color)]
    return ImageChops.multiply(ImageChops.multiply(masks[0], masks[1]), masks[2])


def monochrome():
    layer = foreground()
    holes = Image.new("L", layer.size, 0)
    for color in (DARK, IRIS, WHITE):
        holes = ImageChops.lighter(holes, color_mask(layer, color))
    k = S / 1000 * FOREGROUND_SCALE
    top = S * (1 - FOREGROUND_SCALE) / 2 + OFFSET[1] * k + 400 * k
    ImageDraw.Draw(holes).rectangle([0, 0, S, top], fill=0)
    silhouette = Image.new("RGBA", layer.size, WHITE + (0,))
    silhouette.putalpha(ImageChops.subtract(layer.getchannel("A"), holes))
    return silhouette


def no_background():
    layer = lynx(S, live=False)
    return layer.crop(layer.getchannel("A").getbbox())


def resized(image, size):
    return image.resize(size if isinstance(size, tuple) else (size, size), Image.LANCZOS)


def rounded(image):
    mask = Image.new("L", (S, S), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, S - 1, S - 1], fill=255)
    result = resized(image.convert("RGBA"), S)
    result.putalpha(ImageChops.multiply(result.getchannel("A"), mask))
    return result


def edge_color(image):
    rgb = resized(image.convert("RGB"), 64)
    pixels = [rgb.getpixel((x, y)) for x in range(64) for y in (0, 63)] + \
             [rgb.getpixel((x, y)) for y in range(64) for x in (0, 63)]
    return tuple(round(sum(pixel[i] for pixel in pixels) / len(pixels)) for i in range(3))


def from_png(path):
    image = Image.open(path).convert("RGBA")
    side = min(image.size)
    left, top = (image.width - side) // 2, (image.height - side) // 2
    image = resized(image.crop((left, top, left + side, top + side)), S)
    layer = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    inner = round(S * 72 / 108)
    layer.alpha_composite(resized(image, inner), ((S - inner) // 2, (S - inner) // 2))
    color = edge_color(image)
    return image, layer, lambda size: Image.new("RGB", (size, size), color), image


def adaptive_xml(monochrome_layer):
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">',
        '    <background android:drawable="@mipmap/ic_launcher_background" />',
        '    <foreground android:drawable="@mipmap/ic_launcher_foreground" />',
    ]
    if monochrome_layer:
        lines.append('    <monochrome android:drawable="@mipmap/ic_launcher_monochrome" />')
    lines.append("</adaptive-icon>")
    return "\n".join(lines) + "\n"


def save(image, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


def write(icon, front, back, mono, inapp):
    for name in ("ic_launcher", "ic_launcher_round"):
        path = RES / "mipmap-anydpi-v26" / f"{name}.xml"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(adaptive_xml(mono is not None), encoding="utf-8", newline="\n")
    for density, factor in DENSITIES.items():
        folder = RES / f"mipmap-{density}"
        legacy = round(48 * factor)
        layer = round(108 * factor)
        save(resized(icon.convert("RGB"), legacy), folder / "ic_launcher.png")
        save(resized(rounded(icon), legacy), folder / "ic_launcher_round.png")
        save(resized(front, layer), folder / "ic_launcher_foreground.png")
        save(back(layer), folder / "ic_launcher_background.png")
        monochrome_path = folder / "ic_launcher_monochrome.png"
        if mono is not None:
            save(resized(mono, layer), monochrome_path)
        elif monochrome_path.exists():
            monochrome_path.unlink()
    save(resized(icon, 512), PLAY_ICON)
    save(resized(icon, 1024), ASSETS / "AppIcon.png")
    width = 500
    height = round(inapp.height * width / inapp.width)
    save(resized(inapp, (width, height)), ASSETS / "AppIconNoBackground.png")


def main():
    parser = argparse.ArgumentParser(
        description="Generate the Loxen launcher icons, the Google Play icon and the in-app icon images."
    )
    parser.add_argument("--from", dest="source", type=Path, default=None,
                        help="a square PNG, at least 512x512, to use instead of the drawn lynx")
    parser.add_argument("--preview", type=Path, default=None, help="also write previews to this folder")
    args = parser.parse_args()
    if args.source:
        icon, front, back, inapp = from_png(args.source)
        mono = None
    else:
        icon, front, back, mono, inapp = full_icon(), foreground(), background, monochrome(), no_background()
    write(icon, front, back, mono, inapp)
    if args.preview:
        args.preview.mkdir(parents=True, exist_ok=True)
        preview = Image.new("RGBA", (512 + 16 + 192 + 16 + 96 + 16 + 48, 512), (240, 240, 240, 255))
        small = resized(icon.convert("RGBA"), 512)
        preview.paste(small, (0, 0))
        x = 528
        for size in (192, 96, 48):
            preview.paste(resized(small, size), (x, 0))
            x += size + 16
        preview.save(args.preview / "loxen-preview.png")
        margin = round(S * 18 / 108)
        viewport = (margin, margin, S - margin, S - margin)
        adaptive = Image.new("RGBA", (S, S))
        adaptive.paste(back(S).convert("RGBA"), (0, 0))
        adaptive.alpha_composite(front)
        save(resized(rounded(adaptive.crop(viewport)), 432), args.preview / "loxen-adaptive-preview.png")
        if mono is not None:
            dark = Image.new("RGBA", (S, S), (40, 40, 40, 255))
            dark.alpha_composite(mono)
            save(resized(rounded(dark.crop(viewport)), 432), args.preview / "loxen-monochrome-preview.png")
    print(f"wrote the launcher icons under {RES.relative_to(ROOT).as_posix()}, "
          f"{PLAY_ICON.relative_to(ROOT).as_posix()} and {ASSETS.relative_to(ROOT).as_posix()}")


if __name__ == "__main__":
    main()
