#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
إعادة بناء مكتبة الأحاديث بالترقيم المعتمد الكامل (alpha57):

بلاغ المالك: «مازال هناك كتب ناقصه من كتب الاحاديث بل ايضا كتاب صحيح
البخاري كان كاملا الان يبدا ب الحديث رقم ٢ … عايز كل الكتب تبدا برقم ١
و يكون ترقيم الحديث في كل كتاب هو الترقيم المعتمد».

جذران كشفهما التحقق:
1) **«البخاري يبدأ من ٢»**: إصلاح alpha56 كان يحفظ معرفات الصفوف القديمة
   ويلحق الناقص الجديد بمعرفات بعد أقصى القديم — فأصبح الحديث ١ للبخاري
   (والمقدمة لمسلم) في **ذيل** جدول الـ id، والتصفح كان يرتب بـ id فظهرت
   القائمة تبدأ من ٢. الجذر إذن ترتيبُ العرض وليس نقصُ النص — والعلاج
   مزدوج: الترتيب في الكود يصير (sortOrder الكتاب، رقم الحديث، المعرف)
   والقاعدة نفسها تُعاد بناءها بترتيبٍ نظيف.
2) **ترقيم مسلم ليس هو المعتمد**: مصدر alpha56 (fawazahmed0 ara-muslim)
   يرقّم مسلم تسلسلياً ١..٧٥٦٣ باللواحق الحرفية (أ، ب، ت…) محسوبة
   أرقاماً مستقلة — والترقيم المعتمد (عبد الباقي، وهو ما يعرضه
   sunnah.com ويُستشهد به) آخره **٣٠٣٣** واللواحق الحرفية تحت الرقم
   نفسه. الجديد: مسلم يُبنى من نسخة sunnah.com الحاملة الرقم الرسمي
   (CheeseWithSauce/HadithsJSONFormat — كشط مباشر لصفحات sunnah.com)
   «Sahih Muslim 8a / 1697/1698a / 3006-3014» فتُفكَّك كلها إلى صفوف
   تحت رقمها المعتمد.

الكتب الست الأخرى تبقى من fawazahmed0 (ترقيمها المعتمد نفسه حرفياً:
بخاري ٧٥٦٣، نسائي ٥٧٥٨، أبو داود ٥٢٧٤، ترمذي ٣٩٥٦، ابن ماجه ٤٣٤١،
موطأ ١٨٥٨ — وكلها متصلة من ١ بعد توسيع المدى «٥٧٠٩-٥٧١١»).

المحفوظات: كل نصٍّ قديم يُعاد بمعرّفه القديم نفسه (مطابقة نصٍّ مطبَّع،
ثم مطابقة بادئة ١٠٠ حرف إن اختلفت المصادر) فمفضلة المستخدم لا تكسر،
وأبواب الكتب العربية تُورَّث بالمطابقة نفسها ثم بباب الجار السابق.
الفهرس FTS4 يُبنى من جديد بالتطبيع المرجعي نفسه (عقد التوافق مع
HadithSearch.kt) ونسخة القاعدة تُرفع إلى 4 فتبدّلها Room على الأجهزة.
"""

import json
import os
import re
import sqlite3
import subprocess
import unicodedata
import urllib.request

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DB = os.path.join(REPO, 'app/src/main/assets/databases/hadiths.db')
CACHE = '/tmp/hadith_official_cache'

# bookId في القاعدة ← إصدار fawazahmed0 المعتمد (للكتب الست)
EDS = {
    1: 'ara-bukhari',
    3: 'ara-nasai',
    4: 'ara-abudawud',
    5: 'ara-tirmidhi',
    6: 'ara-ibnmajah',
    7: 'ara-malik',
}
# مسلم — النسخة الحاملة الرقم الرسمي (كشط sunnah.com)
MUSLIM_BOOK_ID = 2
MUSLIM_REPO = 'https://github.com/CheeseWithSauce/HadithsJSONFormat'

# الملاحظة المعتمدة: هذه الأرقام ليست موجودة على sunnah.com نفسه
# (مدمجة مع جيرانها في الطبعة الرقمية) — تُوثَّق ولا تُختلق لها نصوص.
MUSLIM_ABSENT = [1824, 2483]

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


def fetch(url, path):
    if os.path.exists(path):
        return
    os.makedirs(os.path.dirname(path), exist_ok=True)
    print('   تنزيل', url)
    with urllib.request.urlopen(url, timeout=240) as r, open(path, 'wb') as f:
        f.write(r.read())


def canon_from_fawaz(book_id):
    """حمل إصدار fawazahmed0 وارجع قائمة (رقم معتمد، نص) — بمدىً مُوسَّع"""
    ed = EDS[book_id]
    path = os.path.join(CACHE, ed + '.min.json')
    fetch('https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/%s.min.json' % ed, path)
    with open(path, encoding='utf-8') as f:
        d = json.load(f)
    out = []
    for h in d['hadiths']:
        num = h.get('hadithnumber')
        text = h.get('text', '')
        if isinstance(num, bool):
            continue
        if isinstance(num, (int, float)):
            out.append((int(float(num)), text))
        elif isinstance(num, str):
            m = re.match(r'^\s*(\d+)\s*[-–]\s*(\d+)\s*$', num)
            if m:
                a, b = int(m.group(1)), int(m.group(2))
                if 0 < b - a < 30:
                    for x in range(a, b + 1):
                        out.append((x, text))
                else:
                    out.append((a, text))
            else:
                m2 = re.match(r'^\s*(\d+)', num)
                if m2:
                    out.append((int(m2.group(1)), text))
    return out


def canon_from_muslim():
    """مسلم من نسخة sunnah.com الرسمية — الرقم مع اللواحق الحرفية"""
    repo_dir = os.path.join(CACHE, 'sunnah-json')
    if not os.path.isdir(os.path.join(repo_dir, '.git')):
        print('   استنساخ', MUSLIM_REPO)
        subprocess.run(['git', 'clone', '--depth', '1', MUSLIM_REPO, repo_dir],
                       check=True, capture_output=True)
    muslim_dir = os.path.join(repo_dir, 'Sunnah', 'muslim')
    out = []
    for name in sorted(os.listdir(muslim_dir)):
        if not name.endswith('.json'):
            continue
        with open(os.path.join(muslim_dir, name), encoding='utf-8-sig') as f:
            d = json.load(f)
        for x in d:
            ref = x.get('reference', '')
            text = x.get('arabic', '').strip()
            m = re.match(r'Reference\s*:\s*Sahih Muslim\s+(.+?)(?:\s+In-book|\s+USC|$)', ref)
            if not m:
                continue
            token = m.group(1).strip()
            if token.startswith('Introduction'):
                # المتون غير المرقَّمة رسمياً في المقدمة — بلا رقم معتمد
                continue
            out.extend(expand_muslim_token(token, text))
    return out


def expand_muslim_token(token, text):
    """كل صور الرقم على sunnah.com ← قائمة أرقام معتمدة

    الصور المرصودة: «8a» و«4 a» (بمسافة) و«1697/1698a» (مركّب بمائلة)
    و«3006-3014» (مدى) و«598a-b» (مدى حرفي تحت الرقم نفسه)
    و«928a, 927h, 929a» (متعدّد بفواصل — الحديث نفسه بأكثر من رقم).
    """
    rows = []
    token = re.sub(r'\s+', ' ', token.strip())
    # مدى رقمي «3006-3014»
    m = re.match(r'^(\d+)\s*[-–]\s*(\d+)$', token)
    if m:
        a, b = int(m.group(1)), int(m.group(2))
        if 0 <= b - a < 30:
            return [(x, text) for x in range(a, b + 1)]
        return [(a, text)]
    # أجزاء بمائلة «1697/1698a» وبفواصل «928a, 927h, 929a»
    parts = [p.strip() for p in re.split(r'[/,]', token) if p.strip()]
    for p in parts:
        # «928» أو «928a» أو «4 a» أو «598a-b» (مدى حرفي)
        m = re.match(r'^(\d+)\s*([a-z]?)(?:\s*[-–]\s*([a-z]))?$', p)
        if not m:
            continue
        num = int(m.group(1))
        first = m.group(2)
        last = m.group(3) or first
        if first and last:
            letters = [chr(c) for c in range(ord(first), ord(last) + 1)]
            rows.extend((num, text) for _ in letters)  # كل لاحقة صفٌّ تحت الرقم نفسه
        else:
            rows.append((num, text))
    return rows


def main():
    conn = sqlite3.connect(DB)
    cur = conn.cursor()

    print('== ١) صفوف القاعدة القديمة (المعرّفات والأبواب والمحفوظات) ==')
    old_rows = {}
    for bid, num, ch, text, oid in cur.execute(
            'SELECT bookId, numberInBook, chapter, text, id FROM hadiths'):
        old_rows.setdefault(bid, []).append((oid, num, ch, text))
    max_old_id = max((oid for rows in old_rows.values() for oid, *_ in rows), default=0)
    print('   أقصى معرف قديم:', max_old_id, '| صفوف:', sum(len(v) for v in old_rows.values()))

    print('== ٢) الكتب السبع من مصادرها المعتمدة ==')
    canonical = {}
    for bid in sorted(EDS):
        canonical[bid] = canon_from_fawaz(bid)
    canonical[MUSLIM_BOOK_ID] = canon_from_muslim()

    # إزالة التكرار (نفس الرقم ونفس النص المطبَّع)
    for bid in canonical:
        seen = set()
        uniq = []
        for num, text in canonical[bid]:
            key = (num, norm(text)[:400])
            if key in seen:
                continue
            seen.add(key)
            uniq.append((num, text))
        canonical[bid] = sorted(uniq, key=lambda r: r[0])

    print('== ٣) مطابقة المعرّفات والأبواب من القديم ==')
    next_id = max_old_id
    new_rows = []  # (id, bookId, numberInBook, chapter, text)
    stats = {}
    for bid in sorted(canonical):
        # فهارس القديم لهذا الكتاب
        exact = {}
        prefix = {}
        old_sorted = sorted(old_rows.get(bid, []), key=lambda r: r[1])
        for oid, num, ch, text in old_rows.get(bid, []):
            k = norm(text)
            exact.setdefault(k, (oid, ch))
            prefix.setdefault(k[:100], (oid, ch))
        last_chapter = ''
        matched = 0
        fresh = 0
        used_ids = set()
        for num, text in canonical[bid]:
            k = norm(text)
            # المطابقة تُستهلك مرةً واحدة (pop + ذاكرة استهلاك) — فلا يرث
            # صفّان معرفاً واحداً: قاموسا التامة والبادئة يتشاركان المعرفات
            # (سلاسل اللواحق المتشابهة تتشابه أوائلها وتكسر القيد وإلا)
            hit = exact.pop(k, None) or prefix.pop(k[:100], None)
            if hit is not None and hit[0] not in used_ids:
                oid, ch = hit
                used_ids.add(oid)
                matched += 1
            else:
                next_id += 1
                oid, ch = next_id, None
                fresh += 1
            if ch:
                last_chapter = ch
            chapter = ch or last_chapter
            new_rows.append((oid, bid, num, chapter, text))
        nums = sorted({r[2] for r in new_rows if r[1] == bid})
        gaps = [n for n in range(nums[0], nums[-1] + 1) if n not in set(nums)]
        stats[bid] = dict(first=nums[0], last=nums[-1],
                          rows=sum(1 for r in new_rows if r[1] == bid),
                          distinct=len(nums), matched=matched, fresh=fresh,
                          gaps=gaps)
        print(f'   كتاب {bid}: أول={nums[0]} آخر={nums[-1]} صفوف={stats[bid]["rows"]} '
              f'مميزة={len(nums)} قديم المطابقة={matched} جديد={fresh} فجوات={gaps[:6]}')

    print('== ٤) إعادة الكتابة بترتيب (الكتاب، الرقم، المعرف) ==')
    cur.execute('BEGIN')
    cur.execute('DELETE FROM hadiths')
    cur.executemany(
        'INSERT INTO hadiths(id, bookId, numberInBook, chapter, text) VALUES (?,?,?,?,?)',
        sorted(new_rows, key=lambda r: (r[1], r[2], r[0])),
    )

    print('== ٥) فهرس FTS4 من جديد بالتطبيع المرجعي ==')
    for t in ('hadiths_fts', 'hadiths_fts_segments', 'hadiths_fts_segdir',
              'hadiths_fts_docsize', 'hadiths_fts_stat'):
        cur.execute('DROP TABLE IF EXISTS "%s"' % t)
    cur.execute("CREATE VIRTUAL TABLE hadiths_fts USING fts4(norm, tokenize=unicode61, content='')")
    cur.executemany(
        'INSERT INTO hadiths_fts(docid, norm) VALUES (?,?)',
        [(r[0], norm(r[4])) for r in new_rows],
    )

    print('== ٦) نسخة القاعدة ٤ ثم ضغط ==')
    cur.execute('PRAGMA user_version = 4')
    cur.execute('COMMIT')
    cur.execute('VACUUM')
    conn.commit()

    print('== ٧) العدّ النهائي ==')
    ok = True
    expect = {1: 7563, 2: 3033, 3: 5758, 4: 5274, 5: 3956, 6: 4341, 7: 1858}
    absent_ok = set(MUSLIM_ABSENT)
    for bid in sorted(expect):
        row = cur.execute(
            'SELECT MIN(numberInBook), MAX(numberInBook), COUNT(*), COUNT(DISTINCT numberInBook) '
            'FROM hadiths WHERE bookId=?', (bid,)).fetchone()
        gaps = [n for n in range(1, row[1] + 1)
                if cur.execute('SELECT 1 FROM hadiths WHERE bookId=? AND numberInBook=?',
                               (bid, n)).fetchone() is None]
        bad_gaps = [g for g in gaps if not (bid == MUSLIM_BOOK_ID and g in absent_ok)]
        status = 'سليم' if (row[0] == 1 and row[1] == expect[bid] and not bad_gaps) else 'إشكال!'
        if status != 'سليم':
            ok = False
        print(f'   كتاب {bid}: أول={row[0]} آخر={row[1]} صفوف={row[2]} مميزة={row[3]} '
              f'فجوات={gaps if gaps else "لا شيء"} — {status}')
    total = cur.execute('SELECT COUNT(*) FROM hadiths').fetchone()[0]
    fts = cur.execute('SELECT COUNT(*) FROM hadiths_fts_docsize').fetchone()[0]
    print(f'   الإجمالي: {total} حديثاً | فهرس: {fts}')
    conn.close()
    if not ok:
        raise SystemExit('فشل التحقق النهائي — راجع الفجوات أعلاه')
    print('تم — القاعدة بالترقيم المعتمد وكل الكتب تبدأ من ١.')


if __name__ == '__main__':
    main()
