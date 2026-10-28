import React, { useState } from 'react';
import {
  Compass,
  Moon,
  Sun,
  Volume2,
  VolumeX,
  Download,
  Smartphone,
  MapPin,
  Palette,
  Sparkles,
} from 'lucide-react';
import { Coordinates } from '../utils/prayerMath';
import { LocationPickerModal } from './LocationPickerModal';
import { ThemeSelectorModal } from './ThemeSelectorModal';
import { AppTheme, ColorMode } from '../utils/themeData';

export type ActiveTab =
  | 'prayer'
  | 'quran'
  | 'qibla'
  | 'adhkar'
  | 'tasbih'
  | 'widgets'
  | 'kotlin';

interface NavbarProps {
  currentTab: ActiveTab;
  setCurrentTab: (tab: ActiveTab) => void;
  coordinates: Coordinates;
  setCoordinates: (coords: Coordinates) => void;
  soundEnabled: boolean;
  setSoundEnabled: (enabled: boolean) => void;
  hijriString: string;
  currentTheme: AppTheme;
  setCurrentTheme: (theme: AppTheme) => void;
  colorMode: ColorMode;
  setColorMode: (mode: ColorMode) => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  currentTab,
  setCurrentTab,
  coordinates,
  setCoordinates,
  soundEnabled,
  setSoundEnabled,
  hijriString,
  currentTheme,
  setCurrentTheme,
  colorMode,
  setColorMode,
}) => {
  const [showLocationModal, setShowLocationModal] = useState<boolean>(false);
  const [showThemeModal, setShowThemeModal] = useState<boolean>(false);

  const isDark = colorMode === 'dark';

  return (
    <header
      className={`sticky top-0 z-40 backdrop-blur-md border-b transition-colors ${
        isDark
          ? 'bg-slate-950/90 border-slate-800'
          : 'bg-white/95 border-slate-200 shadow-sm'
      }`}
    >
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo & Brand */}
          <div className="flex items-center gap-3">
            <div
              className="w-10 h-10 rounded-xl flex items-center justify-center shadow-lg ring-1 ring-white/20"
              style={{
                background: `linear-gradient(135deg, ${currentTheme.primaryColor}, ${currentTheme.accentColor})`,
              }}
            >
              <span className="text-xl font-bold text-white">🕌</span>
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1
                  className="text-lg font-bold bg-clip-text text-transparent"
                  style={{
                    backgroundImage: `linear-gradient(to right, ${currentTheme.primaryColor}, ${currentTheme.accentColor})`,
                  }}
                >
                  نور المسلم
                </h1>
                <span
                  className="text-[10px] px-2 py-0.5 rounded-full border font-medium"
                  style={{
                    backgroundColor: `${currentTheme.primaryColor}15`,
                    borderColor: `${currentTheme.primaryColor}40`,
                    color: currentTheme.primaryColor,
                  }}
                >
                  Kotlin + Android
                </span>
              </div>
              <p className="text-xs text-slate-500 dark:text-slate-400 hidden sm:block">
                {hijriString}
              </p>
            </div>
          </div>

          {/* Location, Theme & Audio Controls */}
          <div className="flex items-center gap-1.5 sm:gap-2.5">
            {/* GPS Location Button */}
            <button
              onClick={() => setShowLocationModal(true)}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl border text-xs transition-colors shadow-sm ${
                isDark
                  ? 'bg-slate-900/90 hover:bg-slate-800 border-slate-700 text-slate-200'
                  : 'bg-slate-100 hover:bg-slate-200/80 border-slate-200 text-slate-800'
              }`}
              title="تحديد مواقيت الصلاة عبر GPS أو باختيار البلد ثم المدينة"
            >
              <MapPin className="w-3.5 h-3.5 text-emerald-500" />
              <span className="max-w-[110px] sm:max-w-[150px] truncate">{coordinates.city}</span>
            </button>

            {/* Theme Selector Button */}
            <button
              onClick={() => setShowThemeModal(true)}
              className={`flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl border text-xs transition-all shadow-sm ${
                isDark
                  ? 'bg-slate-900/90 hover:bg-slate-800 border-slate-700 text-slate-200'
                  : 'bg-slate-100 hover:bg-slate-200 border-slate-200 text-slate-800'
              }`}
              title="اختيار نمط وثيم التطبيق"
              aria-label="اختيار النمط اللوني للتطبيق"
            >
              <div
                className="w-3.5 h-3.5 rounded-full border border-white/50 shadow-inner"
                style={{
                  background: `linear-gradient(135deg, ${currentTheme.primaryColor}, ${currentTheme.accentColor})`,
                }}
              />
              <span className="hidden md:inline font-medium">{currentTheme.name}</span>
            </button>

            {/* Light / Dark Mode Toggle */}
            <button
              onClick={() => setColorMode(isDark ? 'light' : 'dark')}
              className={`p-2 rounded-xl border transition-colors ${
                isDark
                  ? 'bg-slate-900 text-amber-300 border-slate-700 hover:bg-slate-800'
                  : 'bg-slate-100 text-slate-700 border-slate-200 hover:bg-slate-200'
              }`}
              title={isDark ? 'التبديل إلى الوضع النهاري (الفاتح)' : 'التبديل إلى الوضع الليلي (الداكن)'}
              aria-label="تبديل وضع الإضاءة"
            >
              {isDark ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
            </button>

            {/* Audio Toggle */}
            <button
              onClick={() => setSoundEnabled(!soundEnabled)}
              className={`p-2 rounded-xl border transition-colors ${
                soundEnabled
                  ? 'bg-emerald-500/20 text-emerald-600 dark:text-emerald-300 border-emerald-500/40'
                  : 'bg-slate-900 text-slate-400 border-slate-800 hover:text-slate-200'
              }`}
              title={soundEnabled ? 'صوت الأذان والتنبيهات مفعّل' : 'صامت'}
              aria-label="تبديل صوت التنبيهات"
            >
              {soundEnabled ? <Volume2 className="w-4 h-4" /> : <VolumeX className="w-4 h-4" />}
            </button>
          </div>
        </div>

        {/* Navigation Tabs */}
        <nav
          className="flex space-x-1 space-x-reverse overflow-x-auto py-2 no-scrollbar border-t border-slate-800/40"
          aria-label="أقسام التطبيق"
        >
          {/* 1. Prayer Times */}
          <button
            onClick={() => setCurrentTab('prayer')}
            className={`flex items-center gap-2 px-3.5 py-1.5 rounded-xl text-xs sm:text-sm font-medium transition-all whitespace-nowrap ${
              currentTab === 'prayer'
                ? currentTheme.tabActiveClass
                : isDark
                ? 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/80'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
            }`}
          >
            <span>📢</span>
            <span>المؤذن والمواقيت</span>
          </button>

          {/* 2. Quran & Reciters */}
          <button
            onClick={() => setCurrentTab('quran')}
            className={`flex items-center gap-2 px-3.5 py-1.5 rounded-xl text-xs sm:text-sm font-medium transition-all whitespace-nowrap ${
              currentTab === 'quran'
                ? currentTheme.tabActiveClass
                : isDark
                ? 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/80'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
            }`}
          >
            <span>📖</span>
            <span>المصحف والقراء</span>
          </button>

          {/* 3. Qibla Compass (New feature requested by user) */}
          <button
            onClick={() => setCurrentTab('qibla')}
            className={`flex items-center gap-2 px-3.5 py-1.5 rounded-xl text-xs sm:text-sm font-medium transition-all whitespace-nowrap ${
              currentTab === 'qibla'
                ? currentTheme.tabActiveClass
                : isDark
                ? 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/80'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
            }`}
          >
            <Compass className="w-3.5 h-3.5" />
            <span>بوصلة القبلة 🕋</span>
          </button>

          {/* 4. Adhkar */}
          <button
            onClick={() => setCurrentTab('adhkar')}
            className={`flex items-center gap-2 px-3.5 py-1.5 rounded-xl text-xs sm:text-sm font-medium transition-all whitespace-nowrap ${
              currentTab === 'adhkar'
                ? currentTheme.tabActiveClass
                : isDark
                ? 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/80'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
            }`}
          >
            <span>✨</span>
            <span>أذكار بعد الصلاة واليوم</span>
          </button>

          {/* 5. Tasbih */}
          <button
            onClick={() => setCurrentTab('tasbih')}
            className={`flex items-center gap-2 px-3.5 py-1.5 rounded-xl text-xs sm:text-sm font-medium transition-all whitespace-nowrap ${
              currentTab === 'tasbih'
                ? currentTheme.tabActiveClass
                : isDark
                ? 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/80'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
            }`}
          >
            <span>📿</span>
            <span>المسبحة الذكية</span>
          </button>

          {/* 6. Widgets */}
          <button
            onClick={() => setCurrentTab('widgets')}
            className={`flex items-center gap-2 px-3.5 py-1.5 rounded-xl text-xs sm:text-sm font-medium transition-all whitespace-nowrap ${
              currentTab === 'widgets'
                ? currentTheme.tabActiveClass
                : isDark
                ? 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/80'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
            }`}
          >
            <Smartphone className="w-3.5 h-3.5" />
            <span>ويدجتس أندرويد</span>
          </button>

          {/* 7. Kotlin Project */}
          <button
            onClick={() => setCurrentTab('kotlin')}
            className={`flex items-center gap-2 px-3.5 py-1.5 rounded-xl text-xs sm:text-sm font-medium transition-all whitespace-nowrap ${
              currentTab === 'kotlin'
                ? 'bg-amber-600 text-white shadow-md shadow-amber-900/40'
                : isDark
                ? 'text-amber-300 hover:bg-amber-950/40'
                : 'text-amber-700 hover:bg-amber-100'
            }`}
          >
            <Download className="w-3.5 h-3.5" />
            <span>كود أندرويد كوتلن (ZIP)</span>
          </button>
        </nav>
      </div>

      {/* GPS & Country/City Picker Modal */}
      <LocationPickerModal
        isOpen={showLocationModal}
        onClose={() => setShowLocationModal(false)}
        currentCoords={coordinates}
        onSelectCoords={(coords) => setCoordinates(coords)}
      />

      {/* Theme & Mode Selector Modal */}
      <ThemeSelectorModal
        isOpen={showThemeModal}
        onClose={() => setShowThemeModal(false)}
        currentTheme={currentTheme}
        onSelectTheme={(theme) => setCurrentTheme(theme)}
        colorMode={colorMode}
        onToggleColorMode={(mode) => setColorMode(mode)}
      />
    </header>
  );
};
