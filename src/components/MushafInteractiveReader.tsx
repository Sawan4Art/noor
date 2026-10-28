import React, { useState, useRef, useEffect, useCallback } from 'react';
import {
  Play,
  Pause,
  Bookmark,
  Share2,
  Copy,
  BookOpen,
  Info,
  X,
  Volume2,
  ChevronRight,
  ChevronLeft,
  Search,
  Sliders,
  Check,
  Eye,
  EyeOff,
  Maximize2,
  Minimize2,
  ListFilter,
  Sparkles,
} from 'lucide-react';
import {
  TajweedAyah,
  MushafPage,
  TAJWEED_COLORS,
  AYAH_RECITERS,
  AyahReciter,
  getAyahAudioUrl,
  MUSHAF_PAGES_DATA,
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
  parseSurahAyahQuery,
} from '../utils/quranSearch';
import { SURAHS_LIST, Surah } from '../utils/quranData';

/**
 * Synthesizes a gentle, authentic paper rustle sound using Web Audio API
 */
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
    // Ignore audio autoplay policies gracefully
  }
}

export const MushafInteractiveReader: React.FC = () => {
  // Page Navigation State
  const [currentPageNum, setCurrentPageNum] = useState<number>(4); // Default to Page 4 matching user screenshot
  const [activeAyah, setActiveAyah] = useState<TajweedAyah | null>(null);
  const [showRadialMenu, setShowRadialMenu] = useState<boolean>(false);
  const [radialMenuPos, setRadialMenuPos] = useState<{ x: number; y: number }>({ x: 0, y: 0 });

  // Fullscreen & Book Flipping state
  const [isFullscreen, setIsFullscreen] = useState<boolean>(false);
  const [showSurahListModal, setShowSurahListModal] = useState<boolean>(false);
  const [surahSearchQuery, setSurahSearchQuery] = useState<string>('');
  const [surahTabFilter, setSurahTabFilter] = useState<'all' | 'meccan' | 'medinan'>('all');
  const [flipDirection, setFlipDirection] = useState<'next' | 'prev' | null>(null);

  // Modals state
  const [showListenModal, setShowListenModal] = useState<boolean>(false);
  const [showTafseerModal, setShowTafseerModal] = useState<boolean>(false);
  const [showSearchModal, setShowSearchModal] = useState<boolean>(false);
  const [showBookmarksModal, setShowBookmarksModal] = useState<boolean>(false);

  // Custom Audio Range Playback & Memorization State
  const [selectedReciter, setSelectedReciter] = useState<AyahReciter>(AYAH_RECITERS[0]); // Nasser Al-Qatami default
  const [rangeStartAyah, setRangeStartAyah] = useState<number>(17);
  const [rangeEndAyah, setRangeEndAyah] = useState<number>(24);
  const [repeatPerAyah, setRepeatPerAyah] = useState<number>(1);
  const [repeatEntireRange, setRepeatEntireRange] = useState<number>(1);
  const [currentPlayingAyahNum, setCurrentPlayingAyahNum] = useState<number | null>(null);
  const [isPlayingRange, setIsPlayingRange] = useState<boolean>(false);
  const [currentAyahRepeatCount, setCurrentAyahRepeatCount] = useState<number>(0);
  const [currentRangeCycleCount, setCurrentRangeCycleCount] = useState<number>(0);
  const [copiedToast, setCopiedToast] = useState<string | null>(null);

  // Memorization Mode (Mask Words)
  const [memorizationTestMode, setMemorizationTestMode] = useState<boolean>(false);
  const [revealedAyahs, setRevealedAyahs] = useState<Record<number, boolean>>({});

  // Search Input State
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [searchResults, setSearchResults] = useState<SearchResult[]>([]);
  const [bookmarks, setBookmarks] = useState<BookmarkItem[]>(getSavedBookmarks());

  const audioRef = useRef<HTMLAudioElement | null>(null);
  const pageContainerRef = useRef<HTMLDivElement | null>(null);
  const touchStartX = useRef<number | null>(null);
  const touchStartY = useRef<number | null>(null);

  // تحميل نص المصحف الكامل (604 صفحة) — بإعادة رسم فور الجاهزية
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

  const currentPage: MushafPage = getOrCreateMushafPage(currentPageNum);

  // Handle book page flip (Arabic RTL convention: next page is to the left, prev page is to the right)
  const handleFlipPage = useCallback(
    (direction: 'next' | 'prev') => {
      playPaperTurnSound();
      setFlipDirection(direction);

      setTimeout(() => {
        if (direction === 'next') {
          setCurrentPageNum((p) => Math.min(604, p + 1));
        } else {
          setCurrentPageNum((p) => Math.max(1, p - 1));
        }
        setShowRadialMenu(false);
      }, 140);

      setTimeout(() => {
        setFlipDirection(null);
      }, 340);
    },
    []
  );

  // Select Surah from list -> Jump to page and open full screen!
  const handleSelectSurah = (surah: Surah) => {
    setCurrentPageNum(surah.page);
    setIsFullscreen(true);
    setShowSurahListModal(false);
    playPaperTurnSound();
    setCopiedToast(`تم فتح ${surah.name} بملء الشاشة - صفحة ${surah.page}`);
    setTimeout(() => setCopiedToast(null), 3500);
  };

  // Keyboard navigation for page flipping (ArrowLeft: next page, ArrowRight: prev page)
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (
        showSearchModal ||
        showBookmarksModal ||
        showListenModal ||
        showSurahListModal ||
        showTafseerModal
      ) {
        return;
      }
      if (e.key === 'ArrowLeft') {
        handleFlipPage('next');
      } else if (e.key === 'ArrowRight') {
        handleFlipPage('prev');
      } else if (e.key === 'f' || e.key === 'F') {
        setIsFullscreen((prev) => !prev);
      } else if (e.key === 'Escape' && isFullscreen) {
        setIsFullscreen(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [
    isFullscreen,
    showSearchModal,
    showBookmarksModal,
    showListenModal,
    showSurahListModal,
    showTafseerModal,
    handleFlipPage,
  ]);

  // Touch Swipe Handling for real book gesture
  const handleTouchStart = (e: React.TouchEvent) => {
    touchStartX.current = e.touches[0].clientX;
    touchStartY.current = e.touches[0].clientY;
  };

  const handleTouchEnd = (e: React.TouchEvent) => {
    if (touchStartX.current === null || touchStartY.current === null) return;
    const deltaX = e.changedTouches[0].clientX - touchStartX.current;
    const deltaY = e.changedTouches[0].clientY - touchStartY.current;

    if (Math.abs(deltaX) > 45 && Math.abs(deltaX) > Math.abs(deltaY)) {
      if (deltaX < 0) {
        // Swiped Left -> advance to next page in Arabic book
        handleFlipPage('next');
      } else {
        // Swiped Right -> go back to previous page
        handleFlipPage('prev');
      }
    }
    touchStartX.current = null;
    touchStartY.current = null;
  };

  // Handle Ayah click -> Position radial action menu
  const handleAyahClick = (ayah: TajweedAyah, event: React.MouseEvent) => {
    setActiveAyah(ayah);
    setRangeStartAyah(ayah.ayahNumber);
    setRangeEndAyah(Math.max(ayah.ayahNumber, rangeEndAyah));

    if (pageContainerRef.current) {
      const rect = pageContainerRef.current.getBoundingClientRect();
      const clickX = event.clientX - rect.left;
      const clickY = event.clientY - rect.top;
      setRadialMenuPos({ x: clickX, y: clickY });
    }
    setShowRadialMenu(true);
  };

  // Play audio for a specific ayah or custom range
  const playAyahAudio = (surahNum: number, ayahNum: number) => {
    const url = getAyahAudioUrl(selectedReciter.folder, surahNum, ayahNum);
    if (!audioRef.current) {
      audioRef.current = new Audio(url);
    } else {
      audioRef.current.src = url;
    }

    setCurrentPlayingAyahNum(ayahNum);
    setIsPlayingRange(true);

    audioRef.current.onended = () => {
      if (currentAyahRepeatCount + 1 < repeatPerAyah) {
        setCurrentAyahRepeatCount((c) => c + 1);
        audioRef.current?.play().catch(() => {});
        return;
      }

      setCurrentAyahRepeatCount(0);
      if (ayahNum < rangeEndAyah) {
        playAyahAudio(surahNum, ayahNum + 1);
      } else {
        if (currentRangeCycleCount + 1 < repeatEntireRange) {
          setCurrentRangeCycleCount((c) => c + 1);
          playAyahAudio(surahNum, rangeStartAyah);
        } else {
          stopAudio();
        }
      }
    };

    audioRef.current.play().catch(() => {
      // Audio stream fallback
    });
  };

  const stopAudio = () => {
    if (audioRef.current) {
      audioRef.current.pause();
      audioRef.current.currentTime = 0;
    }
    setIsPlayingRange(false);
    setCurrentPlayingAyahNum(null);
    setCurrentAyahRepeatCount(0);
    setCurrentRangeCycleCount(0);
  };

  const handleStartCustomRange = () => {
    setShowListenModal(false);
    setShowRadialMenu(false);
    setCurrentAyahRepeatCount(0);
    setCurrentRangeCycleCount(0);
    playAyahAudio(currentPage.ayahs[0].surahNumber, rangeStartAyah);
  };

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
    setShowRadialMenu(false);
    setCopiedToast(`تمت إضافة الآية ${activeAyah.ayahNumber} إلى إشارات الحفظ المرجعية`);
    setTimeout(() => setCopiedToast(null), 3000);
  };

  const handleCopyAyah = () => {
    if (!activeAyah) return;
    navigator.clipboard.writeText(
      `﴿${activeAyah.plainText}﴾ [${currentPage.surahName}: ${activeAyah.ayahNumber}]`
    );
    setShowRadialMenu(false);
    setCopiedToast('تم نسخ نص الآية الكريمة إلى الحافظة');
    setTimeout(() => setCopiedToast(null), 3000);
  };

  const handleShareAyah = () => {
    if (!activeAyah) return;
    const shareText = `﴿${activeAyah.plainText}﴾\n[${currentPage.surahName} - آية ${activeAyah.ayahNumber}]`;
    if (navigator.share) {
      navigator.share({ title: currentPage.surahName, text: shareText }).catch(() => {});
    } else {
      navigator.clipboard.writeText(shareText);
      setCopiedToast('تم نسخ رابط ونَص الآية للمشاركة');
      setTimeout(() => setCopiedToast(null), 3000);
    }
    setShowRadialMenu(false);
  };

  const handleSearchChange = (q: string) => {
    setSearchQuery(q);
    if (q.trim().length >= 2) {
      setSearchResults(searchQuran(q));
    } else {
      setSearchResults([]);
    }
  };

  const handleSelectSearchResult = (res: SearchResult) => {
    setCurrentPageNum(res.pageNumber);
    setShowSearchModal(false);
    const page = getOrCreateMushafPage(res.pageNumber);
    const match = page.ayahs.find((a) => a.ayahNumber === res.ayahNumber);
    if (match) {
      setActiveAyah(match);
      setRangeStartAyah(match.ayahNumber);
    }
  };

  // Filter 114 Surahs for the selection modal
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

  // عدد آيات السورة المعروضة — لنطاقات تشغيل صحيحة بدل الثوابت القديمة (286/141)
  const displayedSurahNumber = activeAyah?.surahNumber ?? currentPage.ayahs[0]?.surahNumber ?? 1;
  const surahAyahCount = SURAHS_LIST.find((s) => s.number === displayedSurahNumber)?.numberOfAyahs ?? 286;

  /** تدفق الآيات بالتلوين التجويدي — مشترك بين العرض العادي وملء الشاشة */
  const renderAyahFlow = (textClass: string, numberSize: string) => (
    <div className={`text-justify font-quran text-slate-950 font-normal dir-rtl px-1 ${textClass}`}>
      {currentPage.ayahs.map((ayah) => {
        const isSelected = activeAyah?.ayahNumber === ayah.ayahNumber;
        const isPlaying = currentPlayingAyahNum === ayah.ayahNumber;
        const isMasked = memorizationTestMode && !revealedAyahs[ayah.ayahNumber];

        return (
          <span
            key={`${ayah.surahNumber}-${ayah.ayahNumber}`}
            onClick={(e) => handleAyahClick(ayah, e)}
            className={`inline cursor-pointer transition-colors duration-150 rounded px-1 py-0.5 mx-0.5 ${
              isPlaying
                ? 'bg-emerald-200/90 shadow-sm ring-2 ring-emerald-500'
                : isSelected
                ? 'bg-amber-200/80 shadow-sm ring-2 ring-amber-400'
                : 'hover:bg-amber-100/60'
            }`}
            title={`انقر للتفاعل مع الآية ${ayah.ayahNumber}`}
          >
            {isMasked ? (
              <span
                onClick={(e) => {
                  e.stopPropagation();
                  setRevealedAyahs((prev) => ({ ...prev, [ayah.ayahNumber]: true }));
                }}
                className="filter blur-sm bg-slate-300 text-transparent select-none cursor-pointer px-2 py-0.5 rounded"
                title="انقر لإظهار الكلمات والتحقق من حفظك"
              >
                {ayah.plainText}
              </span>
            ) : (
              ayah.segments.map((seg, sIdx) => {
                const ruleStyle = seg.rule ? TAJWEED_COLORS[seg.rule] : null;
                return (
                  <span
                    key={sIdx}
                    style={ruleStyle ? { color: ruleStyle.color, fontWeight: 700 } : {}}
                  >
                    {seg.text}
                  </span>
                );
              })
            )}

            {/* رقم الآية بالزخرفة العثمانية ﴿١٧﴾ */}
            <span className={`inline-flex items-center justify-center font-sans font-bold text-[#8c6b23] border border-[#dfbe72] bg-[#fbf6e3] rounded-full mx-1 shadow-inner align-middle ${numberSize}`}>
              {ayah.ayahNumber}
            </span>
          </span>
        );
      })}
    </div>
  );

  /* ==================== وضع ملء الشاشة الغامر — الصفحة تغطي الشاشة كلها ==================== */
  if (isFullscreen) {
    return (
      <div className="fixed inset-0 z-[90] flex flex-col bg-[#05070d]" dir="rtl">
        {/* Toast */}
        {copiedToast && (
          <div className="fixed top-4 left-1/2 -translate-x-1/2 z-[95] px-5 py-3 bg-emerald-600 text-white rounded-2xl shadow-2xl flex items-center gap-3 text-xs sm:text-sm">
            <Check className="w-4 h-4 text-emerald-200" />
            <span>{copiedToast}</span>
            <button onClick={() => setCopiedToast(null)} className="text-xs text-emerald-200 hover:text-white px-2 py-0.5">
              إغلاق
            </button>
          </div>
        )}

        {/* شريط علوي رفيع */}
        <div
          className="flex items-center justify-between gap-2 px-3 sm:px-5 py-2 border-b backdrop-blur-md shrink-0"
          style={{
            backgroundColor: 'color-mix(in srgb, var(--noor-n-950) 82%, transparent)',
            borderColor: 'color-mix(in srgb, var(--noor-p-500) 25%, transparent)',
          }}
        >
          <div className="flex items-center gap-2 min-w-0">
            <BookOpen className="w-4 h-4 shrink-0" style={{ color: 'var(--noor-a-400)' }} />
            <span className="text-xs sm:text-sm font-bold truncate" style={{ color: 'var(--noor-n-100)' }}>
              {currentPage.surahName}
            </span>
            <span className="text-[10px] sm:text-xs px-2 py-0.5 rounded-full border shrink-0" style={{
              color: 'var(--noor-n-400)',
              borderColor: 'color-mix(in srgb, var(--noor-p-500) 30%, transparent)',
            }}>
              الجزء {currentPage.juzNumber} • {currentPage.hizbQuarter}
            </span>
          </div>
          <button
            onClick={() => setIsFullscreen(false)}
            className="p-2 rounded-xl border transition-colors shrink-0"
            style={{
              borderColor: 'color-mix(in srgb, var(--noor-p-500) 30%, transparent)',
              color: 'var(--noor-n-300)',
            }}
            title="الخروج من ملء الشاشة (Esc)"
          >
            <Minimize2 className="w-4 h-4" />
          </button>
        </div>

        {/* الصفحة — تغطي المساحة المتاحة بالكامل */}
        <div className="flex-1 flex items-stretch justify-center p-1.5 sm:p-3 min-h-0">
          <div
            ref={pageContainerRef}
            onTouchStart={handleTouchStart}
            onTouchEnd={handleTouchEnd}
            className={`relative w-full max-w-4xl bg-[#fffdf5] text-slate-900 rounded-xl sm:rounded-2xl shadow-2xl overflow-hidden border-[6px] sm:border-[10px] border-[#dfbe72] ring-2 ring-[#997a31] select-none book-spine-shadow ${
              flipDirection === 'next'
                ? 'book-flip-forward'
                : flipDirection === 'prev'
                ? 'book-flip-backward'
                : ''
            }`}
          >
            <div className="h-full p-2 sm:p-4 border-4 border-[#3b8270] m-1 rounded-lg flex flex-col min-h-0">
              {/* تدفق الآيات — يملأ المتاح ويمكن تمريره لو زاد */}
              <div className="flex-1 overflow-y-auto min-h-0">
                {renderAyahFlow('leading-[2.5] text-lg sm:text-2xl', 'w-6 h-6 sm:w-7 sm:h-7 text-xs sm:text-sm')}
              </div>

              {/* تذييل الصفحة */}
              <div className="shrink-0 pt-2 border-t-2 border-[#dfbe72] flex items-center justify-between text-xs font-bold text-slate-700 px-2">
                <span>جزء {currentPage.juzNumber}</span>
                <span className="text-sm font-extrabold text-[#997a31]">{currentPage.pageNumber}</span>
                <span>{currentPage.surahName}</span>
              </div>
            </div>

            {showRadialMenu && (
              <div
                className="absolute z-30 pointer-events-auto"
                style={{
                  left: `${Math.min(Math.max(radialMenuPos.x, 100), 500)}px`,
                  top: `${Math.min(Math.max(radialMenuPos.y, 100), 750)}px`,
                }}
              >
                <RadialMenuActions
                  onClose={() => setShowRadialMenu(false)}
                  onListen={() => { setShowRadialMenu(false); setShowListenModal(true); }}
                  onBookmark={handleBookmarkCurrentAyah}
                  onTafseer={() => { setShowRadialMenu(false); setShowTafseerModal(true); }}
                  onShare={handleShareAyah}
                  onCopy={handleCopyAyah}
                />
              </div>
            )}
          </div>
        </div>

        {/* شريط تنقل الصفحات السفلي الرفيع */}
        <div
          className="shrink-0 flex items-center justify-center gap-2 sm:gap-3 py-2 pb-[max(0.5rem,env(safe-area-inset-bottom))] border-t backdrop-blur-md"
          style={{
            backgroundColor: 'color-mix(in srgb, var(--noor-n-950) 82%, transparent)',
            borderColor: 'color-mix(in srgb, var(--noor-p-500) 25%, transparent)',
          }}
        >
          <button
            onClick={() => handleFlipPage('prev')}
            disabled={currentPageNum <= 1}
            className="p-2.5 rounded-xl disabled:opacity-30 transition-colors"
            style={{ color: 'var(--noor-n-200)' }}
            title="الصفحة السابقة"
          >
            <ChevronRight className="w-5 h-5" />
          </button>

          <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-2xl border text-sm" style={{
            borderColor: 'color-mix(in srgb, var(--noor-p-500) 30%, transparent)',
          }}>
            <span className="text-xs" style={{ color: 'var(--noor-n-400)' }}>صفحة</span>
            <input
              type="number"
              min={1}
              max={604}
              value={currentPageNum}
              onChange={(e) => {
                const p = parseInt(e.target.value, 10);
                if (p >= 1 && p <= 604) setCurrentPageNum(p);
              }}
              className="w-12 bg-transparent text-center font-bold focus:outline-none"
              style={{ color: 'var(--noor-p-400)' }}
            />
            <span className="text-xs" style={{ color: 'var(--noor-n-500)' }}>/ 604</span>
          </div>

          {!pagesReady && (
            <span className="text-[10px] px-2 py-1 rounded-lg animate-pulse" style={{ color: 'var(--noor-a-400)' }}>
              جاري تحميل نص المصحف…
            </span>
          )}

          <button
            onClick={() => handleFlipPage('next')}
            disabled={currentPageNum >= 604}
            className="p-2.5 rounded-xl disabled:opacity-30 transition-colors"
            style={{ color: 'var(--noor-n-200)' }}
            title="الصفحة التالية"
          >
            <ChevronLeft className="w-5 h-5" />
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-5">
      {/* Toast Alert */}
      {copiedToast && (
        <div className="fixed top-4 left-1/2 -translate-x-1/2 z-50 px-5 py-3 bg-emerald-600 text-white rounded-2xl shadow-2xl flex items-center gap-3 text-xs sm:text-sm animate-in fade-in slide-in-from-top-4 border border-emerald-400/40">
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

      {/* Top Action Toolbar (Search, Surahs List, Reciters, Fullscreen Toggle) */}
      <div className="p-3.5 sm:p-4 rounded-3xl bg-slate-900/95 border border-slate-800 shadow-2xl flex flex-wrap items-center justify-between gap-3 backdrop-blur-md sticky top-0 z-40">
        {/* Right side: Surah Selection Button & Current Page */}
        <div className="flex items-center gap-2">
          <button
            onClick={() => setShowSurahListModal(true)}
            className="flex items-center gap-2 px-4 py-2.5 rounded-2xl bg-gradient-to-r from-amber-600 to-amber-700 hover:from-amber-500 hover:to-amber-600 text-white text-xs sm:text-sm font-bold shadow-lg shadow-amber-950/40 transition-all active:scale-95"
            title="فتح فهرس سور القرآن الـ 114"
          >
            <BookOpen className="w-4 h-4 text-amber-200" />
            <span>فهرس السور (114)</span>
          </button>

          {/* Page navigation controls with book flip indication */}
          <div className="flex items-center gap-1.5 bg-slate-950 border border-slate-800 rounded-2xl p-1">
            <button
              onClick={() => handleFlipPage('prev')}
              disabled={currentPageNum <= 1}
              className="p-2 rounded-xl text-slate-300 hover:text-white hover:bg-slate-800 disabled:opacity-30 transition-colors"
              title="الصفحة السابقة (تقليب لليمين)"
            >
              <ChevronRight className="w-4 h-4" />
            </button>

            <div className="flex items-center gap-1 px-2.5 py-1 text-xs">
              <span className="text-slate-400">صفحة</span>
              <input
                type="number"
                min={1}
                max={604}
                value={currentPageNum}
                onChange={(e) => {
                  const p = parseInt(e.target.value, 10);
                  if (p >= 1 && p <= 604) setCurrentPageNum(p);
                }}
                className="w-12 bg-transparent text-center font-bold text-emerald-400 focus:outline-none"
              />
              <span className="text-slate-500">/ 604</span>
            </div>

            <button
              onClick={() => handleFlipPage('next')}
              disabled={currentPageNum >= 604}
              className="p-2 rounded-xl text-slate-300 hover:text-white hover:bg-slate-800 disabled:opacity-30 transition-colors"
              title="الصفحة التالية (تقليب لليسار)"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Center: Reciter Picker (16 Famous Reciters including Egyptian Masters & Nasser Al-Qatami) */}
        <div className="flex items-center gap-2">
          <div className="relative">
            <select
              value={selectedReciter.id}
              onChange={(e) => {
                const r = AYAH_RECITERS.find((rec) => rec.id === e.target.value);
                if (r) setSelectedReciter(r);
              }}
              className="bg-slate-950 border border-slate-800 text-xs text-slate-200 rounded-2xl px-3.5 py-2.5 focus:outline-none focus:ring-2 focus:ring-emerald-500 font-medium"
              aria-label="اختر القارئ لتلاوة الآيات"
            >
              {AYAH_RECITERS.map((rec) => (
                <option key={rec.id} value={rec.id} className="bg-slate-900 text-slate-100 py-1">
                  🎙️ {rec.name} {rec.country ? `(${rec.country})` : ''}
                </option>
              ))}
            </select>
          </div>

          {isPlayingRange && (
            <button
              onClick={stopAudio}
              className="flex items-center gap-1.5 px-3.5 py-2 rounded-2xl bg-rose-600 hover:bg-rose-500 text-white text-xs font-bold animate-pulse shadow-md"
            >
              <Pause className="w-3.5 h-3.5" />
              <span>إيقاف التلاوة (آية {currentPlayingAyahNum})</span>
            </button>
          )}
        </div>

        {/* Left Tools: Search, Memorization, Bookmarks, and Fullscreen Toggle */}
        <div className="flex items-center gap-2">
          <button
            onClick={() => setMemorizationTestMode(!memorizationTestMode)}
            className={`flex items-center gap-1.5 px-3 py-2 rounded-2xl border text-xs font-semibold transition-all ${
              memorizationTestMode
                ? 'bg-amber-500/20 text-amber-300 border-amber-500/40 shadow-md'
                : 'bg-slate-950 text-slate-400 border-slate-800 hover:text-white'
            }`}
            title="تفعيل وضع إخفاء الكلمات لاختبار الحفظ"
          >
            {memorizationTestMode ? <EyeOff className="w-3.5 h-3.5 text-amber-400" /> : <Eye className="w-3.5 h-3.5" />}
            <span className="hidden sm:inline">{memorizationTestMode ? 'وضع التسميع مفعّل' : 'تسميع واختبار الحفظ'}</span>
          </button>

          <button
            onClick={() => setShowBookmarksModal(true)}
            className="flex items-center gap-1.5 px-3 py-2 rounded-2xl bg-slate-950 border border-slate-800 text-slate-300 hover:text-white text-xs font-medium"
            title="المحفوظات والإشارات المرجعية"
          >
            <Bookmark className="w-3.5 h-3.5 text-emerald-400" />
            <span className="hidden sm:inline">المحفوظات ({bookmarks.length})</span>
          </button>

          <button
            onClick={() => setShowSearchModal(true)}
            className="flex items-center gap-1.5 px-3.5 py-2 rounded-2xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold shadow-md shadow-emerald-950/60"
            title="البحث بالآيات والأرقام"
          >
            <Search className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">بحث</span>
          </button>

          {/* Fullscreen Button */}
          <button
            onClick={() => setIsFullscreen(!isFullscreen)}
            className={`p-2.5 rounded-2xl border transition-all ${
              isFullscreen
                ? 'bg-amber-500/20 border-amber-500/40 text-amber-300 hover:bg-amber-500/30'
                : 'bg-slate-950 border-slate-800 text-slate-400 hover:text-white'
            }`}
            title={isFullscreen ? 'الخروج من ملء الشاشة' : 'المصحف بكامل الشاشة'}
          >
            {isFullscreen ? <Minimize2 className="w-4 h-4" /> : <Maximize2 className="w-4 h-4" />}
          </button>
        </div>
      </div>

      {/* Book Reading Stage with Realistic Flap Navigation and 3D Flip Animation */}
      <div className="relative flex items-center justify-center gap-2 max-w-5xl mx-auto">
        {/* Left Book Page Edge Clicker: Next Page (الصفحة التالية - لليسار) */}
        <button
          onClick={() => handleFlipPage('next')}
          disabled={currentPageNum >= 604}
          className="hidden md:flex flex-col items-center justify-center w-12 h-96 rounded-r-2xl bg-gradient-to-r from-slate-900/60 to-slate-950/90 hover:from-emerald-950/80 hover:to-slate-900 border border-slate-800 text-slate-400 hover:text-emerald-300 transition-all group disabled:opacity-20 cursor-pointer shadow-xl"
          title="الصفحة التالية (تقليب صفحات الكتاب لليسار)"
        >
          <ChevronLeft className="w-6 h-6 group-hover:-translate-x-1 transition-transform" />
          <span className="text-[10px] [writing-mode:vertical-rl] font-bold mt-2 tracking-wider">
            الصفحة التالية ⮜
          </span>
        </button>

        {/* Main Ornate Tajweed Quran Mushaf Canvas (Matching User Screenshots with 3D Flip Animation) */}
        <div
          ref={pageContainerRef}
          onTouchStart={handleTouchStart}
          onTouchEnd={handleTouchEnd}
          className={`relative mx-auto w-full max-w-2xl bg-[#fffdf5] text-slate-900 rounded-2xl shadow-2xl overflow-hidden border-[10px] border-[#dfbe72] ring-2 ring-[#997a31] transition-transform select-none book-spine-shadow ${
            flipDirection === 'next'
              ? 'book-flip-forward'
              : flipDirection === 'prev'
              ? 'book-flip-backward'
              : ''
          }`}
        >
          {/* Inner Ornamental Frame Border */}
          <div className="p-3 sm:p-5 border-4 border-[#3b8270] m-1 rounded-lg relative">
            {/* Header Banner: Juz and Surah Name */}
            <div className="flex items-center justify-between text-xs font-bold text-slate-800 pb-2 mb-3 border-b-2 border-[#dfbe72]">
              <div className="px-3 py-1 rounded bg-[#dfbe72]/20 border border-[#dfbe72] text-[11px] font-sans">
                {currentPage.hizbQuarter} • الجزء {currentPage.juzNumber}
              </div>

              <div
                onClick={() => setShowSurahListModal(true)}
                className="px-4 py-1 rounded bg-[#3b8270]/15 border border-[#3b8270] text-sm font-bold font-arabic text-[#1f594b] cursor-pointer hover:bg-[#3b8270]/25 transition-colors"
                title="انقر لتغيير السورة"
              >
                {currentPage.surahName} ▾
              </div>
            </div>

            {/* Ayahs Flow with Tajweed Colors and End Markers */}
            <div className="text-justify leading-[3.2] font-quran text-xl sm:text-2xl text-slate-950 font-normal dir-rtl px-1">
              {currentPage.ayahs.map((ayah) => {
                const isSelected = activeAyah?.ayahNumber === ayah.ayahNumber;
                const isPlaying = currentPlayingAyahNum === ayah.ayahNumber;
                const isMasked = memorizationTestMode && !revealedAyahs[ayah.ayahNumber];

                return (
                  <span
                    key={ayah.ayahNumber}
                    onClick={(e) => handleAyahClick(ayah, e)}
                    className={`inline cursor-pointer transition-colors duration-150 rounded px-1 py-0.5 mx-0.5 ${
                      isPlaying
                        ? 'bg-emerald-200/90 shadow-sm ring-2 ring-emerald-500'
                        : isSelected
                        ? 'bg-amber-200/80 shadow-sm ring-2 ring-amber-400'
                        : 'hover:bg-amber-100/60'
                    }`}
                    title={`انقر للتفاعل مع الآية ${ayah.ayahNumber}`}
                  >
                    {isMasked ? (
                      <span
                        onClick={(e) => {
                          e.stopPropagation();
                          setRevealedAyahs((prev) => ({ ...prev, [ayah.ayahNumber]: true }));
                        }}
                        className="filter blur-sm bg-slate-300 text-transparent select-none cursor-pointer px-2 py-0.5 rounded"
                        title="انقر لإظهار الكلمات والتحقق من حفظك"
                      >
                        {ayah.plainText}
                      </span>
                    ) : (
                      ayah.segments.map((seg, sIdx) => {
                        const ruleStyle = seg.rule ? TAJWEED_COLORS[seg.rule] : null;
                        return (
                          <span
                            key={sIdx}
                            style={ruleStyle ? { color: ruleStyle.color, fontWeight: 700 } : {}}
                          >
                            {seg.text}
                          </span>
                        );
                      })
                    )}

                    {/* Ornamental Ayah Number Circle ﴿١٧﴾ */}
                    <span className="inline-flex items-center justify-center font-sans font-bold text-xs sm:text-sm text-[#8c6b23] border border-[#dfbe72] bg-[#fbf6e3] rounded-full w-6 h-6 sm:w-7 sm:h-7 mx-1 shadow-inner align-middle">
                      {ayah.ayahNumber}
                    </span>
                  </span>
                );
              })}
            </div>

            {/* Footer of Mushaf Page (Tajweed Guide and Page Info) */}
            <div className="mt-6 pt-3 border-t-2 border-[#dfbe72] space-y-2 text-slate-800">
              {/* Tajweed Legend Bar matching User Screenshot */}
              <div className="p-2 rounded bg-[#f4ece0] border border-[#dfbe72] text-[10px] sm:text-[11px] font-sans flex flex-wrap items-center justify-center gap-x-3 gap-y-1">
                <span className="flex items-center gap-1">
                  <span className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: TAJWEED_COLORS.madd_6.color }} />
                  <span>مد 6 حركات لزوماً</span>
                </span>
                <span className="flex items-center gap-1">
                  <span className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: TAJWEED_COLORS.madd_jaiz.color }} />
                  <span>مد 2 أو 4 أو 6 جوازاً</span>
                </span>
                <span className="flex items-center gap-1">
                  <span className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: TAJWEED_COLORS.ghunnah.color }} />
                  <span>إخفاء ، ومواقع الغُنّة (حركتان)</span>
                </span>
                <span className="flex items-center gap-1">
                  <span className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: TAJWEED_COLORS.idgham.color }} />
                  <span>إدغام ، وما لا يُلفظ</span>
                </span>
                <span className="flex items-center gap-1">
                  <span className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: TAJWEED_COLORS.madd_wajib.color }} />
                  <span>مد واجب 4 أو 5 حركات</span>
                </span>
                <span className="flex items-center gap-1">
                  <span className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: TAJWEED_COLORS.qalqalah.color }} />
                  <span>قلقلة</span>
                </span>
                <span className="flex items-center gap-1">
                  <span className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: TAJWEED_COLORS.tafkheem.color }} />
                  <span>تفخيم</span>
                </span>
              </div>

              {/* Bottom Numbers Row: Juz 1 | Page 4 | Surah Al-Baqarah */}
              <div className="flex items-center justify-between text-xs font-bold pt-1 px-2 text-slate-700">
                <span>جزء {currentPage.juzNumber}</span>
                <span className="text-sm font-extrabold text-[#997a31]">{currentPage.pageNumber}</span>
                <span>{currentPage.surahName}</span>
              </div>
            </div>
          </div>

          {/* =========================================================================
              AUTHENTIC RADIAL ACTION MENU (MATCHING USER SCREENSHOT 1)
             ========================================================================= */}
          {showRadialMenu && (
            <div
              className="absolute z-30 pointer-events-auto"
              style={{
                left: `${Math.min(Math.max(radialMenuPos.x, 100), 500)}px`,
                top: `${Math.min(Math.max(radialMenuPos.y, 100), 750)}px`,
              }}
            >
              <RadialMenuActions
                onClose={() => setShowRadialMenu(false)}
                onListen={() => { setShowRadialMenu(false); setShowListenModal(true); }}
                onBookmark={handleBookmarkCurrentAyah}
                onTafseer={() => { setShowRadialMenu(false); setShowTafseerModal(true); }}
                onShare={handleShareAyah}
                onCopy={handleCopyAyah}
              />
            </div>
          )}
        </div>

        {/* Right Book Page Edge Clicker: Prev Page (الصفحة السابقة - لليمين) */}
        <button
          onClick={() => handleFlipPage('prev')}
          disabled={currentPageNum <= 1}
          className="hidden md:flex flex-col items-center justify-center w-12 h-96 rounded-l-2xl bg-gradient-to-l from-slate-900/60 to-slate-950/90 hover:from-emerald-950/80 hover:to-slate-900 border border-slate-800 text-slate-400 hover:text-emerald-300 transition-all group disabled:opacity-20 cursor-pointer shadow-xl"
          title="الصفحة السابقة (تقليب صفحات الكتاب لليمين)"
        >
          <ChevronRight className="w-6 h-6 group-hover:translate-x-1 transition-transform" />
          <span className="text-[10px] [writing-mode:vertical-rl] font-bold mt-2 tracking-wider">
            ⮞ الصفحة السابقة
          </span>
        </button>
      </div>

      {/* Mobile/Bottom Flip Controls */}
      <div className="flex items-center justify-between max-w-2xl mx-auto px-2 text-xs text-slate-400">
        <button
          onClick={() => handleFlipPage('next')}
          disabled={currentPageNum >= 604}
          className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-slate-900 border border-slate-800 hover:text-emerald-400 transition-colors disabled:opacity-30"
        >
          <ChevronLeft className="w-4 h-4" />
          <span>الصفحة التالية ⮜</span>
        </button>

        <span className="text-[11px] text-slate-500 font-sans">
          اسحب الشاشة أو استخدم الأسهم لتقليب صفحات الكتاب
        </span>

        <button
          onClick={() => handleFlipPage('prev')}
          disabled={currentPageNum <= 1}
          className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-slate-900 border border-slate-800 hover:text-emerald-400 transition-colors disabled:opacity-30"
        >
          <span>⮞ الصفحة السابقة</span>
          <ChevronRight className="w-4 h-4" />
        </button>
      </div>

      {/* =========================================================================
          SURAH SELECTION MODAL (قائمة اختيار السورة لفتحها بكامل الشاشة)
         ========================================================================= */}
      {showSurahListModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-5 bg-black/85 backdrop-blur-md animate-in fade-in">
          <div className="w-full max-w-2xl rounded-3xl bg-slate-900 border border-amber-600/40 p-5 space-y-4 text-right max-h-[90vh] flex flex-col shadow-2xl">
            {/* Modal Header */}
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <div className="flex items-center gap-2">
                <BookOpen className="w-5 h-5 text-amber-400" />
                <h3 className="text-base font-bold text-white">
                  فهرس السور الكريمة (114 سورة)
                </h3>
              </div>
              <button
                onClick={() => setShowSurahListModal(false)}
                className="p-1.5 rounded-xl bg-slate-800 text-slate-400 hover:text-white"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Search and Category Filter */}
            <div className="space-y-2.5">
              <div className="relative">
                <Search className="w-4 h-4 absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
                <input
                  type="text"
                  value={surahSearchQuery}
                  onChange={(e) => setSurahSearchQuery(e.target.value)}
                  placeholder="ابحث باسم السورة، رقمها أو اسمها بالإنجليزية..."
                  className="w-full pl-4 pr-10 py-2.5 rounded-2xl bg-slate-950 border border-slate-800 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-amber-500"
                />
              </div>

              <div className="flex items-center gap-2">
                <button
                  onClick={() => setSurahTabFilter('all')}
                  className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-colors ${
                    surahTabFilter === 'all'
                      ? 'bg-amber-600 text-white'
                      : 'bg-slate-950 text-slate-400 hover:text-white'
                  }`}
                >
                  جميع السور (114)
                </button>
                <button
                  onClick={() => setSurahTabFilter('meccan')}
                  className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-colors ${
                    surahTabFilter === 'meccan'
                      ? 'bg-emerald-600 text-white'
                      : 'bg-slate-950 text-slate-400 hover:text-white'
                  }`}
                >
                  مكية (86)
                </button>
                <button
                  onClick={() => setSurahTabFilter('medinan')}
                  className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-colors ${
                    surahTabFilter === 'medinan'
                      ? 'bg-blue-600 text-white'
                      : 'bg-slate-950 text-slate-400 hover:text-white'
                  }`}
                >
                  مدنية (28)
                </button>
              </div>
            </div>

            {/* Surahs Grid */}
            <div className="flex-1 overflow-y-auto grid grid-cols-1 sm:grid-cols-2 gap-2.5 pr-1">
              {filteredSurahs.map((surah) => (
                <div
                  key={surah.number}
                  onClick={() => handleSelectSurah(surah)}
                  className="p-3 rounded-2xl bg-slate-950 hover:bg-slate-800 border border-slate-800/80 hover:border-amber-500/50 flex items-center justify-between gap-3 cursor-pointer transition-all group active:scale-98"
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
                        {surah.englishName} • {surah.numberOfAyahs} آيات
                      </div>
                    </div>
                  </div>

                  <div className="flex flex-col items-end gap-1">
                    <span
                      className={`text-[10px] px-2 py-0.5 rounded-full font-bold ${
                        surah.revelationType === 'Meccan'
                          ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30'
                          : 'bg-blue-500/10 text-blue-400 border border-blue-500/30'
                      }`}
                    >
                      {surah.revelationTypeAr}
                    </span>
                    <span className="text-[10px] text-amber-400/90 font-sans font-bold">
                      صفحة {surah.page}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* =========================================================================
          "الاستماع إلى؟" MODAL & CUSTOM RANGE / MEMORIZATION (MATCHING SCREENSHOT 2)
         ========================================================================= */}
      {showListenModal && activeAyah && (
        <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/80 backdrop-blur-sm animate-in fade-in duration-200 p-0 sm:p-4">
          <div className="w-full max-w-md rounded-t-3xl sm:rounded-3xl bg-slate-900 border border-slate-800 p-6 space-y-4 text-center max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between pb-2 border-b border-slate-800">
              <h3 className="text-lg font-bold text-white">الاستماع إلى؟</h3>
              <button
                onClick={() => setShowListenModal(false)}
                className="p-1 rounded-lg text-slate-400 hover:text-white"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Standard Quick Options matching Screenshot 2 */}
            <div className="space-y-1.5 text-base font-semibold text-slate-200">
              <button
                onClick={() => {
                  setRangeStartAyah(activeAyah.ayahNumber);
                  setRangeEndAyah(activeAyah.ayahNumber);
                  handleStartCustomRange();
                }}
                className="w-full py-2.5 rounded-xl hover:bg-slate-800 transition-colors"
              >
                آية {activeAyah.ayahNumber}
              </button>

              <button
                onClick={() => {
                  setRangeStartAyah(currentPage.ayahs[0].ayahNumber);
                  setRangeEndAyah(currentPage.ayahs[currentPage.ayahs.length - 1].ayahNumber);
                  handleStartCustomRange();
                }}
                className="w-full py-2.5 rounded-xl hover:bg-slate-800 transition-colors"
              >
                صفحة {currentPage.pageNumber}
              </button>

              <button
                onClick={() => {
                  setRangeStartAyah(1);
                  setRangeEndAyah(surahAyahCount);
                  handleStartCustomRange();
                }}
                className="w-full py-2.5 rounded-xl hover:bg-slate-800 transition-colors"
              >
                {currentPage.surahName}
              </button>

              <button
                onClick={() => {
                  setRangeStartAyah(activeAyah.ayahNumber);
                  setRangeEndAyah(surahAyahCount);
                  handleStartCustomRange();
                }}
                className="w-full py-2.5 rounded-xl hover:bg-slate-800 transition-colors"
              >
                {currentPage.surahName} من الآية {activeAyah.ayahNumber}
              </button>

              <button
                onClick={() => {
                  setRangeStartAyah(1);
                  setRangeEndAyah(141);
                  handleStartCustomRange();
                }}
                className="w-full py-2.5 rounded-xl hover:bg-slate-800 transition-colors"
              >
                الجزء {currentPage.juzNumber}
              </button>
            </div>

            {/* Custom Range & Repeat System (نظام الحفظ وتحديد الآيات كما طلب المستخدم) */}
            <div className="pt-3 border-t border-slate-800 text-right space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-emerald-400 flex items-center gap-1.5">
                  <Sliders className="w-4 h-4" />
                  <span>تخصيص نطاق الآيات والتكرار للحفظ</span>
                </span>
                <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 font-bold">
                  مخصص
                </span>
              </div>

              <div className="grid grid-cols-2 gap-3 text-xs">
                <div className="p-2.5 rounded-xl bg-slate-950 border border-slate-800 space-y-1">
                  <span className="text-slate-400">من آية:</span>
                  <input
                    type="number"
                    min={1}
                    max={286}
                    value={rangeStartAyah}
                    onChange={(e) => setRangeStartAyah(parseInt(e.target.value, 10) || 1)}
                    className="w-full bg-transparent text-center font-bold text-white text-sm focus:outline-none"
                  />
                </div>

                <div className="p-2.5 rounded-xl bg-slate-950 border border-slate-800 space-y-1">
                  <span className="text-slate-400">إلى آية:</span>
                  <input
                    type="number"
                    min={rangeStartAyah}
                    max={286}
                    value={rangeEndAyah}
                    onChange={(e) => setRangeEndAyah(parseInt(e.target.value, 10) || rangeStartAyah)}
                    className="w-full bg-transparent text-center font-bold text-white text-sm focus:outline-none"
                  />
                </div>
              </div>

              {/* Repeat Per Ayah */}
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

              {/* Repeat Entire Selection */}
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

              {/* Reciter Picker inside Modal */}
              <div className="space-y-1.5 pt-1">
                <span className="text-xs text-slate-300">القارئ:</span>
                <select
                  value={selectedReciter.id}
                  onChange={(e) => {
                    const r = AYAH_RECITERS.find((rec) => rec.id === e.target.value);
                    if (r) setSelectedReciter(r);
                  }}
                  className="w-full bg-slate-950 border border-slate-800 text-xs text-slate-200 rounded-xl px-3 py-2.5 focus:outline-none"
                >
                  {AYAH_RECITERS.map((rec) => (
                    <option key={rec.id} value={rec.id} className="bg-slate-900 text-slate-100">
                      🎙️ {rec.name} {rec.country ? `(${rec.country})` : ''}
                    </option>
                  ))}
                </select>
              </div>

              <button
                onClick={handleStartCustomRange}
                className="w-full py-3 rounded-2xl bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white font-bold text-sm shadow-xl flex items-center justify-center gap-2 mt-2"
              >
                <Play className="w-4 h-4 fill-current" />
                <span>بدء تشغيل النطاق المخصص للحفظ (الآيات {rangeStartAyah} - {rangeEndAyah})</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Tafseer & Reflections Modal */}
      {showTafseerModal && activeAyah && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in">
          <div className="w-full max-w-lg rounded-3xl bg-slate-900 border border-slate-800 p-6 space-y-4 text-right max-h-[85vh] overflow-y-auto">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <h3 className="text-base font-bold text-white flex items-center gap-2">
                <BookOpen className="w-5 h-5 text-emerald-400" />
                <span>التفسير الميسر • آية {activeAyah.ayahNumber}</span>
              </h3>
              <button
                onClick={() => setShowTafseerModal(false)}
                className="p-1.5 rounded-xl bg-slate-800 text-slate-400 hover:text-white"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="p-4 rounded-2xl bg-[#fffdf5] text-slate-950 font-quran text-lg leading-relaxed border-2 border-[#dfbe72]">
              ﴿{activeAyah.plainText}﴾
            </div>

            <div className="space-y-2">
              <h4 className="text-xs font-bold text-emerald-400">التفسير وبيان المعنى:</h4>
              {activeAyah.tafseer ? (
                <p className="text-sm text-slate-300 leading-relaxed font-sans bg-slate-950 p-4 rounded-2xl border border-slate-800/80">
                  {activeAyah.tafseer}
                </p>
              ) : (
                <p className="text-sm text-slate-400 leading-relaxed font-sans bg-slate-950 p-4 rounded-2xl border border-slate-800/80 text-center">
                  التفسير الميسر متاح تدريجياً — هذه الصفحة من نص المصحف الكامل المعتمد (رسم عثماني).
                </p>
              )}
            </div>

            <button
              onClick={() => {
                setShowTafseerModal(false);
                setShowListenModal(true);
              }}
              className="w-full py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold flex items-center justify-center gap-2"
            >
              <Play className="w-4 h-4 fill-current" />
              <span>الاستماع لتلاوة هذه الآية</span>
            </button>
          </div>
        </div>
      )}

      {/* Smart Search Modal */}
      {showSearchModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in">
          <div className="w-full max-w-lg rounded-3xl bg-slate-900 border border-emerald-600/40 p-6 space-y-4 text-right max-h-[85vh] flex flex-col">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <h3 className="text-base font-bold text-white flex items-center gap-2">
                <Search className="w-5 h-5 text-emerald-400" />
                <span>البحث بالآيات أو رقم السورة والآية</span>
              </h3>
              <button
                onClick={() => setShowSearchModal(false)}
                className="p-1.5 rounded-xl bg-slate-800 text-slate-400 hover:text-white"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="relative">
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => handleSearchChange(e.target.value)}
                placeholder="اكتب نص الآية أو رقمها مثلاً: 'البقرة 255' أو '2:255'..."
                className="w-full px-4 py-3 rounded-2xl bg-slate-950 border border-slate-800 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                autoFocus
              />
            </div>

            <div className="flex-1 overflow-y-auto space-y-2 pr-1">
              {searchResults.length === 0 ? (
                <div className="text-center py-8 text-xs text-slate-400 space-y-1">
                  <p>اكتب حرفين أو أكثر للبحث السريع في المصحف الشريف</p>
                  <p className="text-slate-500">يدعم البحث بدون تشكيل والبحث السريع بالأرقام</p>
                </div>
              ) : (
                searchResults.map((res) => (
                  <div
                    key={`${res.surahNumber}-${res.ayahNumber}`}
                    onClick={() => handleSelectSearchResult(res)}
                    className="p-3.5 rounded-2xl bg-slate-950 hover:bg-slate-800/80 border border-slate-800/60 cursor-pointer transition-colors"
                  >
                    <div className="flex items-center justify-between text-xs text-emerald-400 font-bold mb-1">
                      <span>{res.surahName} • آية {res.ayahNumber}</span>
                      <span className="text-amber-400 text-[11px] font-sans">صفحة {res.pageNumber}</span>
                    </div>
                    <p className="font-quran text-slate-200 text-base leading-relaxed">
                      {res.text}
                    </p>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      )}

      {/* Bookmarks Modal (نظام الحفظ والإشارات المرجعية) */}
      {showBookmarksModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in">
          <div className="w-full max-w-lg rounded-3xl bg-slate-900 border border-emerald-600/40 p-6 space-y-4 text-right max-h-[85vh] flex flex-col">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <h3 className="text-base font-bold text-white flex items-center gap-2">
                <Bookmark className="w-5 h-5 text-emerald-400" />
                <span>نظام حفظ القرآن والإشارات المرجعية</span>
              </h3>
              <button
                onClick={() => setShowBookmarksModal(false)}
                className="p-1.5 rounded-xl bg-slate-800 text-slate-400 hover:text-white"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto space-y-2.5 pr-1">
              {bookmarks.length === 0 ? (
                <div className="text-center py-8 text-xs text-slate-400">
                  لا توجد آيات محفوظة حالياً. انقر على أي آية في المصحف ثم اختر أيقونة الحفظ.
                </div>
              ) : (
                bookmarks.map((bm) => (
                  <div
                    key={bm.id}
                    className="p-3.5 rounded-2xl bg-slate-950 border border-slate-800 flex items-center justify-between gap-3"
                  >
                    <div
                      className="flex-1 cursor-pointer"
                      onClick={() => {
                        setCurrentPageNum(bm.pageNumber);
                        setShowBookmarksModal(false);
                      }}
                    >
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
                      onClick={() => {
                        removeBookmark(bm.id);
                        setBookmarks(getSavedBookmarks());
                      }}
                      className="p-2 rounded-xl text-slate-500 hover:text-rose-400 transition-colors"
                      title="حذف من المحفوظات"
                    >
                      <X className="w-4 h-4" />
                    </button>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

/* ============================================================================
 * قائمة الآية الدائرية — مكوّن مشترك بين العرض العادي وملء الشاشة
 * (نفس التصميم البرتقالي الأصلي بنفس المواقع)
 * ========================================================================== */
interface RadialMenuActionsProps {
  onClose: () => void;
  onListen: () => void;
  onBookmark: () => void;
  onTafseer: () => void;
  onShare: () => void;
  onCopy: () => void;
}

const RadialMenuActions: React.FC<RadialMenuActionsProps> = ({
  onClose,
  onListen,
  onBookmark,
  onTafseer,
  onShare,
  onCopy,
}) => (
  <div className="relative flex items-center justify-center -translate-x-1/2 -translate-y-1/2">
    {/* زر الإغلاق المركزي */}
    <button
      onClick={onClose}
      className="w-12 h-12 rounded-full bg-[#f97316] hover:bg-[#ea580c] text-white flex items-center justify-center shadow-2xl ring-4 ring-white z-20 active:scale-95 transition-transform"
      title="إغلاق قائمة الآية"
    >
      <X className="w-6 h-6 stroke-[3]" />
    </button>

    {/* التلاوة وتحديد النطاق */}
    <button
      onClick={onListen}
      style={{ transform: 'translate(60px, -45px)' }}
      className="absolute w-12 h-12 rounded-full bg-[#f97316] hover:bg-[#ea580c] text-white flex items-center justify-center shadow-xl ring-2 ring-white active:scale-90 transition-transform"
      title="الاستماع وتحديد نطاق الآيات"
    >
      <Play className="w-5 h-5 fill-current ml-0.5" />
    </button>

    {/* الحفظ في الإشارات المرجعية */}
    <button
      onClick={onBookmark}
      style={{ transform: 'translate(0px, -75px)' }}
      className="absolute w-12 h-12 rounded-full bg-[#f97316] hover:bg-[#ea580c] text-white flex items-center justify-center shadow-xl ring-2 ring-white active:scale-90 transition-transform"
      title="حفظ الآية في الإشارات المرجعية"
    >
      <Bookmark className="w-5 h-5 fill-current" />
    </button>

    {/* التفسير */}
    <button
      onClick={onTafseer}
      style={{ transform: 'translate(-60px, -45px)' }}
      className="absolute w-12 h-12 rounded-full bg-[#f97316] hover:bg-[#ea580c] text-white flex items-center justify-center shadow-xl ring-2 ring-white active:scale-90 transition-transform"
      title="تفسير الآية"
    >
      <BookOpen className="w-5 h-5" />
    </button>

    {/* المشاركة */}
    <button
      onClick={onShare}
      style={{ transform: 'translate(75px, 20px)' }}
      className="absolute w-12 h-12 rounded-full bg-[#f97316] hover:bg-[#ea580c] text-white flex items-center justify-center shadow-xl ring-2 ring-white active:scale-90 transition-transform"
      title="مشاركة الآية"
    >
      <Share2 className="w-5 h-5" />
    </button>

    {/* النسخ */}
    <button
      onClick={onCopy}
      style={{ transform: 'translate(45px, 70px)' }}
      className="absolute w-12 h-12 rounded-full bg-[#f97316] hover:bg-[#ea580c] text-white flex items-center justify-center shadow-xl ring-2 ring-white active:scale-90 transition-transform"
      title="نسخ الآية"
    >
      <Copy className="w-5 h-5" />
    </button>

    {/* التدبر والمعلومات */}
    <button
      onClick={onTafseer}
      style={{ transform: 'translate(-25px, 70px)' }}
      className="absolute w-12 h-12 rounded-full bg-[#f97316] hover:bg-[#ea580c] text-white flex items-center justify-center shadow-xl ring-2 ring-white active:scale-90 transition-transform"
      title="معلومات وتدبر"
    >
      <Info className="w-5 h-5" />
    </button>

    {/* نطاق التلاوة والحفظ */}
    <button
      onClick={onListen}
      style={{ transform: 'translate(-75px, 0px)' }}
      className="absolute w-12 h-12 rounded-full bg-[#f97316] hover:bg-[#ea580c] text-white flex items-center justify-center shadow-xl ring-2 ring-white active:scale-90 transition-transform"
      title="تحديد نطاق التلاوة والحفظ"
    >
      <Sliders className="w-5 h-5" />
    </button>
  </div>
);
