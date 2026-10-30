/**
 * إعدادات التطبيق المحفوظة — طبعة المصحف، القارئ المفضل، ضبط المواقيت الدقيق،
 * الموقع، طريقة الحساب، المذهب، الأذان، وأنماط التنبيه لكل صلاة.
 * كلها في localStorage مع fallbackات آمنة (قاعدة الميثاق: fallback دايماً).
 */
import { AyahReciter, AYAH_RECITERS } from './quranTajweedData';
import { PrayerTimeAdjustments, PrayerTimeKey, CalculationMethod, Coordinates } from './prayerMath';

/* ============================ طبعات المصحف ============================ */

/** الطبعات الثلاث: تجويد ملون / رسم عثماني أبيض / مصحف ليلي */
export type MushafEdition = 'tajweed' | 'uthmani' | 'night';

export interface MushafEditionStyle {
  id: MushafEdition;
  label: string;
  description: string;
  /** ورق الصفحة */
  paper: string;
  /** لون الحبر الأساسي للنص */
  ink: string;
  /** الإطار الخارجي الذهبي */
  frame: string;
  /** الإطار الداخلي */
  innerFrame: string;
  /** لون رقم الآية */
  ayahNumberColor: string;
  /** تشغيل التلوين التجويدي */
  showTajweed: boolean;
}

export const MUSHAF_EDITIONS: Record<MushafEdition, MushafEditionStyle> = {
  tajweed: {
    id: 'tajweed',
    label: 'التجويد الملون',
    description: 'ورق عاجي بألوان أحكام التجويد',
    paper: '#fffdf5',
    ink: '#0f172a',
    frame: '#dfbe72',
    innerFrame: '#3b8270',
    ayahNumberColor: '#8c6b23',
    showTajweed: true,
  },
  uthmani: {
    id: 'uthmani',
    label: 'الرسم العثماني',
    description: 'ورق أبيض ونص أسود كلاسيكي',
    paper: '#ffffff',
    ink: '#111827',
    frame: '#b45309',
    innerFrame: '#92400e',
    ayahNumberColor: '#92400e',
    showTajweed: false,
  },
  night: {
    id: 'night',
    label: 'المصحف الليلي',
    description: 'ورق داكن مريح للعين بقراءة الليل',
    paper: '#131a22',
    ink: '#e8e3d5',
    frame: '#3f4a56',
    innerFrame: '#2b333c',
    ayahNumberColor: '#c9a227',
    showTajweed: true,
  },
};

const EDITION_KEY = 'noor_mushaf_edition';

export function getSavedEdition(): MushafEditionStyle {
  try {
    const saved = localStorage.getItem(EDITION_KEY) as MushafEdition | null;
    if (saved && MUSHAF_EDITIONS[saved]) return MUSHAF_EDITIONS[saved];
  } catch {
    // localStorage غير متاح
  }
  return MUSHAF_EDITIONS.tajweed;
}

export function saveEdition(edition: MushafEdition): void {
  try {
    localStorage.setItem(EDITION_KEY, edition);
  } catch {
    // تجاهل بصمت
  }
}

/* ============================ القارئ المفضل ============================ */

const RECITER_KEY = 'noor_preferred_reciter';

export function getSavedAyahReciter(): AyahReciter {
  try {
    const saved = localStorage.getItem(RECITER_KEY);
    const found = AYAH_RECITERS.find((r) => r.id === saved);
    return found || AYAH_RECITERS[0];
  } catch {
    return AYAH_RECITERS[0];
  }
}

export function saveAyahReciter(reciterId: string): void {
  try {
    localStorage.setItem(RECITER_KEY, reciterId);
  } catch {
    // تجاهل بصمت
  }
}

/* ==================== الضبط الدقيق لمواقيت الصلاة ==================== */

const ADJUSTMENTS_KEY = 'noor_prayer_adjustments';

export const ADJUSTABLE_PRAYERS: Array<{ key: PrayerTimeKey; label: string }> = [
  { key: 'fajr', label: 'الفجر' },
  { key: 'sunrise', label: 'الشروق' },
  { key: 'dhuhr', label: 'الظهر' },
  { key: 'asr', label: 'العصر' },
  { key: 'maghrib', label: 'المغرب' },
  { key: 'isha', label: 'العشاء' },
];

export function getSavedPrayerAdjustments(): PrayerTimeAdjustments {
  try {
    const raw = localStorage.getItem(ADJUSTMENTS_KEY);
    if (!raw) return {};
    const parsed = JSON.parse(raw) as PrayerTimeAdjustments;
    // تنقية: أرقام صحيحة معقولة فقط (من -30 إلى +30 دقيقة)
    const clean: PrayerTimeAdjustments = {};
    for (const { key } of ADJUSTABLE_PRAYERS) {
      const v = parsed[key];
      if (typeof v === 'number' && Number.isFinite(v) && Math.abs(v) <= 30) {
        clean[key] = Math.round(v);
      }
    }
    return clean;
  } catch {
    return {};
  }
}

export function savePrayerAdjustments(adj: PrayerTimeAdjustments): void {
  try {
    localStorage.setItem(ADJUSTMENTS_KEY, JSON.stringify(adj));
  } catch {
    // تجاهل بصمت
  }
}

/* ==================== الموقع المحفوظ ==================== */

const LOCATION_KEY = 'noor_location';

export function getSavedLocation(): Coordinates {
  try {
    const raw = localStorage.getItem(LOCATION_KEY);
    if (raw) {
      const parsed = JSON.parse(raw) as Partial<Coordinates>;
      if (
        typeof parsed.latitude === 'number' && Number.isFinite(parsed.latitude) &&
        typeof parsed.longitude === 'number' && Number.isFinite(parsed.longitude)
      ) {
        return {
          city: typeof parsed.city === 'string' && parsed.city ? parsed.city : 'موقعي',
          latitude: parsed.latitude,
          longitude: parsed.longitude,
        };
      }
    }
  } catch {
    // تجاهل
  }
  return { city: 'القاهرة، مصر', latitude: 30.0444, longitude: 31.2357 };
}

export function saveLocation(coords: Coordinates): void {
  try {
    localStorage.setItem(LOCATION_KEY, JSON.stringify(coords));
  } catch {
    // تجاهل
  }
}

/* ==================== طريقة الحساب والمذهب ==================== */

const METHOD_KEY = 'noor_calc_method';
const MADHAB_KEY = 'noor_asr_madhab';

const VALID_METHODS: CalculationMethod[] = ['MWL', 'ISNA', 'Egypt', 'Makkah', 'Karachi', 'Tehran'];

export function getSavedMethod(): CalculationMethod {
  try {
    const saved = localStorage.getItem(METHOD_KEY) as CalculationMethod | null;
    if (saved && VALID_METHODS.includes(saved)) return saved;
  } catch {
    // تجاهل
  }
  return 'Egypt';
}

export function saveMethod(method: CalculationMethod): void {
  try {
    localStorage.setItem(METHOD_KEY, method);
  } catch {
    // تجاهل
  }
}

export function getSavedMadhab(): 'shafi' | 'hanafi' {
  try {
    return localStorage.getItem(MADHAB_KEY) === 'hanafi' ? 'hanafi' : 'shafi';
  } catch {
    return 'shafi';
  }
}

export function saveMadhab(madhab: 'shafi' | 'hanafi'): void {
  try {
    localStorage.setItem(MADHAB_KEY, madhab);
  } catch {
    // تجاهل
  }
}

/* ==================== الأذان وأصواته ==================== */

export interface AdhanVoice {
  id: string;
  name: string;
  location: string;
}

/**
 * أصوات الأذان المدمجة داخل التطبيق (تعمل بدون إنترنت):
 * ملفات الصوت في android/app/src/main/res/raw (للنيتِف)
 * وفي public/assets/adhan (لمعاينة الويب وداخل التطبيق).
 * المصدر: ويكيميديا كومنز — تسجيلات حرة مشاعة.
 */
export const ADHAN_VOICES: AdhanVoice[] = [
  { id: 'main', name: 'الأذان الكامل الخاشع', location: 'صوت واضح ومؤثر' },
  { id: 'fakhry', name: 'أذان المؤذن صباح فخري', location: 'أداء تقليدي عريق' },
  { id: 'traditional', name: 'الأذان التقليدي', location: 'مؤذن تقليدي' },
  { id: 'beautiful', name: 'أذان هادئ جميل', location: 'صوت عذب مطمئن' },
];

/** مسار ملف صوت الأذان للمعاينة (يعمل داخل APK وعلى الويب) */
export function getAdhanAudioSrc(voiceId: string): string {
  const id = ADHAN_VOICES.some((v) => v.id === voiceId) ? voiceId : 'main';
  return `/assets/adhan/${id}.${id === 'main' || id === 'fakhry' ? 'mp3' : 'ogg'}`;
}

const ADHAN_VOICE_KEY = 'noor_adhan_voice';
const ADHAN_ENABLED_KEY = 'noor_adhan_enabled';

export function getSavedAdhanVoice(): AdhanVoice {
  try {
    const saved = localStorage.getItem(ADHAN_VOICE_KEY);
    const found = ADHAN_VOICES.find((v) => v.id === saved);
    return found ?? ADHAN_VOICES[0];
  } catch {
    return ADHAN_VOICES[0];
  }
}

export function saveAdhanVoice(id: string): void {
  try {
    localStorage.setItem(ADHAN_VOICE_KEY, id);
  } catch {
    // تجاهل
  }
}

export function getSavedAdhanEnabled(): boolean {
  try {
    return localStorage.getItem(ADHAN_ENABLED_KEY) !== 'off';
  } catch {
    return true;
  }
}

export function saveAdhanEnabled(on: boolean): void {
  try {
    localStorage.setItem(ADHAN_ENABLED_KEY, on ? 'on' : 'off');
  } catch {
    // تجاهل
  }
}

/** نمط التنبيه لكل صلاة: أذان صوتي / إشعار صامت / مطفأ */
export type PrayerAlertMode = 'adhan' | 'notification' | 'off';
export type PrayerAlerts = Record<PrayerTimeKey, PrayerAlertMode>;

const ALERTS_KEY = 'noor_prayer_alerts';

export const DEFAULT_PRAYER_ALERTS: PrayerAlerts = {
  fajr: 'adhan',
  sunrise: 'notification',
  dhuhr: 'adhan',
  asr: 'adhan',
  maghrib: 'adhan',
  isha: 'adhan',
};

export function getSavedPrayerAlerts(): PrayerAlerts {
  try {
    const raw = localStorage.getItem(ALERTS_KEY);
    if (raw) {
      const parsed = JSON.parse(raw) as Partial<PrayerAlerts>;
      const clean = { ...DEFAULT_PRAYER_ALERTS };
      for (const key of Object.keys(DEFAULT_PRAYER_ALERTS) as PrayerTimeKey[]) {
        const v = parsed[key];
        if (v === 'adhan' || v === 'notification' || v === 'off') clean[key] = v;
      }
      return clean;
    }
  } catch {
    // تجاهل
  }
  return { ...DEFAULT_PRAYER_ALERTS };
}

export function savePrayerAlerts(alerts: PrayerAlerts): void {
  try {
    localStorage.setItem(ALERTS_KEY, JSON.stringify(alerts));
  } catch {
    // تجاهل
  }
}

/* ==================== قيام الليل (اختياري — وضعان) ==================== */

/** وضع تنبيه قيام الليل: قبل الفجر أو بعد العشاء (بطلب المالك 2026-10-29) */
export type QiyamMode = 'before_fajr' | 'after_isha';

const QIYAM_ENABLED_KEY = 'noor_qiyam_enabled';
const QIYAM_MODE_KEY = 'noor_qiyam_mode';
const QIYAM_OFFSET_KEY = 'noor_qiyam_offset_minutes';

/** الدقائق المسموحة لتنبيه قيام الليل (قبل الفجر أو بعد العشاء) */
export const QIYAM_OFFSET_OPTIONS = [15, 30, 60, 90, 120] as const;

export function getSavedQiyamEnabled(): boolean {
  try {
    return localStorage.getItem(QIYAM_ENABLED_KEY) === 'on';
  } catch {
    return false;
  }
}

export function saveQiyamEnabled(on: boolean): void {
  try {
    localStorage.setItem(QIYAM_ENABLED_KEY, on ? 'on' : 'off');
  } catch {
    // تجاهل
  }
}

export function getSavedQiyamMode(): QiyamMode {
  try {
    const saved = localStorage.getItem(QIYAM_MODE_KEY);
    if (saved === 'after_isha') return 'after_isha';
  } catch {
    // تجاهل
  }
  return 'before_fajr';
}

export function saveQiyamMode(mode: QiyamMode): void {
  try {
    localStorage.setItem(QIYAM_MODE_KEY, mode);
  } catch {
    // تجاهل
  }
}

/** كم دقيقة قبل الفجر/بعد العشاء ينبه لقيام الليل */
export function getSavedQiyamOffsetMinutes(): number {
  try {
    const v = parseInt(localStorage.getItem(QIYAM_OFFSET_KEY) ?? '', 10);
    if (QIYAM_OFFSET_OPTIONS.includes(v as (typeof QIYAM_OFFSET_OPTIONS)[number])) return v;
  } catch {
    // تجاهل
  }
  return 60;
}

export function saveQiyamOffsetMinutes(minutes: number): void {
  try {
    if (QIYAM_OFFSET_OPTIONS.includes(minutes as (typeof QIYAM_OFFSET_OPTIONS)[number])) {
      localStorage.setItem(QIYAM_OFFSET_KEY, String(minutes));
    }
  } catch {
    // تجاهل
  }
}

/* ==================== السحور (رمضان فقط) ==================== */

/**
 * السحور يظهر ويعمل في رمضان فقط — بطلب المالك:
 * إلغاء الإمساك نهائياً + تنبيه السحور اختياري افتراضه قبل الفجر بـ15 دقيقة.
 */
const SUHOOR_ENABLED_KEY = 'noor_suhoor_enabled';
const SUHOOR_OFFSET_KEY = 'noor_suhoor_offset_minutes';

export const SUHOOR_OFFSET_OPTIONS = [10, 15, 20, 30, 60] as const;

export function getSavedSuhoorEnabled(): boolean {
  try {
    return localStorage.getItem(SUHOOR_ENABLED_KEY) !== 'off';
  } catch {
    return true;
  }
}

export function saveSuhoorEnabled(on: boolean): void {
  try {
    localStorage.setItem(SUHOOR_ENABLED_KEY, on ? 'on' : 'off');
  } catch {
    // تجاهل
  }
}

export function getSavedSuhoorOffsetMinutes(): number {
  try {
    const v = parseInt(localStorage.getItem(SUHOOR_OFFSET_KEY) ?? '', 10);
    if (SUHOOR_OFFSET_OPTIONS.includes(v as (typeof SUHOOR_OFFSET_OPTIONS)[number])) return v;
  } catch {
    // تجاهل
  }
  return 15;
}

export function saveSuhoorOffsetMinutes(minutes: number): void {
  try {
    if (SUHOOR_OFFSET_OPTIONS.includes(minutes as (typeof SUHOOR_OFFSET_OPTIONS)[number])) {
      localStorage.setItem(SUHOOR_OFFSET_KEY, String(minutes));
    }
  } catch {
    // تجاهل
  }
}

/* ==================== الإقامة (أذان الإقامة) ==================== */

/**
 * الإقامة: تنبيه صوتي بصوت الإقامة بعد الأذان بمدة يحددها المستخدم،
 * وقابل للتعطيل كلياً — بطلب المالك 2026-10-29.
 */
const IQAMAH_ENABLED_KEY = 'noor_iqamah_enabled';
const IQAMAH_OFFSET_KEY = 'noor_iqamah_offset_minutes';
const IQAMAH_VOICE_KEY = 'noor_iqamah_voice';

export const IQAMAH_OFFSET_OPTIONS = [5, 10, 15, 20, 30] as const;

export interface IqamahVoice {
  id: string;
  name: string;
  location: string;
}

export const IQAMAH_VOICES: IqamahVoice[] = [
  { id: 'makkah', name: 'إقامة الحرم المكي', location: 'الشيخ أحمد بسناوي — مكة المكرمة' },
  { id: 'beautiful', name: 'إقامة خاشعة جميلة', location: 'أداء مؤثر مطول' },
];

export function getSavedIqamahEnabled(): boolean {
  try {
    return localStorage.getItem(IQAMAH_ENABLED_KEY) === 'on';
  } catch {
    return false;
  }
}

export function saveIqamahEnabled(on: boolean): void {
  try {
    localStorage.setItem(IQAMAH_ENABLED_KEY, on ? 'on' : 'off');
  } catch {
    // تجاهل
  }
}

export function getSavedIqamahOffsetMinutes(): number {
  try {
    const v = parseInt(localStorage.getItem(IQAMAH_OFFSET_KEY) ?? '', 10);
    if (IQAMAH_OFFSET_OPTIONS.includes(v as (typeof IQAMAH_OFFSET_OPTIONS)[number])) return v;
  } catch {
    // تجاهل
  }
  return 15;
}

export function saveIqamahOffsetMinutes(minutes: number): void {
  try {
    if (IQAMAH_OFFSET_OPTIONS.includes(minutes as (typeof IQAMAH_OFFSET_OPTIONS)[number])) {
      localStorage.setItem(IQAMAH_OFFSET_KEY, String(minutes));
    }
  } catch {
    // تجاهل
  }
}

export function getSavedIqamahVoice(): IqamahVoice {
  try {
    const saved = localStorage.getItem(IQAMAH_VOICE_KEY);
    const found = IQAMAH_VOICES.find((v) => v.id === saved);
    return found ?? IQAMAH_VOICES[0];
  } catch {
    return IQAMAH_VOICES[0];
  }
}

export function saveIqamahVoice(id: string): void {
  try {
    localStorage.setItem(IQAMAH_VOICE_KEY, id);
  } catch {
    // تجاهل
  }
}

/** مسار صوت الإقامة للمعاينة (يعمل داخل APK وعلى الويب) */
export function getIqamahAudioSrc(voiceId: string): string {
  const id = IQAMAH_VOICES.some((v) => v.id === voiceId) ? voiceId : 'makkah';
  return `/assets/adhan/iqamah_${id}.ogg`;
}

/* ==================== كتم كل أصوات الهاتف مع الأذان ==================== */

/**
 * كتم الأصوات: يبدأ مع الأذان وينتهي بعد الإقامة بمدة قابلة للتعديل
 * (افتراضي 10 دقائق بعد الإقامة) — اختياري من الإعدادات.
 */
const MUTE_ENABLED_KEY = 'noor_mute_enabled';
const MUTE_MINUTES_KEY = 'noor_mute_minutes';

export const MUTE_MINUTES_OPTIONS = [5, 10, 15, 20, 30, 45, 60] as const;

export function getSavedMuteEnabled(): boolean {
  try {
    return localStorage.getItem(MUTE_ENABLED_KEY) === 'on';
  } catch {
    return false;
  }
}

export function saveMuteEnabled(on: boolean): void {
  try {
    localStorage.setItem(MUTE_ENABLED_KEY, on ? 'on' : 'off');
  } catch {
    // تجاهل
  }
}

export function getSavedMuteMinutes(): number {
  try {
    const v = parseInt(localStorage.getItem(MUTE_MINUTES_KEY) ?? '', 10);
    if (MUTE_MINUTES_OPTIONS.includes(v as (typeof MUTE_MINUTES_OPTIONS)[number])) return v;
  } catch {
    // تجاهل
  }
  return 10;
}

export function saveMuteMinutes(minutes: number): void {
  try {
    if (MUTE_MINUTES_OPTIONS.includes(minutes as (typeof MUTE_MINUTES_OPTIONS)[number])) {
      localStorage.setItem(MUTE_MINUTES_KEY, String(minutes));
    }
  } catch {
    // تجاهل
  }
}

/* ==================== تذكير الصلاة على النبي ﷺ ==================== */

/**
 * تذكير دوري بصوت «الصلاة على النبي» كل فترة يحددها المستخدم
 * (افتراضي كل 30 دقيقة) — قابل للتعطيل كلياً.
 */
const SALAWAT_ENABLED_KEY = 'noor_salawat_enabled';
const SALAWAT_INTERVAL_KEY = 'noor_salawat_interval_minutes';

export const SALAWAT_INTERVAL_OPTIONS = [15, 30, 60, 120] as const;

export function getSavedSalawatEnabled(): boolean {
  try {
    return localStorage.getItem(SALAWAT_ENABLED_KEY) === 'on';
  } catch {
    return false;
  }
}

export function saveSalawatEnabled(on: boolean): void {
  try {
    localStorage.setItem(SALAWAT_ENABLED_KEY, on ? 'on' : 'off');
  } catch {
    // تجاهل
  }
}

export function getSavedSalawatIntervalMinutes(): number {
  try {
    const v = parseInt(localStorage.getItem(SALAWAT_INTERVAL_KEY) ?? '', 10);
    if (SALAWAT_INTERVAL_OPTIONS.includes(v as (typeof SALAWAT_INTERVAL_OPTIONS)[number])) return v;
  } catch {
    // تجاهل
  }
  return 30;
}

export function saveSalawatIntervalMinutes(minutes: number): void {
  try {
    if (SALAWAT_INTERVAL_OPTIONS.includes(minutes as (typeof SALAWAT_INTERVAL_OPTIONS)[number])) {
      localStorage.setItem(SALAWAT_INTERVAL_KEY, String(minutes));
    }
  } catch {
    // تجاهل
  }
}

/* ==================== تخصيص الويدجت الكامل ==================== */

/**
 * تخصيص كلي لويدجت شاشة الرئيسية — بطلب المالك:
 * لون التمييز، حجم الخط، إظهار/إخفاء كل عنصر، شفافية الخلفية، ونظام الوقت.
 * تُحفظ في localStorage للتطبيق وتُمرَّر نيتِفياً للويدجت مع كل تحديث.
 */
export type WidgetAccent = 'green' | 'gold' | 'sky' | 'violet' | 'rose';
export type WidgetTextSize = 'small' | 'medium' | 'large';
export type WidgetTimeFormat = '12h' | '24h';

export interface WidgetSettings {
  /** لون التمييز (العناوين والأوقات والعداد) */
  accent: WidgetAccent;
  /** حجم الخط العام */
  textSize: WidgetTextSize;
  /** إظهار اسم التطبيق في الترويسة */
  showAppName: boolean;
  /** إظهار المدينة */
  showCity: boolean;
  /** إظهار الصلاة القادمة */
  showNextPrayer: boolean;
  /** إظهار العداد التنازلي */
  showCountdown: boolean;
  /** إظهار صف المواقيت الستة */
  showTimesRow: boolean;
  /** شفافية خلفية الويدجت الداكن (0-100) — النسخة الشفافة بلا خلفية أصلاً */
  bgOpacity: number;
  /** نظام الوقت: 12 ساعة ص/م أو 24 ساعة */
  timeFormat: WidgetTimeFormat;
}

export const WIDGET_ACCENTS: Array<{ id: WidgetAccent; label: string; color: string }> = [
  { id: 'green', label: 'أخضر زمردي', color: '#6ee7b7' },
  { id: 'gold', label: 'ذهبي', color: '#fbbf24' },
  { id: 'sky', label: 'سماوي', color: '#7dd3fc' },
  { id: 'violet', label: 'بنفسجي', color: '#c4b5fd' },
  { id: 'rose', label: 'وردي', color: '#fda4af' },
];

export const WIDGET_TEXT_SIZES: Array<{ id: WidgetTextSize; label: string; scale: number }> = [
  { id: 'small', label: 'صغير', scale: 0.85 },
  { id: 'medium', label: 'متوسط', scale: 1.0 },
  { id: 'large', label: 'كبير', scale: 1.2 },
];

export const WIDGET_BG_OPACITY_OPTIONS = [0, 25, 50, 75, 100] as const;

const WIDGET_SETTINGS_KEY = 'noor_widget_settings';

export const DEFAULT_WIDGET_SETTINGS: WidgetSettings = {
  accent: 'green',
  textSize: 'medium',
  showAppName: true,
  showCity: true,
  showNextPrayer: true,
  showCountdown: true,
  showTimesRow: true,
  bgOpacity: 100,
  timeFormat: '12h',
};

/** تنقية إعدادات الويدجت القادمة من التخزين أو من النيتِف — أي قيمة غريبة ترجع للافتراضي */
export function sanitizeWidgetSettings(raw: unknown): WidgetSettings {
  const d = DEFAULT_WIDGET_SETTINGS;
  if (!raw || typeof raw !== 'object') return { ...d };
  const r = raw as Partial<WidgetSettings>;
  const accent = WIDGET_ACCENTS.some((a) => a.id === r.accent) ? r.accent! : d.accent;
  const textSize = WIDGET_TEXT_SIZES.some((s) => s.id === r.textSize) ? r.textSize! : d.textSize;
  const timeFormat: WidgetTimeFormat = r.timeFormat === '24h' ? '24h' : '12h';
  const bool = (v: unknown, fallback: boolean): boolean => (typeof v === 'boolean' ? v : fallback);
  let bgOpacity = typeof r.bgOpacity === 'number' ? Math.round(r.bgOpacity) : d.bgOpacity;
  if (!Number.isFinite(bgOpacity) || bgOpacity < 0) bgOpacity = d.bgOpacity;
  if (bgOpacity > 100) bgOpacity = 100;
  // تقريب لأقرب خيار متاح (0/25/50/75/100)
  bgOpacity = WIDGET_BG_OPACITY_OPTIONS.reduce((best, opt) =>
    Math.abs(opt - bgOpacity) < Math.abs(best - bgOpacity) ? opt : best,
  );
  return {
    accent,
    textSize,
    showAppName: bool(r.showAppName, d.showAppName),
    showCity: bool(r.showCity, d.showCity),
    showNextPrayer: bool(r.showNextPrayer, d.showNextPrayer),
    showCountdown: bool(r.showCountdown, d.showCountdown),
    showTimesRow: bool(r.showTimesRow, d.showTimesRow),
    bgOpacity,
    timeFormat,
  };
}

export function getSavedWidgetSettings(): WidgetSettings {
  try {
    const raw = localStorage.getItem(WIDGET_SETTINGS_KEY);
    if (raw) return sanitizeWidgetSettings(JSON.parse(raw));
  } catch {
    // تجاهل
  }
  return { ...DEFAULT_WIDGET_SETTINGS };
}

export function saveWidgetSettings(settings: WidgetSettings): void {
  try {
    localStorage.setItem(WIDGET_SETTINGS_KEY, JSON.stringify(sanitizeWidgetSettings(settings)));
  } catch {
    // تجاهل
  }
}
