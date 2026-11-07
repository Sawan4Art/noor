#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
سدّ نصوص الأحاديث الفارغة من sunnah.com (alpha56 — مكمّل rebuild):

المصدر المعتمد fawazahmed0 يترك نصوص بعض الأرقام فارغةً (مقدمة صحيح
مسلم كلها ١..٩٢ وبعض أرقام بقية الكتب ~٢٠٠) — وهذه النصوص حاضرة على
sunnah.com صفحةً صفحة. هذا السكربت يجلب كل رقمٍ فارغ من صفحته المعتمدة
(/muslim:1 …) ويملأ نصَّه وبابَه، ثم يعيد بناء الفهرس ويتحقق:

- لا يُخترع نصٌّ أبداً — ما تعذّر جلبه يُحذف صفُّه (لا بطاقات فارغة).
- الأرقام الكسرية (391.2 روايات موازية) بلا صفحاتٍ فردية تُحذف أيضاً.
- الفهرس FTS4 يُبنى من جديد والنسخة 3 (بعد rebuild_hadiths_complete.py).
"""
import json
import os
import re
import sqlite3
import time
import unicodedata
import urllib.request
from concurrent.futures import ThreadPoolExecutor

CACHE_DIR = '/tmp/sunnah_pages' 

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DB = os.path.join(REPO, 'app/src/main/assets/databases/hadiths.db')
UA = 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/120 Safari/537.36'

# bookId ← مسار sunnah.com
PATHS = {
    1: 'bukhari', 2: 'muslim', 3: 'nasai', 4: 'abudawud',
    5: 'tirmidhi', 6: 'ibnmajah', 7: 'malik',
}

STRIP = re.compile(
    '[\u0610-\u061A\u0640\u064B-\u065F\u0670\u06D6-\u06ED\u08D3-\u08FF'
    '\uFD3E\uFD3F\u200B-\u200F\u202A-\u202E\u2066-\u2069\uFEFF\uFFF9-\uFFFC]'
)
LIG = {'\uFDFA': 'صلى الله عليه وسلم', '\uFDF2': 'الله', '\uFDFD': 'بسم الله الرحمن الرحيم'}
LM = {'\u0623': '\u0627', '\u0625': '\u0627', '\u0622': '\u0627', '\u0671': '\u0627',
      '\u0629': '\u0647', '\u0649': '\u064A', '\u0624': '\u0648', '\u0626': '\u064A'}
DIGITS = {0x0660 + i: str(i) for i in range(10)}
DIGITS.update({0x06F0 + i: str(i) for i in range(10)})


def norm(s):
    for a, b in LIG.items():
        s = s.replace(a, b)
    s = unicodedata.normalize('NFKC', s)
    s = STRIP.sub('', s)
    s = ''.join(LM.get(ch, ch) for ch in s)
    s = s.translate(DIGITS)
    return re.sub(r'\s+', ' ', s).strip()


ARABIC_RE = re.compile(r'class="arabic_hadith_full[^"]*"[^>]*>(.*?)</div>', re.S)
TAGS_RE = re.compile(r'<[^>]+>')
CHAPTER_RE = re.compile(r'class="arabic_chapter"[^>]*>(.*?)</a>', re.S)


def fetch(path):
    """جلب صفحة حديثٍ من sunnah.com — نصها العربي وبابها — أو None
    (بكاشٍ محلي في /tmp كي لا تضرب الشبكة في كل إعادة)"""
    os.makedirs(CACHE_DIR, exist_ok=True)
    safe = path.replace(':', '_').replace('/', '_')
    cache = os.path.join(CACHE_DIR, safe + '.html')
    html = None
    if os.path.exists(cache):
        html = open(cache, encoding='utf-8', errors='ignore').read()
    else:
        url = 'https://sunnah.com/%s' % path
        for attempt in range(3):
            try:
                req = urllib.request.Request(url, headers={'User-Agent': UA})
                with urllib.request.urlopen(req, timeout=30) as r:
                    html = r.read().decode('utf-8', 'ignore')
                open(cache, 'w', encoding='utf-8').write(html)
                break
            except Exception:
                html = None
                time.sleep(1.0 * (attempt + 1))
    if not html:
        return None
    m = ARABIC_RE.search(html)
    if not m:
        return None
    text = TAGS_RE.sub('', m.group(1)).strip()
    c = CHAPTER_RE.search(html)
    chapter = TAGS_RE.sub('', c.group(1)).strip() if c else ''
    return text, chapter


def fetch_many(jobs, workers=8):
    """جلب متوازٍ لكل الصفحات — (rid, bookId, num) ← نتيجتها"""
    results = {}
    def one(job):
        rid, book_id, num = job
        return job, fetch('%s:%d' % (PATHS[book_id], num))
    with ThreadPoolExecutor(max_workers=workers) as pool:
        for job, res in pool.map(one, jobs):
            results[job] = res
    return results


def main():
    conn = sqlite3.connect(DB)
    cur = conn.cursor()
    empties = cur.execute(
        'SELECT id, bookId, numberInBook FROM hadiths '
        'WHERE TRIM(text) = "" ORDER BY bookId, numberInBook'
    ).fetchall()
    print('empty rows to fill:', len(empties))

    jobs = [e for e in empties
            if PATHS.get(e[1]) is not None and isinstance(e[2], int) and e[2] >= 1]
    fractional = len(empties) - len(jobs)
    print('fetching %d pages in parallel (cache: %s)…' % (len(jobs), CACHE_DIR))
    results = fetch_many(jobs)

    filled, missing = 0, 0
    for (rid, book_id, num), got in results.items():
        if got is None or not got[0].strip():
            missing += 1
            cur.execute('DELETE FROM hadiths WHERE id=?', (rid,))
            if got is None:
                print('  MISS', PATHS[book_id], num)
            else:
                print('  BLANK', PATHS[book_id], num)
            continue
        text, chapter = got
        cur.execute(
            'UPDATE hadiths SET text=?, chapter=? WHERE id=?',
            (text, chapter or "", rid),
        )
        filled += 1
    for rid, book_id, num in empties:
        if (rid, book_id, num) not in results:
            cur.execute('DELETE FROM hadiths WHERE id=?', (rid,))
    conn.commit()
    print('filled=%d deleted_missing=%d deleted_fractional=%d' % (filled, missing, fractional))

    # إعادة بناء الفهرس بكل النصوص الجديدة
    print('== rebuilding FTS4 ==')
    cur.execute('DROP TABLE IF EXISTS hadiths_fts')
    cur.execute('DROP TABLE IF EXISTS hadiths_fts_segments')
    cur.execute('DROP TABLE IF EXISTS hadiths_fts_segdir')
    cur.execute('DROP TABLE IF EXISTS hadiths_fts_docsize')
    cur.execute('DROP TABLE IF EXISTS hadiths_fts_stat')
    cur.execute("CREATE VIRTUAL TABLE hadiths_fts USING fts4("
                "norm, tokenize=unicode61, content='')")
    for rid, text in cur.execute('SELECT id, text FROM hadiths ORDER BY id').fetchall():
        cur.execute('INSERT INTO hadiths_fts(docid, norm) VALUES(?,?)', (rid, norm(text)))
    cur.execute('PRAGMA user_version=3')
    conn.commit()

    # تحقق
    empt = cur.execute('SELECT COUNT(*) FROM hadiths WHERE TRIM(text)=""').fetchone()[0]
    assert empt == 0, empt
    for book_id in sorted(PATHS):
        mn, mx, n = cur.execute(
            'SELECT MIN(numberInBook), MAX(numberInBook), COUNT(*) FROM hadiths '
            'WHERE bookId=?', (book_id,)).fetchone()
        print('book %-2d rows=%-5d min=%-4d max=%-4d' % (book_id, n, mn, mx))
    b = cur.execute(
        "SELECT numberInBook FROM hadiths WHERE bookId=1 AND text LIKE '%كَلِمَتَانِ حَبِيبَتَانِ%'"
    ).fetchone()
    print('spot bukhari kelimatan ->', b)
    m1 = cur.execute("SELECT substr(text,1,60) FROM hadiths WHERE bookId=2 AND numberInBook=1").fetchone()
    print('spot muslim #1 ->', m1)
    conn.close()
    print('OK')


if __name__ == '__main__':
    main()
