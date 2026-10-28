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

function sunPosition(jd: number): { declination: number; equationOfTime: number } {
  const D = jd - 2451545.0;
  const g = fixAngle(357.529 + 0.98560028 * D);
  const q = fixAngle(280.459 + 0.98564736 * D);
  const L = fixAngle(q + 1.915 * sinD(g) + 0.02 * sinD(2 * g));

  const e = 23.439 - 0.00000036 * D;
  const RA = fixAngle(atanD(cosD(e) * sinD(L)) / 15.0);
  const declination = asinD(sinD(e) * sinD(L));
  const equationOfTime = q / 15.0 - RA;

  return { declination, equationOfTime };
}

export function computePrayerTimes(
  date: Date,
  lat: number,
  lng: number,
  method: CalculationMethod = 'Egypt',
  isHanafi: boolean = false
): PrayerTimesResult {
  const year = date.getFullYear();
  const month = date.getMonth() + 1;
  const day = date.getDate();
  const timeZoneOffsetHours = -date.getTimezoneOffset() / 60;

  const jd = julianDate(year, month, day);
  const { declination: d, equationOfTime: eqt } = sunPosition(jd);

  const methodParams = CALCULATION_METHODS[method] || CALCULATION_METHODS.Egypt;

  // Dhuhr is mid-day
  const midDay = fixHour(12 + timeZoneOffsetHours - lng / 15 - eqt);

  // Fajr
  const fajrDiff =
    (1 / 15) *
    acosD(
      (-sinD(methodParams.fajrAngle) - sinD(lat) * sinD(d)) /
        (cosD(lat) * cosD(d))
    );
  const fajr = isNaN(fajrDiff) ? midDay - 1.5 : midDay - fajrDiff;

  // Sunrise (0.833 degrees refraction)
  const sunriseDiff =
    (1 / 15) *
    acosD((-sinD(0.833) - sinD(lat) * sinD(d)) / (cosD(lat) * cosD(d)));
  const sunrise = isNaN(sunriseDiff) ? midDay - 1.2 : midDay - sunriseDiff;

  // Asr
  const asrFactor = isHanafi ? 2 : 1;
  const asrAlt = -atanD(1 / (asrFactor + tanD(Math.abs(lat - d))));
  const asrDiff =
    (1 / 15) *
    acosD((-sinD(asrAlt) - sinD(lat) * sinD(d)) / (cosD(lat) * cosD(d)));
  const asr = isNaN(asrDiff) ? midDay + 3 : midDay + asrDiff;

  // Sunset / Maghrib
  const sunsetDiff =
    (1 / 15) *
    acosD((-sinD(0.833) - sinD(lat) * sinD(d)) / (cosD(lat) * cosD(d)));
  const maghrib = isNaN(sunsetDiff) ? midDay + 6 : midDay + sunsetDiff;

  // Isha
  let isha = midDay + 7.5;
  if (methodParams.ishaInterval) {
    isha = maghrib + methodParams.ishaInterval / 60;
  } else if (methodParams.ishaAngle) {
    const ishaDiff =
      (1 / 15) *
      acosD(
        (-sinD(methodParams.ishaAngle) - sinD(lat) * sinD(d)) /
          (cosD(lat) * cosD(d))
      );
    isha = isNaN(ishaDiff) ? maghrib + 1.5 : midDay + ishaDiff;
  }

  const toDate = (hours: number): Date => {
    const dObj = new Date(date);
    const h = Math.floor(fixHour(hours));
    const m = Math.floor((fixHour(hours) - h) * 60);
    const s = Math.floor(((fixHour(hours) - h) * 60 - m) * 60);
    dObj.setHours(h, m, s, 0);
    return dObj;
  };

  const rawTimes = {
    fajr: toDate(fajr),
    sunrise: toDate(sunrise),
    dhuhr: toDate(dhuhrMid(midDay)),
    asr: toDate(asr),
    maghrib: toDate(maghrib),
    isha: toDate(isha),
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
