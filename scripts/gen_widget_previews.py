#!/usr/bin/env python3
"""توليد صور معاينة الويدجت (previewImage) عبر متصفح حقيقي — نور المسلم v0.4.
بطلب المالك: الويدجت كان يظهر فارغاً في قائمة الويدجتس على أندرويد ≤11.
المتصفح يضمن تشكيل الحروف العربية بشكل سليم (PIL لا يمدّ الحروف).
"""
import os
from playwright.sync_api import sync_playwright

RES = os.path.join(os.path.dirname(__file__), '..', 'android', 'app', 'src', 'main', 'res')
W, H = 500, 220

HTML = """<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body { width: 500px; height: 220px; background: transparent; font-family: 'DejaVu Sans', sans-serif; }
  .widget {
    width: 100%; height: 100%;
    border-radius: 26px;
    background: __BG__;
    border: 2px solid rgba(6,185,129,0.25);
    padding: 18px 22px;
    display: flex; flex-direction: column; justify-content: space-between;
    direction: rtl;
  }
  .header { display: flex; justify-content: space-between; align-items: center; }
  .brand { color: #6ee7b7; font-size: 17px; font-weight: bold; }
  .city { color: #a7f3d0; font-size: 12px; }
  .next { display: flex; justify-content: space-between; align-items: baseline; }
  .next-name { color: #ffffff; font-size: 30px; font-weight: 800; }
  .next-time { color: #6ee7b7; font-size: 24px; font-weight: bold; }
  .countdown { text-align: center; color: #e2e8f0; font-size: 15px; }
  .row { text-align: center; color: #94a3b8; font-size: 10.5px; white-space: nowrap; }
</style>
</head>
<body>
  <div class="widget" style="__BG__">
    <div class="header">
      <span class="brand">🕌 نور المسلم</span>
      <span class="city">القاهرة</span>
    </div>
    <div class="next">
      <span class="next-name">الفجر</span>
      <span class="next-time">05:00</span>
    </div>
    <div class="countdown">المتبقي 02:14:33</div>
    <div class="row">الفجر 05:00 • الظهر 11:54 • العصر 03:15 • المغرب 05:50 • العشاء 07:12</div>
  </div>
</body>
</html>
"""


def render(with_bg: bool, out: str):
    bg = (
        'background: linear-gradient(rgba(1,22,15,0.90), rgba(1,22,15,0.92));'
        if with_bg
        else 'background: rgba(0,0,0,0.55);'  # شفاف ظل خفيف لإظهار النص في المعاينة
    )
    html = HTML.replace('__BG__', bg)
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={'width': W, 'height': H}, device_scale_factor=2)
        page.set_content(html)
        page.wait_for_timeout(150)
        page.screenshot(path=out, omit_background=not with_bg)
        browser.close()
    print('wrote', out)


os.makedirs(os.path.join(RES, 'drawable'), exist_ok=True)
render(True, os.path.join(RES, 'drawable', 'widget_preview.png'))
render(False, os.path.join(RES, 'drawable', 'widget_preview_transparent.png'))
