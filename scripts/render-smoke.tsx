/**
 * اختبار دخان: رسم كل الأقسام الرئيسية بـ renderToString للتأكد
 * إن مفيش انهيار وقت الرسم (بدون DOM حقيقي).
 */
import React from 'react';
import { renderToString } from 'react-dom/server';

// تجاوزات البيئة قبل أي استيراد
const g = globalThis as unknown as Record<string, unknown>;
if (typeof g.localStorage === 'undefined' || g.localStorage === null) {
  const store = new Map<string, string>();
  g.localStorage = {
    getItem: (k: string) => store.get(k) ?? null,
    setItem: (k: string, v: string) => void store.set(k, v),
    removeItem: (k: string) => void store.delete(k),
    clear: () => store.clear(),
    key: (i: number) => Array.from(store.keys())[i] ?? null,
    length: 0,
  };
}
if (typeof g.window === 'undefined')
  g.window = { addEventListener: () => {}, removeEventListener: () => {}, DeviceOrientationEvent: function () {} };
if (typeof g.navigator === 'undefined') g.navigator = { vibrate: () => {} };
if (typeof g.document === 'undefined')
  g.document = { visibilityState: 'visible', addEventListener: () => {}, removeEventListener: () => {} };

const { PrayerTimesSection } = await import('../src/components/PrayerTimesSection');
const { AdhkarSection } = await import('../src/components/AdhkarSection');
const { SplashDua } = await import('../src/components/SplashDua');
const { Navbar } = await import('../src/components/Navbar');
const { MushafInteractiveReader } = await import('../src/components/MushafInteractiveReader');
const { SettingsSection } = await import('../src/components/SettingsSection');
const { default: App } = await import('../src/App');
const { APP_THEMES } = await import('../src/utils/themeData');

const coords = { city: 'القاهرة، مصر', latitude: 30.0444, longitude: 31.2357 };
const theme = APP_THEMES[0];
const alerts = {
  fajr: 'adhan',
  sunrise: 'notification',
  dhuhr: 'adhan',
  asr: 'adhan',
  maghrib: 'adhan',
  isha: 'adhan',
} as const;

let failures = 0;
const check = (name: string, el: React.ReactElement) => {
  try {
    const html = renderToString(el);
    if (!html || html.length < 40) throw new Error(`html قصير جداً (${html.length})`);
    console.log(`✅ ${name} (${html.length} حرف)`);
  } catch (e) {
    failures++;
    console.error(`❌ ${name}: ${e instanceof Error ? e.message : String(e)}`);
  }
};

check('PrayerTimesSection', <PrayerTimesSection coordinates={coords} method="Egypt" isHanafi={false} />);
check('AdhkarSection', <AdhkarSection />);
check('SplashDua', <SplashDua onDone={() => {}} currentTheme={theme} colorMode="dark" />);
check('Navbar', (
  <Navbar
    currentTab="prayer"
    setCurrentTab={() => {}}
    hijriString="١٤٤٧"
    currentTheme={theme}
    colorMode="dark"
    setColorMode={() => {}}
  />
));
check('MushafInteractiveReader', <MushafInteractiveReader />);
check('SettingsSection', (
  <SettingsSection
    currentTheme={theme}
    setCurrentTheme={() => {}}
    colorMode="dark"
    setColorMode={() => {}}
    coordinates={coords}
    setCoordinates={() => {}}
    method="Egypt"
    setMethod={() => {}}
    isHanafi={false}
    setIsHanafi={() => {}}
    adhanVoiceId="main"
    setAdhanVoiceId={() => {}}
    adhanEnabled
    setAdhanEnabled={() => {}}
    prayerAlerts={alerts}
    setPrayerAlerts={() => {}}
    qiyamEnabled={false}
    setQiyamEnabled={() => {}}
    qiyamOffsetMinutes={60}
    setQiyamOffsetMinutes={() => {}}
  />
));
check('App كامل', <App />);

if (failures > 0) {
  console.error(`\n💥 ${failures} قسم فشل في الرسم`);
  process.exit(1);
}
console.log('\n🎉 كل الأقسام ترسم بدون انهيار');
