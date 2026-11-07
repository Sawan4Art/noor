package com.sawan4art.noor

import com.sawan4art.noor.domain.mushaf.QuranAudioMath
import com.sawan4art.noor.domain.mushaf.QuranReciters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبارات شيوخ التلاوة العشرين (alpha54) — عقد المالك: عشرة مصريين
 * وعشرة من غيرهم بينهم ناصر القطامي وياسر الدوسري، وأسماء الملفات
 * الموحدة «٣ سورة + ٣ آية».
 */
class QuranRecitersTest {

    @Test
    fun `القائمة عشرون شيخا كما أمر المالك`() {
        assertEquals(20, QuranReciters.COUNT)
        assertEquals(20, QuranReciters.ALL.size)
    }

    @Test
    fun `عشرة مصريين وعشرة من غير المصريين`() {
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
        assertEquals(20, QuranReciters.ALL.map { it.id }.toSet().size)
        assertEquals(20, QuranReciters.ALL.map { it.folder }.toSet().size)
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
    fun `المعرّف الغريب يسير بأمان إلى الافتراضي`() {
        assertEquals("alafasy", QuranReciters.DEFAULT_ID)
        assertEquals("alafasy", QuranReciters.byId("alafasy").id)
        assertEquals("alafasy", QuranReciters.byId("ghost").id)
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
    fun `مجلد بو خاطر الصحيح بعلامتين — إصلاح رابط 404`() {
        // بلاغ المالك alpha56: «تأكد من روابط تحميل اصوات الشيوخ» —
        // المجلد القديم Salah_… كان 404 فكان تنزيله لا يعمل أبداً
        val bukhatir = QuranReciters.byId("bukhatir")
        assertTrue("المجلد يبدأ Salaah بحرفين: ${bukhatir.folder}", bukhatir.folder.startsWith("Salaah_"))
        assertEquals(
            "https://everyayah.com/data/Salaah_AbdulRahman_Bukhatir_128kbps/001001.mp3",
            QuranReciters.ayahUrl(bukhatir, 1, 1),
        )
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
    fun `إصدارات الاحتياط مضبوطة للشيوخ الذين لهم نظير`() {
        assertEquals("ar.abdulbasitmurattal", QuranReciters.byId("abdelbaset").fallbackEdition)
        assertEquals("ar.minshawi", QuranReciters.byId("minshawi").fallbackEdition)
        assertEquals("ar.husary", QuranReciters.byId("husary").fallbackEdition)
        assertEquals("ar.mahermuaiqly", QuranReciters.byId("muaiqly").fallbackEdition)
        assertEquals("ar.abdurrahmaansudais", QuranReciters.byId("sudais").fallbackEdition)
        assertEquals("ar.hanirifai", QuranReciters.byId("rifai").fallbackEdition)
        assertEquals("ar.saoodshuraym", QuranReciters.byId("shuraym").fallbackEdition)
        assertEquals("ar.aymanswoaid", QuranReciters.byId("sowaid").fallbackEdition)
        assertEquals("ar.muhammadjibreel", QuranReciters.byId("jibreel").fallbackEdition)
        // ومن لا نظير له إصداره null
        assertTrue(
            listOf("mustafa_ismail", "al_banna", "tablaway", "hajjaj", "neana",
                "qatami", "dossary", "juhany", "bukhatir", "tunaiji")
                .all { QuranReciters.byId(it).fallbackEdition == null },
        )
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
}
