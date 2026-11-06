package com.sawan4art.noor

import com.sawan4art.noor.domain.mushaf.MushafPages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * منطق صفحات المصحف — الترقيم القياسي ٦٠٤ والمسار داخل المرفقات
 * (alpha53 توجيه المالك: «كل الصفحات والتفاسير تكون داخل الكود علشان
 * ميظهرش ابدا ان الصفحه بتحمل» — الصفحات مرفق WebP لا روابط شبكة).
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
    fun `مسار الصفحة داخل مرفقات التطبيق بصيغة webp`() {
        assertEquals("mushaf_pages/1.webp", MushafPages.assetPath(1))
        assertEquals("mushaf_pages/50.webp", MushafPages.assetPath(50))
        assertEquals("mushaf_pages/604.webp", MushafPages.assetPath(604))
        assertEquals("mushaf_pages/1.webp", MushafPages.assetPath(0))
        assertEquals("mushaf_pages/604.webp", MushafPages.assetPath(1000))
    }

    @Test
    fun `لا روابط شبكة بعدما — الصفحات داخل التطبيق`() {
        // حارس رجعي: أي إضافة لروابط شبكة في MushafPages تكسر الاختبار
        val methods = MushafPages::class.java.methods.map { it.name }
        assertFalse("primaryUrl قديمة", methods.contains("primaryUrl"))
        assertFalse("fallbackUrl قديمة", methods.contains("fallbackUrl"))
    }
}
