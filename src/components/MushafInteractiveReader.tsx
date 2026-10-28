import React, { useState, useRef, useEffect, useCallback } from 'react';
import {
  Play,
  Pause,
  Bookmark,
  Share2,
  Copy,
  BookOpen,
  X,
  Search,
  Sliders,
  Check,
  Eye,
  EyeOff,
  Type,
} from 'lucide-react';
import {
  TajweedAyah,
  MushafPage,
  TAJWEED_RULES,
  AYAH_RECITERS,
  AyahReciter,
  getAyahAudioUrl,
  getOrCreateMushafPage,
  loadQuranPages,
  isQuranPagesLoaded,
} from '../utils/quranTajweedData';
import {
  SearchResult,
  BookmarkItem,
  searchQuran,
  getSavedBookmarks,
  saveBookmark,
  removeBookmark,
} from '../utils/quranSearch';
import { SURAHS_LIST, Surah } from '../utils/quranData';
import {
  MushafEditionStyle,
  getSavedEdition,
  getSavedAyahReciter,
  saveAyahReciter,
} from '../utils/appSettings';
import { registerBackHandler } from '../utils/backButton';
import { resolveAyahAudioSrc } from '../utils/ayahAudioCache';

/**
 * المصحف الشريف التفاعلي — مصحف التجويد الملون (604 صفحة).
 *
 * بطلب المالك (2026-10، الدفعة الثالثة):
 * - ملء الشاشة الكامل دائماً: لا شريط علوي ولا سفلي إطلاقاً —
 *   الخروج بزر الرجوع في الهاتف، والتقليب بالسحب أو بلمس حافة الشاشة.
 * - الضغطة المطولة أطول (750ms) مع قائمة آية زجاجية أنيقة.
 * - التلوين التجويدي الحقييد من مصحف التجويد المعتمد (6236 آية).
 * - تلاوة متواصلة بلا تقطع: الآية التالية تُحمَّل مسبقاً أثناء تشغيل الحالية.
 * - حجم الخط يتكيف مع ارتفاع الصفحة ليملأها كمصحف حقيقي.
 */

/** صوت تقليب ورق حقيقي عبر Web Audio API */
function playPaperTurnSound() {
  try {
    const AudioContextClass =
      window.AudioContext ||
      (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
    if (!AudioContextClass) return;
    const ctx = new AudioContextClass();
    const duration = 0.09;
    const bufferSize = Math.floor(ctx.sampleRate * duration);
    const buffer = ctx.createBuffer(1, bufferSize, ctx.sampleRate);
    const data = buffer.getChannelData(0);

    for (let i = 0; i < bufferSize; i++) {
      const envelope = Math.exp(-i / (bufferSize * 0.3));
      data[i] = (Math.random() * 2 - 1) * envelope * 0.12;
    }

    const noise = ctx.createBufferSource();
    noise.buffer = buffer;

    const filter = ctx.createBiquadFilter();
    filter.type = 'bandpass';
    filter.frequency.value = 1350;
    filter.Q.value = 1.4;

    noise.connect(filter);
    filter.connect(ctx.destination);
    noise.start();
  } catch {
    // تجاهل سياسات التشغيل التلقائي
  }
}

/** مدة الضغطة الطويلة قبل ظهور قائمة الآية — أطول بطلب المالك */
const LONG_PRESS_MS = 750;

/** مفتاح مصحف نصي لربط الآيات بصوتها */
const ayahKey = (surah: number, ayah: number) => `${surah}:${ayah}`;

export const MushafInteractiveReader: React.FC = () => {
  // ===== حالة التنقل بين الصفحات =====
  const [currentPageNum, setCurrentPageNum] = useState<number>(4);
  const [activeAyah, setActiveAyah] = useState<TajweedAyah | null>(null);
  const [showAyahMenu, setShowAyahMenu] = useState<boolean>(false);
  const [menuPos, setMenuPos] = useState<{ x: number; y: number }>({ x: 0, y: 0 });
  const [flipDirection, setFlipDirection] = useState<'next' | 'prev' | null>(null);

  // ===== مكتبة المصحف: السور / البحث / الإشارات / الأدوات =====
  const [showLibrary, setShowLibrary] = useState<boolean>(false);
  const [libraryTab, setLibraryTab] = useState<'surahs' | 'search' | 'bookmarks' | 'tools'>('surahs');
  const [surahSearchQuery, setSurahSearchQuery] = useState<string>('');
  const [surahTabFilter, setSurahTabFilter] = useState<'all' | 'meccan' | 'medinan'>('all');

  // ===== النوافذ =====
  const [showListenModal, setShowListenModal] = useState<boolean>(false);
  const [showTafseerModal, setShowTafseerModal] = useState<boolean>(false);
  const [copiedToast, setCopiedToast] = useState<string | null>(null);

  // ===== التلاوة بالنطاقات والتكرار (الحفظ) =====
  const [selectedReciter, setSelectedReciter] = useState<AyahReciter>(getSavedAyahReciter);
  const [rangeStartAyah, setRangeStartAyah] = useState<number>(1);
  const [rangeEndAyah, setRangeEndAyah] = useState<number>(1);
  const [repeatPerAyah, setRepeatPerAyah] = useState<number>(1);
  const [repeatEntireRange, setRepeatEntireRange] = useState<number>(1);
  const [currentPlayingAyahNum, setCurrentPlayingAyahNum] = useState<number | null>(null);
  const [isPlayingRange, setIsPlayingRange] = useState<boolean>(false);

  // ===== وضع التسميع (إخفاء الكلمات) =====
  const [memorizationTestMode, setMemorizationTestMode] = useState<boolean>(false);
  const [revealedAyahs, setRevealedAyahs] = useState<Record<number, boolean>>({});

  // ===== البحث والمحفوظات =====
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [searchResults, setSearchResults] = useState<SearchResult[]>([]);
  const [bookmarks, setBookmarks] = useState<BookmarkItem[]>(getSavedBookmarks());

  // ===== طبعة المصحف من الإعدادات =====
  const [edition, setEdition] = useState<MushafEditionStyle>(getSavedEdition);

  // ===== حجم الخط التكيفي مع ارتفاع الصفحة =====
  const [adaptiveFontSize, setAdaptiveFontSize] = useState<number>(24);

  // ===== مراجع =====
  const pageContainerRef = useRef<HTMLDivElement | null>(null);
  const screenRef = useRef<HTMLDivElement | null>(null);
  const touchStartX = useRef<number | null>(null);
  const touchStartY = useRef<number | null>(null);
  const longPressTimer = useRef<number | null>(null);
  const longPressFired = useRef<boolean>(false);
  const [pressingAyahNum, setPressingAyahNum] = useState<number | null>(null);

  // ===== مشغل التلاوة بلا تقطع: تحميل مسبق للآية التالية + كاش أوفلاين =====
  interface QueueState {
    surah: number;
    start: number;
    end: number;
    repeatPer: number;
    repeatAll: number;
    ayahRepeat: number;
    cycle: number;
    playing: boolean;
  }
  const queueRef = useRef<QueueState | null>(null);
  const currentAudioRef = useRef<HTMLAudioElement | null>(null);
  const currentRevokeRef = useRef<(() => void) | null>(null);
  const nextAudioRef = useRef<HTMLAudioElement | null>(null);
  const nextAudioKeyRef = useRef<string | null>(null);
  const nextRevokeRef = useRef<(() => void) | null>(null);

  /** تحميل آية مسبقاً (أثناء تشغيل الحالية) لتشغيلها فوراً بدون تقطع */
  const prefetchAyah = useCallback(
    async (surah: number, ayah: number) => {
      try {
        const key = ayahKey(surah, ayah);
        if (nextAudioKeyRef.current === key && nextAudioRef.current) return;
        nextAudioKeyRef.current = key; // حجز فوري لمنع التحميل المزدوج
        const url = getAyahAudioUrl(selectedReciter.folder, surah, ayah);
        const { src, revoke } = await resolveAyahAudioSrc(url);
        if (nextAudioKeyRef.current !== key) {
          // تغيّر الطلب أثناء التحميل — نتجاهل النتيجة
          revoke?.();
          return;
        }
        const el = new Audio();
        el.preload = 'auto';
        el.src = src;
        el.load();
        if (nextRevokeRef.current) nextRevokeRef.current();
        nextAudioRef.current = el;
        nextRevokeRef.current = revoke ?? null;
      } catch {
        // تجاهل — التحميل المسبق تحسين اختياري
      }
    },
    [selectedReciter],
  );

  const stopAudio = useCallback(() => {
    queueRef.current = null;
    if (currentAudioRef.current) {
      currentAudioRef.current.pause();
      currentAudioRef.current.onended = null;
      currentAudioRef.current = null;
    }
    if (currentRevokeRef.current) {
      currentRevokeRef.current();
      currentRevokeRef.current = null;
    }
    if (nextAudioRef.current) {
      nextAudioRef.current.pause();
      nextAudioRef.current = null;
      nextAudioKeyRef.current = null;
    }
    if (nextRevokeRef.current) {
      nextRevokeRef.current();
      nextRevokeRef.current = null;
    }
    setIsPlayingRange(false);
    setCurrentPlayingAyahNum(null);
  }, []);

  /** تشغيل آية — تستخدم العنصر المحمّل مسبقاً لو متاح (صفر فجوة) */
  const playAyah = useCallback(
    (surah: number, ayah: number): void => {
      const q = queueRef.current;
      if (!q || !q.playing) return;

      const advance = (nextAyah: number | null, repeatRange: boolean) => {
        const state = queueRef.current;
        if (!state || !state.playing) return;
        if (nextAyah !== null && nextAyah > 0) {
          void playAyah(surah, nextAyah);
        } else if (repeatRange) {
          void playAyah(surah, state.start);
        } else {
          stopAudio();
        }
      };

      const startWith = (el: HTMLAudioElement, revoke: (() => void) | null) => {
        if (currentAudioRef.current) {
          currentAudioRef.current.pause();
          currentAudioRef.current.onended = null;
        }
        if (currentRevokeRef.current) currentRevokeRef.current();
        currentAudioRef.current = el;
        currentRevokeRef.current = revoke;
        setCurrentPlayingAyahNum(ayah);
        setIsPlayingRange(true);

        // تحميل الآية التالية مسبقاً فور بدء الحالية (استمرارية بلا تقطع)
        const nextAyah = ayah < q.end ? ayah + 1 : null;
        if (nextAyah !== null) {
          window.setTimeout(() => {
            void prefetchAyah(surah, nextAyah);
          }, 400);
        }

        el.onended = () => {
          const state = queueRef.current;
          if (!state || !state.playing) return;
          // تكرار الآية نفسها للحفظ
          if (state.ayahRepeat + 1 < state.repeatPer) {
            state.ayahRepeat += 1;
            el.currentTime = 0;
            void el.play().catch(() => {});
            return;
          }
          state.ayahRepeat = 0;
          // الانتقال للآية التالية في النطاق
          if (ayah < state.end) {
            advance(ayah + 1, false);
            return;
          }
          // إعادة النطاق كاملاً
          if (state.cycle + 1 < state.repeatAll) {
            state.cycle += 1;
            advance(null, true);
            return;
          }
          stopAudio();
        };

        el.onerror = () => {
          const state = queueRef.current;
          if (state && state.playing && ayah < state.end) {
            // فشل تحميل الآية: تخطي للآية التالية بدل التوقف الكامل
            advance(ayah + 1, false);
          } else {
            stopAudio();
          }
        };

        void el.play().catch(() => {
          // fallback صمت — لا نكسر الواجهة
        });
      };

      const key = ayahKey(surah, ayah);
      if (nextAudioKeyRef.current === key && nextAudioRef.current) {
        const el = nextAudioRef.current;
        const revoke = nextRevokeRef.current;
        nextAudioRef.current = null;
        nextAudioKeyRef.current = null;
        nextRevokeRef.current = null;
        startWith(el, revoke);
        return;
      }

      // لا يوجد تحميل مسبق — نجلب من الكاش/الشبكة مباشرة
      const url = getAyahAudioUrl(selectedReciter.folder, surah, ayah);
      void resolveAyahAudioSrc(url).then(({ src, revoke }) => {
        const state = queueRef.current;
        if (!state || !state.playing) {
          revoke?.();
          return;
        }
        const el = new Audio();
        el.preload = 'auto';
        el.src = src;
        startWith(el, revoke ?? null);
      });
    },
    [prefetchAyah, selectedReciter, stopAudio],
  );

  const startRange = useCallback(
    (surah: number, start: number, end: number, repeatPer: number, repeatAll: number) => {
      stopAudio();
      queueRef.current = {
        surah,
        start,
        end,
        repeatPer,
        repeatAll,
        ayahRepeat: 0,
        cycle: 0,
        playing: true,
      };
      playAyah(surah, start);
    },
    [playAyah, stopAudio],
  );

  // إيقاف الصوت عند مغادرة التبويب
  useEffect(() => stopAudio, [stopAudio]);

  // تحميل نص المصحف الكامل (604 صفحة) + مصحف التجويد الملون
  const [pagesReady, setPagesReady] = useState<boolean>(isQuranPagesLoaded());
  useEffect(() => {
    if (pagesReady) return;
    let alive = true;
    void loadQuranPages().then(() => {
      if (alive) setPagesReady(true);
    });
    return () => {
      alive = false;
    };
  }, [pagesReady]);

  // مزامنة الطبعة مع الإعدادات كل ما رجع التبويب
  useEffect(() => {
    setEdition(getSavedEdition());
  }, []);

  const currentPage: MushafPage = getOrCreateMushafPage(currentPageNum);

  // عدد آيات السورة المعروضة
  const displayedSurahNumber =
    activeAyah?.surahNumber ?? currentPage.ayahs[0]?.surahNumber ?? 1;
  const surahAyahCount =
    SURAHS_LIST.find((s) => s.number === displayedSurahNumber)?.numberOfAyahs ?? 286;

  // ===== حجم الخط: قياس ارتفاع الشاشة وتكبير الخط لملء الصفحة =====
  useEffect(() => {
    const compute = () => {
      const h = screenRef.current?.clientHeight ?? window.innerHeight;
      // مصحف حقيقي: ~15 سطراً في الصفحة — الخط ≈ 2.3% من الارتفاع
      const size = Math.max(15, Math.min(42, Math.round(h * 0.023)));
      setAdaptiveFontSize(size);
    };
    compute();
    window.addEventListener('resize', compute);
    return () => window.removeEventListener('resize', compute);
  }, []);

  // ===== تقليب الصفحات (عربي RTL: التالية لليسار) =====
  const handleFlipPage = useCallback((direction: 'next' | 'prev') => {
    playPaperTurnSound();
    setFlipDirection(direction);
    setTimeout(() => {
      if (direction === 'next') {
        setCurrentPageNum((p) => Math.min(604, p + 1));
      } else {
        setCurrentPageNum((p) => Math.max(1, p - 1));
      }
      setShowAyahMenu(false);
    }, 140);
    setTimeout(() => setFlipDirection(null), 340);
  }, []);

  // اختيار سورة → فتح صفحتها مباشرة بملء الشاشة
  const handleSelectSurah = (surah: Surah) => {
    setCurrentPageNum(surah.page);
    setShowLibrary(false);
    playPaperTurnSound();
    setCopiedToast(`سورة ${surah.name} — صفحة ${surah.page}`);
    setTimeout(() => setCopiedToast(null), 2500);
  };

  // لوحة المفاتيح: أسهم للتقليب + Esc يغلق المودالات
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (showLibrary || showListenModal || showTafseerModal) return;
      if (e.key === 'ArrowLeft') handleFlipPage('next');
      else if (e.key === 'ArrowRight') handleFlipPage('prev');
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [handleFlipPage, showLibrary, showListenModal, showTafseerModal]);

  // زر الرجوع في أندرويد: يغلق المودالات ويوقف الصوت أولاً
  useEffect(() => {
    return registerBackHandler(() => {
      if (showTafseerModal) {
        setShowTafseerModal(false);
        return true;
      }
      if (showListenModal) {
        setShowListenModal(false);
        return true;
      }
      if (showLibrary) {
        setShowLibrary(false);
        return true;
      }
      if (showAyahMenu) {
        setShowAyahMenu(false);
        return true;
      }
      if (isPlayingRange) {
        stopAudio();
        return true;
      }
      return false;
    });
  }, [showAyahMenu, showLibrary, showListenModal, showTafseerModal, isPlayingRange, stopAudio]);

  // ===== السحب (Swipe) لتقليب الصفحات =====
  const handleTouchStart = (e: React.TouchEvent) => {
    touchStartX.current = e.touches[0].clientX;
    touchStartY.current = e.touches[0].clientY;
  };

  const handleTouchEnd = (e: React.TouchEvent) => {
    if (touchStartX.current === null || touchStartY.current === null) return;
    const deltaX = e.changedTouches[0].clientX - touchStartX.current;
    const deltaY = e.changedTouches[0].clientY - touchStartY.current;

    if (Math.abs(deltaX) > 45 && Math.abs(deltaX) > Math.abs(deltaY)) {
      if (deltaX < 0) handleFlipPage('next');
      else handleFlipPage('prev');
    }
    touchStartX.current = null;
    touchStartY.current = null;
  };

  // ===== قائمة الآية: فتح عند نقطة الضغط مع تثبيت داخل حدود الصفحة =====
  const openAyahMenu = (ayah: TajweedAyah, clientX: number, clientY: number) => {
    setActiveAyah(ayah);
    setRangeStartAyah(ayah.ayahNumber);
    setRangeEndAyah(Math.max(ayah.ayahNumber, rangeEndAyah || ayah.ayahNumber));

    if (pageContainerRef.current) {
      const rect = pageContainerRef.current.getBoundingClientRect();
      const clickX = clientX - rect.left;
      const clickY = clientY - rect.top;
      const menuW = 260;
      const menuH = 340;
      const x = Math.min(
        Math.max(clickX - menuW / 2, 10),
        Math.max(rect.width - menuW - 10, 10),
      );
      const y = Math.min(
        Math.max(clickY - 60, 10),
        Math.max(rect.height - menuH - 10, 10),
      );
      setMenuPos({ x, y });
    }
    setShowAyahMenu(true);
  };

  const closeAyahMenu = useCallback(() => setShowAyahMenu(false), []);

  // ===== الضغطة الطويلة (لمس + فأرة) =====
  const startLongPress = (ayah: TajweedAyah, clientX: number, clientY: number) => {
    longPressFired.current = false;
    setPressingAyahNum(ayah.ayahNumber);
    if (longPressTimer.current) window.clearTimeout(longPressTimer.current);
    longPressTimer.current = window.setTimeout(() => {
      longPressFired.current = true;
      if ('vibrate' in navigator) {
        try {
          navigator.vibrate(30);
        } catch {
          // تجاهل
        }
      }
      openAyahMenu(ayah, clientX, clientY);
      setPressingAyahNum(null);
    }, LONG_PRESS_MS);
  };

  const cancelLongPress = () => {
    if (longPressTimer.current) {
      window.clearTimeout(longPressTimer.current);
      longPressTimer.current = null;
    }
    setPressingAyahNum(null);
  };

  // نقرة عادية (أو نهاية ضغطة طويلة قصيرة) → فتح القائمة أيضاً
  const handleAyahClick = (ayah: TajweedAyah, event: React.MouseEvent) => {
    if (longPressFired.current) {
      longPressFired.current = false;
      return;
    }
    openAyahMenu(ayah, event.clientX, event.clientY);
  };

  // ===== إجراءات الآية =====
  const handleBookmarkCurrentAyah = () => {
    if (!activeAyah) return;
    const item: BookmarkItem = {
      id: `${activeAyah.surahNumber}-${activeAyah.ayahNumber}-${Date.now()}`,
      surahNumber: activeAyah.surahNumber,
      surahName: currentPage.surahName,
      ayahNumber: activeAyah.ayahNumber,
      pageNumber: currentPageNum,
      timestamp: Date.now(),
      textSnippet: activeAyah.plainText.substring(0, 70) + '...',
      status: 'memorizing',
    };
    saveBookmark(item);
    setBookmarks(getSavedBookmarks());
    setShowAyahMenu(false);
    setCopiedToast(`تمت إضافة الآية ${activeAyah.ayahNumber} إلى المحفوظات`);
    setTimeout(() => setCopiedToast(null), 3000);
  };

  const handleCopyAyah = () => {
    if (!activeAyah) return;
    navigator.clipboard.writeText(
      `﴿${activeAyah.plainText}﴾ [${currentPage.surahName}: ${activeAyah.ayahNumber}]`,
    );
    setShowAyahMenu(false);
    setCopiedToast('تم نسخ نص الآية الكريمة إلى الحافظة');
    setTimeout(() => setCopiedToast(null), 3000);
  };

  const handleShareAyah = () => {
    if (!activeAyah) return;
    const shareText = `﴿${activeAyah.plainText}﴾\n[${currentPage.surahName} — آية ${activeAyah.ayahNumber}]`;
    if (navigator.share) {
      navigator.share({ title: currentPage.surahName, text: shareText }).catch(() => {});
    } else {
      navigator.clipboard.writeText(shareText);
      setCopiedToast('تم نسخ نص الآية للمشاركة');
      setTimeout(() => setCopiedToast(null), 3000);
    }
    setShowAyahMenu(false);
  };

  // ===== البحث =====
  const handleSearchChange = (q: string) => {
    setSearchQuery(q);
    if (q.trim().length >= 2) setSearchResults(searchQuran(q));
    else setSearchResults([]);
  };

  const handleSelectSearchResult = (res: SearchResult) => {
    setCurrentPageNum(res.pageNumber);
    setShowLibrary(false);
    const page = getOrCreateMushafPage(res.pageNumber);
    const match = page.ayahs.find((a) => a.ayahNumber === res.ayahNumber);
    if (match) {
      setActiveAyah(match);
      setRangeStartAyah(match.ayahNumber);
    }
  };

  // ===== تصفية السور =====
  const filteredSurahs = SURAHS_LIST.filter((s) => {
    const matchesText =
      s.name.includes(surahSearchQuery) ||
      s.englishName.toLowerCase().includes(surahSearchQuery.toLowerCase()) ||
      s.number.toString() === surahSearchQuery;
    if (!matchesText) return false;
    if (surahTabFilter === 'meccan') return s.revelationType === 'Meccan';
    if (surahTabFilter === 'medinan') return s.revelationType === 'Medinan';
    return true;
  });

  const isNightEdition = edition.id === 'night';

  // =====================================================================
  // تدفق الآيات بالتلوين التجويدي المعتمد حسب الطبعة
  // =====================================================================
  const renderAyahFlow = () => (
    <div
      className="text-justify font-quran font-normal dir-rtl leading-[2.6]"
      style={{ color: edition.ink, fontSize: `${adaptiveFontSize}px` }}
    >
      {currentPage.ayahs.map((ayah) => {
        const isSelected = activeAyah?.ayahNumber === ayah.ayahNumber;
        const isPlaying = currentPlayingAyahNum === ayah.ayahNumber;
        const isMasked = memorizationTestMode && !revealedAyahs[ayah.ayahNumber];
        const isPressing = pressingAyahNum === ayah.ayahNumber;

        return (
          <span
            key={`${ayah.surahNumber}-${ayah.ayahNumber}`}
            onClick={(e) => handleAyahClick(ayah, e)}
            onContextMenu={(e) => {
              e.preventDefault();
              openAyahMenu(ayah, e.clientX, e.clientY);
            }}
            onTouchStart={(e) =>
              startLongPress(ayah, e.touches[0].clientX, e.touches[0].clientY)
            }
            onTouchEnd={(e) => {
              cancelLongPress();
              if (!longPressFired.current && e.changedTouches.length > 0) {
                openAyahMenu(ayah, e.changedTouches[0].clientX, e.changedTouches[0].clientY);
              }
            }}
            onTouchMove={cancelLongPress}
            onTouchCancel={cancelLongPress}
            onMouseDown={(e) => startLongPress(ayah, e.clientX, e.clientY)}
            onMouseUp={cancelLongPress}
            onMouseLeave={cancelLongPress}
            className={`inline cursor-pointer transition-all duration-150 rounded px-1 py-0.5 mx-0.5 select-none ${
              isPressing ? 'scale-105' : ''
            } ${
              isPlaying
                ? 'bg-emerald-300/90 shadow-sm ring-2 ring-emerald-600'
                : isSelected
                ? 'bg-amber-200/80 shadow-sm ring-2 ring-amber-400'
                : 'hover:bg-amber-100/30'
            }`}
            title={`اضغط مطولاً على الآية ${ayah.ayahNumber} للتفاعل`}
          >
            {isMasked ? (
              <span
                onClick={(e) => {
                  e.stopPropagation();
                  setRevealedAyahs((prev) => ({ ...prev, [ayah.ayahNumber]: true }));
                }}
                className="filter blur-sm bg-slate-400/40 text-transparent select-none cursor-pointer px-2 py-0.5 rounded"
                title="انقر لإظهار الكلمات والتحقق من حفظك"
              >
                {ayah.plainText}
              </span>
            ) : (
              ayah.segments.map((seg, sIdx) => {
                const ruleStyle =
                  seg.rule && edition.showTajweed ? TAJWEED_RULES[seg.rule] : null;
                return (
                  <span
                    key={sIdx}
                    style={
                      ruleStyle
                        ? {
                            color: isNightEdition ? ruleStyle.nightColor : ruleStyle.color,
                          }
                        : {}
                    }
                  >
                    {seg.text}
                  </span>
                );
              })
            )}

            {/* رقم الآية بالزخرفة العثمانية */}
            <span
              className="inline-flex items-center justify-center font-sans font-bold border rounded-full mx-1 shadow-inner align-middle"
              style={{
                width: `${Math.round(adaptiveFontSize * 1.15)}px`,
                height: `${Math.round(adaptiveFontSize * 1.15)}px`,
                fontSize: `${Math.max(9, Math.round(adaptiveFontSize * 0.45))}px`,
                color: edition.ayahNumberColor,
                borderColor: edition.frame,
                backgroundColor: isNightEdition ? 'rgba(255,255,255,0.06)' : '#fbf6e3',
              }}
            >
              {ayah.ayahNumber}
            </span>
          </span>
        );
      })}
    </div>
  );

  // =====================================================================
  // صفحة المصحف نفسها — تملأ الشاشة بالكامل (نسبة مصحف حقيقية)
  // =====================================================================
  const renderMushafPage = () => (
    <div
      ref={pageContainerRef}
      onTouchStart={handleTouchStart}
      onTouchEnd={handleTouchEnd}
      className={`relative h-full mx-auto aspect-[66/100] max-w-full shadow-2xl overflow-hidden select-none book-spine-shadow rounded-lg border-[6px] sm:border-[10px] ${
        flipDirection === 'next'
          ? 'book-flip-forward'
          : flipDirection === 'prev'
          ? 'book-flip-backward'
          : ''
      }`}
      style={{
        backgroundColor: edition.paper,
        borderColor: edition.frame,
        boxShadow: '0 18px 50px rgba(0,0,0,0.55)',
      }}
      dir="rtl"
    >
      <div
        className="h-full p-2 sm:p-4 border-4 rounded-lg relative flex flex-col overflow-hidden"
        style={{ borderColor: edition.innerFrame }}
      >
        {/* ترويسة الصفحة داخل إطار المصحف (زي المصحف الحقيقي) */}
        <div
          className="flex items-center justify-between shrink-0 pb-2 mb-3 border-b-2"
          style={{ borderColor: edition.frame, color: edition.ink }}
        >
          <div
            className="px-3 py-1 rounded border font-sans font-bold"
            style={{
              fontSize: `${Math.max(9, adaptiveFontSize * 0.42)}px`,
              borderColor: edition.frame,
              backgroundColor: isNightEdition ? 'rgba(255,255,255,0.05)' : `${edition.frame}22`,
            }}
          >
            {currentPage.hizbQuarter} • الجزء {currentPage.juzNumber}
          </div>

          <button
            onClick={() => {
              setLibraryTab('surahs');
              setShowLibrary(true);
            }}
            className="px-4 py-1 rounded border font-arabic font-bold transition-all hover:brightness-110"
            style={{
              fontSize: `${Math.max(10, adaptiveFontSize * 0.48)}px`,
              borderColor: edition.innerFrame,
              color: edition.ink,
              backgroundColor: isNightEdition ? 'rgba(255,255,255,0.05)' : `${edition.innerFrame}18`,
            }}
            title="فهرس السور والبحث والإشارات"
          >
            {currentPage.surahName} ▾
          </button>
        </div>

        {/* تدفق الآيات — يملأ الصفحة بالكامل */}
        <div className="flex-1 min-h-0 overflow-y-auto">{renderAyahFlow()}</div>

        {/* تذييل الصفحة: الجزء | رقم الصفحة | السورة */}
        <div
          className="mt-3 pt-2 border-t-2 shrink-0 flex items-center justify-between font-bold px-2"
          style={{
            borderColor: edition.frame,
            color: edition.ink,
            fontSize: `${Math.max(9, adaptiveFontSize * 0.42)}px`,
          }}
        >
          <span>جزء {currentPage.juzNumber}</span>
          <span
            className="font-extrabold"
            style={{
              color: edition.ayahNumberColor,
              fontSize: `${Math.max(11, adaptiveFontSize * 0.55)}px`,
            }}
          >
            {currentPage.pageNumber}
          </span>
          <span>{currentPage.surahName}</span>
        </div>
      </div>

      {/* قائمة الآية الزجاجية */}
      {showAyahMenu && activeAyah && (
        <AyahContextMenu
          ayahNumber={activeAyah.ayahNumber}
          surahName={currentPage.surahName}
          x={menuPos.x}
          y={menuPos.y}
          onClose={closeAyahMenu}
          onListen={() => {
            closeAyahMenu();
            setShowListenModal(true);
          }}
          onTafseer={() => {
            closeAyahMenu();
            setShowTafseerModal(true);
          }}
          onBookmark={handleBookmarkCurrentAyah}
          onCopy={handleCopyAyah}
          onShare={handleShareAyah}
        />
      )}
    </div>
  );

  // =====================================================================
  // العرض الغامر الكامل: لا شريط علوي ولا سفلي — تقليب بالحواف والسحب
  // =====================================================================
  return (
    <div ref={screenRef} className="fixed inset-0 z-[90] flex flex-col bg-[#05070d]" dir="rtl">
      {/* Toast */}
      {copiedToast && (
        <div className="fixed top-4 left-1/2 -translate-x-1/2 z-[95] px-5 py-3 bg-emerald-600 text-white rounded-2xl shadow-2xl flex items-center gap-3 text-xs sm:text-sm">
          <Check className="w-4 h-4 text-emerald-200" />
          <span>{copiedToast}</span>
          <button
            onClick={() => setCopiedToast(null)}
            className="text-xs text-emerald-200 hover:text-white px-2 py-0.5"
          >
            إغلاق
          </button>
        </div>
      )}

      {/* زر إيقاف التلاوة العائم الصغير أثناء التشغيل فقط */}
      {isPlayingRange && (
        <button
          onClick={stopAudio}
          className="fixed top-3 right-3 z-[92] flex items-center gap-1.5 px-3.5 py-2 rounded-2xl text-xs font-bold text-white shadow-2xl backdrop-blur-md animate-pulse"
          style={{ backgroundColor: 'rgba(190, 18, 60, 0.92)' }}
          title="إيقاف التلاوة"
        >
          <Pause className="w-4 h-4" />
          <span className="font-mono">آية {currentPlayingAyahNum}</span>
        </button>
      )}

      {!pagesReady && (
        <div
          className="fixed top-3 left-1/2 -translate-x-1/2 z-[92] px-4 py-1.5 rounded-full text-[11px] backdrop-blur-md"
          style={{
            backgroundColor: 'color-mix(in srgb, var(--noor-n-950) 80%, transparent)',
            color: 'var(--noor-a-400)',
          }}
        >
          جاري تحميل نص المصحف…
        </div>
      )}

      {/* صفحة المصحف تملأ الشاشة */}
      <div className="flex-1 min-h-0 flex items-center justify-center p-1.5 sm:p-3">
        {renderMushafPage()}
      </div>

      {/* مناطق لمس الحواف للتقليب — بلا أي شكل مرئي */}
      <button
        aria-label="الصفحة التالية"
        onClick={() => handleFlipPage('next')}
        disabled={currentPageNum >= 604}
        className="fixed left-0 top-0 bottom-0 w-[9%] max-w-[70px] z-[88] disabled:opacity-0 bg-transparent border-0 cursor-w-resize active:bg-white/[0.04]"
      />
      <button
        aria-label="الصفحة السابقة"
        onClick={() => handleFlipPage('prev')}
        disabled={currentPageNum <= 1}
        className="fixed right-0 top-0 bottom-0 w-[9%] max-w-[70px] z-[88] disabled:opacity-0 bg-transparent border-0 cursor-e-resize active:bg-white/[0.04]"
      />

      {/* مكتبة المصحف: السور | البحث | الإشارات | الأدوات */}
      {showLibrary && (
        <LibraryModal
          initialTab={libraryTab}
          onClose={() => setShowLibrary(false)}
          onSelectSurah={handleSelectSurah}
          // بحث
          searchQuery={searchQuery}
          onSearchChange={handleSearchChange}
          searchResults={searchResults}
          onSelectSearchResult={handleSelectSearchResult}
          // إشارات
          bookmarks={bookmarks}
          onSelectBookmark={(bm) => {
            setCurrentPageNum(bm.pageNumber);
            setShowLibrary(false);
          }}
          onRemoveBookmark={(id) => {
            removeBookmark(id);
            setBookmarks(getSavedBookmarks());
          }}
          // أدوات
          memorizationTestMode={memorizationTestMode}
          onToggleMemorization={() => setMemorizationTestMode((v) => !v)}
          edition={edition}
          onOpenListen={() => {
            setShowLibrary(false);
            setShowListenModal(true);
          }}
          hasActiveAyah={activeAyah !== null}
        />
      )}

      {/* الاستماع إلى؟ */}
      {showListenModal && activeAyah && (
        <ListenModal
          ayahNumber={activeAyah.ayahNumber}
          surahName={currentPage.surahName}
          firstAyahOnPage={currentPage.ayahs[0]?.ayahNumber ?? 1}
          lastAyahOnPage={currentPage.ayahs[currentPage.ayahs.length - 1]?.ayahNumber ?? 1}
          surahAyahCount={surahAyahCount}
          rangeStart={rangeStartAyah}
          rangeEnd={rangeEndAyah}
          setRangeStart={setRangeStartAyah}
          setRangeEnd={setRangeEndAyah}
          repeatPerAyah={repeatPerAyah}
          setRepeatPerAyah={setRepeatPerAyah}
          repeatEntireRange={repeatEntireRange}
          setRepeatEntireRange={setRepeatEntireRange}
          selectedReciter={selectedReciter}
          onSelectReciter={(r) => {
            setSelectedReciter(r);
            saveAyahReciter(r.id);
          }}
          onClose={() => setShowListenModal(false)}
          onStart={(start, end, per, all) => {
            setShowListenModal(false);
            setShowAyahMenu(false);
            startRange(displayedSurahNumber, start, end, per, all);
          }}
        />
      )}

      {/* التفسير */}
      {showTafseerModal && activeAyah && (
        <TafseerModal
          ayah={activeAyah}
          edition={edition}
          onClose={() => setShowTafseerModal(false)}
          onListen={() => {
            setShowTafseerModal(false);
            setShowListenModal(true);
          }}
        />
      )}
    </div>
  );
};

/* ============================================================================
 * مكتبة المصحف — السور والبحث والإشارات والأدوات في نافذة واحدة منظمة
 * ========================================================================== */
interface LibraryModalProps {
  initialTab: 'surahs' | 'search' | 'bookmarks' | 'tools';
  onClose: () => void;
  onSelectSurah: (surah: Surah) => void;
  searchQuery: string;
  onSearchChange: (q: string) => void;
  searchResults: SearchResult[];
  onSelectSearchResult: (res: SearchResult) => void;
  bookmarks: BookmarkItem[];
  onSelectBookmark: (bm: BookmarkItem) => void;
  onRemoveBookmark: (id: string) => void;
  memorizationTestMode: boolean;
  onToggleMemorization: () => void;
  edition: MushafEditionStyle;
  onOpenListen: () => void;
  hasActiveAyah: boolean;
}

const LibraryModal: React.FC<LibraryModalProps> = ({
  initialTab,
  onClose,
  onSelectSurah,
  searchQuery,
  onSearchChange,
  searchResults,
  onSelectSearchResult,
  bookmarks,
  onSelectBookmark,
  onRemoveBookmark,
  memorizationTestMode,
  onToggleMemorization,
  edition,
  onOpenListen,
  hasActiveAyah,
}) => {
  const [tab, setTab] = useState(initialTab);
  const [surahQuery, setSurahQuery] = useState('');
  const [filter, setFilter] = useState<'all' | 'meccan' | 'medinan'>('all');

  const tabs: Array<{ key: typeof tab; label: string; Icon: React.FC<{ className?: string }> }> = [
    { key: 'surahs', label: 'السور', Icon: BookOpen },
    { key: 'search', label: 'البحث', Icon: Search },
    { key: 'bookmarks', label: 'إشاراتي', Icon: Bookmark },
    { key: 'tools', label: 'أدوات', Icon: Sliders },
  ];

  const filtered = SURAHS_LIST.filter((s) => {
    const matches =
      s.name.includes(surahQuery) ||
      s.englishName.toLowerCase().includes(surahQuery.toLowerCase()) ||
      s.number.toString() === surahQuery;
    if (!matches) return false;
    if (filter === 'meccan') return s.revelationType === 'Meccan';
    if (filter === 'medinan') return s.revelationType === 'Medinan';
    return true;
  });

  return (
    <div
      className="fixed inset-0 z-[96] flex items-center justify-center p-3 sm:p-5 bg-black/85 backdrop-blur-md animate-in fade-in"
      onClick={onClose}
    >
      <div
        className="w-full max-w-2xl rounded-3xl bg-slate-900 border border-amber-600/40 p-4 sm:p-5 space-y-3 text-right max-h-[92vh] flex flex-col shadow-2xl"
        onClick={(e) => e.stopPropagation()}
        dir="rtl"
      >
        <div className="flex items-center justify-between pb-3 border-b border-slate-800 shrink-0">
          <div className="flex items-center gap-2">
            <BookOpen className="w-5 h-5 text-amber-400" />
            <h3 className="text-base font-bold text-white">مكتبة المصحف</h3>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-xl bg-slate-800 text-slate-400 hover:text-white"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* تبويبات المكتبة */}
        <div className="flex items-center gap-1.5 shrink-0">
          {tabs.map(({ key, label, Icon }) => (
            <button
              key={key}
              onClick={() => setTab(key)}
              className={`flex-1 flex items-center justify-center gap-1.5 py-2 rounded-xl text-xs font-bold transition-colors ${
                tab === key
                  ? 'bg-amber-600 text-white shadow'
                  : 'bg-slate-950 text-slate-400 hover:text-white'
              }`}
            >
              <Icon className="w-4 h-4" />
              <span>{label}</span>
            </button>
          ))}
        </div>

        {/* ===== السور ===== */}
        {tab === 'surahs' && (
          <>
            <div className="relative shrink-0">
              <Search className="w-4 h-4 absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                type="text"
                value={surahQuery}
                onChange={(e) => setSurahQuery(e.target.value)}
                placeholder="ابحث باسم السورة أو رقمها..."
                className="w-full pl-4 pr-10 py-2.5 rounded-2xl bg-slate-950 border border-slate-800 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-amber-500"
              />
            </div>
            <div className="flex items-center gap-2 shrink-0">
              {(
                [
                  ['all', 'جميع السور (114)'],
                  ['meccan', 'مكية (86)'],
                  ['medinan', 'مدنية (28)'],
                ] as const
              ).map(([key, label]) => (
                <button
                  key={key}
                  onClick={() => setFilter(key)}
                  className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-colors ${
                    filter === key
                      ? 'bg-amber-600 text-white'
                      : 'bg-slate-950 text-slate-400 hover:text-white'
                  }`}
                >
                  {label}
                </button>
              ))}
            </div>
            <div className="flex-1 overflow-y-auto grid grid-cols-1 sm:grid-cols-2 gap-2.5 pr-1 min-h-0">
              {filtered.map((surah) => (
                <div
                  key={surah.number}
                  onClick={() => onSelectSurah(surah)}
                  className="p-3 rounded-2xl bg-slate-950 hover:bg-slate-800 border border-slate-800/80 hover:border-amber-500/50 flex items-center justify-between gap-3 cursor-pointer transition-all group"
                >
                  <div className="flex items-center gap-3">
                    <span className="w-8 h-8 rounded-full bg-slate-900 border border-slate-700 group-hover:border-amber-500 group-hover:text-amber-400 flex items-center justify-center text-xs font-bold font-sans text-slate-300">
                      {surah.number}
                    </span>
                    <div>
                      <div className="font-bold text-sm text-slate-100 group-hover:text-amber-300">
                        سورة {surah.name}
                      </div>
                      <div className="text-[11px] text-slate-400 font-sans">
                        {surah.numberOfAyahs} آيات • {surah.revelationTypeAr}
                      </div>
                    </div>
                  </div>
                  <span className="text-[10px] text-amber-400/90 font-sans font-bold">
                    صفحة {surah.page}
                  </span>
                </div>
              ))}
            </div>
          </>
        )}

        {/* ===== البحث ===== */}
        {tab === 'search' && (
          <>
            <div className="relative shrink-0">
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => onSearchChange(e.target.value)}
                placeholder="اكتب نص الآية أو رقمها مثلاً: «البقرة 255» أو «2:255»..."
                className="w-full px-4 py-3 rounded-2xl bg-slate-950 border border-slate-800 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                autoFocus
              />
            </div>
            <div className="flex-1 overflow-y-auto space-y-2 pr-1 min-h-0">
              {searchResults.length === 0 ? (
                <div className="text-center py-8 text-xs text-slate-400 space-y-1">
                  <p>اكتب حرفين أو أكثر للبحث السريع في المصحف الشريف</p>
                  <p className="text-slate-500">يدعم البحث بدون تشكيل والبحث السريع بالأرقام</p>
                </div>
              ) : (
                searchResults.map((res) => (
                  <div
                    key={`${res.surahNumber}-${res.ayahNumber}`}
                    onClick={() => onSelectSearchResult(res)}
                    className="p-3.5 rounded-2xl bg-slate-950 hover:bg-slate-800/80 border border-slate-800/60 cursor-pointer transition-colors"
                  >
                    <div className="flex items-center justify-between text-xs text-emerald-400 font-bold mb-1">
                      <span>
                        {res.surahName} • آية {res.ayahNumber}
                      </span>
                      <span className="text-amber-400 text-[11px] font-sans">
                        صفحة {res.pageNumber}
                      </span>
                    </div>
                    <p className="font-quran text-slate-200 text-base leading-relaxed">{res.text}</p>
                  </div>
                ))
              )}
            </div>
          </>
        )}

        {/* ===== الإشارات المرجعية ===== */}
        {tab === 'bookmarks' && (
          <div className="flex-1 overflow-y-auto space-y-2.5 pr-1 min-h-0">
            {bookmarks.length === 0 ? (
              <div className="text-center py-8 text-xs text-slate-400">
                لا توجد آيات محفوظة حالياً. اضغط مطولاً على أي آية في المصحف ثم اختر الحفظ.
              </div>
            ) : (
              bookmarks.map((bm) => (
                <div
                  key={bm.id}
                  className="p-3.5 rounded-2xl bg-slate-950 border border-slate-800 flex items-center justify-between gap-3"
                >
                  <div className="flex-1 cursor-pointer" onClick={() => onSelectBookmark(bm)}>
                    <div className="flex items-center gap-2 mb-1">
                      <span className="text-xs font-bold text-emerald-400">
                        {bm.surahName} • آية {bm.ayahNumber}
                      </span>
                      <span className="text-[10px] px-2 py-0.5 rounded-full bg-amber-500/10 text-amber-300 border border-amber-500/30">
                        صفحة {bm.pageNumber}
                      </span>
                    </div>
                    <p className="font-quran text-sm text-slate-300 line-clamp-1">
                      {bm.textSnippet}
                    </p>
                  </div>
                  <button
                    onClick={() => onRemoveBookmark(bm.id)}
                    className="p-2 rounded-xl text-slate-500 hover:text-rose-400 transition-colors"
                    title="حذف من المحفوظات"
                  >
                    <X className="w-4 h-4" />
                  </button>
                </div>
              ))
            )}
          </div>
        )}

        {/* ===== الأدوات ===== */}
        {tab === 'tools' && (
          <div className="flex-1 overflow-y-auto space-y-3 pr-1 min-h-0">
            <button
              onClick={onToggleMemorization}
              className={`w-full flex items-center gap-3 p-4 rounded-2xl border transition-colors text-right ${
                memorizationTestMode
                  ? 'bg-amber-500/15 border-amber-500/50'
                  : 'bg-slate-950 border-slate-800 hover:border-slate-700'
              }`}
            >
              <span
                className={`w-10 h-10 rounded-xl flex items-center justify-center ${
                  memorizationTestMode ? 'bg-amber-500 text-slate-950' : 'bg-slate-800 text-amber-400'
                }`}
              >
                {memorizationTestMode ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
              </span>
              <span className="flex-1">
                <span className="block text-sm font-bold text-slate-100">
                  وضع التسميع واختبار الحفظ
                </span>
                <span className="block text-[11px] text-slate-400 mt-0.5">
                  إخفاء كلمات الآيات ثم اكشفها باللمس للتحقق من حفظك
                </span>
              </span>
              {memorizationTestMode && (
                <span className="text-[10px] px-2 py-1 rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/30 font-bold">
                  مفعّل
                </span>
              )}
            </button>

            <button
              onClick={onOpenListen}
              disabled={!hasActiveAyah}
              className="w-full flex items-center gap-3 p-4 rounded-2xl bg-slate-950 border border-slate-800 hover:border-emerald-600/40 transition-colors text-right disabled:opacity-40"
            >
              <span className="w-10 h-10 rounded-xl bg-slate-800 text-emerald-400 flex items-center justify-center">
                <Play className="w-5 h-5 fill-current" />
              </span>
              <span className="flex-1">
                <span className="block text-sm font-bold text-slate-100">
                  الاستماع بالنطاقات والتكرار
                </span>
                <span className="block text-[11px] text-slate-400 mt-0.5">
                  {hasActiveAyah
                    ? 'تلاوة الآية المحددة أو نطاق كامل بصوت القارئ المفضل'
                    : 'اضغط على أي آية أولاً ثم افتح الأدوات'}
                </span>
              </span>
            </button>

            {/* دليل ألوان التجويد */}
            {edition.showTajweed && (
              <div className="p-4 rounded-2xl bg-slate-950 border border-slate-800 space-y-2.5">
                <div className="flex items-center gap-2">
                  <Type className="w-4 h-4 text-emerald-400" />
                  <span className="text-sm font-bold text-slate-100">دليل ألوان التجويد</span>
                </div>
                <div className="grid grid-cols-2 gap-2">
                  {Object.entries(TAJWEED_RULES).map(([key, rule]) => (
                    <div
                      key={key}
                      className="flex items-center gap-2 px-2.5 py-1.5 rounded-xl bg-slate-900/60"
                    >
                      <span
                        className="w-4 h-4 rounded-full border border-white/20 shrink-0"
                        style={{ backgroundColor: rule.color }}
                      />
                      <span className="text-[11px] text-slate-300">{rule.label}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

/* ============================================================================
 * نافذة الاستماع — آية / صفحة / سورة / نطاق مخصص مع التكرار
 * ========================================================================== */
interface ListenModalProps {
  ayahNumber: number;
  surahName: string;
  firstAyahOnPage: number;
  lastAyahOnPage: number;
  surahAyahCount: number;
  rangeStart: number;
  rangeEnd: number;
  setRangeStart: (n: number) => void;
  setRangeEnd: (n: number) => void;
  repeatPerAyah: number;
  setRepeatPerAyah: (n: number) => void;
  repeatEntireRange: number;
  setRepeatEntireRange: (n: number) => void;
  selectedReciter: AyahReciter;
  onSelectReciter: (r: AyahReciter) => void;
  onClose: () => void;
  onStart: (start: number, end: number, per: number, all: number) => void;
}

const ListenModal: React.FC<ListenModalProps> = ({
  ayahNumber,
  surahName,
  firstAyahOnPage,
  lastAyahOnPage,
  surahAyahCount,
  rangeStart,
  rangeEnd,
  setRangeStart,
  setRangeEnd,
  repeatPerAyah,
  setRepeatPerAyah,
  repeatEntireRange,
  setRepeatEntireRange,
  selectedReciter,
  onSelectReciter,
  onClose,
  onStart,
}) => (
  <div
    className="fixed inset-0 z-[96] flex items-end sm:items-center justify-center bg-black/80 backdrop-blur-sm animate-in fade-in duration-200 p-0 sm:p-4"
    onClick={onClose}
  >
    <div
      className="w-full max-w-md rounded-t-3xl sm:rounded-3xl bg-slate-900 border border-slate-800 p-6 space-y-4 text-center max-h-[90vh] overflow-y-auto"
      onClick={(e) => e.stopPropagation()}
      dir="rtl"
    >
      <div className="flex items-center justify-between pb-2 border-b border-slate-800">
        <h3 className="text-lg font-bold text-white">الاستماع إلى؟</h3>
        <button onClick={onClose} className="p-1 rounded-lg text-slate-400 hover:text-white">
          <X className="w-5 h-5" />
        </button>
      </div>

      <div className="space-y-1.5 text-base font-semibold text-slate-200">
        <button
          onClick={() => onStart(ayahNumber, ayahNumber, 1, 1)}
          className="w-full py-2.5 rounded-xl hover:bg-slate-800 transition-colors"
        >
          آية {ayahNumber}
        </button>
        <button
          onClick={() => onStart(firstAyahOnPage, lastAyahOnPage, 1, 1)}
          className="w-full py-2.5 rounded-xl hover:bg-slate-800 transition-colors"
        >
          هذه الصفحة كاملة
        </button>
        <button
          onClick={() => onStart(1, surahAyahCount, 1, 1)}
          className="w-full py-2.5 rounded-xl hover:bg-slate-800 transition-colors"
        >
          {surahName} كاملة من أولها
        </button>
        <button
          onClick={() => onStart(ayahNumber, surahAyahCount, 1, 1)}
          className="w-full py-2.5 rounded-xl hover:bg-slate-800 transition-colors"
        >
          من الآية {ayahNumber} إلى آخر السورة
        </button>
      </div>

      {/* نطاق مخصص وتكرار الحفظ */}
      <div className="pt-3 border-t border-slate-800 text-right space-y-3">
        <div className="flex items-center justify-between">
          <span className="text-xs font-bold text-emerald-400 flex items-center gap-1.5">
            <Sliders className="w-4 h-4" />
            <span>تخصيص نطاق الآيات والتكرار للحفظ</span>
          </span>
        </div>

        <div className="grid grid-cols-2 gap-3 text-xs">
          <div className="p-2.5 rounded-xl bg-slate-950 border border-slate-800 space-y-1">
            <span className="text-slate-400">من آية:</span>
            <input
              type="number"
              min={1}
              max={surahAyahCount}
              value={rangeStart}
              onChange={(e) => setRangeStart(parseInt(e.target.value, 10) || 1)}
              className="w-full bg-transparent text-center font-bold text-white text-sm focus:outline-none"
            />
          </div>
          <div className="p-2.5 rounded-xl bg-slate-950 border border-slate-800 space-y-1">
            <span className="text-slate-400">إلى آية:</span>
            <input
              type="number"
              min={rangeStart}
              max={surahAyahCount}
              value={rangeEnd}
              onChange={(e) => setRangeEnd(parseInt(e.target.value, 10) || rangeStart)}
              className="w-full bg-transparent text-center font-bold text-white text-sm focus:outline-none"
            />
          </div>
        </div>

        <div className="space-y-1.5">
          <span className="text-xs text-slate-300">تكرار كل آية لتثبيت الحفظ:</span>
          <div className="grid grid-cols-5 gap-1.5">
            {[1, 2, 3, 5, 10].map((num) => (
              <button
                key={num}
                onClick={() => setRepeatPerAyah(num)}
                className={`py-1.5 rounded-lg text-xs font-bold transition-colors ${
                  repeatPerAyah === num
                    ? 'bg-emerald-600 text-white shadow-sm'
                    : 'bg-slate-950 text-slate-400 hover:text-white'
                }`}
              >
                {num === 1 ? 'مرة' : `${num} مرات`}
              </button>
            ))}
          </div>
        </div>

        <div className="space-y-1.5">
          <span className="text-xs text-slate-300">تكرار المقطع كاملاً:</span>
          <div className="grid grid-cols-4 gap-1.5">
            {[1, 2, 3, 5].map((num) => (
              <button
                key={num}
                onClick={() => setRepeatEntireRange(num)}
                className={`py-1.5 rounded-lg text-xs font-bold transition-colors ${
                  repeatEntireRange === num
                    ? 'bg-teal-600 text-white shadow-sm'
                    : 'bg-slate-950 text-slate-400 hover:text-white'
                }`}
              >
                {num === 1 ? 'مرة' : `${num} مرات`}
              </button>
            ))}
          </div>
        </div>

        {/* القارئ — أسماء فقط بلا صور بطلب المالك */}
        <div className="space-y-1.5 pt-1">
          <span className="text-xs text-slate-300">القارئ:</span>
          <select
            value={selectedReciter.id}
            onChange={(e) => {
              const r = AYAH_RECITERS.find((rec) => rec.id === e.target.value);
              if (r) onSelectReciter(r);
            }}
            className="w-full bg-slate-950 border border-slate-800 text-xs text-slate-200 rounded-xl px-3 py-2.5 focus:outline-none"
          >
            {AYAH_RECITERS.map((rec) => (
              <option key={rec.id} value={rec.id} className="bg-slate-900 text-slate-100">
                {rec.name} {rec.country ? `(${rec.country})` : ''}
              </option>
            ))}
          </select>
        </div>

        <button
          onClick={() => onStart(rangeStart, rangeEnd, repeatPerAyah, repeatEntireRange)}
          className="w-full py-3 rounded-2xl bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white font-bold text-sm shadow-xl flex items-center justify-center gap-2 mt-2"
        >
          <Play className="w-4 h-4 fill-current" />
          <span>
            بدء التلاوة (الآيات {rangeStart} — {rangeEnd})
          </span>
        </button>
      </div>
    </div>
  </div>
);

/* ============================================================================
 * نافذة التفسير
 * ========================================================================== */
const TafseerModal: React.FC<{
  ayah: TajweedAyah;
  edition: MushafEditionStyle;
  onClose: () => void;
  onListen: () => void;
}> = ({ ayah, edition, onClose, onListen }) => (
  <div
    className="fixed inset-0 z-[96] flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in"
    onClick={onClose}
  >
    <div
      className="w-full max-w-lg rounded-3xl bg-slate-900 border border-slate-800 p-6 space-y-4 text-right max-h-[85vh] overflow-y-auto"
      onClick={(e) => e.stopPropagation()}
      dir="rtl"
    >
      <div className="flex items-center justify-between pb-3 border-b border-slate-800">
        <h3 className="text-base font-bold text-white flex items-center gap-2">
          <BookOpen className="w-5 h-5 text-emerald-400" />
          <span>التفسير الميسر • آية {ayah.ayahNumber}</span>
        </h3>
        <button
          onClick={onClose}
          className="p-1.5 rounded-xl bg-slate-800 text-slate-400 hover:text-white"
        >
          <X className="w-4 h-4" />
        </button>
      </div>

      <div
        className="p-4 rounded-2xl font-quran text-lg leading-relaxed border-2"
        style={{
          backgroundColor: edition.paper,
          color: edition.ink,
          borderColor: edition.frame,
        }}
      >
        ﴿{ayah.plainText}﴾
      </div>

      <div className="space-y-2">
        <h4 className="text-xs font-bold text-emerald-400">التفسير وبيان المعنى:</h4>
        {ayah.tafseer ? (
          <p className="text-sm text-slate-300 leading-relaxed font-sans bg-slate-950 p-4 rounded-2xl border border-slate-800/80">
            {ayah.tafseer}
          </p>
        ) : (
          <p className="text-sm text-slate-400 leading-relaxed font-sans bg-slate-950 p-4 rounded-2xl border border-slate-800/80 text-center">
            التفسير الميسر متاح تدريجياً — هذه الصفحة من نص المصحف الكامل المعتمد (رسم عثماني).
          </p>
        )}
      </div>

      <button
        onClick={onListen}
        className="w-full py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold flex items-center justify-center gap-2"
      >
        <Play className="w-4 h-4 fill-current" />
        <span>الاستماع لتلاوة هذه الآية</span>
      </button>
    </div>
  </div>
);

/* ============================================================================
 * قائمة الآية الزجاجية المنسقة — تظهر عند النقر أو الضغطة الطويلة
 * ========================================================================== */
interface AyahContextMenuProps {
  ayahNumber: number;
  surahName: string;
  x: number;
  y: number;
  onClose: () => void;
  onListen: () => void;
  onTafseer: () => void;
  onBookmark: () => void;
  onCopy: () => void;
  onShare: () => void;
}

const AyahContextMenu: React.FC<AyahContextMenuProps> = ({
  ayahNumber,
  surahName,
  x,
  y,
  onClose,
  onListen,
  onTafseer,
  onBookmark,
  onCopy,
  onShare,
}) => (
  <>
    <div className="absolute inset-0 z-20" onClick={onClose} />

    <div
      dir="rtl"
      className="absolute z-30 w-[260px] rounded-3xl border shadow-2xl backdrop-blur-2xl overflow-hidden animate-in fade-in zoom-in-95 duration-150"
      style={{
        left: `${x}px`,
        top: `${y}px`,
        backgroundColor: 'color-mix(in srgb, var(--noor-n-900) 94%, transparent)',
        borderColor: 'color-mix(in srgb, var(--noor-p-500) 35%, transparent)',
        boxShadow: '0 24px 60px rgba(0,0,0,0.55)',
      }}
    >
      <div
        className="px-4 py-3 border-b flex items-center justify-between"
        style={{
          background: `linear-gradient(135deg, color-mix(in srgb, var(--noor-p-500) 30%, transparent), color-mix(in srgb, var(--noor-a-500) 18%, transparent))`,
          borderColor: 'color-mix(in srgb, var(--noor-p-500) 25%, transparent)',
        }}
      >
        <div>
          <div className="text-xs font-extrabold" style={{ color: 'var(--noor-n-50)' }}>
            {surahName} — آية {ayahNumber}
          </div>
          <div className="text-[10px] mt-0.5" style={{ color: 'var(--noor-n-400)' }}>
            اختر ما تريد فعله بالآية
          </div>
        </div>
        <button
          onClick={onClose}
          className="p-1.5 rounded-xl transition-colors hover:bg-white/10"
          style={{ color: 'var(--noor-n-300)' }}
          aria-label="إغلاق"
        >
          <X className="w-4 h-4" />
        </button>
      </div>

      <div className="p-2 space-y-1">
        <ContextMenuAction
          onClick={onListen}
          icon={<Play className="w-4 h-4 fill-current" />}
          label="استماع وتحديد نطاق"
          hint="تلاوة الآية أو مجموعة آيات بصوت القارئ"
          color="#10b981"
        />
        <ContextMenuAction
          onClick={onTafseer}
          icon={<BookOpen className="w-4 h-4" />}
          label="التفسير والتدبر"
          hint="عرض التفسير الميسر للآية"
          color="#14b8a6"
        />
        <ContextMenuAction
          onClick={onBookmark}
          icon={<Bookmark className="w-4 h-4" />}
          label="حفظ الآية"
          hint="إضافة لإشارات الحفظ المرجعية"
          color="#f59e0b"
        />
        <ContextMenuAction
          onClick={onCopy}
          icon={<Copy className="w-4 h-4" />}
          label="نسخ الآية"
          hint="نسخ نص الآية إلى الحافظة"
          color="#0ea5e9"
        />
        <ContextMenuAction
          onClick={onShare}
          icon={<Share2 className="w-4 h-4" />}
          label="مشاركة الآية"
          hint="مشاركة الآية مع الأهل والأصدقاء"
          color="#f43f5e"
        />
      </div>
    </div>
  </>
);

/** صف إجراء واحد داخل قائمة الآية */
const ContextMenuAction: React.FC<{
  onClick: () => void;
  icon: React.ReactNode;
  label: string;
  hint: string;
  color: string;
}> = ({ onClick, icon, label, hint, color }) => (
  <button
    onClick={onClick}
    className="w-full flex items-center gap-3 px-2.5 py-2.5 rounded-2xl transition-colors hover:bg-white/[0.07] text-right group"
  >
    <span
      className="w-9 h-9 rounded-xl flex items-center justify-center shrink-0 text-white shadow-md transition-transform group-hover:scale-105"
      style={{ backgroundColor: color }}
    >
      {icon}
    </span>
    <span className="flex-1 min-w-0">
      <span className="block text-xs font-bold" style={{ color: 'var(--noor-n-100)' }}>
        {label}
      </span>
      <span className="block text-[10px] mt-0.5" style={{ color: 'var(--noor-n-400)' }}>
        {hint}
      </span>
    </span>
  </button>
);
