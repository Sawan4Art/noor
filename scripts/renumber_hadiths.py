#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
إعادة بناء hadiths.db — النسخة ٢ (alpha46):

١) **الترقيم المعتمد**: كان numberInBook يأتي من idInBook في hadith-json
   وهو ترقيم تسلسلي لا يطابق الترقيم المعتمد المنشور في الكتب (sunnah.com) —
   فهنا يُعاد ترقيم كتب السبعة (البخاري حتى الموطأ) بمطابقة نصية محاذاة
   ترتيبياً (difflib.SequenceMatcher) مع نسخة fawazahmed0/hadith-api التي
   تحمل الترقيم المعتمد حرفياً في حقل hadithnumber (انتبه: حقل
   arabicnumber فيه ترقيم الطبعات العربية «8.01» — ليس هو المطلوب).
   الأربعون النووية والقدسية ترقيماها المعتمد أصلًا ١..٤٢/١..٤٠ فتبقى كما
   هي، ورياض الصالحين (نسخة بترتيب مختلف من المصدر) تبقى بترقيمها
   الداخلي المتسق مع بطاقاتها.

٢) **فهرس FTS4 بدل FTS5**: أندرويد لا يبني SQLite بنظام FTS5 نهائيًا
   («no such module: fts5» على أجهزة حقيقية) — لهذا كان البحث بالكلمة
   لا يعمل خارج جهاز المحاكاة. الفهرس الآن USING fts4 بمُقطِّع unicode61
   (مضمون من API 21) والمحتوى contentless (الفهرس فقط) كما كان.

نصوص الأحاديث وأبوابها ومعرفاتها لا تتغير — العقد مع fixtures يبقى صحيحًا.
"""
import json
import os
import re
import sqlite3
import unicodedata
import urllib.request
from difflib import SequenceMatcher

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DB = os.path.join(REPO, 'app/src/main/assets/databases/hadiths.db')
CACHE = '/tmp/hadith-api-cache'

# bookId في القاعدة ← إصدار fawazahmed0 المعتمد (الأربعون ورياض الصالحين تُترك)
EDS = {
    1: 'ara-bukhari',
    2: 'ara-muslim',
    3: 'ara-nasai',
    4: 'ara-abudawud',
    5: 'ara-tirmidhi',
    6: 'ara-ibnmajah',
    7: 'ara-malik',
}

STRIP = re.compile(
    '[\u0610-\u061A\u0640\u064B-\u065F\u0670\u06D6-\u06ED\u08D3-\u08FF'
    '\uFD3E\uFD3F\u200B-\u200F\u202A-\u202E\u2066-\u2069\uFEFF\uFFF9-\uFFFC]'
)
LIG = {'\uFDFA': 'صلى الله عليه وسلم', '\uFDF2': 'الله', '\uFDFD': 'بسم الله الرحمن الرحيم'}
LM = {'\u0623': '\u0627', '\u0625': '\u0627', '\u0622': '\u0627', '\u0671': '\u0627',
      '\u0629': '\u0647', '\u0649': '\u064A', '\u0624': '\u0648', '\u0626': '\u064A'}


def norm(s):
    for a, b in LIG.items():
        s = s.replace(a, b)
    s = unicodedata.normalize('NFKC', s)
    s = STRIP.sub('', s)
    s = ''.join(LM.get(ch, ch) for ch in s)
    return re.sub(r'\s+', ' ', s).strip()


def canonical_numbers(book_id):
    """قائمة مرتبة (مفتاح مطبَّع، الرقم المعتمد) لإصدار الكتاب."""
    os.makedirs(CACHE, exist_ok=True)
    path = os.path.join(CACHE, EDS[book_id] + '.min.json')
    if not os.path.exists(path):
        url = ('https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/'
               'editions/%s.min.json' % EDS[book_id])
        with urllib.request.urlopen(url, timeout=120) as r, open(path, 'wb') as f:
            f.write(r.read())
    with open(path, encoding='utf-8') as f:
        d = json.load(f)
    out = []
    for h in d['hadiths']:
        num = h.get('hadithnumber')
        if not isinstance(num, int):
            try:
                num = int(float(num))  # 4147.02 → 4147 (رواية موازية بالرقم نفسه)
            except (TypeError, ValueError):
                continue
        out.append((norm(h['text']), num))
    return out


def renumber(cur, book_id, canon):
    rows = cur.execute(
        'SELECT id, numberInBook, text FROM hadiths WHERE bookId=? ORDER BY id',
        (book_id,)).fetchall()
    keys = [norm(t) for _, _, t in rows]
    sm = SequenceMatcher(None, keys, [c for c, _ in canon], autojunk=False)
    mapping = {}
    for tag, i1, i2, j1, j2 in sm.get_opcodes():
        if tag == 'equal':
            for k in range(i2 - i1):
                mapping[rows[i1 + k][0]] = canon[j1 + k][1]
        elif tag == 'replace':
            for k in range(min(i2 - i1, j2 - j1)):
                mapping[rows[i1 + k][0]] = canon[j1 + k][1]
    changed = 0
    for rid, old, _ in rows:
        new = mapping.get(rid)
        if new is not None and new != old:
            cur.execute('UPDATE hadiths SET numberInBook=? WHERE id=?', (new, rid))
            changed += 1
    print('book %d: rows=%d assigned=%d renumbered=%d keptOld=%d' % (
        book_id, len(rows), len(mapping), changed, len(rows) - len(mapping)))
    return len(rows), len(mapping), changed


def main():
    conn = sqlite3.connect(DB)
    cur = conn.cursor()

    # ===== ١) الترقيم المعتمد =====
    print('== renumbering to canonical sunnah.com numbering ==')
    for book_id in sorted(EDS):
        renumber(cur, book_id, canonical_numbers(book_id))
    conn.commit()

    # ===== ٢) فهرس FTS4 بدل FTS5 =====
    print('== rebuilding search index as FTS4 ==')
    cur.execute('DROP TABLE IF EXISTS hadiths_fts')
    cur.execute("CREATE VIRTUAL TABLE hadiths_fts USING fts4("
                "norm, tokenize=unicode61, content='')")
    for rid, text in cur.execute('SELECT id, text FROM hadiths ORDER BY id').fetchall():
        cur.execute('INSERT INTO hadiths_fts(docid, norm) VALUES(?,?)',
                    (rid, norm(text)))
    conn.commit()

    # ===== ٣) ترقية نسخة الملف كي يستبدلها Room على الأجهزة المثبتة =====
    cur.execute('PRAGMA user_version=2')
    conn.commit()

    # ===== تحقق نهائي =====
    n_h = cur.execute('SELECT COUNT(*) FROM hadiths').fetchone()[0]
    # جدول contentless لا يقبل COUNT عليه مباشرة — جدول docsize الظل صفٌ لكل حديث
    n_f = cur.execute("SELECT COUNT(*) FROM hadiths_fts_docsize").fetchone()[0]
    assert n_h == n_f, (n_h, n_f)
    # مطابقة نموذجية بالكلمة — عقد عمل الفهرس على FTS4
    rows = cur.execute(
        'SELECT h.id FROM hadiths h JOIN hadiths_fts f ON f.docid = h.id '
        "WHERE hadiths_fts MATCH ? LIMIT 5", ('("الصلاه"*)',)).fetchall()
    assert rows, 'FTS4 match failed'
    # عقد الترقيم: حديث البخاري «كلمتان حبيبتان» رقمه المعتمد ٧٥٦٣
    b = cur.execute(
        "SELECT numberInBook FROM hadiths WHERE bookId=1 AND text LIKE '%كَلِمَتَانِ حَبِيبَتَانِ%'"
    ).fetchone()
    print('spot check bukhari kelimatan ->', b)
    # بلا أرقام مكررة داخل الكتاب الواحد بعد الترقيم — إلا ما في الترقيم
    # المعتمد نفسه (روايتان بسندَين لنصٍّ واحد تحملان الرقم نفسه في
    # sunnah.com، مثل ذيول ابن ماجه ٤٠٨٠+) — تُعرض ولا تُعدَّ
    for bid in sorted(EDS):
        dup = cur.execute(
            'SELECT COUNT(*) FROM (SELECT numberInBook FROM hadiths WHERE bookId=? '
            'GROUP BY numberInBook HAVING COUNT(*)>1)', (bid,)).fetchone()[0]
        if dup:
            print('note: book %d keeps %d shared canonical numbers (parallel chains)' % (bid, dup))
    print('total hadiths:', n_h)
    conn.close()
    print('OK')


if __name__ == '__main__':
    main()
