import test from 'node:test';
import assert from 'node:assert/strict';
import {
  computePrayerTimes,
  getNextPrayer,
  calculateQibla,
  getHijriDate,
  CALCULATION_METHODS,
} from './prayerMath.ts';

test('prayer times compute valid non-empty times for Cairo', () => {
  const cairoDate = new Date('2026-10-28T12:00:00Z');
  const result = computePrayerTimes(cairoDate, 30.0444, 31.2357, 'Egypt');

  assert.ok(result.fajr, 'Fajr should exist');
  assert.ok(result.dhuhr, 'Dhuhr should exist');
  assert.ok(result.asr, 'Asr should exist');
  assert.ok(result.maghrib, 'Maghrib should exist');
  assert.ok(result.isha, 'Isha should exist');
  assert.ok(result.rawTimes.fajr instanceof Date, 'rawTimes should contain Dates');
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
