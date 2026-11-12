#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
قانون الإصدارات (أمر المالك 2026-11-13): آخر خمسة إصداراتٍ فقط —
الأقدم يُحذف تلقائياً بوسمه بعد كل إصدارٍ جديد.

يُستدعى من الـ workflow بعد نشر الـ Release (GITHUB_TOKEN + REPO من
البيئة)، ويُستدعى يدوياً أيضاً بمتغيرَي GITHUB_TOKEN و REPO.

القواعد:
- الإصدارات تُرتَّب بلحظة الإنشاء تنازلياً ويُبقى أحدث خمسة.
- ما دونها يُحذف (الإصدار ثم وسمه) — ولو سقط حذفُ وسمٍ يُكمل الباقي.
- المسودات تُحذف أيضاً إن تجاوزت العدد (الإصدار المحذوف لا يحتاج نشراً).
"""
import json
import os
import sys
import urllib.request
import urllib.error

KEEP = int(os.environ.get("KEEP", "5"))
REPO = os.environ.get("REPO", "")
TOKEN = os.environ.get("GITHUB_TOKEN", "")

API = "https://api.github.com"


def api(method: str, path: str) -> dict | list | None:
    """نداء API واحد — DELETE يرجع None عند 204، والخطأ يرفع استثناءً."""
    req = urllib.request.Request(f"{API}{path}", method=method)
    req.add_header("Authorization", f"token {TOKEN}")
    req.add_header("Accept", "application/vnd.github+json")
    req.add_header("User-Agent", "noor-release-pruner")
    try:
        with urllib.request.urlopen(req) as res:
            body = res.read()
            return json.loads(body) if body else None
    except urllib.error.HTTPError as e:
        if e.code == 404:
            return None  # محذوفٌ سلفاً أو بلا وسم — يُكمل الباقي
        raise


def all_releases() -> list[dict]:
    out: list[dict] = []
    page = 1
    while True:
        batch = api("GET", f"/repos/{REPO}/releases?per_page=100&page={page}")
        if not isinstance(batch, list) or not batch:
            break
        out.extend(batch)
        if len(batch) < 100:
            break
        page += 1
    return out


def main() -> int:
    if not REPO or not TOKEN:
        print("مطلوب GITHUB_TOKEN و REPO في البيئة", file=sys.stderr)
        return 2
    releases = all_releases()
    # الأحدث أولاً بلحظة الإنشاء ثم الوسم كسر التعادل
    releases.sort(key=lambda r: (r.get("created_at") or "", r.get("tag_name") or ""), reverse=True)
    keep = releases[:KEEP]
    drop = releases[KEEP:]
    print(f"الإصدارات الكل: {len(releases)} — يُبقى {len(keep)}: "
          + " ".join(r["tag_name"] for r in keep))
    deleted = 0
    for r in drop:
        tag = r["tag_name"]
        rid = r["id"]
        ok_release = False
        ok_tag = False
        try:
            api("DELETE", f"/repos/{REPO}/releases/{rid}")
            ok_release = True
        except Exception as e:
            print(f"تعذّر حذف الإصدار {tag}: {e}", file=sys.stderr)
        try:
            api("DELETE", f"/repos/{REPO}/git/refs/tags/{tag}")
            ok_tag = True
        except Exception as e:
            print(f"تعذّر حذف وسم {tag}: {e}", file=sys.stderr)
        if ok_release:
            deleted += 1
            print(f"حُذف: {tag}" + (" مع وسمه" if ok_tag else " (الوسم باقٍ)"))
    print(f"انتهى — حُذف {deleted} إصداراً وبقي {KEEP} كما هو القانون")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
