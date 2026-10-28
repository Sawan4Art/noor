import React, { useState } from 'react';
import {
  Sparkles,
  RotateCcw,
  Check,
  CheckCircle2,
  Info,
  SunMedium,
  Sunrise,
  Sunset,
  Moon,
  Volume2,
} from 'lucide-react';
import {
  DhikrItem,
  ADHKAR_LIST,
  ADHKAR_CATEGORIES,
} from '../utils/adhkarData';

export const AdhkarSection: React.FC = () => {
  const [selectedCategory, setSelectedCategory] = useState<
    'after_prayer' | 'morning' | 'evening' | 'sleep' | 'wake_up'
  >('after_prayer');

  // Track counts per dhikr id
  const [counts, setCounts] = useState<Record<string, number>>({});

  // Increment counter for a specific dhikr
  const handleCountClick = (item: DhikrItem) => {
    const current = counts[item.id] || 0;
    if (current < item.count) {
      const nextVal = current + 1;
      setCounts((prev) => ({ ...prev, [item.id]: nextVal }));

      // Haptic feedback for touch devices
      if ('vibrate' in navigator) {
        if (nextVal === item.count) {
          navigator.vibrate([40, 60, 40]); // celebratory double buzz
        } else {
          navigator.vibrate(20);
        }
      }
    }
  };

  // Reset category
  const resetCategoryCounts = () => {
    const categoryItemIds = ADHKAR_LIST.filter((i) => i.category === selectedCategory).map((i) => i.id);
    setCounts((prev) => {
      const copy = { ...prev };
      categoryItemIds.forEach((id) => delete copy[id]);
      return copy;
    });
  };

  const currentList = ADHKAR_LIST.filter((i) => i.category === selectedCategory);
  const totalInCat = currentList.length;
  const completedInCat = currentList.filter(
    (i) => (counts[i.id] || 0) >= i.count
  ).length;
  const categoryProgress = Math.round((completedInCat / (totalInCat || 1)) * 100);

  const getCategoryIcon = (catId: string) => {
    switch (catId) {
      case 'after_prayer':
        return <SunMedium className="w-4 h-4 text-emerald-400" />;
      case 'morning':
        return <Sunrise className="w-4 h-4 text-amber-400" />;
      case 'evening':
        return <Sunset className="w-4 h-4 text-orange-400" />;
      case 'sleep':
        return <Moon className="w-4 h-4 text-indigo-400" />;
      case 'wake_up':
        return <Sparkles className="w-4 h-4 text-teal-400" />;
      default:
        return <Sparkles className="w-4 h-4 text-emerald-400" />;
    }
  };

  return (
    <div className="space-y-6">
      {/* Category Selection Tabs */}
      <div className="p-4 rounded-3xl bg-slate-900/90 border border-slate-800 shadow-xl">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-4">
          <div>
            <h2 className="text-xl font-bold text-white flex items-center gap-2">
              <Sparkles className="w-5 h-5 text-emerald-400" />
              <span>الأذكار والأدعية وحصن المسلم</span>
            </h2>
            <p className="text-xs text-slate-400 mt-0.5">
              أذكار ما بعد الصلاة المأثورة وأذكار الصباح والمساء بعدادات تفاعلية
            </p>
          </div>

          {/* Reset button */}
          <button
            onClick={resetCategoryCounts}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-950 border border-slate-800 text-xs text-slate-300 hover:text-white hover:border-slate-700 transition-colors self-start sm:self-auto"
            title="إعادة ضبط عدادات هذا القسم"
          >
            <RotateCcw className="w-3.5 h-3.5" />
            <span>إعادة ضبط العدادات</span>
          </button>
        </div>

        {/* Categories Pills */}
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-2">
          {ADHKAR_CATEGORIES.map((cat) => {
            const isSelected = selectedCategory === cat.id;
            return (
              <button
                key={cat.id}
                onClick={() => setSelectedCategory(cat.id)}
                className={`flex flex-col items-start p-3 rounded-2xl border transition-all text-right ${
                  isSelected
                    ? 'bg-gradient-to-br from-emerald-950 to-teal-950 border-emerald-500 shadow-md ring-1 ring-emerald-400/40'
                    : 'bg-slate-950/60 border-slate-800/80 hover:bg-slate-950 hover:border-slate-700'
                }`}
              >
                <div className="flex items-center gap-2 mb-1">
                  {getCategoryIcon(cat.id)}
                  <span
                    className={`text-xs font-bold ${
                      isSelected ? 'text-white' : 'text-slate-300'
                    }`}
                  >
                    {cat.name}
                  </span>
                </div>
                <span className="text-[10px] text-slate-400 line-clamp-1">{cat.desc}</span>
              </button>
            );
          })}
        </div>

        {/* Progress Tracker for Active Category */}
        <div className="mt-4 pt-4 border-t border-slate-800/80 flex items-center justify-between text-xs text-slate-300">
          <div className="flex items-center gap-2">
            <span>المنجز:</span>
            <span className="font-bold text-emerald-400 font-mono">
              {completedInCat} من {totalInCat} ذكر
            </span>
          </div>
          <div className="flex-1 max-w-xs mx-4 h-2 rounded-full bg-slate-950 border border-slate-800 overflow-hidden">
            <div
              className="h-full bg-gradient-to-r from-emerald-500 to-teal-400 transition-all duration-500"
              style={{ width: `${categoryProgress}%` }}
            />
          </div>
          <span className="font-mono text-emerald-300 font-semibold">%{categoryProgress}</span>
        </div>
      </div>

      {/* List of Dhikr Cards */}
      <div className="space-y-4">
        {currentList.map((item, index) => {
          const currentCount = counts[item.id] || 0;
          const isDone = currentCount >= item.count;
          const remaining = item.count - currentCount;

          return (
            <div
              key={item.id}
              className={`p-5 rounded-3xl border transition-all ${
                isDone
                  ? 'bg-emerald-950/30 border-emerald-600/40 shadow-inner shadow-emerald-950/30'
                  : 'bg-slate-900/90 border-slate-800 hover:border-slate-700'
              }`}
            >
              <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
                <div className="flex-1 text-right">
                  <div className="flex items-center gap-2 mb-2">
                    <span className="w-6 h-6 rounded-lg bg-slate-800 flex items-center justify-center text-xs font-bold text-slate-300">
                      {index + 1}
                    </span>
                    {isDone && (
                      <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 text-[10px] font-semibold border border-emerald-500/30">
                        <Check className="w-3 h-3" />
                        <span>اكتمل الذكر</span>
                      </span>
                    )}
                  </div>

                  {/* Dhikr Arabic Text */}
                  <p className="font-quran text-lg sm:text-xl text-white leading-loose font-medium">
                    {item.text}
                  </p>

                  {/* Virtue & Reference */}
                  {(item.virtue || item.reference) && (
                    <div className="mt-3 pt-3 border-t border-slate-800/60 text-xs text-slate-400 space-y-1">
                      {item.virtue && (
                        <p className="flex items-start gap-1.5 text-emerald-300/90">
                          <span className="font-semibold text-emerald-400 shrink-0">الفضل:</span>
                          <span>{item.virtue}</span>
                        </p>
                      )}
                      {item.reference && (
                        <p className="flex items-center gap-1.5 text-[11px] text-slate-500">
                          <span>المصدر:</span>
                          <span className="font-mono">{item.reference}</span>
                        </p>
                      )}
                    </div>
                  )}
                </div>

                {/* Big Interactive Counter Button */}
                <div className="sm:self-center shrink-0">
                  <button
                    onClick={() => handleCountClick(item)}
                    disabled={isDone}
                    className={`w-full sm:w-28 h-16 sm:h-24 rounded-2xl flex sm:flex-col items-center justify-center gap-1 sm:gap-0 font-bold transition-all active:scale-95 shadow-lg ${
                      isDone
                        ? 'bg-emerald-600 text-white cursor-default'
                        : 'bg-gradient-to-br from-emerald-500 to-teal-600 hover:from-emerald-400 hover:to-teal-500 text-slate-950'
                    }`}
                    aria-label={`تكرار الذكر ${item.text}`}
                  >
                    {isDone ? (
                      <>
                        <CheckCircle2 className="w-7 h-7 sm:mb-1 text-white" />
                        <span className="text-xs">تم بالكامل</span>
                      </>
                    ) : (
                      <>
                        <span className="text-2xl sm:text-3xl font-mono">
                          {remaining}
                        </span>
                        <span className="text-[10px] opacity-80">
                          {currentCount} / {item.count}
                        </span>
                      </>
                    )}
                  </button>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
