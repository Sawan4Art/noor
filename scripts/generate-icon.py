#!/usr/bin/env python3
"""توليد أيقونة التطبيق PNG لكل المقاسات — طراز هندسي حديث بألوان هادئة
(بطلب المالك 2026-10): هلال كريمي + نجمة رباعية ذهبية هادئة + نقطة توازن
+ حلقة رفيعة، على أخضر مريمي متدرج. يرسم بدقة عالية ثم يصغّر لكل كثافة.

الهندسة تطابق الفيكتور drawable/ic_launcher_foreground.xml (viewport 108)
والخلفية تطابق drawable/bg_launcher.xml. الظاهر من أيقونة adaptive هو دائرة
قطرها 72 من أصل 108، لذا يُرسم الرمز هنا بمقياس size/72 ليطابق ظهوره
في الأيقونة التكيفية تمامًا.
"""
import math
import os

from PIL import Image, ImageDraw

BASE = os.path.join(os.path.dirname(__file__), '..', 'android', 'app', 'src', 'main', 'res')

S = 1024  # دقة الرسم الأساسية

# لوحة الألوان الهادئة
BG_CENTER = (58, 107, 96)         # 3A6B60
BG_EDGE = (43, 83, 74)            # 2B534A
CREAM = (240, 231, 210, 255)      # F0E7D2 — الهلال
GOLD_SOFT = (217, 186, 124, 255)  # D9BA7C — النجمة والنقطة
RING = (240, 231, 210, 43)        # F0E7D2 بشفافية 17% — الحلقة

# هندسة الرمز في viewport 108 (تطابق الفيكتور حرفيًا)
CRESCENT_C = (49.5, 58.5)
CRESCENT_R = 16.8
CUT_C = (56.5, 51.0)
CUT_R = 14.2
STAR_C = (67.0, 42.0)
STAR_LONG = 8.0
STAR_SHORT = 3.0
DOT_C = (43.0, 38.0)
DOT_R = 1.7
RING_C = (54.0, 54.0)
RING_R = 30.0
RING_W = 1.6


def radial_background(size: int) -> Image.Image:
    """خلفية متدرجة شعاعية هادئة: مركز أعلى قليلاً (0.5, 0.45) مثل الفيكتور."""
    img = Image.new('RGB', (size, size))
    px = img.load()
    max_d = math.hypot(size / 2, size / 2)
    cy = size * 0.45
    for y in range(size):
        for x in range(size):
            d = min(1.0, math.hypot(x - size / 2, y - cy) / max_d)
            px[x, y] = tuple(
                int(BG_CENTER[i] + (BG_EDGE[i] - BG_CENTER[i]) * d) for i in range(3)
            )
    return img


def symbol_layer(size: int) -> Image.Image:
    """الرمز على شفاف: هلال + نجمة + نقطة + حلقة.

    إحداثيات viewport 108 تُحوَّل للبكسل بالعلاقة:
    pixel = (v - 18) × size/72 — لأن المنطقة الظاهرة من أيقونة adaptive
    هي المركز 72 من الـ 108 (من 18 إلى 90)، فتُسقط على المربع كاملًا."""
    s = size / 72.0

    def px(v: float) -> float:
        return (v - 18.0) * s

    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # 1) الهلال: دائرة كاملة ثم قص ألفا بدائرة مزاحة (الفتحة أعلى-يمين)
    cr, cx, cy = CRESCENT_R * s, px(CRESCENT_C[0]), px(CRESCENT_C[1])
    draw.ellipse([cx - cr, cy - cr, cx + cr, cy + cr], fill=CREAM)
    ur, ux, uy = CUT_R * s, px(CUT_C[0]), px(CUT_C[1])
    cut = Image.new('L', (size, size), 0)
    ImageDraw.Draw(cut).ellipse([ux - ur, uy - ur, ux + ur, uy + ur], fill=255)
    alpha = img.split()[3]
    alpha.paste(0, (0, 0), cut)
    img.putalpha(alpha)
    draw = ImageDraw.Draw(img)

    # 2) النجمة الرباعية (في فتحة الهلال — أعلى-يمين)
    pts = []
    for i in range(8):
        ang = math.radians(i * 45 - 90)
        r = (STAR_LONG if i % 2 == 0 else STAR_SHORT) * s
        pts.append((px(STAR_C[0]) + r * math.cos(ang), px(STAR_C[1]) + r * math.sin(ang)))
    draw.polygon(pts, fill=GOLD_SOFT)

    # 3) نقطة التوازن (أعلى-يسار)
    dr, dx, dy = DOT_R * s, px(DOT_C[0]), px(DOT_C[1])
    draw.ellipse([dx - dr, dy - dr, dx + dr, dy + dr], fill=GOLD_SOFT)

    # 4) الحلقة الهندسية الرفيعة (آخرًا — لا تتقاطع مع أي عنصر)
    rr, rx, ry = RING_R * s, px(RING_C[0]), px(RING_C[1])
    w = max(1, round(RING_W * s))
    draw.ellipse([rx - rr, ry - rr, rx + rr, ry + rr], outline=RING, width=w)

    return img


def build():
    # 1) التكوين الكامل بدقة عالية
    canvas = radial_background(S).convert('RGBA')
    canvas.alpha_composite(symbol_layer(S))

    # 2) التصدير لكل كثافة: مربعة + دائرية بقناع مُنعّم
    densities = {
        'mipmap-mdpi': 48,
        'mipmap-hdpi': 72,
        'mipmap-xhdpi': 96,
        'mipmap-xxhdpi': 144,
        'mipmap-xxxhdpi': 192,
    }
    for folder, size_px in densities.items():
        out_dir = os.path.join(BASE, folder)
        os.makedirs(out_dir, exist_ok=True)
        img = canvas.resize((size_px, size_px), Image.LANCZOS)
        img.save(os.path.join(out_dir, 'ic_launcher.png'))
        # دائرية
        mask = Image.new('L', (size_px * 4, size_px * 4), 0)
        ImageDraw.Draw(mask).ellipse((0, 0, size_px * 4 - 1, size_px * 4 - 1), fill=255)
        mask = mask.resize((size_px, size_px), Image.LANCZOS)
        round_img = img.copy()
        round_img.putalpha(mask)
        round_img.save(os.path.join(out_dir, 'ic_launcher_round.png'))
        print(f'✅ {folder}: {size_px}px (مربعة + دائرية)')

    # ملاحظة: مقدمة الـ adaptive على API 26+ هي الفيكتور
    # drawable/ic_launcher_foreground.xml — لا PNG مطلوب.


if __name__ == '__main__':
    build()
