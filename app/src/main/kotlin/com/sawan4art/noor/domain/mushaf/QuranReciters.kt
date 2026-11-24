package com.sawan4art.noor.domain.mushaf

/**
 * شيوخ التلاوة — عشرةٌ بأمر المالك 2026-11-13 ثم **اثنا عشر بالمجودين**
 * (v1.85 بلاغ المالك: «انا عايز اصوات تكون مجوده ان امكن مثل الشيخ
 * المنشاوي و الحصري يعني الاصوات الموجوده مجوده ضيفها» — فأُضيف إصدارا
 * **المجود** للمنشاوي ١٩٢kbps والحصري ٦٤kbps على everyayah مع نظيريهما
 * على CDN: المنشاوي المجود ar.minshawimujawwad يخدم ٦٤ فقط قياساً حياً
 * والحصري المجود ar.husarymujawwad يخدم ١٢٨ فأصبح طريقه الأول) وبلاغ
 * الإصلاح نفسه: «اصوات الشيوخ كلها تعمل و تقبل التنزيل الا الدوسري و
 * القاطمي و البنا و الطبلاوي» — والدراسة الحية أثبتت أن هؤلاء الأربعة
 * وحدهم بلا أي إصدارٍ على islamic.network (٤٠٣ على كل النسبط) وأن
 * everyayah وأصول المرآة سليمان من الشبكة الحرة فالعطل على نوعية شبكة
 * المالك معهما — فلُقيتَ آياتُهم **استضافةً كاملةً على فروع المستودع**
 * تخدمها شبكة jsDelivr العالمية (ayah-dossary وayah-qatami وayah-banna
 * وayah-tablawy — انظر hostAyahBranch) فيصير لكل آيةٍ طريقٌ سحابيٌّ
 * ثالثٌ مستقلٌّ تماماً. (بلاغ 2026-11-13: «عايز
 * فقط الاصوات التاليه هي اللي تبقى موجه في التطبيق و في المقابل عايز
 * اعلى جوده للصوت و اعلى سرعه تحميل للاصوات») — القائمة بأسمائهم
 * بترتيبه حرفياً، وكلُّ واحدٍ منهم على **أعلى جودةٍ متاحةٍ في المصادر
 * المفتوحة الآيّة-آيّة** (١٩٢kbps لمن ثبت له و١٢٨ للبقية و٣٢ للبنا
 * الذي لا نظيرَ أعلى منه في كل الكتالوجات المفتوحة — قيساً حياً
 * 2026-11-13: إصدارات البنا والطبلاوي والقطامي في مكتبة القرآن
 * المفتوحة ميتةٌ 403 على كل الجودات فأعلى طريقٍ لهم everyayah مباشرة)،
 * **والأربعة المتحقق منهم** (المنشاوي والحصري والمعيقلي والعفاسي)
 * على CDN 128 أول طريقٍ — أسرع أربع مراتٍ قياساً حياً.
 *
 * الشيوخ المحذوفون التسعة (أيمن سويد، الحجاج، نائنا، جبريل، الجهني،
 * الرفاعي، بو خاطر، الطنيجي، الشريم) ومعرّفُهم المخزَّنُ في نسخةٍ قديمة
 * يسير بأمانٍ إلى الافتراضي العفاسي — byId يعود به دائماً.
 *
 * الترتيب: المصريون الخمسة أولاً ثم الخمسة من سواهم — والملفات بصيغة
 * «٣ أرقام للسورة + ٣ أرقام للآية» (QuranAudioMath) موحّدة عند الجميع
 * فالتنزيل والتشغيل والمسح بلا استثناءات.
 */
object QuranReciters {

    /** أصل الشيخ في القسم — المصريون خمسة وغير المصريين خمسة */
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
        /**
         * **هل إصداره في islamic.network متاحٌ بجودة 128kbps؟** — درس
         * الدراسة الحية 2026-11-13: القياس الفعلي بروابط HEAD لكل
         * التقديرات العشرة أثبت أن خمسة منها تخدم /128 بينما خمسة
         * يردّون 403 عليها (64kbps وحدهم): المنشاوي والحصري وجبريل
         * والعفاسي والمعيقلي. لهؤلاء الخمسة يكون CDN مكتبة القرآن
         * المفتوحة هو **المصدر الأول** للآيات — نفس الجودة المسجّلة
         * في مجلد everyayah (١٢٨kbps) لكن على شبكة توزيعٍ قيست أسرع
         * أربع مراتٍ من everyayah في الفحص الحي.
         */
        val cdnHighBitrate: Boolean = false,
    )

    /** مجلد التلاوات المفتوح — مصدر الروابط والتنزيل معاً */
    const val DATA_BASE_URL = "https://everyayah.com/data"

    // ===== المصريون الخمسة — بأمر المالك وترتيبه حرفياً =====

    private val EGYPTIAN = listOf(
        Reciter("abdelbaset", "عبد الباسط عبد الصمد", "Abdul_Basit_Murattal_192kbps", "مصر", Origin.EGYPTIAN, "ar.abdulbasitmurattal"),
        Reciter("minshawi", "محمد صديق المنشاوي", "Minshawy_Murattal_128kbps", "مصر", Origin.EGYPTIAN, "ar.minshawi", cdnHighBitrate = true),
        // المجود (v1.85 بلاغ المالك) — ١٩٢kbps أعلى جودةٍ له على everyayah
        // ونظيره ar.minshawimujawwad على CDN يخدم ٦٤ فقط قياساً حياً
        // (٤٠٣ على ١٢٨) فلا راية جودةٍ عاليةٍ له على CDN
        Reciter("minshawi_mujawwad", "المنشاوي المجود", "Minshawy_Mujawwad_192kbps", "مصر", Origin.EGYPTIAN, "ar.minshawimujawwad"),
        Reciter("husary", "محمود خليل الحصري", "Husary_128kbps", "مصر", Origin.EGYPTIAN, "ar.husary", cdnHighBitrate = true),
        // المجود (v1.85 بلاغ المالك) — مجلد everyayah ٦٤kbps ونظيره
        // ar.husarymujawwad يخدم ١٢٨ قياساً حياً فصار طريقه الأول
        Reciter("husary_mujawwad", "الحصري المجود", "Husary_Mujawwad_64kbps", "مصر", Origin.EGYPTIAN, "ar.husarymujawwad", cdnHighBitrate = true),
        // البنا: أعلى جودةٍ آيّة-آيّة موجودةٍ في كل المصادر المفتوحة هي
        // 32kbps — القياس الحي 2026-11-13 أثبت أن إصداره في مكتبة القرآن
        // المفتوحة (ar.mahmoudalialbanna) ميتٌ 403 على ١٢٨ و٦٤ و٣٢ جميعاً
        // فلا إصدار احتياطيّ له فالتنزيل من everyayah وحده
        Reciter("al_banna", "محمود علي البنا", "mahmoud_ali_al_banna_32kbps", "مصر", Origin.EGYPTIAN),
        // الطبلاوي: 128kbps أعلى جودةٍ له — وإصداره ar.mohamedtablawi
        // ميتٌ 403 على كل الجودات فلا إصدار احتياطيّ
        Reciter("tablaway", "محمد الطبلاوي", "Mohammad_al_Tablaway_128kbps", "مصر", Origin.EGYPTIAN),
    )

    // ===== الخمسة من غير المصريين — بأمر المالك وترتيبه حرفياً =====

    private val OTHER = listOf(
        Reciter("qatami", "ناصر القطامي", "Nasser_Alqatami_128kbps", "السعودية", Origin.OTHER),
        Reciter("dossary", "ياسر الدوسري", "Yasser_Ad-Dussary_128kbps", "السعودية", Origin.OTHER),
        Reciter("sudais", "عبد الرحمن السديس", "Abdurrahmaan_As-Sudais_192kbps", "السعودية", Origin.OTHER, "ar.abdurrahmaansudais"),
        Reciter("muaiqly", "ماهر المعيقلي", "MaherAlMuaiqly128kbps", "السعودية", Origin.OTHER, "ar.mahermuaiqly", cdnHighBitrate = true),
        Reciter("alafasy", "مشاري راشد العفاسي", "Alafasy_128kbps", "الكويت", Origin.OTHER, "ar.alafasy", cdnHighBitrate = true),
    )

    /** العشرة كاملة — المصريون الخمسة أولاً ثم الخمسة بترتيب المالك */
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
     * **مصدر الآية الأول للشيخ صاحب جودةٍ عاليةٍ على CDN** — نفس مكتبة
     * islamic.network لكن بجودة 128kbps: الدراسة الحية 2026-11-13 قاست
     * هذا الموجه أسرع أربع مراتٍ من everyayah وبنفس جودة مجلد الشيخ
     * المسجّل هنا (١٢٨kbps) — فمن ثبت أن إصداره يخدم /128 صار هذا
     * أول طريقٍ للآية عنده، ومن ليس كذلك فالـ null يقفز فوقه فوراً
     * (الخمسة المتحقق منهم: المنشاوي والحصري وجبريل والعفاسي والمعيقلي).
     */
    const val CDN_128_BASE_URL = "https://cdn.islamic.network/quran/audio/128"

    fun cdnAyahUrl(reciter: Reciter, globalAyah: Int): String? =
        if (reciter.cdnHighBitrate && reciter.fallbackEdition != null) {
            "$CDN_128_BASE_URL/${reciter.fallbackEdition}/$globalAyah.mp3"
        } else {
            null
        }

    /**
     * **هل الشيخ بلا أي طريق CDN إطلاقاً؟** — لا جودةٍ عاليةٍ على CDN
     * ([Reciter.cdnHighBitrate]) ولا إصدارَ نظيرٍ ([Reciter.fallbackEdition])
     * — وهؤلاء الأربعة بالقياس الحي: البنا والطبلاوي والقطامي والدوسري
     * (بلاغ المالك 2026-11-13: «البنا و الطبلاوي و القطامي و الدوسري كل
     * منهم اصواتهم لا تقبل التحميل و تفشل و باقي الاصوات تعمل بشكل جيد»
     * — والجذر: أنهم الوحيدون الذين طُريقُهم الآيّة-آية الوحيد everyayah،
     * فإذا تعطل على شبكة المالك مات تنزيلهم كله وآخرون نجاوا بالـ CDN).
     * فتنزيل السورة لهم يقيس **مرآة التطبيق أولاً** — أصولٌ مرفوعةٌ
     * بأماكنها تحقّقت آلياً (١١٤ أصلاً لكل واحدٍ منهم) — ثم تكملةُ
     * النواقص آيةً آيةً من everyayah بعدها. دالةٌ نقيةٌ تُختبر آلياً.
     */
    fun hasNoCdnPath(reciter: Reciter): Boolean =
        !reciter.cdnHighBitrate && reciter.fallbackEdition == null

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
     * ستة قراءٍ في audio-v1 وسبعة في audio-v2 وستة في audio-v3
     * (بعد تقليص المالك للقائمة 2026-11-13: خمسةٌ في v1 وأربعةٌ في v2
     * والدوسري وحده في v3 — الأصول المرفوعة كلها باقيةٌ كما هي).
     */
    const val MIRROR_BASE_URL = "https://github.com/Sawan4Art/noor-audio/releases/download"

    /** إصدار المرآة للقارئ — خريطة مثبَّتة تطابق ما هو مرفوع فعلاً،
     *  والفراغ لمن لا أصولَ له على المرآة (المجودان الجديدان v1.85:
     *  طريقُهما الأول CDN فالمرآة عنهما عبءٌ بلا أصل) */
    fun mirrorRelease(reciter: Reciter): String? = when (reciter.id) {
        "abdelbaset", "minshawi", "husary", "al_banna", "tablaway" -> "audio-v1"
        "alafasy", "sudais", "muaiqly", "qatami" -> "audio-v2"
        "dossary" -> "audio-v3"
        else -> null
    }

    /** اسم أصل السورة في المرآة — «Alafasy_128kbps-002.zip» */
    fun mirrorAssetName(folder: String, sura: Int): String =
        "%s-%03d.zip".format(folder, sura.coerceIn(1, 114))

    /** رابط سورة الشيخ المضغوطة على المرآة — والفراغ لمن لا أصلَ له فيها */
    fun mirrorSurahUrl(reciter: Reciter, sura: Int): String? =
        mirrorRelease(reciter)?.let {
            "$MIRROR_BASE_URL/$it/${mirrorAssetName(reciter.folder, sura)}"
        }

    // ===== استضافة الآيات على فروع المستودع (v1.85 — إصلاح الأربعة) =====

    /**
     * **فرع استضافة الآيات** للقارئ من الأربعة الذين لا طريقَ CDN
     * لهم على islamic.network إطلاقاً — أصولُ آياتهم كاملةٌ (٦٢٣٦ ملفاً
     * sssaaa.mp3 مسطّحةً) رُفعت إلى فرعٍ لكل قارئٍ في هذا المستودع
     * عبر workflow «استضافة الآيات» وتُخدم بشبكة jsDelivr العالمية
     * ببنيةٍ سحابيةٍ مستقلةٍ عن everyayah وأصول الإصدارات معاً — فالعطل
     * المبلغ (v1.85 بلاغ المالك: «الدوسري و القاطمي و البنا و الطبلاوي»)
     * كان نقصَ طرقٍ لا نقصَ ملفات: كلُّ طريقٍ سابقٍ لهؤلاء كان يمرّ
     * بنيةٍ لا تصلُه على شبكته فمات تشغيلُهم وتنزيلُهم معاً.
     * والفراغ لمن لا فرعَ استضافةٍ له (البقية طرقُهم CDN أولاً).
     */
    fun hostAyahBranch(reciter: Reciter): String? = when (reciter.id) {
        "dossary" -> "ayah-dossary"
        "qatami" -> "ayah-qatami"
        "al_banna" -> "ayah-banna"
        "tablaway" -> "ayah-tablawy"
        else -> null
    }

    /** أصل شبكة jsDelivr العالمية لفروع المستودع — بنيةٌ مستقلةٌ تماماً */
    const val HOST_AYAH_BASE_URL = "https://cdn.jsdelivr.net/gh/Sawan4Art/noor-audio@"

    /** رابط آية من فرع استضافة الآيات على jsDelivr — والفراغ لمن بلا فرع */
    fun hostedAyahUrl(reciter: Reciter, sura: Int, ayah: Int): String? =
        hostAyahBranch(reciter)?.let {
            "$HOST_AYAH_BASE_URL$it/${reciter.folder}/${QuranAudioMath.ayahFileName(sura, ayah)}"
        }

    /** أصل raw.githubusercontent — الطريق الثاني المستقلّ للفرع نفسه */
    const val HOST_AYAH_RAW_BASE_URL = "https://raw.githubusercontent.com/Sawan4Art/noor-audio/refs/heads"

    /** رابط آية من فرع الاستضافة على raw — والفراغ لمن بلا فرع */
    fun hostedAyahRawUrl(reciter: Reciter, sura: Int, ayah: Int): String? =
        hostAyahBranch(reciter)?.let {
            "$HOST_AYAH_RAW_BASE_URL/$it/${reciter.folder}/${QuranAudioMath.ayahFileName(sura, ayah)}"
        }

    /** عدد المصريين في القائمة — خمسة بأمر المالك 2026-11-13 ثم سبعة بالمجودين (v1.85) */
    const val EGYPTIAN_COUNT = 7

    /** عدد غير المصريين — خمسة بالضبط */
    const val OTHER_COUNT = 5

    /** المجموع الكلي — عشرة شيخاً بأمر المالك ثم اثنا عشر بالمجودين (v1.85) */
    const val COUNT = 12
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
