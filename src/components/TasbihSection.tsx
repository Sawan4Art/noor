import React, { useState } from 'react';
import {
  RotateCcw,
  Sparkles,
  Volume2,
  VolumeX,
  Plus,
  Flame,
  Award,
  ChevronRight,
} from 'lucide-react';

interface TasbihPreset {
  id: string;
  arabic: string;
  defaultTarget: number;
}

/** الإعدادات المسبقة — التصحيح السني: كل من سبحان الله والحمد لله والله أكبر 33 مرة
 *  (كان الله أكبر 34 بالخطأ — حديث مسلم: «وَكَبِّرْ ثَلاَثًا وَثَلاَثِينَ») */
export const TASBIH_PRESETS: TasbihPreset[] = [
  { id: '1', arabic: 'سُبْحَانَ اللهِ', defaultTarget: 33 },
  { id: '2', arabic: 'الحَمْدُ للهِ', defaultTarget: 33 },
  { id: '3', arabic: 'اللهُ أَكْبَرُ', defaultTarget: 33 },
  { id: '4', arabic: 'لَا إِلَهَ إِلَّا اللهُ', defaultTarget: 100 },
  { id: '5', arabic: 'أَسْتَغْفِرُ اللهَ وَأَتُوبُ إِلَيْهِ', defaultTarget: 100 },
  { id: '6', arabic: 'اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ', defaultTarget: 100 },
  { id: '7', arabic: 'سُبْحَانَ اللهِ وَبِحَمْدِهِ سُبْحَانَ اللهِ العَظِيمِ', defaultTarget: 100 },
  { id: '8', arabic: 'لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللهِ العَلِيِّ العَظِيمِ', defaultTarget: 100 },
];

// للتوافق مع أي استخدام داخلي قديم
const PRESETS = TASBIH_PRESETS;

export const TasbihSection: React.FC = () => {
  const [selectedPreset, setSelectedPreset] = useState<TasbihPreset>(PRESETS[0]);
  const [customText, setCustomText] = useState<string>('');
  const [target, setTarget] = useState<number>(33);
  const [count, setCount] = useState<number>(0);
  const [cycles, setCycles] = useState<number>(0);
  const [lifetimeTotal, setLifetimeTotal] = useState<number>(() => {
    try {
      return Number(localStorage.getItem('tasbih_lifetime_total')) || 124;
    } catch {
      return 124;
    }
  });
  const [soundEnabled, setSoundEnabled] = useState<boolean>(true);
  const [vibrateEnabled, setVibrateEnabled] = useState<boolean>(true);

  // Play click tick sound using web audio
  const playClickAudio = () => {
    if (!soundEnabled) return;
    try {
      const ctx = new (window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext)();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(600, ctx.currentTime);
      gain.gain.setValueAtTime(0.15, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.05);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start();
      osc.stop(ctx.currentTime + 0.05);
    } catch {
      // AudioContext unavailable
    }
  };

  const handleIncrement = () => {
    playClickAudio();
    if (vibrateEnabled && 'vibrate' in navigator) {
      navigator.vibrate(20);
    }

    const nextCount = count + 1;
    const newLifetime = lifetimeTotal + 1;
    setLifetimeTotal(newLifetime);
    try {
      localStorage.setItem('tasbih_lifetime_total', newLifetime.toString());
    } catch {}

    if (target > 0 && nextCount >= target) {
      setCount(0);
      setCycles((c) => c + 1);
      if (vibrateEnabled && 'vibrate' in navigator) {
        navigator.vibrate([50, 80, 50]); // double buzz on cycle completion
      }
    } else {
      setCount(nextCount);
    }
  };

  const handleResetCurrent = () => {
    setCount(0);
  };

  const currentDhikr = customText.trim() ? customText : selectedPreset.arabic;
  const progressPercent = target > 0 ? Math.min(100, Math.round((count / target) * 100)) : 100;

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Header */}
      <div className="p-5 rounded-3xl bg-slate-900/90 border border-slate-800 shadow-xl flex flex-col sm:flex-row items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-white flex items-center gap-2">
            <span>📿</span>
            <span>المسبحة الإلكترونية الذكية</span>
          </h2>
          <p className="text-xs text-slate-400 mt-0.5">
            تسبيح ميسر مع ردود فعل لمسية وصوتية وأهداف يومية
          </p>
        </div>

        {/* Stats pill */}
        <div className="flex items-center gap-4 bg-slate-950 px-4 py-2 rounded-2xl border border-slate-800 text-xs">
          <div>
            <span className="text-slate-400 block text-[10px]">الدورات المكتملة</span>
            <span className="font-mono font-bold text-emerald-400 text-base">{cycles}</span>
          </div>
          <div className="w-px h-6 bg-slate-800" />
          <div>
            <span className="text-slate-400 block text-[10px]">إجمالي التسبيحات</span>
            <span className="font-mono font-bold text-teal-300 text-base">{lifetimeTotal}</span>
          </div>
        </div>
      </div>

      {/* Main Tasbih Tactile Controller */}
      <div className="p-8 rounded-3xl bg-gradient-to-b from-slate-900 via-slate-950 to-emerald-950/40 border border-slate-800 shadow-2xl flex flex-col items-center text-center relative overflow-hidden">
        {/* Subtle background glow */}
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-80 h-80 rounded-full bg-emerald-500/10 blur-3xl pointer-events-none" />

        {/* Current Dhikr text */}
        <div className="mb-6 relative z-10">
          <p className="font-quran text-2xl sm:text-3xl text-emerald-300 font-bold min-h-12 flex items-center justify-center">
            {currentDhikr}
          </p>
          <div className="flex items-center justify-center gap-2 mt-2">
            <span className="text-xs text-slate-400">
              الهدف: <span className="text-emerald-400 font-mono font-bold">{target > 0 ? target : 'مفتوح'}</span>
            </span>
            <span className="text-slate-600">•</span>
            <span className="text-xs text-slate-400">
              الدورة رقم: <span className="text-teal-400 font-mono font-bold">{cycles + 1}</span>
            </span>
          </div>
        </div>

        {/* Big Tap Bead Circle Button */}
        <div className="relative z-10 my-4">
          <button
            onClick={handleIncrement}
            className="w-56 h-56 sm:w-64 sm:h-64 rounded-full bg-gradient-to-br from-emerald-500 via-teal-600 to-emerald-700 text-slate-950 p-3 shadow-2xl shadow-emerald-900/60 ring-8 ring-emerald-500/20 active:scale-95 transition-all duration-150 flex flex-col items-center justify-center select-none"
            aria-label="تسبيح عداد"
          >
            <div className="w-full h-full rounded-full bg-slate-950/90 border border-emerald-400/40 flex flex-col items-center justify-center text-white">
              <span className="text-6xl sm:text-7xl font-mono font-extrabold tracking-tight bg-gradient-to-r from-white via-emerald-200 to-teal-100 bg-clip-text text-transparent">
                {count}
              </span>
              <span className="text-xs text-emerald-400 font-medium mt-1">
                {target > 0 ? `${count} / ${target}` : 'انقر للتسبيح'}
              </span>
            </div>
          </button>
        </div>

        {/* Progress bar under button */}
        {target > 0 && (
          <div className="w-full max-w-xs mt-3 relative z-10">
            <div className="w-full h-2 rounded-full bg-slate-800 overflow-hidden">
              <div
                className="h-full bg-gradient-to-r from-emerald-500 to-teal-400 transition-all duration-200"
                style={{ width: `${progressPercent}%` }}
              />
            </div>
            <div className="flex justify-between text-[11px] text-slate-400 mt-1 font-mono">
              <span>0</span>
              <span>%{progressPercent}</span>
              <span>{target}</span>
            </div>
          </div>
        )}

        {/* Action Controls (Reset, Sound, Vibrate) */}
        <div className="flex items-center gap-3 mt-6 relative z-10">
          <button
            onClick={handleResetCurrent}
            className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-slate-900 border border-slate-700 text-xs text-slate-300 hover:text-white hover:border-slate-600 transition-colors"
          >
            <RotateCcw className="w-3.5 h-3.5" />
            <span>تصفير العداد</span>
          </button>

          <button
            onClick={() => setSoundEnabled(!soundEnabled)}
            className={`p-2 rounded-xl border text-xs transition-colors ${
              soundEnabled
                ? 'bg-emerald-500/20 text-emerald-300 border-emerald-500/40'
                : 'bg-slate-900 text-slate-400 border-slate-700'
            }`}
            title="صوت النقر"
          >
            {soundEnabled ? <Volume2 className="w-4 h-4" /> : <VolumeX className="w-4 h-4" />}
          </button>

          <button
            onClick={() => setVibrateEnabled(!vibrateEnabled)}
            className={`px-3 py-2 rounded-xl border text-xs font-medium transition-colors ${
              vibrateEnabled
                ? 'bg-emerald-500/20 text-emerald-300 border-emerald-500/40'
                : 'bg-slate-900 text-slate-400 border-slate-700'
            }`}
            title="الاهتزاز اللمسي"
          >
            {vibrateEnabled ? '📳 اهتزاز مفعّل' : '📳 اهتزاز معطل'}
          </button>
        </div>
      </div>

      {/* Target & Presets Selection */}
      <div className="p-6 rounded-3xl bg-slate-900/90 border border-slate-800 space-y-5">
        <div>
          <label className="block text-xs font-bold text-slate-300 mb-2">
            اختر الذكر المفضل:
          </label>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-2.5">
            {PRESETS.map((preset) => {
              const isSelected = selectedPreset.id === preset.id && !customText.trim();
              return (
                <button
                  key={preset.id}
                  onClick={() => {
                    setSelectedPreset(preset);
                    setCustomText('');
                    setTarget(preset.defaultTarget);
                    setCount(0);
                  }}
                  className={`p-3 rounded-2xl border text-right transition-all font-medium ${
                    isSelected
                      ? 'bg-emerald-950 border-emerald-500 text-white shadow-md'
                      : 'bg-slate-950/60 border-slate-800 text-slate-300 hover:border-slate-700'
                  }`}
                >
                  <p className="font-quran text-base line-clamp-1">{preset.arabic}</p>
                  <span className="text-[10px] text-slate-400 mt-1 block">
                    الهدف الافتراضي: {preset.defaultTarget} مرة
                  </span>
                </button>
              );
            })}
          </div>
        </div>

        {/* Custom text & target pills */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-4 border-t border-slate-800/80">
          <div>
            <label className="block text-xs font-bold text-slate-300 mb-1.5">
              أو اكتب ذكراً أو دعاء مخصصاً:
            </label>
            <input
              type="text"
              placeholder="مثلاً: لا إله إلا أنت سبحانك إني كنت من الظالمين"
              value={customText}
              onChange={(e) => setCustomText(e.target.value)}
              className="w-full bg-slate-950 border border-slate-800 text-xs text-slate-200 rounded-xl px-3 py-2.5 focus:ring-1 focus:ring-emerald-500"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-300 mb-1.5">
              تحديد هدف الدورة:
            </label>
            <div className="flex items-center gap-2">
              {[33, 100, 1000, 0].map((tVal) => (
                <button
                  key={tVal}
                  onClick={() => {
                    setTarget(tVal);
                    setCount(0);
                  }}
                  className={`flex-1 py-2 rounded-xl text-xs font-bold border transition-colors ${
                    target === tVal
                      ? 'bg-emerald-600 text-white border-emerald-500'
                      : 'bg-slate-950 text-slate-400 border-slate-800 hover:text-white'
                  }`}
                >
                  {tVal === 0 ? 'مفتوح' : tVal}
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
