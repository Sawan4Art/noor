import React, { useState } from 'react';
import {
  Clock,
  BookOpen,
  SunMedium,
} from 'lucide-react';
import {
  WidgetConfig,
  WidgetType,
  WidgetTheme,
  DEFAULT_WIDGETS,
  WIDGET_THEMES,
} from '../utils/widgetsData';
import { Coordinates, computePrayerTimes, getNextPrayer, getHijriDate } from '../utils/prayerMath';

interface WidgetsStudioProps {
  coordinates: Coordinates;
  onNavigateToTab?: (tab: 'prayer' | 'quran' | 'adhkar' | 'tasbih') => void;
}

export const WidgetsStudioSection: React.FC<WidgetsStudioProps> = ({
  coordinates,
  onNavigateToTab,
}) => {
  const [widgets, setWidgets] = useState<WidgetConfig[]>(DEFAULT_WIDGETS);
  const [activeWidgetId, setActiveWidgetId] = useState<string>('w-next');
  const [wallpaper, setWallpaper] = useState<'mosque' | 'starry' | 'minimal' | 'gold'>('mosque');
  const [simTasbihCount, setSimTasbihCount] = useState<number>(14);

  const activeWidget = widgets.find((w) => w.id === activeWidgetId) || widgets[0];

  const now = new Date();
  const prayerTimes = computePrayerTimes(now, coordinates.latitude, coordinates.longitude);
  const nextPrayer = getNextPrayer(prayerTimes, now);
  const hijri = getHijriDate(now);

  // Update active widget property
  const updateWidgetConfig = (updates: Partial<WidgetConfig>) => {
    setWidgets((prev) =>
      prev.map((w) => (w.id === activeWidget.id ? { ...w, ...updates } : w))
    );
  };

  const currentTheme = WIDGET_THEMES[activeWidget.theme];

  // Wallpaper backgrounds for Android Home Screen simulation
  const WALLPAPERS = {
    mosque: 'bg-gradient-to-b from-emerald-950 via-slate-950 to-teal-950',
    starry: 'bg-gradient-to-b from-indigo-950 via-slate-950 to-blue-950',
    minimal: 'bg-gradient-to-b from-zinc-950 via-neutral-900 to-black',
    gold: 'bg-gradient-to-b from-amber-950 via-stone-950 to-zinc-950',
  };

;

  // Render Widget Preview by Type
  const renderWidgetPreview = (w: WidgetConfig) => {
    const themeStyle = WIDGET_THEMES[w.theme];

    return (
      <div
        className={`w-full overflow-hidden transition-all duration-300 border ${themeStyle.bgClass} ${themeStyle.borderClass} ${themeStyle.textClass} shadow-2xl relative`}
        style={{
          borderRadius: `${w.borderRadius}px`,
          opacity: w.opacity,
        }}
      >
        {/* Mosque Silhouette / Pattern */}
        {w.showMosquePattern && (
          <div className="absolute inset-0 opacity-10 bg-[radial-gradient(#10b981_1px,transparent_1px)] [background-size:16px_16px] pointer-events-none" />
        )}

        <div className="p-4 sm:p-5 relative z-10">
          {/* Top Bar inside widget */}
          {w.showHijriDate && (
            <div className="flex items-center justify-between text-xs mb-3 pb-2 border-b border-white/10 opacity-80">
              <span className="font-bold flex items-center gap-1.5">
                <span>🕌</span>
                <span>{w.arabicName}</span>
              </span>
              <span className="font-mono text-[11px]">{hijri.formatted}</span>
            </div>
          )}

          {/* Widget Specific Body */}
          {w.type === 'next_prayer' && (
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <div>
                  <span className="text-xs uppercase tracking-wider opacity-75">
                    الصلاة القادمة
                  </span>
                  <h4 className="text-2xl sm:text-3xl font-extrabold">
                    صلاة {nextPrayer.arabicName}
                  </h4>
                </div>
                <div className="text-left font-mono">
                  <span className="text-2xl sm:text-3xl font-bold">
                    {nextPrayer.timeStr}
                  </span>
                  <div className="text-[10px] opacity-75">{coordinates.city}</div>
                </div>
              </div>

              {w.showCountdown && (
                <div className="p-2 rounded-xl bg-black/20 border border-white/10 flex items-center justify-between text-xs">
                  <span className="flex items-center gap-1 opacity-90">
                    <Clock className="w-3.5 h-3.5 text-emerald-400" />
                    <span>متبقي على الأذان:</span>
                  </span>
                  <span className="font-mono font-bold text-sm tracking-wider">
                    {nextPrayer.formattedRemaining}
                  </span>
                </div>
              )}

              {w.showActions && (
                <div className="flex items-center gap-2 pt-1">
                  <button
                    onClick={() => onNavigateToTab?.('prayer')}
                    className="flex-1 py-1.5 rounded-lg bg-emerald-500/20 hover:bg-emerald-500/30 border border-emerald-500/40 text-[11px] font-bold text-center"
                  >
                    فتح المؤذن
                  </button>
                  <button
                    onClick={() => onNavigateToTab?.('adhkar')}
                    className="flex-1 py-1.5 rounded-lg bg-white/10 hover:bg-white/20 border border-white/20 text-[11px] font-bold text-center"
                  >
                    أذكار الصلاة
                  </button>
                </div>
              )}
            </div>
          )}

          {w.type === 'prayer_full' && (
            <div className="space-y-2.5">
              <div className="flex items-center justify-between text-xs">
                <span className="font-bold">مواقيت اليوم</span>
                <span className="text-[11px] opacity-75">{coordinates.city}</span>
              </div>
              <div className="grid grid-cols-5 gap-1.5 text-center">
                {[
                  { name: 'الفجر', time: prayerTimes.fajr },
                  { name: 'الظهر', time: prayerTimes.dhuhr },
                  { name: 'العصر', time: prayerTimes.asr },
                  { name: 'المغرب', time: prayerTimes.maghrib },
                  { name: 'العشاء', time: prayerTimes.isha },
                ].map((p) => {
                  const isCur = nextPrayer.arabicName === p.name;
                  return (
                    <div
                      key={p.name}
                      className={`p-2 rounded-xl border ${
                        isCur
                          ? 'bg-emerald-500/30 border-emerald-400 font-bold'
                          : 'bg-black/20 border-white/10'
                      }`}
                    >
                      <span className="text-[11px] block">{p.name}</span>
                      <span className="text-xs font-mono block mt-0.5">{p.time}</span>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {w.type === 'after_prayer_adhkar' && (
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold flex items-center gap-1">
                  <SunMedium className="w-3.5 h-3.5 text-amber-400" />
                  <span>ذكر ما بعد الصلاة</span>
                </span>
                <span className="text-[10px] px-1.5 py-0.5 rounded bg-emerald-500/20 border border-emerald-500/30">
                  سنة مؤكدة
                </span>
              </div>
              <p className="font-quran text-base leading-relaxed line-clamp-2">
                «أَسْتَغْفِرُ اللهَ، أَسْتَغْفِرُ اللهَ، اللَّهُمَّ أَنْتَ السَّلَامُ وَمِنْكَ السَّلَامُ تَبَارَكْتَ يَا ذَا الجَلَالِ وَالإِكْرَامِ»
              </p>
              {w.showActions && (
                <button
                  onClick={() => onNavigateToTab?.('adhkar')}
                  className="w-full mt-1 py-1.5 rounded-lg bg-emerald-500/20 hover:bg-emerald-500/30 border border-emerald-500/40 text-xs font-semibold text-center"
                >
                  متابعة باقي أذكار الصلاة (٣٣ تسبيحة) 📿
                </button>
              )}
            </div>
          )}

          {w.type === 'daily_ayah' && (
            <div className="space-y-2 text-right">
              <div className="flex items-center justify-between text-xs opacity-80">
                <span className="font-bold flex items-center gap-1">
                  <BookOpen className="w-3.5 h-3.5 text-amber-400" />
                  <span>قبس من القرآن الكريم</span>
                </span>
                <span>سورة البقرة: ٢٥٥</span>
              </div>
              <p className="font-quran text-base sm:text-lg leading-relaxed text-center font-bold">
                ﴿اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ﴾
              </p>
              <p className="text-[11px] opacity-75 text-center line-clamp-1">
                آية الكرسي أعظم آية في كتاب الله تحفظ قارئها
              </p>
            </div>
          )}

          {w.type === 'quick_tasbih' && (
            <div className="space-y-3 text-center">
              <div className="flex items-center justify-between text-xs opacity-80">
                <span className="font-bold">المسبحة السريعة</span>
                <span>الهدف: ٣٣</span>
              </div>
              <p className="font-quran text-base font-bold text-emerald-300">
                سُبْحَانَ اللهِ وَبِحَمْدِهِ
              </p>
              <button
                onClick={() => setSimTasbihCount((c) => (c >= 33 ? 1 : c + 1))}
                className="w-full py-3 rounded-2xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-mono font-extrabold text-xl shadow-lg active:scale-95 transition-all"
                aria-label="تسبيح سريع في الويدجت"
              >
                {simTasbihCount} / 33
              </button>
            </div>
          )}

          {w.type === 'hijri_minimal' && (
            <div className="flex items-center justify-between gap-3">
              <div>
                <div className="text-[11px] opacity-75">{hijri.monthName} {hijri.year} هـ</div>
                <div className="text-xl font-bold font-mono">{hijri.day}</div>
              </div>
              <div className="w-px h-8 bg-white/20" />
              <div className="text-left font-mono">
                <div className="text-[11px] opacity-75">أذان {nextPrayer.arabicName}</div>
                <div className="text-lg font-bold">{nextPrayer.timeStr}</div>
              </div>
            </div>
          )}
        </div>
      </div>
    );
  };

  return (
    <div className="space-y-6">
      {/* Studio Header */}
      <div className="p-5 rounded-3xl bg-slate-900/90 border border-slate-800 shadow-xl flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="text-xl">📱</span>
            <h2 className="text-xl font-bold text-white">
              استوديو ويدجتس الشاشة الرئيسية
            </h2>
          </div>
          <p className="text-xs text-slate-400 mt-1">
            خصص مظهر الويدجت وشفافيته وألوانه — المعاينة لحظية على شاشة هاتف أندرويد
          </p>
        </div>
      </div>

      {/* Main Studio Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left Column: Widget Selector & Customizer Controls */}
        <div className="lg:col-span-5 space-y-4">
          {/* Widget Selector */}
          <div className="p-4 rounded-3xl bg-slate-900/90 border border-slate-800 space-y-3">
            <label className="block text-xs font-bold text-slate-300">
              ١. اختر نوع الويدجت لتخصيصه:
            </label>
            <div className="space-y-1.5">
              {widgets.map((w) => {
                const isSelected = activeWidget.id === w.id;
                return (
                  <button
                    key={w.id}
                    onClick={() => setActiveWidgetId(w.id)}
                    className={`w-full flex items-center justify-between p-3 rounded-2xl border text-right transition-all ${
                      isSelected
                        ? 'bg-teal-950/80 border-teal-500 ring-1 ring-teal-400/50 text-white shadow-md'
                        : 'bg-slate-950/60 border-slate-800/80 text-slate-300 hover:border-slate-700'
                    }`}
                  >
                    <div>
                      <div className="text-xs font-bold flex items-center gap-1.5">
                        <span>{w.title}</span>
                      </div>
                      <div className="text-[10px] text-slate-400 mt-0.5">
                        {w.arabicName}
                      </div>
                    </div>
                    <span className="text-[10px] px-2 py-0.5 rounded-full bg-slate-800 text-slate-300 font-mono">
                      {w.sizeLabel}
                    </span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Theme & Style Customization */}
          <div className="p-4 rounded-3xl bg-slate-900/90 border border-slate-800 space-y-4">
            <label className="block text-xs font-bold text-slate-300">
              ٢. تخصيص نمط ومظهر الويدجت:
            </label>

            {/* Themes list */}
            <div>
              <span className="text-[11px] text-slate-400 block mb-1.5">السمة اللونية:</span>
              <div className="grid grid-cols-2 gap-2">
                {Object.entries(WIDGET_THEMES).map(([tKey, tVal]) => {
                  const isSel = activeWidget.theme === tKey;
                  return (
                    <button
                      key={tKey}
                      onClick={() => updateWidgetConfig({ theme: tKey as WidgetTheme })}
                      className={`p-2 rounded-xl border text-[11px] text-right font-medium transition-all ${
                        isSel
                          ? 'bg-teal-900/60 border-teal-500 text-white ring-1 ring-teal-400/40'
                          : 'bg-slate-950/60 border-slate-800 text-slate-300 hover:border-slate-700'
                      }`}
                    >
                      {tVal.name}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Opacity Slider */}
            <div>
              <div className="flex justify-between text-[11px] text-slate-400 mb-1">
                <span>الشفافية (Transparency):</span>
                <span className="font-mono text-teal-300 font-bold">
                  {Math.round(activeWidget.opacity * 100)}%
                </span>
              </div>
              <input
                type="range"
                min="0.3"
                max="1.0"
                step="0.05"
                value={activeWidget.opacity}
                onChange={(e) => updateWidgetConfig({ opacity: Number(e.target.value) })}
                className="w-full h-1.5 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-teal-500"
              />
            </div>

            {/* Border Radius Slider */}
            <div>
              <div className="flex justify-between text-[11px] text-slate-400 mb-1">
                <span>استدارة الحواف (Corner Radius):</span>
                <span className="font-mono text-teal-300 font-bold">
                  {activeWidget.borderRadius}px
                </span>
              </div>
              <input
                type="range"
                min="12"
                max="36"
                step="2"
                value={activeWidget.borderRadius}
                onChange={(e) => updateWidgetConfig({ borderRadius: Number(e.target.value) })}
                className="w-full h-1.5 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-teal-500"
              />
            </div>

            {/* Feature Toggles */}
            <div className="pt-2 border-t border-slate-800/80 space-y-2">
              <span className="text-[11px] text-slate-400 block mb-1">عناصر الويدجت:</span>
              
              <label className="flex items-center justify-between p-2 rounded-xl bg-slate-950/60 border border-slate-800/80 cursor-pointer text-xs">
                <span>إظهار التاريخ الهجري</span>
                <input
                  type="checkbox"
                  checked={activeWidget.showHijriDate}
                  onChange={(e) => updateWidgetConfig({ showHijriDate: e.target.checked })}
                  className="rounded accent-teal-500 w-4 h-4"
                />
              </label>

              <label className="flex items-center justify-between p-2 rounded-xl bg-slate-950/60 border border-slate-800/80 cursor-pointer text-xs">
                <span>إظهار العد التنازلي للأذان</span>
                <input
                  type="checkbox"
                  checked={activeWidget.showCountdown}
                  onChange={(e) => updateWidgetConfig({ showCountdown: e.target.checked })}
                  className="rounded accent-teal-500 w-4 h-4"
                />
              </label>

              <label className="flex items-center justify-between p-2 rounded-xl bg-slate-950/60 border border-slate-800/80 cursor-pointer text-xs">
                <span>إظهار أزرار الإجراءات السريعة</span>
                <input
                  type="checkbox"
                  checked={activeWidget.showActions}
                  onChange={(e) => updateWidgetConfig({ showActions: e.target.checked })}
                  className="rounded accent-teal-500 w-4 h-4"
                />
              </label>
            </div>
          </div>
        </div>

        {/* Right Column: Interactive Android Simulator — معاينة لحظية دائماً */}
        <div className="lg:col-span-7">
          {(
            <div className="p-5 rounded-3xl bg-slate-900/90 border border-slate-800 shadow-2xl flex flex-col items-center">
              {/* Wallpaper Switcher */}
              <div className="w-full flex items-center justify-between mb-4 pb-3 border-b border-slate-800 text-xs">
                <span className="text-slate-400">خلفية شاشة الأندرويد:</span>
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => setWallpaper('mosque')}
                    className={`px-2.5 py-1 rounded-lg border text-[11px] transition-colors ${
                      wallpaper === 'mosque'
                        ? 'bg-emerald-600 text-white border-emerald-500'
                        : 'bg-slate-950 text-slate-400 border-slate-800'
                    }`}
                  >
                    مسجد إسلامي
                  </button>
                  <button
                    onClick={() => setWallpaper('starry')}
                    className={`px-2.5 py-1 rounded-lg border text-[11px] transition-colors ${
                      wallpaper === 'starry'
                        ? 'bg-indigo-600 text-white border-indigo-500'
                        : 'bg-slate-950 text-slate-400 border-slate-800'
                    }`}
                  >
                    سماء ليلية
                  </button>
                  <button
                    onClick={() => setWallpaper('minimal')}
                    className={`px-2.5 py-1 rounded-lg border text-[11px] transition-colors ${
                      wallpaper === 'minimal'
                        ? 'bg-zinc-700 text-white border-zinc-600'
                        : 'bg-slate-950 text-slate-400 border-slate-800'
                    }`}
                  >
                    AMOLED داكن
                  </button>
                </div>
              </div>

              {/* Realistic Android Device Frame */}
              <div className="w-full max-w-sm rounded-[44px] p-3.5 bg-slate-950 border-[6px] border-slate-800 shadow-2xl ring-1 ring-slate-700/50">
                <div
                  className={`w-full min-h-[520px] rounded-[32px] p-4 flex flex-col justify-between ${WALLPAPERS[wallpaper]} transition-all duration-300 relative overflow-hidden`}
                >
                  {/* Android Status Bar */}
                  <div className="flex items-center justify-between text-xs text-white/90 pt-1 px-2 font-mono">
                    <span className="font-bold text-[11px]">12:45</span>
                    {/* Camera punch hole */}
                    <div className="w-3.5 h-3.5 rounded-full bg-black border border-white/20" />
                    <div className="flex items-center gap-1.5 text-[10px]">
                      <span>5G</span>
                      <span>📶</span>
                      <span>100% 🔋</span>
                    </div>
                  </div>

                  {/* Home Screen Middle Area: The Live Active Widget */}
                  <div className="my-auto py-4">
                    <div className="text-[10px] text-white/50 text-center mb-2 font-mono">
                      [ويدجت تفاعلي حي • يمكنك النقر داخله]
                    </div>
                    {renderWidgetPreview(activeWidget)}
                  </div>

                  {/* Android Home Screen App Icons Dock */}
                  <div className="pt-4 border-t border-white/10">
                    <div className="grid grid-cols-4 gap-2 text-center text-[10px] text-white/90">
                      <div
                        onClick={() => onNavigateToTab?.('prayer')}
                        className="flex flex-col items-center gap-1 cursor-pointer active:scale-90 transition-transform"
                      >
                        <div className="w-11 h-11 rounded-2xl bg-emerald-600/90 border border-emerald-400/40 flex items-center justify-center text-lg shadow-md">
                          📢
                        </div>
                        <span className="truncate w-full">المؤذن</span>
                      </div>

                      <div
                        onClick={() => onNavigateToTab?.('quran')}
                        className="flex flex-col items-center gap-1 cursor-pointer active:scale-90 transition-transform"
                      >
                        <div className="w-11 h-11 rounded-2xl bg-teal-600/90 border border-teal-400/40 flex items-center justify-center text-lg shadow-md">
                          📖
                        </div>
                        <span className="truncate w-full">المصحف</span>
                      </div>

                      <div
                        onClick={() => onNavigateToTab?.('adhkar')}
                        className="flex flex-col items-center gap-1 cursor-pointer active:scale-90 transition-transform"
                      >
                        <div className="w-11 h-11 rounded-2xl bg-indigo-600/90 border border-indigo-400/40 flex items-center justify-center text-lg shadow-md">
                          ✨
                        </div>
                        <span className="truncate w-full">الأذكار</span>
                      </div>

                      <div
                        onClick={() => onNavigateToTab?.('tasbih')}
                        className="flex flex-col items-center gap-1 cursor-pointer active:scale-90 transition-transform"
                      >
                        <div className="w-11 h-11 rounded-2xl bg-amber-600/90 border border-amber-400/40 flex items-center justify-center text-lg shadow-md">
                          📿
                        </div>
                        <span className="truncate w-full">المسبحة</span>
                      </div>
                    </div>

                    {/* Android Navigation bar pill */}
                    <div className="w-28 h-1 rounded-full bg-white/40 mx-auto mt-4" />
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
