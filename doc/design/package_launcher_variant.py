"""Package the generated foreground into Android launcher resources and previews."""
from pathlib import Path
import argparse
import math
import shutil
from PIL import Image, ImageDraw, ImageChops, ImageOps

ROOT = Path(__file__).resolve().parents[2]
DESIGN = ROOT / "doc/design"
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("variant", choices=("ghost", "cat"))
args = parser.parse_args()
NAME = {"ghost": "v14", "cat": "cat-v1"}[args.variant]
OUTPUT = DESIGN / "launcher-variants" / args.variant
RES = OUTPUT / "res"
BACKGROUND = "#C3D1AD" if args.variant == "cat" else "#B8C8A5"

def build():
    (RES / "drawable-nodpi").mkdir(parents=True, exist_ok=True)
    source = Image.open(DESIGN / f"launcher-icon-{NAME}-source.png").convert("RGBA")
    subject = source.crop(source.getbbox())
    cx, cy = subject.width / 2, subject.height / 2
    radius = max(math.hypot(x - cx, y - cy)
                 for y in range(subject.height) for x in range(subject.width)
                 if subject.getpixel((x, y))[3] > 16)
    scale = 130 / radius
    subject = subject.resize((round(subject.width * scale), round(subject.height * scale)), Image.Resampling.LANCZOS)
    foreground = Image.new("RGBA", (432, 432))
    foreground.alpha_composite(subject, ((432 - subject.width) // 2, (432 - subject.height) // 2))
    foreground.save(RES / "drawable-nodpi/ic_launcher_art.png")
    mono = Image.new("RGBA", foreground.size, "black")
    ink = ImageOps.invert(ImageOps.grayscale(foreground))
    ink = ink.point(lambda value: 0 if value < 40 else min(255, (value - 40) * 255 // 180))
    mono.putalpha(ImageChops.multiply(ink, foreground.getchannel("A")))
    mono.save(RES / "drawable-nodpi/ic_launcher_monochrome.png")
    full = Image.new("RGBA", foreground.size, BACKGROUND)
    full.alpha_composite(foreground)
    # Launcher displays the central 72dp of the 108dp adaptive canvas.
    visible = full.crop((72, 72, 360, 360))
    for density, size in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)]:
        (RES / f"mipmap-{density}").mkdir(parents=True, exist_ok=True)
        icon = visible.resize((size, size), Image.Resampling.LANCZOS)
        for rounded, name in [(False, "ic_launcher"), (True, "ic_launcher_round")]:
            mask = Image.new("L", (size * 4, size * 4))
            draw = ImageDraw.Draw(mask)
            if rounded:
                draw.ellipse((0, 0, size * 4 - 1, size * 4 - 1), fill=255)
            else:
                draw.rounded_rectangle((0, 0, size * 4 - 1, size * 4 - 1), radius=size * .8, fill=255)
            output = icon.copy()
            output.putalpha(mask.resize((size, size), Image.Resampling.LANCZOS))
            output.save(RES / f"mipmap-{density}/{name}.png")
    preview = visible.resize((512, 512), Image.Resampling.LANCZOS)
    preview.save(OUTPUT / "preview.png")
    for name in ("ic_launcher", "ic_launcher_round"):
        target = RES / "mipmap-anydpi-v26" / f"{name}.xml"
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(ROOT / "app/src/main/res/mipmap-anydpi-v26" / f"{name}.xml", target)
    (RES / "values").mkdir(exist_ok=True)
    (RES / "values/launcher_colors.xml").write_text(f'<resources><color name="launcher_background">{BACKGROUND}</color></resources>\n')
    board = Image.new("RGBA", (840, 300), "#FAF8F1")
    for index, shape in enumerate(("square", "round", "mono")):
        art = visible.resize((240, 240), Image.Resampling.LANCZOS)
        if shape == "mono":
            base = Image.new("RGBA", (432, 432), "#DDEDD8")
            colored = Image.new("RGBA", (432, 432), "#263E36")
            colored.putalpha(mono.getchannel("A"))
            base.alpha_composite(colored)
            art = base.crop((72, 72, 360, 360)).resize((240, 240), Image.Resampling.LANCZOS)
        mask = Image.new("L", (240, 240))
        draw = ImageDraw.Draw(mask)
        if shape == "round":
            draw.ellipse((0, 0, 239, 239), fill=255)
        else:
            draw.rounded_rectangle((0, 0, 239, 239), radius=52, fill=255)
        art.putalpha(mask)
        board.alpha_composite(art, (20 + index * 280, 20))
        small = visible.resize((48, 48), Image.Resampling.LANCZOS)
        board.alpha_composite(small, (116 + index * 280, 250))
    board.save(OUTPUT / "masks-preview.png")

if __name__ == "__main__":
    build()
