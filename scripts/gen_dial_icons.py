#!/usr/bin/env python3
"""توليد أيقونتَي الشمس والقمر المنفصلتين لمزلاج المظهر (بطلب المالك 2026-10-31):

المزلاج السائل يحتاج أيقونتين مستقلتين تتقاطفان بالشفافية أثناء السحب:
- القمر: هلال بنفس هندسة أيقونة التطبيق حرفياً (فرق دائرتين بدقة تحليلية،
  نفس نسب generate-icon.py) لكن ممركزاً في viewport 24 كاملاً.
- الشمس: قرص مركزي + ثماني أشعة مثلثية، الأولى تشرق نحو اليمين (0° = شرق).

المخرجات:
  1) مقطع كوتلن (PathBuilder) للصق في ui/icons/NoorIcons.kt — يُطبع على stdout.
  2) معاينة PNG للمالك في download/ — الوضعان الليلي والنهاري بأحجام متعددة.

التحقق الآلي قبل الإخراج:
  - مطابقة مسار البيزيه لقناع الفرق التحليلي A∖B (IoU ≥ 0.97).
  - كل هندسة الشمس داخل viewport بهامش موجب.
  - كل قطاع قوس ≤ 90° لضمان تقريب البيزيه الدقيق.
"""
import math
import os

from PIL import Image, ImageDraw

DOWNLOAD = '/home/z/my-project/download'

# ===== هندسة الهلال (نسب أيقونة التطبيق نفسها) =====
R1 = 7.9
OFFSET = 10.6 * (R1 / 21.5)
R2 = 19.0 * (R1 / 21.5)
CUT_DIR = (math.sqrt(0.5), -math.sqrt(0.5))
C1 = (9.8, 14.2)
C2 = (C1[0] + CUT_DIR[0] * OFFSET, C1[1] + CUT_DIR[1] * OFFSET)

# ===== هندسة الشمس المستقلة (ممركزة) =====
SUN = (12.0, 12.0)
SUN_R = 4.5
RAY_BASE = 6.1
RAY_TIP = 9.2
RAY_HALF_DEG = 6.6


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
    flags = []
    for i in range(steps):
        a = 2 * math.pi * i / steps
        p = (center[0] + r * math.cos(a), center[1] + r * math.sin(a))
        inside = in_circle(p, other_center, other_r)
        flags.append(inside if want_inside else not inside)
    if all(flags) or not any(flags):
        raise SystemExit('حدود الهلال غير محددة')
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
        t0 = (-math.sin(s0), math.cos(s0))
        t1 = (-math.sin(s1), math.cos(s1))
        sign = 1.0 if step > 0 else -1.0
        p1 = (p0[0] + sign * k * r * t0[0], p0[1] + sign * k * r * t0[1])
        p2 = (p3[0] - sign * k * r * t1[0], p3[1] - sign * k * r * t1[1])
        segs.append((p0, p1, p2, p3))
    return segs


def reverse_segs(segs):
    return [(p3, p2, p1, p0) for (p0, p1, p2, p3) in reversed(segs)]


def sample_cubics(segs, per_seg=14):
    pts = []
    for p0, p1, p2, p3 in segs:
        for j in range(per_seg + 1):
            t = j / per_seg
            mt = 1 - t
            x = mt**3 * p0[0] + 3 * mt**2 * t * p1[0] + 3 * mt * t**2 * p2[0] + t**3 * p3[0]
            y = mt**3 * p0[0] * 0 + 0  # placeholder يُستبدل أدناه
            y = mt**3 * p0[1] + 3 * mt**2 * t * p1[1] + 3 * mt * t**2 * p2[1] + t**3 * p3[1]
            pts.append((x, y))
    return pts


def crescent(c1, c2):
    """قطاعات بيزيه للهلال حول مركزين معينين (لإعادة التوليد بعد التمركز)."""
    pa, pb = circle_intersection(c1, R1, c2, R2)
    aA0, spanA = boundary_run(c1, R1, c2, R2, want_inside=False)
    aB0, spanB = boundary_run(c2, R2, c1, R1, want_inside=True)
    outer = split_arc(c1, R1, aA0, spanA)
    outer_pts = sample_cubics(outer, 10)
    end_outer = outer_pts[-1]
    inner_inc = split_arc(c2, R2, aB0, spanB)
    inc_pts = sample_cubics(inner_inc, 10)
    d_inc = math.hypot(end_outer[0] - inc_pts[0][0], end_outer[1] - inc_pts[0][1])
    d_rev = math.hypot(end_outer[0] - inc_pts[-1][0], end_outer[1] - inc_pts[-1][1])
    if min(d_inc, d_rev) > 0.05:
        raise SystemExit(f'قرون الهلال لا تلتقي: {d_inc:.4f} / {d_rev:.4f}')
    if d_rev <= d_inc:
        inner = reverse_segs(inner_inc)
    else:
        inner = inner_inc
    return outer, inner, (pa, pb)


def analytic_mask(ppu, c1, r1, c2, r2):
    s = float(ppu)
    size = int(24 * ppu)
    img = Image.new('L', (size, size), 0)
    ImageDraw.Draw(img).ellipse([(c1[0] - r1) * s, (c1[1] - r1) * s,
                                 (c1[0] + r1) * s, (c1[1] + r1) * s], fill=255)
    cut = Image.new('L', (size, size), 0)
    ImageDraw.Draw(cut).ellipse([(c2[0] - r2) * s, (c2[1] - r2) * s,
                                 (c2[0] + r2) * s, (c2[1] + r2) * s], fill=255)
    out = Image.new('L', (size, size), 0)
    out.paste(img, (0, 0), Image.eval(cut, lambda v: 255 - v))
    return out


def path_polygon(outer, inner, ppu=8):
    """راستر مسار البيزيه الفعلي (كما سيرسمه كوتلن) لفحص IoU."""
    pts = []
    for segs in (outer, inner):
        for p0, p1, p2, p3 in segs:
            pts.append(p0)
            for j in range(1, 15):
                t = j / 14.0
                mt = 1 - t
                x = mt**3 * p0[0] + 3 * mt**2 * t * p1[0] + 3 * mt * t**2 * p2[0] + t**3 * p3[0]
                y = mt**3 * p0[1] + 3 * mt**2 * t * p1[1] + 3 * mt * t**2 * p2[1] + t**3 * p3[1]
                pts.append((x, y))
    s = float(ppu)
    size = int(24 * ppu)
    mask = Image.new('L', (size, size), 0)
    ImageDraw.Draw(mask).polygon([(x * s, y * s) for x, y in pts], fill=255)
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


def fmt(v):
    s = f'{v:.2f}'.rstrip('0').rstrip('.')
    return s if s else '0'


def kotlin_crescent(outer, inner):
    """مقطع PathBuilder للهلال الممركز في 24."""
    segs_all = [(p0, p1, p2, p3) for (p0, p1, p2, p3) in outer + inner]
    xs = [p[0] for seg in (outer + inner) for p in (seg[0], seg[3])]
    ys = [p[1] for seg in (outer + inner) for p in (seg[0], seg[3])]
    # تمركز: مركز الصندوق المحيط إلى (12,12)
    cx = (min(xs) + max(xs)) / 2
    cy = (min(ys) + max(ys)) / 2
    dx, dy = 12.0 - cx, 12.0 - cy
    lines = []
    first = segs_all[0][0]
    lines.append(f'                moveTo({fmt(first[0] + dx)}f, {fmt(first[1] + dy)}f)')
    for (p0, p1, p2, p3) in segs_all:
        lines.append(
            f'                curveTo({fmt(p1[0] + dx)}f, {fmt(p1[1] + dy)}f, '
            f'{fmt(p2[0] + dx)}f, {fmt(p2[1] + dy)}f, '
            f'{fmt(p3[0] + dx)}f, {fmt(p3[1] + dy)}f)'
        )
    lines.append('                close()')
    return '\n'.join(lines), dx, dy


def kotlin_sun():
    k = 0.552284749831 * SUN_R
    cx, cy = SUN
    L = []
    L.append(f'                // قرص الشمس الممركز')
    L.append(f'                circle({fmt(cx)}f, {fmt(cy)}f, {fmt(SUN_R)}f)')
    L.append(f'                // ثماني أشعة مثلثية — الأولى نحو اليمين (شرق)')
    for i in range(8):
        a = 45.0 * i
        ar = math.radians(a)
        har = math.radians(RAY_HALF_DEG)
        tip = (cx + RAY_TIP * math.cos(ar), cy + RAY_TIP * math.sin(ar))
        b1 = (cx + RAY_BASE * math.cos(ar + har), cy + RAY_BASE * math.sin(ar + har))
        b2 = (cx + RAY_BASE * math.cos(ar - har), cy + RAY_BASE * math.sin(ar - har))
        L.append(
            f'                rayAt({fmt(tip[0])}f, {fmt(tip[1])}f, '
            f'{fmt(b1[0])}f, {fmt(b1[1])}f, {fmt(b2[0])}f, {fmt(b2[1])}f)'
        )
    return '\n'.join(L)


def main():
    outer, inner, horns = crescent(C1, C2)

    # مركز الإزاحة من الصندوق المحيط لنقاط المسار ثم إعادة توليد بالمراكز المزاحة
    xs = [p[0] for seg in (outer + inner) for p in (seg[0], seg[3])]
    ys = [p[1] for seg in (outer + inner) for p in (seg[0], seg[3])]
    dx = 12.0 - (min(xs) + max(xs)) / 2
    dy = 12.0 - (min(ys) + max(ys)) / 2
    c1s = (C1[0] + dx, C1[1] + dy)
    c2s = (C2[0] + dx, C2[1] + dy)
    outer_s, inner_s, horns_s = crescent(c1s, c2s)

    # فحص 1: مطابقة مسار البيزيه الممركز للقناع التحليلي الممركز
    poly = path_polygon(outer_s, inner_s, ppu=20)
    ref = analytic_mask(20, c1s, R1, c2s, R2)
    score = iou(ref, poly)
    if score < 0.97:
        raise SystemExit(f'IoU الهلال {score:.4f} < 0.97')

    # فحص 2: الشمس داخل الشاشة بهامش
    for name, (px, py) in {
        'tip0': (SUN[0] + RAY_TIP, SUN[1]),
        'tip45': (SUN[0] + RAY_TIP * math.cos(math.radians(45)), SUN[1] - RAY_TIP * math.sin(math.radians(45))),
        'tip90': (SUN[0], SUN[1] - RAY_TIP),
        'disc': (SUN[0] - SUN_R, SUN[1] - SUN_R),
    }.items():
        if not (0.4 <= px <= 23.6 and 0.4 <= py <= 23.6):
            raise SystemExit(f'نقطة شمس خارج الشاشة: {name} {px:.2f},{py:.2f}')

    kmoon, dx, dy = kotlin_crescent(outer_s, inner_s)
    ksun = kotlin_sun()

    print('// ===== مولّد آلي: scripts/gen_dial_icons.py — لا تُحرَّر يدوياً =====')
    print('// المزلاج السائل للمظهر (2026-10-31): قمر مركزي + شمس مركزية تتقاطفان بالسحب')
    print(f'// فحص IoU الهلال: {score:.4f}')
    print()
    print('// في NoorIcons.kt — أيقونة القمر (هلال أيقونة التطبيق ممركزاً):')
    print('    val Moon: ImageVector by lazy {')
    print('        noorIcon("Moon") {')
    print('            filled {')
    print(kmoon)
    print('            }')
    print('        }')
    print('    }')
    print()
    print('// أيقونة الشمس (قرص + أشعة، الأولى نحو الشرق):')
    print('    val Sun: ImageVector by lazy {')
    print('        noorIcon("Sun") {')
    print('            filled {')
    print(ksun)
    print('            }')
    print('        }')
    print('    }')
    print()
    print('// الدوال المساعدة المطلوبة في noorIcon helpers:')
    print('// rayAt(tipX,tipY,b1X,b1Y,b2X,b2Y): مثلث شعاع بنقاط جاهزة')
    print(f'// ثوابت الشمس المستقلة: SUN_R={SUN_R} RAY_BASE={RAY_BASE} RAY_TIP={RAY_TIP} RAY_HALF_DEG={RAY_HALF_DEG}')

    # معاينة PNG للمالك: ليلي ونهاري بأحجام متعددة
    sizes = [96, 64, 48, 32]
    cols = [
        ('ليلي', (18, 24, 22), (185, 206, 196)),
        ('نهاري', (245, 240, 230), (58, 84, 74)),
    ]
    cell = max(sizes) + 28
    img = Image.new('RGB', (cell * 2, cell * 2 + 60), (255, 255, 255))
    d = ImageDraw.Draw(img)
    for ci, (label, bg, fg) in enumerate(cols):
        for si, size in enumerate(sizes):
            x0 = ci * cell + (cell - size) // 2
            y0 = 30 + si * (size + 24)
            d.rectangle([x0 - 8, y0 - 8, x0 + size + 8, y0 + size + 8], fill=bg)
            sc = size / 24.0
            # قمر
            moon_mask = path_polygon(outer_s, inner_s, ppu=sc * 2).resize((size, size), Image.LANCZOS)
            solid = Image.new('RGB', (size, size), fg)
            img.paste(solid, (x0, y0), moon_mask)
            # شمس
            sun = Image.new('RGBA', (size * 4, size * 4), (0, 0, 0, 0))
            ds = ImageDraw.Draw(sun)
            ss = size * 4 / 24.0
            ds.ellipse([(SUN[0] - SUN_R) * ss, (SUN[1] - SUN_R) * ss,
                        (SUN[0] + SUN_R) * ss, (SUN[1] + SUN_R) * ss], fill=fg)
            for i in range(8):
                a = math.radians(45 * i)
                har = math.radians(RAY_HALF_DEG)
                tip = (SUN[0] + RAY_TIP * math.cos(a), SUN[1] + RAY_TIP * math.sin(a))
                b1 = (SUN[0] + RAY_BASE * math.cos(a + har), SUN[1] + RAY_BASE * math.sin(a + har))
                b2 = (SUN[0] + RAY_BASE * math.cos(a - har), SUN[1] + RAY_BASE * math.sin(a - har))
                ds.polygon([(tip[0] * ss, tip[1] * ss), (b1[0] * ss, b1[1] * ss),
                            (b2[0] * ss, b2[1] * ss)], fill=fg)
            sun = sun.resize((size, size), Image.LANCZOS)
            x1 = ci * cell + (cell - size) // 2 + size + 20
            if x1 + size < img.width:
                d.rectangle([x1 - 8, y0 - 8, x1 + size + 8, y0 + size + 8], fill=bg)
                img.paste(sun, (x1, y0), sun)
    d.text((10, 6), 'noor theme dial - moon / sun (light+dark)', fill=(0, 0, 0))
    os.makedirs(DOWNLOAD, exist_ok=True)
    out = os.path.join(DOWNLOAD, 'noor_dial_icons_preview.png')
    img.save(out)
    print(f'// معاينة المالك: {out}')


if __name__ == '__main__':
    main()
