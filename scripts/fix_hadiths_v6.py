#!/usr/bin/env python3
"""إصلاح قاعدة الأحاديث — v6 (بلاغ المالك v1.71)

(١) سنن النسائي «ترتيبها غير صحيح» — الجذر: ٨٦ نصاً فارغاً (ومنها ٨٢
    متتالية ٣٨٥٧–٣٩٣٨) أعادها rebuild_hadiths_official.py من fawaz بلا
    ملء، فظهرت بطاقاتٌ بيضاءٌ في وسط الكتاب كأن الترتيب بايظ — تُملأ
    كلها من كشط sunnah.com الرسمي (/tmp/sunnah-json-clone) مع بخاري
    (٩) وترمذي (٧١) وأبي داود (٢).
(٢) صحيح مسلم «لا يعرض كل الأحاديث وغير مرتب» — الجذور الثلاثة:
    - regex التوسيع كان يسقط اللواحق المركبة (1211aa–1211ah و715aa–ad
      و157L) فسقط ١٣ حديثاً — صُحح النمط ليقبل حروفاً متعددةٍ وكبيرة.
    - إزالة التكرار كانت على أول ٤٠٠ حرفٍ فقط فاندماجت سلاسلٌ متوازية
      متاولاها مختلف — صار التكرار على النص المطبَّع كاملاً فرجعت ١٠.
    - ترتيب السلاسل داخل الرقم الواحد يتبع ترتيب الكشط عبر مطابقة
      النصوص بالترتيب نفسه.
(٣) user_version ٥ ← ٦ فتحذف الأجهزة نسخها القديمة وتعيد البناء من الأصل.
"""
import json
import os
import re
import sqlite3
import subprocess
import sys
import time
import unicodedata
import urllib.request
from concurrent.futures import ThreadPoolExecutor

DB = 'app/src/main/assets/databases/hadiths.db'
CLONE = '/tmp/sunnah-json-clone/Sunnah'
REPO = 'https://github.com/A7med3ly/sunnah-json'  # كشط sunnah.com الرسمي
UA = 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/120 Safari/537.36'
SCRAPE_CACHE = '/tmp/sunnah_pages_v6'
COM_PATH = {1: 'bukhari', 3: 'nasai', 4: 'abudawud', 5: 'tirmidhi'}
ARABIC_RE = re.compile(r'class="arabic_hadith_full[^"]*"[^>]*>(.*?)</div>', re.S)
TAGS_RE = re.compile(r'<[^>]+>')
CHAPTER_RE = re.compile(r'class="arabic_chapter"[^>]*>(.*?)</a>', re.S)

SCRAPED_BOOK = {1: 'bukhari', 3: 'nasai', 4: 'abudawud', 5: 'tirmidhi', 2: 'muslim'}
REF_PAT = {
    1: r'Reference\s*:\s*Sahih al-Bukhari\s+(.+?)(?:\s+In-book|\s+USC|$)',
    2: r'Reference\s*:\s*Sahih Muslim\s+(.+?)(?:\s+In-book|\s+USC|$)',
    3: r'Reference\s*:\s*Sunan an-Nasa.i\s+(.+?)(?:\s+In-book|\s+USC|$)',
    4: r'Reference\s*:\s*Sunan Abi Dawud\s+(.+?)(?:\s+In-book|\s+USC|$)',
    5: r'Reference\s*:\s*Jami. at-Tirmidhi\s+(.+?)(?:\s+In-book|\s+USC|$)',
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
    """التطبيع المرجعي — حرفياً كما في HadithSearch.kt"""
    for a, b in LIG.items():
        s = s.replace(a, b)
    s = unicodedata.normalize('NFKC', s)
    s = STRIP.sub('', s)
    s = ''.join(LM.get(ch, ch) for ch in s)
    s = s.translate(DIGITS)
    return re.sub(r'\s+', ' ', s).strip()


def load_scrape(book_id):
    """كشط sunnah.com لكتابٍ — قائمة (token, text) بترتيب الكشط"""
    d = os.path.join(CLONE, SCRAPED_BOOK[book_id])
    if not os.path.isdir(d):
        print('   استنساخ كشط sunnah.com …')
        subprocess.run(['git', 'clone', '--depth', '1',
                        'https://github.com/A7med3ly/sunnah-json',
                        os.path.dirname(CLONE)], check=True, capture_output=True)
    out = []
    for name in sorted(os.listdir(d)):
        if not name.endswith('.json'):
            continue
        with open(os.path.join(d, name), encoding='utf-8-sig') as f:
            rows = json.load(f)
        for x in rows:
            ref = x.get('reference', '')
            text = (x.get('arabic') or '').strip()
            m = re.match(REF_PAT[book_id], ref)
            if not m:
                continue
            out.append((m.group(1).strip(), text))
    return out


def letters_between(a, b):
    """كل اللواحق الحرفية بين a وb — «a»→«h» ثمانية و«aa»→«ah» ثمانية"""
    if len(a) != len(b):
        return [a]
    out, cur = [], list(a)
    while True:
        out.append(''.join(cur))
        if ''.join(cur) == b:
            break
        i = len(cur) - 1
        while i >= 0:
            if cur[i] < 'z':
                cur[i] = chr(ord(cur[i]) + 1)
                break
            cur[i] = 'a'
            i -= 1
        else:
            break
        if len(out) > 64:
            break
    return out


def expand_token(token, text):
    """كل صور الرقم على sunnah.com — بما فيها اللواحق المركبة 1211aa و157L

    الجذر بلاغ «مسلم لا يعرض كل الأحاديث»: النمط القديم ([a-z]?) يسقط
    اللواحق متعددة الحروف (aa–ah) والحرف الكبير (157L) فسقطت ١٣ حديثاً.
    """
    rows = []
    token = re.sub(r'\s+', ' ', token.strip())
    m = re.match(r'^(\d+)\s*[-–]\s*(\d+)$', token)
    if m:
        a, b = int(m.group(1)), int(m.group(2))
        if 0 <= b - a < 30:
            return [(x, text) for x in range(a, b + 1)]
        return [(a, text)]
    parts = [p.strip() for p in re.split(r'[/,]', token) if p.strip()]
    for p in parts:
        m = re.match(r'^(\d+)\s*([a-zA-Z]*)(?:\s*[-–]\s*(?:(\d+)\s*)?([a-zA-Z]*))?$', p)
        if not m:
            continue
        num = int(m.group(1))
        first = m.group(2).lower()
        last = (m.group(4) or first).lower()
        if first and last and first != last:
            for suf in letters_between(first, last):
                rows.append((num, text))  # كل لاحقة صفٌّ تحت الرقم نفسه
        else:
            rows.append((num, text))
    return rows


def fetch_page(path):
    """صفحة حديثٍ من sunnah.com نفسه — للنواقص الغائبة عن الكشط"""
    os.makedirs(SCRAPE_CACHE, exist_ok=True)
    safe = path.replace(':', '_').replace('/', '_')
    cache = os.path.join(SCRAPE_CACHE, safe + '.html')
    if os.path.exists(cache):
        html = open(cache, encoding='utf-8', errors='ignore').read()
    else:
        url = 'https://sunnah.com/%s' % path
        html = None
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


def fill_blanks(conn):
    """(١) ملء النصوص الفارغة من كشط sunnah.com — نُسائي وبخاري وترمذي وأبي داود"""
    cur = conn.cursor()
    blanks = dict(cur.execute(
        'SELECT bookId, COUNT(*) FROM hadiths WHERE TRIM(text) = "" GROUP BY bookId'))
    print('   فراغات قبل الملء:', blanks)
    scraped = {}
    for bid in (1, 3, 4, 5):
        by_num = {}
        for token, text in load_scrape(bid):
            m = re.match(r'^\s*(\d+)', token)
            if not m or not text:
                continue
            n = int(m.group(1))
            if n not in by_num:  # أول نصٍّ غير فارغٍ للرقم بترتيب الكشط
                by_num[n] = text
        scraped[bid] = by_num
    filled = 0
    still = []
    for bid, by_num in scraped.items():
        for rid, num in cur.execute(
                'SELECT id, numberInBook FROM hadiths WHERE bookId=? AND TRIM(text)=""',
                (bid,)).fetchall():
            text = by_num.get(num)
            if text:
                cur.execute('UPDATE hadiths SET text=? WHERE id=?', (text, rid))
                filled += 1
            else:
                still.append((rid, bid, num))
    print('   مُلئ من الكشط:', filled, '| غائبٌ عن الكشط:', len(still))

    # النواقص الغائبة عن الكشط (أرقام مدمجةٌ في جيرانها بلا صفٍّ مستقل)
    # تُجلب صفحةً صفحةً من sunnah.com نفسه — ولا يُخترع نصٌّ أبداً
    jobs = [s for s in still if s[1] in COM_PATH and s[2] >= 1]
    if jobs:
        def one(job):
            rid, bid, num = job
            return job, fetch_page('%s:%d' % (COM_PATH[bid], num))
        got = {}
        with ThreadPoolExecutor(max_workers=8) as pool:
            for job, res in pool.map(one, jobs):
                got[job] = res
        second = 0
        delete = []
        for (rid, bid, num), res in got.items():
            if res is None or not res[0].strip():
                # الرقم غير موجودٍ على sunnah.com نفسه (مدمجٌ بجاره)
                # — نصُّه الحقيقي نصُّ الجار المدمجٍ به في الكشط، ونشير
                # إليه بلا بطاقةٍ فارغة: يُحذف صفُّه الفارغ
                delete.append(rid)
                continue
            text, chapter = res
            cur.execute('UPDATE hadiths SET text=?, chapter=? WHERE id=?',
                        (text, chapter or "", rid))
            second += 1
        for rid in delete:
            cur.execute('DELETE FROM hadiths WHERE id=?', (rid,))
        filled += second
        print('   مُلئ من صفحات sunnah.com:', second, '| أرقامٌ مدمجةٌ حُذف صفّها:', len(delete))
        still = []
    return filled, still


def rebuild_muslim(conn):
    """(٢) مسلم من الكشط الرسمي — كاملٌ بترتيب الكشط والمطابقة تحفظ المعرّفات"""
    cur = conn.cursor()
    old = {}
    max_global = 0
    for oid, num, ch, text in cur.execute(
            'SELECT id, numberInBook, chapter, text FROM hadiths WHERE bookId=2'):
        old.setdefault(num, []).append((oid, ch, norm(text)))
    max_global = cur.execute('SELECT MAX(id) FROM hadiths').fetchone()[0] or 0
    # المعرفات الجديدة فوق أعلى معرفٍ عامٍّ كي لا تصطدم بكتبٍ أخرى
    # (جذر فشل UNIQUE في الجولة الأولى)
    # فهارس المطابقة التامة — المعرف يُستهلك مرةً واحدةً بترتيب الكشط
    id_by_text = {}
    for num, rows in old.items():
        for oid, ch, k in rows:
            id_by_text.setdefault((num, k), []).append(oid)
    for key in id_by_text:
        id_by_text[key].sort()  # المعرفات القديمة داخل الرقم تُستهلك بالأولوية

    canonical = []
    for token, text in load_scrape(2):
        if not text:
            continue
        m = re.match(r'^Introduction\s', token)
        if m:
            continue  # المتون غير المرقَّمة رسمياً في المقدمة — سياسة الوثيقة
        canonical.extend(expand_token(token, text))
    # إزالة التكرار على النص المطبَّع **كاملاً** — الجذر الثاني: أول ٤٠٠
    # حرفٍ كانت تدمج سلاسل متوازية متاولاها مختلف
    seen = set()
    uniq = []
    for num, text in canonical:
        key = (num, norm(text))
        if key in seen:
            continue
        seen.add(key)
        uniq.append((num, text))
    print('   مسلم من الكشط: صفوف=%d مميزة=%d' % (len(canonical), len(uniq)))

    next_id = [max_global]
    new_rows = []  # (id, num, chapter, text)
    matched_total = fresh_total = 0
    last_chapter = ''
    for num, text in sorted(uniq, key=lambda r: r[0]):
        ids = id_by_text.get((num, norm(text)))
        if ids:
            oid = ids.pop(0)
            matched_total += 1
        else:
            next_id[0] += 1
            oid = next_id[0]
            fresh_total += 1
        new_rows.append((oid, num, last_chapter, text))
    print('   مطابقة قديمة=%d جديدة=%d' % (matched_total, fresh_total))

    # ترتيب الكشط داخل الرقم: المجموعات التي خرجت سلاسلها بترتيبٍ غير
    # تصاعديٍّ تُرقَّم معرفاتها من جديدٍ بترتيب الكشط فوق أقصى معرفٍ عامٍّ
    # — فتبدو البطاقات بترتيب sunnah.com تماماً (ومعرّفات تلك المجموعات
    # فقط تتبدل، والمحفوظات عليها تسقط بهدوءٍ من قائمة المفضلة)
    by_num = {}
    for oid, num, ch, text in new_rows:
        by_num.setdefault(num, []).append(oid)
    misordered = [n for n, ids in by_num.items()
                  if len(ids) > 1 and ids != sorted(ids)]
    print('   أرقامٌ سلاسلها بترتيبٍ غير تصاعدي بعد المطابقة:', len(misordered))
    renumbered = 0
    for num in misordered:
        for oid in by_num[num]:
            next_id[0] += 1
            for i, r in enumerate(new_rows):
                if r[0] == oid:
                    new_rows[i] = (next_id[0], r[1], r[2], r[3])
                    renumbered += 1
                    break
    print('   صفوفٌ أُعيد ترقيمها بترتيب الكشط:', renumbered)

    cur.execute('DELETE FROM hadiths WHERE bookId=2')
    # أعداد الكتاب تُعاد كتابتها بترتيب (الرقم، ترتيب الكشط)
    order_index = {}
    for i, r in enumerate(new_rows):
        order_index[r[0]] = i
    final = sorted(new_rows, key=lambda r: (r[1], order_index[r[0]]))
    final = [(oid, 2, num, ch, text) for oid, num, ch, text in final]
    cur.executemany(
        'INSERT INTO hadiths(id, bookId, numberInBook, chapter, text) VALUES (?,?,?,?,?)',
        final,
    )
    return dict(rows=len(final), distinct=len(by_num), fresh=fresh_total)


def rebuild_fts(conn):
    cur = conn.cursor()
    print('   فهرس FTS4 من جديد …')
    for t in ('hadiths_fts', 'hadiths_fts_segments', 'hadiths_fts_segdir',
              'hadiths_fts_docsize', 'hadiths_fts_stat'):
        cur.execute('DROP TABLE IF EXISTS "%s"' % t)
    cur.execute("CREATE VIRTUAL TABLE hadiths_fts USING fts4(norm, tokenize=unicode61, content='')")
    rows = cur.execute('SELECT id, text FROM hadiths').fetchall()
    cur.executemany(
        'INSERT INTO hadiths_fts(docid, norm) VALUES (?,?)',
        [(rid, norm(text)) for rid, text in rows],
    )
    return len(rows)


def main():
    conn = sqlite3.connect(DB)
    cur = conn.cursor()
    ver = cur.execute('PRAGMA user_version').fetchone()[0]
    print('نسخة القاعدة الحالية:', ver)

    print('== ١) ملء النصوص الفارغة من كشط sunnah.com ==')
    filled, still = fill_blanks(conn)

    print('== ٢) مسلم كاملاً من الكشط الرسمي ==')
    m_stats = rebuild_muslim(conn)

    print('== ٣) فهرس البحث ==')
    total = rebuild_fts(conn)

    print('== ٤) النسخة ٦ والضغط ==')
    conn.commit()
    cur.execute('PRAGMA user_version = 6')
    conn.commit()
    cur.execute('VACUUM')
    conn.commit()

    print('== ٥) التحقق النهائي ==')
    counts = dict(cur.execute(
        'SELECT bookId, COUNT(*) FROM hadiths GROUP BY bookId ORDER BY bookId'))
    blanks = cur.execute('SELECT COUNT(*) FROM hadiths WHERE TRIM(text)=""').fetchone()[0]
    nums2 = [r[0] for r in cur.execute(
        'SELECT DISTINCT numberInBook FROM hadiths WHERE bookId=2 ORDER BY numberInBook')]
    print('   العدّ لكل كتاب:', counts, '| المجموع:', sum(counts.values()))
    print('   فراغات بعد:', blanks, '| مسلم: مميزة=%d أول=%d آخر=%d فجوات=%s' % (
        len(nums2), nums2[0], nums2[-1],
        [n for n in range(nums2[0], nums2[-1] + 1) if n not in set(nums2)][:6]))
    conn.close()
    if blanks or still:
        print('فشل: ما زالت فراغات!')
        sys.exit(1)
    print('تم — v6 جاهزة')


if __name__ == '__main__':
    main()
