#!/usr/bin/env python3
"""توليد أيقونة التطبيق PNG لكل المقاسات (الهلال والنجمة على أخضر متدرج) —
بدل الأيقونة الافتراضية من كاباسيتور. ترسم دقة عالية ثم تصغّر لكل كثافة.
"""
import math
import os

from PIL import Image, ImageDraw

BASE = os.path.join(os.path.dirname(__file__), '..', 'android', 'app', 'src', 'main', 'res')

# مقاسات القاعدة عالية الدقة
S = 1024
CX = CY = S // 2


def radial_background(size: int) -> Image.Image:
    """خلفية متدرجة شعاعية: #0e7a5a في الوسط → #052e22 في الأطراف."""
    img = Image.new('RGB', (size, size))
    px = img.load()
    c_center = (14, 122, 90)   # 0e7a5a
    c_edge = (5, 46, 34)       # 052e22
    max_d = math.hypot(size / 2, size / 2)
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - size / 2, (y - size * 0.42) / 1.0) / max_d
            # مركز التدرج أعلى قليلاً
            d = min(1.0, d * 1.05)
            r = int(c_center[0] + (c_edge[0] - c_center[0]) * d)
            g = int(c_center[1] + (c_edge[1] - c_center[1]) * d)
            b = int(c_center[2] + (c_edge[2] - c_center[2]) * d)
            px[x, y] = (r, g, b)
    return img


def draw_crescent_star(size: int) -> Image.Image:
    """هلال ذهبي + نجمة رباعية + نجمة صغيرة.
    الهلال: دائرة كاملة تُقص بدائرة مزاحة يمين-أعلى (قص ألفا = شكل مضمون)."""
    import math as m

    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    gold = (245, 196, 81, 255)
    gold_deep = (232, 181, 58, 255)

    # الدائرة الخارجية (ذهبية بالكامل)
    r_outer = int(size * 0.30)
    draw.ellipse(
        [CX - r_outer, CY - r_outer, CX + r_outer, CY + r_outer],
        fill=gold,
    )

    # قص دائرة مزاحة يمين-أعلى → يبقى الهلال على الشمال-تحت
    cut_cx = CX + size * 0.115
    cut_cy = CY - size * 0.075
    r_cut = int(size * 0.265)
    cut = Image.new('L', (size, size), 0)
    ImageDraw.Draw(cut).ellipse(
        [cut_cx - r_cut, cut_cy - r_cut, cut_cx + r_cut, cut_cy + r_cut],
        fill=255,
    )
    alpha = img.split()[3]
    alpha.paste(0, (0, 0), cut)
    img.putalpha(alpha)

    # النجمة الرباعية (يمين-أعلى — في فتحة الهلال)
    def four_star(cx, cy, r_long, r_short, fill):
        pts = []
        for i in range(8):
            ang = m.radians(i * 45 - 90)
            r = r_long if i % 2 == 0 else r_short
            pts.append((cx + r * m.cos(ang), cy + r * m.sin(ang)))
        draw.polygon(pts, fill=fill)

    four_star(CX + size * 0.165, CY - size * 0.155, size * 0.105, size * 0.038, gold)
    four_star(CX - size * 0.135, CY + size * 0.20, size * 0.045, size * 0.017, gold_deep)

    return img


def build():
    # 1) الخلفية + الرمز في دقة عالية
    bg = radial_background(S)
    fg = draw_crescent_star(S)
    # الرمز داخل المنطقة الآمنة: نصغّره لـ 60% من المساحة
    icon_size = int(S * 0.68)
    fg_small = fg.resize((icon_size, icon_size), Image.LANCZOS)
    canvas = bg.copy()
    offset = (S - icon_size) // 2
    canvas.paste(fg_small, (offset, offset), fg_small)

    # 2) التصدير لكل كثافة
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
        # مربعة
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
