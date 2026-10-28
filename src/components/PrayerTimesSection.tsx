import React, { useState, useEffect, useRef } from 'react';
import {
  Bell,
  BellOff,
  Volume2,
  VolumeX,
  Compass,
  Settings,
  Sparkles,
  Play,
  Pause,
  RotateCw,
  Clock,
  ShieldCheck,
} from 'lucide-react';
import {
  Coordinates,
  PrayerTimesResult,
  NextPrayerInfo,
  PrayerTimeAdjustments,
  computePrayerTimes,
  getNextPrayer,
  calculateQibla,
  CalculationMethod,
  CALCULATION_METHODS,
  PRAYER_NAMES_AR,
} from '../utils/prayerMath';
import { getSavedPrayerAdjustments } from '../utils/appSettings';

import { LocationPickerModal } from './LocationPickerModal';
import { MapPin, Globe2 } from 'lucide-react';

interface PrayerTimesSectionProps {
  coordinates: Coordinates;
  setCoordinates?: (coords: Coordinates) => void;
  soundEnabled: boolean;
  onOpenAdhkarAfterPrayer?: () => void;
  onOpenQibla?: () => void;
}

interface AdhanVoice {
  id: string;
  name: string;
  location: string;
  url: string;
}

const ADHAN_VOICES: AdhanVoice[] = [
  {
    id: 'makkah',
    name: 'أذان الحرم المكي الشريف',
    location: 'مكة المكرمة',
    url: 'https://cdn.islamic.network/audio/adhan/1.mp3',
  },
  {
    id: 'madinah',
    name: 'أذان المسجد النبوي الشريف',
    location: 'المدينة المنورة',
    url: 'https://cdn.islamic.network/audio/adhan/2.mp3',
  },
  {
    id: 'aqsa',
    name: 'أذان المسجد الأقصى المبارك',
    location: 'القدس الشريف',
    url: 'https://cdn.islamic.network/audio/adhan/3.mp3',
  },
  {
    id: 'abdulbasit',
    name: 'أذان الشيخ عبد الباسط عبد الصمد',
    location: 'مصر',
    url: 'https://cdn.islamic.network/audio/adhan/4.mp3',
  },
];

export const PrayerTimesSection: React.FC<PrayerTimesSectionProps> = ({
  coordinates,
  setCoordinates,
  soundEnabled,
  onOpenAdhkarAfterPrayer,
  onOpenQibla,
}) => {
  const [method, setMethod] = useState<CalculationMethod>('Egypt');
  const [isHanafi, setIsHanafi] = useState<boolean>(false);
  const [selectedVoice, setSelectedVoice] = useState<AdhanVoice>(ADHAN_VOICES[0]);
  const [isPlayingAdhan, setIsPlayingAdhan] = useState<boolean>(false);
  const [showLocationModal, setShowLocationModal] = useState<boolean>(false);
  const [showSettings, setShowSettings] = useState<boolean>(false);
  const [showQibla, setShowQibla] = useState<boolean>(false);
  const [now, setNow] = useState<Date>(new Date());
  
  // Notification preference per prayer
  const [prayerNotifications, setPrayerNotifications] = useState<Record<string, 'adhan' | 'notification' | 'off'>>({
    fajr: 'adhan',
    sunrise: 'notification',
    dhuhr: 'adhan',
    asr: 'adhan',
    maghrib: 'adhan',
    isha: 'adhan',
  });

  const audioRef = useRef<HTMLAudioElement | null>(null);

  // الضبط الدقيق للمواقيت (±دقائق لكل صلاة) — من الإعدادات
  const [prayerAdjustments, setPrayerAdjustments] = useState<PrayerTimeAdjustments>(getSavedPrayerAdjustments);

  // إعادة قراءة الضبط عند رجوع المستخدم من الإعدادات للتبويب
  useEffect(() => {
    setPrayerAdjustments(getSavedPrayerAdjustments());
  }, []);

  // Update clock every second
  useEffect(() => {
    const timer = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  const prayerTimes: PrayerTimesResult = computePrayerTimes(
    now,
    coordinates.latitude,
    coordinates.longitude,
    method,
    isHanafi,
    prayerAdjustments
  );

  const nextPrayer: NextPrayerInfo = getNextPrayer(prayerTimes, now);
  const qiblaAngle = Math.round(calculateQibla(coordinates.latitude, coordinates.longitude));

  const toggleAdhanPreview = () => {
    if (isPlayingAdhan) {
      audioRef.current?.pause();
      setIsPlayingAdhan(false);
    } else {
      if (!audioRef.current) {
        audioRef.current = new Audio(selectedVoice.url);
        audioRef.current.onended = () => setIsPlayingAdhan(false);
        audioRef.current.onerror = () => {
          setIsPlayingAdhan(false);
          // Play synth tone if remote fails
          playSynthesizedAdhanTone();
        };
      } else {
        audioRef.current.src = selectedVoice.url;
      }
      audioRef.current.play().catch(() => {
        playSynthesizedAdhanTone();
      });
      setIsPlayingAdhan(true);
    }
  };

  const playSynthesizedAdhanTone = () => {
    try {
      const ctx = new (window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext)();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(440, ctx.currentTime);
      osc.frequency.exponentialRampToValueAtTime(330, ctx.currentTime + 1.2);
      gain.gain.setValueAtTime(0.3, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 1.2);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start();
      osc.stop(ctx.currentTime + 1.2);
    } catch {
      // AudioContext unavailable
    }
  };

  const toggleNotification = (prayerKey: string) => {
    setPrayerNotifications((prev) => {
      const current = prev[prayerKey] || 'adhan';
      const nextVal = current === 'adhan' ? 'notification' : current === 'notification' ? 'off' : 'adhan';
      return { ...prev, [prayerKey]: nextVal };
    });
  };

  const prayersList: Array<{ key: 'fajr' | 'sunrise' | 'dhuhr' | 'asr' | 'maghrib' | 'isha'; icon: string }> = [
    { key: 'fajr', icon: '🌌' },
    { key: 'sunrise', icon: '🌅' },
    { key: 'dhuhr', icon: '☀️' },
    { key: 'asr', icon: '🌤️' },
    { key: 'maghrib', icon: '🌇' },
    { key: 'isha', icon: '🌙' },
  ];

  return (
    <div className="space-y-6">
      {/* Top Banner: Next Prayer Card */}
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-emerald-950 via-teal-900 to-slate-950 border border-emerald-700/40 p-6 sm:p-8 shadow-2xl shadow-emerald-950/50">
        <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-emerald-500 via-teal-400 to-emerald-600" />
        <div className="absolute -left-12 -bottom-12 w-48 h-48 rounded-full bg-emerald-500/10 blur-3xl pointer-events-none" />

        <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-500/20 text-emerald-300 text-xs font-medium border border-emerald-500/30 mb-3">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping" />
              <span>الصلاة القادمة • المؤذن التلقائي</span>
            </div>
            <h2 className="text-3xl sm:text-4xl font-extrabold text-white tracking-wide">
              صلاة {nextPrayer.arabicName}
            </h2>
            <p className="text-emerald-200/80 text-sm mt-1">
              موعد الأذان: <span className="font-bold text-white text-base">{nextPrayer.timeStr}</span> ({coordinates.city})
            </p>
          </div>

          {/* Countdown timer box */}
          <div className="flex flex-col items-start md:items-end">
            <div className="text-xs text-slate-300 mb-1 flex items-center gap-1.5">
              <Clock className="w-3.5 h-3.5 text-emerald-400" />
              <span>متبقي على رفع الأذان</span>
            </div>
            <div className="text-4xl sm:text-5xl font-mono font-bold tracking-tight bg-gradient-to-r from-white via-emerald-100 to-teal-200 bg-clip-text text-transparent">
              {nextPrayer.formattedRemaining}
            </div>

            {/* Progress bar */}
            <div className="w-full md:w-56 mt-3">
              <div className="w-full h-2 rounded-full bg-slate-800/80 overflow-hidden">
                <div
                  className="h-full bg-gradient-to-r from-emerald-500 to-teal-400 transition-all duration-1000"
                  style={{ width: `${nextPrayer.progressPercent}%` }}
                />
              </div>
            </div>
          </div>
        </div>

        {/* Action buttons inside top banner */}
        <div className="relative z-10 mt-6 pt-6 border-t border-emerald-800/40 flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <button
              onClick={toggleAdhanPreview}
              className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs sm:text-sm font-semibold transition-all shadow-lg ${
                isPlayingAdhan
                  ? 'bg-rose-600 text-white shadow-rose-900/50 animate-pulse'
                  : 'bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold shadow-emerald-950/60'
              }`}
              aria-label="سماع الأذان الآن بصوت المؤذن المختار"
            >
              {isPlayingAdhan ? <Pause className="w-4 h-4" /> : <Play className="w-4 h-4" />}
              <span>{isPlayingAdhan ? 'إيقاف صوت الأذان' : 'سماع صوت الأذان الآن'}</span>
            </button>

            <select
              value={selectedVoice.id}
              onChange={(e) => {
                const voice = ADHAN_VOICES.find((v) => v.id === e.target.value);
                if (voice) {
                  setSelectedVoice(voice);
                  if (isPlayingAdhan) {
                    setIsPlayingAdhan(false);
                    audioRef.current?.pause();
                  }
                }
              }}
              className="bg-slate-900/90 border border-emerald-800/60 text-xs text-slate-200 rounded-xl px-3 py-2 focus:ring-1 focus:ring-emerald-400 focus:outline-none"
              aria-label="اختر صوت المؤذن"
            >
              {ADHAN_VOICES.map((voice) => (
                <option key={voice.id} value={voice.id} className="bg-slate-900">
                  🎙️ {voice.name}
                </option>
              ))}
            </select>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <button
              onClick={() => setShowLocationModal(true)}
              className="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-slate-900/80 border border-slate-700 hover:border-emerald-500 text-xs text-slate-200 transition-colors"
              title="تحديد الموقع عبر GPS أو اختيار البلد والمدينة"
            >
              <MapPin className="w-4 h-4 text-emerald-400" />
              <span>تغيير الموقع ({coordinates.city.split('،')[0]})</span>
            </button>

            <button
              onClick={() => {
                if (onOpenQibla) {
                  onOpenQibla();
                } else {
                  setShowQibla(!showQibla);
                }
              }}
              className="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-slate-900/80 border border-slate-700 hover:border-emerald-500 text-xs text-slate-200 transition-colors"
              title="فتح بوصلة القبلة التفاعلية الشريفة"
            >
              <Compass className="w-4 h-4 text-emerald-400" />
              <span>بوصلة القبلة ({qiblaAngle}°)</span>
            </button>

            <button
              onClick={() => setShowSettings(!showSettings)}
              className="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-slate-900/80 border border-slate-700 hover:border-emerald-500 text-xs text-slate-200 transition-colors"
            >
              <Settings className="w-4 h-4 text-slate-400" />
              <span>إعدادات الحساب</span>
            </button>
          </div>
        </div>
      </div>

      {/* Location Picker Modal */}
      {setCoordinates && (
        <LocationPickerModal
          isOpen={showLocationModal}
          onClose={() => setShowLocationModal(false)}
          currentCoords={coordinates}
          onSelectCoords={(coords) => setCoordinates(coords)}
        />
      )}

      {/* Qibla Direction Drawer / Box */}
      {showQibla && (
        <div className="p-6 rounded-2xl bg-slate-900/90 border border-emerald-600/30 text-center animate-in fade-in slide-in-from-top duration-300">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-base font-bold text-white flex items-center gap-2">
              <Compass className="w-5 h-5 text-emerald-400" />
              <span>بوصلة القبلة نحو الكعبة المشرفة</span>
            </h3>
            <button
              onClick={() => setShowQibla(false)}
              className="text-xs text-slate-400 hover:text-white"
            >
              إغلاق
            </button>
          </div>
          <div className="flex flex-col items-center justify-center my-4">
            <div className="relative w-48 h-48 rounded-full border-4 border-slate-700 bg-slate-950 flex items-center justify-center shadow-inner">
              <div className="absolute top-2 text-xs font-bold text-slate-400">ش (N)</div>
              <div className="absolute bottom-2 text-xs font-bold text-slate-400">ج (S)</div>
              <div className="absolute right-2 text-xs font-bold text-slate-400">ق (E)</div>
              <div className="absolute left-2 text-xs font-bold text-slate-400">غ (W)</div>
              
              {/* Needle pointing to Qibla */}
              <div
                className="w-1 h-20 bg-gradient-to-t from-transparent via-emerald-500 to-emerald-400 origin-bottom transition-transform duration-700"
                style={{ transform: `rotate(${qiblaAngle}deg) translateY(-50%)` }}
              >
                <div className="w-4 h-4 -mt-2 -ml-1.5 rounded-full bg-emerald-400 border-2 border-white shadow-lg flex items-center justify-center text-[8px] text-slate-950 font-bold">
                  🕋
                </div>
              </div>

              <div className="w-4 h-4 rounded-full bg-slate-400 border border-slate-900 z-10" />
            </div>
            <p className="mt-4 text-sm text-slate-300">
              زاوية القبلة من <span className="font-bold text-emerald-300">{coordinates.city}</span> هي{' '}
              <span className="font-bold text-emerald-400">{qiblaAngle}°</span> باتجاه الجنوب الشرقي نحو مكة المكرمة.
            </p>

            {onOpenQibla && (
              <button
                onClick={onOpenQibla}
                className="mt-4 px-5 py-2.5 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white text-xs font-bold shadow-lg shadow-emerald-950/60 flex items-center gap-2"
              >
                <Compass className="w-4 h-4" />
                <span>فتح بوصلة القبلة الحية التفاعلية مع الحساسات ⮜</span>
              </button>
            )}
          </div>
        </div>
      )}

      {/* Calculation Settings Box */}
      {showSettings && (
        <div className="p-5 rounded-2xl bg-slate-900/90 border border-slate-800 space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-bold text-white flex items-center gap-2">
              <Settings className="w-4 h-4 text-emerald-400" />
              <span>طريقة الحساب الفلكي والمذهب</span>
            </h3>
            <button
              onClick={() => setShowSettings(false)}
              className="text-xs text-slate-400 hover:text-white"
            >
              تم
            </button>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs text-slate-400 mb-1.5">جهة الحساب الفلكي</label>
              <select
                value={method}
                onChange={(e) => setMethod(e.target.value as CalculationMethod)}
                className="w-full bg-slate-950 border border-slate-800 text-xs text-slate-200 rounded-lg p-2.5 focus:ring-1 focus:ring-emerald-500"
              >
                {Object.entries(CALCULATION_METHODS).map(([key, val]) => (
                  <option key={key} value={key}>
                    {val.arabicName}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs text-slate-400 mb-1.5">مذهب صلاة العصر</label>
              <div className="flex items-center gap-2 bg-slate-950 p-1.5 rounded-lg border border-slate-800">
                <button
                  type="button"
                  onClick={() => setIsHanafi(false)}
                  className={`flex-1 py-1.5 text-xs rounded font-medium transition-colors ${
                    !isHanafi ? 'bg-emerald-600 text-white' : 'text-slate-400 hover:text-white'
                  }`}
                >
                  الجمهور (شافعي / مالكي / حنبلي)
                </button>
                <button
                  type="button"
                  onClick={() => setIsHanafi(true)}
                  className={`flex-1 py-1.5 text-xs rounded font-medium transition-colors ${
                    isHanafi ? 'bg-emerald-600 text-white' : 'text-slate-400 hover:text-white'
                  }`}
                >
                  المذهب الحنفي
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Five Prayers Cards Grid */}
      <div>
        <div className="flex items-center justify-between mb-3 px-1">
          <h3 className="text-base font-bold text-white flex items-center gap-2">
            <span>مواقيت اليوم للصلوات الخمس</span>
          </h3>
          <span className="text-xs text-slate-400">
            انقر على أيقونة الجرس لتغيير تنبيه الصلاة
          </span>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
          {prayersList.map(({ key, icon }) => {
            const isNext = nextPrayer.name === key;
            const notifState = prayerNotifications[key] || 'adhan';
            const timeStr = prayerTimes[key];

            return (
              <div
                key={key}
                className={`relative rounded-2xl p-4 transition-all duration-200 border ${
                  isNext
                    ? 'bg-gradient-to-b from-emerald-950/90 to-teal-950/70 border-emerald-500 shadow-lg shadow-emerald-950/60 ring-1 ring-emerald-400/40'
                    : 'bg-slate-900/70 border-slate-800/80 hover:border-slate-700'
                }`}
              >
                {isNext && (
                  <span className="absolute -top-2.5 right-3 px-2 py-0.5 rounded-full bg-emerald-500 text-[10px] font-bold text-slate-950 shadow">
                    القادمة
                  </span>
                )}

                <div className="flex items-center justify-between mb-2">
                  <span className="text-xl">{icon}</span>
                  <button
                    onClick={() => toggleNotification(key)}
                    className={`p-1.5 rounded-lg border text-xs transition-colors ${
                      notifState === 'adhan'
                        ? 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30'
                        : notifState === 'notification'
                        ? 'bg-amber-500/20 text-amber-300 border-amber-500/30'
                        : 'bg-slate-800 text-slate-500 border-slate-700'
                    }`}
                    title={
                      notifState === 'adhan'
                        ? 'صوت الأذان كامل'
                        : notifState === 'notification'
                        ? 'إشعار صامت فقط'
                        : 'بدون تنبيه'
                    }
                  >
                    {notifState === 'adhan' ? (
                      <Volume2 className="w-3.5 h-3.5" />
                    ) : notifState === 'notification' ? (
                      <Bell className="w-3.5 h-3.5" />
                    ) : (
                      <BellOff className="w-3.5 h-3.5" />
                    )}
                  </button>
                </div>

                <div className="text-sm font-semibold text-slate-300">
                  {PRAYER_NAMES_AR[key]}
                </div>
                <div
                  className={`text-lg font-bold mt-1 font-mono ${
                    isNext ? 'text-emerald-300' : 'text-white'
                  }`}
                >
                  {timeStr}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Post Prayer Banner: Shortcut to After-Prayer Adhkar */}
      {onOpenAdhkarAfterPrayer && (
        <div className="rounded-2xl p-4 bg-gradient-to-r from-emerald-950/60 via-slate-900 to-teal-950/60 border border-emerald-800/40 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-3 text-right">
            <div className="w-10 h-10 rounded-xl bg-emerald-500/20 text-emerald-300 flex items-center justify-center shrink-0">
              <Sparkles className="w-5 h-5" />
            </div>
            <div>
              <h4 className="text-sm font-bold text-white">
                أنهيت صلاتك للتو؟ اقرأ أذكار ما بعد الصلاة
              </h4>
              <p className="text-xs text-slate-400">
                الاستغفار، آية الكرسي، والتسبيح ٣٣ والتحميد والتكبير لتمام المائة ومغفرة الذنوب
              </p>
            </div>
          </div>
          <button
            onClick={onOpenAdhkarAfterPrayer}
            className="w-full sm:w-auto px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold whitespace-nowrap shadow-md transition-all shrink-0"
          >
            فتح أذكار بعد الصلاة الآن 📿
          </button>
        </div>
      )}
    </div>
  );
};
