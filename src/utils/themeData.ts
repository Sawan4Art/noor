/**
 * Theme & Color Mode System for Noor Al-Muslim
 * Supports 6 authentic Islamic color themes & full Dark/Light modes
 */

export type ColorMode = 'dark' | 'light';

export interface AppTheme {
  id: string;
  name: string;
  englishName: string;
  description: string;
  primaryColor: string;
  accentColor: string;
  bgGradientDark: string;
  bgGradientLight: string;
  cardBgDark: string;
  cardBgLight: string;
  borderDark: string;
  borderLight: string;
  primaryButton: string;
  badgeClass: string;
  tabActiveClass: string;
  accentTextDark: string;
  accentTextLight: string;
  glowColor: string;
}

export const APP_THEMES: AppTheme[] = [
  {
    id: 'emerald',
    name: 'زمردي ملكي',
    englishName: 'Royal Emerald',
    description: 'اللون الكلاسيكي للعمارة والمآذن الإسلامية مع لمسات الذهب',
    primaryColor: '#059669',
    accentColor: '#10b981',
    bgGradientDark: 'from-slate-950 via-[#041a14] to-slate-950',
    bgGradientLight: 'from-[#f0fdf4] via-[#f8fafc] to-[#ecfdf5]',
    cardBgDark: 'bg-slate-900/80',
    cardBgLight: 'bg-white/95',
    borderDark: 'border-emerald-800/40',
    borderLight: 'border-emerald-200/80',
    primaryButton: 'bg-emerald-600 hover:bg-emerald-500 text-white shadow-emerald-950/50',
    badgeClass: 'bg-emerald-500/15 text-emerald-600 dark:text-emerald-300 border-emerald-500/30',
    tabActiveClass: 'bg-emerald-600 text-white shadow-md shadow-emerald-900/40',
    accentTextDark: 'text-emerald-400',
    accentTextLight: 'text-emerald-700',
    glowColor: 'rgba(16, 185, 129, 0.25)',
  },
  {
    id: 'sapphire',
    name: 'أزرق أندلسي',
    englishName: 'Andalusian Sapphire',
    description: 'مستوحى من قباب قصر الحمراء والفسيفساء الأندلسية والزليج المغربي',
    primaryColor: '#2563eb',
    accentColor: '#06b6d4',
    bgGradientDark: 'from-slate-950 via-[#08182b] to-slate-950',
    bgGradientLight: 'from-[#eff6ff] via-[#f8fafc] to-[#f0f9ff]',
    cardBgDark: 'bg-slate-900/80',
    cardBgLight: 'bg-white/95',
    borderDark: 'border-blue-800/40',
    borderLight: 'border-blue-200/80',
    primaryButton: 'bg-blue-600 hover:bg-blue-500 text-white shadow-blue-950/50',
    badgeClass: 'bg-blue-500/15 text-blue-600 dark:text-blue-300 border-blue-500/30',
    tabActiveClass: 'bg-blue-600 text-white shadow-md shadow-blue-900/40',
    accentTextDark: 'text-blue-400',
    accentTextLight: 'text-blue-700',
    glowColor: 'rgba(37, 99, 235, 0.25)',
  },
  {
    id: 'kaaba',
    name: 'عنبر الكعبة وكسوتها',
    englishName: 'Kaaba Kiswa & Amber',
    description: 'هيبة سواد كسوة الكعبة المشرفة المطرزة بأسلاك الذهب والفضة الخالصة',
    primaryColor: '#d97706',
    accentColor: '#f59e0b',
    bgGradientDark: 'from-stone-950 via-[#1c1407] to-stone-950',
    bgGradientLight: 'from-[#fffbeb] via-[#fbfaf8] to-[#fef3c7]',
    cardBgDark: 'bg-stone-900/80',
    cardBgLight: 'bg-white/95',
    borderDark: 'border-amber-800/40',
    borderLight: 'border-amber-200/80',
    primaryButton: 'bg-amber-600 hover:bg-amber-500 text-white shadow-amber-950/50',
    badgeClass: 'bg-amber-500/15 text-amber-700 dark:text-amber-300 border-amber-500/30',
    tabActiveClass: 'bg-amber-600 text-white shadow-md shadow-amber-900/40',
    accentTextDark: 'text-amber-400',
    accentTextLight: 'text-amber-700',
    glowColor: 'rgba(217, 119, 6, 0.25)',
  },
  {
    id: 'indigo',
    name: 'نيلي ليلي متهجد',
    englishName: 'Midnight Indigo',
    description: 'سكينة قيام الليل والتهجد تحت سماء صامتة مرصعة بنجوم السماء',
    primaryColor: '#6366f1',
    accentColor: '#a855f7',
    bgGradientDark: 'from-slate-950 via-[#140b2b] to-slate-950',
    bgGradientLight: 'from-[#faf5ff] via-[#f8fafc] to-[#f5f3ff]',
    cardBgDark: 'bg-slate-900/80',
    cardBgLight: 'bg-white/95',
    borderDark: 'border-indigo-800/40',
    borderLight: 'border-indigo-200/80',
    primaryButton: 'bg-indigo-600 hover:bg-indigo-500 text-white shadow-indigo-950/50',
    badgeClass: 'bg-indigo-500/15 text-indigo-600 dark:text-indigo-300 border-indigo-500/30',
    tabActiveClass: 'bg-indigo-600 text-white shadow-md shadow-indigo-900/40',
    accentTextDark: 'text-indigo-400',
    accentTextLight: 'text-indigo-700',
    glowColor: 'rgba(99, 102, 241, 0.25)',
  },
  {
    id: 'ruby',
    name: 'ياقوت أندلسي قرمزي',
    englishName: 'Crimson Ruby',
    description: 'ألوان المخمل والياقوت مع لمسات من الذهب الوردي الدافئ الفاخر',
    primaryColor: '#e11d48',
    accentColor: '#f43f5e',
    bgGradientDark: 'from-stone-950 via-[#260a12] to-stone-950',
    bgGradientLight: 'from-[#fff1f2] via-[#f8fafc] to-[#ffe4e6]',
    cardBgDark: 'bg-stone-900/80',
    cardBgLight: 'bg-white/95',
    borderDark: 'border-rose-800/40',
    borderLight: 'border-rose-200/80',
    primaryButton: 'bg-rose-600 hover:bg-rose-500 text-white shadow-rose-950/50',
    badgeClass: 'bg-rose-500/15 text-rose-600 dark:text-rose-300 border-rose-500/30',
    tabActiveClass: 'bg-rose-600 text-white shadow-md shadow-rose-900/40',
    accentTextDark: 'text-rose-400',
    accentTextLight: 'text-rose-700',
    glowColor: 'rgba(225, 29, 72, 0.25)',
  },
  {
    id: 'sage',
    name: 'واحة الزيتون والمسك',
    englishName: 'Olive Oasis',
    description: 'درجات الزيتون والواحات الهادئة المريحة للعين للقراءة والتلاوة الطويلة',
    primaryColor: '#65a30d',
    accentColor: '#84cc16',
    bgGradientDark: 'from-zinc-950 via-[#151f0b] to-zinc-950',
    bgGradientLight: 'from-[#f7fee7] via-[#f8fafc] to-[#ecfccb]',
    cardBgDark: 'bg-zinc-900/80',
    cardBgLight: 'bg-white/95',
    borderDark: 'border-lime-800/40',
    borderLight: 'border-lime-200/80',
    primaryButton: 'bg-lime-600 hover:bg-lime-500 text-white shadow-lime-950/50',
    badgeClass: 'bg-lime-500/15 text-lime-700 dark:text-lime-300 border-lime-500/30',
    tabActiveClass: 'bg-lime-600 text-white shadow-md shadow-lime-900/40',
    accentTextDark: 'text-lime-400',
    accentTextLight: 'text-lime-700',
    glowColor: 'rgba(101, 163, 13, 0.25)',
  },
];

const THEME_STORAGE_KEY = 'noor_theme_id';
const MODE_STORAGE_KEY = 'noor_color_mode';

export function getSavedTheme(): AppTheme {
  try {
    const savedId = localStorage.getItem(THEME_STORAGE_KEY);
    const found = APP_THEMES.find((t) => t.id === savedId);
    return found || APP_THEMES[0];
  } catch {
    return APP_THEMES[0];
  }
}

export function saveTheme(themeId: string): void {
  try {
    localStorage.setItem(THEME_STORAGE_KEY, themeId);
  } catch {
    // localStorage unavailable
  }
}

export function getSavedColorMode(): ColorMode {
  try {
    const saved = localStorage.getItem(MODE_STORAGE_KEY) as ColorMode;
    if (saved === 'light' || saved === 'dark') return saved;
    return 'dark'; // default to elegant dark Islamic night mode
  } catch {
    return 'dark';
  }
}

export function saveColorMode(mode: ColorMode): void {
  try {
    localStorage.setItem(MODE_STORAGE_KEY, mode);
  } catch {
    // localStorage unavailable
  }
}
