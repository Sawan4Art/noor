import test from 'node:test';
import assert from 'node:assert/strict';
import {
  getLastReadPage,
  saveLastReadPage,
  getKhatma,
  markKhatmaPageRead,
  unmarkKhatmaPage,
  resetKhatma,
  getKhatmaProgress,
  isPageReadInKhatma,
  getJuzForPage,
  getJuzRange,
  getJuzProgress,
  toggleAyahMemorized,
  isAyahMemorized,
  getMemorizationProgress,
  getReviewQueue,
  JUZ_START_PAGES,
} from './mushafProgress';

// بيئة localStorage وهمية (زي بيئة الاختبارات)
const store = new Map<string, string>();
const g = globalThis as unknown as { localStorage: Storage };
g.localStorage = {
  getItem: (k: string) => store.get(k) ?? null,
  setItem: (k: string, v: string) => void store.set(k, v),
  removeItem: (k: string) => void store.delete(k),
  clear: () => store.clear(),
  key: (i: number) => Array.from(store.keys())[i] ?? null,
  length: 0,
};

test('last read page: default 4, saved and restored, invalid ignored', () => {
  store.clear();
  assert.equal(getLastReadPage(), 4);
  saveLastReadPage(42);
  assert.equal(getLastReadPage(), 42);
  saveLastReadPage(9999); // خارج النطاق — يُتجاهل
  assert.equal(getLastReadPage(), 42);
});

test('khatma: mark page read, progress, unmark, reset', () => {
  store.clear();
  assert.equal(isPageReadInKhatma(1), false);
  assert.equal(markKhatmaPageRead(1), false);
  assert.equal(isPageReadInKhatma(1), true);
  assert.deepEqual(getKhatmaProgress(), { read: 1, total: 604, percent: 0 });

  // تعليم نفس الصفحة مرة ثانية = لا تغيير
  assert.equal(markKhatmaPageRead(1), false);
  assert.equal(getKhatmaProgress().read, 1);

  // صفحات متفرقة
  markKhatmaPageRead(2);
  markKhatmaPageRead(3);
  assert.equal(getKhatmaProgress().read, 3);

  // إلغاء تعليم
  unmarkKhatmaPage(2);
  assert.equal(getKhatmaProgress().read, 2);

  // إعادة بدء
  resetKhatma();
  assert.equal(getKhatmaProgress().read, 0);
  assert.equal(getKhatma().history.length, 0);
});

test('juz: page ranges and lookup', () => {
  assert.equal(JUZ_START_PAGES.length, 30);
  assert.equal(JUZ_START_PAGES[0], 1);
  assert.equal(JUZ_START_PAGES[29], 582);
  assert.equal(getJuzForPage(1), 1);
  assert.equal(getJuzForPage(21), 1);
  assert.equal(getJuzForPage(22), 2);
  assert.equal(getJuzForPage(604), 30);
  assert.deepEqual(getJuzRange(1), { start: 1, end: 21 });
  assert.deepEqual(getJuzRange(30), { start: 582, end: 604 });
  assert.deepEqual(getJuzRange(2), { start: 22, end: 41 });
});

test('juz progress reflects khatma pages', () => {
  store.clear();
  // الجزء 1: صفحات 1-21 — علّم كلها
  for (let p = 1; p <= 21; p++) markKhatmaPageRead(p);
  assert.equal(getJuzProgress(1).percent, 100);
  assert.equal(getJuzProgress(1).read, 21);
  assert.equal(getJuzProgress(2).percent, 0);
});

test('memorization: toggle ayah, progress per surah, review queue order', () => {
  store.clear();
  assert.equal(isAyahMemorized(2, 255), false);

  assert.equal(toggleAyahMemorized(2, 255), true);
  assert.equal(isAyahMemorized(2, 255), true);

  // آية تانية بسورة تانية
  assert.equal(toggleAyahMemorized(112, 1), true);

  const progress = getMemorizationProgress();
  assert.equal(progress.length, 2);
  const baqarah = progress.find((p) => p.surahNumber === 2);
  assert.ok(baqarah);
  assert.equal(baqarah.memorized, 1);
  assert.equal(baqarah.total, 286);
  assert.equal(baqarah.percent, 0); // 1 من 286

  const ikhlas = progress.find((p) => p.surahNumber === 112);
  assert.ok(ikhlas);
  assert.equal(ikhlas.memorized, 1);
  assert.equal(ikhlas.total, 4);
  assert.equal(ikhlas.percent, 25);

  // طابور المراجعة: الأقدم أولاً
  const queue = getReviewQueue(10);
  assert.equal(queue.length, 2);
  assert.ok(queue[0].at <= queue[1].at);

  // إلغاء الحفظ
  assert.equal(toggleAyahMemorized(2, 255), false);
  assert.equal(isAyahMemorized(2, 255), false);
  assert.equal(getMemorizationProgress().length, 1);
});
