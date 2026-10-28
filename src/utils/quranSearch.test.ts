import test from 'node:test';
import assert from 'node:assert/strict';
import { normalizeArabicText, parseSurahAyahQuery, searchQuran } from './quranSearch';
import { getAyahAudioUrl } from './quranTajweedData';
import { COUNTRIES_DATA } from './countriesData';

test('normalizeArabicText strips diacritics and harmonizes letters', () => {
  assert.equal(normalizeArabicText('الْحَمْدُ لِلَّهِ'), 'الحمد لله');
  assert.equal(normalizeArabicText('إِيَّاكَ نَعْبُدُ'), 'اياك نعبد');
  assert.equal(normalizeArabicText('سُورَةُ الْبَقَرَةِ'), 'سوره البقره');
});

test('parseSurahAyahQuery correctly parses colon syntax and Arabic names', () => {
  const r1 = parseSurahAyahQuery('2:255');
  assert.ok(r1);
  assert.equal(r1?.surahNumber, 2);
  assert.equal(r1?.ayahNumber, 255);

  const r2 = parseSurahAyahQuery('البقرة: 17');
  assert.ok(r2);
  assert.equal(r2?.surahNumber, 2);
  assert.equal(r2?.ayahNumber, 17);

  const r3 = parseSurahAyahQuery('سورة الكهف 10');
  assert.ok(r3);
  assert.equal(r3?.surahNumber, 18);
  assert.equal(r3?.ayahNumber, 10);
});

test('searchQuran finds matches for text query', () => {
  const results = searchQuran('نارا فلما اضاءت');
  assert.ok(results.length > 0);
  assert.equal(results[0].surahNumber, 2);
  assert.equal(results[0].ayahNumber, 17);
});

test('getAyahAudioUrl builds valid EveryAyah URL for Alafasy', () => {
  const url = getAyahAudioUrl('Alafasy_128kbps', 2, 17);
  assert.equal(url, 'https://everyayah.com/data/Alafasy_128kbps/002017.mp3');
});

test('countries data contains Egypt, Saudi Arabia, Palestine, etc.', () => {
  const egypt = COUNTRIES_DATA.find((c) => c.code === 'EG');
  assert.ok(egypt);
  assert.ok(egypt!.cities.length >= 8);
  const cairo = egypt!.cities.find((city) => city.name === 'القاهرة');
  assert.ok(cairo);
  assert.equal(cairo?.latitude, 30.0444);
});
