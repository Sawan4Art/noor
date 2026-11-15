#!/usr/bin/env python3
"""تجهيز أصول تنزيل الموقع — تقطيع APK وإصدار docs/version.json.

تُستدعى من build-release.yml بعد كل إصدارٍ جديد ومن النشر اليدوي:
    python3 scripts/build_site_assets.py <version> <versionCode> <apk-path> <out-dir>

الناتج في <out-dir>:
    parts/noor-almuslim-<version>.part-NN  أجزاءٌ ≤ ٩٠ م.ب (حد جت هب للملف ١٠٠ م.ب
                                           والـ APK يتجاوزه فلا يُخزَّن كاملاً)
    version.json                          تُنسخ إلى docs/ فيخدمها GitHub Pages

درس بلاغ «الموقع مش شغال»: أزرار الصفحة كانت شاردةً على إصدارٍ مكتوبٍ يدوياً
يُمسحه التنظيفُ الآلي فيموت التحميل، ثم بلاغ لقطة الشاشة: جت هب نفسه يحجز
صفحاته بشاشة إلزام التحقق بخطوتين للحسابات المسجَّلة دخولاً فأي رابطٍ يعدي من
github.com يُوقَف. العلاج: الأجزاء على فرعٍ مستقلٍّ site-assets تُخدَم من
raw.githubusercontent.com (CDN مجهول بلا صفحةِ دخولٍ وبالترويسة
Access-Control-Allow-Origin: *) والصفحة تجمعها وتتحقق ببصمة SHA-256 قبل
إعطاء الملف للمتصفح — وأسماء الأجزاء بالنسخة كي لا تخدمها ذاكرةُ CDN قديمةً
بعد كل نشر.
"""
import hashlib
import json
import sys
from datetime import datetime, timezone
from pathlib import Path

PART_BYTES = 90 * 1024 * 1024  # 94,371,840 — دون حد جت هب (١٠٠ م.ب) بهامش
RAW_BASE = "https://raw.githubusercontent.com/Sawan4Art/noor/site-assets/parts"


def main() -> int:
    if len(sys.argv) != 5:
        print("الاستعمال: build_site_assets.py <version> <versionCode> <apk-path> <out-dir>", file=sys.stderr)
        return 2
    version, code, apk_path, out_dir = sys.argv[1], int(sys.argv[2]), Path(sys.argv[3]), Path(sys.argv[4])
    if not apk_path.is_file():
        print(f"ملف الـ APK غير موجود: {apk_path}", file=sys.stderr)
        return 1
    (out_dir / "parts").mkdir(parents=True, exist_ok=True)

    data = apk_path.read_bytes()
    total = len(data)
    sha256 = hashlib.sha256(data).hexdigest()

    parts = []
    index = 0
    for start in range(0, total, PART_BYTES):
        name = f"noor-almuslim-{version}.part-{index:02d}"
        chunk = data[start:start + PART_BYTES]
        (out_dir / "parts" / name).write_bytes(chunk)
        parts.append({"url": f"{RAW_BASE}/{name}", "bytes": len(chunk)})
        index += 1

    doc = {
        "version": version,
        "code": code,
        "apk": apk_path.name,
        "size": total,
        "sha256": sha256,
        "parts": parts,
        "updated": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
    }
    (out_dir / "version.json").write_text(json.dumps(doc, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"أجزاء: {len(parts)} — الحجم {total} — sha256 {sha256[:16]}…")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
