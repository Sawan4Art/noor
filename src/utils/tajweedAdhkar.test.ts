import test from 'node:test';
import assert from 'node:assert/strict';
import { parseTajweedMarkup, TAJWEED_RULES } from './quranTajweedData';
import { ADHKAR_CATEGORIES, ADHKAR_LIST } from './adhkarData';
import {
  ADHAN_VOICES,
  getAdhanAudioSrc,
  getSavedPrayerAlerts,
  DEFAULT_PRAYER_ALERTS,
} from './appSettings';

test('tajweed parser: plain text returns single segment', () => {
  assert.deepEqual(parseTajweedMarkup('بِسْمِ اللهِ'), [{ text: 'بِسْمِ اللهِ' }]);
  assert.deepEqual(parseTajweedMarkup(''), []);
});

test('tajweed parser: rule without number decodes to colored segment', () => {
  const segs = parseTajweedMarkup('ل[o[َٓ] إِلَ[n[ـٰ]');
  assert.deepEqual(segs, [
    { text: 'ل' },
    { text: 'َٓ', rule: 'madda_obligatory' },
    { text: ' إِلَ' },
    { text: 'ـٰ', rule: 'madda_normal' },
  ]);
});

test('tajweed parser: rule with number (idgham ghunnah) and numbered hamzat wasl', () => {
  const segs = parseTajweedMarkup('سِنَ[a:1470[ةٌ و]َلَا [h:609[ٱ]');
  const ghunnah = segs.find((s) => s.rule === 'idgham_ghunnah');
  const wasl = segs.find((s) => s.rule === 'hamzat_wasl');
  assert.equal(ghunnah?.text, 'ةٌ و');
  assert.equal(wasl?.text, 'ٱ');
});

test('tajweed parser: reassembly removes all markup brackets (al-fatiha 1)', () => {
  const raw = 'بِسْمِ [h:1[ٱ]لَّهِ [l[ل]رَّحْمَ[n[ـٰ]نِ [h:2[ٱ][l[ل]رَّح[p[ِي]مِ';
  const segs = parseTajweedMarkup(raw);
  const joined = segs.map((s) => s.text).join('');
  assert.ok(!joined.includes('['), 'no brackets should remain');
  assert.ok(!joined.includes(']'), 'no brackets should remain');
  assert.ok(joined.startsWith('بِسْمِ'));
  assert.ok(joined.includes('ٱلرَّح'));
});

test('tajweed rules: all 17 standard rules have valid day and night colors', () => {
  const rules = Object.values(TAJWEED_RULES);
  assert.equal(rules.length, 17);
  for (const r of rules) {
    assert.ok(/^#[0-9a-fA-F]{6}$/.test(r.color), `bad color: ${r.color}`);
    assert.ok(/^#[0-9a-fA-F]{6}$/.test(r.nightColor), `bad nightColor: ${r.nightColor}`);
    assert.ok(r.label.length > 2);
  }
});

test('tajweed parser: real sample ayat al-kursi starts with madda wajib', () => {
  const segs = parseTajweedMarkup('ٱللَّهُ ل[o[َٓ] إِلَ[n[ـٰ]هَ');
  assert.equal(segs[1]?.rule, 'madda_obligatory');
  assert.equal(segs[3]?.rule, 'madda_normal');
});

test('expanded adhkar: 12 categories all have items', () => {
  assert.equal(ADHKAR_CATEGORIES.length, 12);
  const cats = new Set(ADHKAR_LIST.map((d) => d.category));
  for (const c of ADHKAR_CATEGORIES) {
    assert.ok(cats.has(c.id), `category ${c.id} has no items`);
  }
});

test('expanded adhkar: travel adhkar exist with 4+ items', () => {
  const travel = ADHKAR_LIST.filter((d) => d.category === 'travel');
  assert.ok(travel.length >= 4);
  assert.ok(travel[0].text.includes('سَخَّرَ'));
});

test('expanded adhkar: every item has positive count and text', () => {
  for (const d of ADHKAR_LIST) {
    assert.ok(d.count > 0, `bad count for ${d.id}`);
    assert.ok(d.text.length > 5, `bad text for ${d.id}`);
  }
});

test('bundled adhan voices: 4 voices with working preview paths', () => {
  assert.equal(ADHAN_VOICES.length, 4);
  assert.equal(getAdhanAudioSrc('main'), '/assets/adhan/main.mp3');
  assert.equal(getAdhanAudioSrc('fakhry'), '/assets/adhan/fakhry.mp3');
  assert.equal(getAdhanAudioSrc('traditional'), '/assets/adhan/traditional.ogg');
  assert.equal(getAdhanAudioSrc('unknown-id'), '/assets/adhan/main.mp3');
});

test('prayer alerts defaults: adhan for prayers, notification for sunrise', () => {
  assert.equal(DEFAULT_PRAYER_ALERTS.fajr, 'adhan');
  assert.equal(DEFAULT_PRAYER_ALERTS.sunrise, 'notification');
  assert.equal(DEFAULT_PRAYER_ALERTS.isha, 'adhan');
  assert.equal(Object.keys(getSavedPrayerAlerts()).length, 6);
});
