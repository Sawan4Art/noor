#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
نص إصدار GitHub — نور المسلم
يستخرج قسم الوسم المطلوب من CHANGELOG.md ويبني نص الإصدار الكامل
(الترويسة الثابتة + ما الجديد + ملاحظات ألفا) — مصدر واحد يقرؤه
الويب والتطبيق معاً في ملخص «ما الجديد».
الاستخدام: python3 scripts/release_body.py v0.5.0-alpha6 > release_body.md
"""
import re
import sys
from pathlib import Path

HEADER = "## نور المسلم {tag}"
SADAQA = (
    "> هذا التطبيق صدقة جارية على روح المرحوم بأذن الله أبي والد المالك "
    "— تقبل الله منا ومنكم"
)

RELEASE_NOTES = (
    "- تحديث رسمي موقّع — إن ظهر تحذير التثبيت فاسمح بتثبيت التطبيقات من هذا المصدر\n"
    "- تطبيق واحد وملف واحد: هذا الملف يتثبت مباشرةً فوق النسخة الموجودة عندك\n"
    "- لو ظهر التطبيق الجديد كأيقونة إضافية جنب القديم — دي مرة واحدة بس: "
    "افتح الجديد وامسح القديم من الهاتف، وكل التحديثات بعدها تتثبت على طول"
)
FALLBACK = "إصلاحات وتحسينات عامة"


def extract_section(changelog: str, tag: str) -> str | None:
    """قسم الوسم المطلوب حتى أول «## » تالية — أو None إن لم يوجد."""
    pattern = re.compile(rf"^## {re.escape(tag)}\b[^\n]*$", re.MULTILINE)
    match = pattern.search(changelog)
    if not match:
        return None
    start = match.end()
    nxt = re.compile(r"^## ", re.MULTILINE).search(changelog, start)
    body = changelog[start : nxt.start()] if nxt else changelog[start:]
    body = body.strip()
    return body or None


def main() -> int:
    if len(sys.argv) != 2:
        print("الاستخدام: release_body.py <tag>", file=sys.stderr)
        return 2
    tag = sys.argv[1]

    changelog_path = Path("CHANGELOG.md")
    changelog = (
        changelog_path.read_text(encoding="utf-8")
        if changelog_path.exists()
        else ""
    )
    section = extract_section(changelog, tag) or FALLBACK
    if section == FALLBACK:
        print(f"تحذير: لا قسم للوسم {tag} في CHANGELOG.md — استُخدم النص الاحتياطي",
              file=sys.stderr)

    print(f"{HEADER.format(tag=tag)}\n\n{SADAQA}\n\n## ما الجديد\n{section}\n\n{RELEASE_NOTES}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
