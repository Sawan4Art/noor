import React, { useState, useRef, useEffect } from 'react';
import {
  Play,
  Pause,
  Download,
  CheckCircle,
  Search,
  BookOpen,
  Volume2,
  VolumeX,
  RotateCcw,
  SkipForward,
  SkipBack,
  Heart,
  Share2,
  Bookmark,
  Sparkles,
  Info,
  Trash2,
  Layers,
} from 'lucide-react';
import {
  Surah,
  Reciter,
  Ayah,
  SURAHS_LIST,
  RECITERS,
  SAMPLE_VERSES_BY_SURAH,
  getSurahAudioUrl,
  isSurahDownloaded,
  markSurahDownloaded,
} from '../utils/quranData';
import { MushafInteractiveReader } from './MushafInteractiveReader';

export const QuranSection: React.FC = () => {
  const [selectedSurah, setSelectedSurah] = useState<Surah>(SURAHS_LIST[0]);
  const [selectedReciter, setSelectedReciter] = useState<Reciter>(RECITERS[0]);
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [filterDownloadedOnly, setFilterDownloadedOnly] = useState<boolean>(false);
  const [activeTab, setActiveTab] = useState<'mushaf_page' | 'surahs_directory'>('mushaf_page');
  const [showTafseer, setShowTafseer] = useState<boolean>(true);
  const [showTranslation, setShowTranslation] = useState<boolean>(false);
  const [quranFontSize, setQuranFontSize] = useState<number>(24);

  // Audio Player State
  const [isPlaying, setIsPlaying] = useState<boolean>(false);
  const [currentTime, setCurrentTime] = useState<number>(0);
  const [duration, setDuration] = useState<number>(0);
  const [audioLoading, setAudioLoading] = useState<boolean>(false);

  // Download simulation progress state
  const [downloadProgress, setDownloadProgress] = useState<Record<string, number>>({});
  const [downloadSuccessToast, setDownloadSuccessToast] = useState<string | null>(null);

  const audioRef = useRef<HTMLAudioElement | null>(null);

  // Get current audio URL
  const currentAudioUrl = getSurahAudioUrl(selectedReciter.serverUrl, selectedSurah.number);
  const isDownloaded = isSurahDownloaded(selectedReciter.id, selectedSurah.number);

  // Filter surahs
  const filteredSurahs = SURAHS_LIST.filter((s) => {
    const matchesSearch =
      s.name.includes(searchQuery) ||
      s.englishName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      s.number.toString() === searchQuery;
    if (!matchesSearch) return false;
    if (filterDownloadedOnly) {
      return isSurahDownloaded(selectedReciter.id, s.number);
    }
    return true;
  });

  // Handle Play/Pause
  const togglePlay = () => {
    if (!audioRef.current) return;
    if (isPlaying) {
      audioRef.current.pause();
      setIsPlaying(false);
    } else {
      setAudioLoading(true);
      audioRef.current
        .play()
        .then(() => {
          setIsPlaying(true);
          setAudioLoading(false);
        })
        .catch(() => {
          setIsPlaying(false);
          setAudioLoading(false);
        });
    }
  };

  // Change Surah
  const selectSurah = (surah: Surah) => {
    setSelectedSurah(surah);
    setIsPlaying(false);
    setCurrentTime(0);
    if (audioRef.current) {
      audioRef.current.pause();
      audioRef.current.src = getSurahAudioUrl(selectedReciter.serverUrl, surah.number);
      audioRef.current.load();
    }
  };

  // Switch Reciter
  const changeReciter = (reciter: Reciter) => {
    setSelectedReciter(reciter);
    setIsPlaying(false);
    if (audioRef.current) {
      audioRef.current.pause();
      audioRef.current.src = getSurahAudioUrl(reciter.serverUrl, selectedSurah.number);
      audioRef.current.load();
    }
  };

  // Trigger Download
  const handleDownloadSurah = (surahNumber: number) => {
    const key = `${selectedReciter.id}_${surahNumber}`;
    setDownloadProgress((prev) => ({ ...prev, [key]: 10 }));

    const interval = setInterval(() => {
      setDownloadProgress((prev) => {
        const current = prev[key] || 10;
        if (current >= 100) {
          clearInterval(interval);
          markSurahDownloaded(selectedReciter.id, surahNumber, true);
          const sName = SURAHS_LIST.find((s) => s.number === surahNumber)?.name || 'السورة';
          setDownloadSuccessToast(`تم تحميل ${sName} بنجاح للاستماع أوفلاين بصوت ${selectedReciter.name}`);
          setTimeout(() => setDownloadSuccessToast(null), 4000);
          return { ...prev, [key]: 100 };
        }
        return { ...prev, [key]: current + 25 };
      });
    }, 250);
  };

  const handleDeleteDownload = (surahNumber: number) => {
    markSurahDownloaded(selectedReciter.id, surahNumber, false);
    const key = `${selectedReciter.id}_${surahNumber}`;
    setDownloadProgress((prev) => {
      const copy = { ...prev };
      delete copy[key];
      return copy;
    });
  };

  // Format seconds to mm:ss
  const formatTime = (sec: number) => {
    if (isNaN(sec) || sec === 0) return '00:00';
    const m = Math.floor(sec / 60);
    const s = Math.floor(sec % 60);
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  // Verses to display
  const ayahs = SAMPLE_VERSES_BY_SURAH[selectedSurah.number] || [
    {
      numberInSurah: 1,
      number: 1,
      text: 'بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ',
      translation: 'In the name of Allah, the Entirely Merciful, the Especially Merciful.',
      tafseer: 'ابتدأ الله كتابه العزيز بالبسملة تيمناً وتعليماً للعباد أن يفتتحوا أقوالهم وأعمالهم باسمه تعالى.',
    },
    {
      numberInSurah: 2,
      number: 2,
      text: `تلاوة وقراءة مباركة لسورة ${selectedSurah.name} كاملة (${selectedSurah.numberOfAyahs} آية، ${selectedSurah.revelationTypeAr}).`,
      translation: `Recitation and reading of Surah ${selectedSurah.englishName} (${selectedSurah.numberOfAyahs} verses).`,
      tafseer: `سورة ${selectedSurah.name} من سور القرآن الكريم (${selectedSurah.revelationTypeAr})، ترتيبها في المصحف رقم ${selectedSurah.number}. استمع لتلاوتها العطرة من القارئ المختار في المشغل بالأسفل.`,
    },
  ];

  return (
    <div className="space-y-6">
      {/* Download Alert Toast */}
      {downloadSuccessToast && (
        <div className="p-3 bg-emerald-600 text-white rounded-xl shadow-xl flex items-center justify-between text-xs sm:text-sm animate-in fade-in slide-in-from-top-2">
          <div className="flex items-center gap-2">
            <CheckCircle className="w-5 h-5 text-emerald-200" />
            <span>{downloadSuccessToast}</span>
          </div>
          <button
            onClick={() => setDownloadSuccessToast(null)}
            className="text-xs text-emerald-200 hover:text-white px-2 py-0.5"
          >
            إغلاق
          </button>
        </div>
      )}

      {/* Main Quran View Switcher: Interactive Mushaf vs Surah Directory */}
      <div className="flex items-center justify-center">
        <div className="inline-flex items-center p-1.5 rounded-2xl bg-slate-900 border border-slate-800 shadow-xl gap-2">
          <button
            onClick={() => setActiveTab('mushaf_page')}
            className={`flex items-center gap-2 px-5 py-2.5 rounded-xl text-xs sm:text-sm font-bold transition-all ${
              activeTab === 'mushaf_page'
                ? 'bg-gradient-to-r from-amber-600 via-amber-500 to-yellow-600 text-slate-950 shadow-lg shadow-amber-950/60 ring-1 ring-amber-400/40'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <BookOpen className="w-4 h-4" />
            <span>المصحف بالرسم العثماني الملون (تفاعلي)</span>
          </button>

          <button
            onClick={() => setActiveTab('surahs_directory')}
            className={`flex items-center gap-2 px-5 py-2.5 rounded-xl text-xs sm:text-sm font-bold transition-all ${
              activeTab === 'surahs_directory'
                ? 'bg-emerald-600 text-white shadow-lg shadow-emerald-950/60'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <Download className="w-4 h-4" />
            <span>فهرس السور وتنزيل التلاوات (114 سورة)</span>
          </button>
        </div>
      </div>

      {/* If Mushaf Page is active -> Render the authentic Tajweed Mushaf with radial menu */}
      {activeTab === 'mushaf_page' && <MushafInteractiveReader />}

      {/* If Surahs Directory is active -> Render the full catalog, search & batch downloads */}
      {activeTab === 'surahs_directory' && (
        <div className="space-y-6">
          {/* Top Header & Reciters Bar */}
          <div className="p-5 rounded-3xl bg-slate-900/90 border border-slate-800 shadow-xl">
            <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
              <div>
                <h2 className="text-xl font-bold text-white flex items-center gap-2">
                  <BookOpen className="w-5 h-5 text-emerald-400" />
                  <span>فهرس السور وقراء القرآن الكريم</span>
                </h2>
                <p className="text-xs text-slate-400 mt-0.5">
                  تلاوة عذبة مع كبار القراء وتنزيل السور للاستماع دون إنترنت
                </p>
              </div>
            </div>

            {/* Reciters Carousel / Picker */}
            <div className="mt-5 pt-4 border-t border-slate-800/80">
              <label className="block text-xs text-slate-400 mb-2 font-medium">
                اختر القارئ المفضل للاستماع والتنزيل:
              </label>
              <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-2.5">
                {RECITERS.map((reciter) => {
                  const isSelected = selectedReciter.id === reciter.id;
                  return (
                    <button
                      key={reciter.id}
                      onClick={() => changeReciter(reciter)}
                      className={`flex flex-col items-center p-2.5 rounded-2xl border text-center transition-all ${
                        isSelected
                          ? 'bg-emerald-950/80 border-emerald-500 ring-1 ring-emerald-400/50 shadow-md'
                          : 'bg-slate-950/60 border-slate-800/80 hover:border-slate-700 hover:bg-slate-950'
                      }`}
                    >
                      <img
                        src={reciter.photoUrl}
                        alt={reciter.name}
                        className="w-12 h-12 rounded-full object-cover mb-2 border border-emerald-500/30"
                      />
                      <span className="text-xs font-bold text-slate-200 line-clamp-1">{reciter.name}</span>
                      <span className="text-[10px] text-slate-400 mt-0.5 line-clamp-1">{reciter.style}</span>
                    </button>
                  );
                })}
              </div>
            </div>
          </div>

          {/* Main Grid: Surahs List & Reader/Player */}
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
            {/* Left Column (Surahs Directory) */}
            <div className="lg:col-span-4 space-y-3">
              <div className="p-4 rounded-2xl bg-slate-900/90 border border-slate-800">
                {/* Search Input */}
                <div className="relative mb-3">
                  <input
                    type="text"
                    placeholder="ابحث باسم السورة أو رقمها..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-800 text-xs text-slate-200 rounded-xl pl-8 pr-3 py-2.5 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                  />
                  <Search className="w-4 h-4 text-slate-500 absolute left-2.5 top-3" />
                </div>

                {/* Offline filter toggle */}
                <div className="flex items-center justify-between py-1 mb-2">
                  <span className="text-xs text-slate-400">
                    عرض {filteredSurahs.length} سورة
                  </span>
                  <button
                    onClick={() => setFilterDownloadedOnly(!filterDownloadedOnly)}
                    className={`text-[11px] px-2.5 py-1 rounded-lg border transition-colors ${
                      filterDownloadedOnly
                        ? 'bg-emerald-500/20 text-emerald-300 border-emerald-500/40'
                        : 'bg-slate-950 text-slate-400 border-slate-800 hover:text-white'
                    }`}
                  >
                    {filterDownloadedOnly ? '✓ المحملة فقط' : 'المحملة أوفلاين'}
                  </button>
                </div>

                {/* Scrollable Surahs list */}
                <div className="max-h-[460px] overflow-y-auto space-y-1.5 pr-1">
                  {filteredSurahs.map((surah) => {
                    const isSelected = selectedSurah.number === surah.number;
                    const downloaded = isSurahDownloaded(selectedReciter.id, surah.number);
                    const progKey = `${selectedReciter.id}_${surah.number}`;
                    const prog = downloadProgress[progKey];

                    return (
                      <div
                        key={surah.number}
                        className={`group flex items-center justify-between p-2.5 rounded-xl border transition-all cursor-pointer ${
                          isSelected
                            ? 'bg-emerald-950/80 border-emerald-500/80 text-white'
                            : 'bg-slate-950/50 border-slate-800/60 hover:bg-slate-950 hover:border-slate-700'
                        }`}
                        onClick={() => selectSurah(surah)}
                      >
                        <div className="flex items-center gap-3">
                          <div
                            className={`w-7 h-7 rounded-lg flex items-center justify-center text-xs font-bold ${
                              isSelected
                                ? 'bg-emerald-500 text-slate-950'
                                : 'bg-slate-800 text-slate-300'
                            }`}
                          >
                            {surah.number}
                          </div>
                          <div>
                            <div className="text-sm font-bold flex items-center gap-1.5">
                              <span>{surah.name}</span>
                              {downloaded && (
                                <span className="text-[10px] px-1.5 py-0.2 rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                                  محمّلة
                                </span>
                              )}
                            </div>
                            <div className="text-[10px] text-slate-400">
                              {surah.revelationTypeAr} • {surah.numberOfAyahs} آيات
                            </div>
                          </div>
                        </div>

                        {/* Download / Status button */}
                        <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
                          {prog !== undefined && prog < 100 ? (
                            <div className="text-[10px] font-mono text-emerald-400 animate-pulse">
                              %{prog}
                            </div>
                          ) : downloaded ? (
                            <button
                              onClick={() => handleDeleteDownload(surah.number)}
                              className="p-1 text-slate-500 hover:text-rose-400"
                              title="حذف الملف المحلي"
                            >
                              <CheckCircle className="w-4 h-4 text-emerald-400" />
                            </button>
                          ) : (
                            <button
                              onClick={() => handleDownloadSurah(surah.number)}
                              className="p-1 text-slate-500 hover:text-emerald-400"
                              title="تنزيل السورة للاستماع بدون إنترنت"
                            >
                              <Download className="w-4 h-4" />
                            </button>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>

            {/* Right Column: Quran Reader & Continuous Audio Player */}
            <div className="lg:col-span-8 space-y-4">
              {/* Surah Header Card */}
              <div className="p-5 rounded-3xl bg-gradient-to-br from-emerald-950/70 via-slate-900 to-teal-950/70 border border-emerald-800/40">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                  <div>
                    <div className="inline-flex items-center gap-2 px-2.5 py-0.5 rounded-full bg-emerald-500/10 text-emerald-300 text-xs border border-emerald-500/30 mb-2">
                      <span>سورة رقم {selectedSurah.number}</span>
                      <span>•</span>
                      <span>{selectedSurah.revelationTypeAr}</span>
                      <span>•</span>
                      <span>{selectedSurah.numberOfAyahs} آيات</span>
                    </div>
                    <h3 className="text-3xl font-extrabold text-white">
                      سورة {selectedSurah.name}
                    </h3>
                    <p className="text-xs text-slate-400 mt-1">
                      القارئ: <span className="text-emerald-300 font-semibold">{selectedReciter.name}</span>
                    </p>
                  </div>

                  {/* Controls: Tafseer & Font size */}
                  <div className="flex flex-wrap items-center gap-2">
                    <button
                      onClick={() => setShowTafseer(!showTafseer)}
                      className={`px-3 py-1.5 rounded-xl border text-xs font-medium transition-colors ${
                        showTafseer
                          ? 'bg-emerald-500/20 text-emerald-300 border-emerald-500/40'
                          : 'bg-slate-900 text-slate-400 border-slate-800'
                      }`}
                    >
                      {showTafseer ? '✓ إخفاء التفسير' : 'إظهار التفسير'}
                    </button>

                    <button
                      onClick={() => setShowTranslation(!showTranslation)}
                      className={`px-3 py-1.5 rounded-xl border text-xs font-medium transition-colors ${
                        showTranslation
                          ? 'bg-teal-500/20 text-teal-300 border-teal-500/40'
                          : 'bg-slate-900 text-slate-400 border-slate-800'
                      }`}
                    >
                      {showTranslation ? '✓ الإنجليزية' : 'English'}
                    </button>

                    <div className="flex items-center bg-slate-950 rounded-xl border border-slate-800 px-2 py-1">
                      <button
                        onClick={() => setQuranFontSize((p) => Math.max(18, p - 2))}
                        className="text-xs px-1.5 py-0.5 text-slate-400 hover:text-white"
                        title="تصغير الخط"
                      >
                        أ-
                      </button>
                      <span className="text-xs px-1 text-slate-300">{quranFontSize}px</span>
                      <button
                        onClick={() => setQuranFontSize((p) => Math.min(36, p + 2))}
                        className="text-xs px-1.5 py-0.5 text-slate-400 hover:text-white"
                        title="تكبير الخط"
                      >
                        أ+
                      </button>
                    </div>
                  </div>
                </div>
              </div>

              {/* Reading Ayahs Surface */}
              <div className="p-6 rounded-3xl bg-slate-900/90 border border-slate-800 shadow-xl space-y-6">
                {/* Bismillah Frame */}
                {selectedSurah.number !== 9 && (
                  <div className="text-center py-4 border-b border-slate-800/80">
                    <p className="font-quran text-2xl sm:text-3xl text-emerald-400 font-bold leading-relaxed">
                      بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ
                    </p>
                  </div>
                )}

                {/* Verses List */}
                <div className="space-y-6">
                  {ayahs.map((ayah) => (
                    <div
                      key={ayah.numberInSurah}
                      className="p-4 rounded-2xl bg-slate-950/60 border border-slate-800/80 hover:border-emerald-600/40 transition-colors"
                    >
                      <div className="flex items-start justify-between gap-4">
                        <p
                          className="font-quran text-white text-right leading-loose flex-1"
                          style={{ fontSize: `${quranFontSize}px` }}
                        >
                          {ayah.text}{' '}
                          <span className="inline-block text-emerald-400 text-sm font-sans px-2 py-0.5 rounded-full bg-emerald-500/10 border border-emerald-500/30 mx-1">
                            ﴿{ayah.numberInSurah}﴾
                          </span>
                        </p>
                      </div>

                      {/* Tafseer Box */}
                      {showTafseer && (
                        <div className="mt-3 pt-3 border-t border-slate-800/70 text-right">
                          <span className="text-[11px] font-semibold text-emerald-400 ml-1">
                            تفسير الآية (الميسر):
                          </span>
                          <p className="text-xs text-slate-300 mt-1 leading-relaxed">
                            {ayah.tafseer}
                          </p>
                        </div>
                      )}

                      {/* Translation Box */}
                      {showTranslation && (
                        <div className="mt-2 text-left dir-ltr">
                          <p className="text-xs text-slate-400 italic">
                            {ayah.translation}
                          </p>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </div>

              {/* Persistent Floating Audio Player Bar */}
              <div className="p-4 rounded-2xl bg-gradient-to-r from-slate-950 via-emerald-950/80 to-slate-950 border border-emerald-600/40 shadow-2xl">
                <audio
                  ref={audioRef}
                  src={currentAudioUrl}
                  onTimeUpdate={() => {
                    if (audioRef.current) {
                      setCurrentTime(audioRef.current.currentTime);
                      setDuration(audioRef.current.duration || 0);
                    }
                  }}
                  onLoadedMetadata={() => {
                    if (audioRef.current) {
                      setDuration(audioRef.current.duration || 0);
                    }
                  }}
                  onEnded={() => {
                    setIsPlaying(false);
                  }}
                />

                <div className="flex flex-col sm:flex-row items-center justify-between gap-3">
                  {/* Surah & Reciter info */}
                  <div className="flex items-center gap-3 w-full sm:w-auto">
                    <button
                      onClick={togglePlay}
                      className="w-12 h-12 rounded-2xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 flex items-center justify-center shrink-0 shadow-lg shadow-emerald-950/60 transition-transform active:scale-95"
                      aria-label={isPlaying ? 'إيقاف مؤقت' : 'تشغيل التلاوة'}
                    >
                      {audioLoading ? (
                        <div className="w-5 h-5 border-2 border-slate-950 border-t-transparent rounded-full animate-spin" />
                      ) : isPlaying ? (
                        <Pause className="w-5 h-5 fill-slate-950" />
                      ) : (
                        <Play className="w-5 h-5 fill-slate-950 ml-0.5" />
                      )}
                    </button>

                    <div>
                      <h4 className="text-sm font-bold text-white flex items-center gap-2">
                        <span>سورة {selectedSurah.name}</span>
                        {isDownloaded && (
                          <span className="text-[10px] px-1.5 rounded bg-emerald-500/20 text-emerald-300">
                            متاح أوفلاين
                          </span>
                        )}
                      </h4>
                      <p className="text-xs text-slate-400">
                        بصوت: {selectedReciter.name}
                      </p>
                    </div>
                  </div>

                  {/* Scrubber & Duration */}
                  <div className="flex-1 w-full max-w-md flex items-center gap-3">
                    <span className="text-[11px] font-mono text-slate-400 min-w-10 text-left">
                      {formatTime(currentTime)}
                    </span>
                    <input
                      type="range"
                      min={0}
                      max={duration || 100}
                      value={currentTime}
                      onChange={(e) => {
                        const newTime = Number(e.target.value);
                        setCurrentTime(newTime);
                        if (audioRef.current) {
                          audioRef.current.currentTime = newTime;
                        }
                      }}
                      className="w-full h-1.5 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-emerald-500"
                    />
                    <span className="text-[11px] font-mono text-slate-400 min-w-10 text-right">
                      {formatTime(duration)}
                    </span>
                  </div>

                  {/* Actions: Download Current Surah */}
                  <div className="flex items-center gap-2 w-full sm:w-auto justify-end">
                    {!isDownloaded ? (
                      <button
                        onClick={() => handleDownloadSurah(selectedSurah.number)}
                        className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 text-xs font-medium hover:bg-emerald-500/30 transition-colors"
                      >
                        <Download className="w-3.5 h-3.5" />
                        <span>تنزيل السورة</span>
                      </button>
                    ) : (
                      <span className="flex items-center gap-1 text-xs text-emerald-400 font-medium">
                        <CheckCircle className="w-4 h-4" />
                        <span>محملة في الذاكرة</span>
                      </span>
                    )}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

