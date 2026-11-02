#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
بناء databases/hadiths.db لمكتبة الأحاديث (نور المسلم)
- المصدر: AhmedBaset/hadith-json v1.2.0 (من sunnah.com) — ١٠ كتب مختارة
- المخطط: جداول Room (books + hadiths) + فهرس FTS5 contentless بنص مُطبَّع
- التطبيع (Normalization) هنا هو المرجع الحرفي — وتنفيذه في Kotlin
  (domain/hadith/HadithSearch.kt) يجب أن يطابقه حرفياً، وعقد اختبار
  fixtures يضمن التطابق (app/src/test/resources/hadith_normalization_fixtures.json)

خطوات التطبيع (بترتيبها الحرفي في كلتا اللغتين):
  1) NFKC (يفكك ﷺ و﷽ وأشكال العرض ويُركّب الهمزات المفككة)
  2) حذف: التشكيل U+064B-065F، علامات قرآنية U+0610-061A/U+06D6-06ED/U+08D3-08FF،
     التطويل U+0640، ألف خنجرية U+0670، زخارف FD3E/FD3F، علامات اتجاه وصفرية العرض
  3) توحيد الحروف: أإآٱ→ا ، ة→ه ، ى→ي ، ؤ→و ، ئ→ي (والهمزة المفردة ء تبقى)
  4) توحيد الأرقام العربية/الفارسية → 0-9
"""
import json
import os
import re
import sqlite3
import unicodedata

# المسارات — تُشتق من مكان السكريبت داخل الريبو ويمكن تجاوزها بالبيئة:
#   HADITH_JSON_SRC  مجلد db/by_book في نسخة hadith-json v1.2.0
#     git clone --depth 1 --branch v1.2.0 https://github.com/AhmedBaset/hadith-json
REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.environ.get('HADITH_JSON_SRC', '/tmp/hadith-json/db/by_book')
OUT = os.path.join(REPO, 'app/src/main/assets/databases/hadiths.db')
FIXTURE = os.path.join(REPO, 'app/src/test/resources/hadith_normalization_fixtures.json')

# (مسار الملف، bookId في المصدر، sortOrder للعرض)
BOOKS = [
    ('the_9_books/bukhari', 1, 1),
    ('the_9_books/muslim', 2, 2),
    ('the_9_books/abudawud', 4, 3),
    ('the_9_books/tirmidhi', 5, 4),
    ('the_9_books/nasai', 3, 5),
    ('the_9_books/ibnmajah', 6, 6),
    ('the_9_books/malik', 7, 7),
    ('other_books/riyad_assalihin', 13, 8),
    ('forties/nawawi40', 10, 9),
    ('forties/qudsi40', 11, 10),
]

STRIP = re.compile(
    '[\u0610-\u061A\u0640\u064B-\u065F\u0670\u06D6-\u06ED\u08D3-\u08FF'
    '\uFD3E\uFD3F\u200B-\u200F\u202A-\u202E\u2066-\u2069\uFEFF\uFFF9-\uFFFC]'
)
# الزخارف تُفكّك صراحةً قبل NFKC — إصدارات Unicode مختلفة (أندرويد قديم/جديد
# أو JDK مختلف) تختلف في فكّها، والخريطة الصريحة تجعل النتيجة حتمية للجميع
LIGATURES = {
    '\uFDFA': 'صلى الله عليه وسلم',
    '\uFDF2': 'الله',
    '\uFDFD': 'بسم الله الرحمن الرحيم',
}
LETTER_MAP = {'\u0623': '\u0627', '\u0625': '\u0627', '\u0622': '\u0627',
              '\u0671': '\u0627', '\u0629': '\u0647', '\u0649': '\u064A',
              '\u0624': '\u0648', '\u0626': '\u064A'}
DIGITS = {0x0660 + i: str(i) for i in range(10)}
DIGITS.update({0x06F0 + i: str(i) for i in range(10)})


def normalize(s):
    for src, dst in LIGATURES.items():
        s = s.replace(src, dst)
    s = unicodedata.normalize('NFKC', s)
    s = STRIP.sub('', s)
    s = ''.join(LETTER_MAP.get(ch, ch) for ch in s)
    s = s.translate(DIGITS)
    return s


def match_expr(q):
    """تعبير MATCH — نفس منطق FtsQueryBuilder في Kotlin: كل كلمة مسبوقة بعلامة *"""
    norm = normalize(q)
    tokens = [t for t in re.split(r'[^\w\u0600-\u06FF]+', norm) if t]
    # \w في بايثون يشمل العربية والأرقام والحروف — مطابق للاسترخاء المطلوب
    tokens = [re.sub(r'[^\w\u0600-\u06FF]', '', t) for t in tokens]
    tokens = [t for t in tokens if t]
    if not tokens:
        return None
    return ' '.join('"%s"*' % t for t in tokens)


def main():
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    os.makedirs(os.path.dirname(FIXTURE), exist_ok=True)
    if os.path.exists(OUT):
        os.remove(OUT)

    conn = sqlite3.connect(OUT)
    cur = conn.cursor()
    cur.execute('PRAGMA journal_mode=DELETE')
    cur.execute('PRAGMA page_size=4096')

    cur.execute('''CREATE TABLE books(
        id INTEGER NOT NULL,
        title TEXT NOT NULL,
        author TEXT NOT NULL,
        sortOrder INTEGER NOT NULL,
        PRIMARY KEY(id)
    )''')
    cur.execute('''CREATE TABLE hadiths(
        id INTEGER NOT NULL,
        bookId INTEGER NOT NULL,
        numberInBook INTEGER NOT NULL,
        chapter TEXT NOT NULL,
        text TEXT NOT NULL,
        PRIMARY KEY(id)
    )''')
    # FTS5 contentless — الفهرس فقط بلا نسخة نص إضافية، rowid = hadiths.id
    cur.execute("CREATE VIRTUAL TABLE hadiths_fts USING fts5(norm, content='')")

    fixtures = []
    total_h = 0
    for path, ds_id, sort_order in BOOKS:
        with open(os.path.join(SRC, path + '.json'), encoding='utf-8') as f:
            d = json.load(f)
        meta = d['metadata']
        title = meta['arabic']['title'].strip()
        author = (meta['arabic'].get('author') or '').strip()
        chapters = {c['id']: (c.get('arabic') or '').strip() for c in d['chapters']}
        # كتب ذات باب واحد أو اثنين: اسم الباب مكرر لا فائدة منه في البطاقة
        show_chapter = len(chapters) > 2
        cur.execute('INSERT INTO books(id,title,author,sortOrder) VALUES(?,?,?,?)',
                    (ds_id, title, author, sort_order))
        n = 0
        skipped = 0
        for h in d['hadiths']:
            hid = h['id']
            num = h['idInBook']
            assert isinstance(num, int), (path, hid, num)
            txt = (h.get('arabic') or '').strip()
            if not txt:
                skipped += 1
                continue
            chap = chapters.get(h['chapterId'], '') if show_chapter else ''
            chap = re.sub(r'\s+', ' ', chap).strip()
            cur.execute('INSERT INTO hadiths(id,bookId,numberInBook,chapter,text) VALUES(?,?,?,?,?)',
                        (hid, ds_id, num, chap, txt))
            norm = normalize(txt)
            cur.execute('INSERT INTO hadiths_fts(rowid,norm) VALUES(?,?)', (hid, norm))
            if len(fixtures) < 300 and 40 < len(txt) < 140:
                fixtures.append({'raw': txt, 'expected': normalize(txt)})
            n += 1
        total_h += n
        print('%-24s id=%-3d %s — %d حديث (محذوف فارغ: %d)' % (path, ds_id, title, n, skipped))

    # ضغط الفهرس والتقسيم
    cur.execute("INSERT INTO hadiths_fts(hadiths_fts) VALUES('optimize')")
    cur.execute('PRAGMA user_version=1')
    conn.commit()

    # ===== تحقق =====
    cnt_h = cur.execute('SELECT COUNT(*) FROM hadiths').fetchone()[0]
    cnt_f = cur.execute("SELECT COUNT(*) FROM hadiths_fts").fetchone()[0]
    assert cnt_h == total_h, (cnt_h, total_h)
    assert cnt_f == cnt_h, (cnt_f, cnt_h)
    ver = cur.execute('PRAGMA user_version').fetchone()[0]
    assert ver == 1

    probes = [
        ('النية', 5), ('الصلاة', 100), ('أئمة', 10), ('الأئمة', 10),
        ('على اثر قدميه', 3), ('الثلث', 5), ('شيء', 50), ('مؤمن', 50),
        ('الطهور شطر الايمان', 3), ('بالنيات', 5),
    ]
    print('--- فحص استعلامات MATCH ---')
    for q, min_expected in probes:
        expr = match_expr(q)
        row = cur.execute(
            'SELECT COUNT(*) FROM hadiths_fts WHERE hadiths_fts MATCH ?', (expr,)
        ).fetchone()
        status = 'OK ' if row[0] >= min_expected else 'LOW'
        print('%s [%s] %s -> %d نتيجة' % (status, expr, q, row[0]))
        assert row[0] >= 1, q

    # استعلام مقيد بكتاب مع الترتيب بالصلة — نفس شكل استعلام التطبيق
    q = match_expr('صلاة الفجر')
    # ملاحظة: MATCH يُكتب باسم الجدول الأصلي حتى مع alias — نفس استعلام التطبيق
    rows = cur.execute(
        'SELECT h.id, h.bookId, h.numberInBook, h.text FROM hadiths h '
        'JOIN hadiths_fts f ON f.rowid = h.id '
        'WHERE hadiths_fts MATCH ? AND (? IS NULL OR h.bookId = ?) '
        'ORDER BY rank LIMIT 5', (q, 1, 1)
    ).fetchall()
    print('--- عينة: صلاة الفجر في البخاري (ترتيب الصلة) ---')
    for r in rows:
        print('  #%d ب%.1d: %s' % (r[2], r[1], r[3][:70].replace('\n', ' ')))
    assert rows

    # ترتيب التصفح = ترتيب الإدخال (rowid) — تحقق أن أول صفوف هي البخاري ١..٣
    top = cur.execute('SELECT id, bookId, numberInBook FROM hadiths ORDER BY id LIMIT 3').fetchall()
    assert [t[2] for t in top] == [1, 2, 3], top

    # ===== fixtures للتوافق بين بايثون وKotlin =====
    handcrafted = [
        'الأَئِمَّةُ', 'إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ', 'ٱلْحَمْدُ لِلَّٰهِ رَبِّ الْعَـٰلَمِينَ',
        'قَالَ رَسُولُ ﷺ', '﷽', 'عَلَىٰ', 'كِتَـٰبٌ', 'مُؤْمِنٌ', 'شَيْءٌ',
        '٠١٢٣٤٥٦٧٨٩', 'الصَّلَاةَ', 'رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً',
        'بِسْمِ\u200F اللَّهِ', 'الْقُرْآنِ الْكَرِيمِـــ', 'أَتْعَبْنَا إِذَا أَتْعَبْتَنَا',
        'Fiqh 40', 'وَالْعَصْرِ إِنَّ الْإِنسَـٰنَ', 'سُـَٔالٌ', 'ٱلنَّبِيِّ صَلَّىٰ',
        'ﷺ', 'ﷲ', '﷽', 'عَلَيْهِ ﷺ وَآلِهِ', 'كِتَـٰبٌ', 'الطُّهُور شطر الايمان',
    ]
    seen = set()
    out_fixtures = []
    for f in fixtures:
        if f['raw'] not in seen:
            seen.add(f['raw'])
            out_fixtures.append(f)
    for raw in handcrafted:
        out_fixtures.append({'raw': raw, 'expected': normalize(raw)})
    with open(FIXTURE, 'w', encoding='utf-8') as f:
        json.dump(out_fixtures, f, ensure_ascii=False, indent=1)
    print('--- fixtures: %d زوجاً في %s ---' % (len(out_fixtures), FIXTURE))

    # نفس الأزواج بصيغة Kotlin لاختبار الوحدة — بلا أي مكتبة JSON في الاختبار
    ktl = os.path.join(REPO, 'app/src/test/kotlin/com/sawan4art/noor/HadithNormalizationFixtures.kt')
    os.makedirs(os.path.dirname(ktl), exist_ok=True)

    def kesc(s):
        return (s.replace('\\', '\\\\').replace('"', '\\"')
                 .replace('\n', '\\n').replace('\r', '\\r')
                 .replace('\t', '\\t').replace('$', '\\$'))

    with open(ktl, 'w', encoding='utf-8') as f:
        f.write('package com.sawan4art.noor\n\n')
        f.write('/**\n')
        f.write(' * عقود توافق التطبيع بين مولّد قاعدة الأحاديث (scripts/build_hadith_db.py)\n')
        f.write(' * وHadithSearch.normalize في التطبيق — %d زوجاً: عينات من نصوص الكوربس\n' % len(out_fixtures))
        f.write(' * نفسه + حالات مصاغة يدوياً (همزات، تشكيل، تطويل، أرقام، ﷺ و﷽).\n')
        f.write(' * **مولّد آلياً — لا تُحرَّر يدوياً**.\n')
        f.write(' */\n')
        f.write('internal val HadithNormalizationFixtures: List<Pair<String, String>> = listOf(\n')
        for fx in out_fixtures:
            f.write('    "%s" to "%s",\n' % (kesc(fx['raw']), kesc(fx['expected'])))
        f.write(')\n')
    print('--- Kotlin fixtures: %s ---' % ktl)

    conn.close()
    size = os.path.getsize(OUT)
    print('=== hadiths.db: %d حديثاً — %.1f MB ===' % (total_h, size / 1e6))


if __name__ == '__main__':
    main()
