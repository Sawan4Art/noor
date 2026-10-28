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

/* ============================================================================
 * اختبارات إصلاح الدقة (2026-10) — جذر تأخر المواقيت ~1 دقيقة:
 * موضع الشمس كان يُقرأ مرة واحدة عند منتصف الليل؛ الحل الجديد يقيّم الميل
 * ومعادلة الزمن عند وقت كل صلاة بالتكرار. التحقق: زاوية الشمس الفعلية عند
 * كل وقت محسوب يجب أن تطابق الزاوية المستهدفة بدقة عالية جداً.
 * ========================================================================== */

/** مرجع NOAA فلكي دقيق (نفس معادلات المرصد) لاستخراج زاوية الشمس */
function noaaSolarPosition(jd: number): { decl: number; eqTime: number } {
  const t = (jd - 2451545.0) / 36525.0;
  const L0 = (280.46646 + t * (36000.76983 + t * 0.0003032)) % 360;
  const M = (357.52911 + t * (35999.05029 - 0.0001537 * t)) % 360;
  const e = 0.016708634 - t * (0.000042037 + 0.0000001267 * t);
  const rad = Math.PI / 180;
  const Mrad = M * rad;
  const C =
    Math.sin(Mrad) * (1.914602 - t * (0.004817 + 0.000014 * t)) +
    Math.sin(2 * Mrad) * (0.019993 - 0.000101 * t) +
    Math.sin(3 * Mrad) * 0.000289;
  const trueLong = L0 + C;
  const omega = 125.04 - 1934.136 * t;
  const lambda = trueLong - 0.00569 - 0.00478 * Math.sin(omega * rad);
  const seconds = 21.448 - t * (46.815 + t * (0.00059 - t * 0.001813));
  const e0 = seconds / 3600 + 23.0 + 26.0 / 60.0;
  const decl = Math.asin(Math.sin(lambda * rad) * Math.sin(e0 * rad)) / rad;
  const y = Math.tan((e0 / 2) * rad) ** 2;
  const eqTime =
    (4 *
      (y * Math.sin(2 * L0 * rad) -
        2 * e * Math.sin(Mrad) +
        4 * e * y * Math.sin(Mrad) * Math.cos(2 * L0 * rad) -
        0.5 * y * y * Math.sin(4 * L0 * rad) -
        1.25 * e * e * Math.sin(2 * Mrad))) /
    rad;
  return { decl, eqTime };
}

/** زاوية الشمس عند تاريخ/ساعة UTC وموقع — مرجع مستقل تماماً عن الخوارزمية */
function sunAltitudeNOAA(date: Date, hourUT: number, lat: number, lng: number): number {
  const rad = Math.PI / 180;
  const y = date.getUTCFullYear();
  const m = date.getUTCMonth() + 1;
  const d = date.getUTCDate();
  const jd0 =
    Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + d - 1537.5;
  const { decl, eqTime } = noaaSolarPosition(jd0 + hourUT / 24);
  const solarTime = hourUT + eqTime / 60 + lng / 15;
  const ha = (solarTime - 12) * 15;
  return (
    Math.asin(
      Math.sin(lat * rad) * Math.sin(decl * rad) +
        Math.cos(lat * rad) * Math.cos(decl * rad) * Math.cos(ha * rad)
    ) / rad
  );
}

test('solar altitude at computed prayer times matches target angles (iteration fix — sub-0.1° accuracy)', () => {
  const date = new Date(2026, 9, 28, 12, 0, 0);
  const lat = 30.0444;
  const lng = 31.2357;
  const result = computePrayerTimes(date, lat, lng, 'Egypt');
  const tz = -date.getTimezoneOffset() / 60;
  const utcDate = new Date(Date.UTC(2026, 9, 28));

  const checkAngle = (key: 'fajr' | 'sunrise' | 'maghrib' | 'isha', target: number) => {
    const t = result.rawTimes[key];
    const hourUT = t.getHours() + t.getMinutes() / 60 + t.getSeconds() / 3600 - tz;
    const alt = sunAltitudeNOAA(utcDate, hourUT, lat, lng);
    assert.ok(
      Math.abs(alt - target) < 0.1,
      `${key}: زاوية الشمس ${alt.toFixed(3)}° بعيدة عن المستهدف ${target}° — التكرار مكسور؟`
    );
  };

  checkAngle('fajr', -19.5);
  checkAngle('sunrise', -0.833);
  checkAngle('maghrib', -0.833);
  checkAngle('isha', -17.5);
});

test('per-prayer minute adjustments shift only the targeted prayer', () => {
  const date = new Date(2026, 9, 28, 12, 0, 0);
  const base = computePrayerTimes(date, 30.0444, 31.2357, 'Egypt');
  const adjusted = computePrayerTimes(date, 30.0444, 31.2357, 'Egypt', false, {
    maghrib: -2,
    fajr: 3,
  });

  const mins = (d: Date) => d.getHours() * 60 + d.getMinutes() + d.getSeconds() / 60;

  // المغرب تقديم دقيقتين
  assert.ok(
    Math.abs(mins(base.rawTimes.maghrib) - mins(adjusted.rawTimes.maghrib) - 2) < 0.02,
    'تعديل المغرب -2 لم يُطبق'
  );
  // الفجر تأخير 3 دقائق
  assert.ok(
    Math.abs(mins(adjusted.rawTimes.fajr) - mins(base.rawTimes.fajr) - 3) < 0.02,
    'تعديل الفجر +3 لم يُطبق'
  );
  // باقي المواقيت لم تتأثر
  assert.equal(mins(base.rawTimes.dhuhr), mins(adjusted.rawTimes.dhuhr));
  assert.equal(mins(base.rawTimes.isha), mins(adjusted.rawTimes.isha));
});
