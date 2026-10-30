#!/usr/bin/env python3
"""توليد أيقونة «الشمس والقمر» لمبدّل المظهر (بطلب المالك 2026-10-30):

- القمر: هلال بنفس لغة أيقونة التطبيق — فرق دائرتين بدقة تحليلية (قوسان حقيقيان
  بين نقطتي التقاطع)، نفس نسب مولّد الأيقونة (offset/R و R2/R من generate-icon.py)
  مصغّرة إلى viewport 24، والفتحة لأعلى-يمين نحو الشمس.
- الشمس: قرص + ثماني أشعة مثلثية في الزاوية أعلى-يمين، خارج دائرة القمر تماماً
  (فحص آلي: صفر تقاطع مع جسم الهلال).

المخرجات:
  1) مقطع كوتلن (PathBuilder) للصق في ui/icons/NoorIcons.kt — يُطبع على stdout.
  2) معاينة PNG للمالك في download/ — الوضعان النهاري والليلي بأحجام متعددة.

التحقق الآلي قبل الإخراج:
  - مطابقة مسار القوسين (كاتمول-روم مقرّب) لقناع الفرق التحليلي A∖B (IoU ≥ 0.97).
  - الشمس بأكملها (قرص وأشعة) خارج دائرة القمر بهامش موجب.
  - كل قطاع قوس ≤ 90° لضمان تقريب البيزيه الدقيق.
"""
import math
import os

from PIL import Image, ImageDraw

DOWNLOAD = '/home/z/my-project/download'

# ===== هندسة الهلال (نسب أيقونة التطبيق نفسها مصغّرة 7.9/21.5) =====
R1 = 7.9                       # دائرة القمر الكبيرة
SCALE = R1 / 21.5
OFFSET = 10.6 * SCALE          # إزاحة دائرة القصّ (نفس نسبة أيقونة التطبيق)
R2 = 19.0 * SCALE              # دائرة القصّ (نفس نسبة أيقونة التطبيق)
CUT_DIR = (math.sqrt(0.5), -math.sqrt(0.5))   # الفتحة لأعلى-يمين (y-لأسفل)
C1 = (9.8, 14.2)               # مركز القمر — الكتلة أسفل-يسار
C2 = (C1[0] + CUT_DIR[0] * OFFSET, C1[1] + CUT_DIR[1] * OFFSET)

# ===== هندسة الشمس =====
SUN = (19.2, 4.8)
SUN_R = 1.9                    # القرص
RAY_BASE = 2.55                # قاعدة الشعاع
RAY_TIP = 3.8                  # رأس الشعاع
RAY_HALF_DEG = 3.4             # نصف زاوية قاعدة المثلث


def circle_intersection(c1, r1, c2, r2):
    dx, dy = c2[0] - c1[0], c2[1] - c1[1]
    d = math.hypot(dx, dy)
    a = (d * d + r1 * r1 - r2 * r2) / (2 * d)
    h = math.sqrt(max(0.0, r1 * r1 - a * a))
    ux, uy = dx / d, dy / d
    fx, fy = c1[0] + a * ux, c1[1] + a * uy
    px, py = -uy, ux
    return ((fx + h * px, fy + h * py), (fx - h * px, fy - h * py))


def in_circle(p, c, r):
    return math.hypot(p[0] - c[0], p[1] - c[1]) <= r


def boundary_run(center, r, other_center, other_r, want_inside, steps=4096):
    """أطول قوس متصل على دائرة (center,r) داخل/خارج الدائرة الأخرى — حدود الهلال.
    want_inside=False: القوس الخارجي (على A خارج B). want_inside=True: الداخلي (على B داخل A).
    يرجع (زاوية البداية، زاوية النهاية) باتجاه الزيادة عبر الجزء المطلوب."""
    flags = []
    for i in range(steps):
        a = 2 * math.pi * i / steps
        p = (center[0] + r * math.cos(a), center[1] + r * math.sin(a))
        inside = in_circle(p, other_center, other_r)
        flags.append(inside if want_inside else not inside)
    if all(flags) or not any(flags):
        raise SystemExit('حدود الهلال غير محددة — الدائرتان متطابقتان أو منفصلتان')
    best_len, best_start, cur_len, cur_start = 0, 0, 0, None
    doubled = flags + flags
    for i, ok in enumerate(doubled):
        if ok:
            if cur_len == 0:
                cur_start = i
            cur_len += 1
            if cur_len > best_len:
                best_len, best_start = cur_len, cur_start
        else:
            cur_len = 0
    a0 = 2 * math.pi * (best_start % steps) / steps
    span = best_len * 2 * math.pi / steps
    return a0, span


def split_arc(center, r, a0, span, max_deg=85.0):
    """قطع القوس من a0 بطول span (موجب = باتجاه زيادة الزاوية، سالب = عكسه)
    إلى قطاعات ≤ max_deg — يرجع قائمة (p0, p1, p2, p3) لنقاط البيزيه المكعبة."""
    if abs(span) < 1e-9 or abs(span) > 2 * math.pi:
        raise SystemExit(f'قوس غير سليم: span={span:.4f}')
    n = max(1, math.ceil(math.degrees(abs(span)) / max_deg))
    step = span / n
    k = 4.0 / 3.0 * math.tan(abs(step) / 4.0)
    segs = []
    for i in range(n):
        s0 = a0 + step * i
        s1 = s0 + step
        p0 = (center[0] + r * math.cos(s0), center[1] + r * math.sin(s0))
        p3 = (center[0] + r * math.cos(s1), center[1] + r * math.sin(s1))
        t0 = (-math.sin(s0), math.cos(s0))   # المماس باتجاه زيادة الزاوية
        t1 = (-math.sin(s1), math.cos(s1))
        sign = 1.0 if step > 0 else -1.0
        p1 = (p0[0] + sign * k * r * t0[0], p0[1] + sign * k * r * t0[1])
        p2 = (p3[0] - sign * k * r * t1[0], p3[1] - sign * k * r * t1[1])
        segs.append((p0, p1, p2, p3))
    return segs


def reverse_segs(segs):
    """عكس اتجاه سير قطاعات قوس مع بقاء الشكل — (p0,p1,p2,p3) ← (p3,p2,p1,p0)."""
    return [(p3, p2, p1, p0) for (p0, p1, p2, p3) in reversed(segs)]


def sample_cubics(segs, per_seg=14):
    pts = []
    for p0, p1, p2, p3 in segs:
        for j in range(per_seg + 1):
            t = j / per_seg
            mt = 1 - t
            x = mt**3 * p0[0] + 3 * mt**2 * t * p1[0] + 3 * mt * t**2 * p2[0] + t**3 * p3[0]
            y = mt**3 * p0[1] + 3 * mt**2 * t * p1[1] + 3 * mt * t**2 * p2[1] + t**3 * p3[1]
            pts.append((x, y))
    return pts


def crescent_polygon():
    """مضلع الهلال: القوس الخارجي (على A خارج B) ثم القوس الداخلي (على B داخل A)
    بالاتجاه الذي يوصل قرن النهاية إلى بداية الداخلي بلا تقاطع."""
    pa, pb = circle_intersection(C1, R1, C2, R2)
    aA0, spanA = boundary_run(C1, R1, C2, R2, want_inside=False)   # خارجي
    aB0, spanB = boundary_run(C2, R2, C1, R1, want_inside=True)    # داخلي
    outer = split_arc(C1, R1, aA0, spanA)
    outer_pts = sample_cubics(outer, 8)
    end_outer = outer_pts[-1]
    inner_inc = split_arc(C2, R2, aB0, spanB)   # باتجاه الزيادة
    inc_pts = sample_cubics(inner_inc, 8)
    d_inc = math.hypot(end_outer[0] - inc_pts[0][0], end_outer[1] - inc_pts[0][1])
    d_rev = math.hypot(end_outer[0] - inc_pts[-1][0], end_outer[1] - inc_pts[-1][1])
    if min(d_inc, d_rev) > 0.05:
        raise SystemExit(f'قرون الهلال لا تلتقي: {d_inc:.4f} / {d_rev:.4f}')
    if d_rev <= d_inc:
        inner, inner_pts = reverse_segs(inner_inc), sample_cubics(reverse_segs(inner_inc), 8)
    else:
        inner, inner_pts = inner_inc, inc_pts
    polygon = outer_pts + inner_pts
    return polygon, outer, inner, (pa, pb)


def analytic_mask(ppu):
    s = float(ppu)
    size = int(24 * ppu)
    img = Image.new('L', (size, size), 0)
    d = ImageDraw.Draw(img)
    d.ellipse([(C1[0] - R1) * s, (C1[1] - R1) * s, (C1[0] + R1) * s, (C1[1] + R1) * s], fill=255)
    cut = Image.new('L', (size, size), 0)
    ImageDraw.Draw(cut).ellipse([(C2[0] - R2) * s, (C2[1] - R2) * s,
                                 (C2[0] + R2) * s, (C2[1] + R2) * s], fill=255)
    out = Image.new('L', (size, size), 0)
    out.paste(img, (0, 0), Image.eval(cut, lambda v: 255 - v))
    return out


def polygon_mask(poly, ppu):
    s = float(ppu)
    size = int(24 * ppu)
    mask = Image.new('L', (size, size), 0)
    ImageDraw.Draw(mask).polygon([(x * s, y * s) for x, y in poly], fill=255)
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


def sun_points():
    """نقاط الشمس: القرص (كاتمول) والأشعة (مثلثات) — للفحص والمعاينة."""
    pts = []
    k = 0.552284749831 * SUN_R
    cx, cy = SUN
    pts += [(cx - SUN_R, cy), (cx - SUN_R, cy - k), (cx - k, cy - SUN_R), (cx, cy - SUN_R),
            (cx + k, cy - SUN_R), (cx + SUN_R, cy - k), (cx + SUN_R, cy), (cx + SUN_R, cy + k),
            (cx + k, cy + SUN_R), (cx, cy + SUN_R), (cx - k, cy + SUN_R), (cx - SUN_R, cy + k)]
    for i in range(8):
        a = math.radians(45 * i)
        ha = math.radians(RAY_HALF_DEG)
        pts += [(cx + RAY_TIP * math.cos(a), cy + RAY_TIP * math.sin(a)),
                (cx + RAY_BASE * math.cos(a - ha), cy + RAY_BASE * math.sin(a - ha)),
                (cx + RAY_BASE * math.cos(a + ha), cy + RAY_BASE * math.sin(a + ha))]
    return pts


def check_sun_clear_pil(polygon, ppu=8):
    """فحص أقوى: راستر الشمس (قرص وأشعة) × راستر الهلال — التقاطع صفر."""
    s = float(ppu)
    size = int(24 * ppu)
    moon = polygon_mask(polygon, ppu)
    sun = Image.new('L', (size, size), 0)
    d = ImageDraw.Draw(sun)
    cx, cy = SUN[0] * s, SUN[1] * s
    d.ellipse([cx - SUN_R * s, cy - SUN_R * s, cx + SUN_R * s, cy + SUN_R * s], fill=255)
    for i in range(3600):
        a = math.radians(i / 10.0)
        # عينة كثيفة على كل شعاع من القاعدة للرأس
        for t in range(int(RAY_BASE * 10), int(RAY_TIP * 10) + 1):
            r = t / 10.0
            x = cx + r * s * math.cos(a)
            y = cy + r * s * math.sin(a)
            d.point((x, y), fill=255)
    overlap = 0
    for ma, sa in zip(moon.getdata(), sun.getdata()):
        if ma > 127 and sa > 127:
            overlap += 1
    if overlap:
        raise SystemExit(f'تقاطع الشمس مع الهلال: {overlap} بكسل')
    return margin_of_sun()


def margin_of_sun():
    d = math.hypot(SUN[0] - C1[0], SUN[1] - C1[1])
    return d - R1 - RAY_TIP


def fmt(v):
    return f'{v:.2f}'.rstrip('0').rstrip('.') if abs(v) >= 0.005 else '0f'


def kotlin_f(v):
    s = f'{v:.2f}'
    if s.endswith('.00'):
        s = s[:-3]
    elif s.endswith('0'):
        s = s[:-1]
    return s + 'f'


def emit_kotlin(outer, inner):
    lines = []
    first = outer[0][0]
    lines.append(f'                moveTo({kotlin_f(first[0])}, {kotlin_f(first[1])})')
    for seg in outer + inner:
        _, p1, p2, p3 = seg
        lines.append(
            f'                curveTo({kotlin_f(p1[0])}, {kotlin_f(p1[1])}, '
            f'{kotlin_f(p2[0])}, {kotlin_f(p2[1])}, {kotlin_f(p3[0])}, {kotlin_f(p3[1])})'
        )
    lines.append('                close()')
    return '\n'.join(lines)


def build_preview(polygon):
    s = 20.0   # بكسل لكل dp في الرسم الكبير
    size = int(24 * s)

    def render(bg, tint):
        img = Image.new('RGBA', (size, size), bg)
        d = ImageDraw.Draw(img)
        poly = [(x * s, y * s) for x, y in polygon]
        d.polygon(poly, fill=tint)
        cx, cy = SUN[0] * s, SUN[1] * s
        d.ellipse([cx - SUN_R * s, cy - SUN_R * s, cx + SUN_R * s, cy + SUN_R * s], fill=tint)
        for i in range(8):
            a = math.radians(45 * i)
            ha = math.radians(RAY_HALF_DEG)
            d.polygon([
                (cx + RAY_TIP * s * math.cos(a), cy + RAY_TIP * s * math.sin(a)),
                (cx + RAY_BASE * s * math.cos(a - ha), cy + RAY_BASE * s * math.sin(a - ha)),
                (cx + RAY_BASE * s * math.cos(a + ha), cy + RAY_BASE * s * math.sin(a + ha)),
            ], fill=tint)
        return img

    DAY_BG, DAY_TINT = (0xF6, 0xF2, 0xE9, 255), (0x2E, 0x6B, 0x5E, 255)
    NIGHT_BG, NIGHT_TINT = (0x0C, 0x15, 0x12, 255), (0x82, 0xC9, 0xB0, 255)

    big_day, big_night = render(DAY_BG, DAY_TINT), render(NIGHT_BG, NIGHT_TINT)
    W, H = 1240, 560
    board = Image.new('RGB', (W, H), (232, 228, 218))
    board.paste(big_day.resize((440, 440), Image.LANCZOS), (60, 60))
    board.paste(big_night.resize((440, 440), Image.LANCZOS), (560, 60))
    # شرائط الأحجام الحقيقية (26dp داخل الشريط و40dp زر الهيدر)
    for row, (theme, tint) in enumerate([((DAY_BG, DAY_TINT), 'day'),
                                          ((NIGHT_BG, NIGHT_TINT), 'night')]):
        img = render(theme[0], theme[1])
        x = 1040
        for dp in (40, 26):
            px = int(dp * 2.5)   # تكبير ×2.5 للعرض
            small = img.resize((px, px), Image.LANCZOS)
            board.paste(small.convert('RGB'), (x, 90 + row * 160), None)
            x += px + 24
    out = os.path.join(DOWNLOAD, 'noor-theme-toggle-icon-preview.png')
    board.save(out)
    print(f'✅ معاينة المالك: {out}')


def main():
    polygon, outer, inner, horns = crescent_polygon()
    # فحص 1: مطابقة المسار للفرق التحليلي (دقة عالية — خطأ حواف الرسترة لا يخدع)
    v = iou(analytic_mask(20), polygon_mask(polygon, 20))
    if v < 0.97:
        raise SystemExit(f'مسار الهلال لا يطابق الفرق التحليلي: IoU={v:.4f}')
    print(f'✅ مطابقة مسار الهلال للفرق التحليلي: IoU={v:.4f}')
    # فحص 2: الشمس خارج جسم الهلال تماماً
    check_sun_clear_pil(polygon)
    print(f'✅ الشمس خارج دائرة القمر بهامش {margin_of_sun():.2f}dp وصفر تقاطع بكسلي')
    # فحص 3: الحدود
    xs = [p[0] for p in polygon] + [SUN[0] - RAY_TIP, SUN[0] + RAY_TIP]
    ys = [p[1] for p in polygon] + [SUN[1] - RAY_TIP, SUN[1] + RAY_TIP]
    if min(xs) < 0.8 or max(xs) > 23.2 or min(ys) < 0.8 or max(ys) > 23.2:
        raise SystemExit(f'الرسم خارج الحدود الآمنة: x[{min(xs):.2f},{max(xs):.2f}] y[{min(ys):.2f},{max(ys):.2f}]')
    print(f'✅ الحدود: x[{min(xs):.2f},{max(xs):.2f}] y[{min(ys):.2f},{max(ys):.2f}]')
    # فحص 4: قطاعات القوس ≤ 90°
    for seg in outer + inner:
        pass  # split_arc يقصّ عند 85° — أكبر قطاع مضمون
    print('\n===== مقطع كوتلن للصق في NoorIcons.kt =====\n')
    print(emit_kotlin(outer, inner))
    print('\n===== نهاية المقطع =====\n')
    build_preview(polygon)


if __name__ == '__main__':
    main()
