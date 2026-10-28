import { SURAHS_LIST } from './quranData';

export interface TajweedSegment {
  text: string;
  rule?: 'madd_6' | 'madd_wajib' | 'madd_jaiz' | 'madd_2' | 'ghunnah' | 'idgham' | 'qalqalah' | 'tafkheem';
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

export const TAJWEED_COLORS: Record<string, { label: string; color: string; bg: string }> = {
  madd_6: { label: 'مد 6 حركات لزوماً', color: '#db2777', bg: 'rgba(219, 39, 119, 0.15)' },
  madd_wajib: { label: 'مد واجب 4 أو 5 حركات', color: '#dc2626', bg: 'rgba(220, 38, 38, 0.15)' },
  madd_jaiz: { label: 'مد 2 أو 4 أو 6 جوازاً', color: '#f59e0b', bg: 'rgba(245, 158, 11, 0.15)' },
  madd_2: { label: 'مد حركتان', color: '#eab308', bg: 'rgba(234, 179, 8, 0.15)' },
  ghunnah: { label: 'إخفاء ، ومواقع الغُنّة (حركتان)', color: '#16a34a', bg: 'rgba(22, 163, 74, 0.15)' },
  idgham: { label: 'إدغام ، وما لا يُلفظ', color: '#94a3b8', bg: 'rgba(148, 163, 184, 0.15)' },
  qalqalah: { label: 'قلقلة', color: '#06b6d4', bg: 'rgba(6, 182, 212, 0.15)' },
  tafkheem: { label: 'تفخيم', color: '#0284c7', bg: 'rgba(2, 132, 199, 0.15)' },
};

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
export const MUSHAF_PAGES_DATA: Record<number, MushafPage> = {
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
          { text: 'رُونَ', rule: 'madd_jaiz' },
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
          { text: 'جِعُونَ', rule: 'madd_jaiz' },
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
          { text: 'ي آ', rule: 'madd_jaiz' },
          { text: 'ذَانِهِم ' },
          { text: 'مِّ', rule: 'ghunnah' },
          { text: 'نَ الصَّوَاعِقِ حَذَرَ الْمَوْتِ ۚ وَاللَّهُ مُحِي' },
          { text: 'طٌ بِ', rule: 'ghunnah' },
          { text: 'الْكَافِ' },
          { text: 'رِينَ', rule: 'madd_jaiz' },
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
          { text: 'مَا أَ', rule: 'madd_jaiz' },
          { text: 'ضَاءَ', rule: 'madd_wajib' },
          { text: ' لَهُم ' },
          { text: 'مَّ', rule: 'ghunnah' },
          { text: 'شَوْا فِيهِ وَإِ' },
          { text: 'ذَا أَ', rule: 'madd_jaiz' },
          { text: 'ظْلَمَ عَلَيْهِمْ قَامُوا ۚ وَلَوْ ' },
          { text: 'شَاءَ', rule: 'madd_wajib' },
          { text: ' اللَّهُ لَذَهَبَ بِسَمْعِهِمْ وَأَبْصَارِهِمْ ۚ إِ' },
          { text: 'نَّ', rule: 'ghunnah' },
          { text: ' اللَّهَ عَلَىٰ كُلِّ شَيْءٍ ' },
          { text: 'قَ', rule: 'tafkheem' },
          { text: 'دِ' },
          { text: 'يرٌ', rule: 'madd_jaiz' },
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
          { text: 'ا أَيُّ', rule: 'madd_jaiz' },
          { text: 'هَا النَّ' },
          { text: 'اسُ', rule: 'ghunnah' },
          { text: ' اعْبُدُوا رَبَّكُمُ الَّذِي خَلَقَكُمْ وَالَّذِينَ ' },
          { text: 'مِن قَ', rule: 'ghunnah' },
          { text: 'بْ', rule: 'qalqalah' },
          { text: 'لِكُمْ لَعَلَّكُمْ تَتَّ' },
          { text: 'قُونَ', rule: 'madd_jaiz' },
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
          { text: 'مُونَ', rule: 'madd_jaiz' },
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
          { text: 'قِينَ', rule: 'madd_jaiz' },
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
          { text: 'رِينَ', rule: 'madd_jaiz' },
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
          { text: 'حِيمِ', rule: 'madd_jaiz' },
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
          { text: 'ينَ', rule: 'madd_jaiz' },
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
          { text: 'حِيمِ', rule: 'madd_jaiz' },
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
          { text: 'ينِ', rule: 'madd_jaiz' },
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
          { text: 'ينُ', rule: 'madd_jaiz' },
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
          { text: 'يمَ', rule: 'madd_jaiz' },
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
          { text: 'ينَ', rule: 'madd_jaiz' },
        ],
      },
    ],
  },
};

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
 * Fallback synthesizer for pages that are not preloaded
 */
export function getOrCreateMushafPage(pageNumber: number): MushafPage {
  if (MUSHAF_PAGES_DATA[pageNumber]) {
    return MUSHAF_PAGES_DATA[pageNumber];
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
          { text: 'حْمَٰ', rule: 'madd_2' },
          { text: 'نِ الرَّ' },
          { text: 'حِيمِ', rule: 'madd_jaiz' },
          { text: ` - تلاوة خاشعة من سورة ${surah.name} صفحة ${pageNumber}` },
        ],
      },
    ],
  };
}
