import { SURAHS_LIST } from './quranData';

/**
 * قواعد التجويد القياسية — نفس ترميز مصحف التجويد من alquran.cloud
 * (المبني على مصحف مجمع الملك فهد) بألوان المصاحف التجويدية المعتمدة.
 */
export type TajweedRule =
  | 'hamzat_wasl' // همزة وصل (رمادي)
  | 'silent' // حرف لا يُلفظ (رمادي)
  | 'laam_shamsiyah' // لام شمسية (رمادي)
  | 'madda_normal' // مد طبيعي حركتان
  | 'madda_permissible' // مد جائز 2/4/6
  | 'madda_necessary' // مد لازم 6
  | 'madda_obligatory' // مد واجب 4/5
  | 'qalaqalah' // قلقلة
  | 'ikhafa_shafawi' // إخفاء شفوي
  | 'ikhafa' // إخفاء
  | 'idgham_shafawi' // إدغام شفوي
  | 'iqlab' // إقلاب
  | 'idgham_ghunnah' // إدغام بغنة
  | 'idgham_wo_ghunnah' // إدغام بغير غنة
  | 'idgham_mutajanisayn' // إدغام متجانسين
  | 'idgham_mutaqaribayn' // إدغام متقاربين
  | 'ghunnah'; // غنة

export interface TajweedSegment {
  text: string;
  rule?: TajweedRule;
}

export interface TajweedAyah {
  surahNumber: number;
  ayahNumber: number;
  segments: TajweedSegment[];
  plainText: string;
  tafseer: string;
  juz: number;
  page: number;
}

export interface MushafPage {
  pageNumber: number;
  juzNumber: number;
  hizbQuarter: string;
  surahName: string;
  ayahs: TajweedAyah[];
}

export interface TajweedRuleStyle {
  label: string;
  /** لون الحبر في الطبعات الفاتحة */
  color: string;
  /** لون الحبر في المصحف الليلي (أفتح للقراءة على الداكن) */
  nightColor: string;
  bg: string;
  group: 'madd' | 'ghunnah_hide' | 'idgham' | 'qalqalah' | 'not_pronounced';
}

/** لوحة ألوان المصاحف التجويدية المعتمدة + درجات ليلية مريحة */
export const TAJWEED_RULES: Record<TajweedRule, TajweedRuleStyle> = {
  hamzat_wasl: { label: 'همزة وصل', color: '#9e9e9e', nightColor: '#9aa4b2', bg: 'rgba(158,158,158,0.15)', group: 'not_pronounced' },
  silent: { label: 'حرف لا يُلفظ', color: '#9e9e9e', nightColor: '#9aa4b2', bg: 'rgba(158,158,158,0.15)', group: 'not_pronounced' },
  laam_shamsiyah: { label: 'لام شمسية', color: '#9e9e9e', nightColor: '#9aa4b2', bg: 'rgba(158,158,158,0.15)', group: 'not_pronounced' },
  madda_normal: { label: 'مد طبيعي (حركتان)', color: '#537fff', nightColor: '#8fb0ff', bg: 'rgba(83,127,255,0.12)', group: 'madd' },
  madda_permissible: { label: 'مد جائز (2/4/6)', color: '#4050ff', nightColor: '#8091ff', bg: 'rgba(64,80,255,0.12)', group: 'madd' },
  madda_necessary: { label: 'مد لازم (6 حركات)', color: '#000ebc', nightColor: '#6d7dff', bg: 'rgba(0,14,188,0.10)', group: 'madd' },
  madda_obligatory: { label: 'مد واجب (4/5 حركات)', color: '#2144c1', nightColor: '#7f9bff', bg: 'rgba(33,68,193,0.12)', group: 'madd' },
  qalaqalah: { label: 'قلقلة', color: '#dd0008', nightColor: '#ff6b6f', bg: 'rgba(221,0,8,0.10)', group: 'qalqalah' },
  ikhafa_shafawi: { label: 'إخفاء شفوي', color: '#d500b7', nightColor: '#ff7ae0', bg: 'rgba(213,0,183,0.10)', group: 'ghunnah_hide' },
  ikhafa: { label: 'إخفاء', color: '#9400a8', nightColor: '#d878ea', bg: 'rgba(148,0,168,0.10)', group: 'ghunnah_hide' },
  idgham_shafawi: { label: 'إدغام شفوي', color: '#58b800', nightColor: '#8ee02e', bg: 'rgba(88,184,0,0.12)', group: 'idgham' },
  iqlab: { label: 'إقلاب', color: '#26bffd', nightColor: '#6fd8ff', bg: 'rgba(38,191,253,0.12)', group: 'idgham' },
  idgham_ghunnah: { label: 'إدغام بغنة', color: '#169777', nightColor: '#4fd1ac', bg: 'rgba(22,151,119,0.12)', group: 'idgham' },
  idgham_wo_ghunnah: { label: 'إدغام بغير غنة', color: '#169200', nightColor: '#6ee02e', bg: 'rgba(22,146,0,0.12)', group: 'idgham' },
  idgham_mutajanisayn: { label: 'إدغام متجانسين', color: '#a1a1a1', nightColor: '#b0b6bf', bg: 'rgba(161,161,161,0.12)', group: 'idgham' },
  idgham_mutaqaribayn: { label: 'إدغام متقاربين', color: '#a1a1a1', nightColor: '#b0b6bf', bg: 'rgba(161,161,161,0.12)', group: 'idgham' },
  ghunnah: { label: 'غنة (حركتان)', color: '#ff7e1e', nightColor: '#ffa45c', bg: 'rgba(255,126,30,0.12)', group: 'ghunnah_hide' },
};

/** خريطة مفاتيح الطبعة القديمة إلى القواعد القياسية الجديدة */
const LEGACY_RULE_MAP: Record<string, TajweedRule> = {
  madd_6: 'madda_necessary',
  madd_wajib: 'madda_obligatory',
  madd_jaiz: 'madda_permissible',
  madd_2: 'madda_normal',
  ghunnah: 'ghunnah',
  idgham: 'idgham_ghunnah',
  qalqalah: 'qalaqalah',
  tafkheem: 'laam_shamsiyah',
};

/** حروف ترميز alquran.cloud إلى قواعدنا */
const MARKUP_CODES: Record<string, TajweedRule> = {
  h: 'hamzat_wasl',
  s: 'silent',
  l: 'laam_shamsiyah',
  n: 'madda_normal',
  p: 'madda_permissible',
  m: 'madda_necessary',
  q: 'qalaqalah',
  o: 'madda_obligatory',
  c: 'ikhafa_shafawi',
  f: 'ikhafa',
  w: 'idgham_shafawi',
  i: 'iqlab',
  a: 'idgham_ghunnah',
  u: 'idgham_wo_ghunnah',
  d: 'idgham_mutajanisayn',
  b: 'idgham_mutaqaribayn',
  g: 'ghunnah',
};

const MARKUP_RE = /\[([a-z]*):?[0-9]*\[([^\]]*)\]/g;

/**
 * تحليل نص الآية بترميز التجويد [o[َٓ]] إلى مقاطع ملونة قابلة للعرض.
 * النص الخالي من الترميز يرجع كمقطع واحد بلا قاعدة.
 */
export function parseTajweedMarkup(text: string): TajweedSegment[] {
  if (!text || !text.includes('[')) {
    return text ? [{ text }] : [];
  }
  const segments: TajweedSegment[] = [];
  let lastIndex = 0;
  let m: RegExpExecArray | null;
  MARKUP_RE.lastIndex = 0;
  while ((m = MARKUP_RE.exec(text)) !== null) {
    if (m.index > lastIndex) {
      segments.push({ text: text.slice(lastIndex, m.index) });
    }
    const code = m[1];
    const rule = code ? MARKUP_CODES[code] : undefined;
    if (m[2]) {
      segments.push(rule ? { text: m[2], rule } : { text: m[2] });
    }
    lastIndex = m.index + m[0].length;
  }
  if (lastIndex < text.length) {
    segments.push({ text: text.slice(lastIndex) });
  }
  return segments;
}

/** توحيد أي مفتاح قاعدة قديم/جديد — يرجّع null لو مش معروف */
export function normalizeTajweedRule(raw: string | undefined): TajweedRule | null {
  if (!raw) return null;
  if (raw in TAJWEED_RULES) return raw as TajweedRule;
  const mapped = LEGACY_RULE_MAP[raw];
  return mapped ?? null;
}

export interface AyahReciter {
  id: string;
  name: string;
  folder: string; // EveryAyah folder
  country?: string;
  style?: string;
}

export const AYAH_RECITERS: AyahReciter[] = [
  { id: 'qatami', name: 'ناصر القطامي', folder: 'Nasser_Alqatami_128kbps', country: 'السعودية', style: 'تلاوة خاشعة مميزة' },
  { id: 'husary', name: 'محمود خليل الحصري', folder: 'Husary_128kbps', country: 'مصر', style: 'شيخ عموم المقارئ المصرية' },
  { id: 'minshawy', name: 'محمد صديق المنشاوي', folder: 'Minshawy_Murattal_128kbps', country: 'مصر', style: 'الصوت الباكي الخاشع' },
  { id: 'abdulbasit', name: 'عبد الباسط عبد الصمد', folder: 'Abdul_Basit_Murattal_192kbps', country: 'مصر', style: 'سفير القرآن وحنجرة السماء' },
  { id: 'mustafa_ismail', name: 'مصطفى إسماعيل', folder: 'Mustafa_Ismail_48kbps', country: 'مصر', style: 'إمام دولة التلاوة المصرية' },
  { id: 'tablawi', name: 'محمد محمود الطبلاوي', folder: 'Mohammad_al_Tablaway_128kbps', country: 'مصر', style: 'نقيب القراء المصريين الأسبق' },
  { id: 'banna', name: 'محمود علي البنا', folder: 'Ali_Al-banna_32kbps', country: 'مصر', style: 'تلاوة مصرية أصيلة خاشعة' },
  { id: 'jibreel', name: 'محمد جبريل', folder: 'Muhammad_Jibreel_128kbps', country: 'مصر', style: 'إمام صلاة التراويح بمصر' },
  { id: 'alafasy', name: 'مشاري راشد العفاسي', folder: 'Alafasy_128kbps', country: 'الكويت', style: 'حفص عن عاصم - مرتل' },
  { id: 'maher', name: 'ماهر المعيقلي', folder: 'MaherAlMuaiqly128kbps', country: 'السعودية', style: 'إمام الحرم المكي الشريف' },
  { id: 'dosari', name: 'ياسر الدوسري', folder: 'Yasser_Ad-Dussary_128kbps', country: 'السعودية', style: 'إمام وخطيب المسجد الحرام' },
  { id: 'sudais', name: 'عبد الرحمن السديس', folder: 'Abdurrahmaan_As-Sudais_192kbps', country: 'السعودية', style: 'إمام الحرم المكي الشريف' },
  { id: 'shuraim', name: 'سعود الشريم', folder: 'Saood_ash-Shuraym_128kbps', country: 'السعودية', style: 'إمام المسجد الحرام' },
  { id: 'ghamadi', name: 'سعد الغامدي', folder: 'Ghamadi_40kbps', country: 'السعودية', style: 'تلاوة هادئة خاشعة' },
  { id: 'shatri', name: 'أبو بكر الشاطري', folder: 'Abu_Bakr_Ash-Shaatree_128kbps', country: 'اليمن', style: 'تلاوة حجازية عذبة' },
  { id: 'ajamy', name: 'أحمد بن علي العجمي', folder: 'Ahmed_ibn_Ali_al-Ajamy_128kbps_KetabAllah.net', country: 'السعودية', style: 'قراءة مؤثرة ومشهورة' },
];

/**
 * Generates exact EveryAyah MP3 endpoint for single verse
 * format: {surahNumberPadded3}{ayahNumberPadded3}.mp3
 * Example: Surah 2, Ayah 17 -> "002017.mp3"
 */
export function getAyahAudioUrl(folder: string, surahNumber: number, ayahNumber: number): string {
  const s = surahNumber.toString().padStart(3, '0');
  const a = ayahNumber.toString().padStart(3, '0');
  return `https://everyayah.com/data/${folder}/${s}${a}.mp3`;
}

/**
 * Pre-cached authentic Mushaf Pages (including Page 4 matching user screenshot)
 */
/** مقاطع بطبعة قديمة — تُرقّى للقواعد القياسية عند التحميل */
interface LegacySegment {
  text: string;
  rule?: string;
}

type MushafPageLegacy = Omit<MushafPage, 'ayahs'> & {
  ayahs: Array<Omit<TajweedAyah, 'segments'> & { segments: LegacySegment[] }>;
};

const MUSHAF_PAGES_DATA_LEGACY: Record<number, MushafPageLegacy> = {
  // Page 4 - Exactly from User Screenshot!
  4: {
    pageNumber: 4,
    juzNumber: 1,
    hizbQuarter: 'الحزب 1',
    surahName: 'سورة البقرة',
    ayahs: [
      {
        surahNumber: 2,
        ayahNumber: 17,
        plainText: 'مَثَلُهُمْ كَمَثَلِ الَّذِي اسْتَوْقَدَ نَارًا فَلَمَّا أَضَاءَتْ مَا حَوْلَهُ ذَهَبَ اللَّهُ بِنُورِهِمْ وَتَرَكَهُمْ فِي ظُلُمَاتٍ لَا يُبْصِرُونَ',
        tafseer: 'حال المنافقين كحال من أوقد ناراً ليستضيء بها فلما أضاءت ما حوله انطفأت فبقي في حيرة وظلمة، كذلك هؤلاء آمنوا ثم كفروا فذهبت بصيرتهم.',
        juz: 1,
        page: 4,
        segments: [
          { text: 'مَثَلُهُمْ كَمَثَلِ الَّذِي اسْتَوْ' },
          { text: 'قَدَ', rule: 'qalqalah' },
          { text: ' نَارًا فَلَ' },
          { text: 'مَّا', rule: 'ghunnah' },
          { text: ' أَ' },
          { text: 'ضَاءَتْ', rule: 'madd_wajib' },
          { text: ' مَا حَوْلَهُ ذَهَبَ اللَّهُ بِنُورِهِمْ وَتَرَكَهُمْ فِي ظُلُمَاتٍ ' },
          { text: 'لَّا', rule: 'idgham' },
          { text: ' يُ' },
          { text: 'بْ', rule: 'qalqalah' },
          { text: 'صِ' },
          { text: 'رُونَ', rule: 'madda_permissible' },
        ],
      },
      {
        surahNumber: 2,
        ayahNumber: 18,
        plainText: 'صُمٌّ بُكْمٌ عُمْيٌ فَهُمْ لَا يَرْجِعُونَ',
        tafseer: 'هم صم عن سماع الحق سماع قبول، بكم عن النطق به، عمي عن رؤيته، فهم لا يرجعون عن ضلالهم.',
        juz: 1,
        page: 4,
        segments: [
          { text: 'صُ' },
          { text: 'مٌّ', rule: 'ghunnah' },
          { text: ' بُ' },
          { text: 'كْمٌ', rule: 'ghunnah' },
          { text: ' عُمْيٌ فَهُمْ لَا يَرْ' },
          { text: 'جِعُونَ', rule: 'madda_permissible' },
        ],
      },
      {
        surahNumber: 2,
        ayahNumber: 19,
        plainText: 'أَوْ كَصَيِّبٍ مِنَ السَّمَاءِ فِيهِ ظُلُمَاتٌ وَرَعْدٌ وَبَرْقٌ يَجْعَلُونَ أَصَابِعَهُمْ فِي آذَانِهِمْ مِنَ الصَّوَاعِقِ حَذَرَ الْمَوْتِ وَاللَّهُ مُحِيطٌ بِالْكَافِرِينَ',
        tafseer: 'أو كمثل مطر شديد من السماء فيه ظلمات ورعد وبرق، يجعلون أصابعهم في آذانهم مخافة الموت من شدة الصواعق، والله محيط بالكافرين لا يفوتونه.',
        juz: 1,
        page: 4,
        segments: [
          { text: 'أَوْ كَصَيِّ' },
          { text: 'بٍ مِّ', rule: 'idgham' },
          { text: 'نَ السَّ' },
          { text: 'مَاءِ', rule: 'madd_wajib' },
          { text: ' فِيهِ ظُلُمَاتٌ ' },
          { text: 'وَّ', rule: 'idgham' },
          { text: 'رَعْ' },
          { text: 'دٌ وَ', rule: 'idgham' },
          { text: 'بَرْ' },
          { text: 'قٌ يَ', rule: 'idgham' },
          { text: 'جْعَلُونَ أَصَابِعَهُمْ فِ' },
          { text: 'ي آ', rule: 'madda_permissible' },
          { text: 'ذَانِهِم ' },
          { text: 'مِّ', rule: 'ghunnah' },
          { text: 'نَ الصَّوَاعِقِ حَذَرَ الْمَوْتِ ۚ وَاللَّهُ مُحِي' },
          { text: 'طٌ بِ', rule: 'ghunnah' },
          { text: 'الْكَافِ' },
          { text: 'رِينَ', rule: 'madda_permissible' },
        ],
      },
      {
        surahNumber: 2,
        ayahNumber: 20,
        plainText: 'يَكَادُ الْبَرْقُ يَخْطَفُ أَبْصَارَهُمْ كُلَّمَا أَضَاءَ لَهُمْ مَشَوْا فِيهِ وَإِذَا أَظْلَمَ عَلَيْهِمْ قَامُوا وَلَوْ شَاءَ اللَّهُ لَذَهَبَ بِسَمْعِهِمْ وَأَبْصَارِهِمْ إِنَّ اللَّهَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ',
        tafseer: 'يكاد ضوء البرق الخاطف يعمي أبصارهم، كلما أضاء لهم تقدموا فيه وإذا أظلم توقفوا حيارى، ولو شاء الله لأذهب أسماعهم وأبصارهم، إن الله قادر على كل شيء.',
        juz: 1,
        page: 4,
        segments: [
          { text: 'يَكَادُ الْبَرْقُ يَخْطَفُ أَبْصَارَهُمْ ۖ كُلَّ' },
          { text: 'مَا أَ', rule: 'madda_permissible' },
          { text: 'ضَاءَ', rule: 'madd_wajib' },
          { text: ' لَهُم ' },
          { text: 'مَّ', rule: 'ghunnah' },
          { text: 'شَوْا فِيهِ وَإِ' },
          { text: 'ذَا أَ', rule: 'madda_permissible' },
          { text: 'ظْلَمَ عَلَيْهِمْ قَامُوا ۚ وَلَوْ ' },
          { text: 'شَاءَ', rule: 'madd_wajib' },
          { text: ' اللَّهُ لَذَهَبَ بِسَمْعِهِمْ وَأَبْصَارِهِمْ ۚ إِ' },
          { text: 'نَّ', rule: 'ghunnah' },
          { text: ' اللَّهَ عَلَىٰ كُلِّ شَيْءٍ ' },
          { text: 'قَ', rule: 'tafkheem' },
          { text: 'دِ' },
          { text: 'يرٌ', rule: 'madda_permissible' },
        ],
      },
      {
        surahNumber: 2,
        ayahNumber: 21,
        plainText: 'يَا أَيُّهَا النَّاسُ اعْبُدُوا رَبَّكُمُ الَّذِي خَلَقَكُمْ وَالَّذِينَ مِنْ قَبْلِكُمْ لَعَلَّكُمْ تَتَّقُونَ',
        tafseer: 'نداء للناس جميعاً: اعبدوا ربكم وحده الذي أوجدكم وأوجد الأمم السابقة لكي تنالوا وقاية من عذابه بتقواه.',
        juz: 1,
        page: 4,
        segments: [
          { text: 'يَ' },
          { text: 'ا أَيُّ', rule: 'madda_permissible' },
          { text: 'هَا النَّ' },
          { text: 'اسُ', rule: 'ghunnah' },
          { text: ' اعْبُدُوا رَبَّكُمُ الَّذِي خَلَقَكُمْ وَالَّذِينَ ' },
          { text: 'مِن قَ', rule: 'ghunnah' },
          { text: 'بْ', rule: 'qalqalah' },
          { text: 'لِكُمْ لَعَلَّكُمْ تَتَّ' },
          { text: 'قُونَ', rule: 'madda_permissible' },
        ],
      },
      {
        surahNumber: 2,
        ayahNumber: 22,
        plainText: 'الَّذِي جَعَلَ لَكُمُ الْأَرْضَ فِرَاشًا وَالسَّمَاءَ بِنَاءً وَأَنْزَلَ مِنَ السَّمَاءِ مَاءً فَأَخْرَجَ بِهِ مِنَ الثَّمَرَاتِ رِزْقًا لَكُمْ فَلَا تَجْعَلُوا لِلَّهِ أَنْدَادًا وَأَنْتُمْ تَعْلَمُونَ',
        tafseer: 'الذي بسط لكم الأرض ممهدة وسقف السماء مرفوعة وأنزل المطر فأخرج به شتى الأرزاق، فلا تجعلوا لله شركاء وأنتم تعلمون تفرده بالخلق والرزق.',
        juz: 1,
        page: 4,
        segments: [
          { text: 'الَّذِي جَعَلَ لَكُمُ الْأَرْضَ فِرَاشًا وَالسَّ' },
          { text: 'مَاءَ', rule: 'madd_wajib' },
          { text: ' بِنَ' },
          { text: 'اءً وَ', rule: 'idgham' },
          { text: 'أَ' },
          { text: 'نزَ', rule: 'ghunnah' },
          { text: 'لَ مِنَ السَّ' },
          { text: 'مَاءِ', rule: 'madd_wajib' },
          { text: ' ' },
          { text: 'مَاءً فَ', rule: 'ghunnah' },
          { text: 'أَخْرَجَ بِهِ مِنَ الثَّمَرَاتِ رِزْ' },
          { text: 'قًا لَّ', rule: 'idgham' },
          { text: 'كُمْ ۖ فَلَا تَجْعَلُوا لِلَّهِ أَ' },
          { text: 'ندَ', rule: 'ghunnah' },
          { text: 'ادًا وَ', rule: 'idgham' },
          { text: 'أَ' },
          { text: 'نتُمْ', rule: 'ghunnah' },
          { text: ' تَعْلَ' },
          { text: 'مُونَ', rule: 'madda_permissible' },
        ],
      },
      {
        surahNumber: 2,
        ayahNumber: 23,
        plainText: 'وَإِنْ كُنْتُمْ فِي رَيْبٍ مِمَّا نَزَّلْنَا عَلَى عَبْدِنَا فَأْتُوا بِسُورَةٍ مِنْ مِثْلِهِ وَادْعُوا شُهَدَاءَكُمْ مِنْ دُونِ اللَّهِ إِنْ كُنْتُمْ صَادِقِينَ',
        tafseer: 'وإن كنتم في شك من هذا القرآن الذي نزلناه على محمد ﷺ فأتوا بسورة واحدة من مثله واستعينوا بكل من استطعتم إن كنتم صادقين في دعواكم.',
        juz: 1,
        page: 4,
        segments: [
          { text: 'وَإِ' },
          { text: 'ن كُ', rule: 'ghunnah' },
          { text: 'نتُمْ', rule: 'ghunnah' },
          { text: ' فِي رَيْ' },
          { text: 'بٍ مِّ', rule: 'idgham' },
          { text: 'مَّا', rule: 'ghunnah' },
          { text: ' نَزَّلْنَا عَلَىٰ عَبْدِنَا فَأْتُوا بِسُورَ' },
          { text: 'ةٍ مِّ', rule: 'idgham' },
          { text: 'ن مِّ' },
          { text: 'ثْلِهِ وَادْعُوا شُهَ' },
          { text: 'دَاءَ', rule: 'madd_wajib' },
          { text: 'كُمْ ' },
          { text: 'مِّن دُ', rule: 'ghunnah' },
          { text: 'ونِ اللَّهِ إِ' },
          { text: 'ن كُ', rule: 'ghunnah' },
          { text: 'نتُمْ صَادِ' },
          { text: 'قِينَ', rule: 'madda_permissible' },
        ],
      },
      {
        surahNumber: 2,
        ayahNumber: 24,
        plainText: 'فَإِنْ لَمْ تَفْعَلُوا وَلَنْ تَفْعَلُوا فَاتَّقُوا النَّارَ الَّتِي وَقُودُهَا النَّاسُ وَالْحِجَارَةُ أُعِدَّتْ لِلْكَافِرِينَ',
        tafseer: 'فإن لم تفعلوا ذلك ولن تقدروا عليه أبداً لعجزكم، فاحذروا نار جهنم التي حطبها الناس والحجارة، هيئت وجهزت للكافرين.',
        juz: 1,
        page: 4,
        segments: [
          { text: 'فَإِ' },
          { text: 'ن لَّ', rule: 'idgham' },
          { text: 'مْ تَفْعَلُوا وَلَ' },
          { text: 'ن تَ', rule: 'ghunnah' },
          { text: 'فْعَلُوا فَاتَّقُوا النَّ' },
          { text: 'ارَ', rule: 'ghunnah' },
          { text: ' الَّتِي وَقُودُهَا النَّ' },
          { text: 'اسُ', rule: 'ghunnah' },
          { text: ' وَالْحِجَارَةُ ۖ أُعِدَّتْ لِلْكَافِ' },
          { text: 'رِينَ', rule: 'madda_permissible' },
        ],
      },
    ],
  },
  // Page 1 - Al Fatiha
  1: {
    pageNumber: 1,
    juzNumber: 1,
    hizbQuarter: 'الحزب 1',
    surahName: 'سورة الفاتحة',
    ayahs: [
      {
        surahNumber: 1,
        ayahNumber: 1,
        plainText: 'بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ',
        tafseer: 'أبدأ قراءتي مستعيناً باسم الله.',
        juz: 1,
        page: 1,
        segments: [
          { text: 'بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّ' },
          { text: 'حِيمِ', rule: 'madda_permissible' },
        ],
      },
      {
        surahNumber: 1,
        ayahNumber: 2,
        plainText: 'الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ',
        tafseer: 'الثناء الكامل والمجد لله رب الخلائق أجمعين.',
        juz: 1,
        page: 1,
        segments: [
          { text: 'الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِ' },
          { text: 'ينَ', rule: 'madda_permissible' },
        ],
      },
      {
        surahNumber: 1,
        ayahNumber: 3,
        plainText: 'الرَّحْمَٰنِ الرَّحِيمِ',
        tafseer: 'الرحمن بالخلائق، الرحيم بالمؤمنين.',
        juz: 1,
        page: 1,
        segments: [
          { text: 'الرَّحْمَٰنِ الرَّ' },
          { text: 'حِيمِ', rule: 'madda_permissible' },
        ],
      },
      {
        surahNumber: 1,
        ayahNumber: 4,
        plainText: 'مَالِكِ يَوْمِ الدِّينِ',
        tafseer: 'المتصرف بيوم الجزاء والحساب.',
        juz: 1,
        page: 1,
        segments: [
          { text: 'مَالِكِ يَوْمِ الدِّ' },
          { text: 'ينِ', rule: 'madda_permissible' },
        ],
      },
      {
        surahNumber: 1,
        ayahNumber: 5,
        plainText: 'إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ',
        tafseer: 'نخصك وحدك بالعبادة ونستعين بك في كل شأن.',
        juz: 1,
        page: 1,
        segments: [
          { text: 'إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِ' },
          { text: 'ينُ', rule: 'madda_permissible' },
        ],
      },
      {
        surahNumber: 1,
        ayahNumber: 6,
        plainText: 'اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ',
        tafseer: 'وفقنا وثبتنا على الصراط السوي.',
        juz: 1,
        page: 1,
        segments: [
          { text: 'اهْدِنَا الصِّرَاطَ الْمُسْتَقِ' },
          { text: 'يمَ', rule: 'madda_permissible' },
        ],
      },
      {
        surahNumber: 1,
        ayahNumber: 7,
        plainText: 'صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ',
        tafseer: 'طريق الذين أنعمت عليهم غير طريق المغضوب عليهم ولا الضالين.',
        juz: 1,
        page: 1,
        segments: [
          { text: 'صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا ' },
          { text: 'الضَّالِّ', rule: 'madd_6' },
          { text: 'ينَ', rule: 'madda_permissible' },
        ],
      },
    ],
  },
};

/** الصفحات المنتقاة — تُرقّى قواعدها القديمة للقواعد القياسية (fallback قبل اكتمال التحميل) */
export const MUSHAF_PAGES_DATA: Record<number, MushafPage> = Object.fromEntries(
  Object.entries(MUSHAF_PAGES_DATA_LEGACY).map(([num, page]) => [
    Number(num),
    {
      ...page,
      ayahs: page.ayahs.map((a) => ({
        ...a,
        segments: a.segments.map((seg) => {
          const rule = normalizeTajweedRule(seg.rule);
          return rule ? { text: seg.text, rule } : { text: seg.text };
        }),
      })),
    },
  ]),
);

/**
 * تحميل نص المصحف كامل (604 صفحة) + مصحف التجويد الملون (6236 آية)
 * — الاثنان يُدمجان: نص الصفحات للبنية والنص العثماني، وترميز التجويد للتلوين.
 * أي فشل شبكة = صمت والرجوع للصفحات المنتقاة المدمجة (fallback دايماً).
 */
let quranPagesCache: Record<number, MushafPage> | null = null;
let quranPagesPromise: Promise<void> | null = null;
let tajweedCache: Record<string, string[]> | null = null;

interface RawQuranPage {
  j: number;
  h: number;
  sur: number;
  a: Array<{ s: number; a: number; t: string }>;
}

/** بناء مقاطع التجويد لآية من كاش مصحف التجويد (لو محمّل) */
function buildSegments(surahNumber: number, ayahNumber: number): TajweedSegment[] | null {
  if (!tajweedCache) return null;
  const markup = tajweedCache[String(surahNumber)]?.[ayahNumber - 1];
  if (typeof markup !== 'string') return null;
  return parseTajweedMarkup(markup);
}

export function loadQuranPages(): Promise<void> {
  if (quranPagesCache) return Promise.resolve();
  if (quranPagesPromise) return quranPagesPromise;

  const pagesPromise = fetch('/data/quran-pages.json').then((res) =>
    res.ok
      ? (res.json() as Promise<{ pages: Record<string, RawQuranPage> }>)
      : Promise.reject(new Error(String(res.status))),
  );
  // مصحف التجويد يُحمَّل بالتوازي — فشله لا يمنع الصفحات (تظهر بلا تلوين فقط)
  const tajweedPromise = fetch('/data/quran-tajweed.json')
    .then((res) => (res.ok ? (res.json() as Promise<Record<string, string[]>>) : null))
    .catch(() => null);

  quranPagesPromise = Promise.all([pagesPromise, tajweedPromise])
    .then(([data, tajweed]) => {
      tajweedCache = tajweed;
      const hydrated: Record<number, MushafPage> = {};
      for (const [num, p] of Object.entries(data.pages)) {
        const pageNumber = Number(num);
        hydrated[pageNumber] = {
          pageNumber,
          juzNumber: p.j,
          hizbQuarter: `الحزب ${p.h}`,
          surahName: `سورة ${SURAHS_LIST[p.sur - 1]?.name ?? ''}`,
          ayahs: p.a.map((x) => ({
            surahNumber: x.s,
            ayahNumber: x.a,
            plainText: x.t,
            tafseer: '',
            juz: p.j,
            page: pageNumber,
            segments: buildSegments(x.s, x.a) ?? [{ text: x.t }],
          })),
        };
      }
      quranPagesCache = hydrated;
    })
    .catch(() => {
      // fallback: نص المصحف المدمج مش متاح — الصفحات المنتقاة/المولّدة تكفي
      quranPagesPromise = null;
    });
  return quranPagesPromise;
}

export function isQuranPagesLoaded(): boolean {
  return quranPagesCache !== null;
}

export function getSurahForPage(pageNumber: number) {
  let matched = SURAHS_LIST[0];
  for (const s of SURAHS_LIST) {
    if (s.page <= pageNumber) {
      matched = s;
    } else {
      break;
    }
  }
  return matched;
}

/**
 * جلب صفحة المصحف: النص المدمج بالتجويد أولاً، ثم الصفحات المنتقاة
 * مع ترقية تلوينها من مصحف التجويد لو محمّل، ثم مولّد احتياطي قبل اكتمال التحميل.
 */
export function getOrCreateMushafPage(pageNumber: number): MushafPage {
  if (quranPagesCache?.[pageNumber]) {
    return quranPagesCache[pageNumber];
  }
  if (MUSHAF_PAGES_DATA[pageNumber]) {
    const page = MUSHAF_PAGES_DATA[pageNumber];
    // لو مصحف التجويد محمّل — نستبدل المقاطع اليدوية بالتلوين المعتمد الكامل
    const upgraded = page.ayahs.map((ayah) => ({
      ...ayah,
      segments: buildSegments(ayah.surahNumber, ayah.ayahNumber) ?? ayah.segments,
    }));
    return { ...page, ayahs: upgraded };
  }

  const surah = getSurahForPage(pageNumber);
  const juz = Math.max(1, Math.min(30, Math.ceil(pageNumber / 20)));
  const hizb = Math.ceil(juz * 2);

  return {
    pageNumber,
    juzNumber: juz,
    hizbQuarter: `الحزب ${hizb}`,
    surahName: `سورة ${surah.name}`,
    ayahs: [
      {
        surahNumber: surah.number,
        ayahNumber: 1,
        plainText: `بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ - قراءة خاشعة من سورة ${surah.name} بالرسم العثماني الملون (صفحة ${pageNumber})`,
        tafseer: `بداية مباركة من سورة ${surah.name}، وهي سورة ${surah.revelationTypeAr} وعدد آياتها ${surah.numberOfAyahs} آية في الجزء ${juz}. يمكنك الضغط على أي آية للاستماع بصوت القارئ المفضل والبدء بالحفظ.`,
        juz,
        page: pageNumber,
        segments: [
          { text: 'بِسْمِ اللَّهِ الرَّ' },
          { text: 'حْمَٰ', rule: 'madda_normal' },
          { text: 'نِ الرَّ' },
          { text: 'حِيمِ', rule: 'madda_permissible' },
          { text: ` - تلاوة خاشعة من سورة ${surah.name} صفحة ${pageNumber}` },
        ],
      },
    ],
  };
}
