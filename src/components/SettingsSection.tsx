import React, { useState } from 'react';
import {
  Palette,
  Sun,
  Moon,
  BookOpen,
  Mic2,
  Download,
  CheckCircle,
  Search,
  Clock,
  RefreshCw,
  ExternalLink,
  MessageSquareHeart,
  Smartphone,
  Info,
  Trash2,
  Plus,
  Minus,
  RotateCcw,
  X,
  Check,
  HeartHandshake,
  Send,
  Copy as CopyIcon,
} from 'lucide-react';
import {
  APP_THEMES,
  AppTheme,
  ColorMode,
} from '../utils/themeData';
import {
  MUSHAF_EDITIONS,
  MushafEdition,
  getSavedEdition,
  saveEdition,
  getSavedPrayerAdjustments,
  savePrayerAdjustments,
  ADJUSTABLE_PRAYERS,
} from '../utils/appSettings';
import {
  RECITERS,
  Reciter,
  SURAHS_LIST,
  getSurahAudioUrl,
  isSurahDownloaded,
  markSurahDownloaded,
} from '../utils/quranData';
import {
  APP_VERSION,
  RELEASES_PAGE_URL,
  fetchLatestRelease,
  compareVersions,
  detectApkEnvironment,
} from '../utils/otaUpdate';
import { PrayerTimeAdjustments } from '../utils/prayerMath';

/**
 * قسم الإعدادات — المركز الكامل لإدارة التطبيق (بطلب المالك 2026-10):
 * المظهر والألوان • طبعة المصحف • القارئ وتنزيل التلاوات •
 * ضبط المواقيت الدقيق • التحقق من التحديثات • الشكاوى والمقترحات •
 * استوديو الويدجتس • عن التطبيق.
 */

interface SettingsSectionProps {
  currentTheme: AppTheme;
  setCurrentTheme: (theme: AppTheme) => void;
  colorMode: ColorMode;
  setColorMode: (mode: ColorMode) => void;
  onOpenWidgets: () => void;
}

/** بطاقة قسم إعدادات موحدة */
const SettingsCard: React.FC<{
  icon: React.ReactNode;
  title: string;
  subtitle?: string;
  children: React.ReactNode;
}> = ({ icon, title, subtitle, children }) => (
  <div
    className="rounded-3xl border backdrop-blur-xl overflow-hidden shadow-xl"
    style={{
      backgroundColor: 'color-mix(in srgb, var(--noor-n-900) 78%, transparent)',
      borderColor: 'color-mix(in srgb, var(--noor-p-500) 22%, transparent)',
    }}
  >
    <div
      className="flex items-center gap-3 px-5 py-4 border-b"
      style={{ borderColor: 'color-mix(in srgb, var(--noor-p-500) 18%, transparent)' }}
    >
      <span
        className="w-10 h-10 rounded-2xl flex items-center justify-center text-white shadow-md shrink-0"
        style={{
          background: 'linear-gradient(135deg, var(--noor-p-500), var(--noor-a-500))',
        }}
      >
        {icon}
      </span>
      <div className="min-w-0">
        <h2 className="text-sm font-extrabold" style={{ color: 'var(--noor-n-50)' }}>
          {title}
        </h2>
        {subtitle && (
          <p className="text-[11px] mt-0.5 truncate" style={{ color: 'var(--noor-n-400)' }}>
            {subtitle}
          </p>
        )}
      </div>
    </div>
    <div className="p-4 sm:p-5 space-y-4">{children}</div>
  </div>
);

export const SettingsSection: React.FC<SettingsSectionProps> = ({
  currentTheme,
  setCurrentTheme,
  colorMode,
  setColorMode,
  onOpenWidgets,
}) => {
  const isDark = colorMode === 'dark';

  // ===== طبعة المصحف =====
  const [edition, setEdition] = useState<MushafEdition>(getSavedEdition().id);
  const handleEditionChange = (id: MushafEdition) => {
    setEdition(id);
    saveEdition(id);
  };

  // ===== القارئ والتنزيلات =====
  const [selectedReciter, setSelectedReciter] = useState<Reciter>(RECITERS[0]);
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [filterDownloadedOnly, setFilterDownloadedOnly] = useState<boolean>(false);
  const [downloadProgress, setDownloadProgress] = useState<Record<string, number>>({});
  const [downloadToast, setDownloadToast] = useState<string | null>(null);

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
          setDownloadToast(`تم تحميل ${sName} بنجاح — متاحة أوفلاين بصوت ${selectedReciter.name}`);
          setTimeout(() => setDownloadToast(null), 4000);
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

  const filteredSurahs = SURAHS_LIST.filter((s) => {
    const matches =
      s.name.includes(searchQuery) ||
      s.englishName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      s.number.toString() === searchQuery;
    if (!matches) return false;
    if (filterDownloadedOnly) return isSurahDownloaded(selectedReciter.id, s.number);
    return true;
  });

  // ===== ضبط المواقيت الدقيق =====
  const [adjustments, setAdjustments] = useState<PrayerTimeAdjustments>(getSavedPrayerAdjustments);
  const updateAdjustment = (key: string, delta: number) => {
    setAdjustments((prev) => {
      const current = prev[key as keyof PrayerTimeAdjustments] ?? 0;
      const next = Math.max(-30, Math.min(30, current + delta));
      const updated = { ...prev, [key]: next };
      savePrayerAdjustments(updated);
      return updated;
    });
  };
  const resetAdjustments = () => {
    setAdjustments({});
    savePrayerAdjustments({});
  };

  // ===== التحقق من التحديثات =====
  const [updateChecking, setUpdateChecking] = useState<boolean>(false);
  const [updateResult, setUpdateResult] = useState<
    { status: 'latest' | 'available' | 'error'; message: string; apkUrl?: string | null } | null
  >(null);

  const handleCheckUpdate = async () => {
    setUpdateChecking(true);
    setUpdateResult(null);
    try {
      const release = await fetchLatestRelease();
      if (!release) {
        setUpdateResult({
          status: 'error',
          message: 'تعذر الوصول لصفحة الإصدارات الآن — تأكد من اتصال الإنترنت وحاول مجدداً.',
        });
      } else if (compareVersions(release.version, APP_VERSION) > 0) {
        setUpdateResult({
          status: 'available',
          message: `تحديث جديد متاح: الإصدار ${release.version} (الحالي ${APP_VERSION}).`,
          apkUrl: release.apkUrl,
        });
      } else {
        setUpdateResult({
          status: 'latest',
          message: `أنت على أحدث إصدار (${APP_VERSION}) — جزاك الله خيراً.`,
        });
      }
    } catch {
      setUpdateResult({ status: 'error', message: 'حدث خطأ غير متوقع أثناء فحص التحديث.' });
    } finally {
      setUpdateChecking(false);
    }
  };

  // ===== الشكاوى والمقترحات =====
  const [showFeedbackModal, setShowFeedbackModal] = useState<boolean>(false);
  const [feedbackText, setFeedbackText] = useState<string>('');
  const [feedbackCopied, setFeedbackCopied] = useState<boolean>(false);

  const feedbackIssueUrl = () => {
    const base = 'https://github.com/Sawan4Art/noor/issues/new';
    const title = encodeURIComponent('شكوى / مقترح — تطبيق نور المسلم');
    const body = encodeURIComponent(
      `${feedbackText.trim()}\n\n---\nإصدار التطبيق: ${APP_VERSION}\n${detectApkEnvironment() ? 'من داخل تطبيق أندرويد' : 'من نسخة الويب'}`
    );
    return `${base}?title=${title}&body=${body}`;
  };

  const isApk = detectApkEnvironment();

  return (
    <div className="max-w-3xl mx-auto space-y-5 animate-in fade-in duration-300">
      {/* Toast التنزيلات */}
      {downloadToast && (
        <div className="fixed top-4 left-1/2 -translate-x-1/2 z-50 p-3 rounded-2xl bg-emerald-600 text-white shadow-2xl flex items-center gap-2 text-xs animate-in fade-in slide-in-from-top-2">
          <CheckCircle className="w-4 h-4" />
          <span>{downloadToast}</span>
          <button onClick={() => setDownloadToast(null)} className="text-emerald-200 px-1">
            إغلاق
          </button>
        </div>
      )}

      {/* ===================== المظهر والألوان ===================== */}
      <SettingsCard
        icon={<Palette className="w-5 h-5" />}
        title="المظهر والألوان"
        subtitle="لون التطبيق ووضع الإضاءة"
      >
        {/* ألوان بسيطة بدون شروحات */}
        <div className="grid grid-cols-3 sm:grid-cols-6 gap-2.5">
          {APP_THEMES.map((t) => {
            const isSelected = currentTheme.id === t.id;
            return (
              <button
                key={t.id}
                onClick={() => setCurrentTheme(t)}
                className={`flex flex-col items-center gap-2 p-3 rounded-2xl border transition-all ${
                  isSelected ? 'ring-2' : 'hover:brightness-110'
                }`}
                style={{
                  borderColor: isSelected ? t.primaryColor : 'color-mix(in srgb, var(--noor-n-700) 60%, transparent)',
                  backgroundColor: isSelected
                    ? `color-mix(in srgb, ${t.primaryColor} 15%, transparent)`
                    : 'color-mix(in srgb, var(--noor-n-950) 50%, transparent)',
                  ...(isSelected ? { ['--tw-ring-color' as string]: `${t.primaryColor}66` } : {}),
                }}
                title={t.name}
              >
                <span
                  className="w-10 h-10 rounded-2xl shadow-md flex items-center justify-center"
                  style={{ background: `linear-gradient(135deg, ${t.primaryColor}, ${t.accentColor})` }}
                >
                  {isSelected && <Check className="w-5 h-5 text-white drop-shadow" />}
                </span>
                <span className="text-xs font-bold" style={{ color: 'var(--noor-n-100)' }}>
                  {t.name}
                </span>
              </button>
            );
          })}
        </div>

        {/* فاتح / مظلم بأيقونة */}
        <div className="flex items-center justify-between p-3 rounded-2xl border" style={{ borderColor: 'color-mix(in srgb, var(--noor-p-500) 20%, transparent)' }}>
          <div>
            <div className="text-xs font-bold" style={{ color: 'var(--noor-n-100)' }}>
              وضع الإضاءة
            </div>
            <div className="text-[11px] mt-0.5" style={{ color: 'var(--noor-n-400)' }}>
              {isDark ? 'الوضع المظلم مفعّل — مريح للعين في الإضاءة الخافتة' : 'الوضع الفاتح مفعّل — وضوح عالي'}
            </div>
          </div>
          <button
            onClick={() => setColorMode(isDark ? 'light' : 'dark')}
            className="w-12 h-12 rounded-2xl flex items-center justify-center text-white shadow-lg transition-transform active:scale-90"
            style={{
              background: isDark
                ? 'linear-gradient(135deg, var(--noor-p-500), var(--noor-a-500))'
                : 'linear-gradient(135deg, #f59e0b, #fbbf24)',
            }}
            title={isDark ? 'التبديل إلى الوضع الفاتح' : 'التبديل إلى الوضع المظلم'}
            aria-label="تبديل وضع الإضاءة"
          >
            {isDark ? <Moon className="w-5 h-5" /> : <Sun className="w-5 h-5 text-amber-900" />}
          </button>
        </div>
      </SettingsCard>

      {/* ===================== طبعة المصحف ===================== */}
      <SettingsCard
        icon={<BookOpen className="w-5 h-5" />}
        title="طبعة المصحف"
        subtitle="تتغير فوراً في تبويب المصحف"
      >
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          {(Object.values(MUSHAF_EDITIONS) as Array<(typeof MUSHAF_EDITIONS)[MushafEdition]>).map((ed) => {
            const isSelected = edition === ed.id;
            return (
              <button
                key={ed.id}
                onClick={() => handleEditionChange(ed.id)}
                className={`p-3 rounded-2xl border-2 transition-all text-right space-y-2 ${
                  isSelected ? 'shadow-lg' : 'hover:brightness-110'
                }`}
                style={{
                  borderColor: isSelected ? 'var(--noor-p-500)' : 'color-mix(in srgb, var(--noor-n-700) 60%, transparent)',
                  backgroundColor: 'color-mix(in srgb, var(--noor-n-950) 50%, transparent)',
                }}
              >
                {/* معاينة مصغرة للورق */}
                <div
                  className="h-16 rounded-xl border-2 flex items-center justify-center"
                  style={{ backgroundColor: ed.paper, borderColor: ed.frame }}
                >
                  <span
                    className="font-quran text-sm"
                    style={{ color: ed.showTajweed ? '#db2777' : ed.ink }}
                  >
                    ﷽
                  </span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold" style={{ color: 'var(--noor-n-100)' }}>
                    {ed.label}
                  </span>
                  {isSelected && <Check className="w-4 h-4" style={{ color: 'var(--noor-p-400)' }} />}
                </div>
                <span className="block text-[10px]" style={{ color: 'var(--noor-n-400)' }}>
                  {ed.description}
                </span>
              </button>
            );
          })}
        </div>
      </SettingsCard>

      {/* ===================== القارئ والتلاوات ===================== */}
      <SettingsCard
        icon={<Mic2 className="w-5 h-5" />}
        title="القارئ وتنزيل التلاوات"
        subtitle="اختر صوت القارئ المفضل ونزّل السور للاستماع بدون إنترنت"
      >
        {/* اختيار القارئ — أسماء فقط بدون صور */}
        <div className="space-y-2">
          <span className="text-xs font-bold" style={{ color: 'var(--noor-n-300)' }}>
            صوت القارئ:
          </span>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
            {RECITERS.map((reciter) => {
              const isSelected = selectedReciter.id === reciter.id;
              return (
                <button
                  key={reciter.id}
                  onClick={() => setSelectedReciter(reciter)}
                  className={`p-2.5 rounded-2xl border text-center transition-all ${
                    isSelected ? 'ring-1' : 'hover:brightness-110'
                  }`}
                  style={{
                    borderColor: isSelected
                      ? 'var(--noor-p-500)'
                      : 'color-mix(in srgb, var(--noor-n-700) 60%, transparent)',
                    backgroundColor: isSelected
                      ? 'color-mix(in srgb, var(--noor-p-500) 16%, transparent)'
                      : 'color-mix(in srgb, var(--noor-n-950) 50%, transparent)',
                  }}
                  title={reciter.style}
                >
                  <div className="text-xs font-bold truncate" style={{ color: 'var(--noor-n-100)' }}>
                    {reciter.name}
                  </div>
                  <div className="text-[10px] mt-0.5 truncate" style={{ color: 'var(--noor-n-400)' }}>
                    {reciter.country}
                  </div>
                </button>
              );
            })}
          </div>
        </div>

        {/* البحث والفلترة */}
        <div className="flex flex-col sm:flex-row gap-2">
          <div className="relative flex-1">
            <input
              type="text"
              placeholder="ابحث باسم السورة أو رقمها..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full rounded-2xl border px-4 py-2.5 text-xs focus:outline-none focus:ring-1"
              style={{
                backgroundColor: 'color-mix(in srgb, var(--noor-n-950) 70%, transparent)',
                borderColor: 'color-mix(in srgb, var(--noor-p-500) 20%, transparent)',
                color: 'var(--noor-n-100)',
              }}
            />
            <Search className="w-4 h-4 absolute left-3 top-3" style={{ color: 'var(--noor-n-400)' }} />
          </div>
          <button
            onClick={() => setFilterDownloadedOnly(!filterDownloadedOnly)}
            className="px-4 py-2.5 rounded-2xl border text-xs font-bold transition-colors"
            style={{
              backgroundColor: filterDownloadedOnly
                ? 'color-mix(in srgb, var(--noor-p-500) 20%, transparent)'
                : 'transparent',
              borderColor: 'color-mix(in srgb, var(--noor-p-500) 30%, transparent)',
              color: filterDownloadedOnly ? 'var(--noor-p-300)' : 'var(--noor-n-300)',
            }}
          >
            {filterDownloadedOnly ? '✓ المحملة فقط' : 'المحملة أوفلاين'}
          </button>
        </div>

        {/* قائمة السور للتنزيل */}
        <div className="max-h-72 overflow-y-auto space-y-1.5 pl-1" dir="rtl">
          {filteredSurahs.map((surah) => {
            const downloaded = isSurahDownloaded(selectedReciter.id, surah.number);
            const progKey = `${selectedReciter.id}_${surah.number}`;
            const prog = downloadProgress[progKey];
            return (
              <div
                key={surah.number}
                className="flex items-center justify-between p-2.5 rounded-2xl border"
                style={{
                  backgroundColor: 'color-mix(in srgb, var(--noor-n-950) 60%, transparent)',
                  borderColor: 'color-mix(in srgb, var(--noor-n-700) 50%, transparent)',
                }}
              >
                <div className="flex items-center gap-3">
                  <span
                    className="w-7 h-7 rounded-lg flex items-center justify-center text-xs font-bold shrink-0"
                    style={{
                      backgroundColor: 'color-mix(in srgb, var(--noor-n-800) 70%, transparent)',
                      color: 'var(--noor-n-200)',
                    }}
                  >
                    {surah.number}
                  </span>
                  <div>
                    <div className="text-sm font-bold flex items-center gap-1.5" style={{ color: 'var(--noor-n-100)' }}>
                      <span>{surah.name}</span>
                      {downloaded && (
                        <span
                          className="text-[9px] px-1.5 py-0.5 rounded"
                          style={{
                            backgroundColor: 'color-mix(in srgb, var(--noor-p-500) 18%, transparent)',
                            color: 'var(--noor-p-300)',
                          }}
                        >
                          محمّلة
                        </span>
                      )}
                    </div>
                    <div className="text-[10px]" style={{ color: 'var(--noor-n-400)' }}>
                      {surah.revelationTypeAr} • {surah.numberOfAyahs} آيات
                    </div>
                  </div>
                </div>

                {prog !== undefined && prog < 100 ? (
                  <span className="text-[10px] font-mono animate-pulse" style={{ color: 'var(--noor-p-400)' }}>
                    %{prog}
                  </span>
                ) : downloaded ? (
                  <button
                    onClick={() => handleDeleteDownload(surah.number)}
                    className="p-1.5 rounded-xl transition-colors hover:bg-white/5"
                    style={{ color: 'var(--noor-n-400)' }}
                    title="حذف النسخة المحملة"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                ) : (
                  <button
                    onClick={() => handleDownloadSurah(surah.number)}
                    className="p-1.5 rounded-xl transition-colors hover:bg-white/5"
                    style={{ color: 'var(--noor-p-400)' }}
                    title={`تنزيل بصوت ${selectedReciter.name}`}
                  >
                    <Download className="w-4 h-4" />
                  </button>
                )}
              </div>
            );
          })}
        </div>

        {/* زر تشغيل معاينة صوتية للقارئ المختار */}
        <a
          href={getSurahAudioUrl(selectedReciter.serverUrl, 1)}
          target="_blank"
          rel="noreferrer"
          className="flex items-center justify-center gap-2 w-full py-2.5 rounded-2xl border text-xs font-bold transition-colors hover:brightness-110"
          style={{
            borderColor: 'color-mix(in srgb, var(--noor-p-500) 35%, transparent)',
            color: 'var(--noor-p-300)',
          }}
        >
          <ExternalLink className="w-3.5 h-3.5" />
          <span>معاينة صوتية: سورة الفاتحة بصوت {selectedReciter.name}</span>
        </a>
      </SettingsCard>

      {/* ===================== ضبط المواقيت الدقيق ===================== */}
      <SettingsCard
        icon={<Clock className="w-5 h-5" />}
        title="ضبط دقيق لمواقيت الصلاة"
        subtitle="تعديل كل صلاة ±دقائق لمطابقة مسجد منطقتك بالضبط"
      >
        <div className="space-y-2">
          {ADJUSTABLE_PRAYERS.map(({ key, label }) => {
            const value = adjustments[key] ?? 0;
            return (
              <div
                key={key}
                className="flex items-center justify-between p-2.5 rounded-2xl border"
                style={{
                  borderColor: 'color-mix(in srgb, var(--noor-n-700) 50%, transparent)',
                  backgroundColor: 'color-mix(in srgb, var(--noor-n-950) 55%, transparent)',
                }}
              >
                <span className="text-sm font-bold" style={{ color: 'var(--noor-n-100)' }}>
                  {label}
                </span>

                <div className="flex items-center gap-2">
                  <button
                    onClick={() => updateAdjustment(key, -1)}
                    className="w-8 h-8 rounded-xl border flex items-center justify-center transition-colors hover:bg-white/5"
                    style={{ borderColor: 'color-mix(in srgb, var(--noor-p-500) 30%, transparent)', color: 'var(--noor-n-200)' }}
                    title="تقديم دقيقة"
                  >
                    <Minus className="w-3.5 h-3.5" />
                  </button>
                  <span
                    className="w-16 text-center text-sm font-extrabold font-mono"
                    style={{ color: value === 0 ? 'var(--noor-n-400)' : 'var(--noor-p-300)' }}
                  >
                    {value > 0 ? `+${value}` : value} د
                  </span>
                  <button
                    onClick={() => updateAdjustment(key, +1)}
                    className="w-8 h-8 rounded-xl border flex items-center justify-center transition-colors hover:bg-white/5"
                    style={{ borderColor: 'color-mix(in srgb, var(--noor-p-500) 30%, transparent)', color: 'var(--noor-n-200)' }}
                    title="تأخير دقيقة"
                  >
                    <Plus className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            );
          })}
        </div>

        <button
          onClick={resetAdjustments}
          className="flex items-center justify-center gap-2 w-full py-2.5 rounded-2xl border text-xs font-bold transition-colors hover:brightness-110"
          style={{
            borderColor: 'color-mix(in srgb, var(--noor-p-500) 30%, transparent)',
            color: 'var(--noor-n-300)',
          }}
        >
          <RotateCcw className="w-3.5 h-3.5" />
          <span>إعادة الضبط الافتراضي (الحساب الفلكي الدقيق)</span>
        </button>
      </SettingsCard>

      {/* ===================== التحديثات ===================== */}
      <SettingsCard
        icon={<RefreshCw className="w-5 h-5" />}
        title="التحديثات"
        subtitle={`الإصدار الحالي ${APP_VERSION} • ${isApk ? 'نسخة أندرويد' : 'نسخة ويب'}`}
      >
        <button
          onClick={handleCheckUpdate}
          disabled={updateChecking}
          className="w-full py-3 rounded-2xl font-bold text-sm text-white shadow-lg transition-all hover:brightness-110 active:scale-[0.98] disabled:opacity-60 flex items-center justify-center gap-2"
          style={{ background: 'linear-gradient(135deg, var(--noor-p-600), var(--noor-a-600))' }}
        >
          {updateChecking ? (
            <>
              <RefreshCw className="w-4 h-4 animate-spin" />
              <span>جاري التحقق من التحديثات…</span>
            </>
          ) : (
            <>
              <RefreshCw className="w-4 h-4" />
              <span>التحقق من التحديثات الآن</span>
            </>
          )}
        </button>

        {updateResult && (
          <div
            className="p-3.5 rounded-2xl border text-xs leading-relaxed space-y-2"
            style={{
              backgroundColor:
                updateResult.status === 'available'
                  ? 'color-mix(in srgb, #10b981 14%, transparent)'
                  : updateResult.status === 'error'
                  ? 'color-mix(in srgb, #f43f5e 12%, transparent)'
                  : 'color-mix(in srgb, var(--noor-n-800) 60%, transparent)',
              borderColor:
                updateResult.status === 'available'
                  ? 'color-mix(in srgb, #10b981 40%, transparent)'
                  : updateResult.status === 'error'
                  ? 'color-mix(in srgb, #f43f5e 35%, transparent)'
                  : 'color-mix(in srgb, var(--noor-p-500) 25%, transparent)',
              color: 'var(--noor-n-100)',
            }}
          >
            <p>{updateResult.message}</p>
            {updateResult.status === 'available' && updateResult.apkUrl && (
              <a
                href={updateResult.apkUrl}
                target="_blank"
                rel="noreferrer"
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl font-bold text-white"
                style={{ backgroundColor: '#059669' }}
              >
                <Download className="w-3.5 h-3.5" />
                تحميل التحديث
              </a>
            )}
          </div>
        )}

        <a
          href={RELEASES_PAGE_URL}
          target="_blank"
          rel="noreferrer"
          className="flex items-center justify-center gap-1.5 text-[11px] transition-colors hover:brightness-125"
          style={{ color: 'var(--noor-n-400)' }}
        >
          <ExternalLink className="w-3 h-3" />
          <span>صفحة جميع الإصدارات على GitHub</span>
        </a>
      </SettingsCard>

      {/* ===================== الشكاوى والمقترحات ===================== */}
      <SettingsCard
        icon={<MessageSquareHeart className="w-5 h-5" />}
        title="شكاوى ومقترحات"
        subtitle="رأيك يطور التطبيق — شاركنا بأي ملاحظة أو مشكلة"
      >
        <button
          onClick={() => setShowFeedbackModal(true)}
          className="w-full py-3 rounded-2xl font-bold text-sm text-white shadow-lg transition-all hover:brightness-110 active:scale-[0.98] flex items-center justify-center gap-2"
          style={{ background: 'linear-gradient(135deg, #be185d, #f43f5e)' }}
        >
          <MessageSquareHeart className="w-4 h-4" />
          <span>إرسال شكوى أو مقترح</span>
        </button>
      </SettingsCard>

      {/* ===================== ويدجتس الشاشة الرئيسية ===================== */}
      <SettingsCard
        icon={<Smartphone className="w-5 h-5" />}
        title="ويدجتس الشاشة الرئيسية"
        subtitle="صمّم ويدجت المواقيت والأذكار لشاشة هاتفك"
      >
        <button
          onClick={onOpenWidgets}
          className="w-full py-3 rounded-2xl font-bold text-sm text-white shadow-lg transition-all hover:brightness-110 active:scale-[0.98] flex items-center justify-center gap-2"
          style={{ background: 'linear-gradient(135deg, var(--noor-p-600), #0d9488)' }}
        >
          <Smartphone className="w-4 h-4" />
          <span>فتح استوديو الويدجتس</span>
        </button>
      </SettingsCard>

      {/* ===================== عن التطبيق ===================== */}
      <SettingsCard icon={<Info className="w-5 h-5" />} title="عن التطبيق">
        <div
          className="p-4 rounded-2xl border space-y-2 text-center"
          style={{
            backgroundColor: `color-mix(in srgb, ${currentTheme.primaryColor} 8%, transparent)`,
            borderColor: `color-mix(in srgb, ${currentTheme.primaryColor} 25%, transparent)`,
          }}
        >
          <div className="flex items-center justify-center gap-2 text-xs font-extrabold" style={{ color: 'var(--noor-a-400)' }}>
            <HeartHandshake className="w-4 h-4" />
            <span>هذا التطبيق صدقة جارية على روح المرحوم بأذن الله أبي</span>
          </div>
          <div className="text-xs" style={{ color: 'var(--noor-n-300)' }}>
            نور المسلم — الإصدار {APP_VERSION}
          </div>
          <div className="text-[11px]" style={{ color: 'var(--noor-n-400)' }}>
            تطوير: <span className="font-bold" style={{ color: 'var(--noor-n-200)' }}>Yousef Sawan</span>
          </div>
        </div>
      </SettingsCard>

      {/* ===================== نافذة الشكاوى والمقترحات ===================== */}
      {showFeedbackModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in">
          <div
            className="w-full max-w-lg rounded-3xl border shadow-2xl p-6 space-y-4 text-right"
            style={{
              backgroundColor: 'color-mix(in srgb, var(--noor-n-900) 96%, transparent)',
              borderColor: 'color-mix(in srgb, #f43f5e 35%, transparent)',
            }}
          >
            <div className="flex items-center justify-between pb-3 border-b" style={{ borderColor: 'color-mix(in srgb, var(--noor-n-700) 60%, transparent)' }}>
              <h3 className="text-base font-bold flex items-center gap-2" style={{ color: 'var(--noor-n-50)' }}>
                <MessageSquareHeart className="w-5 h-5 text-rose-400" />
                <span>شكوى أو مقترح</span>
              </h3>
              <button
                onClick={() => {
                  setShowFeedbackModal(false);
                  setFeedbackCopied(false);
                }}
                className="p-1.5 rounded-xl transition-colors hover:bg-white/10"
                style={{ color: 'var(--noor-n-400)' }}
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <textarea
              value={feedbackText}
              onChange={(e) => setFeedbackText(e.target.value)}
              rows={5}
              placeholder="اكتب شكواك أو اقتراحك بالتفصيل…"
              className="w-full rounded-2xl border p-4 text-sm focus:outline-none focus:ring-1 resize-none"
              style={{
                backgroundColor: 'color-mix(in srgb, var(--noor-n-950) 70%, transparent)',
                borderColor: 'color-mix(in srgb, var(--noor-p-500) 25%, transparent)',
                color: 'var(--noor-n-100)',
              }}
            />

            <div className="flex flex-col sm:flex-row gap-2">
              <a
                href={feedbackIssueUrl()}
                target="_blank"
                rel="noreferrer"
                className="flex-1 flex items-center justify-center gap-2 py-3 rounded-2xl font-bold text-xs text-white shadow-lg transition-all hover:brightness-110"
                style={{ background: 'linear-gradient(135deg, #be185d, #f43f5e)' }}
              >
                <Send className="w-4 h-4" />
                <span>إرسال عبر GitHub</span>
              </a>
              <button
                onClick={() => {
                  navigator.clipboard.writeText(feedbackText);
                  setFeedbackCopied(true);
                  setTimeout(() => setFeedbackCopied(false), 2000);
                }}
                disabled={!feedbackText.trim()}
                className="flex-1 flex items-center justify-center gap-2 py-3 rounded-2xl border font-bold text-xs transition-colors hover:brightness-110 disabled:opacity-50"
                style={{
                  borderColor: 'color-mix(in srgb, var(--noor-p-500) 35%, transparent)',
                  color: 'var(--noor-n-200)',
                }}
              >
                {feedbackCopied ? <Check className="w-4 h-4" /> : <CopyIcon className="w-4 h-4" />}
                <span>{feedbackCopied ? 'تم النسخ!' : 'نسخ النص'}</span>
              </button>
            </div>

            <p className="text-[10px] text-center" style={{ color: 'var(--noor-n-500)' }}>
              يُرسل رأيك مباشرة إلى صفحة مطور التطبيق على GitHub — ويُراجع بإذن الله في أقرب وقت.
            </p>
          </div>
        </div>
      )}
    </div>
  );
};
