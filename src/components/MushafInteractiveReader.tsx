import React, { useState, useRef, useEffect, useCallback } from 'react';
import {
  Play,
  Pause,
  Bookmark,
  Share2,
  Copy,
  BookOpen,
  X,
  ChevronRight,
  ChevronLeft,
  Search,
  Sliders,
  Check,
  Eye,
  EyeOff,
  Maximize2,
  Minimize2,
} from 'lucide-react';
import {
  TajweedAyah,
  MushafPage,
  TAJWEED_COLORS,
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
import { MushafEditionStyle, getSavedEdition, getSavedAyahReciter, saveAyahReciter } from '../utils/appSettings';

/**
 * المصحف الشريف التفاعلي — مصحف التجويد الملون (604 صفحة).
 *
 * إعادة تصميم بطلب المالك (2026-10):
 * - المصحف فقط هو اللي يظهر — لا شريط أدوات ولا أي عنصر فوق الصفحة.
 * - تفاعلي بالضغطة الطويلة (وبالنقر أيضاً) مع قائمة آية منسقة زجاجية.
 * - الطبعة (تجويد/عثماني/ليلي) تتغير من الإعدادات وتُطبق فوراً.
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

/** مدة الضغطة الطويلة قبل ظهور قائمة الآية */
const LONG_PRESS_MS = 480;

export const MushafInteractiveReader: React.FC = () => {
  // ===== حالة التنقل بين الصفحات =====
  const [currentPageNum, setCurrentPageNum] = useState<number>(4);
  const [activeAyah, setActiveAyah] = useState<TajweedAyah | null>(null);
  const [showAyahMenu, setShowAyahMenu] = useState<boolean>(false);
  const [menuPos, setMenuPos] = useState<{ x: number; y: number }>({ x: 0, y: 0 });

  // ===== ملء الشاشة وتقليب الكتاب =====
  const [isFullscreen, setIsFullscreen] = useState<boolean>(false);
  const [showSurahListModal, setShowSurahListModal] = useState<boolean>(false);
  const [surahSearchQuery, setSurahSearchQuery] = useState<string>('');
  const [surahTabFilter, setSurahTabFilter] = useState<'all' | 'meccan' | 'medinan'>('all');
  const [flipDirection, setFlipDirection] = useState<'next' | 'prev' | null>(null);

  // ===== النوافذ =====
  const [showListenModal, setShowListenModal] = useState<boolean>(false);
  const [showTafseerModal, setShowTafseerModal] = useState<boolean>(false);
  const [showSearchModal, setShowSearchModal] = useState<boolean>(false);
  const [showBookmarksModal, setShowBookmarksModal] = useState<boolean>(false);

  // ===== التلاوة بالنطاقات والتكرار (الحفظ) =====
  const [selectedReciter, setSelectedReciter] = useState<AyahReciter>(getSavedAyahReciter);
  const [rangeStartAyah, setRangeStartAyah] = useState<number>(1);
  const [rangeEndAyah, setRangeEndAyah] = useState<number>(1);
  const [repeatPerAyah, setRepeatPerAyah] = useState<number>(1);
  const [repeatEntireRange, setRepeatEntireRange] = useState<number>(1);
  const [currentPlayingAyahNum, setCurrentPlayingAyahNum] = useState<number | null>(null);
  const [isPlayingRange, setIsPlayingRange] = useState<boolean>(false);
  const [currentAyahRepeatCount, setCurrentAyahRepeatCount] = useState<number>(0);
  const [currentRangeCycleCount, setCurrentRangeCycleCount] = useState<number>(0);
  const [copiedToast, setCopiedToast] = useState<string | null>(null);

  // ===== وضع التسميع (إخفاء الكلمات) =====
  const [memorizationTestMode, setMemorizationTestMode] = useState<boolean>(false);
  const [revealedAyahs, setRevealedAyahs] = useState<Record<number, boolean>>({});

  // ===== البحث والمحفوظات =====
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [searchResults, setSearchResults] = useState<SearchResult[]>([]);
  const [bookmarks, setBookmarks] = useState<BookmarkItem[]>(getSavedBookmarks());

  // ===== طبعة المصحف من الإعدادات =====
  const [edition, setEdition] = useState<MushafEditionStyle>(getSavedEdition);

  // ===== مراجع =====
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const pageContainerRef = useRef<HTMLDivElement | null>(null);
  const touchStartX = useRef<number | null>(null);
  const touchStartY = useRef<number | null>(null);
  const longPressTimer = useRef<number | null>(null);
  const longPressFired = useRef<boolean>(false);
  const [pressingAyahNum, setPressingAyahNum] = useState<number | null>(null);

  // تحميل نص المصحف الكامل (604 صفحة)
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

  // مزامنة الطبعة لو اتغيرت من الإعدادات والرجوع لتبويب المصحف
  useEffect(() => {
    setEdition(getSavedEdition());
  }, []);

  const currentPage: MushafPage = getOrCreateMushafPage(currentPageNum);

  // عدد آيات السورة المعروضة
  const displayedSurahNumber = activeAyah?.surahNumber ?? currentPage.ayahs[0]?.surahNumber ?? 1;
  const surahAyahCount = SURAHS_LIST.find((s) => s.number === displayedSurahNumber)?.numberOfAyahs ?? 286;

  // ===== تقليب الصفحات (عربي RTL: التالية لليسار) =====
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
        setShowAyahMenu(false);
      }, 140);

      setTimeout(() => {
        setFlipDirection(null);
      }, 340);
    },
    []
  );

  // اختيار سورة → فتح صفحتها بملء الشاشة
  const handleSelectSurah = (surah: Surah) => {
    setCurrentPageNum(surah.page);
    setIsFullscreen(true);
    setShowSurahListModal(false);
    playPaperTurnSound();
    setCopiedToast(`تم فتح ${surah.name} بملء الشاشة — صفحة ${surah.page}`);
    setTimeout(() => setCopiedToast(null), 3500);
  };

  // لوحة المفاتيح: أسهم للتقليب + F لملء الشاشة + Esc خروج
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
      if (deltaX < 0) {
        handleFlipPage('next');
      } else {
        handleFlipPage('prev');
      }
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
      // قائمة بعرض ~260 وارتفاع ~330 — نثبتها جوّه الصفحة كلها
      const menuW = 260;
      const menuH = 340;
      const x = Math.min(Math.max(clickX - menuW / 2, 10), Math.max(rect.width - menuW - 10, 10));
      const y = Math.min(Math.max(clickY - 60, 10), Math.max(rect.height - menuH - 10, 10));
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
    // حماية: بعد ضغطة طويلة فتحت القائمة بالفعل — نتجاهل النقرة الاصطناعية التالية
    if (longPressFired.current) {
      longPressFired.current = false;
      return;
    }
    openAyahMenu(ayah, event.clientX, event.clientY);
  };

  // ===== التلاوة: آية واحدة أو نطاق مخصص =====
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
      // fallback صمت
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
    setShowAyahMenu(false);
    setCurrentAyahRepeatCount(0);
    setCurrentRangeCycleCount(0);
    playAyahAudio(currentPage.ayahs[0].surahNumber, rangeStartAyah);
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
      `﴿${activeAyah.plainText}﴾ [${currentPage.surahName}: ${activeAyah.ayahNumber}]`
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

  // ===== تدفق الآيات بالتلوين التجويدي حسب الطبعة =====
  const renderAyahFlow = (textClass: string, numberSize: string) => (
    <div
      className={`text-justify font-quran font-normal dir-rtl px-1 ${textClass}`}
      style={{ color: edition.ink }}
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
            onTouchStart={(e) => startLongPress(ayah, e.touches[0].clientX, e.touches[0].clientY)}
            onTouchEnd={(e) => {
              cancelLongPress();
              // لو الضغطة الطويلة مافضلتش خلال المدة — اعتبرها نقرة عادية على موضع اللمس
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
                : 'hover:bg-amber-100/50'
            }`}
            title={`اضغط مطولاً أو انقر للتفاعل مع الآية ${ayah.ayahNumber}`}
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
                  seg.rule && edition.showTajweed ? TAJWEED_COLORS[seg.rule] : null;
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

            {/* رقم الآية بالزخرفة العثمانية */}
            <span
              className={`inline-flex items-center justify-center font-sans font-bold border rounded-full mx-1 shadow-inner align-middle ${numberSize}`}
              style={{
                color: edition.ayahNumberColor,
                borderColor: edition.frame,
                backgroundColor: edition.id === 'night' ? 'rgba(255,255,255,0.06)' : '#fbf6e3',
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
  // شريط التحكم المدمج (أسفل الصفحة — ولا شيء فوق المصحف)
  // =====================================================================
  const renderCompactControls = () => (
    <div
      className="flex flex-wrap items-center justify-center gap-1.5 p-2 rounded-3xl border shadow-2xl backdrop-blur-xl"
      style={{
        backgroundColor: 'color-mix(in srgb, var(--noor-n-900) 88%, transparent)',
        borderColor: 'color-mix(in srgb, var(--noor-p-500) 28%, transparent)',
      }}
    >
      <button
        onClick={() => setShowSurahListModal(true)}
        className="flex items-center gap-1.5 px-3 py-2 rounded-2xl text-xs font-bold transition-colors hover:bg-white/10"
        style={{ color: 'var(--noor-a-400)' }}
        title="فهرس السور الـ114"
      >
        <BookOpen className="w-4 h-4" />
        <span className="hidden sm:inline">السور</span>
      </button>

      <div
        className="flex items-center gap-1 px-2 py-1 rounded-2xl border"
        style={{ borderColor: 'color-mix(in srgb, var(--noor-p-500) 25%, transparent)' }}
      >
        <button
          onClick={() => handleFlipPage('prev')}
          disabled={currentPageNum <= 1}
          className="p-1.5 rounded-xl disabled:opacity-30 transition-colors hover:bg-white/10"
          style={{ color: 'var(--noor-n-200)' }}
          title="الصفحة السابقة"
        >
          <ChevronRight className="w-4 h-4" />
        </button>
        <div className="flex items-center gap-1 text-xs px-1">
          <span style={{ color: 'var(--noor-n-400)' }}>صفحة</span>
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
          <span style={{ color: 'var(--noor-n-500)' }}>/ 604</span>
        </div>
        <button
          onClick={() => handleFlipPage('next')}
          disabled={currentPageNum >= 604}
          className="p-1.5 rounded-xl disabled:opacity-30 transition-colors hover:bg-white/10"
          style={{ color: 'var(--noor-n-200)' }}
          title="الصفحة التالية"
        >
          <ChevronLeft className="w-4 h-4" />
        </button>
      </div>

      <button
        onClick={() => setShowSearchModal(true)}
        className="p-2 rounded-2xl transition-colors hover:bg-white/10"
        style={{ color: 'var(--noor-n-200)' }}
        title="البحث في المصحف"
      >
        <Search className="w-4 h-4" />
      </button>

      <button
        onClick={() => setShowBookmarksModal(true)}
        className="relative p-2 rounded-2xl transition-colors hover:bg-white/10"
        style={{ color: 'var(--noor-n-200)' }}
        title="المحفوظات والإشارات المرجعية"
      >
        <Bookmark className="w-4 h-4" />
        {bookmarks.length > 0 && (
          <span
            className="absolute -top-1 -left-1 min-w-[16px] h-4 px-1 rounded-full text-[9px] font-bold flex items-center justify-center text-white"
            style={{ backgroundColor: 'var(--noor-p-500)' }}
          >
            {bookmarks.length}
          </span>
        )}
      </button>

      <button
        onClick={() => setMemorizationTestMode(!memorizationTestMode)}
        className={`p-2 rounded-2xl transition-colors ${
          memorizationTestMode ? 'bg-amber-500/25' : 'hover:bg-white/10'
        }`}
        style={{
          color: memorizationTestMode ? 'var(--noor-a-400)' : 'var(--noor-n-200)',
        }}
        title="وضع التسميع واختبار الحفظ"
      >
        {memorizationTestMode ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
      </button>

      {isPlayingRange && (
        <button
          onClick={stopAudio}
          className="flex items-center gap-1.5 px-3 py-2 rounded-2xl text-xs font-bold text-white animate-pulse"
          style={{ backgroundColor: '#e11d48' }}
          title="إيقاف التلاوة"
        >
          <Pause className="w-4 h-4" />
          <span className="hidden sm:inline">إيقاف (آية {currentPlayingAyahNum})</span>
        </button>
      )}

      <button
        onClick={() => setIsFullscreen(true)}
        className="p-2 rounded-2xl transition-colors hover:bg-white/10"
        style={{ color: 'var(--noor-n-200)' }}
        title="المصحف بملء الشاشة"
      >
        <Maximize2 className="w-4 h-4" />
      </button>
    </div>
  );

  // =====================================================================
  // صفحة المصحف نفسها — بلا أي شيء فوقها
  // =====================================================================
  const renderMushafPage = (compact: boolean) => (
    <div
      ref={pageContainerRef}
      onTouchStart={handleTouchStart}
      onTouchEnd={handleTouchEnd}
      className={`relative mx-auto w-full ${compact ? 'max-w-3xl' : 'max-w-4xl'} shadow-2xl overflow-hidden select-none book-spine-shadow ${
        flipDirection === 'next'
          ? 'book-flip-forward'
          : flipDirection === 'prev'
          ? 'book-flip-backward'
          : ''
      } ${compact ? 'rounded-2xl border-[10px] sm:border-[12px]' : 'rounded-xl sm:rounded-2xl border-[6px] sm:border-[10px]'}`}
      style={{
        backgroundColor: edition.paper,
        borderColor: edition.frame,
        boxShadow: '0 18px 50px rgba(0,0,0,0.45)',
      }}
    >
      <div
        className={`${compact ? 'p-3 sm:p-5 m-1' : 'p-2 sm:p-4 m-1'} border-4 rounded-lg relative flex flex-col`}
        style={{ borderColor: edition.innerFrame }}
      >
        {/* ترويسة الصفحة داخل إطار المصحف (زي المصحف الحقيقي) */}
        <div
          className="flex items-center justify-between text-xs font-bold pb-2 mb-3 border-b-2"
          style={{ borderColor: edition.frame, color: edition.ink }}
        >
          <div
            className="px-3 py-1 rounded border text-[11px] font-sans"
            style={{
              borderColor: edition.frame,
              backgroundColor: edition.id === 'night' ? 'rgba(255,255,255,0.05)' : `${edition.frame}22`,
            }}
          >
            {currentPage.hizbQuarter} • الجزء {currentPage.juzNumber}
          </div>

          <button
            onClick={() => setShowSurahListModal(true)}
            className="px-4 py-1 rounded border text-sm font-bold font-arabic transition-all hover:brightness-110"
            style={{
              borderColor: edition.innerFrame,
              color: edition.ink,
              backgroundColor: edition.id === 'night' ? 'rgba(255,255,255,0.05)' : `${edition.innerFrame}18`,
            }}
            title="انقر لتغيير السورة"
          >
            {currentPage.surahName} ▾
          </button>
        </div>

        {/* تدفق الآيات — التلوين حسب الطبعة */}
        <div
          className={
            compact
              ? 'text-justify leading-[3.1] font-quran text-xl sm:text-2xl dir-rtl px-1'
              : undefined
          }
        >
          {compact ? (
            renderAyahFlow('leading-[3.1] text-xl sm:text-2xl', 'w-6 h-6 sm:w-7 sm:h-7 text-xs sm:text-sm')
          ) : (
            renderAyahFlow('leading-[2.5] text-lg sm:text-2xl', 'w-6 h-6 sm:w-7 sm:h-7 text-xs sm:text-sm')
          )}
        </div>

        {/* تذييل الصفحة: الجزء | رقم الصفحة | السورة */}
        <div
          className="mt-4 pt-3 border-t-2 flex items-center justify-between text-xs font-bold px-2"
          style={{ borderColor: edition.frame, color: edition.ink }}
        >
          <span>جزء {currentPage.juzNumber}</span>
          <span className="text-sm font-extrabold" style={{ color: edition.ayahNumberColor }}>
            {currentPage.pageNumber}
          </span>
          <span>{currentPage.surahName}</span>
        </div>
      </div>

      {/* قائمة الآية الزجاجية المنسقة — تعمل بالنقر والضغطة الطويلة */}
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
  // وضع ملء الشاشة الغامر
  // =====================================================================
  if (isFullscreen) {
    return (
      <div className="fixed inset-0 z-[90] flex flex-col bg-[#05070d]" dir="rtl">
        {copiedToast && (
          <div className="fixed top-4 left-1/2 -translate-x-1/2 z-[95] px-5 py-3 bg-emerald-600 text-white rounded-2xl shadow-2xl flex items-center gap-3 text-xs sm:text-sm">
            <Check className="w-4 h-4 text-emerald-200" />
            <span>{copiedToast}</span>
            <button onClick={() => setCopiedToast(null)} className="text-xs text-emerald-200 hover:text-white px-2 py-0.5">
              إغلاق
            </button>
          </div>
        )}

        {/* شريط علوي رفيع جداً */}
        <div
          className="flex items-center justify-between gap-2 px-3 sm:px-5 py-1.5 border-b backdrop-blur-md shrink-0"
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
            <span
              className="text-[10px] sm:text-xs px-2 py-0.5 rounded-full border shrink-0"
              style={{
                color: 'var(--noor-n-400)',
                borderColor: 'color-mix(in srgb, var(--noor-p-500) 30%, transparent)',
              }}
            >
              الجزء {currentPage.juzNumber} • {currentPage.hizbQuarter}
            </span>
            {!pagesReady && (
              <span className="text-[10px] animate-pulse" style={{ color: 'var(--noor-a-400)' }}>
                جاري تحميل نص المصحف…
              </span>
            )}
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

        {/* الصفحة تغطي المتاح بالكامل */}
        <div className="flex-1 flex items-stretch justify-center p-1.5 sm:p-3 min-h-0 overflow-y-auto">
          {renderMushafPage(false)}
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

          <div
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-2xl border text-sm"
            style={{ borderColor: 'color-mix(in srgb, var(--noor-p-500) 30%, transparent)' }}
          >
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

        {renderAllModals()}
      </div>
    );
  }

  // =====================================================================
  // العرض العادي: المصحف أولاً وبعده شريط التحكم المدمج
  // =====================================================================
  return (
    <div className="max-w-3xl mx-auto space-y-3">
      {/* Toast */}
      {copiedToast && (
        <div className="fixed top-4 left-1/2 -translate-x-1/2 z-50 px-5 py-3 bg-emerald-600 text-white rounded-2xl shadow-2xl flex items-center gap-3 text-xs sm:text-sm border border-emerald-400/40">
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

      {/* صفحة المصحف — لا شيء فوقها إطلاقاً */}
      {renderMushafPage(true)}

      {/* شريط التحكم المدمج أسفل الصفحة */}
      {renderCompactControls()}

      {/* تلميح التفاعل */}
      <p className="text-center text-[11px] pb-1" style={{ color: 'var(--noor-n-400)' }}>
        اضغط مطولاً أو انقر على أي آية للتفاعل • اسحب يميناً/يساراً لتقليب الصفحات • طبعة «{edition.label}»
      </p>

      {renderAllModals()}
    </div>
  );

  // =====================================================================
  // النوافذ المشتركة بين العرضين
  // =====================================================================
  function renderAllModals() {
    return (
      <>
        {/* فهرس السور */}
        {showSurahListModal && (
          <div className="fixed inset-0 z-[60] flex items-center justify-center p-3 sm:p-5 bg-black/85 backdrop-blur-md animate-in fade-in">
            <div className="w-full max-w-2xl rounded-3xl bg-slate-900 border border-amber-600/40 p-5 space-y-4 text-right max-h-[90vh] flex flex-col shadow-2xl">
              <div className="flex items-center justify-between pb-3 border-b border-slate-800">
                <div className="flex items-center gap-2">
                  <BookOpen className="w-5 h-5 text-amber-400" />
                  <h3 className="text-base font-bold text-white">فهرس السور الكريمة (114 سورة)</h3>
                </div>
                <button
                  onClick={() => setShowSurahListModal(false)}
                  className="p-1.5 rounded-xl bg-slate-800 text-slate-400 hover:text-white"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>

              <div className="space-y-2.5">
                <div className="relative">
                  <Search className="w-4 h-4 absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
                  <input
                    type="text"
                    value={surahSearchQuery}
                    onChange={(e) => setSurahSearchQuery(e.target.value)}
                    placeholder="ابحث باسم السورة أو رقمها..."
                    className="w-full pl-4 pr-10 py-2.5 rounded-2xl bg-slate-950 border border-slate-800 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-amber-500"
                  />
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={() => setSurahTabFilter('all')}
                    className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-colors ${
                      surahTabFilter === 'all' ? 'bg-amber-600 text-white' : 'bg-slate-950 text-slate-400 hover:text-white'
                    }`}
                  >
                    جميع السور (114)
                  </button>
                  <button
                    onClick={() => setSurahTabFilter('meccan')}
                    className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-colors ${
                      surahTabFilter === 'meccan' ? 'bg-emerald-600 text-white' : 'bg-slate-950 text-slate-400 hover:text-white'
                    }`}
                  >
                    مكية (86)
                  </button>
                  <button
                    onClick={() => setSurahTabFilter('medinan')}
                    className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-colors ${
                      surahTabFilter === 'medinan' ? 'bg-blue-600 text-white' : 'bg-slate-950 text-slate-400 hover:text-white'
                    }`}
                  >
                    مدنية (28)
                  </button>
                </div>
              </div>

              <div className="flex-1 overflow-y-auto grid grid-cols-1 sm:grid-cols-2 gap-2.5 pr-1">
                {filteredSurahs.map((surah) => (
                  <div
                    key={surah.number}
                    onClick={() => handleSelectSurah(surah)}
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
            </div>
          </div>
        )}

        {/* الاستماع إلى؟ */}
        {showListenModal && activeAyah && (
          <div className="fixed inset-0 z-[60] flex items-end sm:items-center justify-center bg-black/80 backdrop-blur-sm animate-in fade-in duration-200 p-0 sm:p-4">
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
                  {currentPage.surahName} كاملة
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
              </div>

              {/* نطاق مخصص وتكرار الحفظ */}
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
                      max={surahAyahCount}
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
                      max={surahAyahCount}
                      value={rangeEndAyah}
                      onChange={(e) => setRangeEndAyah(parseInt(e.target.value, 10) || rangeStartAyah)}
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
                          repeatPerAyah === num ? 'bg-emerald-600 text-white shadow-sm' : 'bg-slate-950 text-slate-400 hover:text-white'
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
                          repeatEntireRange === num ? 'bg-teal-600 text-white shadow-sm' : 'bg-slate-950 text-slate-400 hover:text-white'
                        }`}
                      >
                        {num === 1 ? 'مرة' : `${num} مرات`}
                      </button>
                    ))}
                  </div>
                </div>

                {/* القارئ */}
                <div className="space-y-1.5 pt-1">
                  <span className="text-xs text-slate-300">القارئ:</span>
                  <select
                    value={selectedReciter.id}
                    onChange={(e) => {
                      const r = AYAH_RECITERS.find((rec) => rec.id === e.target.value);
                      if (r) {
                        setSelectedReciter(r);
                        saveAyahReciter(r.id);
                      }
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
                  onClick={handleStartCustomRange}
                  className="w-full py-3 rounded-2xl bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white font-bold text-sm shadow-xl flex items-center justify-center gap-2 mt-2"
                >
                  <Play className="w-4 h-4 fill-current" />
                  <span>
                    بدء التلاوة (الآيات {rangeStartAyah} — {rangeEndAyah})
                  </span>
                </button>
              </div>
            </div>
          </div>
        )}

        {/* التفسير */}
        {showTafseerModal && activeAyah && (
          <div className="fixed inset-0 z-[60] flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in">
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

              <div
                className="p-4 rounded-2xl font-quran text-lg leading-relaxed border-2"
                style={{ backgroundColor: edition.paper, color: edition.ink, borderColor: edition.frame }}
              >
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

        {/* البحث الذكي */}
        {showSearchModal && (
          <div className="fixed inset-0 z-[60] flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in">
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
                  placeholder="اكتب نص الآية أو رقمها مثلاً: «البقرة 255» أو «2:255»..."
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
                        <span>
                          {res.surahName} • آية {res.ayahNumber}
                        </span>
                        <span className="text-amber-400 text-[11px] font-sans">صفحة {res.pageNumber}</span>
                      </div>
                      <p className="font-quran text-slate-200 text-base leading-relaxed">{res.text}</p>
                    </div>
                  ))
                )}
              </div>
            </div>
          </div>
        )}

        {/* المحفوظات */}
        {showBookmarksModal && (
          <div className="fixed inset-0 z-[60] flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in">
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
                    لا توجد آيات محفوظة حالياً. اضغط مطولاً على أي آية في المصحف ثم اختر الحفظ.
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
                        <p className="font-quran text-sm text-slate-300 line-clamp-1">{bm.textSnippet}</p>
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
      </>
    );
  }
};

/* ============================================================================
 * قائمة الآية الزجاجية المنسقة — بديل القائمة الدائرية القديمة غير المنسقة.
 * تظهر عند النقر أو الضغطة الطويلة، مع خلفية تُغلق باللمس، وأيقونات ملونة وأسماء واضحة.
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
    {/* خلفية شفافة تُغلق القائمة بالنقر عليها */}
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
      {/* ترويسة القائمة */}
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

      {/* الإجراءات */}
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
