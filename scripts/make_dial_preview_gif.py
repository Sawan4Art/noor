#!/usr/bin/env python3
"""معاينة مزلاج الشمس والقمر — نفس أرقام كوتلن حرفياً (عقيدة alpha4):

المنهج: كل صيغة تُنقل حرفياً من ThemeDialMath/GooeyMath/ThemeDialState
- surfaceWave / centerDrift / bridgeSway / waistFor / bridge (الجسر بلا فصل)
- addLiquidBlob (كاتمول-روم مغلق) + مسار الجسر بقبتيه
- حدّ انقسام مستقيم رأسي **قصّ صلب** (clipRect — قرار alpha11 بعد فشل القناع
  بصمتاً على جهاز المالك) وعلى الحدّ **توهج خافت** (تلعيم alpha13: «الوضع
  الفاتح يبان كأنه ضوءه باهت على الوضع المظلم والعكس صحيح — ليس شعاع نور
  مستقيم وواضح»): ستارة بذروة 0.05 تنساب أطول في الجهة المُنارة (GLOW_OUT)
  وأقصر في جهة الهدف (GLOW_IN) + توهج إهليلجي مفلطح 0.42 حول الأيقونة —
  شعاع الكشاف الساطع بنواته الحارّة **أُزيل كلياً** (طلب alpha13)
- الإيماءة من الأيقونة وحدها — الفراغ يمر للطبقات تحتيه (درس alpha7)
- المدد: DRAIN=1040 HEAD=800 JIGGLE=1250 SWEEP=960 و EaseOutBack(0.34,1.56,0.64,1)

السيناريو (بطلب المالك): مظلم → سحب لليمين (الأيقونة مع اليد، الفاتح خلفها
بلا أي خط مرئي) → إمساك في المنتصف (نصف فاتح ونصف مظلم بلا فاصل باين)
→ رفع عند 70% (تثبيت فاتح: طفح ومرسى يمين برجفة) → نقرة (عودة للظلام برحلة القمر).

تخطيط alpha12 (بطلب المالك): المسار وحده أعلى الشاشة، المدينة ودبوسها وزر
التحديد داخل بطاقة الهيرو تحت التواريخ، وهيدر التذكيرات مكان «مواقيت اليوم».

قتل الومضة (alpha12): نهاية الرحلة لا تُسقط غطاء المعاينة فوراً — يبقى
معلقاً (صورة كاملة للثيم الجديد) حتى يصل الثيم المثبت ثم يُسقط في اللحظة
نفسها؛ هنا لا يتغير شكل الإطارات لأن الغطاء يطابق الأساس بكسلياً — عقد
الومضة مُختبر وحدةً في ThemeDialStateCommitTest.
"""
import math
import os

from PIL import Image, ImageDraw, ImageFilter

W, H = 360, 640
FPS = 30
DOWNLOAD = '/home/z/my-project/download'

# ===== ثوابت كوتلن حرفياً — بمقياس متناسب: المعاينة تمثل شاشة 411dp عرضاً =====
DENS = W / 411.0
TRACK_H = 46 * DENS
BLOB_R = 17 * DENS
TRACK_X = 20 * DENS
TRACK_W = W - 2 * TRACK_X
PAD = BLOB_R * 1.6
TRACK_TOP = 40 * DENS  # alpha12: المسار وحده أعلى الشاشة (تحت شريط الحالة)
ICON_PX = 24 * DENS
A_L = TRACK_X + PAD
A_R = TRACK_X + TRACK_W - PAD

DRAIN_MS, HEAD_MS, SWEEP_MS, JIGGLE_MS = 1040, 800, 960, 1250
FADE_MS = 300  # انسياب ناعم للحواف

# ===== ألوان الوضعين (من Theme.kt و ThemeDialOverlay) =====
DARK_BG = (18, 26, 23)
DARK_SURF = (26, 38, 34)
DARK_CARD = (23, 34, 30)
DARK_TXT = (222, 233, 226)
DARK_MUT = (167, 181, 173)
DARK_HERO_TOP = (24, 46, 40)
DARK_HERO_BOT = (16, 27, 24)
DARK_PRIMARY = (139, 195, 168)

LIGHT_BG = (247, 244, 238)
LIGHT_SURF = (255, 255, 255)
LIGHT_CARD = (252, 250, 246)
LIGHT_TXT = (26, 38, 34)
LIGHT_MUT = (104, 116, 109)
LIGHT_HERO_TOP = (206, 227, 210)
LIGHT_HERO_BOT = (166, 200, 181)
LIGHT_PRIMARY = (46, 105, 86)

SUN_TOP = (255, 226, 174)
SUN_BOT = (244, 168, 92)
MOON_TOP = (215, 227, 244)
MOON_BOT = (143, 166, 200)
SUN_TINT = (69, 43, 16)
MOON_TINT = (26, 36, 51)


# ===== نقل GooeyMath حرفياً =====
def surface_wave(angle, time, energy, phase=0.0):
    idle = 0.038 * math.sin(2 * angle + 1.0 * time + phase) \
        + 0.028 * math.sin(4 * angle - 1.5 * time + 2.1 * phase) \
        + 0.017 * math.sin(6 * angle + 2.1 * time + 4.2 * phase)
    live = energy * (0.062 * math.sin(4 * angle + 4.8 * time + phase)
                     + 0.042 * math.sin(7 * angle - 3.6 * time + 1.7 * phase))
    return idle + live


def center_drift(time, phase=0.0):
    return 0.040 * math.sin(0.7 * time + phase) + 0.024 * math.sin(1.3 * time + 1.3 + phase)


def bridge_sway(time, phase=0.0):
    return 0.6 * math.sin(1.2 * time + phase) + 0.4 * math.sin(1.9 * time + 1.9 + phase)


MIN_R = 1.2


def waist_for(source_radius, full_radius):
    if full_radius <= 0:
        return 0.97
    remaining = max(0.0, min(1.0, source_radius / full_radius))
    return 0.42 + 0.55 * (1 - remaining) ** 1.5


def bridge(x1, y1, r1, x2, y2, r2, waist):
    if x1 <= x2:
        ax, ay, ar, bx, by, br = x1, y1, r1, x2, y2, r2
    else:
        ax, ay, ar, bx, by, br = x2, y2, r2, x1, y1, r1
    d = bx - ax
    if d < 1e-4 or d <= abs(ar - br):
        return None
    mx, my = (ax + bx) / 2, (ay + by) / 2
    qax = ax + (mx - ax) * waist
    qbx = bx + (mx - bx) * waist
    return dict(
        ax=ax, ay=ay, ar=ar, bx=bx, by=by, br=br,
        p1=(ax, ay - ar), p2=(ax, ay + ar), p3=(bx, by - br), p4=(bx, by + br),
        qa1=(qax, (ay - ar) + (my - (ay - ar)) * waist),
        qa2=(qbx, (by - br) + (my - (by - br)) * waist),
        qb2=(qbx, (by + br) + (my - (by + br)) * waist),
        qb1=(qax, (ay + ar) + (my - (ay + ar)) * waist),
    )


def cubic(p0, c1, c2, p3, n=14):
    pts = []
    for j in range(n + 1):
        t = j / n
        mt = 1 - t
        pts.append((mt**3 * p0[0] + 3 * mt**2 * t * c1[0] + 3 * mt * t**2 * c2[0] + t**3 * p3[0],
                    mt**3 * p0[1] + 3 * mt**2 * t * c1[1] + 3 * mt * t**2 * c2[1] + t**3 * p3[1]))
    return pts


def arc(center, r, a0, a1, n=18):
    return [(center[0] + r * math.cos(math.radians(a0 + (a1 - a0) * j / n)),
             center[1] + r * math.sin(math.radians(a0 + (a1 - a0) * j / n))) for j in range(n + 1)]


def liquid_polygon(cx, cy, base_r, time, energy, phase):
    n = 44
    pts = []
    for i in range(n):
        ang = 2 * math.pi * i / n - math.pi / 2
        r = base_r * (1 + surface_wave(ang, time, energy, phase))
        pts.append((cx + r * math.cos(ang), cy + r * math.sin(ang)))
    # كاتمول-روم مغلق بنفس معاملات كوتلن
    out = []
    m = len(pts)
    for i in range(m):
        p0, p1, p2, p3 = pts[(i - 1) % m], pts[i], pts[(i + 1) % m], pts[(i + 2) % m]
        out.append(p1)
        c1 = (p1[0] + (p2[0] - p0[0]) / 6, p1[1] + (p2[1] - p0[1]) / 6)
        c2 = (p2[0] - (p3[0] - p1[0]) / 6, p2[1] - (p3[1] - p1[1]) / 6)
        out.extend(cubic(p1, c1, c2, p2, 6)[1:])
    return out


def bridge_polygon(b, time):
    """مسار الجسر: أعلى-يسار ← منحنى علوي ← قبة يمين ← منحنى سفلي ← قبة يسار."""
    sway = BLOB_R * 0.05 * bridge_sway(time)
    qa1 = (b['qa1'][0], b['qa1'][1] + sway)
    qa2 = (b['qa2'][0], b['qa2'][1] + sway * 0.55)
    qb1 = (b['qb1'][0], b['qb1'][1] - sway * 0.85)
    qb2 = (b['qb2'][0], b['qb2'][1] - sway * 0.5)
    pts = []
    pts += [b['p1']]
    pts += cubic(b['p1'], qa1, qa2, b['p3'])[1:]
    pts += arc((b['bx'], b['by']), b['br'], -90, 90)[1:]
    pts += cubic(b['p4'], qb2, qb1, b['p2'])[1:]
    pts += arc((b['ax'], b['ay']), b['ar'], 90, 270)[1:]
    return pts


# ===== نقل ThemeDialMath حرفياً (alpha13: قصّ صلب + توهج خافت) =====
GLOW_IN = 44 * DENS        # GLOW_IN_DP — انسياب التوهج في جهة الهدف (أقصر)
GLOW_OUT = 120 * DENS      # GLOW_OUT_DP — انسيابه في الجهة المُنارة (أطول)

GLOW_COL = (255, 196, 126)   # توهج دافئ خافت (نور الفاتح على المظلم)
GLOW_COL_C = (185, 205, 236) # توهج بارد خافت (المظلم على الفاتح) — متناسب


def edge_x(boundary):
    """حدّ الانقسام — مستقيم رأسي عند موضع الأيقونة، والمعاينة **قصّ صلب**
    (clipRect): الفاتح يسار الحدّ والمظلم يمينه دائماً بلا أي تدرج في القص،
    وحدّ القص يُذيبه التوهج الخافت المرسوم فوقه."""
    return boundary


def sun_alpha(p):
    return max(0.0, min(1.0, (p - 0.35) / 0.3))


def moon_alpha(p):
    return sun_alpha(1 - p)


# ===== منحنيات الحركة =====
def _cubic_bezier_y(t, x1, y1, x2, y2):
    """y عند x=t على منحنى بيزيه مكعبي (1.4.2 بنفس خوارزمية Compose)."""
    t = min(max(t, 0.0), 1.0)
    lo, hi, u = 0.0, 1.0, t
    for _ in range(28):
        xv = 3 * (1 - u) ** 2 * u * x1 + 3 * (1 - u) * u ** 2 * x2 + u ** 3
        if abs(xv - t) < 1e-6:
            break
        if xv < t:
            lo = u
        else:
            hi = u
        u = (lo + hi) / 2
    return 3 * (1 - u) ** 2 * u * y1 + 3 * (1 - u) * u ** 2 * y2 + u ** 3


def ease_out_back(t):
    # CubicBezierEasing(0.34, 1.56, 0.64, 1.0) — نفس كوتلن
    return _cubic_bezier_y(t, 0.34, 1.56, 0.64, 1.0)


def fast_out_slow_in(t):
    return _cubic_bezier_y(t, 0.4, 0.0, 0.2, 1.0)


def lerp(a, b, t):
    return a + (b - a) * t


def lerp_c(c1, c2, t):
    return tuple(int(lerp(c1[i], c2[i], t)) for i in range(3))


# ===== واجهة مُبسطة بثيم معين — تخطيط alpha12 =====
def draw_app(draw, theme):
    if theme == 'dark':
        bg, surf, card, txt, mut, hero_t, hero_b, prim = \
            DARK_BG, DARK_SURF, DARK_CARD, DARK_TXT, DARK_MUT, DARK_HERO_TOP, DARK_HERO_BOT, DARK_PRIMARY
    else:
        bg, surf, card, txt, mut, hero_t, hero_b, prim = \
            LIGHT_BG, LIGHT_SURF, LIGHT_CARD, LIGHT_TXT, LIGHT_MUT, LIGHT_HERO_TOP, LIGHT_HERO_BOT, LIGHT_PRIMARY
    draw.rectangle([0, 0, W, H], fill=bg)
    # بطاقة الهيرو المربعة — تمتد بعد خروج بطاقة المدينة من الأعلى (alpha12)
    HERO_TOP, HERO_BOT = 94 * DENS, 440 * DENS
    draw.rounded_rectangle([TRACK_X, HERO_TOP, W - TRACK_X, HERO_BOT], radius=32 * DENS,
                           fill=hero_b)
    draw.rounded_rectangle([TRACK_X, HERO_TOP, W - TRACK_X, HERO_BOT - 40 * DENS], radius=32 * DENS,
                           fill=hero_t)
    # حلقة العدّاد + علامة موضع الصلاة
    cx, cy, r = W / 2, 250 * DENS, 46 * DENS
    draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=prim, width=int(4 * DENS))
    draw.ellipse([cx - 5 * DENS, cy - 5 * DENS, cx + 5 * DENS, cy + 5 * DENS], fill=prim)
    # شجرتا التواريخ تحت الحلقة
    for i, wdt in enumerate((70, 56)):
        x0 = cx - (i == 0) * (10 * DENS + wdt * DENS / 2) + (i == 1) * 10 * DENS
        draw.rounded_rectangle([cx - wdt * DENS / 2 - (i == 0) * 10 * DENS,
                                366 * DENS,
                                cx + wdt * DENS / 2 + (i == 1) * 10 * DENS,
                                390 * DENS], radius=12 * DENS, fill=(255, 255, 255, 26))
    # صف الموقع داخل البطاقة تحت التواريخ (alpha12): كبسولة المدينة يميناً + زر تحديد دائري يساراً
    cy_loc = 412 * DENS
    pill_w, pill_h = 108 * DENS, 20 * DENS
    pill_x1 = cx + 34 * DENS
    draw.rounded_rectangle([pill_x1 - pill_w, cy_loc - pill_h / 2, pill_x1, cy_loc + pill_h / 2],
                           radius=pill_h / 2, fill=(255, 255, 255, 26))
    draw.ellipse([pill_x1 - pill_w + 6 * DENS, cy_loc - 3.5 * DENS,
                  pill_x1 - pill_w + 13 * DENS, cy_loc + 3.5 * DENS], fill=prim)
    btn_r = 13 * DENS
    draw.ellipse([pill_x1 - pill_w - 10 * DENS - btn_r, cy_loc - btn_r,
                  pill_x1 - pill_w - 10 * DENS + btn_r, cy_loc + btn_r],
                 fill=(255, 255, 255, 26), outline=prim, width=2)
    # هيدر التذكيرات — مكان «مواقيت اليوم» (alpha12): صندوق الأيقونة يميناً RTL وسطرا تذكير
    ry = 456 * DENS
    draw.rounded_rectangle([W - TRACK_X - 36 * DENS, ry, W - TRACK_X, ry + 36 * DENS],
                           radius=12 * DENS, fill=prim)
    for k, lw in enumerate((0.62, 0.38)):
        draw.rounded_rectangle([TRACK_X, ry + 6 * DENS + k * 14 * DENS,
                                TRACK_X + (W - 2 * TRACK_X - 44 * DENS) * lw,
                                ry + 14 * DENS + k * 14 * DENS],
                               radius=4 * DENS, fill=mut)
    # شبكة المواقيت
    gx, gy = TRACK_X, 504 * DENS
    gw = (W - 2 * TRACK_X - 2 * 10 * DENS) / 3
    for row in range(2):
        for col in range(3):
            x0 = gx + col * (gw + 10 * DENS)
            y0 = gy + row * (56 * DENS)
            active = (row * 3 + col) == 0
            draw.rounded_rectangle([x0, y0, x0 + gw, y0 + 48 * DENS], radius=14 * DENS,
                                   fill=(prim if active else card),
                                   outline=(prim if active else None), width=2)


def draw_sun_moon(draw, x, y, p, size):
    """أيقونتا الشمس والقمر بشفافيتيهما — نفس sunAlpha/moonAlpha."""
    sa, ma = sun_alpha(p), moon_alpha(p)
    r = size / 2
    if ma > 0.01:
        # هلال: دائرة كبيرة ناقصة دائرة مزاحة نحو أعلى-يمين
        mask = Image.new('L', (int(size * 3), int(size * 3)), 0)
        dm = ImageDraw.Draw(mask)
        c = size * 1.5
        dm.ellipse([c - r, c - r, c + r, c + r], fill=int(255 * ma))
        off, r2 = r * 0.62, r * 1.18
        dm.ellipse([c + off - r2, c - off - r2, c + off + r2, c - off + r2], fill=0)
        glyph = Image.new('RGBA', mask.size, MOON_TINT + (255,))
        frame.paste(glyph, (int(x - c), int(y - c)), mask)
    if sa > 0.01:
        # قرص + ثماني أشعة
        disc = r * 0.56
        col = SUN_TINT + (int(255 * sa),)
        draw.ellipse([x - disc, y - disc, x + disc, y + disc], fill=col)
        for i in range(8):
            a = math.radians(45 * i)
            tip = r * 1.02
            base = r * 0.72
            ha = math.radians(6.6)
            pts = [(x + tip * math.cos(a), y + tip * math.sin(a)),
                   (x + base * math.cos(a + ha), y + base * math.sin(a + ha)),
                   (x + base * math.cos(a - ha), y + base * math.sin(a - ha))]
            draw.polygon(pts, fill=col)


def render_frame(state, t_sec):
    global frame
    img = Image.new('RGB', (W, H), DARK_BG)
    frame = img
    d = ImageDraw.Draw(img, 'RGBA')

    base_dark = state['base_dark']
    split = state['split_active']
    overlay_light = state['overlay_is_light']
    boundary = state['mask_x']

    # الطبقة الأساسية بثيمها
    draw_app(d, 'dark' if base_dark else 'light')

    # طبقة الهدف **قصّاً صلباً** (نفس clipRect في كوتلن): الفاتح يسار الحدّ والمظلم يمينه
    if split:
        ov = Image.new('RGB', (W, H), 0)
        draw_app(ImageDraw.Draw(ov), 'light' if overlay_light else 'dark')
        mask = Image.new('L', (W, H), 0)
        buf = bytearray(W * H)
        edge = int(boundary)
        for y in range(H):
            base = y * W
            if overlay_light:
                # الفاتح (الهدف) على يسار الحدّ
                for x in range(W):
                    buf[base + x] = 255 if x < edge else 0
            else:
                # المظلم (الهدف) على يمين الحدّ — والفاتح (الأساس) يساره
                for x in range(W):
                    buf[base + x] = 255 if x >= edge else 0
        mask.putdata(bytes(buf))
        img.paste(ov, (0, 0), mask)

    # ===== التوهج الخافت على حدّ الانقسام — نفس طبقات كوتلن حرفياً =====
    # (١) الستارة الخافتة كاملة الارتفاع: ذروة 0.05 عند الحدّ تنساب أطول في
    # الجهة المُنارة وأقصر في جهة الهدف — تُذيب حدّ القص ولا تُقرأ جداراً
    if split:
        power = state['beam']
        if power > 0.004:
            warm = overlay_light
            glow_col = GLOW_COL if warm else GLOW_COL_C

            def curtain_alpha(x, left, right, edge_frac):
                """شفافية الستارة عند x — ذروة 0.05*power عند الحدّ، سقوطها
                أطول يميناً (المُنارة) وأقصر يساراً (جهة الهدف)."""
                if x < left or x >= right:
                    return 0.0
                frac = (x - left) / (right - left)
                if frac <= edge_frac:
                    t = frac / edge_frac if edge_frac > 0 else 1.0
                    return 0.050 * power * (t ** 1.8)
                t = (frac - edge_frac) / (1 - edge_frac)
                return 0.050 * power * ((1 - t) ** 2.2)

            curtain_mask = Image.new('L', (W, H), 0)
            cbuf = bytearray(W * H)
            ccol = bytearray(W * H * 3)
            left0, right0 = boundary - GLOW_IN, boundary + GLOW_OUT
            edge_frac = GLOW_IN / (GLOW_IN + GLOW_OUT)
            for y in range(H):
                base = y * W
                for x in range(W):
                    aa = curtain_alpha(x, left0, right0, edge_frac)
                    if aa > 0:
                        i3 = base * 3 + x * 3
                        cbuf[base + x] = int(255 * aa)
                        ccol[i3:i3 + 3] = bytes(glow_col)
            curtain_mask.putdata(bytes(cbuf))
            curtain_img = Image.frombytes('RGB', (W, H), bytes(ccol))
            img.paste(curtain_img, (0, 0), curtain_mask)
            d = ImageDraw.Draw(img, 'RGBA')
            # (٢) التوهج الإهليلجي حول الأيقونة — ينساب من المصدر مفلطحاً
            # رأسياً (0.42) كي لا يُقرأ كجدار ضوء مستقيم — نفس تدرج كوتلن:
            # ذروة 0.11 عند المركز و0.05 عند نصف نصف القطر ثم 0 عند الحافة
            hx0, hy0 = boundary, TRACK_TOP + TRACK_H / 2
            halo_r = GLOW_OUT * 1.35
            halo_h = halo_r * 0.42
            halo = Image.new('L', (max(2, int(halo_r * 2)), max(2, int(halo_h * 2))), 0)
            dh = ImageDraw.Draw(halo)
            pw, ph = halo.size
            for py in range(ph):
                ny = (py - ph / 2) / (ph / 2)
                for px_ in range(pw):
                    nx = (px_ - pw / 2) / (pw / 2)
                    rr = math.hypot(nx, ny)
                    if rr < 1.0:
                        if rr <= 0.5:
                            aa = (0.11 - (0.11 - 0.05) * (rr / 0.5)) * power
                        else:
                            aa = 0.05 * ((1.0 - rr) / 0.5) * power
                        dh.point((px_, py), fill=int(255 * aa))
            tinted = Image.new('RGB', halo.size, glow_col)
            img.paste(tinted, (int(hx0 - pw / 2), int(hy0 - ph / 2)), halo)
            d = ImageDraw.Draw(img, 'RGBA')

    # كبسولة المسار
    d.rounded_rectangle([TRACK_X, TRACK_TOP, TRACK_X + TRACK_W, TRACK_TOP + TRACK_H],
                        radius=TRACK_H / 2,
                        fill=(100, 100, 100, 70), outline=(150, 150, 150, 90), width=1)

    # السائل: ذيل + رأس + جسر
    p_top = lerp_c(MOON_TOP, SUN_TOP, state['p'])
    p_bot = lerp_c(MOON_BOT, SUN_BOT, state['p'])
    mask = Image.new('L', (W, H), 0)
    dmask = ImageDraw.Draw(mask)
    tr, hr = state['tail_r'], state['head_r']
    if tr > MIN_R:
        dmask.polygon(liquid_polygon(state['tail_x'], TRACK_TOP + TRACK_H / 2 + BLOB_R * center_drift(t_sec, 1.7),
                                     tr, t_sec, state['energy'], 1.3), fill=255)
    if hr > MIN_R:
        hy = TRACK_TOP + TRACK_H / 2 + (0 if state['dragging'] else BLOB_R * center_drift(t_sec, 0.0))
        dmask.polygon(liquid_polygon(state['head_x'], hy, hr, t_sec, state['energy'], 0.0), fill=255)
    if tr > MIN_R and hr > MIN_R:
        b = bridge(state['tail_x'], TRACK_TOP + TRACK_H / 2, tr,
                   state['head_x'], TRACK_TOP + TRACK_H / 2, hr,
                   waist_for(tr, BLOB_R))
        if b:
            dmask.polygon(bridge_polygon(b, t_sec), fill=255)
    blob = Image.new('RGB', (W, H), 0)
    db = ImageDraw.Draw(blob)
    for y in range(int(TRACK_TOP - BLOB_R * 1.6), int(TRACK_TOP + TRACK_H + BLOB_R * 1.6)):
        t = (y - (TRACK_TOP - BLOB_R * 1.4)) / (TRACK_H + BLOB_R * 2.8)
        t = min(max(t, 0.0), 1.0)
        db.line([(0, y), (W, y)], fill=lerp_c(p_top, p_bot, t))
    img.paste(blob, (0, 0), mask)
    d = ImageDraw.Draw(img, 'RGBA')

    # الأيقونتان
    draw_sun_moon(d, state['head_x'], TRACK_TOP + TRACK_H / 2, state['p'], ICON_PX)
    return img


frame = None


def empty_state():
    return dict(base_dark=True, split_active=False, overlay_is_light=False,
                mask_x=0.0, beam=0.0, head_x=A_L, head_r=BLOB_R,
                tail_x=A_L, tail_r=0.0, p=0.0, dragging=False, energy=0.0, label='')


def build_timeline():
    """مولد الحالة إطاراً بإطار — نفس أطوار ThemeDialState."""
    frames = []
    ms = 0

    def push(st, label):
        frames.append((ms if not frames else frames[-1][0] + 1, dict(st, label=label)))

    # 1) سكون مظلم 0.6s
    for i in range(int(0.6 * FPS)):
        st = empty_state()
        st['label'] = 'ليل — القمر في مرساه اليسار (الإيماءة من الأيقونة وحدها)'
        frames.append((ms, st)); ms += 1000 // FPS

    # 2) سحب لليمين: اليد من مرسى اليسار إلى المنتصف خلال 1.3s
    drag_frames = int(1.3 * FPS)
    for i in range(drag_frames):
        t = i / (drag_frames - 1)
        finger = lerp(A_L, (A_L + A_R) / 2, t)
        st = empty_state()
        st.update(split_active=True, overlay_is_light=True, mask_x=finger, beam=1.0,
                  head_x=finger, head_r=BLOB_R, tail_x=A_L, tail_r=BLOB_R,
                  p=max(0.0, (finger - A_L) / (A_R - A_L)), dragging=True, energy=1.0)
        st['label'] = 'سحب يمين = فاتح — الفاتح يسار الحدّ وتوهج خافت ينساب فيه'
        frames.append((ms, st)); ms += 1000 // FPS

    # 3) إمساك في المنتصف 1.0s — نصف فاتح ونصف مظلم
    for i in range(int(1.0 * FPS)):
        st = empty_state()
        mid = (A_L + A_R) / 2
        st.update(split_active=True, overlay_is_light=True, mask_x=mid, beam=1.0,
                  head_x=mid, head_r=BLOB_R, tail_x=A_L, tail_r=BLOB_R,
                  p=0.5, dragging=True, energy=1.0)
        st['label'] = 'ممسوك في المنتصف: نص فاتح ونص مظلم وتوهج خافت يذيب حدّهما'
        frames.append((ms, st)); ms += 1000 // FPS

    # 4) سحب لليمين حتى 70% خلال 0.6s
    drag_frames = int(0.6 * FPS)
    for i in range(drag_frames):
        t = i / (drag_frames - 1)
        finger = lerp((A_L + A_R) / 2, lerp(A_L, A_R, 0.7), t)
        st = empty_state()
        st.update(split_active=True, overlay_is_light=True, mask_x=finger, beam=1.0,
                  head_x=finger, head_r=BLOB_R, tail_x=A_L, tail_r=BLOB_R,
                  p=(finger - A_L) / (A_R - A_L), dragging=True, energy=1.0)
        st['label'] = 'الرفع بعد المنتصف يثبت الفاتح'
        frames.append((ms, st)); ms += 1000 // FPS

    # 5) تثبيت: رحلة الرأس EaseOutBack للمرسى + طفح القناع + ذوبان المصدر
    for i in range(int(1.35 * FPS)):
        ft = i / FPS
        head_t = min(1.0, ft / (HEAD_MS / 1000))
        sweep_t = min(1.0, ft / (SWEEP_MS / 1000))
        drain_t = min(1.0, ft / (DRAIN_MS / 1000))
        jig = max(0.0, 1 - max(0.0, ft - (HEAD_MS - 40) / 1000) / (JIGGLE_MS / 1000))
        st = empty_state()
        frac = fast_out_slow_in(sweep_t)
        end_b = W + GLOW_OUT   # sweepTargetX(true): التوهج بكامل انسيابه خارج الشاشة
        start_b = lerp(A_L, A_R, 0.7)
        st.update(split_active=True, overlay_is_light=True,
                  mask_x=lerp(start_b, end_b, frac), beam=1.0,
                  head_x=lerp(lerp(A_L, A_R, 0.7), A_R, ease_out_back(head_t)),
                  head_r=BLOB_R * (1 + 0.16 * max(0.0, 1 - abs(ft - HEAD_MS / 1000) / 0.35)),
                  tail_x=A_L, tail_r=BLOB_R * (1 - drain_t),
                  p=ease_out_back(head_t), energy=jig)
        st['label'] = 'ثبت الفاتح: الطفح يجتاح الشاشة والشمس تستقر يميناً برجفة حية'
        frames.append((ms, st)); ms += 1000 // FPS

    # 6) سكون فاتح 0.5s
    for i in range(int(0.5 * FPS)):
        st = empty_state()
        st.update(base_dark=False, head_x=A_R, head_r=BLOB_R, p=1.0)
        st['label'] = 'نهار — الشمس في مرساها اليمين'
        frames.append((ms, st)); ms += 1000 // FPS

    # 7) نقرة: القمر يعود والظلام يجتاح — رحلة معاكسة
    for i in range(int(1.35 * FPS)):
        ft = i / FPS
        head_t = min(1.0, ft / (HEAD_MS / 1000))
        sweep_t = min(1.0, ft / (SWEEP_MS / 1000))
        drain_t = min(1.0, ft / (DRAIN_MS / 1000))
        jig = max(0.0, 1 - max(0.0, ft - (HEAD_MS - 40) / 1000) / (JIGGLE_MS / 1000))
        st = empty_state()
        st.update(base_dark=False, split_active=True, overlay_is_light=False,
                  mask_x=lerp(A_R, -GLOW_OUT, fast_out_slow_in(sweep_t)), beam=1.0,
                  head_x=lerp(A_R, A_L, ease_out_back(head_t)),
                  head_r=BLOB_R * (1 + 0.16 * max(0.0, 1 - abs(ft - HEAD_MS / 1000) / 0.35)),
                  tail_x=A_R, tail_r=BLOB_R * (1 - drain_t),
                  p=1 - ease_out_back(head_t), energy=jig)
        st['label'] = 'نقرة = تبديل: القمر يهبط يساراً والليل يجتاح بنفس السائل'
        frames.append((ms, st)); ms += 1000 // FPS

    # 8) سكون مظلم 0.5s
    for i in range(int(0.5 * FPS)):
        st = empty_state()
        st['label'] = 'ليل — جاهز للسحب التالي'
        frames.append((ms, st)); ms += 1000 // FPS

    return frames


def main():
    frames = build_timeline()
    imgs = [render_frame(st, i / FPS) for i, (_, st) in enumerate(frames)]
    out = os.path.join(DOWNLOAD, 'noor_dial_preview.gif')
    imgs[0].save(out, save_all=True, append_images=imgs[1:], duration=int(1000 / FPS), loop=0)
    print(f'✅ معاينة المزلاج: {out} ({len(imgs)} إطاراً، {len(imgs)/FPS:.1f}ث)')


if __name__ == '__main__':
    main()
