#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
إكمال مكتبة الأحاديث بالترقيم المعتمد الكامل (alpha56):

بلاغ المالك: «في بعض كتب الاحاديث ناقصه فهي لا تبدا برقم واحد بعضها
يبدا ب الحديث رقم ٩٤ و كمان عايز تراجع الكتب الموجوده بخصوص ارقام
الاحاديث المعتمد» — والتحقق أثبت أن الترقيم القديم نفسه صحيحٌ (معتمد
من sunnah.com) لكن **النصوص ناقصة**: صحيح مسلم يبدأ من ٩٣ (ناقص
١..٩٢) وابن ماجه من ٢٦٧ والبخاري ناقصٌ ٢٨٦ رقماً — المصدر الأول
(AhmedBaset/hadith-json) لم يكن كاملاً.

الحل: الكتب السبعة الكبرى تُبنى من **fawazahmed0/hadith-api** مباشرة
— نفس مصدر الترقيم المعتمد الذي أُعيد الترقيم بمطابقته — وهو كاملٌ
متصل من ١ حتى آخر رقم معتمد (بخاري ٧٥٦٣، مسلم ٧٥٦٣، نسائي ٥٧٥٨،
أبو داود ٥٢٧٤، ترمذي ٣٩٥٦، ابن ماجه ٤٣٤١، موطأ ١٨٥٨):

- **الأرقام** تأتي من حقل hadithnumber حرفياً — بلا محاذاة نصية.
- **أبواب الكتب العربية** تُورَّث من الصفوف القديمة بمطابقة النص،
  وما لم يُطابق يرث بابَ جاره السابق (فالأبواب متصلة بالترتيب).
- **معرفات المفضلة محفوظة**: كل نصٍّ قديم يُعاد بمعرّفه القديم نفسه
  (مفضلة المستخدم لا تكسر)، والجديد كلياً يأخذ معرفاتٍ بعد أقصى القديم.
- الفهرس FTS4 يُبنى من جديد والتطبيع نفسه حرفياً (عقد التوافق مع
  HadithSearch.kt)، ونسخة القاعدة تُرفع إلى 3 فتبدّلها Room على الأجهزة.
- الأربعون النووية والقدسية ورياض الصالحين كما هي (كاملة أصلاً ١..٤٢/
  ١..٤٠/١..١٨٩٦ بترقيمها المعتمد).
"""
import json
import os
import re
import sqlite3
import unicodedata
import urllib.request

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DB = os.path.join(REPO, 'app/src/main/assets/databases/hadiths.db')
CACHE = '/tmp/hadith-api-cache'

# bookId في القاعدة ← إصدار fawazahmed0 المعتمد
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
DIGITS = {0x0660 + i: str(i) for i in range(10)}
DIGITS.update({0x06F0 + i: str(i) for i in range(10)})


def norm(s):
    """التطبيع المرجعي — حرفياً كما في build_hadith_db.py و HadithSearch.kt"""
    for a, b in LIG.items():
        s = s.replace(a, b)
    s = unicodedata.normalize('NFKC', s)
    s = STRIP.sub('', s)
    s = ''.join(LM.get(ch, ch) for ch in s)
    s = s.translate(DIGITS)
    return re.sub(r'\s+', ' ', s).strip()


def canonical(book_id):
    """حمل إصدار الكتاب — كاش محلي — وارجع قائمة (رقم معتمد، نص، عناوين)"""
    ed = EDS[book_id]
    path = os.path.join(CACHE, ed + '.min.json')
    if not os.path.exists(path):
        os.makedirs(CACHE, exist_ok=True)
        url = ('https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/'
               'editions/%s.min.json' % ed)
        with urllib.request.urlopen(url, timeout=180) as r, open(path, 'wb') as f:
            f.write(r.read())
    with open(path, encoding='utf-8') as f:
        d = json.load(f)
    out = []
    for h in d['hadiths']:
        num = h.get('hadithnumber')
        if isinstance(num, bool):
            continue
        if not isinstance(num, int):
            try:
                num = int(float(num))  # 4147.02 → 4147 (رواية موازية بالرقم نفسه)
            except (TypeError, ValueError):
                continue
        out.append((num, h['text']))
    return out


def main():
    conn = sqlite3.connect(DB)
    cur = conn.cursor()

    print('== اكتمال الكتب السبع من المصدر المعتمد ==')
    # المعرّفات عالمية على الجدول كله — الجديد يبدأ بعد أقصى معرّفٍ قائم
    # (كتب ١٣/١٠/١١ ما زالت صفوفها حاضرة أثناء الكتابة فلا يصطدم بها)
    global_next_id = cur.execute('SELECT MAX(id) FROM hadiths').fetchone()[0] + 1
    for book_id in sorted(EDS):
        canon = canonical(book_id)

        # ١) أبواب الصفوف القديمة بمفتاح النص المطبَّع
        old_chapters = {}
        for rid, chapter, text in cur.execute(
            'SELECT id, chapter, text FROM hadiths WHERE bookId=?', (book_id,)
        ).fetchall():
            old_chapters.setdefault(norm(text), chapter)

        # ٢) الصفوف الجديدة — الأرقام من المصدر مباشرة، والباب من القديم
        #    بمطابقة النص، وما لم يُطابق يرث بابَ ما قبله (والأول يتكئ على
        #    أول بابٍ معروف بعده — تمريرة ثانية عكسية)
        rows = []
        seen_ids = set()
        last_chapter = ''
        for num, text in canon:
            chapter = old_chapters.get(norm(text), '')
            if chapter:
                last_chapter = chapter
            rows.append([num, chapter, text])
        known = [r[1] for r in rows if r[1]]
        fallback = known[0] if known else ''
        filled = 0
        prev = fallback
        for r in rows:
            if not r[1]:
                r[1] = prev
                filled += 1
            prev = r[1]

        # ٣) المعرّفات — القديم لمن طابق نصّه (المفضلة لا تكسر) والجديد بعد الأقصى
        old_id_by_norm = {}
        for rid, text in cur.execute(
            'SELECT id, text FROM hadiths WHERE bookId=?', (book_id,)
        ).fetchall():
            old_id_by_norm.setdefault(norm(text), rid)
        next_id = global_next_id
        final_rows = []
        for num, chapter, text in rows:
            rid = old_id_by_norm.get(norm(text))
            if rid is None or rid in seen_ids:
                rid = next_id
                next_id += 1
            seen_ids.add(rid)
            final_rows.append((rid, book_id, num, chapter, text))

        # ٤) استبدال صفوف الكتاب
        cur.execute('DELETE FROM hadiths WHERE bookId=?', (book_id,))
        cur.executemany(
            'INSERT INTO hadiths(id, bookId, numberInBook, chapter, text) '
            'VALUES(?,?,?,?,?)',
            final_rows,
        )
        nums = sorted({r[2] for r in final_rows})
        gaps = [(a, b) for a, b in zip(nums, nums[1:]) if b != a + 1]
        fresh = next_id - global_next_id
        global_next_id = next_id
        print('book %-2d %-14s rows=%-5d min=%-5d max=%-5d gaps=%d filled_chapters=%d kept_ids=%d fresh_ids=%d' % (
            book_id, EDS[book_id], len(final_rows), nums[0], nums[-1],
            len(gaps), filled, len(final_rows) - fresh, fresh,
        ))
        if gaps:
            print('   note: canonical itself has shared/parallel numbers — gaps:', gaps[:5])
    conn.commit()

    # ===== فهرس FTS4 من جديد =====
    print('== rebuilding FTS4 index ==')
    cur.execute('DROP TABLE IF EXISTS hadiths_fts')
    cur.execute('DROP TABLE IF EXISTS hadiths_fts_segments')
    cur.execute('DROP TABLE IF EXISTS hadiths_fts_segdir')
    cur.execute('DROP TABLE IF EXISTS hadiths_fts_docsize')
    cur.execute('DROP TABLE IF EXISTS hadiths_fts_stat')
    cur.execute("CREATE VIRTUAL TABLE hadiths_fts USING fts4("
                "norm, tokenize=unicode61, content='')")
    for rid, text in cur.execute('SELECT id, text FROM hadiths ORDER BY id').fetchall():
        cur.execute('INSERT INTO hadiths_fts(docid, norm) VALUES(?,?)', (rid, norm(text)))
    conn.commit()

    # ===== ترقية نسخة القاعدة =====
    cur.execute('PRAGMA user_version=3')
    conn.commit()

    # ===== تحقق نهائي =====
    print('== verification ==')
    n_h = cur.execute('SELECT COUNT(*) FROM hadiths').fetchone()[0]
    n_f = cur.execute("SELECT COUNT(*) FROM hadiths_fts_docsize").fetchone()[0]
    assert n_h == n_f, (n_h, n_f)
    rows = cur.execute(
        'SELECT h.id FROM hadiths h JOIN hadiths_fts f ON f.docid = h.id '
        "WHERE hadiths_fts MATCH ? LIMIT 5", ('("الصلاه"*)',)).fetchall()
    assert rows, 'FTS4 match failed'
    # عقد المالك: كل كتاب يبدأ من رقم ١
    for book_id in sorted(EDS):
        mn, = cur.execute(
            'SELECT MIN(numberInBook) FROM hadiths WHERE bookId=?', (book_id,)).fetchone()
        assert mn == 1, (book_id, mn)
    # عقد البخاري: «كلمتان حبيبتان» رقمه ٧٥٦٣
    b = cur.execute(
        "SELECT numberInBook FROM hadiths WHERE bookId=1 AND text LIKE '%كَلِمَتَانِ حَبِيبَتَانِ%'"
    ).fetchone()
    print('spot check bukhari kelimatan ->', b)
    print('total hadiths:', n_h)
    conn.close()
    print('OK')


if __name__ == '__main__':
    main()
