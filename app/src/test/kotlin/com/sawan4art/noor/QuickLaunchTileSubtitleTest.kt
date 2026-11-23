package com.sawan4art.noor

import com.sawan4art.noor.util.QuickLaunch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقود نصّ البلاطة السريعة — v1.81 (بلاغ المالك: «انا عايز ايقونه
 * شريط اشعارات زي اللي موجودين في الصوره مش مجرد اشعار ثابت الاشعار
 * الثابت ده شيله Quick Settings Tile يكون داعم ليها»):
 *
 * البلاطة تعرض لحظة سحب الشريط «متبقي حتى أذان الفلان» بالأرقام
 * العربية، والعقد نقيٌّ بزمنِ الآن ممرراً كي تُختبر عقاربه:
 * ١. لحظة مستقبلية → النص بالصلاة والمتبقي بالأرقام العربية.
 * ٢. لحظة ماضية أو اسم فارغ → نصٌّ عامٌّ بلا عدّادٍ على صفر.
 * ٣. ما زاد على دقيقةٍ يُقرَّب لأعلى ثانيةٍ فلا يعرض صفراً متجاهلاً
 *    البقايا، والثواني تظهر بصيغة الساعة الكاملة كما في الشاشة.
 */
class QuickLaunchTileSubtitleTest {

    private val now = 1_766_000_000_000L // لحظة ثابتة للاختبار

    @Test
    fun `لحظة مستقبلية تعرض الصلاة والمتبقي بأرقام عربية`() {
        val afterFiveMinutes = now + 5 * 60_000L
        val text = QuickLaunch.tileSubtitle("العصر", afterFiveMinutes, now)
        assertTrue("النص يجب أن يسمي الصلاة: $text", text.contains("العصر"))
        assertTrue("النص يجب أن يقول متبقي: $text", text.startsWith("متبقي حتى أذان"))
        // لا أرقام لاتينية — العدّاد بالأرقام العربية كعرف التطبيق
        assertTrue(
            "يجب ألا تبقى أرقام لاتينية: $text",
            text.none { it in '0'..'9' },
        )
    }

    @Test
    fun `لحظة ماضية تعطي النص العام بلا عدّاد صفر`() {
        val past = now - 1_000L
        assertEquals(
            "الوقت المتبقي — اضغط للفتح",
            QuickLaunch.tileSubtitle("العصر", past, now),
        )
        assertEquals(
            "الوقت المتبقي — اضغط للفتح",
            QuickLaunch.tileSubtitle("", now + 60_000L, now),
        )
        // اللحظة الحية بالضبط (نفس الآن) عامةٌ كذلك
        assertEquals(
            "الوقت المتبقي — اضغط للفتح",
            QuickLaunch.tileSubtitle("المغرب", now, now),
        )
    }

    @Test
    fun `البقية تحت الدقيقة تقرب لأعلى فلا صفر متجاهل`() {
        // ٥٩ ثانية متبقية — تعرض ٠٠:٠٠:٥٩ (تقريب البقية ٥٩٫٩٩ يبقى ٥٩)
        val text = QuickLaunch.tileSubtitle("الفجر", now + 59_500L, now)
        assertTrue("نص الدقيقة الأخيرة: $text", text.contains("٥٩"))
        // ثانٍ واحد يعرض ٠٠:٠٠:٠١ وليس صفراً
        val one = QuickLaunch.tileSubtitle("الفجر", now + 1_000L, now)
        assertTrue("ثانٍ واحد: $one", one.contains("١") && !one.contains("٠٠:٠٠:٠٠"))
    }
}
