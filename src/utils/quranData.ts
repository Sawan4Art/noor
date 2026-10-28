/**
 * Quran Data, Surahs Registry, Reciters, and Offline Download Manager
 */

export interface Surah {
  number: number;
  name: string; // Arabic name e.g. الفاتحة
  englishName: string; // Al-Faatiha
  englishNameTranslation: string; // The Opening
  numberOfAyahs: number;
  revelationType: 'Meccan' | 'Medinan';
  revelationTypeAr: 'مكية' | 'مدنية';
  page: number;
}

export interface Reciter {
  id: string;
  name: string; // مشاري راشد العفاسي
  englishName: string;
  style: string; // مرتل / مجود / رواية حفص
  serverUrl: string; // MP3 base server url
  photoUrl: string;
  bitrate: string;
  country?: string;
}

export interface Ayah {
  numberInSurah: number;
  number: number;
  text: string;
  translation: string;
  tafseer: string;
  audioUrl?: string;
}

export const RECITERS: Reciter[] = [
  {
    id: 'qatami',
    name: 'ناصر القطامي',
    englishName: 'Nasser Al-Qatami',
    style: 'تلاوة خاشعة برواية حفص عن عاصم',
    serverUrl: 'https://server6.mp3quran.net/qtm/',
    photoUrl: 'https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&w=120&q=80',
    bitrate: '128 kbps',
    country: 'السعودية 🇸🇦',
  },
  {
    id: 'husary',
    name: 'محمود خليل الحصري',
    englishName: 'Mahmoud Khalil Al-Husary',
    style: 'شيخ عموم المقارئ المصرية - إتقان التجويد',
    serverUrl: 'https://server13.mp3quran.net/husr/',
    photoUrl: 'https://images.unsplash.com/photo-1585829365295-ab7cd400c167?auto=format&fit=crop&w=120&q=80',
    bitrate: '128 kbps',
    country: 'مصر 🇪🇬',
  },
  {
    id: 'minshawy',
    name: 'محمد صديق المنشاوي',
    englishName: 'Muhammad Siddiq Al-Minshawi',
    style: 'الصوت الباكي الخاشع - أعلام دولة التلاوة',
    serverUrl: 'https://server10.mp3quran.net/minsh/',
    photoUrl: 'https://images.unsplash.com/photo-1507679799987-c73779587ccf?auto=format&fit=crop&w=120&q=80',
    bitrate: '128 kbps',
    country: 'مصر 🇪🇬',
  },
  {
    id: 'abdulbasit',
    name: 'عبد الباسط عبد الصمد',
    englishName: 'Abdul Basit Abdul Samad',
    style: 'المصحف المرتل برواية حفص - صوت السماء',
    serverUrl: 'https://server7.mp3quran.net/basit/',
    photoUrl: 'https://images.unsplash.com/photo-1519817650390-64a93db51149?auto=format&fit=crop&w=120&q=80',
    bitrate: '128 kbps',
    country: 'مصر 🇪🇬',
  },
  {
    id: 'mustafa_ismail',
    name: 'مصطفى إسماعيل',
    englishName: 'Mustafa Ismail',
    style: 'إمام دولة التلاوة وقارئ الجامع الأزهر',
    serverUrl: 'https://server8.mp3quran.net/mustafa/',
    photoUrl: 'https://images.unsplash.com/photo-1544717305-2782549b5136?auto=format&fit=crop&w=120&q=80',
    bitrate: '128 kbps',
    country: 'مصر 🇪🇬',
  },
  {
    id: 'tablawi',
    name: 'محمد محمود الطبلاوي',
    englishName: 'Mohammad Al-Tablawi',
    style: 'نقيب القراء المصريين الأسبق - نبرة فريدة',
    serverUrl: 'https://server12.mp3quran.net/tblawi/',
    photoUrl: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=crop&w=120&q=80',
    bitrate: '128 kbps',
    country: 'مصر 🇪🇬',
  },
  {
    id: 'banna',
    name: 'محمود علي البنا',
    englishName: 'Mahmoud Ali Al-Banna',
    style: 'عذوبة الصوت والتلاوة المصرية الخاشعة',
    serverUrl: 'https://server8.mp3quran.net/bna/',
    photoUrl: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=120&q=80',
    bitrate: '128 kbps',
    country: 'مصر 🇪🇬',
  },
  {
    id: 'jibreel',
    name: 'محمد جبريل',
    englishName: 'Muhammad Jibreel',
    style: 'إمام صلاة التراويح بمسجد عمرو بن العاص',
    serverUrl: 'https://server8.mp3quran.net/jbrl/',
    photoUrl: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=120&q=80',
    bitrate: '128 kbps',
    country: 'مصر 🇪🇬',
  },
  {
    id: 'mishary',
    name: 'مشاري راشد العفاسي',
    englishName: 'Mishary Rashid Alafasy',
    style: 'حفص عن عاصم - مرتل عالي الجودة',
    serverUrl: 'https://server8.mp3quran.net/afs/',
    photoUrl: 'https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&w=120&q=80',
    bitrate: '128 kbps',
    country: 'الكويت 🇰🇼',
  },
  {
    id: 'maher',
    name: 'ماهر المعيقلي',
    englishName: 'Maher Al Muaiqly',
    style: 'إمام الحرم المكي الشريف',
    serverUrl: 'https://server12.mp3quran.net/maher/',
    photoUrl: 'https://images.unsplash.com/photo-1564769625905-50e93615e769?auto=format&fit=crop&w=120&q=80',
    bitrate: '128 kbps',
    country: 'السعودية 🇸🇦',
  },
  {
    id: 'dosari',
    name: 'ياسر الدوسري',
    englishName: 'Yasser Al-Dosari',
    style: 'إمام وخطيب المسجد الحرام',
    serverUrl: 'https://server11.mp3quran.net/yasser/',
    photoUrl: 'https://images.unsplash.com/photo-1579783902614-a3fb3927b675?auto=format&fit=crop&w=120&q=80',
    bitrate: '128 kbps',
    country: 'السعودية 🇸🇦',
  },
  {
    id: 'sudais',
    name: 'عبد الرحمن السديس',
    englishName: 'Abdur-Rahman As-Sudais',
    style: 'الرئيس العام لشؤون المسجد الحرام والمسجد النبوي',
    serverUrl: 'https://server11.mp3quran.net/sds/',
    photoUrl: 'https://images.unsplash.com/photo-1560250097-0b93528c311a?auto=format&fit=crop&w=120&q=80',
    bitrate: '128 kbps',
    country: 'السعودية 🇸🇦',
  },
  {
    id: 'shuraim',
    name: 'سعود الشريم',
    englishName: 'Saud Al-Shuraim',
    style: 'إمام وخطيب المسجد الحرام الأسبق',
    serverUrl: 'https://server7.mp3quran.net/shur/',
    photoUrl: 'https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?auto=format&fit=crop&w=120&q=80',
    bitrate: '128 kbps',
    country: 'السعودية 🇸🇦',
  },
  {
    id: 'ghamdi',
    name: 'سعد الغامدي',
    englishName: 'Saad Al-Ghamdi',
    style: 'قراءة خاشعة مرتلة',
    serverUrl: 'https://server7.mp3quran.net/s_gmd/',
    photoUrl: 'https://images.unsplash.com/photo-1590076215667-875d4ef2d7ee?auto=format&fit=crop&w=120&q=80',
    bitrate: '128 kbps',
    country: 'السعودية 🇸🇦',
  },
  {
    id: 'shatri',
    name: 'أبو بكر الشاطري',
    englishName: 'Abu Bakr Al-Shatri',
    style: 'تلاوة حجازية عذبة وهادئة',
    serverUrl: 'https://server11.mp3quran.net/shatri/',
    photoUrl: 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?auto=format&fit=crop&w=120&q=80',
    bitrate: '128 kbps',
    country: 'اليمن 🇾🇪',
  },
  {
    id: 'ajamy',
    name: 'أحمد بن علي العجمي',
    englishName: 'Ahmed Al-Ajmi',
    style: 'تلاوة مؤثرة تحرك القلوب',
    serverUrl: 'https://server10.mp3quran.net/ajm/',
    photoUrl: 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=120&q=80',
    bitrate: '128 kbps',
    country: 'السعودية 🇸🇦',
  },
];

/**
 * Format 3-digit surah number string for MP3Quran servers e.g. 1 -> "001.mp3"
 */
export function getSurahAudioUrl(serverUrl: string, surahNumber: number): string {
  const padNum = surahNumber.toString().padStart(3, '0');
  const cleanServer = serverUrl.endsWith('/') ? serverUrl : `${serverUrl}/`;
  return `${cleanServer}${padNum}.mp3`;
}

// 114 Surahs Directory
export const SURAHS_LIST: Surah[] = [
  { number: 1, name: 'الفاتحة', englishName: 'Al-Fatiha', englishNameTranslation: 'The Opening', numberOfAyahs: 7, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 1 },
  { number: 2, name: 'البقرة', englishName: 'Al-Baqarah', englishNameTranslation: 'The Cow', numberOfAyahs: 286, revelationType: 'Medinan', revelationTypeAr: 'مدنية', page: 2 },
  { number: 3, name: 'آل عمران', englishName: 'Ali \'Imran', englishNameTranslation: 'Family of Imran', numberOfAyahs: 200, revelationType: 'Medinan', revelationTypeAr: 'مدنية', page: 50 },
  { number: 4, name: 'النساء', englishName: 'An-Nisa', englishNameTranslation: 'The Women', numberOfAyahs: 176, revelationType: 'Medinan', revelationTypeAr: 'مدنية', page: 77 },
  { number: 5, name: 'المائدة', englishName: 'Al-Ma\'idah', englishNameTranslation: 'The Table Spread', numberOfAyahs: 120, revelationType: 'Medinan', revelationTypeAr: 'مدنية', page: 106 },
  { number: 6, name: 'الأنعام', englishName: 'Al-An\'am', englishNameTranslation: 'The Cattle', numberOfAyahs: 165, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 128 },
  { number: 7, name: 'الأعراف', englishName: 'Al-A\'raf', englishNameTranslation: 'The Heights', numberOfAyahs: 206, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 151 },
  { number: 8, name: 'الأنفال', englishName: 'Al-Anfal', englishNameTranslation: 'The Spoils of War', numberOfAyahs: 75, revelationType: 'Medinan', revelationTypeAr: 'مدنية', page: 177 },
  { number: 9, name: 'التوبة', englishName: 'At-Tawbah', englishNameTranslation: 'The Repentance', numberOfAyahs: 129, revelationType: 'Medinan', revelationTypeAr: 'مدنية', page: 187 },
  { number: 10, name: 'يونس', englishName: 'Yunus', englishNameTranslation: 'Jonah', numberOfAyahs: 109, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 208 },
  { number: 11, name: 'هود', englishName: 'Hud', englishNameTranslation: 'Hud', numberOfAyahs: 123, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 221 },
  { number: 12, name: 'يوسف', englishName: 'Yusuf', englishNameTranslation: 'Joseph', numberOfAyahs: 111, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 235 },
  { number: 13, name: 'الرعد', englishName: 'Ar-Ra\'d', englishNameTranslation: 'The Thunder', numberOfAyahs: 43, revelationType: 'Medinan', revelationTypeAr: 'مدنية', page: 249 },
  { number: 14, name: 'إبراهيم', englishName: 'Ibrahim', englishNameTranslation: 'Abraham', numberOfAyahs: 52, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 255 },
  { number: 15, name: 'الحجر', englishName: 'Al-Hijr', englishNameTranslation: 'The Rocky Tract', numberOfAyahs: 99, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 262 },
  { number: 16, name: 'النحل', englishName: 'An-Nahl', englishNameTranslation: 'The Bee', numberOfAyahs: 128, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 267 },
  { number: 17, name: 'الإسراء', englishName: 'Al-Isra', englishNameTranslation: 'The Night Journey', numberOfAyahs: 111, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 282 },
  { number: 18, name: 'الكهف', englishName: 'Al-Kahf', englishNameTranslation: 'The Cave', numberOfAyahs: 110, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 293 },
  { number: 19, name: 'مريم', englishName: 'Maryam', englishNameTranslation: 'Mary', numberOfAyahs: 98, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 305 },
  { number: 20, name: 'طه', englishName: 'Ta-Ha', englishNameTranslation: 'Ta-Ha', numberOfAyahs: 135, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 312 },
  { number: 21, name: 'الأنبياء', englishName: 'Al-Anbiya', englishNameTranslation: 'The Prophets', numberOfAyahs: 112, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 322 },
  { number: 22, name: 'الحج', englishName: 'Al-Hajj', englishNameTranslation: 'The Pilgrimage', numberOfAyahs: 78, revelationType: 'Medinan', revelationTypeAr: 'مدنية', page: 332 },
  { number: 23, name: 'المؤمنون', englishName: 'Al-Mu\'minun', englishNameTranslation: 'The Believers', numberOfAyahs: 118, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 342 },
  { number: 24, name: 'النور', englishName: 'An-Nur', englishNameTranslation: 'The Light', numberOfAyahs: 64, revelationType: 'Medinan', revelationTypeAr: 'مدنية', page: 350 },
  { number: 25, name: 'الفرقان', englishName: 'Al-Furqan', englishNameTranslation: 'The Criterion', numberOfAyahs: 77, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 359 },
  { number: 26, name: 'الشعراء', englishName: 'Ash-Shu\'ara', englishNameTranslation: 'The Poets', numberOfAyahs: 227, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 367 },
  { number: 27, name: 'النمل', englishName: 'An-Naml', englishNameTranslation: 'The Ant', numberOfAyahs: 93, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 377 },
  { number: 28, name: 'القصص', englishName: 'Al-Qasas', englishNameTranslation: 'The Stories', numberOfAyahs: 88, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 385 },
  { number: 29, name: 'العنكبوت', englishName: 'Al-\'Ankabut', englishNameTranslation: 'The Spider', numberOfAyahs: 69, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 396 },
  { number: 30, name: 'الروم', englishName: 'Ar-Rum', englishNameTranslation: 'The Romans', numberOfAyahs: 60, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 404 },
  { number: 31, name: 'لقمان', englishName: 'Luqman', englishNameTranslation: 'Luqman', numberOfAyahs: 34, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 411 },
  { number: 32, name: 'السجدة', englishName: 'As-Sajdah', englishNameTranslation: 'The Prostration', numberOfAyahs: 30, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 415 },
  { number: 33, name: 'الأحزاب', englishName: 'Al-Ahzab', englishNameTranslation: 'The Combined Forces', numberOfAyahs: 73, revelationType: 'Medinan', revelationTypeAr: 'مدنية', page: 418 },
  { number: 34, name: 'سبأ', englishName: 'Saba', englishNameTranslation: 'Sheba', numberOfAyahs: 54, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 428 },
  { number: 35, name: 'فاطر', englishName: 'Fatir', englishNameTranslation: 'Originator', numberOfAyahs: 45, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 434 },
  { number: 36, name: 'يس', englishName: 'Ya-Sin', englishNameTranslation: 'Ya Sin', numberOfAyahs: 83, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 440 },
  { number: 37, name: 'الصافات', englishName: 'As-Saffat', englishNameTranslation: 'Those who set the Ranks', numberOfAyahs: 182, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 446 },
  { number: 38, name: 'ص', englishName: 'Sad', englishNameTranslation: 'The Letter "Saad"', numberOfAyahs: 88, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 453 },
  { number: 39, name: 'الزمر', englishName: 'Az-Zumar', englishNameTranslation: 'The Troops', numberOfAyahs: 75, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 458 },
  { number: 40, name: 'غافر', englishName: 'Ghafir', englishNameTranslation: 'The Forgiver', numberOfAyahs: 85, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 467 },
  { number: 41, name: 'فصلت', englishName: 'Fussilat', englishNameTranslation: 'Explained in Detail', numberOfAyahs: 54, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 477 },
  { number: 42, name: 'الشورى', englishName: 'Ash-Shura', englishNameTranslation: 'The Consultation', numberOfAyahs: 53, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 483 },
  { number: 43, name: 'الزخرف', englishName: 'Az-Zukhruf', englishNameTranslation: 'The Ornaments of Gold', numberOfAyahs: 89, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 489 },
  { number: 44, name: 'الدخان', englishName: 'Ad-Dukhan', englishNameTranslation: 'The Smoke', numberOfAyahs: 59, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 496 },
  { number: 45, name: 'الجاثية', englishName: 'Al-Jathiyah', englishNameTranslation: 'The Crouching', numberOfAyahs: 37, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 499 },
  { number: 46, name: 'الأحقاف', englishName: 'Al-Ahqaf', englishNameTranslation: 'The Wind-Curved Sandhills', numberOfAyahs: 35, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 502 },
  { number: 47, name: 'محمد', englishName: 'Muhammad', englishNameTranslation: 'Muhammad', numberOfAyahs: 38, revelationType: 'Medinan', revelationTypeAr: 'مدنية', page: 507 },
  { number: 48, name: 'الفتح', englishName: 'Al-Fath', englishNameTranslation: 'The Victory', numberOfAyahs: 29, revelationType: 'Medinan', revelationTypeAr: 'مدنية', page: 511 },
  { number: 49, name: 'الحجرات', englishName: 'Al-Hujurat', englishNameTranslation: 'The Rooms', numberOfAyahs: 18, revelationType: 'Medinan', revelationTypeAr: 'مدنية', page: 515 },
  { number: 50, name: 'ق', englishName: 'Qaf', englishNameTranslation: 'The Letter "Qaf"', numberOfAyahs: 45, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 518 },
  { number: 55, name: 'الرحمن', englishName: 'Ar-Rahman', englishNameTranslation: 'The Beneficent', numberOfAyahs: 78, revelationType: 'Medinan', revelationTypeAr: 'مدنية', page: 531 },
  { number: 56, name: 'الواقعة', englishName: 'Al-Waqi\'ah', englishNameTranslation: 'The Inevitable', numberOfAyahs: 96, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 534 },
  { number: 67, name: 'الملك', englishName: 'Al-Mulk', englishNameTranslation: 'The Sovereignty', numberOfAyahs: 30, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 562 },
  { number: 112, name: 'الإخلاص', englishName: 'Al-Ikhlas', englishNameTranslation: 'The Sincerity', numberOfAyahs: 4, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 604 },
  { number: 113, name: 'الفلق', englishName: 'Al-Falaq', englishNameTranslation: 'The Daybreak', numberOfAyahs: 5, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 604 },
  { number: 114, name: 'الناس', englishName: 'An-Nas', englishNameTranslation: 'Mankind', numberOfAyahs: 6, revelationType: 'Meccan', revelationTypeAr: 'مكية', page: 604 },
];

/**
 * Built-in high fidelity Ayahs with Tashkeel, Translation & Tafseer
 */
export const SAMPLE_VERSES_BY_SURAH: Record<number, Ayah[]> = {
  1: [
    { numberInSurah: 1, number: 1, text: 'بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ', translation: 'In the name of Allah, the Entirely Merciful, the Especially Merciful.', tafseer: 'أبدأ قراءتي مستعينا باسم الله، مستحضرا رحمته الواسعة بجميع خلقه.' },
    { numberInSurah: 2, number: 2, text: 'الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ', translation: '[All] praise is [due] to Allah, Lord of the worlds -', tafseer: 'الثناء الكامل والمجد المطلق لله وحده المربي لجميع خلقه بالنعم.' },
    { numberInSurah: 3, number: 3, text: 'الرَّحْمَٰنِ الرَّحِيمِ', translation: 'The Entirely Merciful, the Especially Merciful,', tafseer: 'الرحمن الذي وسعت رحمته كل شيء، والرحيم بالمؤمنين خصوصاً.' },
    { numberInSurah: 4, number: 4, text: 'مَالِكِ يَوْمِ الدِّينِ', translation: 'Sovereign of the Day of Recompense.', tafseer: 'المتصرف وحده في يوم الحساب والجزاء وهو يوم القيامة.' },
    { numberInSurah: 5, number: 5, text: 'إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ', translation: 'It is You we worship and You we ask for help.', tafseer: 'نخصك وحدك بالعبادة والتذلل، ونستعين بك وحدك في جميع شؤوننا.' },
    { numberInSurah: 6, number: 6, text: 'اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ', translation: 'Guide us to the straight path -', tafseer: 'دلنا وأرشدنا ووفقنا وثبتنا على الصراط الواضح وهو الإسلام.' },
    { numberInSurah: 7, number: 7, text: 'صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ', translation: 'The path of those upon whom You have bestowed favor, not of those who have evoked [Your] anger or of those who are astray.', tafseer: 'طريق النبيين والصديقين والشهداء، غير طريق المغضوب عليهم (اليهود ومن شابههم) ولا الضالين (النصارى ومن تاه عن الحق).' },
  ],
  67: [
    { numberInSurah: 1, number: 5242, text: 'تَبَارَكَ الَّذِي بِيَدِهِ الْمُلْكُ وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ', translation: 'Blessed is He in whose hand is dominion, and He is over all things competent -', tafseer: 'تعاظم وتكاثر خير الله وبركته، الذي بيده مقاليد السماوات والأرض وهو على كل شيء قدير.' },
    { numberInSurah: 2, number: 5243, text: 'الَّذِي خَلَقَ الْمَوْتَ وَالْحَيَاةَ لِيَبْلُوَكُمْ أَيُّكُمْ أَحْسَنُ عَمَلًا ۚ وَهُوَ الْعَزِيزُ الْغَفُورُ', translation: '[He] who created death and life to test you [as to] which of you is best in deed - and He is the Exalted in Might, the Forgiving -', tafseer: 'خلق الحياة والموت ليختبركم: أيكم أخلص لله عملاً وأصوبه سنة.' },
    { numberInSurah: 3, number: 5244, text: 'الَّذِي خَلَقَ سَبْعَ سَمَاوَاتٍ طِبَاقًا ۖ مَّا تَرَىٰ فِي خَلْقِ الرَّحْمَٰنِ مِن تَفَاوُتٍ', translation: '[And] who created seven heavens in layers. You do not see in the creation of the Most Merciful any inconsistency.', tafseer: 'خلق سبع سماوات بعضها فوق بعض بإحكام وتناسق مبهر لا خلل فيه.' },
  ],
  112: [
    { numberInSurah: 1, number: 6222, text: 'قُلْ هُوَ اللَّهُ أَحَدٌ', translation: 'Say, "He is Allah, [who is] One,', tafseer: 'قل أيها الرسول للمشركين: الله هو الواحد الأحد الذي لا شريك له.' },
    { numberInSurah: 2, number: 6223, text: 'اللَّهُ الصَّمَدُ', translation: 'Allah, the Eternal Refuge.', tafseer: 'السيد الذي كمل في سؤدده والذي تصمد إليه الخلائق في حوائجها.' },
    { numberInSurah: 3, number: 6224, text: 'لَمْ يَلِدْ وَلَمْ يُولَدْ', translation: 'He neither begets nor is born,', tafseer: 'منزه عن الولد والوالد والكفؤ.' },
    { numberInSurah: 4, number: 6225, text: 'وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ', translation: 'Nor is there to Him any equivalent."', tafseer: 'وليس له مماثل ولا نظير في ذاته أو أسمائه أو صفاته أو أفعاله سبحانه.' },
  ],
  113: [
    { numberInSurah: 1, number: 6226, text: 'قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ', translation: 'Say, "I seek refuge in the Lord of daybreak', tafseer: 'قل: ألتجئ وأعتصم برب الصبح وفالقه بنوره.' },
    { numberInSurah: 2, number: 6227, text: 'مِن شَرِّ مَا خَلَقَ', translation: 'From the evil of that which He created', tafseer: 'من كل شر يؤذي من خلق الله.' },
    { numberInSurah: 3, number: 6228, text: 'وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ', translation: 'And from the evil of darkness when it settles', tafseer: 'ومن شر الليل إذا دخل واشتد ظلامه.' },
    { numberInSurah: 4, number: 6229, text: 'وَمِن شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ', translation: 'And from the evil of the blowers in knots', tafseer: 'ومن شر الساحرات اللاتي ينفثن في عقد السحر.' },
    { numberInSurah: 5, number: 6230, text: 'وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ', translation: 'And from the evil of an envier when he envies."', tafseer: 'ومن شر كل حاسد يتمنى زوال النعمة عن غيره.' },
  ],
  114: [
    { numberInSurah: 1, number: 6231, text: 'قُلْ أَعُوذُ بِرَبِّ النَّاسِ', translation: 'Say, "I seek refuge in the Lord of mankind,', tafseer: 'قل أعتصم برب الناس وخالقهم ومدبر أمورهم.' },
    { numberInSurah: 2, number: 6232, text: 'مَلِكِ النَّاسِ', translation: 'The Sovereign of mankind,', tafseer: 'ملكهم المتصرف فيهم بما يشاء سبحانه.' },
    { numberInSurah: 3, number: 6233, text: 'إِلَٰهِ النَّاسِ', translation: 'The God of mankind,', tafseer: 'معبودهم الحق الذي لا معبود سواه.' },
    { numberInSurah: 4, number: 6234, text: 'مِن شَرِّ الْوَسْوَاسِ الْخَنَّاسِ', translation: 'From the evil of the retreating whisperer -', tafseer: 'من شر الشيطان الذي يوسوس عند الغفلة ويخنس عند ذكر الله.' },
    { numberInSurah: 5, number: 6235, text: 'الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ', translation: 'Who whispers into the breasts of mankind -', tafseer: 'الذي يبث الشكوك والشهوات في قلوب بني آدم.' },
    { numberInSurah: 6, number: 6236, text: 'مِنَ الْجِنَّةِ وَالنَّاسِ', translation: 'From among the jinn and mankind."', tafseer: 'من شياطين الإنس والجن.' },
  ],
};

/**
 * Offline download key generator & storage state
 */
export function getOfflineStorageKey(reciterId: string, surahNumber: number): string {
  return `offline_audio_${reciterId}_${surahNumber}`;
}

export function isSurahDownloaded(reciterId: string, surahNumber: number): boolean {
  try {
    const key = getOfflineStorageKey(reciterId, surahNumber);
    return localStorage.getItem(key) === 'downloaded';
  } catch {
    return false;
  }
}

export function markSurahDownloaded(reciterId: string, surahNumber: number, state: boolean): void {
  try {
    const key = getOfflineStorageKey(reciterId, surahNumber);
    if (state) {
      localStorage.setItem(key, 'downloaded');
      localStorage.setItem(`${key}_timestamp`, Date.now().toString());
    } else {
      localStorage.removeItem(key);
      localStorage.removeItem(`${key}_timestamp`);
    }
  } catch (e) {
    console.error('Storage error', e);
  }
}
