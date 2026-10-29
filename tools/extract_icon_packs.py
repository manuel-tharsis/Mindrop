"""Extract the approved artwork; never redraw or regenerate the icon silhouettes.

Development-only dependencies: Pillow and NumPy. Runtime Android has no dependency
on this script. Keep source crops and lossless PNG/WebP pairs for visual auditing.
"""
from pathlib import Path
from collections import deque
import hashlib
import json
import colorsys
import argparse

import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / "artwork" / "icon-packs"
SOURCES = ART / "sources"
EXTRACTED = ART / "originals"
PREPARED = ART / "prepared"
RES = ROOT / "app/src/main/res/drawable-nodpi"
CATALOG = ROOT / "app/src/main/java/com/mindrop/app/ui/icons/PackAssetCatalog.kt"
PACKS = ["basic", "adventure", "forest", "arcane", "cyber"]
ENUMS = dict(zip(PACKS, ["BASIC", "ADVENTURE", "FOREST", "ARCANE", "CYBER"]))
ACCENTS = {"adventure": "edbb58", "forest": "8fc478", "arcane": "699cff", "cyber": "55e9de"}
VARIANTS = {"forest": "8de4a8", "night": "c7beff", "cyber": "55f4e6", "gold": "ffd56a"}
ROLES = "idea folder code mobile computer car tools game home work document star brain money sport terminal calendar flag add edit delete search move back next list check page_add image_add pdf draw project settings trophy camera image save completed".split()
originals = {}
records = []
REEXTRACT = False


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def remove_background(image):
    """Fit the smooth navy background from the crop border and retain its objects.

    Foreground RGB is copied verbatim, including the antialiasing pixels. Only
    alpha changes. Connected components reject isolated background noise.
    """
    rgb = np.asarray(image.convert("RGB"), dtype=np.float64)
    h, w = rgb.shape[:2]
    yy, xx = np.mgrid[-1:1:complex(h), -1:1:complex(w)]
    basis = np.stack([np.ones_like(xx), xx, yy, xx * yy, xx * xx, yy * yy], axis=-1)
    border = np.zeros((h, w), dtype=bool)
    border[:4] = True; border[-4:] = True; border[:, :4] = True; border[:, -4:] = True
    # A few drawings approach the crop border; never fit the background to them.
    samples = border & (rgb.max(axis=2) < 65)
    for _ in range(3):
        coefficients = np.linalg.lstsq(basis[samples], rgb[samples], rcond=None)[0]
        background = np.clip(basis @ coefficients, 0, 255)
        residual = np.max(np.abs(rgb-background), axis=2)
        samples = border & (rgb.max(axis=2) < 65) & (residual < 5)
    distance = np.max(np.abs(rgb - background), axis=2)
    # The navy backdrop and its soft poster shadows are not part of the icons.
    navy = (rgb[:,:,0] < 32) & (rgb[:,:,1] < 37) & (rgb[:,:,2] < 62) & (rgb[:,:,2] > rgb[:,:,0]*1.15) & (rgb[:,:,2] > rgb[:,:,1]*1.15)
    weak = (distance > 7) & ~navy
    strong = distance > 20
    keep = np.zeros((h, w), dtype=bool)
    seen = np.zeros((h, w), dtype=bool)
    for y, x in zip(*np.nonzero(weak)):
        if seen[y, x]:
            continue
        queue = deque([(y, x)])
        seen[y, x] = True
        component = []
        strong_count = 0
        while queue:
            cy, cx = queue.popleft(); component.append((cy, cx))
            strong_count += int(strong[cy, cx])
            for ny, nx in ((cy-1, cx), (cy+1, cx), (cy, cx-1), (cy, cx+1)):
                if 0 <= ny < h and 0 <= nx < w and weak[ny, nx] and not seen[ny, nx]:
                    seen[ny, nx] = True; queue.append((ny, nx))
        if strong_count >= 3 and len(component) >= 10:
            ys, xs = zip(*component); keep[ys, xs] = True
    alpha = np.where(keep, np.clip((distance - 7) / 7, 0, 1), 0)
    out = np.zeros((h, w, 4), dtype=np.uint8)
    out[keep, :3] = rgb[keep].astype(np.uint8)
    out[:, :, 3] = (alpha * 255).round().astype(np.uint8)
    out[out[:,:,3] == 0, :3] = 0
    return Image.fromarray(out)


def square(icon):
    box = icon.getbbox()
    if not box:
        raise ValueError("No foreground detected")
    icon = icon.crop(box)
    # No upscaling: native source pixels are kept in a padded square.
    side = int(np.ceil(max(icon.size) / .86))
    result = Image.new("RGBA", (side, side))
    result.paste(icon, ((side-icon.width)//2, (side-icon.height)//2))
    return result


def extract(sheet, pack, role, bounds):
    source = SOURCES / sheet
    path = EXTRACTED / pack / f"{role}.png"
    if path.is_file() and not REEXTRACT:
        with Image.open(path) as existing:
            icon = existing.convert("RGBA")
    else:
        with Image.open(source) as full:
            icon = square(remove_background(full.crop(bounds)))
        path.parent.mkdir(parents=True, exist_ok=True)
        icon.save(path)
    originals[pack, role] = icon
    records.append(dict(pack=pack, role=role, source=sheet, crop=list(bounds), png=str(path.relative_to(ROOT)), sha256=digest(path)))


def recolor(icon, target, change_yellow=False):
    """Adapt the source colors for artwork that was not drawn in that pack.

    Shape, opacity and neutral shading are retained. A hue-based color adaptation
    is recorded as derived, never described as an exact source extraction.
    """
    data = np.array(icon)
    rgb = data[:, :, :3].astype(float) / 255
    maximum = rgb.max(axis=2); minimum = rgb.min(axis=2)
    saturation = np.divide(maximum-minimum, maximum, out=np.zeros_like(maximum), where=maximum > 0)
    blue = (rgb[:, :, 2] > rgb[:, :, 0] * 1.12) & (rgb[:, :, 2] >= rgb[:, :, 1]) & (saturation > .4) & (maximum > .6) & (data[:, :, 3] > 0)
    hue, sat, val = colorsys.rgb_to_hsv(*(int(target[i:i+2], 16)/255 for i in (0, 2, 4)))
    for y, x in zip(*np.nonzero(blue)):
        value = maximum[y, x]
        new = colorsys.hsv_to_rgb(hue, min(sat, saturation[y, x]), value)
        data[y, x, :3] = np.round(np.array(new) * 255).astype(np.uint8)
    if change_yellow:
        warm = (rgb[:, :, 0] > rgb[:, :, 2]*1.5) & (rgb[:, :, 1] > rgb[:, :, 2]*1.25) & (saturation > .35) & (data[:, :, 3] > 0)
        body = np.array([.61, .43, .28]) if target == ACCENTS["adventure"] else np.array([.36, .4, .49])
        for y, x in zip(*np.nonzero(warm)):
            data[y, x, :3] = np.clip(body * (.65 + maximum[y, x]*.45) * 255, 0, 255).round().astype(np.uint8)
    return Image.fromarray(data)


def badge(image, plus):
    result = image.copy()
    side = max(1, int(image.width * .4))
    overlay = plus.resize((side, side), Image.Resampling.LANCZOS)
    result.alpha_composite(overlay, (image.width-side, 0))
    return result


def main(reextract=False):
    global REEXTRACT
    REEXTRACT = reextract
    basic_rows = [
        "folder idea project calendar trophy settings".split(),
        "code mobile computer car tools game".split(),
        "home work document star brain money".split(),
        "sport camera image draw flag completed".split(),
        "add edit save delete move search".split(),
        "back next list page_add pdf check".split(),
    ]
    bx = [(45,207),(252,413),(455,619),(660,827),(863,1055),(1075,1270)]
    by = [(135,258),(307,432),(487,605),(662,774),(826,943),(997,1116)]
    for row, roles in enumerate(basic_rows):
        for col, role in enumerate(roles):
            extract("basic.png", "basic", role, (bx[col][0],by[row][0],bx[col][1],by[row][1]))
    tx = [(164,320),(333,453),(482,627),(644,786),(807,960),(966,1117),(1127,1281),(1290,1420)]
    ty = [(168,313),(334,482),(491,637),(646,793)]
    for row, pack in enumerate(PACKS[1:]):
        for col, role in enumerate("folder idea project calendar trophy settings search edit".split()):
            extract("thematic.png", pack, role, (tx[col][0],ty[row][0],tx[col][1],ty[row][1]))
    cx = [[(35,206),(215,353),(370,524)],[(574,718),(736,886),(904,1053)]]
    cy = [[(148,263),(300,402),(433,551),(586,699)],[(808,927),(965,1070),(1107,1220),(1259,1364)]]
    categories = "code mobile computer car tools game home work document brain money sport".split()
    for pack_index, pack in enumerate(PACKS[1:]):
        group_row, group_col = divmod(pack_index, 2)
        for index, role in enumerate(categories):
            row, col = divmod(index, 3)
            extract("categories.png", pack, role, (cx[group_col][col][0],cy[group_row][row][0],cx[group_col][col][1],cy[group_row][row][1]))

    prepared = {}
    provenance = {}
    for pack in PACKS:
        for role in ROLES:
            source_role = "code" if role == "terminal" else "trophy" if role == "star" and pack != "basic" else "edit" if role == "draw" and pack != "basic" else role
            if (pack, source_role) in originals:
                image = originals[pack, source_role].copy()
                origin = dict(kind="source", original_pack=pack, original_role=source_role)
            elif role == "image_add":
                image = originals["basic", "image"].copy()
                plus = originals["basic", "add"].copy()
                if pack != "basic":
                    image = recolor(image, ACCENTS[pack], change_yellow=True)
                    plus = recolor(plus, ACCENTS[pack])
                image = badge(image, plus)
                origin = dict(kind="composite", original_pack="basic", original_role="image")
            else:
                image = originals["basic", source_role].copy()
                preserve = role in ("flag", "completed", "check")
                if pack != "basic" and not preserve:
                    image = recolor(image, ACCENTS[pack], change_yellow=True)
                origin = dict(kind="shared" if preserve else "adapted", original_pack="basic", original_role=source_role)
            prepared[pack, role] = image
            provenance[pack, role] = origin

    # The image_add composition needs the image prepared even when role order changes.
    assets = []
    canonical = {}
    used_resources = set()
    RES.mkdir(parents=True, exist_ok=True)
    for (pack, role), image in prepared.items():
        for variant in (["default", *VARIANTS] if pack == "arcane" else ["default"]):
            actual = recolor(image, VARIANTS[variant]) if variant != "default" else image
            suffix = "" if variant == "default" else f"_{variant}"
            filename = f"pack_{pack}_{role}{suffix}"
            png = PREPARED / pack / f"{role}{suffix}.png"
            png.parent.mkdir(parents=True, exist_ok=True); actual.save(png)
            webp = RES / f"{filename}.webp"
            actual.save(webp, "WEBP", lossless=True, exact=True, method=6)
            with Image.open(webp) as encoded:
                if not np.array_equal(np.asarray(actual), np.asarray(encoded.convert("RGBA"))):
                    raise ValueError(f"Lossless mismatch: {filename}")
            sha = digest(webp)
            if sha in canonical:
                filename = canonical[sha]
                webp = RES / f"{filename}.webp"
            else:
                canonical[sha] = filename
            used_resources.add(filename)
            assets.append(dict(pack=pack, role=role, variant=variant, resource=filename, png=str(png.relative_to(ROOT)), webp=str(webp.relative_to(ROOT)), sha256=sha, provenance=provenance[pack,role]))

    # Only generated files in this exact resource folder are eligible for cleanup.
    for path in RES.glob("pack_*.webp"):
        if path.stem not in used_resources:
            assert path.parent.resolve() == RES.resolve()
            path.unlink()

    catalog = ["// Generated by tools/extract_icon_packs.py from the approved artwork.",
        "package com.mindrop.app.ui.icons", "", "import com.mindrop.app.R", "", "internal data class PackAssetKey(val pack: IconPack, val role: String, val variant: String = \"default\")", "", "internal val packAssetResources = mapOf("]
    for asset in assets:
        catalog.append(f'    PackAssetKey(IconPack.{ENUMS[asset["pack"]]}, "{asset["role"]}", "{asset["variant"]}") to R.drawable.{asset["resource"]},')
    catalog += [")", ""]
    CATALOG.write_text("\n".join(catalog), encoding="utf-8")
    manifest = dict(sources={p.name: digest(p) for p in SOURCES.glob("*.png")}, originals=records, assets=assets)
    (ART / "manifest.json").write_text(json.dumps(manifest, indent=2, ensure_ascii=False)+"\n", encoding="utf-8")

    # QA contacts are built from the actual prepared files, never redrawn examples.
    qa = ROOT / "work/icon-assets-qa"; qa.mkdir(parents=True, exist_ok=True)
    labels = "folder idea project calendar trophy settings search edit code mobile computer car tools game home work document brain money sport".split()
    for pack in PACKS:
        board = Image.new("RGB", (800, 5*160), "#000000")
        drawing = ImageDraw.Draw(board)
        for i, role in enumerate(labels):
            image = prepared[pack,role].copy(); image.thumbnail((110,110), Image.Resampling.LANCZOS)
            x=(i%5)*160+(160-image.width)//2; y=(i//5)*160+14
            board.paste(image,(x,y),image)
            drawing.text(((i%5)*160+12,(i//5)*160+130),role,fill="white")
        board.save(qa/f"{pack}.png")
    print(json.dumps(dict(originals=len(records), mappings=len(assets), resources=len(used_resources), webp_bytes=sum((RES/(name+".webp")).stat().st_size for name in used_resources))))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--extract-originals", action="store_true", help="Rehacer los originales desde las láminas; sustituye sus ediciones.")
    main(parser.parse_args().extract_originals)
