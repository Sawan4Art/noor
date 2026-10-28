import test from 'node:test';
import assert from 'node:assert/strict';
import { ADHKAR_LIST, ADHKAR_CATEGORIES } from './adhkarData';
import { SURAHS_LIST, RECITERS, getSurahAudioUrl, getOfflineStorageKey } from './quranData';
import { DEFAULT_WIDGETS, WIDGET_THEMES } from './widgetsData';
import { ANDROID_KOTLIN_PROJECT_FILES } from './androidKotlinCode';

test('after_prayer adhkar items exist and have authentic counts', () => {
  const afterPrayer = ADHKAR_LIST.filter((i) => i.category === 'after_prayer');
  assert.ok(afterPrayer.length >= 8, 'Should have comprehensive after-prayer adhkar');
  
  const tasbih = afterPrayer.find((i) => i.text.includes('سُبْحَانَ اللهِ'));
  assert.ok(tasbih);
  assert.equal(tasbih?.count, 33);

  const istighfar = afterPrayer.find((i) => i.text.includes('أَسْتَغْفِرُ اللهَ'));
  assert.ok(istighfar);
  assert.equal(istighfar?.count, 3);
});

test('reciters and audio URL formatting are valid with 15+ reciters including Nasser Al-Qatami and Egyptian icons', () => {
  assert.ok(RECITERS.length >= 15, `Should have at least 15 famous reciters, got ${RECITERS.length}`);
  
  // Nasser Al-Qatami check (explicitly requested by user)
  const qatami = RECITERS.find((r) => r.id === 'qatami');
  assert.ok(qatami, 'Nasser Al-Qatami must be present in reciters');
  assert.ok(qatami!.name.includes('ناصر القطامي'));

  // Egyptian giants check
  const egyptianReciters = RECITERS.filter((r) => r.country?.includes('مصر'));
  assert.ok(egyptianReciters.length >= 6, 'Must include famous Egyptian reciters (Husary, Minshawi, Abdulbasit, etc.)');
  assert.ok(RECITERS.some((r) => r.id === 'husary'));
  assert.ok(RECITERS.some((r) => r.id === 'minshawy'));
  assert.ok(RECITERS.some((r) => r.id === 'abdulbasit'));
  assert.ok(RECITERS.some((r) => r.id === 'mustafa_ismail'));
  assert.ok(RECITERS.some((r) => r.id === 'tablawi'));

  const mishary = RECITERS.find((r) => r.id === 'mishary');
  assert.ok(mishary);
  const audioUrl = getSurahAudioUrl(mishary!.serverUrl, 1);
  assert.ok(audioUrl.endsWith('001.mp3'), `Audio url should end with 001.mp3, got ${audioUrl}`);
});

test('widgets have complete themes and configs', () => {
  assert.ok(DEFAULT_WIDGETS.length >= 5, 'Should provide many widgets');
  for (const w of DEFAULT_WIDGETS) {
    assert.ok(WIDGET_THEMES[w.theme], `Theme ${w.theme} must exist`);
    assert.ok(w.borderRadius > 0);
  }
});

test('android kotlin project files are complete', () => {
  assert.ok(ANDROID_KOTLIN_PROJECT_FILES.length >= 5);
  const glanceWidget = ANDROID_KOTLIN_PROJECT_FILES.find((f) => f.filename === 'PrayerGlanceWidget.kt');
  assert.ok(glanceWidget);
  assert.ok(glanceWidget!.content.includes('GlanceAppWidget'));
});
