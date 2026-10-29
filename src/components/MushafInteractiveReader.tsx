import React, { useState, useRef, useEffect, useCallback, useMemo } from 'react';
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
  Layers,
  CircleDot,
  RotateCcw,
  Trash2,
  CheckCheck,
  Sparkles,
  MapPin,
  Minus,
  Plus,
} from 'lucide-react';
import {
  TajweedAyah,
  MushafPage,
  TajweedRule,
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
import {
  getLastReadPage,
  saveLastReadPage,
  getKhatma,
  getKhatmaProgress,
  isPageReadInKhatma,
  markKhatmaPageRead,
  resetKhatma,
  getJuzForPage,
  getJuzRange,
  getJuzProgress,
  toggleAyahMemorized,
  isAyahMemorized,
  getMemorizationProgress,
  getReviewQueue,
} from '../utils/mushafProgress';

/**
 * المصحف الشريف التفاعلي — مصحف التجويد الملون (604 صفحة).
 *
 * الدفعة الرابعة (بطلب المالك 2026-10-29):
 * - القائمة تظهر بالضغطة الطويلة فقط — اللمسة العادية لا تفتح شيئاً
 * - تحديد متعدد الآيات: ضغطة طويلة تبدأ التحديد، واللمس يضيف/يزيل آيات،
 *   وشريط إجراءات عائم (تلاوة المحدد، تفسير المحدد، حفظ، نسخ، مشاركة)
 * - ورقة المصحف بملء الشاشة فوق شريط التنقل مباشرة (التنقل مرئي دائماً)
 * - شريط سفلي نحيف: متابعة القراءة (آخر صفحة) + الأجزاء + الختمات + تعليم الصفحة
 * - نظام حفظ مطور: تعليم الآيات المحفوظة + لوحة تقدم لكل سورة + طابور مراجعة
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

/** مدة الضغطة الطويلة قبل بدء التحديد — أطول بطلب المالك */
const LONG_PRESS_MS = 750;

/** مفتاح مصحف نصي لربط الآيات بصوتها */
const ayahKey = (surah: number, ayah: number) => `${surah}:${ayah}`;

export const MushafInteractiveReader: React.FC = () => {
  // ===== حالة التنقل بين الصفحات =====
  const [currentPageNum, setCurrentPageNum] = useState<number>(getLastReadPage);
  const [activeAyah, setActiveAyah] = useState<TajweedAyah | null>(null);
  const [flipDirection, setFlipDirection] = useState<'next' | 'prev' | null>(null);

  // ===== نظام التحديد المتصل (ضغطة طويلة تبدأ + لمسة تمتد نطاقاً متصلاً) =====
  /** أرقام الآيات المحددة — دائماً نطاق متصل من anchor إلى end بلا فجوات */
  const [selectedAyahs, setSelectedAyahs] = useState<Set<number>>(new Set());
  const [selectionMode, setSelectionMode] = useState<boolean>(false);
  /** آية بداية التحديد (الضغطة الطويلة) — تظل ثابتة أثناء التمديد */
  const selectionAnchorRef = useRef<number | null>(null);

  // ===== مكتبة المصحف: السور / البحث / الإشارات / الأدوات =====
  const [showLibrary, setShowLibrary] = useState<boolean>(false);
  const [libraryTab, setLibraryTab] = useState<'surahs' | 'juz' | 'khatma' | 'search' | 'bookmarks' | 'tools'>('surahs');
  const [surahSearchQuery, setSurahSearchQuery] = useState<string>('');
  const [surahTabFilter, setSurahTabFilter] = useState<'all' | 'meccan' | 'medinan'>('all');

  // ===== النوافذ =====
  const [showListenModal, setShowListenModal] = useState<boolean>(false);
  const [showTafseerModal, setShowTafseerModal] = useState<boolean>(false);
  /** ورقة خيارات تشغيل الصوت من شريط التحديد (بطلب المالك) */
  const [showPlayOptions, setShowPlayOptions] = useState<boolean>(false);
  const [tafseerAyahs, setTafseerAyahs] = useState<TajweedAyah[]>([]);
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

  // ===== حجم الخط + أبعاد الصفحة التكيفية — الصفحة تملأ المساحة كاملة بلا اسكرول =====
  const [adaptive, setAdaptive] = useState<{ fontSize: number; pageW: number; pageH: number }>({
    fontSize: 24,
    pageW: 300,
    pageH: 455,
  });
  /** حجم الخط بعد الملاءمة التلقائية ليملأ الصفحة بلا اسكرول */
  const [fitFontSize, setFitFontSize] = useState<number>(24);
  const flowRef = useRef<HTMLDivElement | null>(null);
  const flowBoxRef = useRef<HTMLDivElement | null>(null);
  /** عداد محاولات التحقق من الملاءمة — يتصفر مع كل صفحة جديدة */
  const fitVerifyRef = useRef<number>(0);
  /** مستنسخ القياس المخفي خارج تحكم React — لملاءمة بلا سباقات */
  const measureRef = useRef<HTMLDivElement | null>(null);
  /** آخر حجم خط مطبّق — لكسر تذبذب إعادة الملاءمة */
  const fitFontSizeRef = useRef<number>(24);

  // ===== حالة الختمة (محدثة عند التعليم) =====
  const [khatmaTick, setKhatmaTick] = useState(0);
  const khatma = useMemo(() => getKhatma(), [khatmaTick]);
  const pageReadInKhatma = useMemo(
    () => isPageReadInKhatma(currentPageNum),
    [currentPageNum, khatmaTick],
  );

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
        nextAudioKeyRef.current = key;
        const url = getAyahAudioUrl(selectedReciter.folder, surah, ayah);
        const { src, revoke } = await resolveAyahAudioSrc(url);
        if (nextAudioKeyRef.current !== key) {
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
        if (nextAyah !== null && nextAyah > 0) void playAyah(surah, nextAyah);
        else if (repeatRange) void playAyah(surah, state.start);
        else stopAudio();
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

        const nextAyah = ayah < q.end ? ayah + 1 : null;
        if (nextAyah !== null) {
          window.setTimeout(() => {
            void prefetchAyah(surah, nextAyah);
          }, 400);
        }

        el.onended = () => {
          const state = queueRef.current;
          if (!state || !state.playing) return;
          if (state.ayahRepeat + 1 < state.repeatPer) {
            state.ayahRepeat += 1;
            el.currentTime = 0;
            void el.play().catch(() => {});
            return;
          }
          state.ayahRepeat = 0;
          if (ayah < state.end) {
            advance(ayah + 1, false);
            return;
          }
          if (state.cycle + 1 < state.repeatAll) {
            state.cycle += 1;
            advance(null, true);
            return;
          }
          stopAudio();
        };

        el.onerror = () => {
          const state = queueRef.current;
          if (state && state.playing && ayah < state.end) advance(ayah + 1, false);
          else stopAudio();
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

  useEffect(() => stopAudio, [stopAudio]);

  // تنظيف مستنسخ القياس عند إلغاء تركيب المكوّن
  useEffect(() => {
    return () => {
      if (measureRef.current) {
        measureRef.current.remove();
        measureRef.current = null;
      }
    };
  }, []);

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
    selectedAyahs.size > 0
      ? currentPage.ayahs.find((a) => selectedAyahs.has(a.ayahNumber))?.surahNumber ??
        currentPage.ayahs[0]?.surahNumber ??
        1
      : currentPage.ayahs[0]?.surahNumber ?? 1;
  const surahAyahCount =
    SURAHS_LIST.find((s) => s.number === displayedSurahNumber)?.numberOfAyahs ?? 286;

  // ===== القياس التكيفي: ورقة المصحف تملأ كل المساحة المتاحة (بلا نسبة كتاب) =====
  useEffect(() => {
    const compute = () => {
      const el = screenRef.current;
      const w = el?.clientWidth ?? window.innerWidth;
      const h = el?.clientHeight ?? window.innerHeight;
      // شريط سفلي نحيف ~46 + هوامش ~14
      const availH = Math.max(280, h - 60);
      const availW = Math.max(200, w - 12);
      // الصفحة تملأ المساحة كاملة — لا فراغ فوق وتحت (بطلب المالك)
      const fontSize = Math.max(14, Math.min(42, Math.round(availH * 0.0265)));
      setAdaptive({ fontSize, pageW: Math.round(availW), pageH: Math.round(availH) });
    };
    compute();
    window.addEventListener('resize', compute);
    return () => window.removeEventListener('resize', compute);
  }, []);

  /**
   * ملاءمة الخط تلقائية — بملء الصفحة كاملة بلا اسكرول ولا فراغ (بطلب المالك).
   *
   * المنهج المحصّن ضد سباقات React: القياس يجري على «مستنسخ قياس» مخفي
   * خارج تحكم React (React هو الكاتب الوحيد لحجم خط الصفحة الحقيقية)،
   * ببحث ثنائي عن أكبر حجم لا يفيض — ثم يُضبط على الحالة مرة واحدة.
   */
  const computeFit = useCallback(() => {
    const el = flowRef.current;
    const box = flowBoxRef.current;
    if (!el || !box || !pagesReady) return;
    const boxH = box.clientHeight;
    const boxW = box.clientWidth;
    // عرض/ارتفاع عابر أثناء تدوير الشاشة أو تغيير حجم النافذة — القياس عليه بلا معنى
    if (boxH < 60 || boxW < 150 || el.children.length === 0) return;

    const pageKey = `${currentPageNum}|${edition.id}`;
    // بناء المستنسخ المخفي (يُعاد بناؤه مع كل صفحة/طبعة جديدة فقط)
    let m = measureRef.current;
    if (!m || m.dataset.pageKey !== pageKey) {
      if (m) m.remove();
      m = document.createElement('div');
      m.setAttribute('aria-hidden', 'true');
      m.style.cssText =
        'position:fixed;top:0;inset-inline-start:-99999px;visibility:hidden;' +
        'pointer-events:none;z-index:-1;';
      m.className = el.className;
      m.innerHTML = el.innerHTML;
      m.dataset.pageKey = pageKey;
      document.body.appendChild(m);
      measureRef.current = m;
    }
    m.style.width = `${boxW}px`;

    // خانات دوائر أرقام الآيات — عرضها يتغير مع الحجم فيؤثر على اللف
    const circleSlots = Array.from(
      m.querySelectorAll('span[class*="inline-flex"]'),
    ) as HTMLElement[];

    /** هل الحجم المقترح يُدخل النص في الصندوق دون فيضان؟ */
    const fits = (size: number): boolean => {
      m!.style.fontSize = `${size}px`;
      const slotW = Math.round(size * 1.12) + 8; // الدائرة + هوامشها
      for (const c of circleSlots) {
        c.style.display = 'inline-block';
        c.style.width = `${slotW}px`;
        c.style.height = '0px';
        c.style.fontSize = '0px';
      }
      void m!.offsetHeight;
      return m!.scrollHeight <= boxH;
    };

    // بحث ثنائي: أكبر حجم (بدقة 0.5px) لا يفيض — يملأ ≥96% تلقائياً
    // حارس «لا تقدم»: mid قد يساوي hi مع تقريب نصف الوحدة — بدونه حلقة لا نهائية
    let lo = 9;
    let hi = 84;
    let best = 9;
    while (hi - lo >= 0.5) {
      const mid = Math.round(((lo + hi) / 2) * 2) / 2;
      if (mid <= lo || mid >= hi) break;
      if (fits(mid)) {
        best = mid;
        lo = mid;
      } else {
        hi = mid;
      }
    }

    // لا تحدّث الحالة لو النتيجة نفسها — يقطع أي تذبذب عرض محتمل
    if (Math.abs(best - fitFontSizeRef.current) > 0.25) {
      fitFontSizeRef.current = best;
      setFitFontSize(best);
    }

    // تحقق خفيف بعد الرسم + فحوصات مؤجلة — لأي تغير تخطيط متأخر
    // (حد أقصى 6 محاولات ثم يتوقف — لا حلقات)
    if (fitVerifyRef.current < 6) {
      fitVerifyRef.current += 1;
      const attempt = (): void => {
        const el2 = flowRef.current;
        const box2 = flowBoxRef.current;
        if (!el2 || !box2 || el2.children.length === 0) return;
        const fill = el2.scrollHeight / Math.max(1, box2.clientHeight);
        if (fill < 0.88 || fill > 1.02) computeFit();
        else fitVerifyRef.current = 6; // استقرت — أوقف المحاولات
      };
      window.requestAnimationFrame(attempt);
      window.setTimeout(attempt, 400);
      window.setTimeout(attempt, 1000);
    }
  }, [pagesReady, currentPageNum, edition.id]);

  // إعادة الملاءمة مع كل صفحة/طبعة/تغيّر أبعاد
  useEffect(() => {
    fitVerifyRef.current = 0;
    computeFit();
  }, [computeFit, adaptive.pageW, adaptive.pageH]);

  // مراقب أبعاد الصندوق ومحتوى التدفق + اكتمال الخطوط:
  // يعيد الملاءمة فقط إذا خرج الملء عن النطاق (خاصية عدم الحساسية تمنع الحلقات)
  useEffect(() => {
    if (!pagesReady) return;
    const box = flowBoxRef.current;
    const flow = flowRef.current;
    if (!box) return;
    let raf = 0;
    const scheduleFit = () => {
      window.cancelAnimationFrame(raf);
      raf = window.requestAnimationFrame(() => {
        const el2 = flowRef.current;
        const box2 = flowBoxRef.current;
        if (!el2 || !box2 || el2.children.length === 0) return;
        const fill = el2.scrollHeight / Math.max(1, box2.clientHeight);
        if (fill < 0.88 || fill > 1.02) computeFit();
      });
    };
    const ro = new ResizeObserver(scheduleFit);
    ro.observe(box);
    if (flow) ro.observe(flow); // تغير ارتفاع المحتوى (بعد الخط) يعيد التقييم
    window.addEventListener('resize', scheduleFit);
    if (typeof document.fonts !== 'undefined') {
      void document.fonts.ready.then(scheduleFit);
    }
    // فحوصات مؤجلة تحسم أي سباق تخطيط عند الفتح أو تدوير الشاشة
    const timers = [350, 900, 2000].map((ms) => window.setTimeout(scheduleFit, ms));
    return () => {
      window.cancelAnimationFrame(raf);
      timers.forEach((t) => window.clearTimeout(t));
      ro.disconnect();
      window.removeEventListener('resize', scheduleFit);
    };
  }, [pagesReady, computeFit]);

  // حفظ آخر صفحة مقروءة مع كل تنقل
  useEffect(() => {
    saveLastReadPage(currentPageNum);
  }, [currentPageNum]);

  // ===== تقليب الصفحات (عربي RTL: التالية لليسار) =====
  const handleFlipPage = useCallback((direction: 'next' | 'prev') => {
    playPaperTurnSound();
    setFlipDirection(direction);
    setTimeout(() => {
      if (direction === 'next') setCurrentPageNum((p) => Math.min(604, p + 1));
      else setCurrentPageNum((p) => Math.max(1, p - 1));
    }, 140);
    setTimeout(() => setFlipDirection(null), 340);
  }, []);

  // اختيار سورة → فتح صفحتها مباشرة
  const handleSelectSurah = (surah: Surah) => {
    setCurrentPageNum(surah.page);
    exitSelectionMode();
    setShowLibrary(false);
    playPaperTurnSound();
    setCopiedToast(`سورة ${surah.name} — صفحة ${surah.page}`);
    setTimeout(() => setCopiedToast(null), 2500);
  };

  // ===== التحديد المتصل: ضغطة طويلة تبدأ + لمسة تمتد نطاقاً بلا فجوات =====
  /** بناء نطاق متصل من نقطتين — لا يجوز ترك آيات في النص (بطلب المالك) */
  const buildContiguousRange = (a: number, b: number): Set<number> => {
    const start = Math.min(a, b);
    const end = Math.max(a, b);
    const set = new Set<number>();
    for (let n = start; n <= end; n++) set.add(n);
    return set;
  };

  const enterSelectionMode = (ayahNumber: number) => {
    selectionAnchorRef.current = ayahNumber;
    setSelectedAyahs(new Set([ayahNumber]));
    setSelectionMode(true);
    if ('vibrate' in navigator) {
      try {
        navigator.vibrate(30);
      } catch {
        // تجاهل
      }
    }
  };

  /**
   * لمسة عادية أثناء التحديد: تمتد/تقلّص النطاق المتصل بين آية البداية
   * والآية الملموسة — تحديد 30 ثم 50 يحدد كل ما بينهما تلقائياً.
   */
  const extendSelectionTo = (ayahNumber: number) => {
    const anchor = selectionAnchorRef.current ?? ayahNumber;
    setSelectedAyahs(buildContiguousRange(anchor, ayahNumber));
    setSelectionMode(true);
    if ('vibrate' in navigator) {
      try {
        navigator.vibrate(15);
      } catch {
        // تجاهل
      }
    }
  };

  const exitSelectionMode = useCallback(() => {
    selectionAnchorRef.current = null;
    setSelectedAyahs(new Set());
    setSelectionMode(false);
  }, []);

  // نطاق التحديد: من أصغر لأكبر آية محددة
  const selectionRange = useMemo(() => {
    if (selectedAyahs.size === 0) return null;
    const nums = [...selectedAyahs];
    return { start: Math.min(...nums), end: Math.max(...nums) };
  }, [selectedAyahs]);

  // الآيات المحددة كاملة البيانات
  const selectedAyahObjects = useMemo(
    () => currentPage.ayahs.filter((a) => selectedAyahs.has(a.ayahNumber)),
    [currentPage, selectedAyahs],
  );

  /** تشغيل الصوت من شريط التحديد — يفتح ورقة الخيارات الثلاثة (بطلب المالك) */
  const handlePlaySelection = () => {
    setShowPlayOptions(true);
  };

  /** تشغيل نطاق فعلي مع إغلاق ورقة الخيارات */
  const startPlayback = (start: number, end: number) => {
    const surah = selectedAyahObjects[0]?.surahNumber ?? displayedSurahNumber;
    setRangeStartAyah(start);
    setRangeEndAyah(end);
    setShowPlayOptions(false);
    startRange(surah, start, end, 1, 1);
  };

  /** زر التشغيل في الشريط السفلي: بتحديد قائم يفتح خيارات التلاوة — وإلا نافذة الاستماع */
  const handleBottomPlayClick = () => {
    if (selectionMode && selectedAyahs.size > 0) {
      setShowPlayOptions(true);
      return;
    }
    const first = currentPage.ayahs[0];
    if (first) {
      setActiveAyah(first);
      setRangeStartAyah(first.ayahNumber);
      setRangeEndAyah(first.ayahNumber);
    }
    setShowListenModal(true);
  };

  /** فتح التفسير للآيات المحددة */
  const handleTafseerSelection = () => {
    const ayahs =
      selectedAyahObjects.length > 0
        ? selectedAyahObjects
        : currentPage.ayahs.filter((a) => selectionRange && a.ayahNumber >= selectionRange.start && a.ayahNumber <= selectionRange.end);
    setTafseerAyahs(ayahs);
    setShowTafseerModal(true);
  };

  /** حفظ كل الآيات المحددة (نظام الحفظ) */
  const handleMemorizeSelection = () => {
    const surah = selectedAyahObjects[0]?.surahNumber ?? displayedSurahNumber;
    let added = 0;
    let removed = 0;
    for (const num of selectedAyahs) {
      const now = toggleAyahMemorized(surah, num);
      if (now) added++;
      else removed++;
    }
    setCopiedToast(
      added > 0
        ? `تم تعليم ${added} آية كمحفوظة 🌟`
        : `تم إلغاء حفظ ${removed} آية`,
    );
    setTimeout(() => setCopiedToast(null), 3000);
    exitSelectionMode();
  };

  /** نسخ نص الآيات المحددة */
  const handleCopySelection = () => {
    const text = selectedAyahObjects
      .map((a) => `﴿${a.plainText}﴾`)
      .join('\n');
    navigator.clipboard.writeText(
      `${text}\n[${currentPage.surahName}: ${selectionRange?.start}${selectionRange && selectionRange.end !== selectionRange.start ? `—${selectionRange.end}` : ''}]`,
    );
    setCopiedToast('تم نسخ نص الآيات المحددة إلى الحافظة');
    setTimeout(() => setCopiedToast(null), 3000);
    exitSelectionMode();
  };

  /** مشاركة الآيات المحددة */
  const handleShareSelection = () => {
    const text = selectedAyahObjects.map((a) => `﴿${a.plainText}﴾`).join('\n');
    const shareText = `${text}\n[${currentPage.surahName}]`;
    if (navigator.share) {
      navigator.share({ title: currentPage.surahName, text: shareText }).catch(() => {});
    } else {
      navigator.clipboard.writeText(shareText);
      setCopiedToast('تم نسخ النص للمشاركة');
      setTimeout(() => setCopiedToast(null), 3000);
    }
    exitSelectionMode();
  };

  // ===== لوحة المفاتيح: أسهم للتقليب (عربي RTL: السهم يمين = التالية) =====
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (showLibrary || showListenModal || showTafseerModal || showPlayOptions) return;
      if (e.key === 'ArrowRight') handleFlipPage('next');
      else if (e.key === 'ArrowLeft') handleFlipPage('prev');
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [handleFlipPage, showLibrary, showListenModal, showTafseerModal, showPlayOptions]);

  // ===== زر الرجوع في أندرويد =====
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
      if (showPlayOptions) {
        setShowPlayOptions(false);
        return true;
      }
      if (showLibrary) {
        setShowLibrary(false);
        return true;
      }
      if (selectionMode) {
        exitSelectionMode();
        return true;
      }
      if (isPlayingRange) {
        stopAudio();
        return true;
      }
      return false;
    });
  }, [
    showTafseerModal,
    showListenModal,
    showPlayOptions,
    showLibrary,
    selectionMode,
    isPlayingRange,
    stopAudio,
    exitSelectionMode,
  ]);

  // ===== السحب (Swipe) لتقليب الصفحات — عربي RTL: السحب يميناً = التالية =====
  const handleTouchStart = (e: React.TouchEvent) => {
    touchStartX.current = e.touches[0].clientX;
    touchStartY.current = e.touches[0].clientY;
  };

  const handleTouchEnd = (e: React.TouchEvent) => {
    if (touchStartX.current === null || touchStartY.current === null) return;
    const deltaX = e.changedTouches[0].clientX - touchStartX.current;
    const deltaY = e.changedTouches[0].clientY - touchStartY.current;

    if (Math.abs(deltaX) > 45 && Math.abs(deltaX) > Math.abs(deltaY) * 1.5) {
      // إصلاح الاتجاه المعكوس (بطلب المالك): سحب يميناً = صفحة تالية
      if (deltaX > 0) handleFlipPage('next');
      else handleFlipPage('prev');
    }
    touchStartX.current = null;
    touchStartY.current = null;
  };

  // ===== الضغطة الطويلة فقط: تبدأ التحديد المتصل =====
  const startLongPress = (ayah: TajweedAyah) => {
    longPressFired.current = false;
    setPressingAyahNum(ayah.ayahNumber);
    if (longPressTimer.current) window.clearTimeout(longPressTimer.current);
    longPressTimer.current = window.setTimeout(() => {
      longPressFired.current = true;
      // ضغطة طويلة: تبدأ تحديداً جديداً من هذه الآية (أول مرة أو أثناء تحديد قائم)
      enterSelectionMode(ayah.ayahNumber);
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

  /**
   * اللمسة العادية (بدون ضغط طويل):
   * - في وضع التحديد: تمتد النطاق المتصل بين آية البداية والآية الملموسة
   * - خارج وضع التحديد: لا تفتح أي قائمة (بطلب المالك — القائمة بالضغط الطويل فقط)
   */
  const handleAyahTap = (ayah: TajweedAyah) => {
    if (longPressFired.current) {
      longPressFired.current = false;
      return;
    }
    if (selectionMode) {
      extendSelectionTo(ayah.ayahNumber);
      return;
    }
    // لمسة عادية خارج التحديد — لا شيء (تمنع اللمس من التقليب العرضي أيضاً)
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
      setRangeStartAyah(match.ayahNumber);
      setRangeEndAyah(match.ayahNumber);
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
  const currentJuz = getJuzForPage(currentPageNum);
  const khatmaProgress = getKhatmaProgress();
  const lastRead = getLastReadPage();

  // =====================================================================
  // تدفق الآيات بالتلوين التجويدي المعتمد حسب الطبعة
  // =====================================================================
  const renderAyahFlow = () => (
    <div
      ref={flowRef}
      className="text-justify font-quran font-normal dir-rtl leading-[2.2]"
      style={{ color: edition.ink, fontSize: `${fitFontSize}px` }}
    >
      {currentPage.ayahs.map((ayah) => {
        const isPlaying = currentPlayingAyahNum === ayah.ayahNumber;
        const isSelected = selectedAyahs.has(ayah.ayahNumber);
        const isMasked = memorizationTestMode && !revealedAyahs[ayah.ayahNumber];
        const isMemorized = isAyahMemorized(ayah.surahNumber, ayah.ayahNumber);
        const isPressing = pressingAyahNum === ayah.ayahNumber;

        return (
          <span
            key={`${ayah.surahNumber}-${ayah.ayahNumber}`}
            onContextMenu={(e) => {
              e.preventDefault();
              if (selectionMode) extendSelectionTo(ayah.ayahNumber);
              else enterSelectionMode(ayah.ayahNumber);
            }}
            onTouchStart={() => startLongPress(ayah)}
            onTouchEnd={() => {
              cancelLongPress();
              // الضغطة الطويلة مافضلتش؟ → لمسة عادية
              if (!longPressFired.current) handleAyahTap(ayah);
            }}
            onTouchMove={cancelLongPress}
            onTouchCancel={cancelLongPress}
            onMouseDown={() => startLongPress(ayah)}
            onMouseUp={() => {
              cancelLongPress();
              if (!longPressFired.current) handleAyahTap(ayah);
            }}
            onMouseLeave={cancelLongPress}
            className={`inline cursor-pointer transition-all duration-150 rounded px-1 py-0.5 mx-0.5 select-none ${
              isPressing ? 'scale-[1.02]' : ''
            } ${
              isSelected
                ? 'bg-amber-300/90 shadow-sm ring-2 ring-amber-500'
                : isPlaying
                ? 'bg-emerald-300/90 shadow-sm ring-2 ring-emerald-600'
                : 'hover:bg-amber-100/25'
            }`}
            title={
              selectionMode
                ? 'المس آية لتمتد النطاق المتصل حتى آية البداية'
                : 'اضغط مطولاً لبدء تحديد الآيات ثم المس لتمديد النطاق'
            }
          >
            {isMasked ? (
              <span
                onTouchEnd={(e) => {
                  e.stopPropagation();
                  cancelLongPress();
                }}
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

            {/* رقم الآية بالزخرفة العثمانية + علامة الحفظ */}
            <span
              className="inline-flex items-center justify-center font-sans font-bold border rounded-full mx-1 shadow-inner align-middle relative"
              style={{
                width: `${Math.round(fitFontSize * 1.12)}px`,
                height: `${Math.round(fitFontSize * 1.12)}px`,
                fontSize: `${Math.max(9, Math.round(fitFontSize * 0.44))}px`,
                color: edition.ayahNumberColor,
                borderColor: edition.frame,
                backgroundColor: isNightEdition ? 'rgba(255,255,255,0.06)' : '#fbf6e3',
              }}
            >
              {ayah.ayahNumber}
              {isMemorized && (
                <span
                  className="absolute -top-1 -left-1 w-2 h-2 rounded-full"
                  style={{ backgroundColor: '#10b981' }}
                  title="محفوظة"
                />
              )}
            </span>
          </span>
        );
      })}
    </div>
  );

  // =====================================================================
  // ورقة المصحف — تملأ الشاشة كاملة بلا اسكرول + مفتاح ألوان التجويد أسفلها
  // =====================================================================
  /** قواعد التجويد الموجودة فعلاً في هذه الصفحة — لمفتاح الألوان أسفل كل صفحة */
  const pageTajweedRules = useMemo(() => {
    if (!edition.showTajweed) return [];
    const present = new Set<TajweedRule>();
    for (const ayah of currentPage.ayahs) {
      for (const seg of ayah.segments) {
        if (seg.rule) present.add(seg.rule);
      }
    }
    return (Object.keys(TAJWEED_RULES) as TajweedRule[]).filter((k) => present.has(k));
  }, [currentPage, edition.showTajweed]);

  const renderTajweedLegend = () => {
    if (!edition.showTajweed || pageTajweedRules.length === 0) return null;
    return (
      <div
        className="shrink-0 mt-1.5 pt-1.5 border-t-2 px-1 flex flex-wrap items-center gap-x-3 gap-y-0.5 justify-center"
        style={{ borderColor: edition.innerFrame }}
        dir="rtl"
      >
        {pageTajweedRules.map((ruleKey) => {
          const rule = TAJWEED_RULES[ruleKey];
          return (
            <span
              key={ruleKey}
              className="inline-flex items-center gap-1 font-sans"
              style={{ fontSize: `${Math.max(7.5, Math.round(fitFontSize * 0.32))}px`, color: edition.ink }}
            >
              <span
                className="inline-block w-2 h-2 rounded-full shrink-0"
                style={{ backgroundColor: isNightEdition ? rule.nightColor : rule.color }}
              />
              {rule.label}
            </span>
          );
        })}
      </div>
    );
  };

  const renderMushafPage = () => (
    <div
      ref={pageContainerRef}
      onTouchStart={handleTouchStart}
      onTouchEnd={handleTouchEnd}
      className={`relative mx-auto shadow-2xl overflow-hidden select-none book-spine-shadow rounded-lg border-[5px] sm:border-[6px] ${
        flipDirection === 'next'
          ? 'book-flip-forward'
          : flipDirection === 'prev'
          ? 'book-flip-backward'
          : ''
      }`}
      style={{
        width: `${adaptive.pageW}px`,
        height: `${adaptive.pageH}px`,
        backgroundColor: edition.paper,
        borderColor: edition.frame,
        boxShadow: '0 14px 44px rgba(0,0,0,0.55)',
      }}
      dir="rtl"
    >
      <div
        className="h-full p-2 sm:p-3 border-[3px] rounded-md relative flex flex-col overflow-hidden"
        style={{ borderColor: edition.innerFrame }}
      >
        {/* ترويسة الصفحة داخل إطار المصحف (زي المصحف الحقيقي) */}
        <div
          className="flex items-center justify-between shrink-0 pb-1.5 mb-2 border-b-2"
          style={{
            borderColor: edition.frame,
            color: edition.ink,
            fontSize: `${Math.max(9, fitFontSize * 0.4)}px`,
          }}
        >
          <div
            className="px-2.5 py-0.5 rounded border font-sans font-bold"
            style={{
              borderColor: edition.frame,
              backgroundColor: isNightEdition ? 'rgba(255,255,255,0.05)' : `${edition.frame}22`,
            }}
          >
            الجزء {currentPage.juzNumber} • {currentPage.hizbQuarter}
          </div>

          <button
            onClick={() => {
              setLibraryTab('surahs');
              setShowLibrary(true);
            }}
            className="px-3 py-0.5 rounded border font-arabic font-bold transition-all hover:brightness-110"
            style={{
              fontSize: `${Math.max(10, fitFontSize * 0.45)}px`,
              borderColor: edition.innerFrame,
              color: edition.ink,
              backgroundColor: isNightEdition ? 'rgba(255,255,255,0.05)' : `${edition.innerFrame}18`,
            }}
            title="فهرس السور والبحث والأدوات"
          >
            {currentPage.surahName} ▾
          </button>
        </div>

        {/* تدفق الآيات — يملأ الصفحة بالكامل بلا اسكرول (بطلب المالك) */}
        <div ref={flowBoxRef} className="flex-1 min-h-0 overflow-hidden">
          {renderAyahFlow()}
        </div>

        {/* مفتاح ألوان التجويد أسفل كل صفحة (بطلب المالك) */}
        {renderTajweedLegend()}

        {/* تذييل الصفحة: الجزء | رقم الصفحة | السورة */}
        <div
          className="mt-1.5 pt-1.5 border-t-2 shrink-0 flex items-center justify-between font-bold px-1"
          style={{
            borderColor: edition.frame,
            color: edition.ink,
            fontSize: `${Math.max(9, fitFontSize * 0.4)}px`,
          }}
        >
          <span>جزء {currentPage.juzNumber}</span>
          <span
            className="font-extrabold"
            style={{
              color: edition.ayahNumberColor,
              fontSize: `${Math.max(11, fitFontSize * 0.52)}px`,
            }}
          >
            {currentPage.pageNumber}
          </span>
          <span>{currentPage.surahName}</span>
        </div>
      </div>
    </div>
  );

  // =====================================================================
  // الشريط السفلي النحيف: متابعة القراءة + الأجزاء + الختمات + تعليم الصفحة
  // (فوق شريط التنقل الرئيسي مباشرة)
  // =====================================================================
  const renderBottomStrip = () => (
    <div
      className="shrink-0 flex items-center justify-between gap-1 px-2 py-1 border-t backdrop-blur-md"
      style={{
        backgroundColor: 'color-mix(in srgb, var(--noor-n-950) 92%, transparent)',
        borderColor: 'color-mix(in srgb, var(--noor-p-500) 22%, transparent)',
      }}
      dir="rtl"
    >
      {/* متابعة القراءة: آخر صفحة مفتوحة */}
      <button
        onClick={() => {
          setCurrentPageNum(lastRead);
          setCopiedToast(`متابعة القراءة — صفحة ${lastRead}`);
          setTimeout(() => setCopiedToast(null), 2000);
        }}
        className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl text-[11px] font-bold transition-colors hover:bg-white/10 max-w-[38%]"
        style={{ color: currentTheme.color }}
        title="الانتقال لآخر صفحة قرأتها"
      >
        <MapPin className="w-3.5 h-3.5 shrink-0" style={{ color: 'var(--noor-a-400)' }} />
        <span className="truncate">
          متابعة القراءة: <span style={{ color: 'var(--noor-a-400)' }}>صفحة {lastRead}</span>
        </span>
      </button>

      {/* منتصف: رقم الصفحة الحالية */}
      <div className="flex items-center gap-1 text-[11px] font-mono font-bold shrink-0" style={{ color: 'var(--noor-n-300)' }}>
        <button
          onClick={() => handleFlipPage('prev')}
          disabled={currentPageNum <= 1}
          className="p-1 rounded-lg disabled:opacity-30 hover:bg-white/10"
          style={{ color: 'var(--noor-n-200)' }}
          title="الصفحة السابقة"
        >
          <Plus className="w-3.5 h-3.5 rotate-90" />
        </button>
        <span>
          {currentPageNum} / 604
        </span>
        <button
          onClick={() => handleFlipPage('next')}
          disabled={currentPageNum >= 604}
          className="p-1 rounded-lg disabled:opacity-30 hover:bg-white/10"
          style={{ color: 'var(--noor-n-200)' }}
          title="الصفحة التالية"
        >
          <Minus className="w-3.5 h-3.5 rotate-90" />
        </button>
      </div>

      {/* يمين: التشغيل + الأجزاء + الختمات + تعليم الصفحة */}
      <div className="flex items-center gap-0.5 shrink-0">
        <button
          onClick={handleBottomPlayClick}
          className="p-1.5 rounded-xl transition-colors hover:bg-white/10"
          style={{ color: 'var(--noor-a-400)' }}
          title={
            selectionMode && selectedAyahs.size > 0
              ? 'خيارات تلاوة الآيات المحددة'
              : 'الاستماع إلى هذه الصفحة أو السورة'
          }
        >
          <Play className="w-4 h-4 fill-current" />
        </button>

        <button
          onClick={() => {
            setLibraryTab('juz');
            setShowLibrary(true);
          }}
          className="p-1.5 rounded-xl transition-colors hover:bg-white/10"
          style={{ color: 'var(--noor-n-200)' }}
          title={`فهرس الأجزاء الثلاثين — أنت في الجزء ${currentJuz}`}
        >
          <Layers className="w-4 h-4" />
        </button>

        <button
          onClick={() => {
            setLibraryTab('khatma');
            setShowLibrary(true);
          }}
          className="p-1.5 rounded-xl transition-colors hover:bg-white/10 relative"
          style={{ color: 'var(--noor-n-200)' }}
          title={`الختمات — ${khatmaProgress.percent}% من الختمة الحالية`}
        >
          <CircleDot className="w-4 h-4" />
          <span
            className="absolute -bottom-0.5 -left-0.5 text-[7px] font-black px-1 rounded-full"
            style={{ backgroundColor: 'var(--noor-p-500)', color: '#fff' }}
          >
            {khatmaProgress.percent}%
          </span>
        </button>

        <button
          onClick={() => {
            const completed = markKhatmaPageRead(currentPageNum);
            setKhatmaTick((t) => t + 1);
            setCopiedToast(
              completed
                ? 'ما شاء الله! أكملت ختمة كاملة — تقبل الله منك 🎉'
                : pageReadInKhatma
                ? 'الصفحة معلّمة مقروءة أصلاً'
                : `تم تعليم صفحة ${currentPageNum} كمقروءة في الختمة ✓`,
            );
            setTimeout(() => setCopiedToast(null), 2500);
          }}
          className={`p-1.5 rounded-xl transition-colors ${
            pageReadInKhatma ? 'bg-emerald-500/25' : 'hover:bg-white/10'
          }`}
          style={{ color: pageReadInKhatma ? '#34d399' : 'var(--noor-n-200)' }}
          title={pageReadInKhatma ? 'الصفحة مقروءة في الختمة' : 'تعليم الصفحة كمقروءة في الختمة'}
        >
          {pageReadInKhatma ? <CheckCheck className="w-4 h-4" /> : <Check className="w-4 h-4" />}
        </button>
      </div>
    </div>
  );

  // =====================================================================
  // شريط إجراءات التحديد المتعدد (فوق الشريط السفلي)
  // =====================================================================
  const renderSelectionBar = () => {
    if (!selectionMode || selectedAyahs.size === 0) return null;
    const count = selectedAyahs.size;
    return (
      <div
        className="shrink-0 flex items-center gap-1.5 px-2.5 py-1.5 border-t backdrop-blur-xl animate-in fade-in slide-in-from-bottom-2"
        style={{
          background: `linear-gradient(135deg, color-mix(in srgb, var(--noor-p-500) 22%, transparent), color-mix(in srgb, var(--noor-n-950) 92%, transparent))`,
          borderColor: 'color-mix(in srgb, var(--noor-p-500) 35%, transparent)',
        }}
        dir="rtl"
      >
        <span className="text-[11px] font-extrabold shrink-0" style={{ color: 'var(--noor-n-50)' }}>
          محدد {count} {count === 1 ? 'آية' : 'آيات'}
          {selectionRange && selectionRange.end !== selectionRange.start && (
            <span className="text-[10px] font-normal opacity-70">
              {' '}
              ({selectionRange.start}—{selectionRange.end})
            </span>
          )}
        </span>

        <div className="flex-1 flex items-center justify-end gap-1 flex-wrap">
          <SelectionAction icon={<Play className="w-4 h-4" />} label="تلاوة المحدد" color="#10b981" onClick={handlePlaySelection} />
          <SelectionAction icon={<BookOpen className="w-4 h-4" />} label="تفسير" color="#14b8a6" onClick={handleTafseerSelection} />
          <SelectionAction icon={<Sparkles className="w-4 h-4" />} label="حفظ" color="#f59e0b" onClick={handleMemorizeSelection} />
          <SelectionAction icon={<Copy className="w-4 h-4" />} label="نسخ" color="#0ea5e9" onClick={handleCopySelection} />
          <SelectionAction icon={<Share2 className="w-4 h-4" />} label="مشاركة" color="#f43f5e" onClick={handleShareSelection} />
          <button
            onClick={exitSelectionMode}
            className="flex items-center gap-1 px-2.5 py-1.5 rounded-xl text-[11px] font-bold border transition-colors hover:bg-white/10"
            style={{
              borderColor: 'color-mix(in srgb, var(--noor-p-500) 30%, transparent)',
              color: 'var(--noor-n-200)',
            }}
          >
            <X className="w-3.5 h-3.5" />
            إلغاء
          </button>
        </div>
      </div>
    );
  };

  // =====================================================================
  // العرض: ورقة المصحف بملء الشاشة فوق شريط التنقل مباشرة
  // =====================================================================
  return (
    <div
      ref={screenRef}
      className="fixed inset-x-0 top-0 bottom-[calc(74px+env(safe-area-inset-bottom))] z-[85] flex flex-col bg-[#05070d]"
      dir="rtl"
    >
      {/* Toast */}
      {copiedToast && (
        <div className="fixed top-3 left-1/2 -translate-x-1/2 z-[95] px-4 py-2.5 bg-emerald-600 text-white rounded-2xl shadow-2xl flex items-center gap-2.5 text-xs sm:text-sm">
          <Check className="w-4 h-4 text-emerald-200 shrink-0" />
          <span>{copiedToast}</span>
          <button
            onClick={() => setCopiedToast(null)}
            className="text-xs text-emerald-200 hover:text-white px-1.5 py-0.5"
          >
            إغلاق
          </button>
        </div>
      )}

      {/* زر إيقاف التلاوة العائم أثناء التشغيل فقط */}
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

      {/* تلميح التحديد أول مرة يفعّل فيه الوضع */}
      {selectionMode && (
        <div
          className="fixed top-3 left-1/2 -translate-x-1/2 z-[92] px-4 py-2 rounded-2xl text-[11px] backdrop-blur-md shadow-xl animate-in fade-in"
          style={{
            backgroundColor: 'color-mix(in srgb, var(--noor-p-500) 25%, var(--noor-n-950))',
            color: 'var(--noor-n-50)',
            border: '1px solid color-mix(in srgb, var(--noor-p-500) 40%, transparent)',
          }}
        >
          المس الآيات لإضافتها للتحديد • الضغطة الطويلة على آية محددة تزيلها
        </div>
      )}

      {/* ورقة المصحف تملأ المساحة المتاحة */}
      <div className="flex-1 min-h-0 flex items-center justify-center p-1.5 sm:p-2.5">
        {renderMushafPage()}
      </div>

      {/* مناطق لمس الحواف للتقليب — بلا أي شكل مرئي */}
      <button
        aria-label="الصفحة التالية"
        onClick={() => handleFlipPage('next')}
        disabled={currentPageNum >= 604}
        className="fixed left-0 w-[8%] max-w-[60px] z-[84] disabled:opacity-0 bg-transparent border-0 cursor-w-resize active:bg-white/[0.04]"
        style={{ top: 0, bottom: 'calc(74px + env(safe-area-inset-bottom))' }}
      />
      <button
        aria-label="الصفحة السابقة"
        onClick={() => handleFlipPage('prev')}
        disabled={currentPageNum <= 1}
        className="fixed right-0 w-[8%] max-w-[60px] z-[84] disabled:opacity-0 bg-transparent border-0 cursor-e-resize active:bg-white/[0.04]"
        style={{ top: 0, bottom: 'calc(74px + env(safe-area-inset-bottom))' }}
      />

      {/* شريط إجراءات التحديد (فوق الشريط السفلي) */}
      {renderSelectionBar()}

      {/* الشريط السفلي النحيف — فوق شريط التنقل مباشرة */}
      {renderBottomStrip()}

      {/* مكتبة المصحف */}
      {showLibrary && (
        <LibraryModal
          initialTab={libraryTab}
          onClose={() => setShowLibrary(false)}
          onSelectSurah={handleSelectSurah}
          onSelectJuz={(juz) => {
            setCurrentPageNum(getJuzRange(juz).start);
            exitSelectionMode();
            setShowLibrary(false);
          }}
          searchQuery={searchQuery}
          onSearchChange={handleSearchChange}
          searchResults={searchResults}
          onSelectSearchResult={handleSelectSearchResult}
          bookmarks={bookmarks}
          onSelectBookmark={(bm) => {
            setCurrentPageNum(bm.pageNumber);
            setShowLibrary(false);
          }}
          onRemoveBookmark={(id) => {
            removeBookmark(id);
            setBookmarks(getSavedBookmarks());
          }}
          memorizationTestMode={memorizationTestMode}
          onToggleMemorization={() => setMemorizationTestMode((v) => !v)}
          edition={edition}
          onOpenListen={() => {
            setShowLibrary(false);
            if (selectionRange) {
              setRangeStartAyah(selectionRange.start);
              setRangeEndAyah(selectionRange.end);
            } else {
              const first = currentPage.ayahs[0];
              if (first) {
                setActiveAyah(first);
                setRangeStartAyah(first.ayahNumber);
                setRangeEndAyah(first.ayahNumber);
              }
            }
            setShowListenModal(true);
          }}
          khatmaTick={khatmaTick}
          onResetKhatma={() => setKhatmaTick((t) => t + 1)}
          currentJuz={currentJuz}
        />
      )}

      {/* ورقة خيارات تلاوة الآيات المحددة — بطلب المالك:
          تشغيل المحدد فقط / السورة كاملة / من بداية التحديد إلى نهاية السورة */}
      {showPlayOptions && (
        <PlayOptionsSheet
          surahName={currentPage.surahName}
          surahAyahCount={surahAyahCount}
          selStart={selectionRange?.start ?? 1}
          selEnd={selectionRange?.end ?? 1}
          reciterName={selectedReciter.name}
          onPlay={startPlayback}
          onClose={() => setShowPlayOptions(false)}
        />
      )}

      {/* الاستماع (للنطاقات والتكرار المخصص) */}
      {showListenModal && (
        <ListenModal
          firstAyahOnPage={currentPage.ayahs[0]?.ayahNumber ?? 1}
          lastAyahOnPage={currentPage.ayahs[currentPage.ayahs.length - 1]?.ayahNumber ?? 1}
          surahName={currentPage.surahName}
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
            startRange(displayedSurahNumber, start, end, per, all);
          }}
        />
      )}

      {/* التفسير (للآية أو الآيات المحددة) */}
      {showTafseerModal && tafseerAyahs.length > 0 && (
        <TafseerModal
          ayahs={tafseerAyahs}
          surahName={currentPage.surahName}
          edition={edition}
          onClose={() => setShowTafseerModal(false)}
          onListen={() => {
            setShowTafseerModal(false);
            const first = tafseerAyahs[0];
            const last = tafseerAyahs[tafseerAyahs.length - 1];
            setRangeStartAyah(first.ayahNumber);
            setRangeEndAyah(last.ayahNumber);
            setShowListenModal(true);
          }}
        />
      )}
    </div>
  );
};

/** قيمة لون الثيم الحالي — تستخدم في الشريط السفلي */
const currentTheme = { color: 'var(--noor-n-100)' };

/** زر إجراء في شريط التحديد */
const SelectionAction: React.FC<{
  icon: React.ReactNode;
  label: string;
  color: string;
  onClick: () => void;
}> = ({ icon, label, color, onClick }) => (
  <button
    onClick={onClick}
    className="flex items-center gap-1 px-2.5 py-1.5 rounded-xl text-[11px] font-bold transition-all hover:brightness-110 active:scale-95"
    style={{ backgroundColor: `${color}26`, color, border: `1px solid ${color}55` }}
  >
    {icon}
    <span className="hidden xs:inline sm:inline">{label}</span>
  </button>
);

/* ============================================================================
 * ورقة خيارات التلاوة للآيات المحددة — بطلب المالك:
 * تشغيل المحدد فقط / تشغيل السورة كاملة / من بداية التحديد إلى نهاية السورة
 * ========================================================================== */
const PlayOptionsSheet: React.FC<{
  surahName: string;
  surahAyahCount: number;
  selStart: number;
  selEnd: number;
  reciterName: string;
  onPlay: (start: number, end: number) => void;
  onClose: () => void;
}> = ({ surahName, surahAyahCount, selStart, selEnd, reciterName, onPlay, onClose }) => {
  const isSingle = selStart === selEnd;
  return (
    <div
      className="fixed inset-0 z-[96] flex items-end sm:items-center justify-center bg-black/80 backdrop-blur-sm animate-in fade-in duration-200 p-0 sm:p-4"
      onClick={onClose}
      dir="rtl"
    >
      <div
        className="w-full max-w-md rounded-t-3xl sm:rounded-3xl bg-slate-900 border border-slate-800 p-5 space-y-3.5 text-right shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between pb-2.5 border-b border-slate-800">
          <h3 className="text-base font-bold text-white flex items-center gap-2">
            <Play className="w-4.5 h-4.5 text-emerald-400 fill-current" />
            <span>خيارات التلاوة</span>
          </h3>
          <button
            onClick={onClose}
            className="p-1.5 rounded-xl bg-slate-800 text-slate-400 hover:text-white"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* ملخص التحديد */}
        <div className="px-3.5 py-2.5 rounded-2xl bg-slate-950 border border-slate-800 flex items-center justify-between text-[11px]">
          <span className="font-bold text-emerald-300">سورة {surahName}</span>
          <span className="text-slate-400">
            {isSingle
              ? `الآية ${selStart}`
              : `التحديد: الآيات ${selStart} — ${selEnd}`}
            {' • '}
            <span className="font-sans">{reciterName}</span>
          </span>
        </div>

        {/* الخيارات الثلاثة — بطلب المالك */}
        <div className="space-y-2">
          <PlayOptionRow
            title="تشغيل المحدد فقط"
            subtitle={
              isSingle ? `تلاوة الآية ${selStart} وحدها` : `تلاوة الآيات ${selStart} — ${selEnd}`
            }
            color="#10b981"
            onClick={() => onPlay(selStart, selEnd)}
          />
          <PlayOptionRow
            title="تشغيل السورة كاملة"
            subtitle={`سورة ${surahName} من الآية 1 إلى الآية ${surahAyahCount}`}
            color="#f59e0b"
            onClick={() => onPlay(1, surahAyahCount)}
          />
          <PlayOptionRow
            title="من بداية التحديد إلى نهاية السورة"
            subtitle={`من الآية ${selStart} إلى الآية ${surahAyahCount}`}
            color="#0ea5e9"
            onClick={() => onPlay(selStart, surahAyahCount)}
          />
        </div>

        <button
          onClick={onClose}
          className="w-full py-2.5 rounded-2xl bg-slate-950 border border-slate-800 text-slate-300 text-xs font-bold hover:bg-slate-800 transition-colors"
        >
          إلغاء
        </button>
      </div>
    </div>
  );
};

/** صف خيار في ورقة خيارات التلاوة */
const PlayOptionRow: React.FC<{
  title: string;
  subtitle: string;
  color: string;
  onClick: () => void;
}> = ({ title, subtitle, color, onClick }) => (
  <button
    onClick={onClick}
    className="w-full p-3.5 rounded-2xl border text-right flex items-center gap-3 transition-all hover:brightness-125 active:scale-[0.99]"
    style={{ backgroundColor: `${color}14`, borderColor: `${color}45` }}
  >
    <span
      className="w-9 h-9 rounded-xl flex items-center justify-center shrink-0"
      style={{ backgroundColor: `${color}26`, color }}
    >
      <Play className="w-4 h-4 fill-current" />
    </span>
    <span className="min-w-0">
      <span className="block text-sm font-extrabold" style={{ color }}>
        {title}
      </span>
      <span className="block text-[11px] text-slate-400 truncate">{subtitle}</span>
    </span>
  </button>
);

/* ============================================================================
 * مكتبة المصحف — السور | الأجزاء | الختمات | البحث | الإشارات | أدوات الحفظ
 * ========================================================================== */
interface LibraryModalProps {
  initialTab: 'surahs' | 'juz' | 'khatma' | 'search' | 'bookmarks' | 'tools';
  onClose: () => void;
  onSelectSurah: (surah: Surah) => void;
  onSelectJuz: (juz: number) => void;
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
  khatmaTick: number;
  onResetKhatma: () => void;
  currentJuz: number;
}

const LibraryModal: React.FC<LibraryModalProps> = ({
  initialTab,
  onClose,
  onSelectSurah,
  onSelectJuz,
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
  khatmaTick,
  onResetKhatma,
  currentJuz,
}) => {
  const [tab, setTab] = useState(initialTab);
  const [surahQuery, setSurahQuery] = useState('');
  const [filter, setFilter] = useState<'all' | 'meccan' | 'medinan'>('all');

  // تحديث بيانات الختمة والحفظ عند التغيير
  const khatmaState = useMemo(() => getKhatma(), [khatmaTick]);
  const khatmaProgress = useMemo(
    () => getKhatmaProgress(),
    [khatmaTick],
  );
  const memorization = useMemo(() => getMemorizationProgress(), [khatmaTick]);
  const reviewQueue = useMemo(() => getReviewQueue(6), [khatmaTick]);

  const tabs: Array<{
    key: typeof tab;
    label: string;
    Icon: React.FC<{ className?: string }>;
  }> = [
    { key: 'surahs', label: 'السور', Icon: BookOpen },
    { key: 'juz', label: 'الأجزاء', Icon: Layers },
    { key: 'khatma', label: 'الختمات', Icon: CircleDot },
    { key: 'search', label: 'البحث', Icon: Search },
    { key: 'bookmarks', label: 'إشاراتي', Icon: Bookmark },
    { key: 'tools', label: 'حفظي', Icon: Sliders },
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
        <div className="flex items-center gap-1 shrink-0 overflow-x-auto">
          {tabs.map(({ key, label, Icon }) => (
            <button
              key={key}
              onClick={() => setTab(key)}
              className={`flex-1 min-w-[64px] flex flex-col items-center gap-0.5 py-2 rounded-xl text-[10px] font-bold transition-colors ${
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

        {/* ===== الأجزاء الثلاثون ===== */}
        {tab === 'juz' && (
          <div className="flex-1 overflow-y-auto grid grid-cols-2 sm:grid-cols-3 gap-2 pr-1 min-h-0">
            {Array.from({ length: 30 }, (_, i) => i + 1).map((juz) => {
              const { start, end } = getJuzRange(juz);
              const prog = getJuzProgress(juz);
              const isCurrent = juz === currentJuz;
              return (
                <button
                  key={juz}
                  onClick={() => onSelectJuz(juz)}
                  className={`p-3 rounded-2xl border text-right space-y-1.5 transition-all ${
                    isCurrent
                      ? 'bg-gradient-to-br from-amber-600/25 to-amber-900/20 border-amber-500/60'
                      : 'bg-slate-950 border-slate-800/80 hover:border-amber-500/40'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <span className="text-sm font-extrabold text-slate-100">الجزء {juz}</span>
                    {isCurrent && (
                      <span className="text-[9px] px-1.5 py-0.5 rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/30 font-bold">
                        أنت هنا
                      </span>
                    )}
                  </div>
                  <div className="text-[10px] text-slate-400 font-sans">
                    صفحات {start}—{end}
                  </div>
                  <div className="h-1.5 rounded-full bg-slate-800 overflow-hidden">
                    <div
                      className="h-full rounded-full transition-all"
                      style={{
                        width: `${prog.percent}%`,
                        background:
                          prog.percent === 100
                            ? 'linear-gradient(90deg,#10b981,#34d399)'
                            : 'linear-gradient(90deg,#f59e0b,#fbbf24)',
                      }}
                    />
                  </div>
                  <div className="text-[9px] text-slate-500 font-sans">
                    {prog.read}/{prog.total} صفحة {prog.percent === 100 ? 'مكتمل ✓' : ''}
                  </div>
                </button>
              );
            })}
          </div>
        )}

        {/* ===== الختمات ===== */}
        {tab === 'khatma' && (
          <div className="flex-1 overflow-y-auto space-y-4 pr-1 min-h-0">
            {/* الختمة الجارية */}
            <div className="p-4 rounded-3xl bg-gradient-to-br from-emerald-950/80 to-slate-950 border border-emerald-700/40 text-center space-y-3">
              <p className="text-sm font-bold text-emerald-300">ختمة جارية</p>
              <div className="relative w-36 h-36 mx-auto">
                <svg viewBox="0 0 120 120" className="w-full h-full -rotate-90">
                  <circle cx="60" cy="60" r="52" fill="none" stroke="#1e293b" strokeWidth="10" />
                  <circle
                    cx="60"
                    cy="60"
                    r="52"
                    fill="none"
                    stroke="url(#khatmaGrad)"
                    strokeWidth="10"
                    strokeLinecap="round"
                    strokeDasharray={`${(khatmaProgress.percent / 100) * 326.7} 326.7`}
                  />
                  <defs>
                    <linearGradient id="khatmaGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                      <stop offset="0%" stopColor="#10b981" />
                      <stop offset="100%" stopColor="#34d399" />
                    </linearGradient>
                  </defs>
                </svg>
                <div className="absolute inset-0 flex flex-col items-center justify-center">
                  <span className="text-2xl font-black text-white font-mono">
                    {khatmaProgress.percent}%
                  </span>
                  <span className="text-[10px] text-slate-400">
                    {khatmaProgress.read} / 604 صفحة
                  </span>
                </div>
              </div>
              <p className="text-[11px] text-slate-400">
                بدأت {new Date(khatmaState.startedAt).toLocaleDateString('ar-EG')}
                {khatmaProgress.read > 0 && (
                  <> • متبقٍ {604 - khatmaProgress.read} صفحة</>
                )}
              </p>
              <button
                onClick={() => {
                  resetKhatma();
                  onResetKhatma();
                }}
                className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl text-[11px] font-bold border border-rose-500/40 text-rose-300 hover:bg-rose-500/10 transition-colors"
              >
                <RotateCcw className="w-3.5 h-3.5" />
                إعادة بدء الختمة من الصفر
              </button>
            </div>

            {/* سجل الختمات المكتملة */}
            <div className="p-4 rounded-3xl bg-slate-950 border border-slate-800 space-y-2">
              <p className="text-sm font-bold text-slate-200">الختمات المكتملة 🎉</p>
              {khatmaState.history.length === 0 ? (
                <p className="text-[11px] text-slate-500 leading-relaxed">
                  لسه مفيش ختمات مكتملة. علّم كل صفحة تخلصها بزر ✓ في الشريط السفلي،
                  وكمّل لحد ما تخلص الـ604 صفحة كلها.
                </p>
              ) : (
                khatmaState.history
                  .slice()
                  .reverse()
                  .map((h, i) => (
                    <div
                      key={h.completedAt}
                      className="flex items-center justify-between p-2.5 rounded-xl bg-slate-900 border border-slate-800/60"
                    >
                      <span className="text-xs font-bold text-emerald-300">
                        ختمة {khatmaState.history.length - i}
                      </span>
                      <span className="text-[10px] text-slate-400 font-sans">
                        {new Date(h.completedAt).toLocaleDateString('ar-EG')} • في {h.days}{' '}
                        {h.days === 1 ? 'يوم' : 'أيام'}
                      </span>
                    </div>
                  ))
              )}
            </div>
          </div>
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
                لا توجد آيات محفوظة حالياً. حدد آيات بالضغط المطول ثم اختر الحفظ.
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

        {/* ===== حفظي: نظام الحفظ المطور ===== */}
        {tab === 'tools' && (
          <div className="flex-1 overflow-y-auto space-y-4 pr-1 min-h-0">
            {/* وضع التسميع */}
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
                  memorizationTestMode
                    ? 'bg-amber-500 text-slate-950'
                    : 'bg-slate-800 text-amber-400'
                }`}
              >
                {memorizationTestMode ? (
                  <EyeOff className="w-5 h-5" />
                ) : (
                  <Eye className="w-5 h-5" />
                )}
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

            {/* طابور المراجعة */}
            <div className="p-4 rounded-2xl bg-slate-950 border border-slate-800 space-y-2.5">
              <div className="flex items-center justify-between">
                <span className="text-sm font-bold text-slate-100">مراجعة الحفظ</span>
                <span className="text-[10px] text-slate-500">
                  الأقدم حفظاً أولاً (الأقرب للنسيان)
                </span>
              </div>
              {reviewQueue.length === 0 ? (
                <p className="text-[11px] text-slate-500 leading-relaxed">
                  حدد آيات بالضغط المطول ثم اضغط «حفظ» لتعليمها محفوظة — وهتظهر هنا
                  للمراجعة الدورية بترتيب أقدم حفظ.
                </p>
              ) : (
                <div className="space-y-1.5">
                  {reviewQueue.map(({ surah, ayah, at }) => (
                    <div
                      key={`${surah}-${ayah}`}
                      className="flex items-center justify-between p-2.5 rounded-xl bg-slate-900 border border-slate-800/60"
                    >
                      <span className="text-xs font-bold text-slate-200">
                        {SURAHS_LIST.find((s) => s.number === surah)?.name ?? surah} • آية {ayah}
                      </span>
                      <span className="text-[10px] text-slate-500 font-sans">
                        حُفظت {new Date(at).toLocaleDateString('ar-EG')}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* تقدم الحفظ لكل سورة */}
            <div className="p-4 rounded-2xl bg-slate-950 border border-slate-800 space-y-2.5">
              <div className="flex items-center justify-between">
                <span className="text-sm font-bold text-slate-100">تقدم حفظي لكل سورة</span>
                <span className="text-[10px] text-slate-500">{memorization.length} سورة</span>
              </div>
              {memorization.length === 0 ? (
                <p className="text-[11px] text-slate-500 leading-relaxed">
                  لسه مفيش آيات محفوظة. من المصحف: اضغط مطولاً لتحديد الآيات المحفوظة
                  عندك ثم اضغط «حفظ» — وهيتبع تقدمك هنا آية بآية.
                </p>
              ) : (
                <div className="space-y-2">
                  {memorization
                    .sort((a, b) => b.percent - a.percent)
                    .map((m) => (
                      <div key={m.surahNumber} className="space-y-1">
                        <div className="flex items-center justify-between text-[11px]">
                          <span className="font-bold text-slate-200">
                            سورة {m.surahName}
                            {m.percent === 100 && ' 🌟'}
                          </span>
                          <span className="font-mono text-slate-400">
                            {m.memorized}/{m.total} ({m.percent}%)
                          </span>
                        </div>
                        <div className="h-1.5 rounded-full bg-slate-800 overflow-hidden">
                          <div
                            className="h-full rounded-full"
                            style={{
                              width: `${m.percent}%`,
                              background:
                                m.percent === 100
                                  ? 'linear-gradient(90deg,#10b981,#34d399)'
                                  : 'linear-gradient(90deg,#f59e0b,#fbbf24)',
                            }}
                          />
                        </div>
                      </div>
                    ))}
                </div>
              )}
            </div>

            {/* التلاوة بالنطاقات */}
            <button
              onClick={onOpenListen}
              className="w-full flex items-center gap-3 p-4 rounded-2xl bg-slate-950 border border-slate-800 hover:border-emerald-600/40 transition-colors text-right"
            >
              <span className="w-10 h-10 rounded-xl bg-slate-800 text-emerald-400 flex items-center justify-center">
                <Play className="w-5 h-5 fill-current" />
              </span>
              <span className="flex-1">
                <span className="block text-sm font-bold text-slate-100">
                  الاستماع بالنطاقات والتكرار
                </span>
                <span className="block text-[11px] text-slate-400 mt-0.5">
                  تلاوة نطاق آيات بصوت القارئ المفضل مع تكرار الحفظ
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
 * نافذة الاستماع — نطاق مخصص مع التكرار
 * ========================================================================== */
interface ListenModalProps {
  firstAyahOnPage: number;
  lastAyahOnPage: number;
  surahName: string;
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
  firstAyahOnPage,
  lastAyahOnPage,
  surahName,
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
          onClick={() => onStart(rangeStart, rangeStart, 1, 1)}
          className="w-full py-2.5 rounded-xl hover:bg-slate-800 transition-colors"
        >
          آية {rangeStart}
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
          onClick={() => onStart(rangeStart, surahAyahCount, 1, 1)}
          className="w-full py-2.5 rounded-xl hover:bg-slate-800 transition-colors"
        >
          من الآية {rangeStart} إلى آخر السورة
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
 * نافذة التفسير — لآية أو مجموعة آيات محددة
 * ========================================================================== */
const TafseerModal: React.FC<{
  ayahs: TajweedAyah[];
  surahName: string;
  edition: MushafEditionStyle;
  onClose: () => void;
  onListen: () => void;
}> = ({ ayahs, surahName, edition, onClose, onListen }) => (
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
          <span>
            التفسير الميسر • {surahName}
            {ayahs.length > 1
              ? ` (الآيات ${ayahs[0].ayahNumber}—${ayahs[ayahs.length - 1].ayahNumber})`
              : ` • آية ${ayahs[0]?.ayahNumber}`}
          </span>
        </h3>
        <button
          onClick={onClose}
          className="p-1.5 rounded-xl bg-slate-800 text-slate-400 hover:text-white"
        >
          <X className="w-4 h-4" />
        </button>
      </div>

      {ayahs.map((ayah) => (
        <div key={`${ayah.surahNumber}-${ayah.ayahNumber}`} className="space-y-2">
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

          {ayah.tafseer ? (
            <p className="text-sm text-slate-300 leading-relaxed font-sans bg-slate-950 p-4 rounded-2xl border border-slate-800/80">
              {ayah.tafseer}
            </p>
          ) : (
            <p className="text-[13px] text-slate-400 leading-relaxed font-sans bg-slate-950 p-4 rounded-2xl border border-slate-800/80 text-center">
              التفسير الميسر متاح تدريجياً — هذه الصفحة من نص المصحف الكامل المعتمد (رسم عثماني).
            </p>
          )}
        </div>
      ))}

      <button
        onClick={onListen}
        className="w-full py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold flex items-center justify-center gap-2"
      >
        <Play className="w-4 h-4 fill-current" />
        <span>الاستماع لتلاوة هذه الآيات</span>
      </button>
    </div>
  </div>
);
