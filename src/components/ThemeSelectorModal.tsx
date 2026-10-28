import React from 'react';
import { X, Check, Palette, Moon, Sun, Sparkles } from 'lucide-react';
import { APP_THEMES, AppTheme, ColorMode } from '../utils/themeData';

interface ThemeSelectorModalProps {
  isOpen: boolean;
  onClose: () => void;
  currentTheme: AppTheme;
  onSelectTheme: (theme: AppTheme) => void;
  colorMode: ColorMode;
  onToggleColorMode: (mode: ColorMode) => void;
}

export const ThemeSelectorModal: React.FC<ThemeSelectorModalProps> = ({
  isOpen,
  onClose,
  currentTheme,
  onSelectTheme,
  colorMode,
  onToggleColorMode,
}) => {
  if (!isOpen) return null;

  const isDark = colorMode === 'dark';

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-in fade-in duration-200">
      <div
        className={`w-full max-w-2xl rounded-3xl border shadow-2xl overflow-hidden flex flex-col max-h-[90vh] transition-colors ${
          isDark
            ? 'bg-slate-900 border-emerald-800/40 text-slate-100'
            : 'bg-white border-slate-200 text-slate-900'
        }`}
      >
        {/* Header */}
        <div
          className={`flex items-center justify-between p-6 border-b ${
            isDark ? 'border-slate-800 bg-slate-950/60' : 'border-slate-100 bg-slate-50'
          }`}
        >
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-gradient-to-br from-emerald-500 to-teal-600 flex items-center justify-center text-white shadow-md">
              <Palette className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-lg font-bold">ثيمات ومظهر التطبيق</h2>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                اختر النمط اللوني المناسب لك وللقراءة المريحة (نهاري وليلي)
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-2 rounded-xl text-slate-400 hover:text-slate-200 hover:bg-slate-800/50 transition-colors"
            aria-label="إغلاق"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 overflow-y-auto space-y-6">
          {/* Day / Night Mode Toggle */}
          <div>
            <label className="text-xs font-bold text-slate-400 dark:text-slate-400 uppercase tracking-wider block mb-3">
              نمط الإضاءة (الوضع الليلي والنهاري)
            </label>
            <div className="grid grid-cols-2 gap-3">
              <button
                onClick={() => onToggleColorMode('dark')}
                className={`flex items-center justify-center gap-3 p-3.5 rounded-2xl border transition-all ${
                  isDark
                    ? 'bg-slate-800/90 border-emerald-500 text-white ring-2 ring-emerald-500/30'
                    : 'bg-slate-100 hover:bg-slate-200/70 border-transparent text-slate-700'
                }`}
              >
                <div className="w-8 h-8 rounded-xl bg-slate-950 flex items-center justify-center text-amber-300">
                  <Moon className="w-4 h-4" />
                </div>
                <div className="text-right">
                  <div className="text-sm font-bold">الوضع الليلي (الداكن)</div>
                  <div className="text-[11px] text-slate-400">مريح للعينين في الإضاءة الخافتة</div>
                </div>
                {isDark && <Check className="w-4 h-4 text-emerald-400 mr-auto" />}
              </button>

              <button
                onClick={() => onToggleColorMode('light')}
                className={`flex items-center justify-center gap-3 p-3.5 rounded-2xl border transition-all ${
                  !isDark
                    ? 'bg-emerald-50/80 border-emerald-500 text-slate-900 ring-2 ring-emerald-500/30'
                    : 'bg-slate-800/40 hover:bg-slate-800 border-transparent text-slate-300'
                }`}
              >
                <div className="w-8 h-8 rounded-xl bg-amber-100 flex items-center justify-center text-amber-600">
                  <Sun className="w-4 h-4" />
                </div>
                <div className="text-right">
                  <div className="text-sm font-bold">الوضع النهاري (الفاتح)</div>
                  <div className="text-[11px] text-slate-500">وضوح عالي تحت أشعة الشمس</div>
                </div>
                {!isDark && <Check className="w-4 h-4 text-emerald-600 mr-auto" />}
              </button>
            </div>
          </div>

          {/* Theme Palettes Selection */}
          <div>
            <label className="text-xs font-bold text-slate-400 dark:text-slate-400 uppercase tracking-wider block mb-3">
              الأنماط اللونية الإسلامية الفاخرة
            </label>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
              {APP_THEMES.map((t) => {
                const isSelected = currentTheme.id === t.id;
                return (
                  <button
                    key={t.id}
                    onClick={() => onSelectTheme(t)}
                    className={`flex items-center gap-3.5 p-4 rounded-2xl border text-right transition-all group ${
                      isSelected
                        ? isDark
                          ? 'bg-slate-800/90 border-emerald-500 ring-2 ring-emerald-500/30'
                          : 'bg-emerald-50/70 border-emerald-500 ring-2 ring-emerald-500/30'
                        : isDark
                        ? 'bg-slate-950/60 border-slate-800 hover:border-slate-700 hover:bg-slate-800/40'
                        : 'bg-slate-50/80 border-slate-200 hover:border-slate-300 hover:bg-slate-100/60'
                    }`}
                  >
                    {/* Color Swatch Circle */}
                    <div
                      className="w-12 h-12 rounded-2xl flex items-center justify-center shadow-md flex-shrink-0 relative overflow-hidden"
                      style={{
                        background: `linear-gradient(135deg, ${t.primaryColor}, ${t.accentColor})`,
                      }}
                    >
                      <Sparkles className="w-5 h-5 text-white/90 drop-shadow" />
                    </div>

                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between">
                        <span className="font-bold text-sm">{t.name}</span>
                        {isSelected && <Check className="w-4 h-4 text-emerald-500" />}
                      </div>
                      <p className="text-[11px] text-slate-500 dark:text-slate-400 truncate mt-0.5">
                        {t.description}
                      </p>
                    </div>
                  </button>
                );
              })}
            </div>
          </div>
        </div>

        {/* Footer */}
        <div
          className={`p-4 border-t flex items-center justify-between ${
            isDark ? 'border-slate-800 bg-slate-950/60' : 'border-slate-100 bg-slate-50'
          }`}
        >
          <span className="text-xs text-slate-400">
            يتم حفظ التفضيلات تلقائياً في جهازك
          </span>
          <button
            onClick={onClose}
            className="px-5 py-2 rounded-xl text-xs font-bold bg-emerald-600 hover:bg-emerald-500 text-white transition-colors"
          >
            تطبيق وإغلاق
          </button>
        </div>
      </div>
    </div>
  );
};
