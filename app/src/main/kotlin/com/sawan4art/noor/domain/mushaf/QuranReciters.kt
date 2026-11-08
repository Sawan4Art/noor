package com.sawan4art.noor.domain.mushaf

/**
 * شيوخ التلاوة — تسعة عشر شيخاً (alpha60): توجيه المالك «يبقى نشيل
 * الشيخ مصطفى اسماعيل من التطبيق خالص طالما مش مكتمل اصواته» فحُذف
 * من العشرين، وبقيت التسعة المصريين والعشرة من سواهم. وكلٌّ منهم
 * معرّفه ومجلد صوته في مصدر التلاوات المفتوح (everyayah) الذي
 * تُنزَّل منه الآيات ملفاً ملفاً بالجهاز نفسه.
 *
 * الترتيب: المصريون أولاً بعشرتهم (تسعةٌ الآن) ثم غير المصريين —
 * والملفات بصيغة «٣ أرقام للسورة + ٣ أرقام للآية» (QuranAudioMath)
 * موحّدة عند كل الشيوخ فالتنزيل والتشغيل والمسح بلا استثناءات.
 *
 * ومعرّف الشيخ المحذوف (mustafa_ismail) المخزَّن في نسخةٍ قديمة يسير
 * بأمان: byId يعود به إلى الافتراضي العفاسي.
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
        /**
         * مصدر احتياطي للتنزيل (alpha56) — إصدار الشيخ في مكتبة
         * islamic.network إن وُجد، بترقيم الآية الكلي (١..٦٢٣٦)؛
         * null لمن لا نظير له فالتنزيل من everyayah وحده.
         */
        val fallbackEdition: String? = null,
    )

    /** مجلد التلاوات المفتوح — مصدر الروابط والتنزيل معاً */
    const val DATA_BASE_URL = "https://everyayah.com/data"

    // ===== التسعة المصريون — أفاضل أئمة التلاوة في مصر =====

    private val EGYPTIAN = listOf(
        Reciter("abdelbaset", "عبد الباسط عبد الصمد", "Abdul_Basit_Murattal_192kbps", "مصر", Origin.EGYPTIAN, "ar.abdulbasitmurattal"),
        Reciter("minshawi", "محمد صديق المنشاوي", "Minshawy_Murattal_128kbps", "مصر", Origin.EGYPTIAN, "ar.minshawi"),
        Reciter("husary", "محمود خليل الحصري", "Husary_128kbps", "مصر", Origin.EGYPTIAN, "ar.husary"),
        Reciter("al_banna", "محمود علي البنا", "mahmoud_ali_al_banna_32kbps", "مصر", Origin.EGYPTIAN),
        Reciter("tablaway", "محمد الطبلاوي", "Mohammad_al_Tablaway_128kbps", "مصر", Origin.EGYPTIAN),
        Reciter("sowaid", "أيمن سويد", "Ayman_Sowaid_64kbps", "مصر", Origin.EGYPTIAN, "ar.aymanswoaid"),
        Reciter("hajjaj", "علي الحجاج السويسي", "Ali_Hajjaj_AlSuesy_128kbps", "مصر", Origin.EGYPTIAN),
        Reciter("neana", "أحمد نائنا", "Ahmed_Neana_128kbps", "مصر", Origin.EGYPTIAN),
        Reciter("jibreel", "محمد جبريل", "Muhammad_Jibreel_128kbps", "مصر", Origin.EGYPTIAN, "ar.muhammadjibreel"),
    )

    // ===== العشرة من غير المصريين — أفاضل القرّاء في العالم =====

    private val OTHER = listOf(
        Reciter("alafasy", "مشاري راشد العفاسي", "Alafasy_128kbps", "الكويت", Origin.OTHER, "ar.alafasy"),
        Reciter("sudais", "عبد الرحمن السديس", "Abdurrahmaan_As-Sudais_192kbps", "السعودية", Origin.OTHER, "ar.abdurrahmaansudais"),
        Reciter("muaiqly", "ماهر المعيقلي", "MaherAlMuaiqly128kbps", "السعودية", Origin.OTHER, "ar.mahermuaiqly"),
        Reciter("qatami", "ناصر القطامي", "Nasser_Alqatami_128kbps", "السعودية", Origin.OTHER),
        Reciter("dossary", "ياسر الدوسري", "Yasser_Ad-Dussary_128kbps", "السعودية", Origin.OTHER),
        Reciter("juhany", "عبد الله عواد الجهني", "Abdullaah_3awwaad_Al-Juhaynee_128kbps", "السعودية", Origin.OTHER),
        Reciter("rifai", "هاني الرفاعي", "Hani_Rifai_192kbps", "لبنان", Origin.OTHER, "ar.hanirifai"),
        // إصلاح alpha56: المجلد الصحيح «Salaah» بحرفين — الواسم القديم كان
        // 404 فكان تنزيل بو خاطر لا يعمل أبداً (بلاغ المالك: الروابط)
        Reciter("bukhatir", "صلاح بو خاطر", "Salaah_AbdulRahman_Bukhatir_128kbps", "الإمارات", Origin.OTHER),
        Reciter("tunaiji", "خليفة الطنيجي", "khalefa_al_tunaiji_64kbps", "الكويت", Origin.OTHER),
        Reciter("shuraym", "سعود الشريم", "Saood_ash-Shuraym_128kbps", "السعودية", Origin.OTHER, "ar.saoodshuraym"),
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

    /**
     * رابط الاحتياط للشيخ صاحب إصدارٍ نظير — مكتبة islamic.network
     * بترقيم الآية الكلي (١..٦٢٣٦) وهو ما تنتجه MushafDb.globalAyah.
     */
    const val FALLBACK_BASE_URL = "https://cdn.islamic.network/quran/audio/64"

    fun fallbackUrl(reciter: Reciter, globalAyah: Int): String? =
        reciter.fallbackEdition?.let { "$FALLBACK_BASE_URL/$it/$globalAyah.mp3" }

    /**
     * **مرآة جت هب** — بلاغ المالك alpha59: «عايز اخلي الاصوات دي تكون
     * موجوده على سيرفر على جت هب او على الموقع بتاع التطبيق علشان
     * نضمن ان التحميل يتم صح». فالمرآة مستودعٌ مستقل (noor-audio)
     * أصوله «مجلد الشيخ-رقم السورة.zip» وبداخل كل أصلٍ آيات السورة
     * بأسمائها المعتمدة نفسها (sssaaa.mp3) — فالتطبيق ينزّل السورة
     * ضغطة شبكة واحدة من هنا أولًا، فإن غاب الأصل أو تعذر فالطريق
     * القديم باقٍ بعده: everyayah آيةً آيةً ثم islamic.network.
     *
     * جت هب يحصر الإصدار بـ ١٠٠٠ أصلٍ فالتقسيم ثلاثة إصداراتٍ — والأصول
     * مرفوعةٌ بأماكنها من alpha59 فخريطة الإصدار **مثبَّتة يدوياً**
     * (alpha60: بعد حذف مصطفى إسماعيل لم يعد العدّ التلقائي بالفهارس
     * يطابق ما هو مرفوع — فالحجّاج مثلاً أصوله في audio-v2 لا v1):
     * ستة قراءٍ في audio-v1 وسبعة في audio-v2 وستة في audio-v3.
     */
    const val MIRROR_BASE_URL = "https://github.com/Sawan4Art/noor-audio/releases/download"

    /** إصدار المرآة للقارئ — خريطة مثبَّتة تطابق ما هو مرفوع فعلاً */
    fun mirrorRelease(reciter: Reciter): String = when (reciter.id) {
        "abdelbaset", "minshawi", "husary", "al_banna", "tablaway", "sowaid" -> "audio-v1"
        "hajjaj", "neana", "jibreel", "alafasy", "sudais", "muaiqly", "qatami" -> "audio-v2"
        else -> "audio-v3"
    }

    /** اسم أصل السورة في المرآة — «Alafasy_128kbps-002.zip» */
    fun mirrorAssetName(folder: String, sura: Int): String =
        "%s-%03d.zip".format(folder, sura.coerceIn(1, 114))

    /** رابط سورة الشيخ المضغوطة على المرآة — في إصداره الثابت */
    fun mirrorSurahUrl(reciter: Reciter, sura: Int): String =
        "$MIRROR_BASE_URL/${mirrorRelease(reciter)}/${mirrorAssetName(reciter.folder, sura)}"

    /** عدد المصريين في القائمة — تسعة بعد حذف مصطفى إسماعيل (alpha60) */
    const val EGYPTIAN_COUNT = 9

    /** عدد غير المصريين — عشرة بالضبط */
    const val OTHER_COUNT = 10

    /** المجموع الكلي — تسعة عشر شيخاً */
    const val COUNT = 19
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
     * هل اسم مُدخلٍ في ضغط المرآة آيةً سليمةً لهذه السورة؟ — عقد
     * alpha59: الاسم حرفيًا «٣ سورة + ٣ آية.mp3» وسورة المدخل هي
     * السورة المطلوبة نفسها — فكل غريبٍ (مسارات «../» أو مجلدات أو
     * امتدادات أخرى) يُرمى ولا يُستخرج أبدًا.
     */
    fun validZipEntry(name: String, sura: Int): Boolean {
        val m = ENTRY_PATTERN.matchEntire(name) ?: return false
        return (m.groupValues[1].toIntOrNull() ?: -1) == sura.coerceIn(1, 114)
    }

    /** رقم الآية من اسم مُدخلٍ سليم — للمُدخل المرفوض يعود صفر */
    fun zipEntryAyah(name: String): Int {
        val m = ENTRY_PATTERN.matchEntire(name) ?: return 0
        return m.groupValues[2].toIntOrNull() ?: 0
    }

    private val ENTRY_PATTERN = Regex("^(\\d{3})(\\d{3})\\.mp3$")

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
