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
import { AndroidKotlinTab } from './components/AndroidKotlinTab';
import { Coordinates, getHijriDate } from './utils/prayerMath';
import {
  getSavedTheme,
  saveTheme,
  getSavedColorMode,
  saveColorMode,
  AppTheme,
  ColorMode,
} from './utils/themeData';

export default function App() {
  const [currentTab, setCurrentTab] = useState<ActiveTab>('prayer');

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
          ? `bg-gradient-to-br ${currentTheme.bgGradientDark} text-slate-100 selection:bg-emerald-500 selection:text-white`
          : `bg-gradient-to-br ${currentTheme.bgGradientLight} text-slate-900 selection:bg-emerald-600 selection:text-white`
      }`}
    >
      {/* Top Navigation Bar */}
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

      {/* Main Container */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6">
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

        {currentTab === 'kotlin' && <AndroidKotlinTab />}
      </main>

      {/* Footer */}
      <footer
        className={`border-t py-6 text-center text-xs transition-colors ${
          isDark
            ? 'border-slate-800/80 bg-slate-950/80 text-slate-500'
            : 'border-slate-200/80 bg-white/70 text-slate-500'
        }`}
      >
        <div className="max-w-7xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <span
              className="w-2.5 h-2.5 rounded-full"
              style={{ backgroundColor: currentTheme.primaryColor }}
            />
            <span className={isDark ? 'text-slate-400' : 'text-slate-600'}>
              نور المسلم • تطبيق أندرويد متكامل بلغة Kotlin • تصميم احترافي
            </span>
          </div>

          <div className="flex items-center gap-4 text-[11px]">
            <button
              onClick={() => setCurrentTab('qibla')}
              className="hover:underline"
              style={{ color: currentTheme.primaryColor }}
            >
              🧭 بوصلة القبلة
            </button>
            <button
              onClick={() => setCurrentTab('widgets')}
              className="text-teal-600 dark:text-teal-400 hover:underline"
            >
              استوديو الويدجتس
            </button>
            <button
              onClick={() => setCurrentTab('kotlin')}
              className="text-amber-600 dark:text-amber-400 hover:underline"
            >
              مشروع أندرويد كوتلن (ZIP)
            </button>
            <span>{hijriDate}</span>
          </div>
        </div>
      </footer>
    </div>
  );
}
