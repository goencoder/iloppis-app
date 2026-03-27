from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "output/wallpapers"
OUT_DIR.mkdir(parents=True, exist_ok=True)

SIZES = [(1440, 3200), (1080, 2400)]

for width, height in SIZES:
    base = Image.new("RGBA", (width, height), "#2F7373")
    draw = ImageDraw.Draw(base, "RGBA")

    top = (86, 164, 160)
    bottom = (33, 93, 96)
    for y in range(height):
        t = y / (height - 1)
        r = int(top[0] * (1 - t) + bottom[0] * t)
        g = int(top[1] * (1 - t) + bottom[1] * t)
        b = int(top[2] * (1 - t) + bottom[2] * t)
        draw.line([(0, y), (width, y)], fill=(r, g, b, 255))

    overlay = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    od = ImageDraw.Draw(overlay, "RGBA")

    od.ellipse(
        [int(-0.35 * width), int(-0.20 * height), int(0.70 * width), int(0.42 * height)],
        fill=(245, 232, 233, 36),
    )
    od.ellipse(
        [int(0.30 * width), int(0.05 * height), int(1.20 * width), int(0.72 * height)],
        fill=(24, 79, 83, 74),
    )
    od.rounded_rectangle(
        [int(0.08 * width), int(0.34 * height), int(0.92 * width), int(0.70 * height)],
        radius=int(0.08 * width),
        fill=(250, 243, 244, 24),
    )
    od.ellipse(
        [int(-0.25 * width), int(0.62 * height), int(0.95 * width), int(1.28 * height)],
        fill=(21, 71, 74, 66),
    )
    od.rounded_rectangle(
        [int(0.36 * width), int(0.53 * height), int(0.64 * width), int(0.535 * height)],
        radius=max(8, int(width * 0.005)),
        fill=(237, 179, 27, 175),
    )

    overlay = overlay.filter(ImageFilter.GaussianBlur(radius=max(2, int(width * 0.0025))))
    final = Image.alpha_composite(base, overlay).convert("RGB")

    out = OUT_DIR / f"iloppis-wallpaper-{width}x{height}.png"
    final.save(out)
    print(out)
