import React, { useState, useEffect, useRef, useCallback } from 'react';
import {
  Compass,
  MapPin,
  RotateCw,
  Navigation,
  Sparkles,
  Info,
  CheckCircle2,
  Smartphone,
  Volume2,
  VolumeX,
  Share2,
  Crosshair,
  Sliders,
} from 'lucide-react';
import {
  Coordinates,
  calculateQibla,
  calculateDistanceToKaaba,
  computePrayerTimes,
} from '../utils/prayerMath';
import { LocationPickerModal } from './LocationPickerModal';
import { AppTheme, ColorMode } from '../utils/themeData';

interface QiblaCompassSectionProps {
  coordinates: Coordinates;
  setCoordinates?: (coords: Coordinates) => void;
  theme?: AppTheme;
  colorMode?: ColorMode;
  onNavigateToTab?: (tab: 'prayer' | 'quran' | 'adhkar' | 'tasbih' | 'widgets') => void;
}

export const QiblaCompassSection: React.FC<QiblaCompassSectionProps> = ({
  coordinates,
  setCoordinates,
  theme,
  colorMode = 'dark',
  onNavigateToTab,
}) => {
  const [showLocationModal, setShowLocationModal] = useState<boolean>(false);
  const [deviceHeading, setDeviceHeading] = useState<number>(0);
  const [hasCompassSensor, setHasCompassSensor] = useState<boolean | null>(null);
  const [permissionState, setPermissionState] = useState<'prompt' | 'granted' | 'denied'>('prompt');
  const [isAligned, setIsAligned] = useState<boolean>(false);
  const [soundEnabled, setSoundEnabled] = useState<boolean>(true);
  const [manualRotation, setManualRotation] = useState<number>(0);
  const [useManualMode, setUseManualMode] = useState<boolean>(false);
  const [compassCalibrated, setCompassCalibrated] = useState<boolean>(true);

  const prevAlignedRef = useRef<boolean>(false);

  // Exact Qibla azimuth angle from user coords to Kaaba
  const qiblaAngle = Math.round(calculateQibla(coordinates.latitude, coordinates.longitude) * 10) / 10;
  // Great Circle distance to Kaaba in km
  const distanceKm = calculateDistanceToKaaba(coordinates.latitude, coordinates.longitude);

  // Current heading to evaluate (sensor or manual)
  const currentHeading = useManualMode ? manualRotation : deviceHeading;

  // Difference between current heading and Qibla angle (-180 to +180)
  let angleDiff = (qiblaAngle - currentHeading) % 360;
  if (angleDiff > 180) angleDiff -= 360;
  if (angleDiff < -180) angleDiff += 360;

  // Aligned if within +/- 3.5 degrees
  const alignedNow = Math.abs(angleDiff) <= 3.5;

  // Sound chime synthesizer
  const playAlignmentTone = useCallback(() => {
    if (!soundEnabled) return;
    try {
      const audioCtx = new (window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext)();
      const osc = audioCtx.createOscillator();
      const gain = audioCtx.createGain();

      osc.type = 'sine';
      osc.frequency.setValueAtTime(587.33, audioCtx.currentTime); // D5
      osc.frequency.exponentialRampToValueAtTime(880, audioCtx.currentTime + 0.15); // A5

      gain.gain.setValueAtTime(0.01, audioCtx.currentTime);
      gain.gain.linearRampToValueAtTime(0.2, audioCtx.currentTime + 0.05);
      gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.45);

      osc.connect(gain);
      gain.connect(audioCtx.destination);
      osc.start();
      osc.stop(audioCtx.currentTime + 0.45);
    } catch {
      // Audio not permitted yet
    }
  }, [soundEnabled]);

  // Haptic feedback & sound when entering alignment
  useEffect(() => {
    setIsAligned(alignedNow);
    if (alignedNow && !prevAlignedRef.current) {
      if ('vibrate' in navigator) {
        try {
          navigator.vibrate([40, 50, 40]);
        } catch {
          // ignore
        }
      }
      playAlignmentTone();
    }
    prevAlignedRef.current = alignedNow;
  }, [alignedNow, playAlignmentTone]);

  // Request DeviceOrientation permission (needed for iOS 13+)
  const requestOrientationPermission = async () => {
    const DeviceOrientation = window.DeviceOrientationEvent as unknown as {
      requestPermission?: () => Promise<'granted' | 'denied'>;
    };

    if (typeof DeviceOrientation?.requestPermission === 'function') {
      try {
        const response = await DeviceOrientation.requestPermission();
        setPermissionState(response);
        if (response === 'granted') {
          setUseManualMode(false);
        }
      } catch {
        setPermissionState('denied');
      }
    } else {
      setPermissionState('granted');
      setUseManualMode(false);
    }
  };

  // مستشعر الاتجاه — قراءة الحقيقة الأرضية (true heading)
  const smoothHeadingRef = useRef<number | null>(null);
  const lastEmittedRef = useRef<number | null>(null);

  /**
   * جذر إصلاح البوصلة (2026-10): كانت لا تشير للقبلة لسببين:
   * 1) على أندرويد كروم حدث deviceorientation يعطي alpha *نسبياً* (نقطة صفر عشوائية)
   *    وليس الشمال الجغرافي — الحل: الاستماع لـ deviceorientationabsolute أولاً،
   *    مع fallback لـ webkitCompassHeading (iOS) ثم deviceorientation.
   * 2) عدم تعويض دوران الشاشة (landscape) — الحل: إضافة زاوية الشاشة للقراءة.
   * مع تنعيم متحرك (low-pass) لمنع ارتجاف السهم.
   */
  useEffect(() => {
    const getScreenAngle = (): number => {
      try {
        const so = (screen as Screen & { orientation?: { angle?: number } }).orientation;
        if (so && typeof so.angle === 'number') return so.angle;
        const wo = (window as unknown as { orientation?: number }).orientation;
        if (typeof wo === 'number') return wo;
      } catch {
        // تجاهل
      }
      return 0;
    };

    const applyHeading = (rawHeading: number) => {
      const screenAngle = getScreenAngle();
      const compensated = (rawHeading + screenAngle % 360 + 360) % 360;

      // تنعيم: متوسط متحرك أسي (alpha صغير = استقرار أعلى)
      const prev = smoothHeadingRef.current;
      const smoothed =
        prev === null
          ? compensated
          : (() => {
              // أقصر مسار دائري بين القراءتين
              let delta = compensated - prev;
              if (delta > 180) delta -= 360;
              if (delta < -180) delta += 360;
              return prev + delta * 0.35;
            })();
      smoothHeadingRef.current = smoothed;

      // تحديث الحالة فقط لو التغيير ملموس (> 0.4 درجة) لتقليل إعادة الرسم
      const last = lastEmittedRef.current;
      const moved =
        last === null || Math.abs(((smoothed - last + 540) % 360) - 180) > 0.4;
      if (moved) {
        const rounded = Math.round(((smoothed % 360) + 360) % 360);
        lastEmittedRef.current = rounded;
        setDeviceHeading(rounded);
      }
    };

    const handleOrientation = (e: DeviceOrientationEvent) => {
      let heading: number | null = null;
      let absolute = false;

      // iOS Safari: webkitCompassHeading مرجعي بالشمال الحقيقي مباشرة
      const webkitHeading = (e as unknown as { webkitCompassHeading?: number }).webkitCompassHeading;
      if (typeof webkitHeading === 'number' && !isNaN(webkitHeading)) {
        heading = webkitHeading;
        absolute = true;
      } else if (typeof e.alpha === 'number' && !isNaN(e.alpha)) {
        heading = (360 - e.alpha) % 360;
        // deviceorientationabsolute أو e.absolute=true → مرجعي بالشمال
        absolute = e.type === 'deviceorientationabsolute' || e.absolute === true;
      }

      if (heading !== null) {
        if (absolute) {
          setHasCompassSensor(true);
          setCompassCalibrated(true);
          applyHeading(heading);
        } else {
          // مستشعر موجود لكن قراءته غير مرجعية — نعرضها مع تحذير المعايرة
          setHasCompassSensor(true);
          setCompassCalibrated(false);
          applyHeading(heading);
        }
      }
    };

    const attach = (eventName: string) => {
      window.addEventListener(eventName, handleOrientation as EventListener, true);
      return () => window.removeEventListener(eventName, handleOrientation as EventListener, true);
    };

    const cleanups: Array<() => void> = [];

    if (typeof window !== 'undefined' && window.DeviceOrientationEvent) {
      // 1) الأولوية: الحدث المطلق المرجعي بالشمال (أندرويد كروم)
      const absoluteSupported =
        'DeviceOrientationEvent' in window &&
        typeof (window as unknown as { ondeviceorientationabsolute?: unknown }).ondeviceorientationabsolute !== 'undefined';

      if (absoluteSupported) {
        cleanups.push(attach('deviceorientationabsolute'));
      } else {
        // 2) iOS عبر webkitCompassHeading داخل deviceorientation
        cleanups.push(attach('deviceorientation'));
      }

      // مراقب مساعد: لو المطلق ماوصلش خلال ثانيتين نسمع للنسبي كاحتياط
      const fallbackTimer = window.setTimeout(() => {
        if (absoluteSupported) {
          cleanups.push(attach('deviceorientation'));
        }
      }, 2000);
      cleanups.push(() => window.clearTimeout(fallbackTimer));
    } else {
      setHasCompassSensor(false);
      setUseManualMode(true);
    }

    return () => {
      cleanups.forEach((fn) => fn());
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Cardinal direction text description for Qibla
  const getQiblaDirectionDescription = (angle: number): string => {
    if (angle >= 337.5 || angle < 22.5) return 'شمالاً تماماً';
    if (angle >= 22.5 && angle < 67.5) return 'شمال شرق';
    if (angle >= 67.5 && angle < 112.5) return 'شرقاً تماماً';
    if (angle >= 112.5 && angle < 157.5) return 'جنوب شرق';
    if (angle >= 157.5 && angle < 202.5) return 'جنوباً تماماً';
    if (angle >= 202.5 && angle < 247.5) return 'جنوب غرب';
    if (angle >= 247.5 && angle < 292.5) return 'غرباً تماماً';
    return 'شمال غرب';
  };

  const isDark = colorMode === 'dark';

  return (
    <div className="space-y-6 animate-in fade-in duration-300">
      {/* Header Banner */}
      <div
        className={`relative overflow-hidden rounded-3xl p-6 sm:p-8 border shadow-xl ${
          isDark
            ? 'bg-gradient-to-br from-slate-900/90 via-slate-900/80 to-slate-950 border-emerald-800/40'
            : 'bg-gradient-to-br from-emerald-50 via-white to-teal-50 border-emerald-200'
        }`}
      >
        <div className="absolute top-0 right-0 -mt-8 -mr-8 w-64 h-64 bg-emerald-500/10 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute bottom-0 left-0 -mb-8 -ml-8 w-64 h-64 bg-teal-500/10 rounded-full blur-3xl pointer-events-none" />

        <div className="relative z-10 flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
          <div>
            <div className="flex items-center gap-2 mb-2">
              <span className="px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/20 text-emerald-600 dark:text-emerald-300 border border-emerald-500/30 flex items-center gap-1.5">
                <Compass className="w-3.5 h-3.5" />
                <span>بوصلة القبلة الذكية</span>
              </span>
              <span className="text-xs text-slate-500 dark:text-slate-400">
                مكة المكرمة • الكعبة المشرفة
              </span>
            </div>

            <h2 className="text-2xl sm:text-3xl font-bold bg-gradient-to-r from-emerald-600 via-teal-600 to-emerald-500 dark:from-emerald-300 dark:via-teal-200 dark:to-emerald-400 bg-clip-text text-transparent">
              اتجاه القبلة الشريفة
            </h2>
            <p className="mt-1 text-xs sm:text-sm text-slate-600 dark:text-slate-300 max-w-xl">
              تحديد المسار الدقيق للكعبة المشرفة بدقة فلكية عالية بالاعتماد على حساب الدائرة العظمى (Great Circle)، مع استشعار جيروسكوبي ومغناطيسي حي.
            </p>
          </div>

          {/* Quick city info button */}
          <div className="flex flex-wrap items-center gap-2">
            <button
              onClick={() => setShowLocationModal(true)}
              className={`flex items-center gap-2 px-4 py-2.5 rounded-2xl text-xs sm:text-sm font-medium border transition-all shadow-sm ${
                isDark
                  ? 'bg-slate-900/90 hover:bg-slate-800 border-emerald-700/50 text-slate-200'
                  : 'bg-white hover:bg-slate-50 border-emerald-200 text-slate-800'
              }`}
            >
              <MapPin className="w-4 h-4 text-emerald-500" />
              <span>{coordinates.city}</span>
              <span className="text-[11px] text-slate-400 font-mono">
                ({coordinates.latitude.toFixed(2)}°, {coordinates.longitude.toFixed(2)}°)
              </span>
            </button>

            <button
              onClick={() => setSoundEnabled(!soundEnabled)}
              className={`p-2.5 rounded-2xl border transition-colors ${
                soundEnabled
                  ? 'bg-emerald-500/20 text-emerald-600 dark:text-emerald-300 border-emerald-500/40'
                  : 'bg-slate-900/60 text-slate-400 border-slate-700'
              }`}
              title={soundEnabled ? 'تنبيه المحاذاة الصوتي مفعّل' : 'تنبيه المحاذاة صامت'}
              aria-label="تبديل صوت تنبيه المحاذاة"
            >
              {soundEnabled ? <Volume2 className="w-4 h-4" /> : <VolumeX className="w-4 h-4" />}
            </button>
          </div>
        </div>
      </div>

      {/* Main Interactive Compass Area */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left Column: Visual Compass Dial (7 cols) */}
        <div
          className={`lg:col-span-7 rounded-3xl p-6 sm:p-8 border shadow-xl flex flex-col items-center justify-center relative overflow-hidden ${
            isDark
              ? 'bg-slate-900/80 border-slate-800'
              : 'bg-white border-slate-200 shadow-slate-100'
          }`}
        >
          {/* Alignment status alert banner */}
          <div
            className={`w-full mb-3 py-2.5 px-4 rounded-2xl border text-center font-bold text-xs sm:text-sm flex items-center justify-center gap-2 transition-all duration-300 ${
              isAligned
                ? 'bg-emerald-500/20 text-emerald-700 dark:text-emerald-300 border-emerald-500/50 ring-2 ring-emerald-500/30 animate-pulse'
                : isDark
                ? 'bg-slate-800/60 text-slate-300 border-slate-700'
                : 'bg-slate-100 text-slate-700 border-slate-200'
            }`}
          >
            {isAligned ? (
              <>
                <CheckCircle2 className="w-5 h-5 text-emerald-500" />
                <span>أنت الآن باتجاه القبلة الشريفة تماماً 🕋 تقبّل الله طاعتكم!</span>
              </>
            ) : (
              <>
                <Crosshair className="w-4 h-4 text-emerald-500" />
                <span>
                  أدر جهازك أو قرص البوصلة بمقدار{' '}
                  <strong className="text-emerald-600 dark:text-emerald-400 font-mono">
                    {Math.abs(Math.round(angleDiff))}°
                  </strong>{' '}
                  {angleDiff > 0 ? 'نحو اليمين ⮞' : 'نحو اليسار ⮜'} للمحاذاة
                </span>
              </>
            )}
          </div>

          {/* تحذير المعايرة — يظهر لو قراءة البوصلة غير مرجعية بالشمال */}
          {!useManualMode && hasCompassSensor && !compassCalibrated && (
            <div className="w-full mb-4 py-2 px-4 rounded-2xl border border-amber-500/50 bg-amber-500/10 text-amber-700 dark:text-amber-300 text-[11px] leading-relaxed flex items-start gap-2 text-right">
              <Info className="w-4 h-4 shrink-0 mt-0.5" />
              <span>
                قراءة البوصلة قد تكون غير دقيقة على هذا الجهاز — حرّك هاتفك في الهواء على شكل رقم 8
                عدة مرات لمعايرة المستشعر، أو استخدم زر «نحو القبلة 🕋» أسفل القرص كمقياس يدوي
                (وجّه أعلى الجهاز نحو اتجاه القبلة بالسمت {Math.round(qiblaAngle)}° من الشمال).
              </span>
            </div>
          )}

          {/* Compass Dial Container */}
          <div className="relative w-72 h-72 sm:w-88 sm:h-88 my-4 flex items-center justify-center">
            {/* Outer Decorative Ring with Glow */}
            <div
              className={`absolute inset-0 rounded-full border-4 transition-all duration-500 ${
                isAligned
                  ? 'border-emerald-500 shadow-[0_0_50px_rgba(16,185,129,0.4)]'
                  : isDark
                  ? 'border-slate-800 shadow-[0_0_30px_rgba(0,0,0,0.5)]'
                  : 'border-slate-200 shadow-lg'
              }`}
            />

            {/* Subtle Inner Ring */}
            <div
              className={`absolute inset-4 rounded-full border border-dashed ${
                isDark ? 'border-slate-700/60' : 'border-slate-300'
              }`}
            />

            {/* Rotating Compass Dial Body */}
            <div
              className="absolute inset-2 rounded-full transition-transform duration-200 ease-out"
              style={{
                transform: `rotate(${-currentHeading}deg)`,
              }}
            >
              {/* Compass Dial Degree Marks (every 30 degrees) */}
              {[0, 30, 60, 90, 120, 150, 180, 210, 240, 270, 300, 330].map((deg) => (
                <div
                  key={deg}
                  className="absolute inset-0 flex items-start justify-center"
                  style={{ transform: `rotate(${deg}deg)` }}
                >
                  <div
                    className={`w-0.5 ${
                      deg % 90 === 0 ? 'h-4 bg-emerald-500 font-bold' : 'h-2 bg-slate-400 dark:bg-slate-600'
                    }`}
                  />
                  {deg % 90 === 0 && (
                    <span
                      className="absolute top-5 text-[11px] font-bold text-slate-600 dark:text-slate-300 select-none"
                      style={{ transform: `rotate(${-deg}deg)` }}
                    >
                      {deg === 0 && 'ش'}
                      {deg === 90 && 'ق'}
                      {deg === 180 && 'ج'}
                      {deg === 270 && 'غ'}
                    </span>
                  )}
                </div>
              ))}

              {/* Kaaba Direction Marker on the outer dial rim */}
              <div
                className="absolute inset-0 flex items-start justify-center"
                style={{ transform: `rotate(${qiblaAngle}deg)` }}
              >
                <div className="relative -top-2 flex flex-col items-center">
                  <div className="w-8 h-8 rounded-xl bg-gradient-to-b from-amber-400 to-amber-600 text-white flex items-center justify-center shadow-lg shadow-amber-900/50 ring-2 ring-amber-300 text-sm animate-bounce">
                    🕋
                  </div>
                  <span className="text-[10px] font-bold text-amber-500 bg-amber-500/10 px-1.5 py-0.5 rounded-full mt-1 border border-amber-500/30">
                    القبلة
                  </span>
                </div>
              </div>
            </div>

            {/* Fixed Device Indicator (Phone top heading direction) */}
            <div className="absolute top-0 flex flex-col items-center -mt-3 z-20">
              <div className="w-0 h-0 border-l-[7px] border-l-transparent border-r-[7px] border-r-transparent border-t-[12px] border-t-rose-500 drop-shadow-md" />
              <span className="text-[10px] font-bold text-rose-500 mt-0.5">مقدمة الجهاز</span>
            </div>

            {/* Center Pointer Needle pointing directly to Qibla relative to heading */}
            <div
              className="absolute inset-0 flex items-center justify-center pointer-events-none transition-transform duration-200 ease-out z-10"
              style={{
                transform: `rotate(${angleDiff}deg)`,
              }}
            >
              {/* North/Qibla Pointer Half */}
              <div className="relative flex flex-col items-center h-full justify-center">
                <div
                  className={`w-3 sm:w-3.5 h-24 sm:h-28 rounded-t-full transition-colors shadow-lg ${
                    isAligned
                      ? 'bg-gradient-to-t from-emerald-600 to-emerald-400 ring-2 ring-emerald-300'
                      : 'bg-gradient-to-t from-teal-700 to-emerald-500'
                  }`}
                  style={{
                    clipPath: 'polygon(50% 0%, 0% 100%, 100% 100%)',
                    marginBottom: '14px',
                  }}
                />

                {/* Needle Base Ring */}
                <div
                  className={`w-10 h-10 rounded-full flex items-center justify-center shadow-md ring-4 ${
                    isAligned
                      ? 'bg-emerald-500 ring-emerald-300/60 text-white'
                      : 'bg-slate-900 ring-emerald-500/30 text-emerald-400'
                  }`}
                >
                  <Sparkles className="w-4 h-4 animate-spin-slow" />
                </div>

                {/* South Pointer Half */}
                <div
                  className="w-3 sm:w-3.5 h-16 sm:h-20 rounded-b-full bg-gradient-to-b from-slate-700 to-slate-900 opacity-60"
                  style={{
                    clipPath: 'polygon(0% 0%, 100% 0%, 50% 100%)',
                    marginTop: '14px',
                  }}
                />
              </div>
            </div>

            {/* Center Degree Readout badge */}
            <div className="absolute z-20 flex flex-col items-center justify-center pointer-events-none mt-20">
              <div
                className={`px-2.5 py-1 rounded-full text-xs font-mono font-bold shadow-md border ${
                  isAligned
                    ? 'bg-emerald-600 text-white border-emerald-400'
                    : isDark
                    ? 'bg-slate-900/90 text-slate-200 border-slate-700'
                    : 'bg-white text-slate-800 border-slate-300'
                }`}
              >
                {Math.round(currentHeading)}°
              </div>
            </div>
          </div>

          {/* Compass Control Toolbar */}
          <div className="w-full mt-6 pt-4 border-t border-slate-800/40 flex flex-col sm:flex-row items-center justify-between gap-4">
            <div className="flex items-center gap-2">
              <button
                onClick={() => setUseManualMode(!useManualMode)}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-medium border transition-colors ${
                  useManualMode
                    ? 'bg-emerald-500/20 text-emerald-600 dark:text-emerald-300 border-emerald-500/40'
                    : 'bg-slate-800/40 text-slate-400 border-slate-700 hover:text-slate-200'
                }`}
              >
                <Sliders className="w-3.5 h-3.5" />
                <span>{useManualMode ? 'وضع التدوير اليدوي نشط' : 'التدوير التجريبي (للحاسوب)'}</span>
              </button>

              {typeof window !== 'undefined' &&
                'DeviceOrientationEvent' in window &&
                permissionState !== 'granted' && (
                  <button
                    onClick={requestOrientationPermission}
                    className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-amber-500/20 text-amber-700 dark:text-amber-300 border border-amber-500/30 text-xs font-medium hover:bg-amber-500/30 transition-colors"
                  >
                    <Smartphone className="w-3.5 h-3.5" />
                    <span>تفعيل حساس الهاتف</span>
                  </button>
                )}
            </div>

            {/* Quick angle calibration shortcuts */}
            <div className="flex items-center gap-1.5">
              <button
                onClick={() => {
                  setUseManualMode(true);
                  setManualRotation(0);
                }}
                className="px-2.5 py-1 rounded-lg bg-slate-800/40 hover:bg-slate-800 text-[11px] text-slate-400 hover:text-slate-200 border border-slate-700/50"
                title="توجيه الجهاز نحو الشمال (0°)"
              >
                شمال 0°
              </button>
              <button
                onClick={() => {
                  setUseManualMode(true);
                  setManualRotation(Math.round(qiblaAngle));
                }}
                className="px-2.5 py-1 rounded-lg bg-emerald-500/20 hover:bg-emerald-500/30 text-[11px] text-emerald-600 dark:text-emerald-300 border border-emerald-500/40 font-semibold"
                title="توجيه مباشر للقبلة"
              >
                نحو القبلة 🕋
              </button>
              <button
                onClick={() => {
                  setUseManualMode(true);
                  setManualRotation((prev) => (prev + 30) % 360);
                }}
                className="p-1 rounded-lg bg-slate-800/40 hover:bg-slate-800 text-slate-400 hover:text-slate-200 border border-slate-700/50"
                title="تدوير +30°"
              >
                <RotateCw className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>

          {/* Manual slider for desktop testing */}
          {useManualMode && (
            <div className="w-full mt-3 p-3 rounded-2xl bg-slate-800/30 border border-slate-700/40 flex items-center gap-3">
              <span className="text-[11px] text-slate-400 whitespace-nowrap">محاكاة تدوير الجهاز:</span>
              <input
                type="range"
                min="0"
                max="359"
                value={manualRotation}
                onChange={(e) => setManualRotation(parseInt(e.target.value, 10))}
                className="w-full accent-emerald-500 h-1.5 bg-slate-700 rounded-lg cursor-pointer"
              />
              <span className="text-xs font-mono font-bold text-emerald-500 w-10 text-left">
                {manualRotation}°
              </span>
            </div>
          )}
        </div>

        {/* Right Column: Qibla Astrometry & Information Cards (5 cols) */}
        <div className="lg:col-span-5 space-y-4">
          {/* Main Angle & Distance Card */}
          <div
            className={`rounded-3xl p-6 border shadow-xl ${
              isDark ? 'bg-slate-900/80 border-slate-800' : 'bg-white border-slate-200'
            }`}
          >
            <h3 className="text-sm font-bold text-slate-400 dark:text-slate-400 mb-4 flex items-center gap-2">
              <Crosshair className="w-4 h-4 text-emerald-500" />
              <span>إحداثيات وزاوية القبلة</span>
            </h3>

            <div className="grid grid-cols-2 gap-4">
              <div
                className={`p-4 rounded-2xl border ${
                  isDark ? 'bg-slate-950/60 border-slate-800' : 'bg-slate-50 border-slate-200'
                }`}
              >
                <span className="text-xs text-slate-500 dark:text-slate-400 block mb-1">
                  زاوية القبلة (السمت)
                </span>
                <div className="flex items-baseline gap-1">
                  <span className="text-3xl font-mono font-bold text-emerald-600 dark:text-emerald-400">
                    {qiblaAngle}°
                  </span>
                </div>
                <span className="text-[11px] text-slate-500 dark:text-slate-400 mt-1 block">
                  {getQiblaDirectionDescription(qiblaAngle)}
                </span>
              </div>

              <div
                className={`p-4 rounded-2xl border ${
                  isDark ? 'bg-slate-950/60 border-slate-800' : 'bg-slate-50 border-slate-200'
                }`}
              >
                <span className="text-xs text-slate-500 dark:text-slate-400 block mb-1">
                  المسافة إلى الكعبة
                </span>
                <div className="flex items-baseline gap-1">
                  <span className="text-3xl font-mono font-bold text-teal-600 dark:text-teal-400">
                    {distanceKm.toLocaleString('ar-EG')}
                  </span>
                  <span className="text-xs text-slate-400">كم</span>
                </div>
                <span className="text-[11px] text-slate-500 dark:text-slate-400 mt-1 block">
                  عبر المسار المباشر
                </span>
              </div>
            </div>

            {/* Geographical details */}
            <div className="mt-4 pt-4 border-t border-slate-800/40 space-y-2 text-xs">
              <div className="flex items-center justify-between">
                <span className="text-slate-500 dark:text-slate-400">موقعك الحالي:</span>
                <span className="font-semibold text-slate-800 dark:text-slate-200">
                  {coordinates.city}
                </span>
              </div>
              <div className="flex items-center justify-between font-mono">
                <span className="text-slate-500 dark:text-slate-400 font-sans">خط العرض والطول:</span>
                <span className="text-slate-700 dark:text-slate-300">
                  {coordinates.latitude.toFixed(4)}° N, {coordinates.longitude.toFixed(4)}° E
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-slate-500 dark:text-slate-400">موقع الكعبة المشرفة:</span>
                <span className="text-slate-700 dark:text-slate-300 font-mono">
                  21.4225° N, 39.8262° E
                </span>
              </div>
            </div>
          </div>

          {/* Tips for Compass Accuracy Card */}
          <div
            className={`rounded-3xl p-5 border text-xs leading-relaxed space-y-2.5 ${
              isDark
                ? 'bg-emerald-950/20 border-emerald-800/30 text-emerald-200/90'
                : 'bg-emerald-50/80 border-emerald-200 text-emerald-950'
            }`}
          >
            <div className="flex items-center gap-2 font-bold text-emerald-600 dark:text-emerald-400 text-sm">
              <Info className="w-4 h-4" />
              <span>إرشادات لدقة تحديد القبلة بالهاتف</span>
            </div>
            <ul className="list-disc list-inside space-y-1 text-slate-600 dark:text-emerald-200/80">
              <li>ضع الهاتف أفقياً تماماً على راحة يدك أو سطح مستوٍ.</li>
              <li>ابتعد عن الأجهزة الإلكترونية والمغناطيسية والمعادن الضخمة.</li>
              <li>إذا لاحظت عدم استقرار السهم، حرّك الهاتف في الهواء على شكل رقم (8) لمعايرة البوصلة.</li>
            </ul>
          </div>

          {/* Android Kotlin Native Code Snippet Preview — اتحذف بطلب المالك (خانات ملهاش لازمه) */}
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
    </div>
  );
};
