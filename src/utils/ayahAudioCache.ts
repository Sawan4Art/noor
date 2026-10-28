/**
 * تخزين تلاوات الآيات للعمل بدون إنترنت — Cache API حقيقي.
 * تنزيل السورة = جلب كل ملفات آياتها وتخزينها في Cache Storage،
 * والتشغيل يبحث في الكاش أولاً قبل الشبكة (blob URL فوري).
 */
import { getAyahAudioUrl, AYAH_RECITERS } from './quranTajweedData';
import { SURAHS_LIST } from './quranData';
import { isSurahDownloaded, markSurahDownloaded } from './quranData';

const CACHE_NAME = 'noor-ayah-audio-v1';

function getCache(): Promise<Cache | null> {
  try {
    if (typeof caches === 'undefined' || !('open' in caches)) {
      return Promise.resolve(null);
    }
    return caches.open(CACHE_NAME).catch(() => null);
  } catch {
    return Promise.resolve(null);
  }
}

/**
 * تنزيل سورة كاملة لقارئ معيّن (كل آية ملف مستقل).
 * onProgress: 0..1 — يستدعى بعد كل آية.
 */
export async function downloadSurahForOffline(
  reciterId: string,
  surahNumber: number,
  onProgress?: (fraction: number) => void,
): Promise<boolean> {
  const reciter = AYAH_RECITERS.find((r) => r.id === reciterId);
  const surah = SURAHS_LIST.find((s) => s.number === surahNumber);
  if (!reciter || !surah) return false;

  const cache = await getCache();
  if (!cache) return false;

  const total = surah.numberOfAyahs;
  for (let a = 1; a <= total; a++) {
    const url = getAyahAudioUrl(reciter.folder, surahNumber, a);
    try {
      const existing = await cache.match(url);
      if (!existing) {
        const res = await fetch(url);
        if (res.ok) {
          await cache.put(url, res.clone());
        }
      }
    } catch {
      // آية فشلت: نكمل الباقي — الاستماع يعتمد الشبكة عندها فقط
    }
    onProgress?.(a / total);
  }

  markSurahDownloaded(reciterId, surahNumber, true);
  return true;
}

/** حذف سورة من التخزين المحلي */
export async function removeSurahFromOffline(
  reciterId: string,
  surahNumber: number,
): Promise<void> {
  const reciter = AYAH_RECITERS.find((r) => r.id === reciterId);
  const surah = SURAHS_LIST.find((s) => s.number === surahNumber);
  markSurahDownloaded(reciterId, surahNumber, false);
  if (!reciter || !surah) return;

  const cache = await getCache();
  if (!cache) return;

  for (let a = 1; a <= surah.numberOfAyahs; a++) {
    try {
      await cache.delete(getAyahAudioUrl(reciter.folder, surahNumber, a));
    } catch {
      // تجاهل
    }
  }
}

/** هل السورة محملة فعلاً (علامة محفوظة)؟ */
export function isSurahOffline(reciterId: string, surahNumber: number): boolean {
  return isSurahDownloaded(reciterId, surahNumber);
}

/**
 * حل مصدر الصوت: الكاش أولاً (blob URL) ثم الشبكة.
 * يرجّع { src, revoke? } — المتصل يلغي الـ blob عند الانتهاء.
 */
export async function resolveAyahAudioSrc(url: string): Promise<{
  src: string;
  revoke?: () => void;
}> {
  try {
    if (typeof caches !== 'undefined' && 'match' in caches) {
      const cached = await caches.match(url);
      if (cached) {
        const blob = await cached.blob();
        const blobUrl = URL.createObjectURL(blob);
        return { src: blobUrl, revoke: () => URL.revokeObjectURL(blobUrl) };
      }
    }
  } catch {
    // تجاهل — نرجع للشبكة
  }
  return { src: url };
}
