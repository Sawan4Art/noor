#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
قصّ مكتبة الأحاديث إلى الكتب الخمسة المعتمدة (alpha58):

بلاغ المالك: «بقولك ايي يكفي فقط اهم خمس كتب من الاحاديث بدل عشره
لاكن يكونوا ترقيمهم صحيح و كاملين و بدون نقصان» — وبعهدٍ سابق:
«اخر ٣ كتب في الاحاديث حذفت بالكامل».

الواقع المُتحقَّق عليه قبل القصّ: جدول books فيه عشرة صفوف وآخرها
الثلاثة (رياض الصالحين، الأربعون النووية، الأربعون القدسية) بلا
حديثٍ واحدٍ منذ إعادة بناء alpha56/57 — فتظهر كتباً فارغة تماماً
= «حُذفت بالكامل»، وابن ماجه وموطأ بقيا من إصلاحٍ سبق.

القرار التنفيذي: المكتبة صارت **الكتب الخمسة** — صحيح البخاري، صحيح
مسلم، سنن أبي داود، جامع الترمذي، سنن النسائي — تُحذف الخمسة الباقية
(ابن ماجه، موطأ، والثلاثة الفارغة) مع أحاديثها، ويُبنى فهرس FTS4 من
جديد بالتطبيع المرجعي نفسه (العقد الحرفي مع HadithSearch.kt)، ونسخة
القاعدة تُرفع إلى ٥ فتبدّلها Room على الأجهزة (HadithDatabase.VERSION)،
ثم VACUUM فيرجع حجم الملف لنسِبته.

الترقيم لا يُمسّ — يُتحقَّق منه عدّياً قبل وبعد: كل كتابٍ أول=١ وآخره
الرقم المعتمد وفجواته صفر إلا مسلم ١٨٢٤/٢٤٨٣ الغائبان على sunnah.com
نفسه (مدمجان بجيرانهما — موثَّق لا مُختلَق).
"""

import os
import re
import sqlite3
import sys
import unicodedata

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DB = os.path.join(REPO, 'app/src/main/assets/databases/hadiths.db')

# الكتب الخمسة الباقية — (المعرف، العنوان، الترتيب على الرفّ)
KEEP = [
    (1, 'صحيح البخاري', 1),
    (2, 'صحيح مسلم', 2),
    (4, 'سنن أبي داود', 3),
    (5, 'جامع الترمذي', 4),
    (3, 'سنن النسائي', 5),
]
KEEP_IDS = {k[0] for k in KEEP}
KEEP_MAX = {1: 7563, 2: 3033, 4: 5274, 5: 3956, 3: 5758}

# الفجوة المعتمدة الوحيدة: مسلم ١٨٢٤ و٢٤٨٣ ليستا على sunnah.com نفسه
MUSLIM_ABSENT = {1824, 2483}
MUSLIM_BOOK_ID = 2

# ===== التطبيع المرجعي — حرفياً كما في rebuild_hadiths_official.py =====
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
    """التطبيع المرجعي — عقد التوافق مع HadithSearch.kt حرفياً"""
    for a, b in LIG.items():
        s = s.replace(a, b)
    s = unicodedata.normalize('NFKC', s)
    s = STRIP.sub('', s)
    s = ''.join(LM.get(ch, ch) for ch in s)
    s = s.translate(DIGITS)
    return re.sub(r'\s+', ' ', s).strip()


def verify(conn, stage, strict_books):
    """تحقق عدّي صارم على الكتب الخمسة: أول=١، آخره المعتمد، فجوات صفر
    إلا مسلم الغائبان. قبل القصّ يُفحص الترقيم وحده (جدول books ما زال
    فيه العشرة)؛ بعد القصّ يُفحص أيضاً أن الجدول صار الخمسة بترتيبهم."""
    cur = conn.cursor()
    ok = True
    rows = cur.execute(
        'SELECT id, title, sortOrder FROM books ORDER BY sortOrder').fetchall()
    if strict_books:
        ids = [r[0] for r in rows]
        wanted = [k[0] for k in KEEP]
        if ids != wanted:
            print(f'   [{stage}] الكتب: {ids} — المطلوب {wanted}', flush=True)
            ok = False
        for bid, _t, so in KEEP:
            so_db = next(r[2] for r in rows if r[0] == bid)
            if so_db != so:
                print(f'   [{stage}] ترتيب كتاب {bid} = {so_db} والمطلوب {so}',
                      flush=True)
                ok = False
    # أرقام الكتب الخمسة دفعةً واحدة — بلا استعلامٍ لكل رقم
    numbers = {}
    for bid, n in cur.execute(
            'SELECT bookId, numberInBook FROM hadiths WHERE bookId IN (%s)' %
            ','.join('?' * len(KEEP_IDS)), tuple(KEEP_IDS)):
        numbers.setdefault(bid, set()).add(n)
    for bid, title, _so in KEEP:
        nums = numbers.get(bid, set())
        if not nums:
            print(f'   [{stage}] {title}: بلا أحاديث!', flush=True)
            ok = False
            continue
        mn, mx = min(nums), max(nums)
        gaps = sorted(set(range(mn, mx + 1)) - nums)
        bad = [g for g in gaps if not (bid == MUSLIM_BOOK_ID and g in MUSLIM_ABSENT)]
        status = 'سليم' if (mn == 1 and mx == KEEP_MAX[bid] and not bad) else 'إشكال!'
        if status != 'سليم':
            ok = False
        print(f'   [{stage}] {title}: أول={mn} آخر={mx} أرقام={len(nums)} '
              f'فجوات={gaps if gaps else "لا شيء"} — {status}', flush=True)
    return ok


def main():
    if not os.path.isfile(DB):
        raise SystemExit(f'لا قاعدة في {DB}')
    conn = sqlite3.connect(DB)
    cur = conn.cursor()

    print('== ١) التحقق قبل القصّ ==', flush=True)
    if not verify(conn, 'قبل', strict_books=False):
        raise SystemExit('الترقيم قبل القصّ غير سليم — أصلحه أولاً')

    books_before = cur.execute('SELECT id, title FROM books ORDER BY sortOrder').fetchall()
    removed_books = [(b, t) for b, t in books_before if b not in KEEP_IDS]
    print('== ٢) المحذوفون ==', flush=True)
    for b, t in removed_books:
        n = cur.execute('SELECT COUNT(*) FROM hadiths WHERE bookId=?', (b,)).fetchone()[0]
        print(f'   {t} (id={b}) — {n} حديثاً', flush=True)

    print('== ٣) القصّ ==', flush=True)
    cur.execute('BEGIN')
    cur.execute('DELETE FROM hadiths WHERE bookId NOT IN (%s)' %
                ','.join('?' * len(KEEP_IDS)), tuple(KEEP_IDS))
    cur.execute('DELETE FROM books WHERE id NOT IN (%s)' %
                ','.join('?' * len(KEEP_IDS)), tuple(KEEP_IDS))
    # ترتيب الرفّ ١..٥ حرفياً
    for bid, _title, so in KEEP:
        cur.execute('UPDATE books SET sortOrder=? WHERE id=?', (so, bid))

    print('== ٤) فهرس FTS4 من جديد بالتطبيع المرجعي نفسه ==', flush=True)
    for t in ('hadiths_fts', 'hadiths_fts_segments', 'hadiths_fts_segdir',
              'hadiths_fts_docsize', 'hadiths_fts_stat'):
        cur.execute('DROP TABLE IF EXISTS "%s"' % t)
    cur.execute("CREATE VIRTUAL TABLE hadiths_fts USING fts4(norm, tokenize=unicode61, content='')")
    cur.executemany(
        'INSERT INTO hadiths_fts(docid, norm) VALUES (?,?)',
        [(r[0], norm(r[1])) for r in
         cur.execute('SELECT id, text FROM hadiths ORDER BY id').fetchall()],
    )

    print('== ٥) نسخة القاعدة ٥ ثم ضغط ==', flush=True)
    cur.execute('PRAGMA user_version = 5')
    cur.execute('COMMIT')
    conn.commit()
    cur.execute('VACUUM')
    conn.commit()

    print('== ٦) التحقق بعد القصّ ==', flush=True)
    if not verify(conn, 'بعد', strict_books=True):
        conn.close()
        raise SystemExit('فشل التحقق بعد القصّ — راجع الأرقام أعلاه')

    total = cur.execute('SELECT COUNT(*) FROM hadiths').fetchone()[0]
    fts = cur.execute('SELECT COUNT(*) FROM hadiths_fts_docsize').fetchone()[0]
    user_version = cur.execute('PRAGMA user_version').fetchone()[0]
    orphan = cur.execute(
        'SELECT COUNT(*) FROM hadiths h LEFT JOIN books b ON b.id=h.bookId '
        'WHERE b.id IS NULL').fetchone()[0]
    print(f'   الإجمالي: {total} حديثاً | فهرس: {fts} | نسخة: {user_version} | '
          f'يتامى: {orphan}')
    size_mb = os.path.getsize(DB) / (1024 * 1024)
    print(f'   حجم الملف: {size_mb:.2f} MB')
    conn.close()
    if total != fts or user_version != 5 or orphan != 0:
        raise SystemExit('فشل التحقق النهائي — راجع الأرقام أعلاه')
    print('اكتمل: الكتب الخمسة بترقيمها المعتمد الكامل.')


if __name__ == '__main__':
    sys.exit(main())
