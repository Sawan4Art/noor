import React, { useState, useEffect } from 'react';
import {
  Coordinates,
  PrayerTimesResult,
  NextPrayerInfo,
  PrayerTimeAdjustments,
  computePrayerTimes,
  getNextPrayer,
  PRAYER_NAMES_AR,
  PrayerTimeKey,
  isRamadanNow,
} from '../utils/prayerMath';
import { getSavedPrayerAdjustments, getSavedSuhoorOffsetMinutes } from '../utils/appSettings';

/**
 * الصفحة الرئيسية للمواقيت — بطلب المالك (2026-10-29):
 * - السحور فقط في رمضان المبارك (أُلغي الإمساك نهائياً بطلب المالك)
 * - تنبيه السحور اختياري من الإعدادات — افتراضه قبل الفجر بـ15 دقيقة
 * - مواقيت الصلاة + الصلاة القادمة مع العداد التنازلي
 */

interface PrayerTimesSectionProps {
  coordinates: Coordinates;
  method: Parameters<typeof computePrayerTimes>[3];
  isHanafi: boolean;
}

const PRAYER_ICONS: Record<string, string> = {
  fajr: '🌌',
  sunrise: '🌅',
  dhuhr: '☀️',
  asr: '🌤️',
  maghrib: '🌇',
  isha: '🌙',
};

export const PrayerTimesSection: React.FC<PrayerTimesSectionProps> = ({
  coordinates,
  method,
  isHanafi,
}) => {
  const [now, setNow] = useState<Date>(new Date());
  const [prayerAdjustments, setPrayerAdjustments] = useState<PrayerTimeAdjustments>(
    getSavedPrayerAdjustments,
  );

  // إعادة قراءة الضبط عند الرجوع من الإعدادات
  useEffect(() => {
    setPrayerAdjustments(getSavedPrayerAdjustments());
  }, []);

  // نبضة الساعة كل ثانية للعداد التنازلي
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
    prayerAdjustments,
  );

  const nextPrayer: NextPrayerInfo = getNextPrayer(prayerTimes, now);

  // رمضان + السحور: بطاقة السحور في رمضان فقط — أُلغي الإمساك نهائياً (بطلب المالك)
  const isRamadan = isRamadanNow(now);
  const suhoorOffset = getSavedSuhoorOffsetMinutes();
  // نهاية السحور = أذان الفجر ناقص مدة التنبيه المحددة
  const suhoorEndDate = new Date(
    prayerTimes.rawTimes.fajr.getTime() - suhoorOffset * 60 * 1000,
  );
  const formatHM = (d: Date): string =>
    d.toLocaleTimeString('ar-EG', { hour: '2-digit', minute: '2-digit' });
  const suhoorEndTime = formatHM(suhoorEndDate);
  const suhoorRemainingSec = Math.max(
    0,
    Math.floor((suhoorEndDate.getTime() - now.getTime()) / 1000),
  );

  // تنسيق الوقت المتبقي من الثواني الخام
  const formatCountdown = (totalSeconds: number): string => {
    if (totalSeconds <= 0) return 'حان الآن';
    const h = Math.floor(totalSeconds / 3600);
    const m = Math.floor((totalSeconds % 3600) / 60);
    const s = totalSeconds % 60;
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${pad(h)}:${pad(m)}:${pad(s)}`;
  };

  const prayersList: PrayerTimeKey[] = ['fajr', 'sunrise', 'dhuhr', 'asr', 'maghrib', 'isha'];

  return (
    <div className="space-y-6 animate-in fade-in duration-300">
      {/* ===== بطاقة الصلاة القادمة مع العداد التنازلي ===== */}
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-emerald-950 via-teal-900 to-slate-950 border border-emerald-700/40 p-6 sm:p-8 shadow-2xl shadow-emerald-950/50">
        <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-emerald-500 via-teal-400 to-emerald-600" />
        <div className="absolute -left-12 -bottom-12 w-48 h-48 rounded-full bg-emerald-500/10 blur-3xl pointer-events-none" />
        <div className="absolute -right-16 -top-16 w-56 h-56 rounded-full bg-teal-400/10 blur-3xl pointer-events-none" />

        <div className="relative z-10 text-center">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-500/20 text-emerald-300 text-xs font-medium border border-emerald-500/30 mb-4">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping" />
            <span>الصلاة القادمة</span>
          </div>

          <h2 className="text-3xl sm:text-4xl font-extrabold text-white mb-1">
            {nextPrayer.arabicName}
          </h2>

          <p className="text-emerald-200/90 text-lg sm:text-xl font-bold mb-5 font-mono">
            {nextPrayer.timeStr}
          </p>

          {/* العداد التنازلي */}
          <div
            className="mx-auto max-w-xs rounded-2xl border border-emerald-500/30 bg-slate-950/60 backdrop-blur px-6 py-4"
            dir="ltr"
          >
            <p className="text-[11px] text-emerald-300/80 mb-1 text-center" dir="rtl">
              الوقت المتبقي
            </p>
            <p className="text-4xl sm:text-5xl font-mono font-black text-white text-center tracking-wider tabular-nums">
              {formatCountdown(nextPrayer.remainingSeconds)}
            </p>
          </div>

          <p className="mt-4 text-xs text-emerald-200/70">
            🕌 {coordinates.city}
          </p>
        </div>
      </div>

      {/* ===== بطاقة السحور — رمضان فقط (أُلغي الإمساك بطلب المالك) ===== */}
      {isRamadan && (
        <div className="rounded-3xl border shadow-xl overflow-hidden bg-slate-900/90 border-amber-700/40">
          <div className="px-5 sm:px-6 py-4 border-b border-amber-800/30 flex items-center justify-between">
            <h3 className="text-base font-bold text-white">السحور 🍽️</h3>
            <span className="text-[11px] text-amber-300/90">رمضان مبارك 🌙</span>
          </div>
          <div className="p-5 text-center space-y-2">
            <p className="text-xs text-amber-300/90 font-bold">نهاية السحور قبل أذان الفجر بـ{suhoorOffset} دقيقة</p>
            <p className="text-3xl sm:text-4xl font-mono font-black text-white" dir="ltr">
              {suhoorEndTime}
            </p>
            <p className="text-[11px] text-slate-400">
              {suhoorRemainingSec > 0 && suhoorRemainingSec < 12 * 3600
                ? `يتبقى على نهاية السحور ${formatCountdown(suhoorRemainingSec)}`
                : '«تسحروا فإن في السحور بركة»'}
            </p>
          </div>
          <div className="px-5 py-2.5 bg-slate-950/60 border-t border-amber-800/20 text-center">
            <p className="text-[10px] text-slate-500 leading-relaxed">
              تنبيه السحور اختياري من الإعدادات ← الأذان والتنبيهات (افتراضه قبل أذان الفجر بـ{suhoorOffset} دقيقة)
            </p>
          </div>
        </div>
      )}

      {/* ===== مواقيت اليوم ===== */}
      <div className="rounded-3xl border shadow-xl overflow-hidden bg-slate-900/90 border-slate-800">
        <div className="px-5 sm:px-6 py-4 border-b border-slate-800 flex items-center justify-between">
          <h3 className="text-base font-bold text-white">مواقيت الصلاة اليوم</h3>
          <span className="text-[11px] text-slate-400">{coordinates.city}</span>
        </div>

        <div className="divide-y divide-slate-800/70">
          {prayersList.map((key) => {
            const isNext = nextPrayer.name === key;
            const isPrayer = key !== 'sunrise';
            return (
              <div
                key={key}
                className={`flex items-center justify-between px-5 sm:px-6 py-3.5 transition-colors ${
                  isNext
                    ? 'bg-gradient-to-l from-emerald-600/25 to-transparent border-r-4 border-emerald-400'
                    : ''
                }`}
              >
                <div className="flex items-center gap-3.5">
                  <span className="text-xl">{PRAYER_ICONS[key]}</span>
                  <div>
                    <p
                      className={`font-bold text-sm sm:text-base ${
                        isNext ? 'text-emerald-300' : 'text-slate-200'
                      }`}
                    >
                      {PRAYER_NAMES_AR[key]}
                      {!isPrayer && (
                        <span className="text-[10px] text-slate-500 mr-1.5">(ليست صلاة)</span>
                      )}
                    </p>
                    {isNext && (
                      <p className="text-[10px] text-emerald-400/90 font-semibold">
                        القادمة بعد {formatCountdown(nextPrayer.remainingSeconds)}
                      </p>
                    )}
                  </div>
                </div>

                <p
                  className={`text-lg sm:text-xl font-mono font-bold tabular-nums ${
                    isNext ? 'text-emerald-300' : 'text-slate-300'
                  }`}
                  dir="ltr"
                >
                  {prayerTimes[key]}
                </p>
              </div>
            );
          })}
        </div>

        <div className="px-5 sm:px-6 py-3 bg-slate-950/60 border-t border-slate-800 text-center">
          <p className="text-[11px] text-slate-500">
            لتغيير الموقع أو طريقة الحساب أو الأذان — من الإعدادات ⚙️
          </p>
        </div>
      </div>
    </div>
  );
};
