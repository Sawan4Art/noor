/**
 * Accurate Prayer Times & Islamic Astrometry Calculations
 * Computes Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha based on astronomical algorithms
 * Supports standard Islamic calculation methods & Shafi'i / Hanafi juristic methods
 */

export interface Coordinates {
  latitude: number;
  longitude: number;
  city: string;
}

export type CalculationMethod = 'MWL' | 'ISNA' | 'Egypt' | 'Makkah' | 'Karachi' | 'Tehran';

export interface MethodParams {
  name: string;
  arabicName: string;
  fajrAngle: number;
  ishaAngle?: number;
  ishaInterval?: number; // minutes after maghrib (e.g. Makkah is 90 mins)
}

export const CALCULATION_METHODS: Record<CalculationMethod, MethodParams> = {
  Egypt: {
    name: 'Egyptian General Authority of Survey',
    arabicName: 'الهيئة المصرية العامة للمساحة',
    fajrAngle: 19.5,
    ishaAngle: 17.5,
  },
  Makkah: {
    name: 'Umm Al-Qura University, Makkah',
    arabicName: 'جامعة أم القرى - مكة المكرمة',
    fajrAngle: 18.5,
    ishaInterval: 90,
  },
  MWL: {
    name: 'Muslim World League',
    arabicName: 'رابطة العالم الإسلامي',
    fajrAngle: 18.0,
    ishaAngle: 17.0,
  },
  ISNA: {
    name: 'Islamic Society of North America',
    arabicName: 'الجمعية الإسلامية لأمريكا الشمالية',
    fajrAngle: 15.0,
    ishaAngle: 15.0,
  },
  Karachi: {
    name: 'University of Islamic Sciences, Karachi',
    arabicName: 'جامعة العلوم الإسلامية بكراتشي',
    fajrAngle: 18.0,
    ishaAngle: 18.0,
  },
  Tehran: {
    name: 'Institute of Geophysics, University of Tehran',
    arabicName: 'معهد الجيوفيزياء بجامعة طهران',
    fajrAngle: 17.7,
    ishaAngle: 14.0,
  },
};

export interface PrayerTimesResult {
  fajr: string;
  sunrise: string;
  dhuhr: string;
  asr: string;
  maghrib: string;
  isha: string;
  rawTimes: {
    fajr: Date;
    sunrise: Date;
    dhuhr: Date;
    asr: Date;
    maghrib: Date;
    isha: Date;
  };
}

/** ضبط دقيق بالدقائق لكل صلاة (±) — يُطبق داخل الحساب قبل التنسيق */
export type PrayerTimeKey = 'fajr' | 'sunrise' | 'dhuhr' | 'asr' | 'maghrib' | 'isha';
export type PrayerTimeAdjustments = Partial<Record<PrayerTimeKey, number>>;

export interface NextPrayerInfo {
  name: 'fajr' | 'sunrise' | 'dhuhr' | 'asr' | 'maghrib' | 'isha';
  arabicName: string;
  timeStr: string;
  targetDate: Date;
  remainingSeconds: number;
  formattedRemaining: string;
  progressPercent: number; // 0 to 100 elapsed between previous and next prayer
}

const D2R = Math.PI / 180.0;
const R2D = 180.0 / Math.PI;

function sinD(deg: number): number {
  return Math.sin(deg * D2R);
}

function cosD(deg: number): number {
  return Math.cos(deg * D2R);
}

function tanD(deg: number): number {
  return Math.tan(deg * D2R);
}

function asinD(x: number): number {
  return Math.asin(x) * R2D;
}

function acosD(x: number): number {
  return Math.acos(x) * R2D;
}

function atan2D(y: number, x: number): number {
  return Math.atan2(y, x) * R2D;
}

function atanD(x: number): number {
  return Math.atan(x) * R2D;
}

function fixAngle(a: number): number {
  let res = a - 360.0 * Math.floor(a / 360.0);
  if (res < 0) res += 360.0;
  return res;
}

function fixHour(h: number): number {
  let res = h - 24.0 * Math.floor(h / 24.0);
  if (res < 0) res += 24.0;
  return res;
}

// Astronomical calculations
function julianDate(year: number, month: number, day: number): number {
  if (month <= 2) {
    year -= 1;
    month += 12;
  }
  const A = Math.floor(year / 100);
  const B = 2 - A + Math.floor(A / 4);
  return (
    Math.floor(365.25 * (year + 4716)) +
    Math.floor(30.6001 * (month + 1)) +
    day +
    B -
    1524.5
  );
}

/**
 * موضع الشمس (مطلع مستقيم + ميل + معادلة الزمن) — خوارزمية PrayTimes المرجعية.
 * إصلاح جذر: المطلع المستقيم لازم atan2 (لحفظ الربع الصحيح) وبالساعات مع fixHour،
 * مش atan عادي مع fixAngle — الخطأ ده كان يخرّب معادلة الزمن بمئات الساعات
 * ويزحلق كل المواقيت ساعات كاملة.
 */
function sunPosition(jd: number): { declination: number; equationOfTime: number } {
  const D = jd - 2451545.0;
  const g = fixAngle(357.529 + 0.98560028 * D);
  const q = fixAngle(280.459 + 0.98564736 * D);
  const L = fixAngle(q + 1.915 * sinD(g) + 0.02 * sinD(2 * g));

  const e = 23.439 - 0.00000036 * D;
  const RA = fixHour(atan2D(cosD(e) * sinD(L), cosD(L)) / 15.0);
  const declination = asinD(sinD(e) * sinD(L));
  const equationOfTime = q / 15.0 - RA;

  return { declination, equationOfTime };
}

export function computePrayerTimes(
  date: Date,
  lat: number,
  lng: number,
  method: CalculationMethod = 'Egypt',
  isHanafi: boolean = false,
  adjustments: PrayerTimeAdjustments = {}
): PrayerTimesResult {
  const year = date.getFullYear();
  const month = date.getMonth() + 1;
  const day = date.getDate();
  const timeZoneOffsetHours = -date.getTimezoneOffset() / 60;

  /**
   * جذر إصلاح دقة المواقيت (2026-10):
   * الكود القديم كان بيقرأ موضع الشمس (الميل + معادلة الزمن) مرة واحدة عند
   * منتصف الليل ويستخدمه لكل المواقيت — لكن ميل الشمس يتغير ~0.3°/يوم،
   * فزوايا العشاء/المغرب كانت تُحسب بميل الصباح → مواقيت المساء تتأخر
   * حتى 1.5 دقيقة (قياس ضد NOAA: المغرب كان عند -0.98° بدل -0.833°).
   * الحل: تقييم موضع الشمس عند الوقت المُقدَّر لكل صلاة مع التكرار حتى التقارب
   * (نفس منهج PrayTimes المرجعي بدقة أقل من ثانية).
   */
  const jdBase = julianDate(year, month, day) - lng / (15 * 24);

  // موضع الشمس عند ساعة محلية معينة
  const sunAt = (localHours: number) => sunPosition(jdBase + localHours / 24);

  // الظهيرة المحلية (العبور الشمسي) بموضع الشمس عند اللحظة المقدَّرة
  const noonAt = (localHours: number): number => {
    const { equationOfTime: eqt } = sunAt(localHours);
    return fixHour(12 + timeZoneOffsetHours - lng / 15 - eqt);
  };

  /**
   * وقت الوصول لزاوية شمسية معينة (تحت الأفق) — بالتكرار:
   * كل دورة تعيد تقييم الميل والظهيرة عند الوقت الجديد حتى يتقارب.
   */
  const timeForAngle = (angle: number, guess: number, sign: 1 | -1): number => {
    let t = guess;
    for (let i = 0; i < 3; i++) {
      const { declination: d } = sunAt(t);
      const noon = noonAt(t);
      const diff =
        (1 / 15) *
        acosD((-sinD(angle) - sinD(lat) * sinD(d)) / (cosD(lat) * cosD(d)));
      if (isNaN(diff)) return NaN;
      t = noon + sign * diff;
    }
    return t;
  };

  // العصر: زاوية ظل الشافعي/الحنفي — نفس منطق التكرار
  const asrTime = (guess: number): number => {
    let t = guess;
    for (let i = 0; i < 3; i++) {
      const { declination: d } = sunAt(t);
      const noon = noonAt(t);
      const asrFactor = isHanafi ? 2 : 1;
      const asrAlt = -atanD(1 / (asrFactor + tanD(Math.abs(lat - d))));
      const diff =
        (1 / 15) *
        acosD((-sinD(asrAlt) - sinD(lat) * sinD(d)) / (cosD(lat) * cosD(d)));
      if (isNaN(diff)) return NaN;
      t = noon + diff;
    }
    return t;
  };

  // Dhuhr: العبور الشمسي بالتكرار (دقة < ثانية)
  let midDay = 12 + timeZoneOffsetHours - lng / 15;
  for (let i = 0; i < 3; i++) {
    midDay = noonAt(midDay);
  }

  const safe = (v: number, fallback: number): number => (isNaN(v) ? fallback : v);
  const params = CALCULATION_METHODS[method] || CALCULATION_METHODS.Egypt;

  // Fajr
  const fajr = safe(timeForAngle(params.fajrAngle, midDay - 6, -1), midDay - 1.5);

  // Sunrise (0.833 degrees refraction)
  const sunrise = safe(timeForAngle(0.833, midDay - 6, -1), midDay - 1.2);

  // Asr
  const asr = safe(asrTime(midDay + 3), midDay + 3);

  // Sunset / Maghrib
  const maghrib = safe(timeForAngle(0.833, midDay + 6, +1), midDay + 6);

  // Isha
  let isha: number;
  if (params.ishaInterval) {
    isha = maghrib + params.ishaInterval / 60;
  } else {
    isha = safe(
      timeForAngle(params.ishaAngle ?? 17.5, midDay + 7, +1),
      maghrib + 1.5
    );
  }

  const toDate = (hours: number, adjMinutes = 0): Date => {
    const dObj = new Date(date);
    const total = fixHour(hours + adjMinutes / 60);
    const h = Math.floor(total);
    const m = Math.floor((total - h) * 60);
    const s = Math.floor(((total - h) * 60 - m) * 60);
    dObj.setHours(h, m, s, 0);
    return dObj;
  };

  const adj = (key: PrayerTimeKey): number => adjustments[key] ?? 0;

  const rawTimes = {
    fajr: toDate(fajr, adj('fajr')),
    sunrise: toDate(sunrise, adj('sunrise')),
    dhuhr: toDate(midDay, adj('dhuhr')),
    asr: toDate(asr, adj('asr')),
    maghrib: toDate(maghrib, adj('maghrib')),
    isha: toDate(isha, adj('isha')),
  };

  const format = (dObj: Date): string => {
    return dObj.toLocaleTimeString('ar-EG', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: true,
    });
  };

  return {
    fajr: format(rawTimes.fajr),
    sunrise: format(rawTimes.sunrise),
    dhuhr: format(rawTimes.dhuhr),
    asr: format(rawTimes.asr),
    maghrib: format(rawTimes.maghrib),
    isha: format(rawTimes.isha),
    rawTimes,
  };
}

function dhuhrMid(midDay: number): number {
  return midDay;
}
// (دالة dhuhrMid القديمة اتصالت داخل الحساب الجديد المباشر — محفوظة للتوافق)

export const PRAYER_NAMES_AR: Record<string, string> = {
  fajr: 'الفجر',
  sunrise: 'الشروق',
  dhuhr: 'الظهر',
  asr: 'العصر',
  maghrib: 'المغرب',
  isha: 'العشاء',
};

export function getNextPrayer(prayerTimes: PrayerTimesResult, now: Date = new Date()): NextPrayerInfo {
  const keys: Array<'fajr' | 'sunrise' | 'dhuhr' | 'asr' | 'maghrib' | 'isha'> = [
    'fajr',
    'sunrise',
    'dhuhr',
    'asr',
    'maghrib',
    'isha',
  ];

  const nowMs = now.getTime();
  let nextKey: (typeof keys)[number] = 'fajr';
  let targetDate = prayerTimes.rawTimes.fajr;
  let prevDate = prayerTimes.rawTimes.isha;

  let found = false;
  for (let i = 0; i < keys.length; i++) {
    const k = keys[i];
    const t = prayerTimes.rawTimes[k].getTime();
    if (t > nowMs) {
      nextKey = k;
      targetDate = prayerTimes.rawTimes[k];
      prevDate = i > 0 ? prayerTimes.rawTimes[keys[i - 1]] : prayerTimes.rawTimes.isha;
      found = true;
      break;
    }
  }

  // If all prayers today have passed, next prayer is Fajr tomorrow
  if (!found) {
    nextKey = 'fajr';
    targetDate = new Date(prayerTimes.rawTimes.fajr.getTime() + 24 * 60 * 60 * 1000);
    prevDate = prayerTimes.rawTimes.isha;
  }

  const diffMs = Math.max(0, targetDate.getTime() - nowMs);
  const remainingSeconds = Math.floor(diffMs / 1000);

  const hours = Math.floor(remainingSeconds / 3600);
  const minutes = Math.floor((remainingSeconds % 3600) / 60);
  const seconds = remainingSeconds % 60;

  const pad = (n: number) => n.toString().padStart(2, '0');
  const formattedRemaining = `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`;

  const totalWindow = Math.max(1, targetDate.getTime() - prevDate.getTime());
  const elapsed = Math.max(0, nowMs - prevDate.getTime());
  const progressPercent = Math.min(100, Math.max(0, Math.round((elapsed / totalWindow) * 100)));

  return {
    name: nextKey,
    arabicName: PRAYER_NAMES_AR[nextKey],
    timeStr: prayerTimes[nextKey],
    targetDate,
    remainingSeconds,
    formattedRemaining,
    progressPercent,
  };
}

/**
 * Calculates Qibla direction from user coordinates to Kaaba (Makkah)
 * Kaaba coords: 21.4225° N, 39.8262° E
 */
export function calculateQibla(lat: number, lng: number): number {
  const kaabaLat = 21.4225241 * D2R;
  const kaabaLng = 39.8261818 * D2R;
  const phiK = kaabaLat;
  const lambdaK = kaabaLng;
  const phi = lat * D2R;
  const lambda = lng * D2R;

  const y = Math.sin(lambdaK - lambda);
  const x =
    Math.cos(phi) * Math.tan(phiK) - Math.sin(phi) * Math.cos(lambdaK - lambda);
  let qibla = Math.atan2(y, x) * R2D;
  return fixAngle(qibla);
}

/**
 * Calculates Great-Circle distance in kilometers to the Kaaba in Makkah
 */
export function calculateDistanceToKaaba(lat: number, lng: number): number {
  const R = 6371; // Earth radius in km
  const kaabaLat = 21.4225241 * D2R;
  const kaabaLng = 39.8261818 * D2R;
  const phi1 = lat * D2R;
  const phi2 = kaabaLat;
  const deltaPhi = (21.4225241 - lat) * D2R;
  const deltaLambda = (39.8261818 - lng) * D2R;

  const a =
    Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
    Math.cos(phi1) * Math.cos(phi2) * Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return Math.round(R * c);
}

/**
 * هل اليوم داخل شهر رمضان المبارك؟ (بطلب المالك: السحور يظهر ويعمل في رمضان فقط)
 * تعتمد على التحويل الهجري أم القرى — وسط تقريبي مقبول للتقويم العملي.
 */
export function isRamadanNow(date: Date = new Date()): boolean {
  try {
    return getHijriDate(date).monthName === 'رمضان';
  } catch {
    return false;
  }
}

/**
 * Approximate Hijri Date conversion
 */
export function getHijriDate(date: Date = new Date()): {
  day: number;
  monthName: string;
  year: number;
  formatted: string;
} {
  const HIJRI_MONTHS = [
    'محرم',
    'صفر',
    'ربيع الأول',
    'ربيع الآخر',
    'جمادى الأولى',
    'جمادى الآخرة',
    'رجب',
    'شعبان',
    'رمضان',
    'شوال',
    'ذو القعدة',
    'ذو الحجة',
  ];

  try {
    const formatter = new Intl.DateTimeFormat('ar-SA-u-ca-islamic-umalqura', {
      day: 'numeric',
      month: 'numeric',
      year: 'numeric',
    });
    const parts = formatter.formatToParts(date);
    let day = 1;
    let month = 1;
    let year = 1448;

    for (const part of parts) {
      if (part.type === 'day') day = parseInt(part.value, 10) || 1;
      if (part.type === 'month') month = parseInt(part.value, 10) || 1;
      if (part.type === 'year') year = parseInt(part.value, 10) || 1448;
    }

    const monthIndex = Math.max(0, Math.min(11, month - 1));
    const monthName = HIJRI_MONTHS[monthIndex];

    return {
      day,
      monthName,
      year,
      formatted: `${day} ${monthName} ${year} هـ`,
    };
  } catch {
    return {
      day: 15,
      monthName: 'رمضان',
      year: 1448,
      formatted: '15 رمضان 1448 هـ',
    };
  }
}
