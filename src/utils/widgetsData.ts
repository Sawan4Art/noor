/**
 * Android Customizable Widgets Definitions and Configuration
 * Covers: Prayer Times, Next Prayer, After-Prayer Adhkar, Daily Ayah, Quick Tasbih, Minimal Hijri
 */

export type WidgetType =
  | 'prayer_full'
  | 'next_prayer'
  | 'after_prayer_adhkar'
  | 'daily_ayah'
  | 'quick_tasbih'
  | 'hijri_minimal';

export type WidgetTheme =
  | 'emerald'
  | 'glass'
  | 'amoled'
  | 'material_teal'
  | 'gold'
  | 'paper_light';

export interface WidgetConfig {
  id: string;
  type: WidgetType;
  title: string;
  arabicName: string;
  sizeLabel: '2x1' | '2x2' | '3x2' | '4x1' | '4x2';
  theme: WidgetTheme;
  opacity: number; // 0.2 to 1.0
  borderRadius: number; // 12 to 36
  showHijriDate: boolean;
  showCountdown: boolean;
  showActions: boolean;
  showMosquePattern: boolean;
  accentColor: string;
}

export const WIDGET_THEMES: Record<
  WidgetTheme,
  {
    name: string;
    bgClass: string;
    textClass: string;
    accentClass: string;
    borderClass: string;
  }
> = {
  emerald: {
    name: 'أخضر إسلامي ملكي (Royal Emerald)',
    bgClass: 'bg-gradient-to-br from-emerald-950/90 via-teal-900/90 to-slate-950/95',
    textClass: 'text-emerald-50',
    accentClass: 'text-emerald-400 bg-emerald-500/20 border-emerald-500/40',
    borderClass: 'border-emerald-700/50',
  },
  glass: {
    name: 'زجاجي شفاف حديث (Modern Glassmorphic)',
    bgClass: 'bg-white/10 backdrop-blur-xl',
    textClass: 'text-white',
    accentClass: 'text-cyan-300 bg-white/15 border-white/20',
    borderClass: 'border-white/20',
  },
  amoled: {
    name: 'أسود داكن موفر للطاقة (AMOLED Dark)',
    bgClass: 'bg-black/95',
    textClass: 'text-zinc-100',
    accentClass: 'text-emerald-400 bg-zinc-900 border-zinc-800',
    borderClass: 'border-zinc-800',
  },
  material_teal: {
    name: 'ماتيريال يو (Material You Android 14+)',
    bgClass: 'bg-gradient-to-br from-slate-900 via-teal-950 to-indigo-950',
    textClass: 'text-slate-100',
    accentClass: 'text-teal-300 bg-teal-500/20 border-teal-500/30',
    borderClass: 'border-teal-800/40',
  },
  gold: {
    name: 'ذهبي فاخر مذهب (Imperial Gold)',
    bgClass: 'bg-gradient-to-br from-amber-950/90 via-stone-900/95 to-yellow-950/90',
    textClass: 'text-amber-100',
    accentClass: 'text-amber-300 bg-amber-500/20 border-amber-500/40',
    borderClass: 'border-amber-600/40',
  },
  paper_light: {
    name: 'مصحفي ناصع أنيق (Parchment Light)',
    bgClass: 'bg-gradient-to-br from-amber-50/95 via-emerald-50/95 to-stone-100/95',
    textClass: 'text-slate-900',
    accentClass: 'text-emerald-800 bg-emerald-100/80 border-emerald-300',
    borderClass: 'border-amber-200/80',
  },
};

export const DEFAULT_WIDGETS: WidgetConfig[] = [
  {
    id: 'w-next',
    type: 'next_prayer',
    title: 'الصلاة القادمة والعد التنازلي',
    arabicName: 'ويدجت نداء الأذان والمؤذن',
    sizeLabel: '3x2',
    theme: 'emerald',
    opacity: 0.95,
    borderRadius: 24,
    showHijriDate: true,
    showCountdown: true,
    showActions: true,
    showMosquePattern: true,
    accentColor: '#10b981',
  },
  {
    id: 'w-times',
    type: 'prayer_full',
    title: 'مواقيت الصلوات الخمس كاملة',
    arabicName: 'لوحة مواقيت اليوم',
    sizeLabel: '4x2',
    theme: 'material_teal',
    opacity: 0.9,
    borderRadius: 24,
    showHijriDate: true,
    showCountdown: true,
    showActions: true,
    showMosquePattern: true,
    accentColor: '#14b8a6',
  },
  {
    id: 'w-after-prayer',
    type: 'after_prayer_adhkar',
    title: 'أذكار ما بعد الصلاة المفروضة',
    arabicName: 'حصن ما بعد الصلاة',
    sizeLabel: '4x1',
    theme: 'glass',
    opacity: 0.85,
    borderRadius: 20,
    showHijriDate: false,
    showCountdown: false,
    showActions: true,
    showMosquePattern: false,
    accentColor: '#38bdf8',
  },
  {
    id: 'w-ayah',
    type: 'daily_ayah',
    title: 'آية وتدبر يومي من كتاب الله',
    arabicName: 'قبس من القرآن',
    sizeLabel: '4x2',
    theme: 'gold',
    opacity: 0.95,
    borderRadius: 24,
    showHijriDate: true,
    showCountdown: false,
    showActions: true,
    showMosquePattern: true,
    accentColor: '#f59e0b',
  },
  {
    id: 'w-tasbih',
    type: 'quick_tasbih',
    title: 'المسبحة السريعة التفاعلية بنقرة واحدة',
    arabicName: 'سبحة الشاشة الرئيسية',
    sizeLabel: '2x2',
    theme: 'emerald',
    opacity: 0.95,
    borderRadius: 28,
    showHijriDate: false,
    showCountdown: false,
    showActions: true,
    showMosquePattern: false,
    accentColor: '#10b981',
  },
  {
    id: 'w-hijri',
    type: 'hijri_minimal',
    title: 'التقويم الهجري ومؤشر الصلاة المصغر',
    arabicName: 'ويدجت التاريخ والأذان المصغر',
    sizeLabel: '2x1',
    theme: 'amoled',
    opacity: 0.9,
    borderRadius: 18,
    showHijriDate: true,
    showCountdown: true,
    showActions: false,
    showMosquePattern: false,
    accentColor: '#10b981',
  },
];
