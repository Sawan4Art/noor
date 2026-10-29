/**
 * بانر التحديث — v0.4 بطلب المالك:
 * - نصوص مرتبة منطقياً (العنوان ← الوصف ← التقدم ← الإجراء) بلا تداخل
 * - APK: التنزيل داخل التطبيق بشريط تقدم حي ثم فتح المثبّت تلقائياً — بلا متصفح نهائياً
 * - ويب: تنظيف الكاش + إعادة تحميل
 * - النسخ رهن الاحتياط فقط (زر صغير) — المسار الأساسي كله داخلي
 */
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { DownloadCloud, RefreshCw, X, Copy, Check, ShieldCheck } from 'lucide-react';
import {
  APP_VERSION,
  BUILD_ID,
  GithubReleaseInfo,
  OtaManifest,
  clearWebCaches,
  startOtaWatcher,
} from '../utils/otaUpdate';
import {
  downloadAndInstallApk,
  requestInstallPermission,
} from '../utils/nativeBridge';

type ActiveUpdate =
  | { kind: 'web'; manifest: OtaManifest }
  | { kind: 'apk'; release: GithubReleaseInfo };

type DownloadState = {
  active: boolean;
  percent: number;
  receivedMb: string;
  totalMb: string;
  done: boolean;
  askingPermission: boolean;
  failed: boolean;
};

const EMPTY_DOWNLOAD: DownloadState = {
  active: false,
  percent: 0,
  receivedMb: '0',
  totalMb: '0',
  done: false,
  askingPermission: false,
  failed: false,
};

export const UpdateBanner: React.FC = () => {
  const [update, setUpdate] = useState<ActiveUpdate | null>(null);
  const [dismissed, setDismissed] = useState(false);
  const [copied, setCopied] = useState(false);
  const [download, setDownload] = useState<DownloadState>(EMPTY_DOWNLOAD);
  const downloadStartedRef = useRef(false);

  useEffect(() => {
    const stop = startOtaWatcher({
      currentBuildId: BUILD_ID,
      currentVersion: APP_VERSION,
      onWebUpdate: (manifest) => setUpdate({ kind: 'web', manifest }),
      onApkUpdate: (release) => setUpdate({ kind: 'apk', release }),
    });
    return stop;
  }, []);

  const copyLink = useCallback(async (url: string) => {
    try {
      if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(url);
      } else {
        const ta = document.createElement('textarea');
        ta.value = url;
        document.body.appendChild(ta);
        ta.select();
        document.execCommand('copy');
        document.body.removeChild(ta);
      }
      setCopied(true);
      setTimeout(() => setCopied(false), 2500);
    } catch {
      // آخر fallback: المستخدم ينسخ يدوياً من نص الرابط الظاهر
    }
  }, []);

  const startApkDownload = useCallback(async (url: string) => {
    setDownload({ ...EMPTY_DOWNLOAD, active: true });
    const ok = await downloadAndInstallApk(url, (p) => {
      setDownload({
        active: true,
        percent: Math.max(0, Math.min(100, p.percent)),
        receivedMb: (p.received / 1024 / 1024).toFixed(1),
        totalMb: (p.total / 1024 / 1024).toFixed(1),
        done: p.done,
        askingPermission: false,
        failed: false,
      });
    });
    if (!ok) {
      // أشهر سبب: إذن التثبيت غير ممنوح → نفتح شاشة السماح ونبلّغ بإعادة المحاولة
      setDownload((d) => ({ ...d, failed: true, active: false }));
      try {
        setDownload((d) => ({ ...d, askingPermission: true }));
        await requestInstallPermission();
      } finally {
        setDownload((d) => ({ ...d, askingPermission: false }));
      }
    }
  }, []);

  const handleApply = useCallback(async () => {
    if (!update) return;
    if (update.kind === 'web') {
      // مسار ويب: نظّف الكاش ثم أعد التحميل — والفشل لا يمنع التحميل
      await clearWebCaches();
      window.location.reload();
      return;
    }
    // مسار APK: تنزيل داخلي بشريط تقدم ثم تثبيت — بلا متصفح (v0.4)
    const url = update.release.apkUrl ?? update.release.htmlUrl;
    if (!download.active && !downloadStartedRef.current) {
      downloadStartedRef.current = true;
      void startApkDownload(url);
    }
  }, [update, download.active, startApkDownload]);

  const handleRetry = useCallback(() => {
    if (!update || update.kind !== 'apk') return;
    const url = update.release.apkUrl ?? update.release.htmlUrl;
    downloadStartedRef.current = false;
    void startApkDownload(url);
  }, [update, startApkDownload]);

  if (!update || dismissed) return null;

  const versionLabel =
    update.kind === 'apk' ? update.release.version : update.manifest.version;
  const isApk = update.kind === 'apk';

  return (
    <div
      role="alert"
      className="fixed bottom-24 inset-x-4 sm:inset-x-auto sm:left-1/2 sm:-translate-x-1/2 z-50 max-w-md"
      dir="rtl"
    >
      <div className="rounded-2xl border border-emerald-500/30 bg-emerald-950/95 text-emerald-50 shadow-2xl backdrop-blur px-4 py-3.5 space-y-2.5">
        {/* ١) العنوان + الإغلاق — صف واحد */}
        <div className="flex items-center justify-between gap-3">
          <p className="text-sm font-bold truncate">
            تحديث جديد متاح — النسخة v{versionLabel}
          </p>
          <button
            onClick={() => setDismissed(true)}
            title="إخفاء"
            aria-label="إخفاء إشعار التحديث"
            className="rounded-lg p-1.5 shrink-0 text-emerald-300/70 hover:text-emerald-100 hover:bg-emerald-500/10 transition-colors"
          >
            <X className="h-4 w-4" aria-hidden="true" />
          </button>
        </div>

        {/* ٢) الوصف — جملة واحدة واضحة */}
        <p className="text-xs text-emerald-200/85 leading-relaxed">
          {isApk
            ? 'سيتم تنزيل التحديث داخل التطبيق ثم فتح المثبّت تلقائياً — بدون فقدان بياناتك.'
            : 'إصدار محسّن من التطبيق جاهز — سيتم التحديث فوراً.'}
        </p>

        {/* ٣) شريط التقدم أثناء تنزيل الـ APK */}
        {isApk && (download.active || download.done) && (
          <div className="space-y-1.5">
            <div className="h-2.5 rounded-full bg-emerald-950 border border-emerald-500/30 overflow-hidden">
              <div
                className="h-full rounded-full bg-gradient-to-l from-emerald-400 to-teal-300 transition-all duration-300"
                style={{ width: `${download.percent}%` }}
              />
            </div>
            <div className="flex items-center justify-between text-[10px] text-emerald-300/90 font-mono">
              <span>
                {download.done
                  ? 'تم التنزيل — يُفتح المثبّت الآن…'
                  : `جاري التنزيل ${download.percent}%`}
              </span>
              <span>
                {download.receivedMb} / {download.totalMb} م.ب
              </span>
            </div>
          </div>
        )}

        {isApk && download.failed && !download.askingPermission && (
          <div className="space-y-1.5">
            <p className="text-[10px] text-amber-300 flex items-center gap-1.5">
              <ShieldCheck className="w-3.5 h-3.5" />
              اسمح بالتثبيت من النافذة التي فُتحت ثم أعد المحاولة
            </p>
            <button
              onClick={handleRetry}
              className="w-full flex items-center justify-center gap-2 rounded-xl border border-emerald-500/40 hover:bg-emerald-500/10 px-3 py-2 text-xs font-bold text-emerald-100 transition-colors"
            >
              <RefreshCw className="h-3.5 w-3.5" />
              <span>إعادة المحاولة</span>
            </button>
          </div>
        )}
        {isApk && download.askingPermission && (
          <p className="text-[10px] text-emerald-300 flex items-center gap-1.5">
            <ShieldCheck className="w-3.5 h-3.5" />
            فتحت لك إعدادات السماح بالتثبيت…
          </p>
        )}

        {/* ٤) زر الإجراء */}
        {!download.active && !download.done && !download.failed && (
          <button
            onClick={handleApply}
            className="w-full flex items-center justify-center gap-2 rounded-xl bg-emerald-500 hover:bg-emerald-400 px-3 py-2.5 text-xs font-bold text-emerald-950 transition-colors"
          >
            {isApk ? (
              <DownloadCloud className="h-4 w-4" aria-hidden="true" />
            ) : (
              <RefreshCw className="h-4 w-4" aria-hidden="true" />
            )}
            <span>{isApk ? 'تنزيل التحديث الآن' : 'تحديث الآن'}</span>
            {isApk && (
              <span className="flex items-center gap-1 text-[10px] text-emerald-800 font-bold">
                <Check className="h-3 w-3" />
                داخل التطبيق
              </span>
            )}
          </button>
        )}

        {/* ٥) النسخ — احتياط صغير فقط */}
        {isApk && !download.active && (
          <button
            onClick={() => copyLink(update.kind === 'apk' ? (update.release.apkUrl ?? update.release.htmlUrl) : '')}
            className="mx-auto flex items-center gap-1 text-[10px] text-emerald-300/70 hover:text-emerald-100 transition-colors"
            title="احتياط: نسخ رابط التحميل"
          >
            {copied ? (
              <Check className="h-3 w-3" aria-hidden="true" />
            ) : (
              <Copy className="h-3 w-3" aria-hidden="true" />
            )}
            {copied ? 'تم نسخ الرابط' : 'نسخ رابط التحميل (احتياط)'}
          </button>
        )}
      </div>
    </div>
  );
};
