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

/* ==================== أذونات أول تشغيل ==================== */

const PERMISSIONS_ASKED_KEY = 'noor_permissions_asked';

export function getPermissionsAsked(): boolean {
  try {
    return localStorage.getItem(PERMISSIONS_ASKED_KEY) === 'yes';
  } catch {
    return false;
  }
}

export function savePermissionsAsked(): void {
  try {
    localStorage.setItem(PERMISSIONS_ASKED_KEY, 'yes');
  } catch {
    // تجاهل
  }
}
