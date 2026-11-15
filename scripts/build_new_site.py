#!/usr/bin/env python3
"""تجميع الموقع المعاد تصميمه — يدمج الأجزاء الثلاثة (head/body/js) ويورّث
من التصميم السابق ما لا تغيّره إعادةُ التصميم: شاشاتُ الهاتف المرسومة بالـ
CSS كاملةً (سبع شاشاتٍ بلا أي ملفات صور) وقواعدُ CSS المرافقة لها —
فالتصميم جديدٌ والشاشاتُ نفسُها بواقعية التطبيق، مع تحديث نصّ الإصدار
العتيق («0.5.0-alpha40») إلى إشارةٍ حيّة تملأها الصفحة من version.json.

الاستعمال: python3 scripts/build_new_site.py <dir-of-parts>
الأجزاء: head.html body.html js.html — والـ body فيه __SLIDES__ والـ head
فيه __MOCKUP_CSS__ ثم يُكتَب docs/index.html.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OLD = ROOT / "docs" / "index.html"


def main() -> int:
    if len(sys.argv) != 2:
        print("الاستعمال: build_new_site.py <dir-of-parts>", file=sys.stderr)
        return 2
    parts_dir = Path(sys.argv[1])
    head = (parts_dir / "head.html").read_text(encoding="utf-8")
    body = (parts_dir / "body.html").read_text(encoding="utf-8")
    js = (parts_dir / "js.html").read_text(encoding="utf-8")
    old = OLD.read_text(encoding="utf-8")

    # قواعد CSS لشاشات الهاتف المرسومة — من إطار الهاتف حتى قبل قسم التحميل
    m = re.search(r"/\* ===== إطار الهاتف ===== \*/(.*?)/\* ===== التحميل ===== \*/", old, re.S)
    if not m:
        print("لم أجد قواعد CSS لشاشات الهاتف في الملف القديم", file=sys.stderr)
        return 1
    mockup_css = m.group(1).rstrip() + "\n"

    # شرائح الشاشات — سبع <figure class="slide"> كاملة
    slides = re.findall(r'<figure class="slide">.*?</figure>', old, re.S)
    if len(slides) != 7:
        print(f"عدد الشرائح غير المتوقع: {len(slides)} (المتوقع ٧)", file=sys.stderr)
        return 1
    slides_html = "\n".join(slides)
    # نص الإصدار العتيق داخل شاشة الإعدادات — إشارةٌ حيّة تملأها الصفحة
    slides_html = slides_html.replace(
        '<div class="p-ver">الإصدار 0.5.0-alpha40</div>',
        '<div class="p-ver">الإصدار <span class="js-appver">1.78</span></div>',
    )

    if "__MOCKUP_CSS__" not in head or "__SLIDES__" not in body:
        print("العناصر البديلة مفقودة في الأجزاء", file=sys.stderr)
        return 1
    html = head.replace("__MOCKUP_CSS__", mockup_css) + body.replace("__SLIDES__", slides_html) + js
    (ROOT / "docs" / "index.html").write_text(html, encoding="utf-8")
    print(f"docs/index.html — {len(html)//1024} ك.ب — شرائح: {len(slides)} — CSS شاشات: {len(mockup_css)//1024} ك.ب")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
