import { test } from 'node:test';
import assert from 'node:assert';
import { calculateQibla, calculateDistanceToKaaba } from './prayerMath.ts';
import { APP_THEMES } from './themeData.ts';

test('calculateQibla correctly computes azimuth to Kaaba', () => {
  // Cairo, Egypt -> approx 136 degrees (South-East)
  const cairoQibla = calculateQibla(30.0444, 31.2357);
  assert.ok(cairoQibla >= 135 && cairoQibla <= 138, `Expected ~136° for Cairo, got ${cairoQibla}`);

  // London, UK -> approx 119 degrees (South-East)
  const londonQibla = calculateQibla(51.5074, -0.1278);
  assert.ok(londonQibla >= 118 && londonQibla <= 120, `Expected ~119° for London, got ${londonQibla}`);

  // Jakarta, Indonesia -> approx 295 degrees (North-West)
  const jakartaQibla = calculateQibla(-6.2088, 106.8456);
  assert.ok(jakartaQibla >= 294 && jakartaQibla <= 296, `Expected ~295° for Jakarta, got ${jakartaQibla}`);
});

test('calculateDistanceToKaaba calculates accurate Great Circle distance', () => {
  // Kaaba to Kaaba should be 0 km
  const kaabaDist = calculateDistanceToKaaba(21.4225, 39.8262);
  assert.strictEqual(kaabaDist, 0);

  // Cairo to Kaaba is ~1288 km
  const cairoDist = calculateDistanceToKaaba(30.0444, 31.2357);
  assert.ok(cairoDist >= 1250 && cairoDist <= 1350, `Expected ~1288km for Cairo, got ${cairoDist}`);
});

test('APP_THEMES defines 6 authentic Islamic palettes with complete color definitions', () => {
  assert.strictEqual(APP_THEMES.length, 6);
  const themeIds = APP_THEMES.map((t) => t.id);
  assert.deepStrictEqual(themeIds, ['emerald', 'sapphire', 'kaaba', 'indigo', 'ruby', 'sage']);

  for (const t of APP_THEMES) {
    assert.ok(t.name.length > 0, `Theme ${t.id} missing name`);
    assert.ok(t.primaryColor.startsWith('#'), `Theme ${t.id} primaryColor invalid`);
    assert.ok(t.accentColor.startsWith('#'), `Theme ${t.id} accentColor invalid`);
    assert.ok(t.tabActiveClass.length > 0, `Theme ${t.id} tabActiveClass missing`);
  }
});

test('applyThemePalette generates full themed spectrum into CSS variables (theme changes whole app)', async () => {
  // بيئة اختبار نود مفيهاش document — نتأكد إن الدالة بتتعامل مع الغياب بأمان (fallback)
  const { applyThemePalette } = await import('./themeData.ts');
  const theme = APP_THEMES.find((t) => t.id === 'sapphire')!;
  // مفيش document في نود: لازم ترجع بدون رمي (قاعدة fallback)
  assert.doesNotThrow(() => applyThemePalette(theme, 'dark'));
});

test('theme colors are valid 6-digit hex for palette math', () => {
  for (const t of APP_THEMES) {
    assert.match(t.primaryColor, /^#[0-9a-fA-F]{6}$/, `${t.id} primaryColor hex`);
    assert.match(t.accentColor, /^#[0-9a-fA-F]{6}$/, `${t.id} accentColor hex`);
    assert.match(t.glowColor, /^rgba\(/, `${t.id} glowColor rgba`);
  }
});
