"""Package the approved PNG as Android launcher resources, without redrawing it.

Development dependency: Pillow. All outputs retain the original transparent pixels.
"""
from pathlib import Path
from PIL import Image
import hashlib
import json

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "artwork/branding/mindrop-md-fluido.png"
RES = ROOT / "app/src/main/res"


def main():
    original = Image.open(SOURCE).convert("RGBA")
    bbox = original.getchannel("A").point(lambda a: 255 if a > 16 else 0).getbbox()
    assert bbox, "The approved logo is empty"
    mark = original.crop(bbox)
    for density, scale in (("mdpi", 1), ("hdpi", 1.5), ("xhdpi", 2), ("xxhdpi", 3), ("xxxhdpi", 4)):
        for folder, name, canvas_dp, mark_dp in (
            ("drawable", "ic_launcher_foreground", 108, 60),
            ("mipmap", "ic_launcher", 48, 44),
        ):
            side = round(canvas_dp * scale)
            artwork = mark.copy()
            artwork.thumbnail((round(mark_dp * scale), round(mark_dp * scale)), Image.Resampling.LANCZOS)
            canvas = Image.new("RGBA", (side, side), (0, 0, 0, 0))
            canvas.alpha_composite(artwork, ((side-artwork.width)//2, (side-artwork.height)//2))
            destination = RES / f"{folder}-{density}" / f"{name}.png"
            destination.parent.mkdir(parents=True, exist_ok=True)
            canvas.save(destination, optimize=True)
            assert canvas.getpixel((0, 0))[3] == 0
    (SOURCE.parent / "launcher-source.json").write_text(json.dumps({
        "source": SOURCE.name,
        "sha256": hashlib.sha256(SOURCE.read_bytes()).hexdigest(),
        "selection": "Design 2: fluid lettering with curved color boundary",
        "crop": bbox,
        "adaptive_canvas_dp": 108,
        "adaptive_mark_dp": 60,
        "legacy_canvas_dp": 48,
        "legacy_mark_dp": 44,
        "processing": "Alpha bounding crop, proportional resize and transparent padding only; no redrawing or recoloring",
    }, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
