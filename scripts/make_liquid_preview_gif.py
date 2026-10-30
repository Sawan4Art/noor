#!/usr/bin/env python3
"""معاينة GIF للنقل السائل المتصل — مرآة حرفية لمعادلات GooeyMath.kt و LiquidNavBar.kt.

قاعدة المالك (2026-10-30، دفعة alpha4): نزود اللزوجة — ترددات مكانية وزمنية أقل
(موجات أطول وأبطأ = سائل ألزق) وسعات أوضح، وإبطاء النقل نفسه (DRAIN/HEAD/JIGGLE).

تُغيَّر الثوابت هنا **وبنفس القيم حرفياً** في:
  - app/src/main/kotlin/com/sawan4art/noor/ui/nav/GooeyMath.kt
  - app/src/main/kotlin/com/sawan4art/noor/ui/nav/LiquidNavBar.kt
والاختبار الذهبي GooeyMathTest.kt يثبت التطابق.

الناتج: download/noor-liquid-alpha4-preview.gif (للعرض قبل تركيب أي كود).
"""
import math
import os

from PIL import Image, ImageDraw, ImageFilter

# ===== الثوابت — يجب أن تطابق الكوتلن حرفياً =====
DRAIN_MS = 1040   # LiquidNavBar (كان 780)
HEAD_MS = 800     # LiquidNavBar (كان 620)
JIGGLE_MS = 1250  # LiquidNavBar (كان 950)
PULSE_ZETA = 0.55
PULSE_K = 280.0   # spring stiffness (كان 380)

R_FULL = 25.0     # dp — blobRadius
MIN_RADIUS = 1.2  # GooeyMath.MIN_RADIUS

BAR_W, BAR_H = 180.0, 76.0
XS = [30.0, 90.0, 150.0]  # مراكز الأزرار الثلاثة

# ===== إيزنغات كومبوز — منحنيات بيزيه المكعبة نفسها =====
# FastOutSlowInEasing = CubicBezierEasing(0.4, 0, 0.2, 1)
# EaseOutBack = CubicBezierEasing(0.34, 1.56, 0.64, 1)


def bezier(p1x, p1y, p2x, p2y, x):
    """حل x→t بنيوتن ثم y(t) — مطابقة CubicBezierEasing في كومبوز."""
    def sample(t):
        omt = 1 - t
        return (3 * omt * omt * t * p1x + 3 * omt * t * t * p2x + t ** 3,
                3 * omt * omt * t * p1y + 3 * omt * t * t * p2y + t ** 3)

    t = x
    for _ in range(8):
        omt = 1 - t
        dx = 3 * omt * omt * p1x + 6 * omt * t * (p2x - p1x) + 3 * t * t * (1 - p2x)
        if abs(dx) < 1e-6:
            break
        t -= (sample(t)[0] - x) / dx
    t = min(1.0, max(0.0, t))
    return sample(t)[1]


def fast_out_slow_in(x):
    return bezier(0.4, 0.0, 0.2, 1.0, x)


def ease_out_back(x):
    return bezier(0.34, 1.56, 0.64, 1.0, x)


# ===== GooeyMath — نسخة بايثون حرفية (الثوابت الجديدة: ألزق وأبطأ) =====


def surface_wave(angle, time, energy, phase=0.0):
    idle = 0.038 * math.sin(2 * angle + 1.0 * time + phase) \
        + 0.028 * math.sin(4 * angle - 1.5 * time + 2.1 * phase) \
        + 0.017 * math.sin(6 * angle + 2.1 * time + 4.2 * phase)
    live = energy * (
        0.062 * math.sin(4 * angle + 4.8 * time + phase)
        + 0.042 * math.sin(7 * angle - 3.6 * time + 1.7 * phase)
    )
    return idle + live


def center_drift(time, phase=0.0):
    return 0.040 * math.sin(0.7 * time + phase) + 0.024 * math.sin(1.3 * time + 1.3 + phase)


def bridge_sway(time, phase=0.0):
    return 0.6 * math.sin(1.2 * time + phase) + 0.4 * math.sin(1.9 * time + 1.9 + phase)


def waist_for(source_radius, full_radius):
    if full_radius <= 0:
        return 0.97
    remaining = min(1.0, max(0.0, source_radius / full_radius))
    return 0.42 + 0.55 * (1 - remaining) ** 1.5


def bridge(x1, y1, r1, x2, y2, r2, waist):
    """نسخة GooeyMath.bridge — الأيسر أولاً، بلا حد مسافة."""
    if x1 <= x2:
        ax, ay, ar, bx, by, br = x1, y1, r1, x2, y2, r2
    else:
        ax, ay, ar, bx, by, br = x2, y2, r2, x1, y1, r1
    d = bx - ax
    if d < 1e-4 or d <= abs(ar - br):
        return None
    mx, my = (ax + bx) / 2, (ay + by) / 2
    p1y, p2y = ay - ar, ay + ar
    p3y, p4y = by - br, by + br
    qax = ax + (mx - ax) * waist
    qbx = bx + (mx - bx) * waist
    return dict(
        ax=ax, ay=ay, ar=ar, bx=bx, by=by, br=br,
        p1=(ax, p1y), qA1=(qax, p1y + (my - p1y) * waist),
        qA2=(qbx, p3y + (my - p3y) * waist), p3=(bx, p3y),
        p4=(bx, p4y), qB2=(qbx, p4y + (my - p4y) * waist),
        qB1=(qax, p2y + (my - p2y) * waist), p2=(ax, p2y),
    )


def pulse_spring(x0, v0, target, t):
    """حل تحليلي لنابض كومبوز (كتلة 1): x(t) = target + e^{-ζωt}(A cos + B sin)."""
    zeta, k = PULSE_ZETA, PULSE_K
    w = math.sqrt(k)
    wd = w * math.sqrt(1 - zeta * zeta)
    a = x0 - target
    b = (v0 + zeta * w * a) / wd
    decay = math.exp(-zeta * w * t)
    return target + decay * (a * math.cos(wd * t) + b * math.sin(wd * t))


# ===== الرسم =====
SS = 5            # مضاعف الرسم الفائق
OUT_W, OUT_H = 450, 190   # الناتج النهائي (180×76dp × 2.5)
N = 44            # نقاط السطح — مثل الكوتلن


def liquid_blob_points(cx, cy, base_radius, time, energy, phase):
    """نقاط addLiquidBlob — من الأعلى بعقارب الساعة."""
    pts = []
    for i in range(N):
        angle = 2 * math.pi * i / N - math.pi / 2
        radius = base_radius * (1 + surface_wave(angle, time, energy, phase))
        pts.append((cx + radius * math.cos(angle), cy + radius * math.sin(angle)))
    return pts


def blob_path(pts):
    """كاتمول-روم مغلق بنفس معادلة الكوتلن (المشتقات /6)."""
    path = []
    n = len(pts)
    for i in range(n):
        p_prev, p_i, p_next, p_next2 = pts[(i - 1) % n], pts[i], pts[(i + 1) % n], pts[(i + 2) % n]
        c1 = (p_i[0] + (p_next[0] - p_prev[0]) / 6, p_i[1] + (p_next[1] - p_prev[1]) / 6)
        c2 = (p_next[0] - (p_next2[0] - p_i[0]) / 6, p_next[1] - (p_next2[1] - p_i[1]) / 6)
        path.append((p_i, c1, c2, p_next))
    return path


def flatten_bezier(p0, c1, c2, p1, steps=16):
    out = []
    for s in range(1, steps + 1):
        t = s / steps
        omt = 1 - t
        x = omt ** 3 * p0[0] + 3 * omt * omt * t * c1[0] + 3 * omt * t * t * c2[0] + t ** 3 * p1[0]
        y = omt ** 3 * p0[1] + 3 * omt * omt * t * c1[1] + 3 * omt * t * t * c2[1] + t ** 3 * p1[1]
        out.append((x, y))
    return out


def arc_points(cx, cy, r, a0, a1, steps=24):
    """قوس باتجاه عقارب الساعة في نظام y-لأسفل (زي arcTo sweep=1)."""
    return [(cx + r * math.cos(math.radians(a0 + (a1 - a0) * s / steps)),
             cy + r * math.sin(math.radians(a0 + (a1 - a0) * s / steps)))
            for s in range(1, steps + 1)]


def draw_state(t, from_x, to_x, src_r, head_r, head_x, energy, pulse):
    """نقاط السائل الموحد (نقطة/نقطتان + جسر) بإحداثيات dp."""
    y = BAR_H / 2
    polys = []
    if src_r > MIN_RADIUS:
        pts = liquid_blob_points(from_x, y + R_FULL * center_drift(t, 0.8),
                                 src_r, t, energy, 1.3)
        poly = []
        for seg in blob_path(pts):
            poly += flatten_bezier(*seg)
        polys.append(poly)
    if head_r > MIN_RADIUS:
        pts = liquid_blob_points(head_x, y + R_FULL * center_drift(t, 2.6),
                                 head_r, t, energy, 0.0)
        poly = []
        for seg in blob_path(pts):
            poly += flatten_bezier(*seg)
        polys.append(poly)
    if src_r > MIN_RADIUS and head_r > MIN_RADIUS:
        b = bridge(from_x, y, src_r, head_x, y, head_r, waist_for(src_r, R_FULL))
        if b is not None:
            sway = R_FULL * 0.05 * bridge_sway(t)
            q_a1 = (b['qA1'][0], b['qA1'][1] + sway)
            q_a2 = (b['qA2'][0], b['qA2'][1] + sway * 0.55)
            q_b1 = (b['qB1'][0], b['qB1'][1] - sway * 0.85)
            q_b2 = (b['qB2'][0], b['qB2'][1] - sway * 0.5)
            # نفس ترتيب addBridgePath: p1 → قبة يمين → p2 → قبة يسار
            poly = [b['p1']]
            poly += flatten_bezier(b['p1'], q_a1, q_a2, b['p3'])
            poly += arc_points(b['bx'], b['by'], b['br'], -90, 90)
            poly += flatten_bezier(b['p4'], q_b2, q_b1, b['p2'])
            poly += arc_points(b['ax'], b['ay'], b['ar'], 90, 270)
            polys.append(poly)
    return polys


# ===== مشهد الإطارات =====
DT_MS = 40


def simulate():
    """جدول الحالات — نفس منطق LaunchedEffect(selected)."""
    frames = []  # (time_ms, state)
    t = 0.0

    def steady(t, btn):
        return dict(from_x=XS[btn], to_x=XS[btn], src_r=0.0, head_r=R_FULL, head_x=XS[btn],
                    energy=0.0, pulse=1.0)

    def add_rest(duration, btn):
        nonlocal t
        n = int(round(duration / DT_MS))
        for _ in range(n):
            frames.append((t, steady(t, btn)))
            t += DT_MS

    def add_transfer(a, b):
        nonlocal t
        start = t
        jiggle_start = None
        pulse_start = None
        n = int(math.ceil((DRAIN_MS + JIGGLE_MS) / DT_MS)) + 2
        for i in range(n):
            local = t - start
            drain_p = fast_out_slow_in(min(1.0, local / DRAIN_MS))
            src_r = R_FULL * (1 - drain_p)
            head_r = R_FULL * drain_p
            head_p = ease_out_back(min(1.0, local / HEAD_MS))
            head_x = XS[a] + (XS[b] - XS[a]) * head_p
            energy = 0.0
            pulse = 1.0
            if local >= HEAD_MS - 40:
                if jiggle_start is None:
                    jiggle_start = local
                    pulse_start = local
                e_local = local - jiggle_start
                energy = max(0.0, 1.0 - e_local / JIGGLE_MS)
                pulse = pulse_spring(1.16, 0.0, 1.0, max(0.0, local - pulse_start) / 1000.0)
            head_r_final = head_r * pulse
            # فحص «لا فصل»: الجسر موصول عند كل إطار أثناء النقل
            if src_r > MIN_RADIUS and head_r_final > MIN_RADIUS:
                bb = bridge(XS[a], BAR_H / 2, src_r, head_x, BAR_H / 2, head_r_final,
                            waist_for(src_r, R_FULL))
                assert bb is not None, f'انقطاع الجسر عند t={local:.0f}ms — مخالفة قاعدة لا فصل'
            frames.append((t, dict(from_x=XS[a], to_x=XS[b], src_r=src_r,
                                   head_r=head_r_final, head_x=head_x,
                                   energy=energy, pulse=pulse)))
            t += DT_MS

    add_rest(950, 0)
    add_transfer(0, 1)
    add_rest(1250, 1)
    add_transfer(1, 0)
    add_rest(1450, 0)
    return frames


# ===== الألوان (الوضع الليلي — مطابقة glassSpec) =====
BG_TOP = (12, 21, 18)       # 0C1512
BG_BOTTOM = (18, 30, 26)
GLASS_TINT = (18, 30, 26, 216)
GLASS_BORDER = (255, 255, 255, 44)
BLOB_TOP = (155, 220, 199)  # 9BDCC7
BLOB_BOTTOM = (130, 201, 176)  # 82C9B0 NightPrimary
ICON_ON = (10, 42, 33)      # NightOnPrimary
ICON_OFF = (185, 206, 196)  # B9CEC4


def glyph(draw, kind, cx, cy, s, color):
    """أيقونات هندسية مجردة للأزرار الثلاثة."""
    if kind == 0:  # قوس قبة (بيت مبسط)
        draw.arc([cx - s, cy - s * 0.4, cx + s, cy + s], 180, 360, fill=color, width=max(2, int(s // 4)))
        draw.line([cx - s, cy + s * 0.55, cx + s, cy + s * 0.55], fill=color, width=max(2, int(s // 4)))
    elif kind == 1:  # نجمة رباعية
        pts = []
        for i in range(8):
            ang = math.radians(i * 45 - 90)
            r = s if i % 2 == 0 else s * 0.38
            pts.append((cx + r * math.cos(ang), cy + r * math.sin(ang)))
        draw.polygon(pts, fill=color)
    else:  # هلال هندسي — قوس سميك مفتوح
        draw.arc([cx - s, cy - s, cx + s, cy + s], 65, 295,
                 fill=color, width=max(2, int(s * 0.42)))


def render_frame(state, t_ms):
    scale = SS
    W, H = int(BAR_W * scale), int(BAR_H * scale)
    t = t_ms / 1000.0

    img = Image.new('RGBA', (W, H))
    # خلفية متدرجة عمودية
    grad = Image.new('RGB', (1, H))
    for yy in range(H):
        k = yy / H
        grad.putpixel((0, yy), tuple(int(BG_TOP[i] + (BG_BOTTOM[i] - BG_TOP[i]) * k) for i in range(3)))
    img.paste(grad.resize((W, H)), (0, 0))
    # لمحة محتوى خلف الزجاج: دوائر باهتة تتحرك ببطء
    deco = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    ddec = ImageDraw.Draw(deco)
    for i, (cx, r) in enumerate([(W * 0.2, W * 0.09), (W * 0.55, W * 0.13), (W * 0.85, W * 0.07)]):
        ox = math.sin(t * 0.35 + i * 2.1) * W * 0.02
        oy = math.cos(t * 0.27 + i * 1.7) * H * 0.05
        ddec.ellipse([cx - r + ox, H * 0.28 - r + oy, cx + r + ox, H * 0.28 + r + oy],
                     fill=(155, 220, 199, 26))
    deco = deco.filter(ImageFilter.GaussianBlur(scale * 2))
    img.alpha_composite(deco)

    # كبسولة الزجاج
    glass = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    dg = ImageDraw.Draw(glass)
    m = 2 * scale
    dg.rounded_rectangle([m, m, W - m, H - m], radius=H / 2 - m, fill=GLASS_TINT)
    img.alpha_composite(glass)

    # السائل عبر قناع موحد (اتحاد بلا خطوط تراكب — مثل winding nonzero)
    y = BAR_H / 2 * scale
    polys = draw_state(t, state['from_x'], state['to_x'], state['src_r'],
                       state['head_r'], state['head_x'], state['energy'], state['pulse'])
    if polys:
        mask = Image.new('L', (W, H), 0)
        dmask = ImageDraw.Draw(mask)
        for poly in polys:
            dmask.polygon([(x * scale, yy * scale) for x, yy in poly], fill=255)
        blob = Image.new('RGBA', (W, H), (0, 0, 0, 0))
        y0, y1 = y - R_FULL * 1.4 * scale, y + R_FULL * 1.4 * scale
        bgrad = Image.new('RGB', (1, H))
        for yy in range(H):
            k = min(1.0, max(0.0, (yy - y0) / (y1 - y0)))
            bgrad.putpixel((0, yy), tuple(int(BLOB_TOP[i] + (BLOB_BOTTOM[i] - BLOB_TOP[i]) * k) for i in range(3)))
        bgrad = bgrad.resize((W, H))
        blob.paste(bgrad, (0, 0), mask)
        img.alpha_composite(blob)

    # الأيقونات
    dimg = ImageDraw.Draw(img)
    for i, bx in enumerate(XS):
        on = (state['head_x'] == XS[i]) and state['head_r'] > MIN_RADIUS * 6
        color = ICON_ON if on else ICON_OFF
        glyph(dimg, i, bx * scale, y, 7.5 * scale, color)

    # حد الزجاج
    dg2 = ImageDraw.Draw(img)
    dg2.rounded_rectangle([m, m, W - m, H - m], radius=H / 2 - m,
                          outline=GLASS_BORDER, width=max(1, scale // 2))

    return img.resize((OUT_W, OUT_H), Image.LANCZOS)


def main():
    frames = simulate()
    imgs = []
    durations = []
    prev = None
    for t_ms, state in frames:
        img = render_frame(state, t_ms)
        # دمج الإطارات المتطابقة (درس الدفعة العاشرة: PIL يقص الإطارات المتطابقة)
        if prev is not None and list(img.getdata()) == list(prev.getdata()):
            durations[-1] += DT_MS
        else:
            imgs.append(img)
            durations.append(DT_MS)
        prev = img
    out = os.environ.get('OUT_GIF', '/home/z/my-project/download/noor-liquid-alpha4-preview.gif')
    imgs[0].save(out, save_all=True, append_images=imgs[1:], duration=durations,
                 loop=0, optimize=False)
    print(f'✅ {out}: {len(imgs)} إطار، {sum(durations) / 1000:.1f} ثانية، '
          f'{os.path.getsize(out) // 1024}KB')
    print(f'   ثوابت المعاينة: DRAIN={DRAIN_MS} HEAD={HEAD_MS} JIGGLE={JIGGLE_MS} '
          f'نبض ζ={PULSE_ZETA} k={PULSE_K}')


if __name__ == '__main__':
    main()
