import test from 'node:test';
import assert from 'node:assert/strict';
import {
  computePrayerTimes,
  getNextPrayer,
  calculateQibla,
  getHijriDate,
  CALCULATION_METHODS,
} from './prayerMath.ts';

/**
 * القيم المرجعية من Aladhan API بتوقيت UTC (مصدر مستقل):
 * https://api.aladhan.com/v1/timings/29-10-2026?latitude=30.0444&longitude=31.2357&method=5&timezonestring=UTC
 * الاختبار محمول لأي منطقة زمنية: نقارن بالساعة المحلية = UTC + إزاحة الجهاز.
 * تسامح ±4 دقائق (الفروق الفعلية ±1 دقيقة).
 */
const REF_2026_10_29 = {
  cairo: {
    lat: 30.0444, lng: 31.2357, method: 'Egypt' as const,
    fajr: [2, 39], sunrise: [4, 7], dhuhr: [9, 39], asr: [12, 48], maghrib: [15, 11], isha: [16, 29],
  },
  makkah: {
    lat: 21.4225, lng: 39.8262, method: 'Makkah' as const,
    fajr: [2, 5], sunrise: [3, 22], dhuhr: [9, 4], asr: [12, 21], maghrib: [14, 46], isha: [16, 16],
  },
};

const PRAYER_KEYS = ['fajr', 'sunrise', 'dhuhr', 'asr', 'maghrib', 'isha'] as const;

test('prayer times match independent astronomical reference (Cairo & Makkah, ±4 min)', () => {
  // 29 أكتوبر 2026 — نفس يوم القيم المرجعية
  const date = new Date(2026, 9, 29, 12, 0, 0);
  const tzHours = -date.getTimezoneOffset() / 60;

  for (const [city, cfg] of Object.entries(REF_2026_10_29)) {
    const result = computePrayerTimes(date, cfg.lat, cfg.lng, cfg.method);
    for (const key of PRAYER_KEYS) {
      const [uh, um] = cfg[key];
      const expectedLocal = (uh + um / 60 + tzHours + 24) % 24;
      const d = result.rawTimes[key];
      const actualLocal = d.getHours() + d.getMinutes() / 60;
      const diff = Math.abs(((actualLocal - expectedLocal + 36) % 24) - 12) <= 4 / 60 + 0.0001
        ? 0
        : Math.round(Math.abs(((actualLocal - expectedLocal + 12) % 24) - 12) * 60);
      assert.ok(
        Math.abs(((actualLocal - expectedLocal + 36) % 24) - 12) <= 4 / 60 + 0.0001,
        `${city}.${key}: متوقع ${expectedLocal.toFixed(2)}h فعلي ${actualLocal.toFixed(2)}h (فرق ${diff} دقيقة)`
      );
    }
  }
});

test('prayer times compute valid non-empty times for Cairo', () => {
  const cairoDate = new Date('2026-10-28T12:00:00Z');
  const result = computePrayerTimes(cairoDate, 30.0444, 31.2357, 'Egypt');

  assert.ok(result.fajr, 'Fajr should exist');
  assert.ok(result.dhuhr, 'Dhuhr should exist');
  assert.ok(result.asr, 'Asr should exist');
  assert.ok(result.maghrib, 'Maghrib should exist');
  assert.ok(result.isha, 'Isha should exist');
  assert.ok(result.rawTimes.fajr instanceof Date, 'rawTimes should contain Dates');

  // ترتيب يومي سليم: الفجر قبل الشروق قبل الظهر قبل العصر قبل المغرب قبل العشاء
  const t = result.rawTimes;
  assert.ok(t.fajr < t.sunrise && t.sunrise < t.dhuhr && t.dhuhr < t.asr && t.asr < t.maghrib && t.maghrib < t.isha);
});

test('calculateQibla returns accurate angle towards Kaaba from Cairo (~135-137 deg)', () => {
  const qiblaAngle = calculateQibla(30.0444, 31.2357);
  // From Cairo, Kaaba is approximately south-southeast (around 136 degrees)
  assert.ok(qiblaAngle > 130 && qiblaAngle < 145, `Qibla angle ${qiblaAngle} is within Cairo range`);
});

test('getNextPrayer correctly detects upcoming prayer and countdown', () => {
  const date = new Date('2026-10-28T05:00:00Z');
  const result = computePrayerTimes(date, 30.0444, 31.2357, 'Egypt');

  // Test at a moment before Fajr
  const earlyMorning = new Date(result.rawTimes.fajr.getTime() - 1000 * 60 * 30); // 30 min before fajr
  const next = getNextPrayer(result, earlyMorning);

  assert.equal(next.name, 'fajr');
  assert.ok(next.remainingSeconds > 0);
  assert.ok(next.formattedRemaining.includes(':'));
  assert.ok(next.arabicName.includes('الفجر'));
});

test('getHijriDate produces Arabic formatted hijri string', () => {
  const hijri = getHijriDate(new Date('2026-10-28'));
  assert.ok(hijri.formatted.includes('هـ'));
  assert.ok(hijri.year >= 1445);
  assert.ok(hijri.monthName.length > 0);
});

test('calculation methods are defined with proper angles', () => {
  assert.equal(CALCULATION_METHODS.Egypt.fajrAngle, 19.5);
  assert.equal(CALCULATION_METHODS.Makkah.ishaInterval, 90);
});
