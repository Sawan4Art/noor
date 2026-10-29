/**
 * جسر كاباسيتور للنيتِف أندرويد — نور المسلم.
 * كل الدوال ترجع fallback آمن على الويب (لا شيء يحدث) وعلى أي خطأ نيتِف.
 *
 * المتاح نيتِفياً (NoorNativePlugin.java):
 * - requestAllPermissions / checkPermissions: الموقع + الإشعارات + المنبهات الدقيقة
 * - openAppSettings / openNotificationSettings / openExactAlarmSettings
 * - isDndAccessGranted / openDndAccessSettings (لكتم الأصوات)
 * - scheduleAdhanAlarms: منبهات أذان/إقامة/سحور/قيام تعمل والشاشة مطفية
 *   + كتم أصوات الهاتف من الأذان حتى بعد الإقامة + صوت الإقامة المختار
 * - scheduleSalawatAlarms: تذكير دوري بصوت الصلاة على النبي ﷺ
 * - cancelSalawatAlarms
 * - updateWidget: تحديث ويدجت شاشة الرئيسية (ويحفظ الموقع ليعمل الويدجت وحده)
 */
import { Capacitor, PluginListenerHandle, registerPlugin } from '@capacitor/core';
import { PrayerAlerts, WidgetSettings } from './appSettings';

export interface AdhanAlarm {
  /** مفتاح الصلاة: fajr/dhuhr/... أو qiyam/suhoor */
  key: string;
  /** الاسم العربي المعروض */
  name: string;
  /** وقت التنبيه بالميلي ثانية (epoch) */
  ts: number;
  /** نمط التنبيه: أذان كامل / إقامة / إشعار / إيقاف */
  mode: 'adhan' | 'iqamah' | 'notification' | 'off';
}

/** إعدادات الإقامة والكتم تُمرَّر للنيتِف مع كل جدولة */
export interface AlarmSettings {
  /** تفعيل الإقامة */
  iqamahEnabled: boolean;
  /** دقائق الإقامة بعد الأذان */
  iqamahOffsetMinutes: number;
  /** صوت الإقامة: makkah / beautiful */
  iqamahVoice: string;
  /** كتم كل أصوات الهاتف مع الأذان */
  muteEnabled: boolean;
  /** دقائق الكتم بعد الإقامة (أو بعد الأذان لو الإقامة مطفأة) */
  muteMinutes: number;
}

export interface WidgetData {
  city: string;
  /** التاريخ الهجري المختصر */
  hijri: string;
  /** إحداثيات الطريقة المذهب — لتحسب الويدجت مواقيتها بنفسها بدون فتح التطبيق */
  latitude?: number;
  longitude?: number;
  method?: string;
  hanafi?: boolean;
  /** مواقيت اليوم: مفتاح -> نص الوقت HH:MM */
  times: Record<string, string>;
  /** أسماء الصلوات بالترتيب مع مفاتيحها */
  order: Array<{ key: string; name: string; time: string }>;
  /** الصلاة القادمة */
  next: { key: string; name: string; ts: number; time: string };
  /** فرق التوقيت المحلي بالدقائق عن UTC (لحساب العداد) */
  tzOffsetMinutes: number;
}

export interface PermissionsStatus {
  location: boolean;
  notifications: boolean;
  exactAlarms: boolean;
  dndAccess: boolean;
}

interface NoorNativePlugin {
  requestAllPermissions(): Promise<{ location: boolean; notifications: boolean; exactAlarms: boolean }>;
  checkPermissions(): Promise<PermissionsStatus>;
  scheduleAdhanAlarms(options: {
    alarms: AdhanAlarm[];
    voice: string;
    city: string;
    settings: AlarmSettings;
  }): Promise<void>;
  cancelAdhanAlarms(): Promise<void>;
  scheduleSalawatAlarms(options: { intervalMinutes: number }): Promise<void>;
  cancelSalawatAlarms(): Promise<void>;
  updateWidget(options: { data: WidgetData; settings?: WidgetSettings }): Promise<void>;
  openExactAlarmSettings(): Promise<void>;
  openAppSettings(): Promise<void>;
  openNotificationSettings(): Promise<void>;
  isDndAccessGranted(): Promise<boolean>;
  openDndAccessSettings(): Promise<void>;
  canInstallApk(): Promise<{ can: boolean }>;
  requestInstallPermission(): Promise<void>;
  downloadAndInstallApk(options: { url: string }): Promise<void>;
  addListener(
    eventName: 'noorApkProgress',
    listenerFunc: (data: ApkProgress) => void,
  ): Promise<PluginListenerHandle>;
}

const NoorNative = registerPlugin<NoorNativePlugin>('NoorNative');

export function isNativeApp(): boolean {
  return Capacitor.isNativePlatform();
}

const EMPTY_PERMISSIONS: PermissionsStatus = {
  location: false,
  notifications: false,
  exactAlarms: false,
  dndAccess: false,
};

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

/** قراءة حالة كل الأذونات دون طلب (لعرضها في الإعدادات) */
export async function checkPermissions(): Promise<PermissionsStatus> {
  if (!isNativeApp()) return { location: true, notifications: true, exactAlarms: true, dndAccess: true };
  try {
    return await NoorNative.checkPermissions();
  } catch {
    return EMPTY_PERMISSIONS;
  }
}

/** جدولة منبهات الأذان/الإقامة/السحور/القيام + خطة الكتم — تُستدعى عند أي تغيير */
export async function scheduleAdhanAlarms(
  alarms: AdhanAlarm[],
  voice: string,
  city: string,
  settings: AlarmSettings,
): Promise<void> {
  if (!isNativeApp() || alarms.length === 0) return;
  try {
    await NoorNative.scheduleAdhanAlarms({ alarms, voice, city, settings });
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

/** تذكير دوري بصوت الصلاة على النبي ﷺ */
export async function scheduleSalawatAlarms(intervalMinutes: number): Promise<void> {
  if (!isNativeApp()) return;
  try {
    await NoorNative.scheduleSalawatAlarms({ intervalMinutes });
  } catch {
    // تجاهل
  }
}

export async function cancelSalawatAlarms(): Promise<void> {
  if (!isNativeApp()) return;
  try {
    await NoorNative.cancelSalawatAlarms();
  } catch {
    // تجاهل
  }
}

/** تحديث ويدجت الشاشة الرئيسية ببيانات مواقيت اليوم + إعدادات التخصيص الكاملة */
export async function updateWidget(data: WidgetData, settings?: WidgetSettings): Promise<void> {
  if (!isNativeApp()) return;
  try {
    await NoorNative.updateWidget({ data, settings });
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

/** فتح إعدادات إشعارات التطبيق مباشرة (بطلب المالك: زر فتح الأذونات لا يعمل) */
export async function openNotificationSettings(): Promise<void> {
  if (!isNativeApp()) return;
  try {
    await NoorNative.openNotificationSettings();
  } catch {
    // تجاهل
  }
}

/** هل مُنح إذن «عدم الإزعاج» اللازم لكتم أصوات الهاتف؟ */
export async function isDndAccessGranted(): Promise<boolean> {
  if (!isNativeApp()) return true;
  try {
    return await NoorNative.isDndAccessGranted();
  } catch {
    return false;
  }
}

/** فتح إعدادات منح «عدم الإزعاج» للتطبيق */
export async function openDndAccessSettings(): Promise<void> {
  if (!isNativeApp()) return;
  try {
    await NoorNative.openDndAccessSettings();
  } catch {
    // تجاهل
  }
}

/**
 * تحويل أنماط التنبيه + المواقيت إلى قائمة منبهات للنيتِف.
 * - قيام الليل والسحور: إشعار دائماً (مش أذان)
 * - الإقامة تُجدول نيتِفياً من إعدادات الأذان (لكل أذان صوتي)
 */
export function buildAlarmsFromAlerts(
  upcoming: Array<{ key: string; name: string; ts: number }>,
  alerts: PrayerAlerts,
): AdhanAlarm[] {
  return upcoming
    .map((p) => {
      // مفاتيح خاصة (قيام/سحور) إشعار دائماً
      const alertMode = alerts[p.key as keyof PrayerAlerts];
      const mode: 'adhan' | 'iqamah' | 'notification' | 'off' =
        p.key === 'qiyam' || p.key === 'suhoor'
          ? 'notification'
          : (alertMode ?? 'notification');
      return { ...p, mode };
    })
    .filter((p) => p.mode !== 'off')
    .slice(0, 20);
}

/* ==================== التحديث داخل التطبيق (v0.4 — بلا متصفح) ====================
 * بطلب المالك: نقر التحديث ينزّل الـ APK جوه التطبيق بشريط تقدم حي
 * ثم يفتح المثبّت تلقائياً — مفيش متصفح نهائياً. */

export interface ApkProgress {
  received: number;
  total: number;
  percent: number;
  done: boolean;
}

/** هل يسمح النظام بتثبيت التحديث من داخل التطبيق؟ */
export async function canInstallApk(): Promise<boolean> {
  if (!isNativeApp()) return false;
  try {
    const r = await NoorNative.canInstallApk();
    return !!r?.can;
  } catch {
    return false;
  }
}

/** فتح شاشة السماح بالتثبيت من هذا التطبيق (مرة واحدة تكفي) */
export async function requestInstallPermission(): Promise<void> {
  if (!isNativeApp()) return;
  try {
    await NoorNative.requestInstallPermission();
  } catch {
    // تجاهل
  }
}

/** تنزيل الـ APK داخل التطبيق بشريط تقدم ثم فتح المثبّت — true لو تم التنزيل وفتح المثبّت */
export async function downloadAndInstallApk(
  url: string,
  onProgress?: (p: ApkProgress) => void,
): Promise<boolean> {
  if (!isNativeApp()) return false;
  try {
    let handle: PluginListenerHandle | null = null;
    if (onProgress) {
      handle = await NoorNative.addListener('noorApkProgress', (data: ApkProgress) => {
        onProgress(data);
      });
    }
    try {
      await NoorNative.downloadAndInstallApk({ url });
      return true;
    } finally {
      if (handle) await handle.remove();
    }
  } catch {
    return false;
  }
}
