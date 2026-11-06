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
