/**
 * بانر التحديث الهوائي — يعرض إشعار "نسخة جديدة متاحة"
 * ويب: زر تحديث = تنظيف الكاش + إعادة تحميل.
 * APK: زر تحميل الـ APK الجديد + نسخ الرابط كمسار احتياطي.
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Download, RefreshCw, X, Copy, Check } from 'lucide-react';
import {
  APP_VERSION,
  BUILD_ID,
  GithubReleaseInfo,
  OtaManifest,
  clearWebCaches,
  startOtaWatcher,
} from '../utils/otaUpdate';

type ActiveUpdate =
  | { kind: 'web'; manifest: OtaManifest }
  | { kind: 'apk'; release: GithubReleaseInfo };

export const UpdateBanner: React.FC = () => {
  const [update, setUpdate] = useState<ActiveUpdate | null>(null);
  const [dismissed, setDismissed] = useState(false);
  const [copied, setCopied] = useState(false);
  const [applying, setApplying] = useState(false);

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

  const handleApply = useCallback(async () => {
    if (!update) return;
    setApplying(true);
    try {
      if (update.kind === 'web') {
        // مسار ويب: نظّف الكاش ثم أعد التحميل — والفشل لا يمنع التحميل
        await clearWebCaches();
        window.location.reload();
        return;
      }
      // مسار APK: افتح رابط التحميل؛ لو المتصفح/الويبفيو منعه انسخ الرابط
      const url = update.release.apkUrl ?? update.release.htmlUrl;
      const win = window.open(url, '_blank', 'noopener,noreferrer');
      if (!win) await copyLink(url);
    } catch {
      // Fallback النهائي: نسخ الرابط في كل الحالات
      if (update.kind === 'apk') {
        const url = update.release.apkUrl ?? update.release.htmlUrl;
        await copyLink(url);
      }
    } finally {
      setApplying(false);
    }
  }, [update, copyLink]);


  if (!update || dismissed) return null;

  const versionLabel =
    update.kind === 'apk'
      ? update.release.version
      : update.manifest.version;

  return (
    <div
      role="alert"
      className="fixed bottom-4 inset-x-4 sm:inset-x-auto sm:left-1/2 sm:-translate-x-1/2 z-50 max-w-md"
      dir="rtl"
    >
      <div className="flex items-center gap-3 rounded-2xl border border-emerald-500/30 bg-emerald-950/95 text-emerald-50 shadow-2xl backdrop-blur px-4 py-3">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-emerald-500/20">
          <RefreshCw className="h-5 w-5 text-emerald-300" aria-hidden="true" />
        </div>

        <div className="flex-1 min-w-0">
          <p className="text-sm font-bold">يتوفر تحديث جديد (v{versionLabel})</p>
          {update.kind === 'apk' ? (
            <p className="text-xs text-emerald-200/80 mt-0.5">
              نزّل النسخة المحدثة وثبّتها فوق الحالية بدون فقدان بياناتك.
            </p>
          ) : (
            <p className="text-xs text-emerald-200/80 mt-0.5">
              إصدار محسّن من التطبيق جاهز الآن.
            </p>
          )}
        </div>

        <div className="flex items-center gap-2 shrink-0">
          <button
            onClick={handleApply}
            disabled={applying}
            className="flex items-center gap-1.5 rounded-xl bg-emerald-500 hover:bg-emerald-400 disabled:opacity-60 px-3 py-2 text-xs font-bold text-emerald-950 transition-colors"
          >
            {update.kind === 'apk' ? (
              <Download className="h-4 w-4" aria-hidden="true" />
            ) : (
              <RefreshCw className={`h-4 w-4 ${applying ? 'animate-spin' : ''}`} aria-hidden="true" />
            )}
            {update.kind === 'apk' ? 'تحميل' : 'تحديث'}
          </button>

          {update.kind === 'apk' && (
            <button
              onClick={() => copyLink(update.release.apkUrl ?? update.release.htmlUrl)}
              title="نسخ رابط التحميل"
              className="flex items-center gap-1 rounded-xl border border-emerald-500/40 hover:bg-emerald-500/10 px-2.5 py-2 text-xs font-bold text-emerald-100 transition-colors"
            >
              {copied ? (
                <Check className="h-4 w-4" aria-hidden="true" />
              ) : (
                <Copy className="h-4 w-4" aria-hidden="true" />
              )}
              {copied ? 'تم' : 'نسخ'}
            </button>
          )}

          <button
            onClick={() => setDismissed(true)}
            title="إخفاء"
            aria-label="إخفاء إشعار التحديث"
            className="rounded-lg p-1.5 text-emerald-300/70 hover:text-emerald-100 hover:bg-emerald-500/10 transition-colors"
          >
            <X className="h-4 w-4" aria-hidden="true" />
          </button>
        </div>
      </div>
    </div>
  );
};
