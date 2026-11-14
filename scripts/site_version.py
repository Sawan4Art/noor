#!/usr/bin/env python3
"""كتابة docs/version.json — نسخة الموقع التي تبني صفحة نور المسلم رابطَ التحميل المباشر.

تُستدعى من build-release.yml بعد كل إصدارٍ جديد:
    python3 scripts/site_version.py <version> <versionCode> <apk-file-name>

فصارت صفحة الموقع تتبع آخر إصدارٍ آلياً ولا يبقى فيها رابطٌ مكتوبٌ يدوياً
لإصدارٍ يُمسحه التنظيفُ الآلي فيموت تحميل الموقع (درس بلاغ «الموقع مش شغال»:
أزرار الصفحة كانت شاردةً على v0.5.0-alpha40 المسحوب).
"""
import json
import sys
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TARGET = ROOT / "docs" / "version.json"


def main() -> int:
    if len(sys.argv) != 4:
        print("الاستعمال: site_version.py <version> <versionCode> <apk-file-name>", file=sys.stderr)
        return 2
    version, code, apk = sys.argv[1], int(sys.argv[2]), sys.argv[3]
    doc = {
        "version": version,
        "code": code,
        "apk": apk,
        "updated": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
    }
    TARGET.write_text(json.dumps(doc, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"docs/version.json → v{version} ({code}) / {apk}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
