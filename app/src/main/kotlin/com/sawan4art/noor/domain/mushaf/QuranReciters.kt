package com.sawan4art.noor.domain.mushaf

/**
 * شيوخ التلاوة العشرون — توجيه المالك ٢٠٢٦-١١-٠٦:
 * «اضف عشرين مقرء يتم تحميل اصواتهم من قبل المستخدم نفسه منهم افضل
 * عشر مقرئيين مصريين و الباقي افضل عشره منهم ناصر القطامي و ياسر
 * الدوسري» — عشرة من أفاضل القرّاء المصريين وعشرة من أفاضل غيرهم،
 * وكلٌّ منهم معرّفه ومجلد صوته في مصدر التلاوات المفتوح (everyayah)
 * الذي تُنزَّل منه الآيات ملفاً ملفاً بالجهاز نفسه.
 *
 * الترتيب: المصريون أولاً بعشرتهم المعتمدة ثم غير المصريين — والملفات
 * بصيغة «٣ أرقام للسورة + ٣ أرقام للآية» (QuranAudioMath) موحّدة عند
 * كل الشيوخ فالتنزيل والتشغيل والمسح بلا استثناءات.
 */
object QuranReciters {

    /** أصل الشيخ في القسم — المصريون عشرة وغير المصريين عشرة */
    enum class Origin(val label: String) { EGYPTIAN("مصري"), OTHER("عالمي") }

    /** الشيخ الواحد — معرّف ثابت يُخزَّن ويُقرأ من الإعدادات */
    data class Reciter(
        /** معرّف ثابت لاتيني — مفتاح الملفات والإعدادات */
        val id: String,
        /** اسمه كما يُعرف بين الناس */
        val name: String,
        /** مجلد صوته في مصدر التلاوات (everyayah/data/…) */
        val folder: String,
        /** بلد النسب — مصر أو غيرها */
        val country: String,
        val origin: Origin,
    )

    /** مجلد التلاوات المفتوح — مصدر الروابط والتنزيل معاً */
    const val DATA_BASE_URL = "https://everyayah.com/data"

    // ===== العشرة المصريون — أفاضل أئمة التلاوة في مصر =====

    private val EGYPTIAN = listOf(
        Reciter("abdelbaset", "عبد الباسط عبد الصمد", "Abdul_Basit_Murattal_192kbps", "مصر", Origin.EGYPTIAN),
        Reciter("minshawi", "محمد صديق المنشاوي", "Minshawy_Murattal_128kbps", "مصر", Origin.EGYPTIAN),
        Reciter("husary", "محمود خليل الحصري", "Husary_128kbps", "مصر", Origin.EGYPTIAN),
        Reciter("mustafa_ismail", "مصطفى إسماعيل", "Mustafa_Ismail_48kbps", "مصر", Origin.EGYPTIAN),
        Reciter("al_banna", "محمود علي البنا", "mahmoud_ali_al_banna_32kbps", "مصر", Origin.EGYPTIAN),
        Reciter("tablaway", "محمد الطبلاوي", "Mohammad_al_Tablaway_128kbps", "مصر", Origin.EGYPTIAN),
        Reciter("sowaid", "أيمن سويد", "Ayman_Sowaid_64kbps", "مصر", Origin.EGYPTIAN),
        Reciter("hajjaj", "علي الحجاج السويسي", "Ali_Hajjaj_AlSuesy_128kbps", "مصر", Origin.EGYPTIAN),
        Reciter("neana", "أحمد نائنا", "Ahmed_Neana_128kbps", "مصر", Origin.EGYPTIAN),
        Reciter("jibreel", "محمد جبريل", "Muhammad_Jibreel_128kbps", "مصر", Origin.EGYPTIAN),
    )

    // ===== العشرة من غير المصريين — أفاضل القرّاء في العالم =====

    private val OTHER = listOf(
        Reciter("alafasy", "مشاري راشد العفاسي", "Alafasy_128kbps", "الكويت", Origin.OTHER),
        Reciter("sudais", "عبد الرحمن السديس", "Abdurrahmaan_As-Sudais_192kbps", "السعودية", Origin.OTHER),
        Reciter("muaiqly", "ماهر المعيقلي", "MaherAlMuaiqly128kbps", "السعودية", Origin.OTHER),
        Reciter("qatami", "ناصر القطامي", "Nasser_Alqatami_128kbps", "السعودية", Origin.OTHER),
        Reciter("dossary", "ياسر الدوسري", "Yasser_Ad-Dussary_128kbps", "السعودية", Origin.OTHER),
        Reciter("juhany", "عبد الله عواد الجهني", "Abdullaah_3awwaad_Al-Juhaynee_128kbps", "السعودية", Origin.OTHER),
        Reciter("rifai", "هاني الرفاعي", "Hani_Rifai_192kbps", "لبنان", Origin.OTHER),
        Reciter("bukhatir", "صلاح بو خاطر", "Salah_AbdulRahman_Bukhatir_128kbps", "الإمارات", Origin.OTHER),
        Reciter("tunaiji", "خليفة الطنيجي", "khalefa_al_tunaiji_64kbps", "الكويت", Origin.OTHER),
        Reciter("shuraym", "سعود الشريم", "Saood_ash-Shuraym_128kbps", "السعودية", Origin.OTHER),
    )

    /** العشرون كاملة — المصريون أولاً ثم غيرهم بترتيب القائمتين */
    val ALL: List<Reciter> = EGYPTIAN + OTHER

    /** مقرئ التلاوة الافتراضي — العفاسي (كان هو المصدر الوحيد قبلهما) */
    const val DEFAULT_ID = "alafasy"

    /** الشيخ بالمعرّف — ومعرّف غريب يسير بأمان إلى الافتراضي */
    fun byId(id: String): Reciter =
        ALL.firstOrNull { it.id == id } ?: ALL.first { it.id == DEFAULT_ID }

    /** رابط آية واحدة للشيخ على مصدر التلاوات — «٣ سورة + ٣ آية.mp3» */
    fun ayahUrl(reciter: Reciter, sura: Int, ayah: Int): String =
        "$DATA_BASE_URL/${reciter.folder}/${QuranAudioMath.ayahFileName(sura, ayah)}"

    /** عدد المصريين في القائمة — عقد المالك: عشرة مصريين بالضبط */
    const val EGYPTIAN_COUNT = 10

    /** عدد غير المصريين — عشرة بالضبط */
    const val OTHER_COUNT = 10

    /** المجموع الكلي — عشرون كما أمر المالك */
    const val COUNT = 20
}

/**
 * رياضيات ملفات صوت الآيات — اسم الملف الموحّد «٣ أرقام للسورة + ٣
 * أرقام للآية» (البقرة ٢٥٥ = 002255.mp3) وأسماء المسارات وعدّ الملفات
 * المتوقع لكل تنزيل — وحدات نقية تُختبر آلياً بلا أندرويد.
 */
object QuranAudioMath {

    /** اسم ملف آية — السورة ثلاث خانات والآية ثلاث خانات */
    fun ayahFileName(sura: Int, ayah: Int): String {
        val s = sura.coerceIn(1, 114)
        val a = ayah.coerceAtLeast(1)
        return "%03d%03d.mp3".format(s, a)
    }

    /** اسم مجلد السورة داخل مجلد الشيخ — «surah_002» */
    fun surahDirName(sura: Int): String = "surah_%03d".format(sura.coerceIn(1, 114))

    /**
     * عدد ملفات تنزيل السورة — عدّ آياتها كما في المصحف (الفتحة ٧
     * والبقرة ٢٨٦…)، والعهد أن العدّ يأتي من قاعدة المصحف نفسها.
     */
    fun surahFileCount(sura: Int, surahAyahCount: Int): Int = surahAyahCount.coerceAtLeast(0)

    /**
     * حجم تقديري بالبايت لنصيب الآية الواحدة — مبدئيٌّ من متوسط ٧
     * ثوانٍ للآية بمعدل ١٦ كيلوبايت/ثانية (١٢٨kbps) يُستعمل للعرض
     * التقريبي فقط: «نحو ١٫٢ ميغابايت».
     */
    const val AYAH_ESTIMATED_BYTES: Long = 110_000L

    /** تقدير حجم تنزيل بالبايت — لعدد آيات معطى */
    fun estimatedBytes(ayahCount: Int): Long = ayahCount.toLong() * AYAH_ESTIMATED_BYTES
}
