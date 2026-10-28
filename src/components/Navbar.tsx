import React from 'react';
import { motion } from 'motion/react';
import {
  Compass,
  Moon,
  Sun,
  Clock,
  BookOpen,
  Sparkles,
  CircleDot,
  Settings,
} from 'lucide-react';
import { AppTheme, ColorMode } from '../utils/themeData';

export type ActiveTab =
  | 'prayer'
  | 'quran'
  | 'qibla'
  | 'adhkar'
  | 'tasbih'
  | 'widgets'
  | 'settings';

interface NavbarProps {
  currentTab: ActiveTab;
  setCurrentTab: (tab: ActiveTab) => void;
  hijriString: string;
  currentTheme: AppTheme;
  colorMode: ColorMode;
  setColorMode: (mode: ColorMode) => void;
}

/**
 * عناصر شريط التنقل السفلي — خمسة أقسام رئيسية + الإعدادات.
 * (الدفعة الثالثة 2026-10: الألوان والموقع انتقلوا للإعدادات —
 * الهيدر: الشعار والتاريخ ووضع الإضاءة فقط.)
 */
const NAV_ITEMS: Array<{ key: ActiveTab; label: string; Icon: React.FC<{ className?: string }> }> = [
  { key: 'prayer', label: 'المواقيت', Icon: Clock },
  { key: 'quran', label: 'المصحف', Icon: BookOpen },
  { key: 'qibla', label: 'القبلة', Icon: Compass },
  { key: 'adhkar', label: 'الأذكار', Icon: Sparkles },
  { key: 'tasbih', label: 'المسبحة', Icon: CircleDot },
  { key: 'settings', label: 'الإعدادات', Icon: Settings },
];

export const Navbar: React.FC<NavbarProps> = ({
  currentTab,
  setCurrentTab,
  hijriString,
  currentTheme,
  colorMode,
  setColorMode,
}) => {
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
                <p
                  className="text-[10px] truncate hidden sm:block"
                  style={{ color: 'var(--noor-n-500)' }}
                >
                  {hijriString}
                </p>
              </div>
            </div>

            {/* أدوات التحكم: وضع الإضاءة (أيقونة) — الألوان والموقع من الإعدادات */}
            <div className="flex items-center gap-1 sm:gap-1.5">
              <motion.button
                whileTap={{ scale: 0.88, rotate: 15 }}
                onClick={() => setColorMode(isDark ? 'light' : 'dark')}
                className="p-2 rounded-xl border transition-colors"
                style={{
                  backgroundColor: 'color-mix(in srgb, var(--noor-n-800) 55%, transparent)',
                  borderColor: 'color-mix(in srgb, var(--noor-p-500) 25%, transparent)',
                  color: isDark ? 'var(--noor-a-400)' : 'var(--noor-n-300)',
                }}
                title={isDark ? 'التبديل إلى الوضع الفاتح' : 'التبديل إلى الوضع المظلم'}
                aria-label="تبديل وضع الإضاءة"
              >
                {isDark ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
              </motion.button>
            </div>
          </div>
        </div>
      </header>

      {/* ================= شريط التنقل السفلي العصري ================= */}
      <motion.nav
        initial={{ y: 90, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        transition={{ type: 'spring', stiffness: 260, damping: 26, delay: 0.15 }}
        aria-label="أقسام التطبيق"
        className="fixed bottom-0 inset-x-0 z-50"
      >
        <div className="mx-auto max-w-2xl sm:max-w-3xl px-3 pb-[max(0.5rem,env(safe-area-inset-bottom))] pt-2">
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
                  onClick={() => setCurrentTab(key)}
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
                    animate={active ? { scale: 1.18, y: -1.5 } : { scale: 1, y: 0 }}
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
          </div>
        </div>
      </motion.nav>
    </>
  );
};
