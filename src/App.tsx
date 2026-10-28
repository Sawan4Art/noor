/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import React, { useState, useEffect } from 'react';
import { Navbar, ActiveTab } from './components/Navbar';
import { PrayerTimesSection } from './components/PrayerTimesSection';
import { QuranSection } from './components/QuranSection';
import { QiblaCompassSection } from './components/QiblaCompassSection';
import { AdhkarSection } from './components/AdhkarSection';
import { TasbihSection } from './components/TasbihSection';
import { WidgetsStudioSection } from './components/WidgetsStudioSection';
import { SplashDua } from './components/SplashDua';
import { UpdateBanner } from './components/UpdateBanner';
import { Coordinates, getHijriDate } from './utils/prayerMath';
import { APP_VERSION } from './utils/otaUpdate';
import {
  getSavedTheme,
  saveTheme,
  getSavedColorMode,
  saveColorMode,
  applyThemePalette,
  AppTheme,
  ColorMode,
} from './utils/themeData';

export default function App() {
  const [currentTab, setCurrentTab] = useState<ActiveTab>('prayer');
  const [showSplash, setShowSplash] = useState<boolean>(true);

  const [coordinates, setCoordinates] = useState<Coordinates>({
    city: 'القاهرة، مصر',
    latitude: 30.0444,
    longitude: 31.2357,
  });

  const [soundEnabled, setSoundEnabled] = useState<boolean>(true);
  const [hijriDate, setHijriDate] = useState<string>('');

  // Theme & Color Mode State
  const [currentTheme, setCurrentTheme] = useState<AppTheme>(getSavedTheme);
  const [colorMode, setColorMode] = useState<ColorMode>(getSavedColorMode);

  useEffect(() => {
    const h = getHijriDate(new Date());
    setHijriDate(h.formatted);
  }, []);

  // الثيم الشامل: توليد طيف الألوان وحقنه في متغيرات CSS مع كل تغيير
  useEffect(() => {
    applyThemePalette(currentTheme, colorMode);
  }, [currentTheme, colorMode]);

  // Sync theme changes with localStorage and root HTML classes
  const handleSelectTheme = (theme: AppTheme) => {
    setCurrentTheme(theme);
    saveTheme(theme.id);
  };

  const handleToggleColorMode = (mode: ColorMode) => {
    setColorMode(mode);
    saveColorMode(mode);
  };

  useEffect(() => {
    const root = document.documentElement;
    if (colorMode === 'dark') {
      root.classList.add('dark');
    } else {
      root.classList.remove('dark');
    }
  }, [colorMode]);

  const isDark = colorMode === 'dark';

  return (
    <div
      className={`min-h-screen flex flex-col font-arabic transition-colors duration-300 ${
        isDark
          ? `bg-gradient-to-br ${currentTheme.bgGradientDark} text-slate-100`
          : `bg-gradient-to-br ${currentTheme.bgGradientLight} text-slate-900`
      }`}
    >
      {/* شاشة السبلاش — دعاء لأبي المرحوم + الصدقة الجارية */}
      {showSplash && (
        <SplashDua
          onDone={() => setShowSplash(false)}
          currentTheme={currentTheme}
          colorMode={colorMode}
        />
      )}

      {/* الهيدر العلوي + شريط التنقل السفلي */}
      <Navbar
        currentTab={currentTab}
        setCurrentTab={setCurrentTab}
        coordinates={coordinates}
        setCoordinates={setCoordinates}
        soundEnabled={soundEnabled}
        setSoundEnabled={setSoundEnabled}
        hijriString={hijriDate}
        currentTheme={currentTheme}
        setCurrentTheme={handleSelectTheme}
        colorMode={colorMode}
        setColorMode={handleToggleColorMode}
      />

      {/* Main Container — padding سفلي عشان شريط التنقل الثابت */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6 pb-32">
        {currentTab === 'prayer' && (
          <PrayerTimesSection
            coordinates={coordinates}
            setCoordinates={setCoordinates}
            soundEnabled={soundEnabled}
            onOpenAdhkarAfterPrayer={() => setCurrentTab('adhkar')}
            onOpenQibla={() => setCurrentTab('qibla')}
          />
        )}

        {currentTab === 'quran' && <QuranSection />}

        {currentTab === 'qibla' && (
          <QiblaCompassSection
            coordinates={coordinates}
            setCoordinates={setCoordinates}
            theme={currentTheme}
            colorMode={colorMode}
            onNavigateToTab={(tab) => setCurrentTab(tab)}
          />
        )}

        {currentTab === 'adhkar' && <AdhkarSection />}

        {currentTab === 'tasbih' && <TasbihSection />}

        {currentTab === 'widgets' && (
          <WidgetsStudioSection
            coordinates={coordinates}
            onNavigateToTab={(tab) => setCurrentTab(tab)}
          />
        )}
      </main>

      {/* بانر التحديث الهوائي (OTA) */}
      <UpdateBanner />

      {/* Footer — الصدقة الجارية + اسم المطور */}
      <footer
        className="border-t py-5 text-center text-xs transition-colors"
        style={{
          borderColor: isDark
            ? 'color-mix(in srgb, var(--noor-p-500) 18%, transparent)'
            : 'color-mix(in srgb, var(--noor-p-500) 14%, transparent)',
          color: 'var(--noor-n-500)',
        }}
      >
        <div className="max-w-7xl mx-auto px-4 space-y-1.5">
          <p className="font-bold" style={{ color: currentTheme.accentColor }}>
            هذا التطبيق صدقة جارية على روح المرحوم بأذن الله أبي
          </p>
          <div className="flex items-center justify-center gap-2 text-[11px]">
            <span
              className="w-2 h-2 rounded-full inline-block"
              style={{ backgroundColor: currentTheme.primaryColor }}
            />
            <span>نور المسلم • تطوير: Yousef Sawan</span>
            <span className="opacity-50">•</span>
            <span>إصدار {APP_VERSION}</span>
            <span className="opacity-50">•</span>
            <span>{hijriDate}</span>
          </div>
        </div>
      </footer>
    </div>
  );
}
