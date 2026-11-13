package com.sawan4art.noor

import com.sawan4art.noor.data.mushaf.QuranAudioLibrary
import com.sawan4art.noor.domain.mushaf.QuranAudioMath
import com.sawan4art.noor.domain.mushaf.QuranReciters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبارات شيوخ التلاوة — **عشرة بأمر المالك** (بلاغ 2026-11-13: «عايز
 * فقط الاصوات التاليه هي اللي تبقى موجه في التطبيق») بعد أن كانت
 * تسعة عشر — والعقد: أسماء الملفات الموحدة «٣ سورة + ٣ آية» وخريطة
 * إصدارات المرآة المثبّتة ومعرّفات المحذوفين تسير بأمانٍ للافتراضي.
 */
class QuranRecitersTest {

    @Test
    fun `القائمة عشرة شيوخ بأمر المالك`() {
        assertEquals(10, QuranReciters.COUNT)
        assertEquals(10, QuranReciters.ALL.size)
        // المحذوفون الثلاثة عشر لا يظهرون أبداً بالمعرّف ولا بالاسم
        val removed = listOf(
            "mustafa_ismail", "sowaid", "hajjaj", "neana", "jibreel",
            "juhany", "rifai", "bukhatir", "tunaiji", "shuraym",
        )
        assertTrue(QuranReciters.ALL.none { it.id in removed })
        for (name in listOf(
            "مصطفى إسماعيل", "أيمن سويد", "الحجاج", "نائنا",
            "جبريل", "الجهني", "الرفاعي", "بو خاطر", "الطنيجي", "الشريم",
        )) {
            assertTrue(QuranReciters.ALL.none { it.name.contains(name) })
        }
    }

    @Test
    fun `العشرة المطلوبة بأسمائهم وترتيبهم حرفيا`() {
        // بلاغ المالك 2026-11-13 — القائمة بترتيبه حرفياً
        assertEquals(
            listOf(
                "abdelbaset", "minshawi", "husary", "al_banna", "tablaway",
                "qatami", "dossary", "sudais", "muaiqly", "alafasy",
            ),
            QuranReciters.ALL.map { it.id },
        )
        for (name in listOf(
            "عبد الباسط", "المنشاوي", "الحصري", "البنا", "الطبلاوي",
            "القطامي", "الدوسري", "السديس", "المعيقلي", "العفاسي",
        )) {
            assertTrue(QuranReciters.ALL.any { it.name.contains(name) })
        }
    }

    @Test
    fun `خمسة مصريين وخمسة من غير المصريين`() {
        val egyptian = QuranReciters.ALL.filter { it.origin == QuranReciters.Origin.EGYPTIAN }
        val other = QuranReciters.ALL.filter { it.origin == QuranReciters.Origin.OTHER }
        assertEquals(QuranReciters.EGYPTIAN_COUNT, egyptian.size)
        assertEquals(QuranReciters.OTHER_COUNT, other.size)
        assertTrue(egyptian.all { it.country == "مصر" })
    }

    @Test
    fun `القطامي والدوسري والعفاسي بين الشيوخ`() {
        val names = QuranReciters.ALL.map { it.name }
        assertTrue(names.any { it.contains("القطامي") })
        assertTrue(names.any { it.contains("الدوسري") })
        assertTrue(names.any { it.contains("العفاسي") })
        assertTrue(names.any { it.contains("عبد الباسط") })
        assertTrue(names.any { it.contains("المنشاوي") })
    }

    @Test
    fun `المعرّفات والمجلدات فريدة`() {
        assertEquals(10, QuranReciters.ALL.map { it.id }.toSet().size)
        assertEquals(10, QuranReciters.ALL.map { it.folder }.toSet().size)
        assertTrue(QuranReciters.ALL.all { it.id.isNotBlank() && it.folder.isNotBlank() })
        assertTrue(QuranReciters.ALL.all { !it.folder.contains(' ') || it.folder.contains("_") })
    }

    @Test
    fun `المصريون أولا في الترتيب المعتمد`() {
        assertEquals(
            QuranReciters.Origin.EGYPTIAN,
            QuranReciters.ALL.first().origin,
        )
        assertEquals(
            QuranReciters.Origin.OTHER,
            QuranReciters.ALL.last().origin,
        )
    }

    @Test
    fun `المعرّف الغريب ومعرّفات النسخ المحذوفة تسير بأمان إلى الافتراضي`() {
        assertEquals("alafasy", QuranReciters.DEFAULT_ID)
        assertEquals("alafasy", QuranReciters.byId("alafasy").id)
        assertEquals("alafasy", QuranReciters.byId("ghost").id)
        // شيوخٌ حُذفوا بأمر المالك 2026-11-13 — إعدادُ من اختارهم في
        // نسخةٍ قديمة يسير بأمانٍ إلى الافتراضي العفاسي
        for (old in listOf(
            "mustafa_ismail", "sowaid", "hajjaj", "neana", "jibreel",
            "juhany", "rifai", "bukhatir", "tunaiji", "shuraym",
        )) {
            assertEquals("alafasy", QuranReciters.byId(old).id)
        }
    }

    @Test
    fun `رابط الآية بصيغة ثلاثة وأ ثلاثة`() {
        val reciter = QuranReciters.byId("alafasy")
        assertEquals(
            "https://everyayah.com/data/Alafasy_128kbps/002255.mp3",
            QuranReciters.ayahUrl(reciter, 2, 255),
        )
        assertEquals(
            "https://everyayah.com/data/Alafasy_128kbps/001001.mp3",
            QuranReciters.ayahUrl(reciter, 1, 1),
        )
    }

    @Test
    fun `أعلى جودة متاحة لكل شيخ — 192 لعبد الباسط والسديس و32 للبنا`() {
        // بلاغ المالك: «اعلى جوده للصوت» — القياس الحي 2026-11-13 أثبت
        // أن هذه المجلدات هي أقصى الجودات الموجودة في المصادر المفتوحة
        assertEquals("Abdul_Basit_Murattal_192kbps", QuranReciters.byId("abdelbaset").folder)
        assertEquals("Abdurrahmaan_As-Sudais_192kbps", QuranReciters.byId("sudais").folder)
        assertEquals("mahmoud_ali_al_banna_32kbps", QuranReciters.byId("al_banna").folder)
        for (id in listOf("minshawi", "husary", "tablaway", "qatami", "dossary", "muaiqly", "alafasy")) {
            assertTrue(
                "الشيخ $id على 128kbps: ${QuranReciters.byId(id).folder}",
                QuranReciters.byId(id).folder.contains("128"),
            )
        }
    }

    @Test
    fun `روابط الاحتياط من مكتبة القرآن المفتوحة للشيخ صاحبها فقط`() {
        // القطامي بلا نظيرٍ هناك — فلا رابط احتياط له
        assertEquals(null, QuranReciters.fallbackUrl(QuranReciters.byId("qatami"), 262))
        assertEquals(null, QuranReciters.fallbackUrl(QuranReciters.byId("dossary"), 262))
        // العفاسي بنظيره ar.alafasy وبترقيم الآية الكلي
        assertEquals(
            "https://cdn.islamic.network/quran/audio/64/ar.alafasy/262.mp3",
            QuranReciters.fallbackUrl(QuranReciters.byId("alafasy"), 262),
        )
        // والعفاسي يظل رابطه الأساسي من everyayah
        assertTrue(QuranReciters.ayahUrl(QuranReciters.byId("alafasy"), 2, 255).startsWith("https://everyayah.com"))
    }

    @Test
    fun `إصدارات الاحتياط مضبوطة للشيوخ الذين لهم نظير حي`() {
        assertEquals("ar.abdulbasitmurattal", QuranReciters.byId("abdelbaset").fallbackEdition)
        assertEquals("ar.minshawi", QuranReciters.byId("minshawi").fallbackEdition)
        assertEquals("ar.husary", QuranReciters.byId("husary").fallbackEdition)
        assertEquals("ar.mahermuaiqly", QuranReciters.byId("muaiqly").fallbackEdition)
        assertEquals("ar.abdurrahmaansudais", QuranReciters.byId("sudais").fallbackEdition)
        // ومن لا نظير حيٍّ له إصداره null — القياس الحي 2026-11-13 أثبت
        // أن إصدارات البنا والطبلاوي والقطامي في المكتبة المفتوحة ميتة
        // 403 على كل الجودات (١٢٨ و٦٤ و٣٢) فلا احتياطَ لهم
        assertTrue(
            listOf("al_banna", "tablaway", "qatami", "dossary")
                .all { QuranReciters.byId(it).fallbackEdition == null },
        )
    }

    @Test
    fun `مرآة جت هب رابطها أصل السورة المضغوطة في إصداره — بلاغ alpha59`() {
        // بلاغ المالك: «عايز اخلي الاصوات دي تكون موجوده على سيرفر على
        // جت هب علشان نضمن ان التحميل يتم صح» — فالسورة أصلٌ واحد
        assertEquals(
            "https://github.com/Sawan4Art/noor-audio/releases/download/audio-v1/Abdul_Basit_Murattal_192kbps-002.zip",
            QuranReciters.mirrorSurahUrl(QuranReciters.byId("abdelbaset"), 2),
        )
        // جت هب يحصر ١٠٠٠ أصلٍ للإصدار — فالعفاسي (العاشر) في audio-v2
        assertEquals(
            "https://github.com/Sawan4Art/noor-audio/releases/download/audio-v2/Alafasy_128kbps-001.zip",
            QuranReciters.mirrorSurahUrl(QuranReciters.byId("alafasy"), 1),
        )
        // والدوسري وحده في audio-v3
        assertEquals(
            "https://github.com/Sawan4Art/noor-audio/releases/download/audio-v3/Yasser_Ad-Dussary_128kbps-114.zip",
            QuranReciters.mirrorSurahUrl(QuranReciters.byId("dossary"), 114),
        )
        assertEquals("Alafasy_128kbps-001.zip", QuranReciters.mirrorAssetName("Alafasy_128kbps", 1))
        // خارج النطاق يسير بأمان إلى حدوده
        assertEquals("Alafasy_128kbps-001.zip", QuranReciters.mirrorAssetName("Alafasy_128kbps", 0))
        assertEquals("Alafasy_128kbps-114.zip", QuranReciters.mirrorAssetName("Alafasy_128kbps", 200))
        // الخريطة المثبّتة بعد تقليص المالك للقائمة (2026-11-13):
        // خمسة في v1 وأربعة في v2 والدوسري وحده في v3
        assertEquals(5, QuranReciters.ALL.count { QuranReciters.mirrorRelease(it) == "audio-v1" })
        assertEquals(4, QuranReciters.ALL.count { QuranReciters.mirrorRelease(it) == "audio-v2" })
        assertEquals(1, QuranReciters.ALL.count { QuranReciters.mirrorRelease(it) == "audio-v3" })
        assertEquals("audio-v1", QuranReciters.mirrorRelease(QuranReciters.byId("al_banna")))
        assertEquals("audio-v2", QuranReciters.mirrorRelease(QuranReciters.byId("qatami")))
        assertEquals("audio-v3", QuranReciters.mirrorRelease(QuranReciters.byId("dossary")))
        // وكل الشيوخ مجلداتهم بلا فراغات فأسماء الأصول روابط سليمة
        assertTrue(QuranReciters.ALL.all { !it.folder.contains(' ') })
    }
}

/** اختبارات رياضيات ملفات صوت الآيات (alpha54) */
class QuranAudioMathTest {

    @Test
    fun `اسم ملف الآية ثلاث خانات للسورة وثلاث للآية`() {
        assertEquals("001001.mp3", QuranAudioMath.ayahFileName(1, 1))
        assertEquals("002255.mp3", QuranAudioMath.ayahFileName(2, 255))
        assertEquals("114006.mp3", QuranAudioMath.ayahFileName(114, 6))
    }

    @Test
    fun `خارج النطاق يسير بأمان`() {
        assertEquals("114001.mp3", QuranAudioMath.ayahFileName(120, 1))
        assertEquals("001001.mp3", QuranAudioMath.ayahFileName(1, -5))
    }

    @Test
    fun `اسم مجلد السورة بثلاث خانات`() {
        assertEquals("surah_001", QuranAudioMath.surahDirName(1))
        assertEquals("surah_030", QuranAudioMath.surahDirName(30))
    }

    @Test
    fun `عدد ملفات التنزيل هو عدد الآيات`() {
        assertEquals(286, QuranAudioMath.surahFileCount(2, 286))
        assertEquals(0, QuranAudioMath.surahFileCount(2, -1))
    }

    @Test
    fun `تقدير الحجم خطي في عدد الآيات`() {
        assertEquals(0L, QuranAudioMath.estimatedBytes(0))
        assertEquals(
            QuranAudioMath.AYAH_ESTIMATED_BYTES * 7,
            QuranAudioMath.estimatedBytes(7),
        )
    }

    @Test
    fun `مدخلات ضغط المرآة السليمة فقط تمر — عقد alpha59`() {
        // اسم الآية المعتمد لسورتها يقبل
        assertTrue(QuranAudioMath.validZipEntry("001001.mp3", 1))
        assertTrue(QuranAudioMath.validZipEntry("002255.mp3", 2))
        assertTrue(QuranAudioMath.validZipEntry("114006.mp3", 114))
        // سورة غير المطلوبة يُرمى
        assertTrue(!QuranAudioMath.validZipEntry("001001.mp3", 2))
        assertTrue(!QuranAudioMath.validZipEntry("003001.mp3", 2))
        // وكل الغريب: مسارات ومجلدات وامتدادات وأسماء فارغة
        assertTrue(!QuranAudioMath.validZipEntry("../evil.mp3", 2))
        assertTrue(!QuranAudioMath.validZipEntry("folder/002001.mp3", 2))
        assertTrue(!QuranAudioMath.validZipEntry("002001.mp3.bak", 2))
        assertTrue(!QuranAudioMath.validZipEntry("002001.mp4", 2))
        assertTrue(!QuranAudioMath.validZipEntry("", 2))
        assertTrue(!QuranAudioMath.validZipEntry("surah_002", 2))
        // ورقم الآية يُستخرج من الاسم السليم وحده
        assertEquals(255, QuranAudioMath.zipEntryAyah("002255.mp3"))
        assertEquals(1, QuranAudioMath.zipEntryAyah("001001.mp3"))
        assertEquals(0, QuranAudioMath.zipEntryAyah("hackers.mp3"))
    }
}

/**
 * اختبارات مسار الصوت بعد الدراسة الحية (2026-11-13) — بلاغ المالك:
 * «ادرس كويس جدا طريق تنزيل و اصوات الشيوخ الموجوده داخل تطبيقات
 * المصحف علشان التنزيل للاصوات عندنا مش شغال و مش سريع» — عقد
 * المصدر الأول (CDN ١٢٨ لمن ثبت له بالقياس الحي) وعقد الاستسلام
 * المبكر للطريق الآية-آية.
 */
class QuranAudioRouteTest {

    @Test
    fun `الأربعة المتحقق منهم لهم رابط CDN مئة وثمانية وعشرون`() {
        // القياس الحي: هذه التقديرات تخدم /128 في مكتبة القرآن المفتوحة
        // (المحذوف جبريل كان خامسهم فخرج معه من العقد بحذفه)
        val four = listOf("minshawi", "husary", "muaiqly", "alafasy")
        for (id in four) {
            val reciter = QuranReciters.byId(id)
            assertEquals(
                "https://cdn.islamic.network/quran/audio/128/${reciter.fallbackEdition}/262.mp3",
                QuranReciters.cdnAyahUrl(reciter, 262),
            )
        }
    }

    @Test
    fun `من ليست جودته العالية ثبتة فلا رابط CDN مئة وثمانية وعشرين`() {
        // عبد الباسط والسديس على 192 في everyayah أرفع من 64 المكتبة
        // والبقية بلا إصدارٍ حيٍّ أصلاً — القياس الحي أرجع 403 على /128
        for (id in listOf("abdelbaset", "sudais")) {
            assertEquals(null, QuranReciters.cdnAyahUrl(QuranReciters.byId(id), 262))
        }
        // ومن لا إصدارٍ نظير حيّ له أصلاً فالـ null دائماً
        for (id in listOf("al_banna", "tablaway", "qatami", "dossary")) {
            assertEquals(null, QuranReciters.cdnAyahUrl(QuranReciters.byId(id), 262))
        }
    }

    @Test
    fun `عدد أصحاب الجودة العالية أربعة بالضبط وكلهم أصحاب إصدار`() {
        assertEquals(4, QuranReciters.ALL.count { it.cdnHighBitrate })
        assertTrue(
            QuranReciters.ALL.filter { it.cdnHighBitrate }
                .all { it.fallbackEdition != null },
        )
    }

    @Test
    fun `الاستسلام المبكر بلا نجاح عند بلوغ الحد فقط`() {
        // نجاحٌ صفرٌ وفشلٌ بلغ الحد أو فوقه = استسلام
        assertTrue(QuranAudioLibrary.shouldAbortPerAyah(0, 8, 8))
        assertTrue(QuranAudioLibrary.shouldAbortPerAyah(0, 12, 8))
        // دون الحد لا استسلام
        assertTrue(!QuranAudioLibrary.shouldAbortPerAyah(0, 7, 8))
        // أي نجاحٍ واحدٍ يلغي الاستسلام — المصدر حيّ فالطريق يجدي
        assertTrue(!QuranAudioLibrary.shouldAbortPerAyah(1, 50, 8))
        // والحد صفر = المرور الأخير بلا استسلامٍ أبداً مهما فشل
        assertTrue(!QuranAudioLibrary.shouldAbortPerAyah(0, 1000, 0))
    }
}
