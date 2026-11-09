package com.sawan4art.noor

import com.sawan4art.noor.domain.azkar.Ruqyah
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبارات الرقية الشرعية المعتمدة (alpha63) — بلاغ المالك: «عايز زر
 * اضافي شبيه بزري الصلاة على النبي و اسماء الله الحسنى تحت زر الصلاه
 * علي النبي يكون خاص بالرقيه الشرعيه المعتمده و الصحيحه».
 */
class RuqyahTest {

    @Test
    fun `مواضع الآيات سليمة داخل المصحف`() {
        // كل مواضع الرقية داخل حدود المصحف وبترتيبها
        assertEquals(9, Ruqyah.VERSES.size)
        Ruqyah.VERSES.forEach { ref ->
            assertTrue("موضع خاطئ: ${ref.title}", Ruqyah.isValid(ref))
        }
    }

    @Test
    fun `المواضع المعتمدة موجودة بالأسماء`() {
        // الفاتحة كاملة وآية الكرسي وآخر البقرة وآيات السحر والمعوذات
        assertTrue(Ruqyah.VERSES.contains(Ruqyah.FATIHAH))
        assertEquals(1..7, Ruqyah.FATIHAH.ayahs)
        assertEquals(255..255, Ruqyah.KURSI.ayahs)
        assertEquals(285..286, Ruqyah.BAKARAH_END.ayahs)
        assertEquals(7, Ruqyah.SIHR_AARAF.sura)
        assertEquals(10, Ruqyah.SIHR_YUNUS.sura)
        assertEquals(20, Ruqyah.SIHR_TAHA.sura)
        assertEquals(112..114, Ruqyah.IKHLAS.sura..Ruqyah.NAS.sura)
    }

    @Test
    fun `الأدعية الثابتة بمصادرها لا تصير فارغة`() {
        assertEquals(4, Ruqyah.DUAS.size)
        Ruqyah.DUAS.forEach { dua ->
            assertTrue(dua.text.isNotBlank())
            assertTrue("بلا مصدر: ${dua.text.take(20)}", dua.source.isNotBlank())
        }
        // والأدعية المعتمدة موجودة بنصوصها
        assertTrue(Ruqyah.DUAS.any { it.text.contains("بِكَلِمَاتِ اللهِ التَّامَّاتِ") })
        assertTrue(Ruqyah.DUAS.any { it.text.contains("بِسْمِ اللهِ أَرْقِيكَ") })
        assertTrue(Ruqyah.DUAS.any { it.text.contains("أَذْهِبِ الْبَاسَ") })
        assertTrue(Ruqyah.DUAS.any { it.text.contains("وَقُدْرَتِهِ") })
    }

    @Test
    fun `كتل الشاشة تغطي الرقية كاملة`() {
        // مقدمة + فاتحة + كرسي وآخر البقرة + السحر + المعوذات + الأدعية
        assertEquals(6, Ruqyah.BLOCKS.size)
        val ids = Ruqyah.BLOCKS.map { it.id }
        assertEquals(listOf("intro", "fatiha", "kursi_end", "sihr", "muawwizat", "duas"), ids)
        // كل آيات الرقية موزعة على الكتل
        val blockVerses = Ruqyah.BLOCKS.flatMap { it.verses }
        assertEquals(Ruqyah.VERSES.toSet(), blockVerses.toSet())
        // ٧+١+٢+٣+٢+١+٤+٥+٦ = ٣١ آية
        assertEquals(31, Ruqyah.verseCount())
    }
}
