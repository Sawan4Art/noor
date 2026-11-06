package com.sawan4art.noor

import com.sawan4art.noor.domain.mushaf.MushafJuz
import com.sawan4art.noor.domain.mushaf.MushafPages
import com.sawan4art.noor.domain.mushaf.MushafPlaylist
import com.sawan4art.noor.domain.mushaf.MushafSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبارات البحث بالكلمة والأجزاء — توجيه المالك ٢٠٢٦-١١-٠٦: «نظام بحث
 * بالكلمه في ايات المصحف و يكون نظام بحث بدون تشكيل يكفي فقط كتابه كلمه
 * واحده من قبل المستخدم» و «عايزها تكون فيها اختيار عرض اجزاء القران».
 */
class MushafSearchTest {

    /** نص مشكول حقيقي — الفاتحة ٦ (صور الديانات الحرفية كما في المصحف) */
    private val t = listOf(
        MushafSearch.Hit(1, 1, "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"),
        MushafSearch.Hit(1, 6, "ٱهْدِنَا ٱلصِّرَٰطَ ٱلْمُسْتَقِيمَ"),
        MushafSearch.Hit(1, 7, "صِرَٰطَ ٱلَّذِينَ أَنْعَمْتَ عَلَيْهِمْ"),
        MushafSearch.Hit(2, 3, "وَبِٱلْءَاخِرَةِ هُمْ يُوقِنُونَ"),
    )

    @Test
    fun `يحذف التشكيل كله`() {
        assertEquals("الصرط المستقيم", MushafSearch.normalize("ٱلصِّرَٰطَ ٱلْمُسْتَقِيمَ"))
    }

    @Test
    fun `يوحد أشكال الألف`() {
        assertEquals("اشاره", MushafSearch.normalize("إشارة"))
        assertEquals("اشاره", MushafSearch.normalize("آشَارَة"))
        assertEquals("ا", MushafSearch.normalize("أ"))
    }

    @Test
    fun `التاء المربوطة هاء والألف المقصورة ياء`() {
        assertEquals("الجنه", MushafSearch.normalize("الجَنَّة"))
        assertEquals("عليهم", MushafSearch.normalize("عَلَيْهِمْ"))
        assertEquals("الي", MushafSearch.normalize("إِلَىٰ"))
    }

    @Test
    fun `كلمة بلا تشكيل تلحق نص المصحف المشكول`() {
        val hits = MushafSearch.search("الصيرط", t)
        // «الصيرط» بالإخفاء خطأ مطبعي شائع لا تحملها الآية — والصحيح صرط
        assertEquals(0, hits.size)
        assertEquals(listOf(1 to 6, 1 to 7), MushafSearch.search("صرط", t).map { it.sura to it.ayah })
    }

    @Test
    fun `بحث كلمة الهدنا يجد آيتها بسورتها ورقمها`() {
        val hits = MushafSearch.search("اهدنا", t)
        assertEquals(1, hits.size)
        assertEquals(1, hits[0].sura)
        assertEquals(6, hits[0].ayah)
        assertTrue(hits[0].text.contains("هْدِ"))
    }

    @Test
    fun `كلمة فارغة لا نتائج لها`() {
        assertEquals(0, MushafSearch.search("", t).size)
        assertEquals(0, MushafSearch.search("   ", t).size)
    }

    @Test
    fun `السقف يحفظ النتائج`() {
        val many = (1..500).map { MushafSearch.Hit(2, it, "الكِتَابُ رقم $it") }
        assertEquals(200, MushafSearch.search("الكتاب", many).size)
    }

    /* ==================== الأجزاء الثلاثون ==================== */

    @Test
    fun `ثلاثون جزءاً بصفحات بداية مرتبة`() {
        assertEquals(30, MushafJuz.COUNT)
        assertEquals(30, MushafJuz.START_PAGES.size)
        assertTrue("البدايات ترتيب صاعد", MushafJuz.START_PAGES.toList().zipWithNext().all { (a, b) -> a < b })
        assertEquals(1, MushafJuz.START_PAGES.first())
        assertEquals(582, MushafJuz.START_PAGES.last())
    }

    @Test
    fun `صفحة بداية الجزء داخل حدود المصحف`() {
        assertEquals(1, MushafJuz.startPage(1))
        assertEquals(582, MushafJuz.startPage(30))
        assertEquals(22, MushafJuz.startPage(2))
        assertEquals(1, MushafJuz.startPage(0)) // خارج النطاق يُقيَّد لأول الأجزاء
        assertEquals(582, MushafJuz.startPage(31)) // ولآخرها
    }

    /* ==================== تكرار الحفظ ==================== */

    @Test
    fun `تلاوة الحفظ تكرر كل آية ثلاث مرات`() {
        assertEquals(listOf(5, 5, 5, 6, 6, 6), MushafPlaylist.hifzRepeats(listOf(5, 6), 3))
        assertEquals(listOf(2), MushafPlaylist.hifzRepeats(listOf(2), 1))
        assertEquals(listOf(2), MushafPlaylist.hifzRepeats(listOf(2), 0))
    }

    @Test
    fun `مسار الصفحة داخل المرفقات`() {
        assertEquals("mushaf_pages/1.webp", MushafPages.assetPath(1))
        assertEquals("mushaf_pages/604.webp", MushafPages.assetPath(604))
        assertEquals("mushaf_pages/1.webp", MushafPages.assetPath(0))
    }
}
