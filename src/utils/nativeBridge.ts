/**
 * جسر كاباسيتور للنيتِف أندرويد — نور المسلم.
 * كل الدوال ترجع fallback آمن على الويب (لا شيء يحدث) وعلى أي خطأ نيتِف.
 *
 * المتاح نيتِفياً (NoorNativePlugin.java):
 * - requestAllPermissions: الموقع + الإشعارات (+ فحص المنبهات الدقيقة)
 * - scheduleAdhanAlarms: منبهات أذان دقيقة تعمل والشاشة مطفية
 * - cancelAdhanAlarms: إلغاء كل المنبهات
 * - updateWidget: تحديث ويدجت شاشة الرئيسية ببيانات المواقيت
 */
import { Capacitor, registerPlugin } from '@capacitor/core';
import { PrayerAlerts } from './appSettings';

export interface AdhanAlarm {
  /** مفتاح الصلاة: fajr/dhuhr/... */
  key: string;
  /** الاسم العربي المعروض */
  name: string;
  /** وقت الصلاة بالميلي ثانية (epoch) */
  ts: number;
  /** نمط التنبيه */
  mode: 'adhan' | 'notification' | 'off';
}

export interface WidgetData {
  city: string;
  /** التاريخ الهجري المختصر */
  hijri: string;
  /** مواقيت اليوم: مفتاح -> نص الوقت HH:MM */
  times: Record<string, string>;
  /** أسماء الصلوات بالترتيب مع مفاتيحها */
  order: Array<{ key: string; name: string; time: string }>;
  /** الصلاة القادمة */
  next: { key: string; name: string; ts: number; time: string };
  /** فرق التوقيت المحلي بالدقائق عن UTC (لحساب العداد) */
  tzOffsetMinutes: number;
}

interface NoorNativePlugin {
  requestAllPermissions(): Promise<{ location: boolean; notifications: boolean; exactAlarms: boolean }>;
  scheduleAdhanAlarms(options: { alarms: AdhanAlarm[]; voice: string; city: string }): Promise<void>;
  cancelAdhanAlarms(): Promise<void>;
  updateWidget(options: { data: WidgetData }): Promise<void>;
  openExactAlarmSettings(): Promise<void>;
  openAppSettings(): Promise<void>;
}

const NoorNative = registerPlugin<NoorNativePlugin>('NoorNative');

export function isNativeApp(): boolean {
  return Capacitor.isNativePlatform();
}

/** طلب كل الأذونات دفعة واحدة (أول تشغيل أو من الإعدادات) */
export async function requestAllPermissions(): Promise<{
  location: boolean;
  notifications: boolean;
  exactAlarms: boolean;
}> {
  if (!isNativeApp()) return { location: true, notifications: true, exactAlarms: true };
  try {
    return await NoorNative.requestAllPermissions();
  } catch {
    return { location: false, notifications: false, exactAlarms: false };
  }
}

/** جدولة منبهات الأذان القادمة — تُستدعى كل ما تغيرت المواقيت/الإعدادات */
export async function scheduleAdhanAlarms(alarms: AdhanAlarm[], voice: string, city: string): Promise<void> {
  if (!isNativeApp() || alarms.length === 0) return;
  try {
    await NoorNative.scheduleAdhanAlarms({ alarms, voice, city });
  } catch {
    // fallback صامت — الأذان داخل التطبيق يظل يعمل
  }
}

export async function cancelAdhanAlarms(): Promise<void> {
  if (!isNativeApp()) return;
  try {
    await NoorNative.cancelAdhanAlarms();
  } catch {
    // تجاهل
  }
}

/** تحديث ويدجت الشاشة الرئيسية ببيانات مواقيت اليوم */
export async function updateWidget(data: WidgetData): Promise<void> {
  if (!isNativeApp()) return;
  try {
    await NoorNative.updateWidget({ data });
  } catch {
    // تجاهل
  }
}

export async function openExactAlarmSettings(): Promise<void> {
  if (!isNativeApp()) return;
  try {
    await NoorNative.openExactAlarmSettings();
  } catch {
    // تجاهل
  }
}

/** فتح صفحة إعدادات التطبيق في النظام (للأذونات المرفوضة نهائياً) */
export async function openAppSettings(): Promise<void> {
  if (!isNativeApp()) return;
  try {
    await NoorNative.openAppSettings();
  } catch {
    // تجاهل
  }
}

/** تحويل أنماط التنبيه + المواقيت إلى قائمة منبهات للنيتِف */
export function buildAlarmsFromAlerts(
  upcoming: Array<{ key: string; name: string; ts: number }>,
  alerts: PrayerAlerts,
): AdhanAlarm[] {
  return upcoming
    .map((p) => {
      // قيام الليل وأي مفتاح خاص: إشعار دائماً (مش أذان)
      const alertMode = alerts[p.key as keyof PrayerAlerts];
      const mode: 'adhan' | 'notification' | 'off' =
        p.key === 'qiyam' ? 'notification' : (alertMode ?? 'notification');
      return { ...p, mode };
    })
    .filter((p) => p.mode !== 'off')
    .slice(0, 12);
}
