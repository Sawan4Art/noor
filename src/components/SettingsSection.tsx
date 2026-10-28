import React, { useState, useEffect, useRef, useCallback } from 'react';
import {
  Palette,
  Sun,
  Moon,
  BookOpen,
  Mic2,
  Download,
  CheckCircle,
  Search,
  Clock,
  RefreshCw,
  ExternalLink,
  MessageSquareHeart,
  Smartphone,
  Info,
  Trash2,
  X,
  Check,
  HeartHandshake,
  Send,
  Copy as CopyIcon,
  ChevronRight,
  MapPin,
  Bell,
  BellRing,
  Volume2,
  Crosshair,
  ShieldCheck,
  TriangleAlert,
  Play,
  Square,
  Layers,
  Wifi,
  CircleCheck,
} from 'lucide-react';
import { APP_THEMES, AppTheme, ColorMode } from '../utils/themeData';
import {
  MUSHAF_EDITIONS,
  MushafEdition,
  saveEdition,
  getSavedPrayerAdjustments,
  savePrayerAdjustments,
  ADJUSTABLE_PRAYERS,
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
  ADHAN_VOICES,
  getAdhanAudioSrc,
  PrayerAlerts,
  PrayerAlertMode,
} from '../utils/appSettings';
import { AYAH_RECITERS, getAyahAudioUrl } from '../utils/quranTajweedData';
import {
  downloadSurahForOffline,
  removeSurahFromOffline,
  isSurahOffline,
} from '../utils/ayahAudioCache';
import { SURAHS_LIST } from '../utils/quranData';
import {
  APP_VERSION,
  RELEASES_PAGE_URL,
  fetchLatestRelease,
  detectApkEnvironment,
} from '../utils/otaUpdate';
import {
  Coordinates,
  CalculationMethod,
  CALCULATION_METHODS,
  PRAYER_NAMES_AR,
  PrayerTimeAdjustments,
  computePrayerTimes,
  getNextPrayer,
} from '../utils/prayerMath';
import { LocationPickerModal } from './LocationPickerModal';
import {
  requestAllPermissions,
  updateWidget as pushWidgetUpdate,
  isNativeApp,
} from '../utils/nativeBridge';
import { registerBackHandler } from '../utils/backButton';

/**
 * قسم الإعدادات — بطلب المالك (2026-10، الدفعة الثالثة):
 * شبكة أيقونات، كل أيقونة تفتح صفحة مستقلة لقسمها (بدل صفحة واحدة طويلة):
 * الموقع • المواقيت • الأذان • المصحف • القارئ • المظهر • الويدجت •
 * التحديثات • الشكاوى • عن التطبيق.
 */

type SettingsPage =
  | 'home'
  | 'location'
  | 'times'
  | 'adhan'
  | 'mushaf'
  | 'reciter'
  | 'appearance'
  | 'widgets'
  | 'updates'
  | 'feedback'
  | 'about';

interface SettingsSectionProps {
  currentTheme: AppTheme;
  setCurrentTheme: (theme: AppTheme) => void;
  colorMode: ColorMode;
  setColorMode: (mode: ColorMode) => void;
  coordinates: Coordinates;
  setCoordinates: (coords: Coordinates) => void;
  method: CalculationMethod;
  setMethod: (m: CalculationMethod) => void;
  isHanafi: boolean;
  setIsHanafi: (h: boolean) => void;
  adhanVoiceId: string;
  setAdhanVoiceId: (id: string) => void;
  adhanEnabled: boolean;
  setAdhanEnabled: (on: boolean) => void;
  prayerAlerts: PrayerAlerts;
  setPrayerAlerts: (alerts: PrayerAlerts) => void;
}

/** رأس صفحة فرعية مع زر رجوع */
const PageHeader: React.FC<{
  title: string;
  subtitle?: string;
  onBack: () => void;
  icon: React.ReactNode;
}> = ({ title, subtitle, onBack, icon }) => (
  <div className="flex items-center gap-3 mb-5">
    <button
      onClick={onBack}
      className="p-2.5 rounded-2xl border transition-colors hover:bg-white/10 shrink-0"
      style={{
        borderColor: 'color-mix(in srgb, var(--noor-p-500) 30%, transparent)',
        color: 'var(--noor-n-200)',
      }}
      title="رجوع"
      aria-label="رجوع"
    >
      <ChevronRight className="w-5 h-5" />
    </button>
    <div
      className="w-11 h-11 rounded-2xl flex items-center justify-center text-white shrink-0"
      style={{
        background: 'linear-gradient(135deg, var(--noor-p-500), var(--noor-a-500))',
      }}
    >
      {icon}
    </div>
    <div className="min-w-0">
      <h2 className="text-lg font-extrabold" style={{ color: 'var(--noor-n-50)' }}>
        {title}
      </h2>
      {subtitle && (
        <p className="text-[11px]" style={{ color: 'var(--noor-n-400)' }}>
          {subtitle}
        </p>
      )}
    </div>
  </div>
);

/** بطاقة قسم داخل الصفحة الفرعية */
const Card: React.FC<{
  title: string;
  children: React.ReactNode;
}> = ({ title, children }) => (
  <div
    className="rounded-3xl border p-4 sm:p-5 space-y-3"
    style={{
      borderColor: 'color-mix(in srgb, var(--noor-p-500) 22%, transparent)',
      backgroundColor: 'color-mix(in srgb, var(--noor-n-950) 55%, transparent)',
    }}
  >
    <h3 className="text-sm font-bold" style={{ color: 'var(--noor-n-100)' }}>
      {title}
    </h3>
    {children}
  </div>
);

export const SettingsSection: React.FC<SettingsSectionProps> = (props) => {
  const [page, setPage] = useState<SettingsPage>('home');
  const [showLocationModal, setShowLocationModal] = useState<boolean>(false);

  // زر الرجوع في أندرويد يرجع من الصفحة الفرعية للرئيسية
  useEffect(() => {
    if (page === 'home') return;
    return registerBackHandler(() => {
      setPage('home');
      return true;
    });
  }, [page]);

  const goHome = () => setPage('home');

  return (
    <div className="max-w-3xl mx-auto space-y-5 animate-in fade-in duration-300">
      {page === 'home' && <SettingsHome onOpen={setPage} {...props} />}
      {page === 'location' && (
        <LocationPage
          onBack={goHome}
          coordinates={props.coordinates}
          setCoordinates={props.setCoordinates}
        />
      )}
      {page === 'times' && (
        <TimesPage
          onBack={goHome}
          coordinates={props.coordinates}
          method={props.method}
          setMethod={props.setMethod}
          isHanafi={props.isHanafi}
          setIsHanafi={props.setIsHanafi}
        />
      )}
      {page === 'adhan' && (
        <AdhanPage
          onBack={goHome}
          adhanVoiceId={props.adhanVoiceId}
          setAdhanVoiceId={props.setAdhanVoiceId}
          adhanEnabled={props.adhanEnabled}
          setAdhanEnabled={props.setAdhanEnabled}
          prayerAlerts={props.prayerAlerts}
          setPrayerAlerts={props.setPrayerAlerts}
        />
      )}
      {page === 'mushaf' && <MushafPage onBack={goHome} />}
      {page === 'reciter' && <ReciterPage onBack={goHome} />}
      {page === 'appearance' && (
        <AppearancePage
          onBack={goHome}
          currentTheme={props.currentTheme}
          setCurrentTheme={props.setCurrentTheme}
          colorMode={props.colorMode}
          setColorMode={props.setColorMode}
        />
      )}
      {page === 'widgets' && <WidgetsPage onBack={goHome} coordinates={props.coordinates} />}
      {page === 'updates' && <UpdatesPage onBack={goHome} />}
      {page === 'feedback' && <FeedbackPage onBack={goHome} />}
      {page === 'about' && <AboutPage onBack={goHome} />}

      {/* مودال اختيار الموقع (يُستخدم من صفحة الموقع) */}
      {showLocationModal && (
        <LocationPickerModal
          isOpen={showLocationModal}
          onClose={() => setShowLocationModal(false)}
          currentCoords={getSavedLocation()}
          onSelectCoords={(coords) => {
            props.setCoordinates(coords);
            saveLocation(coords);
            setShowLocationModal(false);
          }}
        />
      )}
    </div>
  );
};

/* ============================================================================
 * الصفحة الرئيسية للإعدادات — شبكة أيقونات
 * ========================================================================== */
const SettingsHome: React.FC<
  SettingsSectionProps & { onOpen: (page: SettingsPage) => void }
> = ({ onOpen, coordinates, prayerAlerts, adhanEnabled, colorMode, currentTheme }) => {
  const items: Array<{
    key: SettingsPage;
    label: string;
    desc: string;
    icon: React.ReactNode;
    hint?: string;
  }> = [
    {
      key: 'location',
      label: 'الموقع',
      desc: 'المدينة وتحديد الموقع تلقائياً',
      icon: <MapPin className="w-6 h-6" />,
      hint: coordinates.city,
    },
    {
      key: 'times',
      label: 'المواقيت',
      desc: 'طريقة الحساب والمذهب والضبط الدقيق',
      icon: <Clock className="w-6 h-6" />,
    },
    {
      key: 'adhan',
      label: 'الأذان',
      desc: 'الأصوات والتنبيهات والأذونات',
      icon: <Bell className="w-6 h-6" />,
      hint: adhanEnabled
        ? Object.values(prayerAlerts).filter((m) => m === 'adhan').length + ' أذان صوتي'
        : 'مطفأ',
    },
    {
      key: 'mushaf',
      label: 'المصحف',
      desc: 'الطبعات وألوان التجويد',
      icon: <BookOpen className="w-6 h-6" />,
    },
    {
      key: 'reciter',
      label: 'القارئ والتلاوات',
      desc: 'اختيار القارئ وتنزيل السور للاستماع أوفلاين',
      icon: <Mic2 className="w-6 h-6" />,
    },
    {
      key: 'appearance',
      label: 'المظهر',
      desc: 'ألوان التطبيق ووضع الليل والنهار',
      icon: <Palette className="w-6 h-6" />,
      hint: colorMode === 'dark' ? 'ليلي' : 'نهاري',
    },
    {
      key: 'widgets',
      label: 'الويدجت',
      desc: 'مواقيت الصلاة على شاشة الهاتف الرئيسية',
      icon: <Smartphone className="w-6 h-6" />,
    },
    {
      key: 'updates',
      label: 'التحديثات',
      desc: 'فحص وجود نسخة جديدة',
      icon: <RefreshCw className="w-6 h-6" />,
      hint: `v${APP_VERSION}`,
    },
    {
      key: 'feedback',
      label: 'الشكاوى والمقترحات',
      desc: 'صوتك يطور التطبيق',
      icon: <MessageSquareHeart className="w-6 h-6" />,
    },
    {
      key: 'about',
      label: 'عن التطبيق',
      desc: 'الصدقة الجارية والمطور',
      icon: <Info className="w-6 h-6" />,
    },
  ];

  return (
    <>
      <div className="text-center mb-5">
        <h1 className="text-2xl font-extrabold" style={{ color: 'var(--noor-n-50)' }}>
          الإعدادات
        </h1>
        <p className="text-xs mt-1" style={{ color: 'var(--noor-n-400)' }}>
          اضغط على أي قسم للدخول إليه
        </p>
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
        {items.map((item) => (
          <button
            key={item.key}
            onClick={() => onOpen(item.key)}
            className="group text-right p-4 rounded-3xl border transition-all hover:-translate-y-0.5 hover:shadow-xl space-y-2.5"
            style={{
              borderColor: 'color-mix(in srgb, var(--noor-p-500) 22%, transparent)',
              backgroundColor: 'color-mix(in srgb, var(--noor-n-950) 60%, transparent)',
            }}
          >
            <div
              className="w-11 h-11 rounded-2xl flex items-center justify-center text-white shadow-lg transition-transform group-hover:scale-105"
              style={{
                background: `linear-gradient(135deg, var(--noor-p-500), var(--noor-a-500))`,
              }}
            >
              {item.icon}
            </div>
            <div>
              <div className="flex items-center gap-1.5">
                <span className="text-sm font-extrabold" style={{ color: 'var(--noor-n-50)' }}>
                  {item.label}
                </span>
              </div>
              <p className="text-[10px] leading-relaxed mt-0.5" style={{ color: 'var(--noor-n-400)' }}>
                {item.desc}
              </p>
              {item.hint && (
                <p
                  className="text-[10px] font-bold mt-1 px-2 py-0.5 rounded-full inline-block"
                  style={{
                    color: currentTheme.accentColor,
                    backgroundColor: `color-mix(in srgb, ${currentTheme.accentColor} 12%, transparent)`,
                  }}
                >
                  {item.hint}
                </p>
              )}
            </div>
          </button>
        ))}
      </div>
    </>
  );
};

/* ============================================================================
 * صفحة الموقع
 * ========================================================================== */
const LocationPage: React.FC<{
  onBack: () => void;
  coordinates: Coordinates;
  setCoordinates: (c: Coordinates) => void;
}> = ({ onBack, coordinates, setCoordinates }) => {
  const [showPicker, setShowPicker] = useState(false);
  const [gpsLoading, setGpsLoading] = useState(false);
  const [gpsError, setGpsError] = useState<string | null>(null);

  useEffect(() => {
    if (!showPicker) return;
    return registerBackHandler(() => {
      setShowPicker(false);
      return true;
    });
  }, [showPicker]);

  const handleGPS = async () => {
    setGpsLoading(true);
    setGpsError(null);
    try {
      // نطلب الأذونات النيتِفية أولاً (أندرويد) ثم نستخدم تحديد الموقع
      await requestAllPermissions();
      const pos = await new Promise<GeolocationPosition>((resolve, reject) => {
        navigator.geolocation.getCurrentPosition(resolve, reject, {
          enableHighAccuracy: true,
          timeout: 15000,
          maximumAge: 60000,
        });
      });
      const coords: Coordinates = {
        city: 'موقعي الحالي',
        latitude: pos.coords.latitude,
        longitude: pos.coords.longitude,
      };
      setCoordinates(coords);
      saveLocation(coords);
    } catch (e) {
      setGpsError(
        e instanceof Error && e.message
          ? `تعذر تحديد الموقع: ${e.message}`
          : 'تعذر تحديد الموقع — تأكد من تفعيل خدمة الموقع وأذوناتها',
      );
    } finally {
      setGpsLoading(false);
    }
  };

  return (
    <div>
      <PageHeader
        title="الموقع"
        subtitle="يُستخدم لحساب المواقيت والقبلة"
        onBack={onBack}
        icon={<MapPin className="w-5 h-5" />}
      />

      <div className="space-y-4">
        {/* الموقع الحالي */}
        <div
          className="rounded-3xl border p-5 flex items-center gap-4"
          style={{
            borderColor: 'color-mix(in srgb, var(--noor-p-500) 22%, transparent)',
            backgroundColor: 'color-mix(in srgb, var(--noor-n-950) 55%, transparent)',
          }}
        >
          <div
            className="w-12 h-12 rounded-2xl flex items-center justify-center text-white shrink-0"
            style={{ background: 'linear-gradient(135deg, var(--noor-p-500), var(--noor-a-500))' }}
          >
            <MapPin className="w-6 h-6" />
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-sm font-extrabold" style={{ color: 'var(--noor-n-50)' }}>
              {coordinates.city}
            </p>
            <p className="text-[11px] font-mono" style={{ color: 'var(--noor-n-400)' }} dir="ltr">
              {coordinates.latitude.toFixed(4)}°, {coordinates.longitude.toFixed(4)}°
            </p>
          </div>
        </div>

        <button
          onClick={handleGPS}
          disabled={gpsLoading}
          className="w-full flex items-center justify-center gap-2 py-3.5 rounded-2xl font-bold text-sm text-white shadow-lg disabled:opacity-60"
          style={{ background: 'linear-gradient(135deg, var(--noor-p-500), var(--noor-a-500))' }}
        >
          <Crosshair className={`w-5 h-5 ${gpsLoading ? 'animate-spin' : ''}`} />
          <span>{gpsLoading ? 'جاري تحديد موقعك…' : 'تحديد موقعي تلقائياً (GPS)'}</span>
        </button>

        {gpsError && (
          <div
            className="flex items-start gap-2 p-3.5 rounded-2xl border text-xs"
            style={{ borderColor: 'rgba(244,63,94,0.3)', backgroundColor: 'rgba(244,63,94,0.08)', color: '#fda4af' }}
          >
            <TriangleAlert className="w-4 h-4 shrink-0 mt-0.5" />
            <span>{gpsError}</span>
          </div>
        )}

        <button
          onClick={() => setShowPicker(true)}
          className="w-full flex items-center justify-center gap-2 py-3.5 rounded-2xl font-bold text-sm border transition-colors hover:bg-white/5"
          style={{
            borderColor: 'color-mix(in srgb, var(--noor-p-500) 35%, transparent)',
            color: 'var(--noor-n-100)',
          }}
        >
          <Search className="w-4 h-4" />
          <span>اختيار الدولة والمدينة يدوياً</span>
        </button>

        <p className="text-[11px] text-center leading-relaxed" style={{ color: 'var(--noor-n-500)' }}>
          التطبيق يطلب إذن الموقع مرة واحدة عند أول استخدام — بياناتك لا تغادر هاتفك إطلاقاً،
          وحساب المواقيت يتم بالكامل على جهازك.
        </p>
      </div>

      {showPicker && (
        <LocationPickerModal
          isOpen={showPicker}
          onClose={() => setShowPicker(false)}
          currentCoords={coordinates}
          onSelectCoords={(coords) => {
            setCoordinates(coords);
            saveLocation(coords);
            setShowPicker(false);
          }}
        />
      )}
    </div>
  );
};

/* ============================================================================
 * صفحة المواقيت: طريقة الحساب + المذهب + الضبط الدقيق
 * ========================================================================== */
const TimesPage: React.FC<{
  onBack: () => void;
  coordinates: Coordinates;
  method: CalculationMethod;
  setMethod: (m: CalculationMethod) => void;
  isHanafi: boolean;
  setIsHanafi: (h: boolean) => void;
}> = ({ onBack, coordinates, method, setMethod, isHanafi, setIsHanafi }) => {
  const [adjustments, setAdjustments] = useState<PrayerTimeAdjustments>(getSavedPrayerAdjustments);

  const applyAdjustment = (key: string, delta: number) => {
    setAdjustments((prev) => {
      const current = prev[key as keyof PrayerTimeAdjustments] ?? 0;
      const next = Math.max(-30, Math.min(30, current + delta));
      const clean = { ...prev, [key]: next };
      savePrayerAdjustments(clean);
      return clean;
    });
  };

  // معاينة المواقيت بالإعدادات الحالية
  const preview = computePrayerTimes(
    new Date(),
    coordinates.latitude,
    coordinates.longitude,
    method,
    isHanafi,
    adjustments,
  );

  return (
    <div>
      <PageHeader
        title="المواقيت"
        subtitle="طريقة الحساب والمذهب والضبط الدقيق بالدقائق"
        onBack={onBack}
        icon={<Clock className="w-5 h-5" />}
      />

      <div className="space-y-4">
        <Card title="طريقة حساب المواقيت">
          <div className="space-y-2">
            {(Object.keys(CALCULATION_METHODS) as CalculationMethod[]).map((m) => (
              <button
                key={m}
                onClick={() => {
                  setMethod(m);
                  saveMethod(m);
                }}
                className="w-full flex items-center justify-between p-3.5 rounded-2xl border transition-colors text-right"
                style={{
                  borderColor:
                    method === m
                      ? 'var(--noor-p-500)'
                      : 'color-mix(in srgb, var(--noor-p-500) 18%, transparent)',
                  backgroundColor:
                    method === m
                      ? 'color-mix(in srgb, var(--noor-p-500) 15%, transparent)'
                      : 'transparent',
                }}
              >
                <div>
                  <span className="block text-sm font-bold" style={{ color: 'var(--noor-n-50)' }}>
                    {CALCULATION_METHODS[m].arabicName}
                  </span>
                  <span className="block text-[10px] mt-0.5" style={{ color: 'var(--noor-n-400)' }} dir="ltr">
                    {CALCULATION_METHODS[m].name}
                  </span>
                </div>
                {method === m && <CheckCircle className="w-5 h-5 shrink-0" style={{ color: 'var(--noor-p-400)' }} />}
              </button>
            ))}
          </div>
        </Card>

        <Card title="مذهب حساب العصر">
          <div className="grid grid-cols-2 gap-2.5">
            <button
              onClick={() => {
                setIsHanafi(false);
                saveMadhab('shafi');
              }}
              className={`py-3 rounded-2xl text-sm font-bold border transition-colors ${
                !isHanafi ? 'text-white' : ''
              }`}
              style={{
                borderColor: !isHanafi ? 'var(--noor-p-500)' : 'color-mix(in srgb, var(--noor-p-500) 18%, transparent)',
                backgroundColor: !isHanafi ? 'color-mix(in srgb, var(--noor-p-500) 18%, transparent)' : 'transparent',
                color: isHanafi ? 'var(--noor-n-300)' : undefined,
              }}
            >
              الجمهور (الشافعي)
            </button>
            <button
              onClick={() => {
                setIsHanafi(true);
                saveMadhab('hanafi');
              }}
              className={`py-3 rounded-2xl text-sm font-bold border transition-colors ${
                isHanafi ? 'text-white' : ''
              }`}
              style={{
                borderColor: isHanafi ? 'var(--noor-p-500)' : 'color-mix(in srgb, var(--noor-p-500) 18%, transparent)',
                backgroundColor: isHanafi ? 'color-mix(in srgb, var(--noor-p-500) 18%, transparent)' : 'transparent',
                color: !isHanafi ? 'var(--noor-n-300)' : undefined,
              }}
            >
              الحنفي
            </button>
          </div>
          <p className="text-[10px] leading-relaxed" style={{ color: 'var(--noor-n-500)' }}>
            الفرق يظهر في وقت صلاة العصر: الحنفي يتأخر عن الجمهور قليلاً.
          </p>
        </Card>

        <Card title="الضبط الدقيق بالدقائق (±)">
          <p className="text-[10px] leading-relaxed" style={{ color: 'var(--noor-n-400)' }}>
            لو مواقيتك متأخرة أو متقدمة دقيقة عن مسجد منطقتك — عدّلها هنا مرة واحدة وتنسى الموضوع.
          </p>
          <div className="space-y-2">
            {ADJUSTABLE_PRAYERS.map(({ key, label }) => (
              <div
                key={key}
                className="flex items-center justify-between p-3 rounded-2xl border"
                style={{ borderColor: 'color-mix(in srgb, var(--noor-p-500) 18%, transparent)' }}
              >
                <span className="text-sm font-bold" style={{ color: 'var(--noor-n-100)' }}>
                  {label}
                  <span className="text-[10px] font-mono mr-2" style={{ color: 'var(--noor-n-400)' }} dir="ltr">
                    {(preview as unknown as Record<string, string>)[key]}
                  </span>
                </span>
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => applyAdjustment(key, -1)}
                    className="w-9 h-9 rounded-xl border font-black text-base flex items-center justify-center transition-colors hover:bg-white/10"
                    style={{
                      borderColor: 'color-mix(in srgb, var(--noor-p-500) 25%, transparent)',
                      color: 'var(--noor-n-200)',
                    }}
                    aria-label="نقص دقيقة"
                  >
                    −
                  </button>
                  <span
                    className="w-14 text-center text-sm font-black font-mono tabular-nums"
                    style={{ color: (adjustments[key as keyof PrayerTimeAdjustments] ?? 0) === 0 ? 'var(--noor-n-400)' : 'var(--noor-a-400)' }}
                    dir="ltr"
                  >
                    {adjustments[key as keyof PrayerTimeAdjustments] ?? 0 > 0 ? '+' : ''}
                    {adjustments[key as keyof PrayerTimeAdjustments] ?? 0} د
                  </span>
                  <button
                    onClick={() => applyAdjustment(key, 1)}
                    className="w-9 h-9 rounded-xl border font-black text-base flex items-center justify-center transition-colors hover:bg-white/10"
                    style={{
                      borderColor: 'color-mix(in srgb, var(--noor-p-500) 25%, transparent)',
                      color: 'var(--noor-n-200)',
                    }}
                    aria-label="زد دقيقة"
                  >
                    +
                  </button>
                </div>
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );
};

/* ============================================================================
 * صفحة الأذان: الأصوات + التنبيهات + الأذونات
 * ========================================================================== */
const AdhanPage: React.FC<{
  onBack: () => void;
  adhanVoiceId: string;
  setAdhanVoiceId: (id: string) => void;
  adhanEnabled: boolean;
  setAdhanEnabled: (on: boolean) => void;
  prayerAlerts: PrayerAlerts;
  setPrayerAlerts: (alerts: PrayerAlerts) => void;
}> = ({
  onBack,
  adhanVoiceId,
  setAdhanVoiceId,
  adhanEnabled,
  setAdhanEnabled,
  prayerAlerts,
  setPrayerAlerts,
}) => {
  const [previewPlaying, setPreviewPlaying] = useState(false);
  const [permissions, setPermissions] = useState<{
    location: boolean;
    notifications: boolean;
    exactAlarms: boolean;
  } | null>(null);
  const audioRef = useRef<HTMLAudioElement | null>(null);

  useEffect(() => {
    void requestAllPermissions().then(setPermissions);
    return () => {
      audioRef.current?.pause();
    };
  }, []);

  const handleRequestPermissions = async () => {
    const result = await requestAllPermissions();
    setPermissions(result);
  };

  const togglePreview = () => {
    if (previewPlaying) {
      audioRef.current?.pause();
      setPreviewPlaying(false);
    } else {
      const voice = ADHAN_VOICES.find((v) => v.id === adhanVoiceId) ?? ADHAN_VOICES[0];
      if (!audioRef.current) {
        audioRef.current = new Audio();
      }
      audioRef.current.src = getAdhanAudioSrc(voice.id);
      audioRef.current.onended = () => setPreviewPlaying(false);
      audioRef.current.onerror = () => setPreviewPlaying(false);
      void audioRef.current.play().catch(() => setPreviewPlaying(false));
      setPreviewPlaying(true);
    }
  };

  const cycleMode = (key: keyof PrayerAlerts) => {
    const current = prayerAlerts[key];
    const next: PrayerAlertMode =
      current === 'adhan' ? 'notification' : current === 'notification' ? 'off' : 'adhan';
    const clean = { ...prayerAlerts, [key]: next };
    setPrayerAlerts(clean);
    savePrayerAlerts(clean);
  };

  const modeLabel: Record<PrayerAlertMode, string> = {
    adhan: 'أذان صوتي',
    notification: 'إشعار',
    off: 'مطفأ',
  };
  const modeColor: Record<PrayerAlertMode, string> = {
    adhan: '#10b981',
    notification: '#f59e0b',
    off: '#64748b',
  };

  return (
    <div>
      <PageHeader
        title="الأذان والتنبيهات"
        subtitle="يعمل الأذان في الخلفية حتى لو الشاشة مطفية"
        onBack={onBack}
        icon={<Bell className="w-5 h-5" />}
      />

      <div className="space-y-4">
        {/* تشغيل الأذان العام */}
        <div
          className="rounded-3xl border p-5 flex items-center justify-between gap-4"
          style={{
            borderColor: 'color-mix(in srgb, var(--noor-p-500) 22%, transparent)',
            backgroundColor: 'color-mix(in srgb, var(--noor-n-950) 55%, transparent)',
          }}
        >
          <div className="flex items-center gap-3.5">
            <div
              className="w-12 h-12 rounded-2xl flex items-center justify-center text-white"
              style={{ background: adhanEnabled ? 'linear-gradient(135deg, var(--noor-p-500), var(--noor-a-500))' : '#475569' }}
            >
              {adhanEnabled ? <BellRing className="w-6 h-6" /> : <Bell className="w-6 h-6" />}
            </div>
            <div>
              <p className="text-sm font-extrabold" style={{ color: 'var(--noor-n-50)' }}>
                تشغيل الأذان تلقائياً
              </p>
              <p className="text-[11px]" style={{ color: 'var(--noor-n-400)' }}>
                {adhanEnabled ? 'الأذان يعمل في وقت كل صلاة' : 'لن يصدر أي تنبيه صوتي'}
              </p>
            </div>
          </div>
          {/* مفتاح التشغيل */}
          <button
            onClick={() => {
              const next = !adhanEnabled;
              setAdhanEnabled(next);
              saveAdhanEnabled(next);
            }}
            className="relative w-14 h-8 rounded-full transition-colors shrink-0"
            style={{
              backgroundColor: adhanEnabled ? 'var(--noor-p-500)' : '#334155',
            }}
            role="switch"
            aria-checked={adhanEnabled}
          >
            <span
              className="absolute top-1 w-6 h-6 rounded-full bg-white shadow transition-all"
              style={{ right: adhanEnabled ? '0.25rem' : '1.75rem' }}
            />
          </button>
        </div>

        {/* الأذونات */}
        <Card title="أذونات التطبيق">
          {!isNativeApp() ? (
            <p className="text-[11px] leading-relaxed" style={{ color: 'var(--noor-n-400)' }}>
              أنت تستخدم نسخة الويب — الأذونات تُطلب داخل تطبيق أندرويد فقط.
            </p>
          ) : (
            <>
              <div className="grid grid-cols-3 gap-2 text-center">
                {[
                  { label: 'الموقع', ok: permissions?.location },
                  { label: 'الإشعارات', ok: permissions?.notifications },
                  { label: 'منبهات دقيقة', ok: permissions?.exactAlarms },
                ].map((p) => (
                  <div
                    key={p.label}
                    className="p-3 rounded-2xl border space-y-1.5"
                    style={{
                      borderColor:
                        p.ok === true
                          ? 'rgba(16,185,129,0.4)'
                          : p.ok === false
                          ? 'rgba(244,63,94,0.4)'
                          : 'color-mix(in srgb, var(--noor-p-500) 18%, transparent)',
                    }}
                  >
                    {p.ok === true ? (
                      <CircleCheck className="w-5 h-5 mx-auto text-emerald-400" />
                    ) : p.ok === false ? (
                      <TriangleAlert className="w-5 h-5 mx-auto text-rose-400" />
                    ) : (
                      <ShieldCheck className="w-5 h-5 mx-auto" style={{ color: 'var(--noor-n-400)' }} />
                    )}
                    <p className="text-[10px] font-bold" style={{ color: 'var(--noor-n-200)' }}>
                      {p.label}
                    </p>
                    <p className="text-[9px]" style={{ color: 'var(--noor-n-500)' }}>
                      {p.ok === true ? 'ممنوح' : p.ok === false ? 'غير ممنوح' : '…'}
                    </p>
                  </div>
                ))}
              </div>
              <button
                onClick={handleRequestPermissions}
                className="w-full flex items-center justify-center gap-2 py-3 rounded-2xl text-sm font-bold border transition-colors hover:bg-white/5"
                style={{
                  borderColor: 'color-mix(in srgb, var(--noor-p-500) 35%, transparent)',
                  color: 'var(--noor-n-100)',
                }}
              >
                <ShieldCheck className="w-4 h-4" />
                <span>طلب كل الأذونات الآن</span>
              </button>
              <p className="text-[10px] leading-relaxed" style={{ color: 'var(--noor-n-500)' }}>
                الموقع لحساب المواقيت بدقة لمدينتك، والإشعارات والمنبهات الدقيقة ليعمل الأذان
                في وقته حتى والشاشة مطفية.
              </p>
            </>
          )}
        </Card>

        {/* صوت الأذان */}
        <Card title="صوت الأذان">
          <div className="space-y-2">
            {ADHAN_VOICES.map((voice) => (
              <div
                key={voice.id}
                className="flex items-center justify-between p-3.5 rounded-2xl border"
                style={{
                  borderColor:
                    adhanVoiceId === voice.id
                      ? 'var(--noor-p-500)'
                      : 'color-mix(in srgb, var(--noor-p-500) 18%, transparent)',
                  backgroundColor:
                    adhanVoiceId === voice.id
                      ? 'color-mix(in srgb, var(--noor-p-500) 12%, transparent)'
                      : 'transparent',
                }}
              >
                <button
                  onClick={() => {
                    setAdhanVoiceId(voice.id);
                    saveAdhanVoice(voice.id);
                  }}
                  className="flex-1 text-right"
                >
                  <span className="block text-sm font-bold" style={{ color: 'var(--noor-n-50)' }}>
                    {voice.name}
                  </span>
                  <span className="block text-[10px] mt-0.5" style={{ color: 'var(--noor-n-400)' }}>
                    {voice.location}
                  </span>
                </button>
                {adhanVoiceId === voice.id && (
                  <CheckCircle className="w-5 h-5 mx-2 shrink-0" style={{ color: 'var(--noor-p-400)' }} />
                )}
              </div>
            ))}
          </div>
          <button
            onClick={togglePreview}
            className="w-full flex items-center justify-center gap-2 py-3 rounded-2xl text-sm font-bold border transition-colors hover:bg-white/5"
            style={{
              borderColor: 'color-mix(in srgb, var(--noor-p-500) 35%, transparent)',
              color: 'var(--noor-n-100)',
            }}
          >
            {previewPlaying ? (
              <>
                <Square className="w-4 h-4" />
                <span>إيقاف المعاينة</span>
              </>
            ) : (
              <>
                <Play className="w-4 h-4" />
                <span>معاينة صوت الأذان</span>
              </>
            )}
          </button>
        </Card>

        {/* نمط كل صلاة */}
        <Card title="تنبيه كل صلاة">
          <p className="text-[10px] leading-relaxed" style={{ color: 'var(--noor-n-400)' }}>
            اضغط على الصلاة للتبديل: أذان صوتي ← إشعار ← مطفأ
          </p>
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
            {(Object.keys(prayerAlerts) as Array<keyof PrayerAlerts>).map((key) => {
              const mode = prayerAlerts[key];
              return (
                <button
                  key={key}
                  onClick={() => cycleMode(key)}
                  className="p-3 rounded-2xl border text-center space-y-1 transition-colors"
                  style={{
                    borderColor: `${modeColor[mode]}66`,
                    backgroundColor: `${modeColor[mode]}14`,
                  }}
                >
                  <p className="text-xs font-extrabold" style={{ color: 'var(--noor-n-50)' }}>
                    {PRAYER_NAMES_AR[key]}
                  </p>
                  <span
                    className="inline-block text-[10px] font-bold px-2 py-0.5 rounded-full"
                    style={{ color: modeColor[mode], backgroundColor: `${modeColor[mode]}1f` }}
                  >
                    {modeLabel[mode]}
                  </span>
                </button>
              );
            })}
          </div>
        </Card>
      </div>
    </div>
  );
};

/* ============================================================================
 * صفحة المصحف: الطبعات
 * ========================================================================== */
const MushafPage: React.FC<{ onBack: () => void }> = ({ onBack }) => {
  const [edition, setEditionState] = useState<MushafEdition>(() => {
    try {
      const saved = localStorage.getItem('noor_mushaf_edition') as MushafEdition | null;
      if (saved && MUSHAF_EDITIONS[saved]) return saved;
    } catch {
      // تجاهل
    }
    return 'tajweed';
  });

  return (
    <div>
      <PageHeader
        title="طبعة المصحف"
        subtitle="تتطبق فوراً على المصحف التفاعلي"
        onBack={onBack}
        icon={<BookOpen className="w-5 h-5" />}
      />

      <div className="space-y-3">
        {(Object.keys(MUSHAF_EDITIONS) as MushafEdition[]).map((key) => {
          const ed = MUSHAF_EDITIONS[key];
          const isSelected = edition === key;
          return (
            <button
              key={key}
              onClick={() => {
                setEditionState(key);
                saveEdition(key);
              }}
              className="w-full p-4 rounded-3xl border flex items-center gap-4 text-right transition-all"
              style={{
                borderColor: isSelected ? ed.frame : 'color-mix(in srgb, var(--noor-p-500) 18%, transparent)',
                backgroundColor: isSelected ? `color-mix(in srgb, ${ed.frame} 10%, transparent)` : 'transparent',
              }}
            >
              <div
                className="w-14 h-20 rounded-xl border-4 flex items-center justify-center shrink-0 shadow-inner"
                style={{ backgroundColor: ed.paper, borderColor: ed.frame }}
              >
                <span className="font-quran text-lg" style={{ color: ed.showTajweed ? '#dd0008' : ed.ink }}>
                  ق
                </span>
              </div>
              <div className="flex-1">
                <div className="flex items-center gap-2">
                  <span className="text-sm font-extrabold" style={{ color: 'var(--noor-n-50)' }}>
                    {ed.label}
                  </span>
                  {isSelected && <CheckCircle className="w-4 h-4" style={{ color: 'var(--noor-p-400)' }} />}
                </div>
                <p className="text-[11px] mt-1 leading-relaxed" style={{ color: 'var(--noor-n-400)' }}>
                  {ed.description}
                </p>
              </div>
            </button>
          );
        })}
      </div>
    </div>
  );
};

/* ============================================================================
 * صفحة القارئ والتلاوات — أسماء فقط (بلا صور) + تنزيل حقيقي للسور
 * ========================================================================== */
const ReciterPage: React.FC<{ onBack: () => void }> = ({ onBack }) => {
  const [reciterId, setReciterId] = useState<string>(() => {
    try {
      return localStorage.getItem('noor_preferred_reciter') ?? AYAH_RECITERS[0].id;
    } catch {
      return AYAH_RECITERS[0].id;
    }
  });
  const [search, setSearch] = useState('');
  const [downloads, setDownloads] = useState<Record<string, number>>({});
  const [toast, setToast] = useState<string | null>(null);

  const showToast = (msg: string) => {
    setToast(msg);
    setTimeout(() => setToast(null), 3500);
  };

  const handleDownload = async (surahNumber: number) => {
    const surah = SURAHS_LIST.find((s) => s.number === surahNumber);
    if (!surah) return;
    const key = `${reciterId}_${surahNumber}`;
    setDownloads((prev) => ({ ...prev, [key]: 0 }));
    const ok = await downloadSurahForOffline(reciterId, surahNumber, (fraction) => {
      setDownloads((prev) => ({ ...prev, [key]: Math.round(fraction * 100) }));
    });
    setDownloads((prev) => {
      const copy = { ...prev };
      delete copy[key];
      return copy;
    });
    if (ok) {
      showToast(`تم تنزيل سورة ${surah.name} — استمع لها بدون إنترنت`);
    } else {
      showToast('تعذر التنزيل — تأكد من اتصال الإنترنت وحاول مجدداً');
    }
  };

  const handleRemove = async (surahNumber: number) => {
    await removeSurahFromOffline(reciterId, surahNumber);
    // إعادة رسم
    setDownloads((prev) => ({ ...prev }));
  };

  const filteredSurahs = SURAHS_LIST.filter((s) => {
    const q = search.trim();
    if (!q) return true;
    return (
      s.name.includes(q) ||
      s.englishName.toLowerCase().includes(q.toLowerCase()) ||
      s.number.toString() === q
    );
  });

  return (
    <div>
      <PageHeader
        title="القارئ والتلاوات"
        subtitle="اختر القارئ ونزّل السور للاستماع بدون إنترنت"
        onBack={onBack}
        icon={<Mic2 className="w-5 h-5" />}
      />

      {toast && (
        <div className="fixed top-4 left-1/2 -translate-x-1/2 z-50 p-3 rounded-2xl bg-emerald-600 text-white shadow-2xl flex items-center gap-2 text-xs animate-in fade-in slide-in-from-top-2">
          <CheckCircle className="w-4 h-4" />
          <span>{toast}</span>
        </div>
      )}

      <div className="space-y-4">
        {/* اختيار القارئ — أسماء فقط */}
        <Card title="القارئ المفضل">
          <div className="grid gap-2 max-h-72 overflow-y-auto pr-1">
            {AYAH_RECITERS.map((rec) => (
              <button
                key={rec.id}
                onClick={() => {
                  setReciterId(rec.id);
                  try {
                    localStorage.setItem('noor_preferred_reciter', rec.id);
                  } catch {
                    // تجاهل
                  }
                }}
                className="flex items-center justify-between p-3 rounded-2xl border transition-colors text-right"
                style={{
                  borderColor:
                    reciterId === rec.id
                      ? 'var(--noor-p-500)'
                      : 'color-mix(in srgb, var(--noor-p-500) 18%, transparent)',
                  backgroundColor:
                    reciterId === rec.id
                      ? 'color-mix(in srgb, var(--noor-p-500) 12%, transparent)'
                      : 'transparent',
                }}
              >
                <div>
                  <span className="text-sm font-bold" style={{ color: 'var(--noor-n-50)' }}>
                    {rec.name}
                  </span>
                  {rec.country && (
                    <span className="text-[10px] mr-2" style={{ color: 'var(--noor-n-400)' }}>
                      {rec.country}
                    </span>
                  )}
                </div>
                {reciterId === rec.id && (
                  <CheckCircle className="w-4 h-4 shrink-0" style={{ color: 'var(--noor-p-400)' }} />
                )}
              </button>
            ))}
          </div>
        </Card>

        {/* تنزيل السور */}
        <Card title="تنزيل السور للاستماع بدون إنترنت">
          <p className="text-[10px] leading-relaxed" style={{ color: 'var(--noor-n-400)' }}>
            التنزيل حقيقي: يُخزَّن صوت كل آية على جهازك، والتلاوة تعمل أوفلاين بالكامل من المصحف.
          </p>
          <div className="relative">
            <Search className="w-4 h-4 absolute right-3.5 top-1/2 -translate-y-1/2" style={{ color: 'var(--noor-n-400)' }} />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="ابحث عن سورة…"
              className="w-full pl-4 pr-10 py-2.5 rounded-2xl border text-sm focus:outline-none"
              style={{
                backgroundColor: 'color-mix(in srgb, var(--noor-n-950) 70%, transparent)',
                borderColor: 'color-mix(in srgb, var(--noor-p-500) 25%, transparent)',
                color: 'var(--noor-n-100)',
              }}
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 max-h-96 overflow-y-auto pr-1">
            {filteredSurahs.map((surah) => {
              const key = `${reciterId}_${surah.number}`;
              const progress = downloads[key];
              const downloaded = isSurahOffline(reciterId, surah.number);
              return (
                <div
                  key={surah.number}
                  className="flex items-center justify-between gap-2 p-3 rounded-2xl border"
                  style={{ borderColor: 'color-mix(in srgb, var(--noor-p-500) 18%, transparent)' }}
                >
                  <div className="min-w-0 flex-1">
                    <span className="text-xs font-bold" style={{ color: 'var(--noor-n-100)' }}>
                      {surah.number}. {surah.name}
                    </span>
                    {progress !== undefined ? (
                      <div className="mt-1.5 h-1.5 rounded-full overflow-hidden" style={{ backgroundColor: 'rgba(255,255,255,0.08)' }}>
                        <div
                          className="h-full transition-all"
                          style={{
                            width: `${progress}%`,
                            background: 'linear-gradient(90deg, var(--noor-p-500), var(--noor-a-500))',
                          }}
                        />
                      </div>
                    ) : (
                      downloaded && (
                        <span className="flex items-center gap-1 text-[9px] text-emerald-400 mt-0.5">
                          <Check className="w-3 h-3" /> محملة أوفلاين
                        </span>
                      )
                    )}
                  </div>
                  {progress !== undefined ? (
                    <span className="text-[10px] font-mono shrink-0" style={{ color: 'var(--noor-a-400)' }}>
                      {progress}%
                    </span>
                  ) : downloaded ? (
                    <button
                      onClick={() => void handleRemove(surah.number)}
                      className="p-2 rounded-xl text-rose-400 hover:bg-rose-500/10 transition-colors shrink-0"
                      title="حذف من التخزين"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  ) : (
                    <button
                      onClick={() => void handleDownload(surah.number)}
                      className="p-2 rounded-xl transition-colors hover:bg-white/10 shrink-0"
                      style={{ color: 'var(--noor-a-400)' }}
                      title="تنزيل للاستماع بدون إنترنت"
                    >
                      <Download className="w-4 h-4" />
                    </button>
                  )}
                </div>
              );
            })}
          </div>
        </Card>
      </div>
    </div>
  );
};

/* ============================================================================
 * صفحة المظهر
 * ========================================================================== */
const AppearancePage: React.FC<{
  onBack: () => void;
  currentTheme: AppTheme;
  setCurrentTheme: (t: AppTheme) => void;
  colorMode: ColorMode;
  setColorMode: (m: ColorMode) => void;
}> = ({ onBack, currentTheme, setCurrentTheme, colorMode, setColorMode }) => (
  <div>
    <PageHeader
      title="المظهر"
      subtitle="لون التطبيق ووضع الإضاءة"
      onBack={onBack}
      icon={<Palette className="w-5 h-5" />}
    />

    <div className="space-y-4">
      {/* وضع الإضاءة — أيقونات */}
      <Card title="وضع الإضاءة">
        <div className="grid grid-cols-2 gap-3">
          <button
            onClick={() => setColorMode('light')}
            className={`flex flex-col items-center gap-2 py-4 rounded-2xl border transition-all ${
              colorMode === 'light' ? 'ring-2' : 'opacity-70 hover:opacity-100'
            }`}
            style={{
              borderColor:
                colorMode === 'light' ? 'var(--noor-a-500)' : 'color-mix(in srgb, var(--noor-p-500) 20%, transparent)',
              backgroundColor:
                colorMode === 'light' ? 'color-mix(in srgb, var(--noor-a-500) 12%, transparent)' : 'transparent',
              ['--tw-ring-color' as string]: 'var(--noor-a-500)',
            }}
          >
            <Sun className="w-8 h-8 text-amber-400" />
            <span className="text-xs font-bold" style={{ color: 'var(--noor-n-100)' }}>
              نهاري
            </span>
          </button>
          <button
            onClick={() => setColorMode('dark')}
            className={`flex flex-col items-center gap-2 py-4 rounded-2xl border transition-all ${
              colorMode === 'dark' ? 'ring-2' : 'opacity-70 hover:opacity-100'
            }`}
            style={{
              borderColor:
                colorMode === 'dark' ? 'var(--noor-a-500)' : 'color-mix(in srgb, var(--noor-p-500) 20%, transparent)',
              backgroundColor:
                colorMode === 'dark' ? 'color-mix(in srgb, var(--noor-a-500) 12%, transparent)' : 'transparent',
              ['--tw-ring-color' as string]: 'var(--noor-a-500)',
            }}
          >
            <Moon className="w-8 h-8 text-indigo-300" />
            <span className="text-xs font-bold" style={{ color: 'var(--noor-n-100)' }}>
              ليلي
            </span>
          </button>
        </div>
      </Card>

      {/* ألوان بسيطة بدون شروحات — أسماء ألوان فقط */}
      <Card title="لون التطبيق">
        <div className="grid grid-cols-3 sm:grid-cols-6 gap-2.5">
          {APP_THEMES.map((t) => {
            const isSelected = currentTheme.id === t.id;
            return (
              <button
                key={t.id}
                onClick={() => setCurrentTheme(t)}
                className={`flex flex-col items-center gap-2 p-3 rounded-2xl border transition-all ${
                  isSelected ? 'ring-2' : 'hover:brightness-110'
                }`}
                style={{
                  borderColor: isSelected
                    ? t.primaryColor
                    : 'color-mix(in srgb, var(--noor-n-700) 60%, transparent)',
                  backgroundColor: isSelected
                    ? `color-mix(in srgb, ${t.primaryColor} 15%, transparent)`
                    : 'color-mix(in srgb, var(--noor-n-950) 50%, transparent)',
                  ...(isSelected ? { ['--tw-ring-color' as string]: `${t.primaryColor}66` } : {}),
                }}
                title={t.name}
              >
                <span
                  className="w-8 h-8 rounded-full shadow-md"
                  style={{
                    background: `linear-gradient(135deg, ${t.primaryColor}, ${t.accentColor})`,
                  }}
                />
                <span className="text-[11px] font-bold" style={{ color: 'var(--noor-n-200)' }}>
                  {t.name}
                </span>
              </button>
            );
          })}
        </div>
      </Card>
    </div>
  </div>
);

/* ============================================================================
 * صفحة الويدجت — إرشادات الإضافة + معاينة حية + تحديث فوري
 * ========================================================================== */
const WidgetsPage: React.FC<{ onBack: () => void; coordinates: Coordinates }> = ({
  onBack,
  coordinates,
}) => {
  const [now, setNow] = useState(new Date());
  const [refreshing, setRefreshing] = useState(false);
  const [refreshedAt, setRefreshedAt] = useState<string | null>(null);

  useEffect(() => {
    const t = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(t);
  }, []);

  const next = getNextPrayer(
    computePrayerTimes(
      now,
      coordinates.latitude,
      coordinates.longitude,
      getSavedMethod(),
      getSavedMadhab() === 'hanafi',
      getSavedPrayerAdjustments(),
    ),
    now,
  );

  const handleRefresh = async () => {
    setRefreshing(true);
    await pushWidgetUpdate({
      city: coordinates.city,
      hijri: '',
      times: {},
      order: [],
      next: {
        key: next.name,
        name: next.arabicName,
        ts: next.targetDate.getTime(),
        time: next.timeStr,
      },
      tzOffsetMinutes: -new Date().getTimezoneOffset(),
    });
    setRefreshing(false);
    setRefreshedAt(
      new Date().toLocaleTimeString('ar-EG', { hour: '2-digit', minute: '2-digit' }),
    );
  };

  const countdown = (() => {
    const s = Math.max(0, next.remainingSeconds);
    const h = Math.floor(s / 3600);
    const m = Math.floor((s % 3600) / 60);
    const sec = s % 60;
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${pad(h)}:${pad(m)}:${pad(sec)}`;
  })();

  return (
    <div>
      <PageHeader
        title="ويدجت المواقيت"
        subtitle="مواقيت الصلاة على شاشة هاتفك الرئيسية"
        onBack={onBack}
        icon={<Smartphone className="w-5 h-5" />}
      />

      <div className="space-y-4">
        {/* المعاينة الحية — تتحدث كل ثانية */}
        <div className="rounded-3xl overflow-hidden border shadow-2xl mx-auto max-w-xs"
          style={{ borderColor: 'color-mix(in srgb, var(--noor-p-500) 30%, transparent)' }}
        >
          <div
            className="p-4 text-center text-white"
            style={{
              background: 'linear-gradient(160deg, #064e3b 0%, #022c22 70%, #01160f 100%)',
            }}
          >
            <div className="flex items-center justify-center gap-1.5 mb-1">
              <span className="text-base">🕌</span>
              <span className="text-[11px] font-bold text-emerald-300">نور المسلم</span>
            </div>
            <p className="text-[10px] text-emerald-200/70 mb-3">{coordinates.city}</p>
            <p className="text-2xl font-black text-white">{next.arabicName}</p>
            <p className="text-sm font-mono text-emerald-300 mb-2" dir="ltr">
              {next.timeStr}
            </p>
            <div
              className="inline-block px-4 py-1.5 rounded-xl border border-emerald-500/30 bg-black/30"
              dir="ltr"
            >
              <span className="font-mono text-base font-bold text-white tabular-nums">{countdown}</span>
            </div>
            <p className="text-[9px] text-emerald-200/60 mt-2">هذه هي المعاينة الحية للويدجت</p>
          </div>
        </div>

        {/* خطوات الإضافة */}
        <Card title="إضافة الويدجت لشاشتك الرئيسية">
          <ol className="space-y-2.5 text-xs leading-relaxed" style={{ color: 'var(--noor-n-200)' }}>
            {[
              'اضغط مطولاً على مكان فارغ في شاشة هاتفك الرئيسية',
              'اختر «Widgets / الأدوات» من القائمة',
              'ابحث عن «نور المسلم» في قائمة التطبيقات',
              'اضغط مطولاً على ويدجت «مواقيت الصلاة» واسحبه لمكانك المفضل',
            ].map((step, i) => (
              <li key={i} className="flex items-start gap-2.5">
                <span
                  className="w-5 h-5 rounded-full flex items-center justify-center text-[10px] font-black text-white shrink-0 mt-0.5"
                  style={{ background: 'linear-gradient(135deg, var(--noor-p-500), var(--noor-a-500))' }}
                >
                  {i + 1}
                </span>
                <span>{step}</span>
              </li>
            ))}
          </ol>
        </Card>

        <button
          onClick={() => void handleRefresh()}
          disabled={refreshing}
          className="w-full flex items-center justify-center gap-2 py-3.5 rounded-2xl font-bold text-sm text-white shadow-lg disabled:opacity-60"
          style={{ background: 'linear-gradient(135deg, var(--noor-p-500), var(--noor-a-500))' }}
        >
          <RefreshCw className={`w-4 h-4 ${refreshing ? 'animate-spin' : ''}`} />
          <span>تحديث الويدجت الآن</span>
        </button>
        {refreshedAt && (
          <p className="text-[11px] text-center" style={{ color: 'var(--noor-n-400)' }}>
            تم التحديث الساعة {refreshedAt}
          </p>
        )}

        {!isNativeApp() && (
          <p className="text-[11px] text-center leading-relaxed" style={{ color: 'var(--noor-n-500)' }}>
            الويدجت متاح في تطبيق أندرويد فقط — هذه نسخة الويب.
          </p>
        )}
      </div>
    </div>
  );
};

/* ============================================================================
 * صفحة التحديثات
 * ========================================================================== */
type UpdateState =
  | { status: 'idle' }
  | { status: 'checking' }
  | { status: 'latest'; version: string }
  | { status: 'available'; version: string; url: string }
  | { status: 'error'; message: string };

const UpdatesPage: React.FC<{ onBack: () => void }> = ({ onBack }) => {
  const [state, setState] = useState<UpdateState>({ status: 'idle' });
  const isApk = detectApkEnvironment();

  const check = useCallback(async () => {
    setState({ status: 'checking' });
    try {
      const release = await fetchLatestRelease();
      if (!release) {
        setState({
          status: 'error',
          message: 'تعذر الوصول لصفحة الإصدارات الآن — تأكد من اتصال الإنترنت وحاول مجدداً.',
        });
        return;
      }
      const current = APP_VERSION;
      const latest = release.version;
      const [cMajor, cMinor, cPatch] = current.split('.').map(Number);
      const [lMajor, lMinor, lPatch] = latest.split('.').map(Number);
      const isNewer =
        lMajor > cMajor || (lMajor === cMajor && lMinor > cMinor) || (lMajor === cMajor && lMinor === cMinor && lPatch > cPatch);
      if (isNewer) {
        setState({
          status: 'available',
          version: latest,
          url: release.apkUrl ?? release.htmlUrl,
        });
      } else {
        setState({ status: 'latest', version: latest });
      }
    } catch {
      setState({ status: 'error', message: 'حدث خطأ غير متوقع أثناء فحص التحديث.' });
    }
  }, []);

  // فحص تلقائي عند فتح الصفحة
  useEffect(() => {
    void check();
  }, [check]);

  return (
    <div>
      <PageHeader
        title="التحديثات"
        subtitle={`نسختك الحالية ${APP_VERSION}`}
        onBack={onBack}
        icon={<RefreshCw className="w-5 h-5" />}
      />

      <div className="space-y-4">
        <div
          className="rounded-3xl border p-5 text-center space-y-3"
          style={{
            borderColor: 'color-mix(in srgb, var(--noor-p-500) 22%, transparent)',
            backgroundColor: 'color-mix(in srgb, var(--noor-n-950) 55%, transparent)',
          }}
        >
          {state.status === 'checking' && (
            <>
              <RefreshCw className="w-10 h-10 mx-auto animate-spin" style={{ color: 'var(--noor-a-400)' }} />
              <p className="text-sm font-bold" style={{ color: 'var(--noor-n-200)' }}>
                جاري فحص التحديثات…
              </p>
            </>
          )}
          {state.status === 'latest' && (
            <>
              <CircleCheck className="w-10 h-10 mx-auto text-emerald-400" />
              <p className="text-sm font-bold" style={{ color: 'var(--noor-n-50)' }}>
                تطبيقك محدّث لأحدث نسخة
              </p>
              <p className="text-xs" style={{ color: 'var(--noor-n-400)' }}>
                أحدث إصدار متاح: v{state.version} — وأنت عليه الآن
              </p>
            </>
          )}
          {state.status === 'available' && (
            <>
              <Wifi className="w-10 h-10 mx-auto" style={{ color: 'var(--noor-a-400)' }} />
              <p className="text-sm font-extrabold" style={{ color: 'var(--noor-n-50)' }}>
                يتوفر تحديث جديد: v{state.version}
              </p>
              <button
                onClick={() => window.open(state.url, '_blank', 'noopener,noreferrer')}
                className="inline-flex items-center gap-2 px-5 py-2.5 rounded-2xl text-sm font-bold text-white shadow-lg"
                style={{ background: 'linear-gradient(135deg, var(--noor-p-500), var(--noor-a-500))' }}
              >
                <Download className="w-4 h-4" />
                <span>تحميل النسخة الجديدة</span>
              </button>
            </>
          )}
          {state.status === 'error' && (
            <>
              <TriangleAlert className="w-10 h-10 mx-auto text-amber-400" />
              <p className="text-xs leading-relaxed" style={{ color: 'var(--noor-n-300)' }}>
                {state.message}
              </p>
            </>
          )}

          <button
            onClick={() => void check()}
            disabled={state.status === 'checking'}
            className="mx-auto flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold border transition-colors hover:bg-white/5 disabled:opacity-50"
            style={{
              borderColor: 'color-mix(in srgb, var(--noor-p-500) 30%, transparent)',
              color: 'var(--noor-n-200)',
            }}
          >
            <RefreshCw className="w-3.5 h-3.5" />
            <span>فحص مجدداً</span>
          </button>
        </div>

        <button
          onClick={() => window.open(RELEASES_PAGE_URL, '_blank', 'noopener,noreferrer')}
          className="w-full flex items-center justify-center gap-2 py-3 rounded-2xl text-xs font-bold border transition-colors hover:bg-white/5"
          style={{
            borderColor: 'color-mix(in srgb, var(--noor-p-500) 25%, transparent)',
            color: 'var(--noor-n-300)',
          }}
        >
          <ExternalLink className="w-4 h-4" />
          <span>سجل الإصدارات على GitHub</span>
        </button>

        <p className="text-[10px] text-center leading-relaxed" style={{ color: 'var(--noor-n-500)' }}>
          {isApk
            ? 'التحميل يتم من GitHub Releases — ثبّت النسخة الجديدة فوق القديمة مباشرة وبياناتك محفوظة.'
            : 'أنت تستخدم نسخة الويب — التحديث يطبق تلقائياً بإعادة تحميل الصفحة.'}
        </p>
      </div>
    </div>
  );
};

/* ============================================================================
 * صفحة الشكاوى والمقترحات
 * ========================================================================== */
const FeedbackPage: React.FC<{ onBack: () => void }> = ({ onBack }) => {
  const [text, setText] = useState('');
  const [copied, setCopied] = useState(false);

  const issueUrl = () => {
    const base = 'https://github.com/Sawan4Art/noor/issues/new';
    const title = encodeURIComponent('شكوى / مقترح — تطبيق نور المسلم');
    const body = encodeURIComponent(
      `${text.trim()}\n\n---\nإصدار التطبيق: ${APP_VERSION}\n${detectApkEnvironment() ? 'من داخل تطبيق أندرويد' : 'من نسخة الويب'}`,
    );
    return `${base}?title=${title}&body=${body}`;
  };

  const copyText = async () => {
    try {
      await navigator.clipboard.writeText(
        `${text.trim()}\n\n(إصدار ${APP_VERSION})`,
      );
      setCopied(true);
      setTimeout(() => setCopied(false), 2500);
    } catch {
      // تجاهل
    }
  };

  return (
    <div>
      <PageHeader
        title="الشكاوى والمقترحات"
        subtitle="صوتك يطوّر التطبيق — كل رسالة تُقرأ بعناية"
        onBack={onBack}
        icon={<MessageSquareHeart className="w-5 h-5" />}
      />

      <div className="space-y-4">
        <div
          className="rounded-3xl border p-4 space-y-3"
          style={{
            borderColor: 'color-mix(in srgb, var(--noor-p-500) 22%, transparent)',
            backgroundColor: 'color-mix(in srgb, var(--noor-n-950) 55%, transparent)',
          }}
        >
          <textarea
            value={text}
            onChange={(e) => setText(e.target.value)}
            rows={6}
            placeholder="اكتب شكواك أو اقتراحك هنا بالتفصيل…"
            className="w-full p-4 rounded-2xl border text-sm leading-relaxed focus:outline-none focus:ring-2 resize-none"
            style={{
              backgroundColor: 'color-mix(in srgb, var(--noor-n-950) 70%, transparent)',
              borderColor: 'color-mix(in srgb, var(--noor-p-500) 25%, transparent)',
              color: 'var(--noor-n-100)',
            }}
          />
          <div className="flex flex-col sm:flex-row gap-2">
            <button
              onClick={() => window.open(issueUrl(), '_blank', 'noopener,noreferrer')}
              disabled={text.trim().length < 3}
              className="flex-1 flex items-center justify-center gap-2 py-3 rounded-2xl text-sm font-bold text-white shadow-lg disabled:opacity-50"
              style={{ background: 'linear-gradient(135deg, var(--noor-p-500), var(--noor-a-500))' }}
            >
              <Send className="w-4 h-4" />
              <span>إرسال للمطور عبر GitHub</span>
            </button>
            <button
              onClick={() => void copyText()}
              disabled={text.trim().length < 3}
              className="flex items-center justify-center gap-2 px-5 py-3 rounded-2xl text-sm font-bold border transition-colors hover:bg-white/5 disabled:opacity-50"
              style={{
                borderColor: 'color-mix(in srgb, var(--noor-p-500) 30%, transparent)',
                color: 'var(--noor-n-200)',
              }}
            >
              {copied ? <Check className="w-4 h-4 text-emerald-400" /> : <CopyIcon className="w-4 h-4" />}
              <span>{copied ? 'تم النسخ' : 'نسخ النص'}</span>
            </button>
          </div>
        </div>

        <div
          className="rounded-3xl border p-4 flex items-start gap-3"
          style={{
            borderColor: 'color-mix(in srgb, var(--noor-a-500) 25%, transparent)',
            backgroundColor: 'color-mix(in srgb, var(--noor-a-500) 6%, transparent)',
          }}
        >
          <HeartHandshake className="w-5 h-5 shrink-0 mt-0.5" style={{ color: 'var(--noor-a-400)' }} />
          <p className="text-[11px] leading-relaxed" style={{ color: 'var(--noor-n-300)' }}>
            جزاك الله خيراً على وقتك — هذه النسخة صدقة جارية على روح والد المالك، وكل ملاحظة
            منك تجعلها أنفع لكل من يستخدمها. لو GitHub صعب عليك، انسخ النص وأرسله للمطور مباشرة.
          </p>
        </div>
      </div>
    </div>
  );
};

/* ============================================================================
 * صفحة عن التطبيق
 * ========================================================================== */
const AboutPage: React.FC<{ onBack: () => void }> = ({ onBack }) => (
  <div>
    <PageHeader
      title="عن التطبيق"
      subtitle="نور المسلم — رفيقك اليومي في العبادة"
      onBack={onBack}
      icon={<Info className="w-5 h-5" />}
    />

    <div className="space-y-4">
      <div
        className="rounded-3xl border p-6 text-center space-y-4"
        style={{
          borderColor: 'color-mix(in srgb, var(--noor-p-500) 25%, transparent)',
          background: 'linear-gradient(160deg, color-mix(in srgb, var(--noor-p-500) 12%, transparent), transparent)',
        }}
      >
        <div
          className="w-20 h-20 rounded-3xl mx-auto flex items-center justify-center text-4xl shadow-2xl"
          style={{ background: 'linear-gradient(135deg, var(--noor-p-500), var(--noor-a-500))' }}
        >
          🕌
        </div>
        <div>
          <h3 className="text-xl font-black text-white">نور المسلم</h3>
          <p className="text-xs mt-1 font-mono" style={{ color: 'var(--noor-n-400)' }} dir="ltr">
            v{APP_VERSION}
          </p>
        </div>
        <p
          className="text-sm font-bold leading-relaxed"
          style={{ color: 'var(--noor-a-400)' }}
        >
          هذا التطبيق صدقة جارية على روح المرحوم بأذن الله أبي
        </p>
      </div>

      <Card title="ماذا يوجد داخل التطبيق؟">
        <ul className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs" style={{ color: 'var(--noor-n-200)' }}>
          {[
            'مواقيت صلاة دقيقة مع أذان يعمل في الخلفية',
            'مصحف التجويد الملون كامل 604 صفحة تفاعلي',
            'بوصلة قبلة مستقرة مع توجيه دقيق',
            'أذكار وأدعية شاملة بعدادات ذكية',
            'مسبحة إلكترونية مع أذكار مأثورة',
            'ويدجت مواقيت لشاشة الهاتف الرئيسية',
            'تلاوات بلا تقطع مع تنزيل أوفلاين',
            'بحث ذكي في آيات القرآن الكريم',
          ].map((feature) => (
            <li key={feature} className="flex items-start gap-2">
              <Check className="w-3.5 h-3.5 text-emerald-400 shrink-0 mt-0.5" />
              <span>{feature}</span>
            </li>
          ))}
        </ul>
      </Card>

      <Card title="المطور">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div
              className="w-11 h-11 rounded-full flex items-center justify-center text-white font-black"
              style={{ background: 'linear-gradient(135deg, var(--noor-p-500), var(--noor-a-500))' }}
            >
              YS
            </div>
            <div>
              <p className="text-sm font-bold" style={{ color: 'var(--noor-n-100)' }}>
                Yousef Sawan
              </p>
              <p className="text-[10px]" style={{ color: 'var(--noor-n-400)' }}>
                Sawan4Art
              </p>
            </div>
          </div>
          <button
            onClick={() => window.open('https://github.com/Sawan4Art/noor', '_blank', 'noopener,noreferrer')}
            className="flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-bold border transition-colors hover:bg-white/5"
            style={{
              borderColor: 'color-mix(in srgb, var(--noor-p-500) 30%, transparent)',
              color: 'var(--noor-n-200)',
            }}
          >
            <ExternalLink className="w-3.5 h-3.5" />
            <span>GitHub</span>
          </button>
        </div>
      </Card>
    </div>
  </div>
);
