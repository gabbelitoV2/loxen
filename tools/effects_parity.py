#!/usr/bin/env python3
import argparse
import json
import math
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

VIDEO_SUFFIXES = {".mp4", ".mov", ".mkv", ".flv", ".ts", ".webm", ".m4v", ".avi"}
SRGB_TO_XYZ = (
    (0.4124564, 0.3575761, 0.1804375),
    (0.2126729, 0.7151522, 0.0721750),
    (0.0193339, 0.1191920, 0.9503041),
)
WHITE_D65 = (0.95047, 1.0, 1.08883)

try:
    import numpy
except ImportError:
    numpy = None


def tool(name):
    path = shutil.which(name)
    if path is None:
        sys.exit(f"{name} was not found in PATH")
    return path


def ffmpeg(*arguments, capture_stdout=True):
    result = subprocess.run(
        [tool("ffmpeg"), "-hide_banner", *[str(argument) for argument in arguments]],
        capture_output=True,
    )
    if result.returncode != 0:
        sys.exit("ffmpeg failed: " + result.stderr.decode("utf-8", "replace").strip()[-800:])
    return result.stdout if capture_stdout else result.stderr.decode("utf-8", "replace")


def frame_size(path):
    result = subprocess.run(
        [tool("ffprobe"), "-v", "error", "-select_streams", "v:0", "-show_entries", "stream=width,height",
         "-of", "csv=p=0:s=x", str(path)],
        capture_output=True, text=True,
    )
    match = re.search(r"(\d+)x(\d+)", result.stdout)
    if not match:
        sys.exit(f"cannot read the frame size of {path}")
    return int(match.group(1)), int(match.group(2))


def is_video(path):
    return path.suffix.lower() in VIDEO_SUFFIXES


def extract_frame(path, time, directory, name):
    output = directory / f"{name}.png"
    ffmpeg("-v", "error", "-ss", str(time or 0), "-i", path, "-frames:v", "1", "-y", output, capture_stdout=False)
    return output


def scaled_copy(path, size, directory, name):
    output = directory / f"{name}-scaled.png"
    ffmpeg("-v", "error", "-i", path, "-vf", f"scale={size[0]}:{size[1]}:flags=bicubic", "-y", output,
           capture_stdout=False)
    return output


def rgb_pixels(path, size):
    data = ffmpeg("-v", "error", "-i", path, "-frames:v", "1", "-f", "rawvideo", "-pix_fmt", "rgb24", "-")
    expected = size[0] * size[1] * 3
    if len(data) < expected:
        sys.exit(f"{path}: got {len(data)} bytes of rgb24, expected {expected}")
    return data[:expected]


def psnr(path_a, path_b, region):
    crop = f"crop={region['width']}:{region['height']}:{region['x']}:{region['y']},format=rgb24"
    output = ffmpeg("-v", "info", "-i", path_a, "-i", path_b, "-lavfi", f"[0:v]{crop}[a];[1:v]{crop}[b];[a][b]psnr",
                    "-f", "null", "-", capture_stdout=False)
    match = re.search(r"average:(inf|[\d.]+)", output)
    if not match:
        sys.exit("ffmpeg printed no PSNR:\n" + output[-800:])
    return math.inf if match.group(1) == "inf" else float(match.group(1))


def linear(value):
    value = value / 255.0
    return value / 12.92 if value <= 0.04045 else ((value + 0.055) / 1.055) ** 2.4


def lab_f(t):
    return t ** (1.0 / 3.0) if t > (6 / 29) ** 3 else t / (3 * (6 / 29) ** 2) + 4 / 29


def srgb_to_lab(r, g, b):
    rl, gl, bl = linear(r), linear(g), linear(b)
    x, y, z = (row[0] * rl + row[1] * gl + row[2] * bl for row in SRGB_TO_XYZ)
    fx, fy, fz = lab_f(x / WHITE_D65[0]), lab_f(y / WHITE_D65[1]), lab_f(z / WHITE_D65[2])
    return 116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz)


def ciede2000(lab1, lab2):
    l1, a1, b1 = lab1
    l2, a2, b2 = lab2
    c_bar = (math.hypot(a1, b1) + math.hypot(a2, b2)) / 2
    g = 0.5 * (1 - math.sqrt(c_bar ** 7 / (c_bar ** 7 + 25 ** 7)))
    a1p, a2p = (1 + g) * a1, (1 + g) * a2
    c1p, c2p = math.hypot(a1p, b1), math.hypot(a2p, b2)
    h1p = math.degrees(math.atan2(b1, a1p)) % 360 if (a1p or b1) else 0.0
    h2p = math.degrees(math.atan2(b2, a2p)) % 360 if (a2p or b2) else 0.0
    dlp = l2 - l1
    dcp = c2p - c1p
    if c1p * c2p == 0:
        dhp = 0.0
    else:
        dhp = h2p - h1p
        if dhp > 180:
            dhp -= 360
        elif dhp < -180:
            dhp += 360
    dhp_big = 2 * math.sqrt(c1p * c2p) * math.sin(math.radians(dhp) / 2)
    l_bar = (l1 + l2) / 2
    c_barp = (c1p + c2p) / 2
    if c1p * c2p == 0:
        h_bar = h1p + h2p
    elif abs(h1p - h2p) <= 180:
        h_bar = (h1p + h2p) / 2
    elif h1p + h2p < 360:
        h_bar = (h1p + h2p + 360) / 2
    else:
        h_bar = (h1p + h2p - 360) / 2
    t = (1 - 0.17 * math.cos(math.radians(h_bar - 30)) + 0.24 * math.cos(math.radians(2 * h_bar))
         + 0.32 * math.cos(math.radians(3 * h_bar + 6)) - 0.20 * math.cos(math.radians(4 * h_bar - 63)))
    d_theta = 30 * math.exp(-(((h_bar - 275) / 25) ** 2))
    r_c = 2 * math.sqrt(c_barp ** 7 / (c_barp ** 7 + 25 ** 7))
    s_l = 1 + 0.015 * (l_bar - 50) ** 2 / math.sqrt(20 + (l_bar - 50) ** 2)
    s_c = 1 + 0.045 * c_barp
    s_h = 1 + 0.015 * c_barp * t
    r_t = -math.sin(math.radians(2 * d_theta)) * r_c
    return math.sqrt((dlp / s_l) ** 2 + (dcp / s_c) ** 2 + (dhp_big / s_h) ** 2 + r_t * (dcp / s_c) * (dhp_big / s_h))


def numpy_lab(rgb):
    np = numpy
    value = rgb.astype(np.float64) / 255.0
    lin = np.where(value <= 0.04045, value / 12.92, ((value + 0.055) / 1.055) ** 2.4)
    xyz = lin @ np.array(SRGB_TO_XYZ).T / np.array(WHITE_D65)
    f = np.where(xyz > (6 / 29) ** 3, np.cbrt(xyz), xyz / (3 * (6 / 29) ** 2) + 4 / 29)
    return np.stack([116 * f[..., 1] - 16, 500 * (f[..., 0] - f[..., 1]), 200 * (f[..., 1] - f[..., 2])], axis=-1)


def numpy_ciede2000(lab1, lab2):
    np = numpy
    l1, a1, b1 = lab1[..., 0], lab1[..., 1], lab1[..., 2]
    l2, a2, b2 = lab2[..., 0], lab2[..., 1], lab2[..., 2]
    c_bar = (np.hypot(a1, b1) + np.hypot(a2, b2)) / 2
    g = 0.5 * (1 - np.sqrt(c_bar ** 7 / (c_bar ** 7 + 25.0 ** 7)))
    a1p, a2p = (1 + g) * a1, (1 + g) * a2
    c1p, c2p = np.hypot(a1p, b1), np.hypot(a2p, b2)
    h1p = np.where((a1p == 0) & (b1 == 0), 0.0, np.degrees(np.arctan2(b1, a1p)) % 360)
    h2p = np.where((a2p == 0) & (b2 == 0), 0.0, np.degrees(np.arctan2(b2, a2p)) % 360)
    zero = c1p * c2p == 0
    dhp = h2p - h1p
    dhp = np.where(dhp > 180, dhp - 360, np.where(dhp < -180, dhp + 360, dhp))
    dhp = np.where(zero, 0.0, dhp)
    dhp_big = 2 * np.sqrt(c1p * c2p) * np.sin(np.radians(dhp) / 2)
    l_bar = (l1 + l2) / 2
    c_barp = (c1p + c2p) / 2
    total = h1p + h2p
    h_bar = np.where(
        zero, total,
        np.where(np.abs(h1p - h2p) <= 180, total / 2, np.where(total < 360, (total + 360) / 2, (total - 360) / 2)),
    )
    t = (1 - 0.17 * np.cos(np.radians(h_bar - 30)) + 0.24 * np.cos(np.radians(2 * h_bar))
         + 0.32 * np.cos(np.radians(3 * h_bar + 6)) - 0.20 * np.cos(np.radians(4 * h_bar - 63)))
    d_theta = 30 * np.exp(-(((h_bar - 275) / 25) ** 2))
    r_c = 2 * np.sqrt(c_barp ** 7 / (c_barp ** 7 + 25.0 ** 7))
    s_l = 1 + 0.015 * (l_bar - 50) ** 2 / np.sqrt(20 + (l_bar - 50) ** 2)
    s_c = 1 + 0.045 * c_barp
    s_h = 1 + 0.015 * c_barp * t
    r_t = -np.sin(np.radians(2 * d_theta)) * r_c
    return np.sqrt(((l2 - l1) / s_l) ** 2 + ((c2p - c1p) / s_c) ** 2 + (dhp_big / s_h) ** 2
                   + r_t * ((c2p - c1p) / s_c) * (dhp_big / s_h))


def mean_delta_e(data_a, data_b, size, region):
    width = size[0]
    x0, y0, w, h = region["x"], region["y"], region["width"], region["height"]
    if numpy is not None:
        a = numpy.frombuffer(data_a, dtype=numpy.uint8).reshape(size[1], width, 3)[y0:y0 + h, x0:x0 + w]
        b = numpy.frombuffer(data_b, dtype=numpy.uint8).reshape(size[1], width, 3)[y0:y0 + h, x0:x0 + w]
        return float(numpy_ciede2000(numpy_lab(a), numpy_lab(b)).mean())
    total = 0.0
    cache = {}
    for y in range(y0, y0 + h):
        row = (y * width + x0) * 3
        for x in range(w):
            offset = row + x * 3
            pixel_a = tuple(data_a[offset:offset + 3])
            pixel_b = tuple(data_b[offset:offset + 3])
            if pixel_a == pixel_b:
                continue
            key = (pixel_a, pixel_b)
            if key not in cache:
                cache[key] = ciede2000(srgb_to_lab(*pixel_a), srgb_to_lab(*pixel_b))
            total += cache[key]
    return total / (w * h)


def load_regions(path, size):
    if path is None:
        return [{"name": "frame", "x": 0, "y": 0, "width": size[0], "height": size[1], "psnr": None, "de": None}]
    data = json.loads(Path(path).read_text(encoding="utf-8-sig"))
    regions = data.get("regions", []) if isinstance(data, dict) else data
    result = []
    for index, region in enumerate(regions):
        name = region.get("name", f"region {index + 1}")
        try:
            x, y, w, h = (int(region[key]) for key in ("x", "y", "width", "height"))
        except (KeyError, TypeError, ValueError):
            sys.exit(f"{path}: region {name} needs integer x, y, width and height (pixels, top-left origin)")
        if w <= 0 or h <= 0 or x < 0 or y < 0 or x + w > size[0] or y + h > size[1]:
            sys.exit(f"{path}: region {name} ({x}, {y}, {w}x{h}) is outside the {size[0]}x{size[1]} frame")
        result.append({"name": name, "x": x, "y": y, "width": w, "height": h,
                       "psnr": region.get("psnr"), "de": region.get("de")})
    if not result:
        sys.exit(f"{path}: no regions")
    return result


def main():
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    parser = argparse.ArgumentParser(
        description="Compare two frames (PNG files, or two videos at a timestamp) region by region: PSNR from "
        "ffmpeg's psnr filter and the mean CIEDE2000 colour difference. Exits with 1 when a region is below "
        "--psnr or above --de."
    )
    parser.add_argument("a", type=Path, help="reference frame or video (for example the iOS stream)")
    parser.add_argument("b", type=Path, help="frame or video to compare (for example the Android stream)")
    parser.add_argument("regions", nargs="?", default=None,
                        help='JSON list of {"name", "x", "y", "width", "height"} in pixels with a top-left origin, '
                        'optionally with per-region "psnr" and "de" thresholds; the whole frame when left out')
    parser.add_argument("--time", type=float, default=None, help="timestamp in seconds for both videos")
    parser.add_argument("--time-a", type=float, default=None, help="timestamp in seconds for the first video")
    parser.add_argument("--time-b", type=float, default=None, help="timestamp in seconds for the second video")
    parser.add_argument("--psnr", type=float, default=None, help="minimum PSNR in dB")
    parser.add_argument("--de", type=float, default=None, help="maximum mean CIEDE2000")
    parser.add_argument("--scale", action="store_true", help="scale the second frame to the size of the first")
    args = parser.parse_args()
    for path in (args.a, args.b):
        if not path.exists():
            sys.exit(f"{path} does not exist")
    with tempfile.TemporaryDirectory(prefix="effects-parity-") as temporary:
        directory = Path(temporary)
        time_a = args.time_a if args.time_a is not None else args.time
        time_b = args.time_b if args.time_b is not None else args.time
        frame_a = extract_frame(args.a, time_a, directory, "a") if is_video(args.a) else args.a
        frame_b = extract_frame(args.b, time_b, directory, "b") if is_video(args.b) else args.b
        size_a, size_b = frame_size(frame_a), frame_size(frame_b)
        if size_a != size_b:
            if not args.scale:
                sys.exit(f"the frames differ in size ({size_a[0]}x{size_a[1]} and {size_b[0]}x{size_b[1]}); "
                         "pass --scale to scale the second one")
            frame_b = scaled_copy(frame_b, size_a, directory, "b")
        regions = load_regions(args.regions, size_a)
        data_a, data_b = rgb_pixels(frame_a, size_a), rgb_pixels(frame_b, size_a)
        failed = 0
        print(f"{'region':<24}{'size':>12}{'PSNR dB':>10}{'mean dE':>10}  result")
        for region in regions:
            value_psnr = psnr(frame_a, frame_b, region)
            value_de = mean_delta_e(data_a, data_b, size_a, region)
            minimum = region["psnr"] if region["psnr"] is not None else args.psnr
            maximum = region["de"] if region["de"] is not None else args.de
            problems = []
            if minimum is not None and value_psnr < minimum:
                problems.append(f"PSNR < {minimum:g}")
            if maximum is not None and value_de > maximum:
                problems.append(f"dE > {maximum:g}")
            failed += bool(problems)
            shown_psnr = "inf" if math.isinf(value_psnr) else f"{value_psnr:.2f}"
            size = f"{region['width']}x{region['height']}"
            verdict = "FAIL " + ", ".join(problems) if problems else "ok"
            print(f"{region['name'][:23]:<24}{size:>12}{shown_psnr:>10}{value_de:>10.3f}  {verdict}")
    if failed:
        print(f"{failed} of {len(regions)} regions below the thresholds")
        sys.exit(1)


if __name__ == "__main__":
    main()
