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
  isRamadanNow,
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
  getSavedQiyamMode,
  saveQiyamMode,
  getSavedQiyamOffsetMinutes,
  saveQiyamOffsetMinutes,
  getSavedSuhoorEnabled,
  saveSuhoorEnabled,
  getSavedSuhoorOffsetMinutes,
  saveSuhoorOffsetMinutes,
  getSavedIqamahEnabled,
  saveIqamahEnabled,
  getSavedIqamahOffsetMinutes,
  saveIqamahOffsetMinutes,
  getSavedIqamahVoice,
  saveIqamahVoice,
  getSavedMuteEnabled,
  saveMuteEnabled,
  getSavedMuteMinutes,
  saveMuteMinutes,
  getSavedSalawatEnabled,
  saveSalawatEnabled,
  getSavedSalawatIntervalMinutes,
  saveSalawatIntervalMinutes,
  getSavedWidgetSettings,
  saveWidgetSettings,
  WidgetSettings,
  PrayerAlerts,
  QiyamMode,
} from './utils/appSettings';
import {
  scheduleAdhanAlarms,
  cancelAdhanAlarms,
  scheduleSalawatAlarms,
  cancelSalawatAlarms,
  updateWidget,
  requestAllPermissions,
  isNativeApp,
  buildAlarmsFromAlerts,
} from './utils/nativeBridge';
import { reverseGeocodeCity } from './utils/geo';
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
  // قيام الليل: اختياري — قبل الفجر أو بعد العشاء (بطلب المالك)
  const [qiyamEnabled, setQiyamEnabled] = useState<boolean>(getSavedQiyamEnabled);
  const [qiyamMode, setQiyamMode] = useState<QiyamMode>(getSavedQiyamMode);
  const [qiyamOffsetMinutes, setQiyamOffsetMinutes] = useState<number>(getSavedQiyamOffsetMinutes);
  // السحور: رمضان فقط + تنبيه اختياري افتراضه قبل الفجر بـ15 دقيقة
  const [suhoorEnabled, setSuhoorEnabled] = useState<boolean>(getSavedSuhoorEnabled);
  const [suhoorOffsetMinutes, setSuhoorOffsetMinutes] = useState<number>(getSavedSuhoorOffsetMinutes);
  // الإقامة: تنبيه صوتي بعد الأذان بمدة محددة بصوت قابل للتغيير
  const [iqamahEnabled, setIqamahEnabled] = useState<boolean>(getSavedIqamahEnabled);
  const [iqamahOffsetMinutes, setIqamahOffsetMinutes] = useState<number>(getSavedIqamahOffsetMinutes);
  const [iqamahVoiceId, setIqamahVoiceId] = useState<string>(() => getSavedIqamahVoice().id);
  // كتم كل أصوات الهاتف مع الأذان حتى بعد الإقامة بمدة محددة
  const [muteEnabled, setMuteEnabled] = useState<boolean>(getSavedMuteEnabled);
  const [muteMinutes, setMuteMinutes] = useState<number>(getSavedMuteMinutes);
  // تذكير دوري بصوت الصلاة على النبي ﷺ
  const [salawatEnabled, setSalawatEnabled] = useState<boolean>(getSavedSalawatEnabled);
  const [salawatIntervalMinutes, setSalawatIntervalMinutes] = useState<number>(getSavedSalawatIntervalMinutes);
  // تخصيص كامل للويدجت: اللون والحجم والعناصر والشفافية ونظام الوقت
  const [widgetSettings, setWidgetSettings] = useState<WidgetSettings>(getSavedWidgetSettings);

  const [hijriDate, setHijriDate] = useState<string>('');
  // رمضان المبارك: السحور يعمل في رمضان فقط (بطلب المالك)
  const [isRamadan, setIsRamadan] = useState<boolean>(() => isRamadanNow());

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
    setIsRamadan(isRamadanNow());
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

  const handleSetQiyamMode = useCallback((m: QiyamMode) => {
    setQiyamMode(m);
    saveQiyamMode(m);
  }, []);

  const handleSetQiyamOffset = useCallback((m: number) => {
    setQiyamOffsetMinutes(m);
    saveQiyamOffsetMinutes(m);
  }, []);

  const handleSetSuhoorEnabled = useCallback((on: boolean) => {
    setSuhoorEnabled(on);
    saveSuhoorEnabled(on);
  }, []);

  const handleSetSuhoorOffset = useCallback((m: number) => {
    setSuhoorOffsetMinutes(m);
    saveSuhoorOffsetMinutes(m);
  }, []);

  const handleSetIqamahEnabled = useCallback((on: boolean) => {
    setIqamahEnabled(on);
    saveIqamahEnabled(on);
  }, []);

  const handleSetIqamahOffset = useCallback((m: number) => {
    setIqamahOffsetMinutes(m);
    saveIqamahOffsetMinutes(m);
  }, []);

  const handleSetIqamahVoiceId = useCallback((id: string) => {
    setIqamahVoiceId(id);
    saveIqamahVoice(id);
  }, []);

  const handleSetMuteEnabled = useCallback((on: boolean) => {
    setMuteEnabled(on);
    saveMuteEnabled(on);
  }, []);

  const handleSetMuteMinutes = useCallback((m: number) => {
    setMuteMinutes(m);
    saveMuteMinutes(m);
  }, []);

  const handleSetSalawatEnabled = useCallback((on: boolean) => {
    setSalawatEnabled(on);
    saveSalawatEnabled(on);
  }, []);

  const handleSetSalawatInterval = useCallback((m: number) => {
    setSalawatIntervalMinutes(m);
    saveSalawatIntervalMinutes(m);
  }, []);

  const handleSetWidgetSettings = useCallback((s: WidgetSettings) => {
    setWidgetSettings(s);
    saveWidgetSettings(s);
  }, []);

  // ===== أذونات الإقلاع: تُطلب في كل تشغيل بعد زر «متابعة» في السبلاش =====
  // بلا قفل «مرة واحدة»: الجانب الأصلي يتخطى الممنوح تلقائياً والنظام لا يُظهر
  // الحوار إلا للناقص — فالطلب صامت حين يكتمل كل شيء. القفل القديم كان يمنع
  // الطلب للأبد لو سُجل مفتاحه قبل ظهور الحوار (أو جاء التحديث فوق نسخة قديمة)
  // — إصلاح «التطبيق لا يطلب الأذونات».
  const permissionsRequestedRef = useRef(false);
  useEffect(() => {
    if (showSplash) return;
    if (permissionsRequestedRef.current) return;
    permissionsRequestedRef.current = true;
    // طلب كل الأذونات: الموقع للمواقيت + الإشعارات والمنبهات للأذان في الخلفية
    void requestAllPermissions().then(() => {
      // بعد منح الموقع: حدّث الموقع تلقائياً + اجلب اسم المدينة (إصلاح «المدينة لا تظهر»)
      const saved = getSavedLocation();
      if (saved.city === 'القاهرة، مصر' && isNativeApp()) {
        navigator.geolocation?.getCurrentPosition(
          (pos) => {
            const lat = pos.coords.latitude;
            const lng = pos.coords.longitude;
            handleSetCoordinates({ city: 'موقعي الحالي…', latitude: lat, longitude: lng });
            void reverseGeocodeCity(lat, lng).then((geo) => {
              handleSetCoordinates({
                city: geo.country && !geo.city.includes(geo.country) ? `${geo.city}، ${geo.country}` : geo.city,
                latitude: lat,
                longitude: lng,
              });
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

  // ===== جدولة الأذان/الإقامة/السحور/القيام/الكتم + الويدجت عند تغير الإعدادات/اليوم =====
  const scheduleKey = `${coordinates.latitude},${coordinates.longitude}|${coordinates.city}|${method}|${isHanafi}|${adhanVoiceId}|${adhanEnabled}|${JSON.stringify(prayerAlerts)}|${qiyamEnabled}|${qiyamMode}|${qiyamOffsetMinutes}|${suhoorEnabled}|${suhoorOffsetMinutes}|${iqamahEnabled}|${iqamahOffsetMinutes}|${iqamahVoiceId}|${muteEnabled}|${muteMinutes}|${isRamadan}|${JSON.stringify(widgetSettings)}|${minuteTick}`;
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

    const nowMs = now.getTime();

    // الصلوات القادمة: المتبقي اليوم + صلوات الغد (لضمان الأذان حتى لو التطبيق ما فتحش)
    const upcoming: Array<{ key: string; name: string; ts: number }> = [];
    for (const key of PRAYER_ORDER) {
      const t = todayTimes.rawTimes[key].getTime();
      if (t > nowMs) {
        upcoming.push({ key, name: PRAYER_NAMES_AR[key], ts: t });
      }
    }
    for (const key of PRAYER_ORDER) {
      upcoming.push({ key, name: PRAYER_NAMES_AR[key], ts: tomorrowTimes.rawTimes[key].getTime() });
    }

    // تنبيه قيام الليل (اختياري): قبل الفجر أو بعد العشاء — بطلب المالك
    if (qiyamEnabled) {
      const qiyamOf = (base: ReturnType<typeof computePrayerTimes>) =>
        qiyamMode === 'before_fajr'
          ? base.rawTimes.fajr.getTime() - qiyamOffsetMinutes * 60000
          : base.rawTimes.isha.getTime() + qiyamOffsetMinutes * 60000;
      const qiyamToday = qiyamOf(todayTimes);
      const qiyamTomorrow = qiyamOf(tomorrowTimes);
      const qiyamTs = qiyamToday > nowMs ? qiyamToday : qiyamTomorrow;
      upcoming.push({ key: 'qiyam', name: 'قيام الليل', ts: qiyamTs });
    }

    // تنبيه السحور (اختياري — رمضان فقط): قبل أذان الفجر بالمدة المحددة (افتراضي 15)
    if (isRamadan && suhoorEnabled) {
      const suhoorToday = todayTimes.rawTimes.fajr.getTime() - suhoorOffsetMinutes * 60000;
      const suhoorTomorrow = tomorrowTimes.rawTimes.fajr.getTime() - suhoorOffsetMinutes * 60000;
      const suhoorTs = suhoorToday > nowMs ? suhoorToday : suhoorTomorrow;
      upcoming.push({ key: 'suhoor', name: 'السحور', ts: suhoorTs });
    }

    // جدولة منبهات الأذان + الإقامة + السحور + القيام + خطة الكتم (تعمل والشاشة مطفية)
    if (adhanEnabled || qiyamEnabled || (isRamadan && suhoorEnabled)) {
      const alarms = buildAlarmsFromAlerts(upcoming, prayerAlerts);
      void scheduleAdhanAlarms(alarms, adhanVoiceId, coordinates.city, {
        iqamahEnabled,
        iqamahOffsetMinutes,
        iqamahVoice: iqamahVoiceId,
        muteEnabled,
        muteMinutes,
      });
    } else {
      void cancelAdhanAlarms();
    }

    // تذكير الصلاة على النبي ﷺ الدوري (اختياري قابل للإلغاء)
    if (salawatEnabled) {
      void scheduleSalawatAlarms(salawatIntervalMinutes);
    } else {
      void cancelSalawatAlarms();
    }

    // تحديث ويدجت الشاشة الرئيسية + حفظ الموقع/الطريقة لتحسب الويدجت مواقيتها بنفسها
    const next = getNextPrayer(todayTimes, now);
    void updateWidget(
      {
        city: coordinates.city,
        hijri: hijriDate,
        latitude: coordinates.latitude,
        longitude: coordinates.longitude,
        method,
        hanafi: isHanafi,
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
      },
      widgetSettings,
    );
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
      {/* شاشة السبلاش — دعاء لأبي المتوفي + الصدقة الجارية */}
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
            qiyamMode={qiyamMode}
            setQiyamMode={handleSetQiyamMode}
            qiyamOffsetMinutes={qiyamOffsetMinutes}
            setQiyamOffsetMinutes={handleSetQiyamOffset}
            suhoorEnabled={suhoorEnabled}
            setSuhoorEnabled={handleSetSuhoorEnabled}
            suhoorOffsetMinutes={suhoorOffsetMinutes}
            setSuhoorOffsetMinutes={handleSetSuhoorOffset}
            iqamahEnabled={iqamahEnabled}
            setIqamahEnabled={handleSetIqamahEnabled}
            iqamahOffsetMinutes={iqamahOffsetMinutes}
            setIqamahOffsetMinutes={handleSetIqamahOffset}
            iqamahVoiceId={iqamahVoiceId}
            setIqamahVoiceId={handleSetIqamahVoiceId}
            muteEnabled={muteEnabled}
            setMuteEnabled={handleSetMuteEnabled}
            muteMinutes={muteMinutes}
            setMuteMinutes={handleSetMuteMinutes}
            salawatEnabled={salawatEnabled}
            setSalawatEnabled={handleSetSalawatEnabled}
            salawatIntervalMinutes={salawatIntervalMinutes}
            setSalawatIntervalMinutes={handleSetSalawatInterval}
            widgetSettings={widgetSettings}
            setWidgetSettings={handleSetWidgetSettings}
            isRamadan={isRamadan}
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
            هذا التطبيق صدقة جارية على روح المتوفي بأذن الله أبي
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
