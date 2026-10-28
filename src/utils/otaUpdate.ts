/**
 * التحديث الهوائي (OTA) — نور المسلم
 *
 * البيئة ويب: يفحص /version.json اللي بيتولّد مع كل بناء، لو فيه buildId
 * جديد يعرض بانر يحدّث التطبيق (مع تنظيف الكاش أولاً).
 *
 * بيئة APK (كاباسيتور): يفحص أحدث GitHub Release للريفو، لو فيه إصدار
 * أحدث من النسخة الحالية يعرض بانر تحميل الـ APK الجديد.
 *
 * قاعدة الميثاق: Fallback دايماً — أي فشل شبكة/تحليل = يرجّع null بصمت
 * بدون كسر التطبيق، مع تشخيص حي على window.__noorOta.
 */

export const RELEASES_API_URL = 'https://api.github.com/repos/Sawan4Art/noor/releases/latest';
export const RELEASES_PAGE_URL = 'https://github.com/Sawan4Art/noor/releases/latest';
/** مصدر بديل بلا حدود معدل: نسخة package.json على main (raw.githubusercontent يسمح CORS) */
const MAIN_PACKAGE_URL = 'https://raw.githubusercontent.com/Sawan4Art/noor/main/package.json';
/** نمط رابط الـ APK في الإصدارات — ثابت بحكم القاعدة الإلزامية للتسمية */
const APK_URL_PATTERN = (tag: string, version: string) =>
  `https://github.com/Sawan4Art/noor/releases/download/${tag}/noor-almuslim-v${version}.apk`;

export interface OtaManifest {
  version: string;
  buildId: string;
  builtAt: string;
}

export interface GithubReleaseInfo {
  tag: string;
  version: string;
  apkUrl: string | null;
  htmlUrl: string;
}

export interface OtaWatcherOptions {
  currentBuildId: string;
  currentVersion: string;
  onWebUpdate: (manifest: OtaManifest) => void;
  onApkUpdate: (release: GithubReleaseInfo) => void;
  webIntervalMs?: number;
  apkIntervalMs?: number;
  webManifestUrl?: string;
  releasesApiUrl?: string;
}

export interface OtaDiagnostics {
  env: 'web' | 'apk';
  checks: number;
  lastCheckAt: string | null;
  lastError: string | null;
  currentBuildId: string;
  currentVersion: string;
  webManifest: OtaManifest | null;
  latestRelease: GithubReleaseInfo | null;
  updateShown: boolean;
}

declare global {
  interface Window {
    __noorOta?: OtaDiagnostics;
  }
}

/**
 * معرّف البناء الحالي — يُحقن وقت البناء. في وضع التطوير = 'dev'.
 */
export const BUILD_ID: string =
  typeof __BUILD_ID__ !== 'undefined' ? __BUILD_ID__ : 'dev';

/**
 * نسخة التطبيق الحالية — من package.json وقت البناء.
 */
export const APP_VERSION: string =
  typeof __APP_VERSION__ !== 'undefined' ? __APP_VERSION__ : '0.0.0';

/**
 * مقارنة إصدارات بصيغة semver مبسطة (1.2.3) — تتسامح مع بادئة v.
 * ترجع: 1 لو a أحدث، -1 لو b أحدث، 0 لو متساويين.
 * أي صيغة غير مفهومة = -2 (إشارة خطأ للمتصل).
 */
export function compareVersions(a: string, b: string): number {
  try {
    const parse = (v: string): number[] => {
      const clean = String(v).trim().replace(/^v/i, '');
      if (!/^\d+(\.\d+)*$/.test(clean)) throw new Error(`bad version: ${v}`);
      return clean.split('.').map((n) => parseInt(n, 10));
    };
    const pa = parse(a);
    const pb = parse(b);
    const len = Math.max(pa.length, pb.length);
    for (let i = 0; i < len; i++) {
      const na = pa[i] ?? 0;
      const nb = pb[i] ?? 0;
      if (na > nb) return 1;
      if (na < nb) return -1;
    }
    return 0;
  } catch {
    return -2;
  }
}

/**
 * هل فيه تحديث ويب؟ (buildId مختلف عن الحالي)
 * وضع التطوير (buildId = 'dev') ما يطلقش بانر وهمي.
 */
export function isWebUpdateAvailable(
  currentBuildId: string,
  manifest: OtaManifest | null,
): boolean {
  if (!manifest) return false;
  if (!manifest.buildId) return false;
  if (currentBuildId === 'dev') return false;
  return manifest.buildId !== currentBuildId;
}

/**
 * هل فيه إصدار APK أحدث؟ أي صيغة غير مفهومة = لا تحديث (أمان).
 */
export function isApkUpdateAvailable(
  currentVersion: string,
  latestVersion: string,
): boolean {
  if (!currentVersion || !latestVersion) return false;
  return compareVersions(latestVersion, currentVersion) === 1;
}

/**
 * هل إحنا جوه APK أصلي (كاباسيتور)؟ بدون كاباسيتور = false.
 */
export function detectApkEnvironment(): boolean {
  try {
    const cap = (globalThis as { Capacitor?: { isNativePlatform?: () => boolean } })
      .Capacitor;
    return typeof cap?.isNativePlatform === 'function' && cap.isNativePlatform();
  } catch {
    return false;
  }
}

/**
 * جلب مانيفست نسخة الويب. أي فشل = null (fallback).
 */
export async function fetchWebManifest(
  webManifestUrl = '/version.json',
  timeoutMs = 5000,
): Promise<OtaManifest | null> {
  try {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);
    const res = await fetch(webManifestUrl, {
      cache: 'no-store',
      signal: controller.signal,
    });
    clearTimeout(timer);
    if (!res.ok) return null;
    const data = (await res.json()) as Partial<OtaManifest>;
    if (
      typeof data?.version !== 'string' ||
      typeof data?.buildId !== 'string' ||
      typeof data?.builtAt !== 'string'
    ) {
      return null;
    }
    return data as OtaManifest;
  } catch {
    return null;
  }
}

/**
 * جلب أحدث إصدار من GitHub Releases. أي فشل = null (fallback).
 * مساران: الـ API الرسمي أولاً، ولو انضرب بحد المعدل (403) أو فشل —
 * نجيب النسخة من package.json على main عبر raw.githubusercontent
 * (يسمح CORS وبلا حدود معدل) ونركّب رابط الـ APK من نمط التسمية الثابت.
 */
export async function fetchLatestRelease(
  releasesApiUrl = RELEASES_API_URL,
  timeoutMs = 8000,
): Promise<GithubReleaseInfo | null> {
  try {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);
    const res = await fetch(releasesApiUrl, {
      cache: 'no-store',
      signal: controller.signal,
      headers: { Accept: 'application/vnd.github+json' },
    });
    clearTimeout(timer);
    if (res.ok) {
      const data = (await res.json()) as {
        tag_name?: string;
        html_url?: string;
        assets?: Array<{ name?: string; browser_download_url?: string }>;
      };
      if (typeof data?.tag_name === 'string' && data.tag_name) {
        const apkAsset = (data.assets ?? []).find(
          (a) => typeof a?.browser_download_url === 'string' && /\.apk$/i.test(a.name ?? ''),
        );
        return {
          tag: data.tag_name,
          version: data.tag_name.replace(/^v/i, ''),
          apkUrl: apkAsset?.browser_download_url ?? null,
          htmlUrl:
            typeof data.html_url === 'string' && data.html_url
              ? data.html_url
              : RELEASES_PAGE_URL,
        };
      }
    }
    // 403 = حد المعدل أو مشكلة مؤقتة → نكمل للمصدر البديل بدل الرجوع بلا شيء
    return await fetchLatestFromMain(timeoutMs);
  } catch {
    return await fetchLatestFromMain(timeoutMs).catch(() => null);
  }
}

/** المصدر البديل: نسخة main من package.json — بلا حدود معدل */
async function fetchLatestFromMain(timeoutMs = 6000): Promise<GithubReleaseInfo | null> {
  try {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);
    const res = await fetch(MAIN_PACKAGE_URL, { cache: 'no-store', signal: controller.signal });
    clearTimeout(timer);
    if (!res.ok) return null;
    const pkg = (await res.json()) as { version?: string };
    if (typeof pkg?.version !== 'string' || !/^\d+(\.\d+)*$/.test(pkg.version)) return null;
    const tag = `v${pkg.version}`;
    return {
      tag,
      version: pkg.version,
      apkUrl: APK_URL_PATTERN(tag, pkg.version),
      htmlUrl: `https://github.com/Sawan4Art/noor/releases/tag/${tag}`,
    };
  } catch {
    return null;
  }
}

/**
 * تنظيف كاش المتصفح قبل إعادة التحميل (بيئة ويب). لا يرفض أبداً.
 */
export async function clearWebCaches(): Promise<void> {
  try {
    if (typeof caches !== 'undefined' && typeof caches.keys === 'function') {
      const keys = await caches.keys();
      await Promise.all(keys.map((k) => caches.delete(k)));
    }
  } catch {
    // Fallback: تنظيف الكاش رفاهي — إعادة التحميل تظل تعمل بدونه
  }
}

/**
 * تشغيل مراقب التحديث الهوائي. يفحص فوراً، ثم دورياً، وعند رجوع
 * التطبيق للمقدمة (visibilitychange). يرجّع دالة إيقاف.
 */
export function startOtaWatcher(options: OtaWatcherOptions): () => void {
  const {
    currentBuildId,
    currentVersion,
    onWebUpdate,
    onApkUpdate,
    webIntervalMs = 15 * 60 * 1000,
    apkIntervalMs = 6 * 60 * 60 * 1000,
    webManifestUrl = '/version.json',
    releasesApiUrl = RELEASES_API_URL,
  } = options;

  const isApk = detectApkEnvironment();

  const diag: OtaDiagnostics = {
    env: isApk ? 'apk' : 'web',
    checks: 0,
    lastCheckAt: null,
    lastError: null,
    currentBuildId,
    currentVersion,
    webManifest: null,
    latestRelease: null,
    updateShown: false,
  };

  const setDiag = (patch: Partial<OtaDiagnostics>): void => {
    Object.assign(diag, patch);
    if (typeof window !== 'undefined') {
      // تشخيص حي — نمط window.__sbLastTplStats الوارد في الميثاق
      window.__noorOta = diag;
    }
  };
  setDiag({});

  let stopped = false;
  let lastVisibilityCheck = 0;
  let timer: ReturnType<typeof setInterval> | null = null;

  const checkOnce = async (): Promise<void> => {
    if (stopped) return;
    setDiag({ checks: diag.checks + 1, lastCheckAt: new Date().toISOString() });
    try {
      if (isApk) {
        const release = await fetchLatestRelease(releasesApiUrl);
        setDiag({ latestRelease: release, lastError: null });
        if (release && isApkUpdateAvailable(currentVersion, release.version)) {
          setDiag({ updateShown: true });
          onApkUpdate(release);
        }
      } else {
        const manifest = await fetchWebManifest(webManifestUrl);
        setDiag({ webManifest: manifest, lastError: null });
        if (isWebUpdateAvailable(currentBuildId, manifest) && manifest) {
          setDiag({ updateShown: true });
          onWebUpdate(manifest);
        }
      }
    } catch (e) {
      // Fallback: أي خطأ غير متوقع لا يكسر التطبيق — يسجّل في التشخيص فقط
      setDiag({ lastError: e instanceof Error ? e.message : String(e) });
    }
  };

  void checkOnce();

  const period = isApk ? apkIntervalMs : webIntervalMs;
  timer = setInterval(() => void checkOnce(), period);

  const onVisibility = (): void => {
    if (stopped || typeof document === 'undefined') return;
    if (document.visibilityState !== 'visible') return;
    const now = Date.now();
    if (now - lastVisibilityCheck < 60_000) return; // خنق: مرة بالشيف كحد أدنى
    lastVisibilityCheck = now;
    void checkOnce();
  };

  if (typeof document !== 'undefined') {
    document.addEventListener('visibilitychange', onVisibility);
  }

  return () => {
    stopped = true;
    if (timer) clearInterval(timer);
    if (typeof document !== 'undefined') {
      document.removeEventListener('visibilitychange', onVisibility);
    }
  };
}
