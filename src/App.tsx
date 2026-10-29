/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import React, { useState, useEffect, useCallback, useRef } from 'react';
import { App as CapApp } from '@capacitor/app';
import { Navbar, ActiveTab } from './components/Navbar';
import { PrayerTimesSection } from './components/PrayerTimesSection';
import { QuranSection } from './components/QuranSection';
import { QiblaCompassSection } from './components/QiblaCompassSection';
import { AdhkarSection } from './components/AdhkarSection';
import { TasbihSection } from './components/TasbihSection';
import { SettingsSection } from './components/SettingsSection';
import { SplashDua } from './components/SplashDua';
import { UpdateBanner } from './components/UpdateBanner';
import {
  Coordinates,
  getHijriDate,
  computePrayerTimes,
  getNextPrayer,
  CalculationMethod,
  PRAYER_NAMES_AR,
  PrayerTimeKey,
} from './utils/prayerMath';
import { APP_VERSION } from './utils/otaUpdate';
import {
  getSavedTheme,
  saveTheme,
  getSavedColorMode,
  saveColorMode,
  applyThemePalette,
  AppTheme,
  ColorMode,
} from './utils/themeData';
import {
  getSavedLocation,
  saveLocation,
  getSavedMethod,
  saveMethod,
  getSavedMadhab,
  saveMadhab,
  getSavedAdhanVoice,
  saveAdhanVoice,
  getSavedAdhanEnabled,
  saveAdhanEnabled,
  getSavedPrayerAlerts,
  savePrayerAlerts,
  getSavedQiyamEnabled,
  saveQiyamEnabled,
  getSavedQiyamOffsetMinutes,
  saveQiyamOffsetMinutes,
  getPermissionsAsked,
  savePermissionsAsked,
  PrayerAlerts,
} from './utils/appSettings';
import {
  scheduleAdhanAlarms,
  cancelAdhanAlarms,
  updateWidget,
  requestAllPermissions,
  isNativeApp,
  buildAlarmsFromAlerts,
} from './utils/nativeBridge';
import { initAndroidBack } from './utils/backButton';

const PRAYER_ORDER: PrayerTimeKey[] = ['fajr', 'sunrise', 'dhuhr', 'asr', 'maghrib', 'isha'];

export default function App() {
  const [currentTab, setCurrentTab] = useState<ActiveTab>('prayer');
  const [showSplash, setShowSplash] = useState<boolean>(true);

  // ===== الإعدادات المشتركة (المصدر الوحيد للحقيقة) =====
  const [coordinates, setCoordinates] = useState<Coordinates>(getSavedLocation);
  const [method, setMethod] = useState<CalculationMethod>(getSavedMethod);
  const [isHanafi, setIsHanafi] = useState<boolean>(() => getSavedMadhab() === 'hanafi');
  const [adhanVoiceId, setAdhanVoiceId] = useState<string>(() => getSavedAdhanVoice().id);
  const [adhanEnabled, setAdhanEnabled] = useState<boolean>(getSavedAdhanEnabled);
  const [prayerAlerts, setPrayerAlerts] = useState<PrayerAlerts>(getSavedPrayerAlerts);
  const [qiyamEnabled, setQiyamEnabled] = useState<boolean>(getSavedQiyamEnabled);
  const [qiyamOffsetMinutes, setQiyamOffsetMinutes] = useState<number>(getSavedQiyamOffsetMinutes);

  const [hijriDate, setHijriDate] = useState<string>('');

  // Theme & Color Mode State
  const [currentTheme, setCurrentTheme] = useState<AppTheme>(getSavedTheme);
  const [colorMode, setColorMode] = useState<ColorMode>(getSavedColorMode);

  // دقيقة حالية (لإعادة الجدولة عند تغير اليوم بدون نبضة كل ثانية)
  const [minuteTick, setMinuteTick] = useState<number>(() => Math.floor(Date.now() / 60000));
  useEffect(() => {
    const t = setInterval(
      () => setMinuteTick(Math.floor(Date.now() / 60000)),
      30 * 1000,
    );
    return () => clearInterval(t);
  }, []);

  useEffect(() => {
    const h = getHijriDate(new Date());
    setHijriDate(h.formatted);
  }, []);

  // الثيم الشامل: توليد طيف الألوان وحقنه في متغيرات CSS مع كل تغيير
  useEffect(() => {
    applyThemePalette(currentTheme, colorMode);
  }, [currentTheme, colorMode]);

  const handleSelectTheme = (theme: AppTheme) => {
    setCurrentTheme(theme);
    saveTheme(theme.id);
  };

  const handleToggleColorMode = (mode: ColorMode) => {
    setColorMode(mode);
    saveColorMode(mode);
  };

  useEffect(() => {
    const root = document.documentElement;
    if (colorMode === 'dark') root.classList.add('dark');
    else root.classList.remove('dark');
  }, [colorMode]);

  // ===== معالجات تغيير الإعدادات (تحفظ فوراً) =====
  const handleSetCoordinates = useCallback((coords: Coordinates) => {
    setCoordinates(coords);
    saveLocation(coords);
  }, []);

  const handleSetMethod = useCallback((m: CalculationMethod) => {
    setMethod(m);
    saveMethod(m);
  }, []);

  const handleSetIsHanafi = useCallback((h: boolean) => {
    setIsHanafi(h);
    saveMadhab(h ? 'hanafi' : 'shafi');
  }, []);

  const handleSetAdhanVoiceId = useCallback((id: string) => {
    setAdhanVoiceId(id);
    saveAdhanVoice(id);
  }, []);

  const handleSetAdhanEnabled = useCallback((on: boolean) => {
    setAdhanEnabled(on);
    saveAdhanEnabled(on);
  }, []);

  const handleSetPrayerAlerts = useCallback((alerts: PrayerAlerts) => {
    setPrayerAlerts(alerts);
    savePrayerAlerts(alerts);
  }, []);

  const handleSetQiyamEnabled = useCallback((on: boolean) => {
    setQiyamEnabled(on);
    saveQiyamEnabled(on);
  }, []);

  const handleSetQiyamOffset = useCallback((m: number) => {
    setQiyamOffsetMinutes(m);
    saveQiyamOffsetMinutes(m);
  }, []);

  // ===== أذونات أول تشغيل: تطلب تلقائياً بعد زر «متابعة» في السبلاش =====
  const permissionsRequestedRef = useRef(false);
  useEffect(() => {
    if (showSplash) return;
    if (permissionsRequestedRef.current) return;
    if (getPermissionsAsked()) {
      permissionsRequestedRef.current = true;
      return;
    }
    permissionsRequestedRef.current = true;
    savePermissionsAsked();
    // طلب كل الأذونات: الموقع للمواقيت + الإشعارات والمنبهات للأذان في الخلفية
    void requestAllPermissions().then(() => {
      // بعد منح الموقع: حدّث الموقع تلقائياً لو ما زال الافتراضي
      const saved = getSavedLocation();
      if (saved.city === 'القاهرة، مصر' && isNativeApp()) {
        navigator.geolocation?.getCurrentPosition(
          (pos) => {
            handleSetCoordinates({
              city: 'موقعي الحالي',
              latitude: pos.coords.latitude,
              longitude: pos.coords.longitude,
            });
          },
          () => {
            // المستخدم رفض الموقع أو غير متاح — يظل الموقع المختار يدوياً
          },
          { enableHighAccuracy: true, timeout: 12000, maximumAge: 60000 },
        );
      }
    });
  }, [showSplash, handleSetCoordinates]);

  // ===== جدولة الأذان النيتِفية + تحديث الويدجت عند تغير الإعدادات/اليوم =====
  const scheduleKey = `${coordinates.latitude},${coordinates.longitude}|${method}|${isHanafi}|${adhanVoiceId}|${adhanEnabled}|${JSON.stringify(prayerAlerts)}|${qiyamEnabled}|${qiyamOffsetMinutes}|${minuteTick}`;
  useEffect(() => {
    if (showSplash) return;
    const now = new Date();
    const tomorrow = new Date(now.getTime() + 24 * 3600 * 1000);

    const todayTimes = computePrayerTimes(
      now,
      coordinates.latitude,
      coordinates.longitude,
      method,
      isHanafi,
    );
    const tomorrowTimes = computePrayerTimes(
      tomorrow,
      coordinates.latitude,
      coordinates.longitude,
      method,
      isHanafi,
    );

    // الصلوات القادمة: المتبقي اليوم + صلوات الغد (لضمان الأذان حتى لو التطبيق ما فتحش)
    const upcoming: Array<{ key: string; name: string; ts: number }> = [];
    for (const key of PRAYER_ORDER) {
      const t = todayTimes.rawTimes[key].getTime();
      if (t > now.getTime()) {
        upcoming.push({ key, name: PRAYER_NAMES_AR[key], ts: t });
      }
    }
    for (const key of PRAYER_ORDER) {
      upcoming.push({ key, name: PRAYER_NAMES_AR[key], ts: tomorrowTimes.rawTimes[key].getTime() });
    }

    // منبه قيام الليل (اختياري): إشعار هادئ قبل الفجر
    if (qiyamEnabled) {
      const qiyamToday = todayTimes.rawTimes.fajr.getTime() - qiyamOffsetMinutes * 60000;
      const qiyamTomorrow =
        tomorrowTimes.rawTimes.fajr.getTime() - qiyamOffsetMinutes * 60000;
      const qiyamTs = qiyamToday > now.getTime() ? qiyamToday : qiyamTomorrow;
      upcoming.push({ key: 'qiyam', name: 'قيام الليل', ts: qiyamTs });
    }

    // جدولة منبهات الأذان + قيام الليل (تعمل والشاشة مطفية)
    if (adhanEnabled || qiyamEnabled) {
      const alarms = buildAlarmsFromAlerts(upcoming, prayerAlerts);
      void scheduleAdhanAlarms(alarms, adhanVoiceId, coordinates.city);
    } else {
      void cancelAdhanAlarms();
    }

    // تحديث ويدجت الشاشة الرئيسية ببيانات اليوم
    const next = getNextPrayer(todayTimes, now);
    void updateWidget({
      city: coordinates.city,
      hijri: hijriDate,
      times: Object.fromEntries(PRAYER_ORDER.map((k) => [k, todayTimes[k]])),
      order: PRAYER_ORDER.map((k) => ({
        key: k,
        name: PRAYER_NAMES_AR[k],
        time: todayTimes[k],
      })),
      next: {
        key: next.name,
        name: next.arabicName,
        ts: next.targetDate.getTime(),
        time: next.timeStr,
      },
      tzOffsetMinutes: -now.getTimezoneOffset(),
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [scheduleKey, showSplash]);

  // ===== زر الرجوع في أندرويد: منطق التبويبات =====
  useEffect(() => {
    initAndroidBack(() => {
      if (currentTab !== 'prayer') {
        setCurrentTab('prayer');
        return;
      }
      // أصلاً في الرئيسية — سلوك أندرويد الافتراضي: تصغير/خروج
      try {
        CapApp.exitApp();
      } catch {
        // ويب — لا شيء
      }
    });
  }, [currentTab]);

  const isDark = colorMode === 'dark';

  return (
    <div
      className={`min-h-screen flex flex-col font-arabic transition-colors duration-300 ${
        isDark
          ? `bg-gradient-to-br ${currentTheme.bgGradientDark} text-slate-100`
          : `bg-gradient-to-br ${currentTheme.bgGradientLight} text-slate-900`
      }`}
    >
      {/* شاشة السبلاش — دعاء لأبي المرحوم + الصدقة الجارية */}
      {showSplash && (
        <SplashDua
          onDone={() => setShowSplash(false)}
          currentTheme={currentTheme}
          colorMode={colorMode}
        />
      )}

      {/* الهيدر العلوي + شريط التنقل السفلي */}
      <Navbar
        currentTab={currentTab}
        setCurrentTab={setCurrentTab}
        hijriString={hijriDate}
        currentTheme={currentTheme}
        colorMode={colorMode}
        setColorMode={handleToggleColorMode}
      />

      {/* Main Container — padding سفلي عشان شريط التنقل الثابت */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6 pb-32">
        {currentTab === 'prayer' && (
          <PrayerTimesSection
            coordinates={coordinates}
            method={method}
            isHanafi={isHanafi}
          />
        )}

        {currentTab === 'quran' && <QuranSection />}

        {currentTab === 'qibla' && (
          <QiblaCompassSection
            coordinates={coordinates}
            theme={currentTheme}
            colorMode={colorMode}
          />
        )}

        {currentTab === 'adhkar' && <AdhkarSection />}

        {currentTab === 'tasbih' && <TasbihSection />}

        {currentTab === 'settings' && (
          <SettingsSection
            currentTheme={currentTheme}
            setCurrentTheme={handleSelectTheme}
            colorMode={colorMode}
            setColorMode={handleToggleColorMode}
            coordinates={coordinates}
            setCoordinates={handleSetCoordinates}
            method={method}
            setMethod={handleSetMethod}
            isHanafi={isHanafi}
            setIsHanafi={handleSetIsHanafi}
            adhanVoiceId={adhanVoiceId}
            setAdhanVoiceId={handleSetAdhanVoiceId}
            adhanEnabled={adhanEnabled}
            setAdhanEnabled={handleSetAdhanEnabled}
            prayerAlerts={prayerAlerts}
            setPrayerAlerts={handleSetPrayerAlerts}
            qiyamEnabled={qiyamEnabled}
            setQiyamEnabled={handleSetQiyamEnabled}
            qiyamOffsetMinutes={qiyamOffsetMinutes}
            setQiyamOffsetMinutes={handleSetQiyamOffset}
          />
        )}
      </main>

      {/* بانر التحديث الهوائي (OTA) */}
      <UpdateBanner />

      {/* Footer — الصدقة الجارية + اسم المطور */}
      <footer
        className="border-t py-5 text-center text-xs transition-colors"
        style={{
          borderColor: isDark
            ? 'color-mix(in srgb, var(--noor-p-500) 18%, transparent)'
            : 'color-mix(in srgb, var(--noor-p-500) 14%, transparent)',
          color: 'var(--noor-n-500)',
        }}
      >
        <div className="max-w-7xl mx-auto px-4 space-y-1.5">
          <p className="font-bold" style={{ color: currentTheme.accentColor }}>
            هذا التطبيق صدقة جارية على روح المرحوم بأذن الله أبي
          </p>
          <div className="flex items-center justify-center gap-2 text-[11px]">
            <span
              className="w-2 h-2 rounded-full inline-block"
              style={{ backgroundColor: currentTheme.primaryColor }}
            />
            <span>نور المسلم • تطوير: Yousef Sawan</span>
            <span className="opacity-50">•</span>
            <span>إصدار {APP_VERSION}</span>
            <span className="opacity-50">•</span>
            <span>{hijriDate}</span>
          </div>
        </div>
      </footer>
    </div>
  );
}
