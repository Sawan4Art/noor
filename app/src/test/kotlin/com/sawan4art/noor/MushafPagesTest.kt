package com.sawan4art.noor

import com.sawan4art.noor.domain.mushaf.MushafPages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * منطق صفحات المصحف — الترقيم القياسي ٦٠٤ وروابط الشبكة الأساسية
 * والاحتياطية (alpha49 توجيه المالك: المصحف حفص مجوّد بالتجويد الملون).
 */
class MushafPagesTest {

    @Test
    fun `عدد صفحات المصحف يساوي ستمئة وأربعة`() {
        assertEquals(604, MushafPages.PAGE_COUNT)
    }

    @Test
    fun `تقييد رقم الصفحة داخل النطاق`() {
        assertEquals(1, MushafPages.clampPage(-3))
        assertEquals(1, MushafPages.clampPage(0))
        assertEquals(1, MushafPages.clampPage(1))
        assertEquals(50, MushafPages.clampPage(50))
        assertEquals(604, MushafPages.clampPage(604))
        assertEquals(604, MushafPages.clampPage(700))
    }

    @Test
    fun `الرابط الأساسي يحمل رقم الصفحة ومسار مصحف التجويد`() {
        val url = MushafPages.primaryUrl(50)
        assertTrue(url.startsWith("https://cdn.jsdelivr.net/"))
        assertTrue(url.endsWith("images_1053/50.jpg"))
    }

    @Test
    fun `الرابط الاحتياطي أرشيف جيتهاب الخام لنفس الصفحة`() {
        val url = MushafPages.fallbackUrl(604)
        assertTrue(url.startsWith("https://raw.githubusercontent.com/"))
        assertTrue(url.endsWith("images_1053/604.jpg"))
    }

    @Test
    fun `كل صفحة لها رابطان أساسي واحتياطي مختلفان`() {
        for (page in 1..MushafPages.PAGE_COUNT) {
            val primary = MushafPages.primaryUrl(page)
            val fallback = MushafPages.fallbackUrl(page)
            assertTrue(primary.endsWith("/$page.jpg"))
            assertTrue(fallback.endsWith("/$page.jpg"))
            assertNotEquals(primary, fallback)
        }
    }

    @Test
    fun `الروابط الخارجة عن النطاق تُقيَّد أيضاً`() {
        assertTrue(MushafPages.primaryUrl(0).endsWith("/1.jpg"))
        assertTrue(MushafPages.fallbackUrl(0).endsWith("/1.jpg"))
        assertTrue(MushafPages.primaryUrl(1000).endsWith("/604.jpg"))
        assertTrue(MushafPages.fallbackUrl(1000).endsWith("/604.jpg"))
    }
}
