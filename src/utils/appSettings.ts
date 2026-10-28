/**
 * إعدادات التطبيق المحفوظة — طبعة المصحف، القارئ المفضل، ضبط المواقيت الدقيق.
 * كلها في localStorage مع fallbackات آمنة (قاعدة الميثاق: fallback دايماً).
 */
import { AyahReciter, AYAH_RECITERS } from './quranTajweedData';
import { PrayerTimeAdjustments, PrayerTimeKey } from './prayerMath';

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
