import test from 'node:test';
import assert from 'node:assert/strict';
import { TASBIH_PRESETS } from '../components/TasbihSection';
import {
  MUSHAF_EDITIONS,
  getSavedEdition,
  saveEdition,
  getSavedPrayerAdjustments,
  savePrayerAdjustments,
  getSavedAyahReciter,
  saveAyahReciter,
  sanitizeWidgetSettings,
} from './appSettings';
import { AYAH_RECITERS } from './quranTajweedData';

/* ============================================================================
 * اختبارات انحدار طلبات المالك (2026-10)
 * ========================================================================== */

test('tasbih preset: الله أكبر default is 33 (was wrongly 34 — fixed per sunnah)', () => {
  const takbir = TASBIH_PRESETS.find((p) => p.arabic.includes('أَكْبَرُ'));
  assert.ok(takbir, 'preset الله أكبر must exist');
  assert.equal(
    takbir!.defaultTarget,
    33,
    `الله أكبر لازم 33 — حديث مسلم «وَكَبِّرْ ثَلَاثًا وَثَلَاثِينَ» (كان ${takbir!.defaultTarget})`
  );

  // السبحات الثلاث كلها 33
  for (const p of TASBIH_PRESETS.slice(0, 3)) {
    assert.equal(p.defaultTarget, 33, `${p.arabic} لازم 33`);
  }
});

test('mushaf editions: three complete print editions with distinct styling', () => {
  const ids = Object.keys(MUSHAF_EDITIONS);
  assert.deepEqual(ids.sort(), ['night', 'tajweed', 'uthmani']);

  for (const ed of Object.values(MUSHAF_EDITIONS)) {
    assert.ok(ed.label.length > 0, `${ed.id} missing label`);
    assert.ok(ed.paper.startsWith('#'), `${ed.id} paper must be hex color`);
    assert.ok(ed.ink.startsWith('#'), `${ed.id} ink must be hex color`);
  }

  // التجويد الملون مفعّل في طبعة التجويد والليلي، ومقفول في العثماني
  assert.equal(MUSHAF_EDITIONS.tajweed.showTajweed, true);
  assert.equal(MUSHAF_EDITIONS.night.showTajweed, true);
  assert.equal(MUSHAF_EDITIONS.uthmani.showTajweed, false);
});

test('edition persistence: save and restore across reloads (localStorage fallback safe)', () => {
  // في بيئة node مفيش localStorage — الدوال لازم ترجع الافتراضي بدون انفجار
  const before = getSavedEdition();
  assert.equal(before.id, 'tajweed', 'الافتراضي طبعة التجويد الملون');

  // محاولة الحفظ لا ترمي خطأ
  saveEdition('night');
  const after = getSavedEdition();
  if (typeof localStorage === 'undefined') {
    assert.equal(after.id, 'tajweed', 'بدون localStorage يرجع الافتراضي بأمان');
  } else {
    assert.equal(after.id, 'night', 'بعد الحفظ يرجع الليلي');
    saveEdition('tajweed');
  }
});

test('reciter persistence: safe defaults and no-crash without localStorage', () => {
  const def = getSavedAyahReciter();
  assert.ok(AYAH_RECITERS.some((r) => r.id === def.id), 'القارئ الافتراضي موجود في القائمة');

  saveAyahReciter('husary');
  if (typeof localStorage !== 'undefined') {
    assert.equal(getSavedAyahReciter().id, 'husary');
  }
});

test('prayer adjustments persistence: clamped values and safe parse', () => {
  // القيم القصوى مرفوضة (أكثر من 30 دقيقة)
  if (typeof localStorage !== 'undefined') {
    localStorage.setItem('noor_prayer_adjustments', JSON.stringify({ fajr: 999, asr: -999, maghrib: 5 }));
    const adj = getSavedPrayerAdjustments();
    assert.equal(adj.fajr, undefined, 'قيمة خارج النطاق تُرفض');
    assert.equal(adj.asr, undefined, 'قيمة خارج النطاق تُرفض');
    assert.equal(adj.maghrib, 5, 'قيمة سليمة تبقى');
    savePrayerAdjustments({});
  } else {
    // بدون localStorage: لا انفجار
    assert.deepEqual(getSavedPrayerAdjustments(), {});
    savePrayerAdjustments({ dhuhr: 1 });
    assert.deepEqual(getSavedPrayerAdjustments(), {});
  }
});

test('widget settings: sanitization rejects garbage and keeps valid values', () => {
  // قيم تالفة كلياً → الافتراضي الآمن
  const def = sanitizeWidgetSettings(null);
  assert.equal(def.accent, 'green');
  assert.equal(def.textSize, 'medium');
  assert.equal(def.bgOpacity, 100);
  assert.equal(def.timeFormat, '12h');

  // قيم غريبة تُرفض لكل حقل على حدة
  const weird = sanitizeWidgetSettings({
    accent: 'neon-pink',
    textSize: 'gigantic',
    showCity: 'yes',
    bgOpacity: 137,
    timeFormat: '13h',
  });
  assert.equal(weird.accent, 'green', 'لون غير معروف → الأخضر');
  assert.equal(weird.textSize, 'medium', 'حجم غير معروف → المتوسط');
  assert.equal(weird.showCity, true, 'قيمة غير منطقية → الافتراضي');
  assert.equal(weird.bgOpacity, 100, 'شفافية خارج النطاق → تُقرّب للافتراضي');
  assert.equal(weird.timeFormat, '12h');

  // قيم سليمة تُحفظ كما هي + تقريب الشفافية لأقرب خيار
  const good = sanitizeWidgetSettings({
    accent: 'gold',
    textSize: 'large',
    showCity: false,
    showTimesRow: false,
    bgOpacity: 42,
    timeFormat: '24h',
  });
  assert.equal(good.accent, 'gold');
  assert.equal(good.textSize, 'large');
  assert.equal(good.showCity, false);
  assert.equal(good.showTimesRow, false);
  assert.equal(good.bgOpacity, 50, '42 تُقرّب لأقرب خيار متاح (50)');
  assert.equal(good.timeFormat, '24h');
});
