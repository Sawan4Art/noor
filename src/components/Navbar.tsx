import React, { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import {
  Compass,
  Moon,
  Sun,
  Volume2,
  VolumeX,
  MapPin,
  Clock,
  BookOpen,
  Sparkles,
  CircleDot,
  LayoutGrid,
  Smartphone,
  HeartHandshake,
  X,
} from 'lucide-react';
import { Coordinates } from '../utils/prayerMath';
import { LocationPickerModal } from './LocationPickerModal';
import { ThemeSelectorModal } from './ThemeSelectorModal';
import { AppTheme, ColorMode } from '../utils/themeData';
import { APP_VERSION } from '../utils/otaUpdate';

export type ActiveTab =
  | 'prayer'
  | 'quran'
  | 'qibla'
  | 'adhkar'
  | 'tasbih'
  | 'widgets';

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

/** عناصر شريط التنقل السفلي — خمسة أقسام رئيسية + زر «المزيد» */
const NAV_ITEMS: Array<{ key: ActiveTab; label: string; Icon: React.FC<{ className?: string }> }> = [
  { key: 'prayer', label: 'المواقيت', Icon: Clock },
  { key: 'quran', label: 'المصحف', Icon: BookOpen },
  { key: 'qibla', label: 'القبلة', Icon: Compass },
  { key: 'adhkar', label: 'الأذكار', Icon: Sparkles },
  { key: 'tasbih', label: 'المسبحة', Icon: CircleDot },
];

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
  const [showMoreSheet, setShowMoreSheet] = useState<boolean>(false);

  const isDark = colorMode === 'dark';

  return (
    <>
      {/* ================= الهيدر العلوي المختصر ================= */}
      <header
        className="sticky top-0 z-40 backdrop-blur-md border-b transition-colors"
        style={{
          backgroundColor: isDark
            ? 'color-mix(in srgb, var(--noor-n-950) 85%, transparent)'
            : 'color-mix(in srgb, #ffffff 95%, transparent)',
          borderBottomColor: isDark
            ? 'color-mix(in srgb, var(--noor-p-500) 22%, transparent)'
            : 'color-mix(in srgb, var(--noor-p-500) 16%, transparent)',
        }}
      >
        <div className="max-w-7xl mx-auto px-3 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-14">
            {/* الشعار والاسم */}
            <div className="flex items-center gap-2.5 min-w-0">
              <motion.div
                initial={{ rotate: -8, scale: 0.9, opacity: 0 }}
                animate={{ rotate: 0, scale: 1, opacity: 1 }}
                transition={{ type: 'spring', stiffness: 260, damping: 18 }}
                className="w-9 h-9 rounded-xl flex items-center justify-center shadow-lg ring-1 ring-white/20 shrink-0"
                style={{
                  background: `linear-gradient(135deg, ${currentTheme.primaryColor}, ${currentTheme.accentColor})`,
                }}
              >
                <span className="text-lg">🕌</span>
              </motion.div>
              <div className="min-w-0">
                <h1
                  className="text-base font-bold bg-clip-text text-transparent leading-tight"
                  style={{
                    backgroundImage: `linear-gradient(to right, ${currentTheme.primaryColor}, ${currentTheme.accentColor})`,
                  }}
                >
                  نور المسلم
                </h1>
                <p className="text-[10px] truncate hidden sm:block" style={{ color: 'var(--noor-n-500)' }}>
                  {hijriString}
                </p>
              </div>
            </div>

            {/* أدوات التحكم: الموقع، الثيم، الإضاءة، الصوت */}
            <div className="flex items-center gap-1 sm:gap-1.5">
              <button
                onClick={() => setShowLocationModal(true)}
                className={`flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl border text-xs transition-colors shadow-sm ${
                  isDark ? 'hover:brightness-125' : 'hover:brightness-105'
                }`}
                style={{
                  backgroundColor: 'color-mix(in srgb, var(--noor-n-800) 55%, transparent)',
                  borderColor: 'color-mix(in srgb, var(--noor-p-500) 25%, transparent)',
                  color: 'var(--noor-n-100)',
                }}
                title="تحديد مواقيت الصلاة عبر GPS أو باختيار البلد ثم المدينة"
              >
                <MapPin className="w-3.5 h-3.5" style={{ color: currentTheme.primaryColor }} />
                <span className="max-w-[90px] sm:max-w-[140px] truncate">{coordinates.city}</span>
              </button>

              <button
                onClick={() => setShowThemeModal(true)}
                className={`p-2 rounded-xl border transition-all shadow-sm ${isDark ? 'hover:brightness-125' : 'hover:brightness-105'}`}
                style={{
                  backgroundColor: 'color-mix(in srgb, var(--noor-n-800) 55%, transparent)',
                  borderColor: 'color-mix(in srgb, var(--noor-p-500) 25%, transparent)',
                }}
                title={`الثيم الحالي: ${currentTheme.name} — اضغط لتغييره`}
                aria-label="اختيار النمط اللوني للتطبيق"
              >
                <div
                  className="w-3.5 h-3.5 rounded-full border border-white/50 shadow-inner"
                  style={{
                    background: `linear-gradient(135deg, ${currentTheme.primaryColor}, ${currentTheme.accentColor})`,
                  }}
                />
              </button>

              <motion.button
                whileTap={{ scale: 0.88, rotate: 15 }}
                onClick={() => setColorMode(isDark ? 'light' : 'dark')}
                className="p-2 rounded-xl border transition-colors"
                style={{
                  backgroundColor: 'color-mix(in srgb, var(--noor-n-800) 55%, transparent)',
                  borderColor: 'color-mix(in srgb, var(--noor-p-500) 25%, transparent)',
                  color: isDark ? 'var(--noor-a-400)' : 'var(--noor-n-300)',
                }}
                title={isDark ? 'التبديل إلى الوضع النهاري (الفاتح)' : 'التبديل إلى الوضع الليلي (الداكن)'}
                aria-label="تبديل وضع الإضاءة"
              >
                {isDark ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
              </motion.button>

              <motion.button
                whileTap={{ scale: 0.88 }}
                onClick={() => setSoundEnabled(!soundEnabled)}
                className="p-2 rounded-xl border transition-colors"
                style={{
                  backgroundColor: soundEnabled
                    ? 'color-mix(in srgb, var(--noor-p-500) 20%, transparent)'
                    : 'color-mix(in srgb, var(--noor-n-800) 55%, transparent)',
                  borderColor: soundEnabled
                    ? 'color-mix(in srgb, var(--noor-p-500) 40%, transparent)'
                    : 'color-mix(in srgb, var(--noor-p-500) 25%, transparent)',
                  color: soundEnabled ? 'var(--noor-p-400)' : 'var(--noor-n-400)',
                }}
                title={soundEnabled ? 'صوت الأذان والتنبيهات مفعّل' : 'صامت'}
                aria-label="تبديل صوت التنبيهات"
              >
                {soundEnabled ? <Volume2 className="w-4 h-4" /> : <VolumeX className="w-4 h-4" />}
              </motion.button>
            </div>
          </div>
        </div>

        <LocationPickerModal
          isOpen={showLocationModal}
          onClose={() => setShowLocationModal(false)}
          currentCoords={coordinates}
          onSelectCoords={(coords) => setCoordinates(coords)}
        />

        <ThemeSelectorModal
          isOpen={showThemeModal}
          onClose={() => setShowThemeModal(false)}
          currentTheme={currentTheme}
          onSelectTheme={(theme) => setCurrentTheme(theme)}
          colorMode={colorMode}
          onToggleColorMode={(mode) => setColorMode(mode)}
        />
      </header>

      {/* ================= شريط التنقل السفلي العصري ================= */}
      <motion.nav
        initial={{ y: 90, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        transition={{ type: 'spring', stiffness: 260, damping: 26, delay: 0.15 }}
        aria-label="أقسام التطبيق"
        className="fixed bottom-0 inset-x-0 z-50"
      >
        <div
          className="mx-auto max-w-2xl sm:max-w-3xl px-3 pb-[max(0.5rem,env(safe-area-inset-bottom))] pt-2"
        >
          <div
            className="relative flex items-stretch justify-between rounded-3xl border shadow-2xl backdrop-blur-xl overflow-hidden"
            style={{
              backgroundColor: 'color-mix(in srgb, var(--noor-n-950) 78%, transparent)',
              borderColor: 'color-mix(in srgb, var(--noor-p-500) 28%, transparent)',
              boxShadow: `0 -6px 40px ${currentTheme.glowColor}, 0 10px 30px rgba(0,0,0,0.45)`,
            }}
          >
            {NAV_ITEMS.map(({ key, label, Icon }) => {
              const active = currentTab === key;
              return (
                <button
                  key={key}
                  onClick={() => {
                    setCurrentTab(key);
                    setShowMoreSheet(false);
                  }}
                  className="relative flex-1 flex flex-col items-center gap-0.5 py-2.5 px-1 group"
                  title={label}
                  aria-current={active ? 'page' : undefined}
                >
                  {/* الكبسولة المنزلقة خلف العنصر النشط */}
                  {active && (
                    <motion.span
                      layoutId="noor-nav-pill"
                      transition={{ type: 'spring', stiffness: 420, damping: 34 }}
                      className="absolute inset-x-1.5 inset-y-1 rounded-2xl"
                      style={{
                        background: `linear-gradient(135deg, color-mix(in srgb, ${currentTheme.primaryColor} 32%, transparent), color-mix(in srgb, ${currentTheme.accentColor} 22%, transparent))`,
                        border: `1px solid color-mix(in srgb, ${currentTheme.accentColor} 45%, transparent)`,
                      }}
                    />
                  )}

                  <motion.span
                    animate={
                      active
                        ? { scale: 1.18, y: -1.5 }
                        : { scale: 1, y: 0 }
                    }
                    transition={{ type: 'spring', stiffness: 420, damping: 20 }}
                    className="relative z-10"
                    style={{ color: active ? currentTheme.accentColor : 'var(--noor-n-400)' }}
                  >
                    <Icon className="w-5 h-5 sm:w-[22px] sm:h-[22px]" />
                    {active && (
                      <motion.span
                        layoutId="noor-nav-dot"
                        className="absolute -bottom-1 left-1/2 -translate-x-1/2 w-1 h-1 rounded-full"
                        style={{ backgroundColor: currentTheme.accentColor }}
                      />
                    )}
                  </motion.span>

                  <span
                    className="relative z-10 text-[10px] sm:text-[11px] font-bold transition-colors"
                    style={{ color: active ? 'var(--noor-n-50)' : 'var(--noor-n-400)' }}
                  >
                    {label}
                  </span>
                </button>
              );
            })}

            {/* زر المزيد: الويدجتس + عن التطبيق */}
            <button
              onClick={() => setShowMoreSheet((s) => !s)}
              className="relative flex-1 flex flex-col items-center gap-0.5 py-2.5 px-1 group"
              title="المزيد — الويدجتس وعن التطبيق"
            >
              <motion.span
                animate={
                  showMoreSheet
                    ? { scale: 1.14, rotate: 90 }
                    : { scale: 1, rotate: 0 }
                }
                transition={{ type: 'spring', stiffness: 420, damping: 20 }}
                className="relative z-10"
                style={{
                  color: showMoreSheet || currentTab === 'widgets' ? currentTheme.accentColor : 'var(--noor-n-400)',
                }}
              >
                <LayoutGrid className="w-5 h-5 sm:w-[22px] sm:h-[22px]" />
              </motion.span>
              <span
                className="relative z-10 text-[10px] sm:text-[11px] font-bold transition-colors"
                style={{
                  color:
                    showMoreSheet || currentTab === 'widgets'
                      ? 'var(--noor-n-50)'
                      : 'var(--noor-n-400)',
                }}
              >
                المزيد
              </span>
            </button>
          </div>
        </div>
      </motion.nav>

      {/* ================= قائمة «المزيد» المنبثقة فوق الشريط ================= */}
      <AnimatePresence>
        {showMoreSheet && (
          <>
            <motion.div
              key="more-backdrop"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setShowMoreSheet(false)}
              className="fixed inset-0 z-40 bg-black/45 backdrop-blur-[2px]"
            />
            <motion.div
              key="more-sheet"
              initial={{ y: 60, opacity: 0, scale: 0.97 }}
              animate={{ y: 0, opacity: 1, scale: 1 }}
              exit={{ y: 60, opacity: 0, scale: 0.97 }}
              transition={{ type: 'spring', stiffness: 340, damping: 30 }}
              className="fixed bottom-[4.6rem] inset-x-3 sm:inset-x-auto sm:right-4 sm:left-auto sm:w-96 z-50 rounded-3xl border shadow-2xl overflow-hidden backdrop-blur-2xl"
              style={{
                backgroundColor: 'color-mix(in srgb, var(--noor-n-900) 92%, transparent)',
                borderColor: 'color-mix(in srgb, var(--noor-p-500) 30%, transparent)',
              }}
            >
              <div className="flex items-center justify-between px-5 pt-4 pb-2">
                <h3 className="text-sm font-extrabold" style={{ color: 'var(--noor-n-50)' }}>
                  المزيد من نور المسلم
                </h3>
                <button
                  onClick={() => setShowMoreSheet(false)}
                  className="p-1.5 rounded-lg transition-colors hover:bg-white/10"
                  style={{ color: 'var(--noor-n-400)' }}
                  aria-label="إغلاق"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>

              <div className="px-3 pb-3 space-y-1.5">
                <button
                  onClick={() => {
                    setCurrentTab('widgets');
                    setShowMoreSheet(false);
                  }}
                  className="w-full flex items-center gap-3 px-3 py-3 rounded-2xl transition-colors hover:bg-white/[0.06] text-right"
                >
                  <span
                    className="w-9 h-9 rounded-xl flex items-center justify-center shrink-0"
                    style={{
                      background: `linear-gradient(135deg, ${currentTheme.primaryColor}, ${currentTheme.accentColor})`,
                    }}
                  >
                    <Smartphone className="w-5 h-5 text-white" />
                  </span>
                  <span className="flex-1">
                    <span className="block text-xs font-bold" style={{ color: 'var(--noor-n-100)' }}>
                      استوديو ويدجتس أندرويد
                    </span>
                    <span className="block text-[10px] mt-0.5" style={{ color: 'var(--noor-n-400)' }}>
                      صمّم ويدجت المواقيت والأذكار لشاشة هاتفك
                    </span>
                  </span>
                </button>

                {/* عن التطبيق: المطور + الإصدار + الصدقة الجارية */}
                <div
                  className="mx-1 rounded-2xl p-3.5 border"
                  style={{
                    backgroundColor: `color-mix(in srgb, ${currentTheme.primaryColor} 10%, transparent)`,
                    borderColor: `color-mix(in srgb, ${currentTheme.primaryColor} 30%, transparent)`,
                  }}
                >
                  <div className="flex items-center gap-2 text-[11px] font-bold" style={{ color: currentTheme.accentColor }}>
                    <HeartHandshake className="w-4 h-4" />
                    <span>هذا التطبيق صدقة جارية على روح المرحوم بأذن الله أبي</span>
                  </div>
                  <div
                    className="mt-2 pt-2 border-t text-[10px] flex items-center justify-between"
                    style={{
                      borderColor: 'color-mix(in srgb, var(--noor-n-500) 35%, transparent)',
                      color: 'var(--noor-n-400)',
                    }}
                  >
                    <span>تطوير: Yousef Sawan</span>
                    <span>الإصدار {APP_VERSION}</span>
                  </div>
                </div>
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </>
  );
};
