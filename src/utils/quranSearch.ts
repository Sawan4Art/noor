/**
 * Quran Search Engine, Ayah-by-Number Parser, Bookmarks & Memorization System
 */
import { SURAHS_LIST, Surah } from './quranData';
import { MUSHAF_PAGES_DATA, TajweedAyah } from './quranTajweedData';

export interface SearchResult {
  surahNumber: number;
  surahName: string;
  ayahNumber: number;
  pageNumber: number;
  juzNumber: number;
  text: string;
  matchedPart?: string;
}

export interface BookmarkItem {
  id: string;
  surahNumber: number;
  ayahNumber: number;
  pageNumber: number;
  surahName: string;
  textSnippet: string;
  timestamp: number;
  status: 'memorizing' | 'memorized' | 'review_needed';
  note?: string;
}

/**
 * Strips all Arabic diacritics / Tashkeel and normalizes letter variations for fuzzy search
 */
export function normalizeArabicText(text: string): string {
  if (!text) return '';
  return text
    // Remove all Tashkeel / Harakat
    .replace(/[\u064B-\u065F\u0670\u06D6-\u06DC\u06DF-\u06E8\u06EA-\u06ED]/g, '')
    // Normalize Alefs
    .replace(/[إأآٱ]/g, 'ا')
    // Normalize Taa Marbuta
    .replace(/ة/g, 'ه')
    // Normalize Yaa / Alef Maksura
    .replace(/[ىي]/g, 'ي')
    // Remove verse marker symbols
    .replace(/[۝۞۩]/g, '')
    .trim()
    .toLowerCase();
}

/**
 * Parse structured search query like "البقرة: 255" or "البقرة 255" or "2:255"
 */
export function parseSurahAyahQuery(query: string): { surahNumber: number; ayahNumber?: number } | null {
  const clean = query.trim();

  // Pattern 1: "2:255" or "18:10"
  const colonMatch = clean.match(/^(\d{1,3})\s*[:\s]\s*(\d{1,3})$/);
  if (colonMatch) {
    const sNum = parseInt(colonMatch[1], 10);
    const aNum = parseInt(colonMatch[2], 10);
    if (sNum >= 1 && sNum <= 114) {
      return { surahNumber: sNum, ayahNumber: aNum };
    }
  }

  // Pattern 2: "سورة البقرة 255" or "البقرة: 255"
  const normQuery = normalizeArabicText(clean);
  for (const surah of SURAHS_LIST) {
    const normSurah = normalizeArabicText(surah.name);
    if (normQuery.startsWith(normSurah) || normQuery.startsWith(`سوره ${normSurah}`) || normQuery.startsWith(`سورة ${normSurah}`)) {
      const remaining = normQuery.replace(new RegExp(`^(سوره|سورة)?\\s*${normSurah}\\s*[:]?\\s*`), '').trim();
      const numMatch = remaining.match(/^(\d{1,3})$/);
      if (numMatch) {
        return { surahNumber: surah.number, ayahNumber: parseInt(numMatch[1], 10) };
      }
      return { surahNumber: surah.number };
    }
  }

  // Pattern 3: Single number <= 114 is surah number
  const singleNum = parseInt(clean, 10);
  if (!isNaN(singleNum) && singleNum >= 1 && singleNum <= 114 && clean.length <= 3) {
    return { surahNumber: singleNum };
  }

  return null;
}

/**
 * Searches in all available verses by text or query
 */
export function searchQuran(query: string): SearchResult[] {
  if (!query || query.trim().length < 2) return [];

  const results: SearchResult[] = [];
  const parsed = parseSurahAyahQuery(query);

  if (parsed) {
    const surah = SURAHS_LIST.find((s) => s.number === parsed.surahNumber);
    if (surah) {
      const ayahNum = parsed.ayahNumber || 1;
      // Search in pre-cached pages or approximate page
      results.push({
        surahNumber: surah.number,
        surahName: surah.name,
        ayahNumber: ayahNum,
        pageNumber: surah.page,
        juzNumber: Math.ceil(surah.page / 20),
        text: `سورة ${surah.name} • الآية ${ayahNum}`,
      });
      return results;
    }
  }

  const normalizedQuery = normalizeArabicText(query);

  // Search across cached pages
  for (const page of Object.values(MUSHAF_PAGES_DATA)) {
    for (const ayah of page.ayahs) {
      const normAyah = normalizeArabicText(ayah.plainText);
      if (normAyah.includes(normalizedQuery)) {
        const surah = SURAHS_LIST.find((s) => s.number === ayah.surahNumber);
        results.push({
          surahNumber: ayah.surahNumber,
          surahName: surah?.name || 'سورة البقرة',
          ayahNumber: ayah.ayahNumber,
          pageNumber: ayah.page,
          juzNumber: ayah.juz,
          text: ayah.plainText,
          matchedPart: query,
        });
      }
    }
  }

  // Also search surah names
  for (const s of SURAHS_LIST) {
    if (normalizeArabicText(s.name).includes(normalizedQuery)) {
      results.push({
        surahNumber: s.number,
        surahName: s.name,
        ayahNumber: 1,
        pageNumber: s.page,
        juzNumber: Math.ceil(s.page / 20),
        text: `سورة ${s.name} (${s.revelationTypeAr} • ${s.numberOfAyahs} آيات)`,
      });
    }
  }

  return results;
}

/**
 * Bookmarks & Memorization Storage Helpers
 */
export function getSavedBookmarks(): BookmarkItem[] {
  try {
    const raw = localStorage.getItem('nour_muslim_bookmarks');
    return raw ? JSON.parse(raw) : [
      {
        id: 'bm-default',
        surahNumber: 2,
        ayahNumber: 17,
        pageNumber: 4,
        surahName: 'البقرة',
        textSnippet: 'مَثَلُهُمْ كَمَثَلِ الَّذِي اسْتَوْقَدَ نَارًا...',
        timestamp: Date.now() - 3600000,
        status: 'memorizing',
        note: 'بداية حزب البقرة - مراجعة أحكام التجويد',
      },
    ];
  } catch {
    return [];
  }
}

export function saveBookmark(bookmark: BookmarkItem): void {
  try {
    const current = getSavedBookmarks();
    const filtered = current.filter((b) => b.id !== bookmark.id);
    localStorage.setItem('nour_muslim_bookmarks', JSON.stringify([bookmark, ...filtered]));
  } catch (e) {
    console.error('Failed to save bookmark', e);
  }
}

export function removeBookmark(id: string): void {
  try {
    const current = getSavedBookmarks();
    const updated = current.filter((b) => b.id !== id);
    localStorage.setItem('nour_muslim_bookmarks', JSON.stringify(updated));
  } catch (e) {
    console.error('Failed to delete bookmark', e);
  }
}
