#!/usr/bin/env python3
"""توليد أيقونة نور المسلم — هلال هندسي فقط (بطلب المالك 2026-10-30).

الهلال = فرق دائرتين بدقة تحليلية: دائرة كبيرة + دائرة قصّ مزاحة نحو أعلى-يمين
(نظام y-لأسفل)، فالفتحة تفتح لأعلى-يمين. بلا نجمة ولا نقطة ولا حلقة — «هلال فقط».

المخرجات:
  1) drawable/ic_launcher_foreground.xml — فيكتور بنقاط تقاطع محسوبة تحليلياً.
     فرق الدائرتين لا يُمثَّل بـ evenOdd (دائرة القصّ تخرج عن الكبيرة — حتىOdd
     يملأ جزء القصّ الخارج فتشوّه القرون)، لذا نحسب نقطتي التقاطع ونبني قوسين
     حقيقيين، ونعتمد أعلام الأقواس بفحص IoU آلي ضد راستر المرجع.
  2) PNG لكل الكثافات (مربعة + دائرية) — legacy API 24−.
  3) لوحة معاينة للمالك في download/.

الهندسة على خلفية bg_launcher.xml (أخضر مريمي شعاعي) كما هي، ولون خلفية
السبلاش noor_launcher_bg كما هو. الظاهر من adaptive هو المركز 72 من 108.
"""
import math
import os

from PIL import Image, ImageDraw

BASE = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'res')
DOWNLOAD = '/home/z/my-project/download'

S = 1024  # دقة الرسم الأساسية

# لوحة الألوان الهادئة نفسها
BG_CENTER = (58, 107, 96)         # 3A6B60
BG_EDGE = (43, 83, 74)            # 2B534A
CREAM = (240, 231, 210, 255)      # F0E7D2 — الهلال وحده

# هندسة الهلال في viewport 108
C1 = (54.9, 53.1)   # إزاحة بصرية طفيفة نحو الفتحة لموازنة كتلة الجسم
R1 = 21.5
CUT_DIR = (math.sqrt(0.5), -math.sqrt(0.5))  # نحو أعلى-يمين (نظام y-لأسفل)
OFFSET = 10.6
C2 = (C1[0] + CUT_DIR[0] * OFFSET, C1[1] + CUT_DIR[1] * OFFSET)
R2 = 19.0


def circle_intersection(c1, r1, c2, r2):
    """نقطتا تقاطع دائرتين — تحليلياً."""
    dx, dy = c2[0] - c1[0], c2[1] - c1[1]
    d = math.hypot(dx, dy)
    a = (d * d + r1 * r1 - r2 * r2) / (2 * d)
    h = math.sqrt(max(0.0, r1 * r1 - a * a))
    ux, uy = dx / d, dy / d
    fx, fy = c1[0] + a * ux, c1[1] + a * uy
    px, py = -uy, ux
    return ((fx + h * px, fy + h * py), (fx - h * px, fy - h * py))


def svg_arc_samples(p0, p1, r, large_arc, sweep, steps=64):
    """تخريد قوس SVG/VectorDrawable (A r r 0 large sweep) — صيغة W3C القياسية."""
    x1, y1 = p0
    x2, y2 = p1
    dx, dy = (x2 - x1) / 2.0, (y2 - y1) / 2.0
    q = math.hypot(dx, dy)
    if q == 0:
        return [p0, p1]
    # المركز (صيغة W3C F.6.5، بمحاذاة نصف الوتر لدينا): c = المنتصف − σ·√(r²−q²)·(y1',−x1')/q
    # أي c = المنتصف + h·(dy,−dx) حيث h=−σ·√(r²−q²)/q و σ=+1 إذا large≠sweep
    csign = -1.0 if large_arc != sweep else 1.0
    h = math.sqrt(max(0.0, r * r - q * q)) / q * csign
    cx = x1 + dx + h * dy
    cy = y1 + dy - h * dx
    a0 = math.atan2(y1 - cy, x1 - cx)
    a1 = math.atan2(y2 - cy, x2 - cx)
    da = a1 - a0
    if sweep == 0 and da > 0:
        da -= 2 * math.pi
    elif sweep == 1 and da < 0:
        da += 2 * math.pi
    return [(cx + r * math.cos(a0 + da * s / steps),
             cy + r * math.sin(a0 + da * s / steps)) for s in range(steps + 1)]


def crescent_difference_mask(viewport, ppu):
    """قناع الهلال بطريقة قصّ الألفا — المرجع."""
    size = int(viewport * ppu)
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    s = float(ppu)
    d.ellipse([(C1[0] - R1) * s, (C1[1] - R1) * s, (C1[0] + R1) * s, (C1[1] + R1) * s],
              fill=(255, 255, 255, 255))  # عدد صحيح يُفسَّر كحزمة بايتات — ألفا صفر!
    cut = Image.new('L', (size, size), 0)
    ImageDraw.Draw(cut).ellipse([(C2[0] - R2) * s, (C2[1] - R2) * s,
                                 (C2[0] + R2) * s, (C2[1] + R2) * s], fill=255)
    alpha = img.split()[3]
    alpha.paste(0, (0, 0), cut)
    return alpha


def crescent_vector_mask(viewport, ppu, large1, sweep1, large2, sweep2):
    """قناع الهلال من مسار القوسين — لاختبار المطابقة مع الفيكتور."""
    pa, pb = circle_intersection(C1, R1, C2, R2)
    s = float(ppu)
    pts = svg_arc_samples((pb[0] * s, pb[1] * s), (pa[0] * s, pa[1] * s), R1 * s, large1, sweep1)
    pts += svg_arc_samples((pa[0] * s, pa[1] * s), (pb[0] * s, pb[1] * s), R2 * s, large2, sweep2)
    size = int(viewport * ppu)
    mask = Image.new('L', (size, size), 0)
    ImageDraw.Draw(mask).polygon(pts, fill=255)
    return mask


def iou(a, b):
    inter = union = 0
    for xa, xb in zip(a.getdata(), b.getdata()):
        ca, cb = xa > 127, xb > 127
        if ca and cb:
            inter += 1
        if ca or cb:
            union += 1
    return inter / union if union else 0.0


def build_vector_path():
    """يحسب نقطتي التقاطع ويثبّت أعلام الأقواس بأعلى IoU ثم يرجع نص pathData."""
    pa, pb = circle_intersection(C1, R1, C2, R2)
    m_ref = crescent_difference_mask(108, 4)
    best = None
    all_scores = []
    for large1 in (0, 1):
        for sweep1 in (0, 1):
            for large2 in (0, 1):
                for sweep2 in (0, 1):
                    m_vec = crescent_vector_mask(108, 4, large1, sweep1, large2, sweep2)
                    v = iou(m_ref, m_vec)
                    all_scores.append(v)
                    if best is None or v > best[0]:
                        best = (v, large1, sweep1, large2, sweep2)
    v, large1, sweep1, large2, sweep2 = best
    # عرض تخريد القوس يفرق قليلاً على الحافة الدقيقة — العتبة 0.90 مع تفرد واضح
    runner = max(r for r in all_scores if r != v) if len(all_scores) > 1 else 0.0
    if v < 0.90 or v - runner < 0.02:
        raise SystemExit(f'فشل تثبيت أعلام الأقواس: IoU={v:.4f} والوصيف={runner:.4f}')
    print(f'✅ أعلام الأقواس المثبتة: القوس الكبير (large={large1}, sweep={sweep1}) '
          f'وقوس القصّ (large={large2}, sweep={sweep2}) — IoU={v:.4f} (الوصيف {runner:.4f})')
    fmt = lambda p: f'{p[0]:.2f},{p[1]:.2f}'
    return (f'M {fmt(pb)} '
            f'A {R1:.2f},{R1:.2f} 0 {large1},{sweep1} {fmt(pa)} '
            f'A {R2:.2f},{R2:.2f} 0 {large2},{sweep2} {fmt(pb)} Z')


def write_foreground_xml(path_data):
    xml = f'''<?xml version="1.0" encoding="utf-8"?>
<!--
  مقدمة أيقونة نور المسلم — هلال هندسي فقط (بطلب المالك 2026-10-30):
  فرق دائرتين بدقة تحليلية (قوسان حقيقيان بين نقطتي التقاطع)،
  فتحة الهلال لأعلى-يمين، بلا أي عناصر أخرى.
  المولّد: scripts/generate-icon.py — لا تُحرَّر يدوياً.
  كل الهندسة داخل المنطقة الآمنة (r33 حول 54,54) لأقنعة الـ adaptive،
  ونفس المسار يُغذي أيقونة السبلاش على Android 12+.
-->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">

    <path
        android:fillColor="#F0E7D2"
        android:pathData="{path_data}" />
</vector>
'''
    out = os.path.join(BASE, 'drawable', 'ic_launcher_foreground.xml')
    with open(out, 'w', encoding='utf-8') as f:
        f.write(xml)
    print(f'✅ {out}')


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
    """الهلال وحده على شفاف — إحداثيات viewport تُسقط بـ (v−18)×size/72."""
    s = size / 72.0
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    # الدائرة الكاملة ثم قصّ الألفا بدائرة القصّ (الفتحة أعلى-يمين)
    cr = R1 * s
    cx, cy = (C1[0] - 18.0) * s, (C1[1] - 18.0) * s
    draw.ellipse([cx - cr, cy - cr, cx + cr, cy + cr], fill=CREAM)
    ur = R2 * s
    ux, uy = (C2[0] - 18.0) * s, (C2[1] - 18.0) * s
    cut = Image.new('L', (size, size), 0)
    ImageDraw.Draw(cut).ellipse([ux - ur, uy - ur, ux + ur, uy + ur], fill=255)
    alpha = img.split()[3]
    alpha.paste(0, (0, 0), cut)
    img.putalpha(alpha)
    return img


def build_pngs():
    canvas = radial_background(S).convert('RGBA')
    canvas.alpha_composite(symbol_layer(S))

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
        mask = Image.new('L', (size_px * 4, size_px * 4), 0)
        ImageDraw.Draw(mask).ellipse((0, 0, size_px * 4 - 1, size_px * 4 - 1), fill=255)
        mask = mask.resize((size_px, size_px), Image.LANCZOS)
        round_img = img.copy()
        round_img.putalpha(mask)
        round_img.save(os.path.join(out_dir, 'ic_launcher_round.png'))
        print(f'✅ {folder}: {size_px}px (مربعة + دائرية)')


def build_preview():
    """لوحة معاينة للمالك: 512 مربعة + دائرية + شريط مقاسات صغيرة."""
    canvas = radial_background(512).convert('RGBA')
    canvas.alpha_composite(symbol_layer(512))
    mask = Image.new('L', (2048, 2048), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, 2047, 2047), fill=255)
    mask = mask.resize((512, 512), Image.LANCZOS)
    round512 = canvas.copy()
    round512.putalpha(mask)

    backdrop = Image.new('RGB', (1180, 660), (232, 228, 218))
    backdrop.paste(canvas, (60, 64))
    backdrop.paste(round512, (640, 64), round512)
    x = 640
    for px in (192, 96, 48):
        small = canvas.resize((px, px), Image.LANCZOS)
        backdrop.paste(small, (x, 640 - px), small)
        x += px + 28
    out = os.path.join(DOWNLOAD, 'noor-icon-alpha4-preview.png')
    backdrop.save(out)
    print(f'✅ {out}')


def main():
    os.makedirs(DOWNLOAD, exist_ok=True)
    path_data = build_vector_path()
    write_foreground_xml(path_data)
    build_pngs()
    build_preview()


if __name__ == '__main__':
    main()
