package com.sawan4art.noor

import com.sawan4art.noor.data.update.ChangelogFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** اختبارات منسق «ما الجديد» — تسطيح Markdown الخفيف بلا مكتبات */
class ChangelogFormatterTest {

    @Test
    fun `النقاط والنصوص العادية تُنقل كما هي`() {
        val lines = ChangelogFormatter.bodyToLines("سطر أول\nسطر ثانٍ")
        assertEquals(listOf("سطر أول", "سطر ثانٍ"), lines)
    }

    @Test
    fun `شرطة القائمة تتحول نقطة عربية`() {
        val lines = ChangelogFormatter.bodyToLines("- إضافة نظام التحديثات\n* بند ثانٍ")
        assertEquals(listOf("• إضافة نظام التحديثات", "• بند ثانٍ"), lines)
    }

    @Test
    fun `العريض والكود والروابط تنزع وسومها`() {
        val lines = ChangelogFormatter.bodyToLines("**تحديث جديد** و `كود` و [الريليز](https://example.com)")
        assertEquals(listOf("تحديث جديد و كود و الريليز"), lines)
    }

    @Test
    fun `الصور تُحذف تماماً`() {
        val lines = ChangelogFormatter.bodyToLines("![لقطة](https://example.com/shot.png) التحديث جاهز")
        assertEquals(listOf("التحديث جاهز"), lines)
    }

    @Test
    fun `العناوين تنزع الرموز والمقتبس ينزع السهم`() {
        val lines = ChangelogFormatter.bodyToLines("## ما الجديد\n> صدقة جارية")
        assertEquals(listOf("ما الجديد", "صدقة جارية"), lines)
    }

    @Test
    fun `تعليقات HTML تُخفى حتى نهايتها`() {
        val body = "<!-- قبل\nوسط التعليق -->\nبعد التعليق"
        assertEquals(listOf("بعد التعليق"), ChangelogFormatter.bodyToLines(body))
    }

    @Test
    fun `ملاحظات GitHub الآلية تُخفى من سطر عنوانها`() {
        val body = "## ما الجديد\n- ميزة أ\n\n## What's Changed\n* commit abc by @dev\n\n## New Contributors\n* @dev"
        assertEquals(listOf("ما الجديد", "• ميزة أ"), ChangelogFormatter.bodyToLines(body))
    }

    @Test
    fun `الفراغات المتتالية تنكمش سطراً واحداً والأطراف تنظف`() {
        val lines = ChangelogFormatter.bodyToLines("\n\nأول\n\n\n\nثانٍ\n\n")
        assertEquals(listOf("أول", "", "ثانٍ"), lines)
    }

    @Test
    fun `الجسم الفارغ أو المعدوم يعطي قائمة فارغة`() {
        assertTrue(ChangelogFormatter.bodyToLines(null).isEmpty())
        assertTrue(ChangelogFormatter.bodyToLines("   \n  ").isEmpty())
    }

    @Test
    fun `أول سطر للإشعار هو أول معنوي والافتراضي عند العدم`() {
        assertEquals("• ميزة أ", ChangelogFormatter.firstLine("\n- ميزة أ\n- ميزة ب"))
        assertEquals("نسخة جديدة متاحة", ChangelogFormatter.firstLine(null))
    }

    @Test
    fun `ملخص الإشعار يحصر الأسطر ويحيل للباقي`() {
        val body = (1..9).joinToString("\n") { "بند $it" }
        val summary = ChangelogFormatter.notificationSummary(body, maxLines = 6)
        assertEquals(7, summary.lines().size)
        assertTrue(summary.endsWith("… افتح التطبيق للمزيد"))
        // عند العدد الأقل لا إحالة
        assertEquals("بند 1\nبند 2", ChangelogFormatter.notificationSummary("بند 1\nبند 2", maxLines = 6))
    }
}
