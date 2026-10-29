/**
 * نظام تقدم المصحف — نور المسلم:
 * - آخر صفحة مقروءة (متابعة القراءة)
 * - الختمات: تتبع الصفحات المقروءة في ختمة جارية + سجل الختمات المكتملة
 * - الحفظ: الآيات المحفوظة لكل سورة مع وقت حفظها (لنظام المراجعة)
 * كلها localStorage مع fallbackات آمنة (قاعدة الميثاق).
 */
import { SURAHS_LIST } from './quranData';

const LAST_PAGE_KEY = 'noor_last_read_page';
const KHATMA_KEY = 'noor_khatma';
const MEMORIZED_KEY = 'noor_memorized';

/* ==================== متابعة القراءة ==================== */

export function getLastReadPage(): number {
  try {
    const v = parseInt(localStorage.getItem(LAST_PAGE_KEY) ?? '', 10);
    if (Number.isFinite(v) && v >= 1 && v <= 604) return v;
  } catch {
    // تجاهل
  }
  return 4; // صفحة البقرة الأولى افتراضياً
}

export function saveLastReadPage(page: number): void {
  try {
    if (page >= 1 && page <= 604) localStorage.setItem(LAST_PAGE_KEY, String(page));
  } catch {
    // تجاهل
  }
}

/* ==================== الختمات ==================== */

export interface KhatmaState {
  /** الصفحات المقروءة في الختمة الجارية */
  pages: number[];
  /** بداية الختمة الحالية */
  startedAt: number;
  /** الختمات المكتملة */
  history: Array<{ completedAt: number; pagesCount: number; days: number }>;
}

const EMPTY_KHATMA: KhatmaState = { pages: [], startedAt: Date.now(), history: [] };

export function getKhatma(): KhatmaState {
  try {
    const raw = localStorage.getItem(KHATMA_KEY);
    if (raw) {
      const parsed = JSON.parse(raw) as Partial<KhatmaState>;
      return {
        pages: Array.isArray(parsed.pages) ? parsed.pages.filter((p) => p >= 1 && p <= 604) : [],
        startedAt: typeof parsed.startedAt === 'number' ? parsed.startedAt : Date.now(),
        history: Array.isArray(parsed.history)
          ? parsed.history.filter(
              (h) => h && typeof h.completedAt === 'number' && typeof h.days === 'number',
            )
          : [],
      };
    }
  } catch {
    // تجاهل
  }
  return { ...EMPTY_KHATMA, pages: [], history: [] };
}

function saveKhatma(state: KhatmaState): void {
  try {
    localStorage.setItem(KHATMA_KEY, JSON.stringify(state));
  } catch {
    // تجاهل
  }
}

export function isPageReadInKhatma(page: number): boolean {
  return getKhatma().pages.includes(page);
}

/** تعليم صفحة كمقروءة — ترجّع الحالة الجديدة (true لو الختمة اكتملت بهذه الصفحة) */
export function markKhatmaPageRead(page: number): boolean {
  const state = getKhatma();
  if (state.pages.includes(page)) return false;
  const pages = [...state.pages, page];
  if (pages.length >= 604) {
    // اكتملت الختمة! سجّلها وابدأ ختمة جديدة
    const completedAt = Date.now();
    const days = Math.max(1, Math.round((completedAt - state.startedAt) / 86400000));
    const history = [...state.history, { completedAt, pagesCount: 604, days }].slice(-20);
    saveKhatma({ pages: [], startedAt: completedAt, history });
    return true;
  }
  saveKhatma({ ...state, pages });
  return false;
}

/** إلغاء تعليم صفحة */
export function unmarkKhatmaPage(page: number): void {
  const state = getKhatma();
  saveKhatma({ ...state, pages: state.pages.filter((p) => p !== page) });
}

/** إعادة بدء الختمة الحالية من الصفر */
export function resetKhatma(): void {
  const state = getKhatma();
  saveKhatma({ pages: [], startedAt: Date.now(), history: state.history });
}

export function getKhatmaProgress(): { read: number; total: 604; percent: number } {
  const read = new Set(getKhatma().pages).size;
  return { read, total: 604, percent: Math.round((read / 604) * 100) };
}

/* ==================== الأجزاء ==================== */

/** بداية كل جزء بالصفحات (مصحف مجمع الملك فهد — 604 صفحة) */
export const JUZ_START_PAGES: number[] = [
  1, 22, 42, 62, 82, 102, 121, 142, 162, 182, 201, 222, 242, 262, 282, 302, 322, 342,
  362, 382, 402, 422, 442, 462, 482, 502, 522, 542, 562, 582,
];

export function getJuzForPage(page: number): number {
  for (let j = JUZ_START_PAGES.length - 1; j >= 0; j--) {
    if (page >= JUZ_START_PAGES[j]) return j + 1;
  }
  return 1;
}

/** نطاق صفحات كل جزء */
export function getJuzRange(juz: number): { start: number; end: number } {
  const idx = Math.max(1, Math.min(30, juz)) - 1;
  const start = JUZ_START_PAGES[idx];
  const end = idx + 1 < JUZ_START_PAGES.length ? JUZ_START_PAGES[idx + 1] - 1 : 604;
  return { start, end };
}

/** تقدم القراءة في جزء معين (نسبة الصفحات المقروءة في الختمة) */
export function getJuzProgress(juz: number): { read: number; total: number; percent: number } {
  const { start, end } = getJuzRange(juz);
  const readSet = new Set(getKhatma().pages);
  let read = 0;
  for (let p = start; p <= end; p++) if (readSet.has(p)) read++;
  const total = end - start + 1;
  return { read, total, percent: Math.round((read / total) * 100) };
}

/* ==================== نظام الحفظ ==================== */

/** خريطة "رقم السورة:رقم الآية" -> وقت الحفظ */
export type MemorizedMap = Record<string, number>;

export function getMemorized(): MemorizedMap {
  try {
    const raw = localStorage.getItem(MEMORIZED_KEY);
    if (raw) {
      const parsed = JSON.parse(raw) as MemorizedMap;
      const clean: MemorizedMap = {};
      for (const [k, v] of Object.entries(parsed)) {
        if (/^\d+:\d+$/.test(k) && typeof v === 'number') clean[k] = v;
      }
      return clean;
    }
  } catch {
    // تجاهل
  }
  return {};
}

function saveMemorized(map: MemorizedMap): void {
  try {
    localStorage.setItem(MEMORIZED_KEY, JSON.stringify(map));
  } catch {
    // تجاهل
  }
}

export function isAyahMemorized(surah: number, ayah: number): boolean {
  return getMemorized()[`${surah}:${ayah}`] !== undefined;
}

/** تعليم آية محفوظة/إلغاء — يرجّع الحالة الجديدة */
export function toggleAyahMemorized(surah: number, ayah: number): boolean {
  const map = getMemorized();
  const key = `${surah}:${ayah}`;
  if (map[key] !== undefined) {
    delete map[key];
    saveMemorized(map);
    return false;
  }
  map[key] = Date.now();
  saveMemorized(map);
  return true;
}

/** تقدم الحفظ لكل السور (المرتبة) */
export interface SurahMemorizationProgress {
  surahNumber: number;
  surahName: string;
  memorized: number;
  total: number;
  percent: number;
  /** آخر وقت حفظ داخل السورة — للأولوية في المراجعة */
  lastAt: number;
}

export function getMemorizationProgress(): SurahMemorizationProgress[] {
  const map = getMemorized();
  const perSurah = new Map<number, { count: number; lastAt: number }>();
  for (const [key, ts] of Object.entries(map)) {
    const [s, a] = key.split(':').map(Number);
    if (!s || !a) continue;
    const cur = perSurah.get(s) ?? { count: 0, lastAt: 0 };
    perSurah.set(s, { count: cur.count + 1, lastAt: Math.max(cur.lastAt, ts) });
  }
  return SURAHS_LIST.map((surah) => {
    const m = perSurah.get(surah.number);
    const memorized = Math.min(m?.count ?? 0, surah.numberOfAyahs);
    return {
      surahNumber: surah.number,
      surahName: surah.name,
      memorized,
      total: surah.numberOfAyahs,
      percent: Math.round((memorized / surah.numberOfAyahs) * 100),
      lastAt: m?.lastAt ?? 0,
    };
  }).filter((p) => p.memorized > 0);
}

/** آيات المراجعة: الأقدم حفظاً أولاً (أقرب لنسيانها) */
export function getReviewQueue(limit = 5): Array<{ surah: number; ayah: number; at: number }> {
  const map = getMemorized();
  return Object.entries(map)
    .map(([key, at]) => {
      const [s, a] = key.split(':').map(Number);
      return { surah: s, ayah: a, at };
    })
    .sort((x, y) => x.at - y.at)
    .slice(0, limit);
}
