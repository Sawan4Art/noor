package com.sawan4art.noor

import com.sawan4art.noor.util.QuickLaunch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

/**
 * عقود نصّ البلاطة السريعة — v1.85 (بلاغ المالك: «خلي بلاطه
 * الاعدادات السريعه تعرض وقت الصلاه القادمه بدل عرض الوقت المتبقي»):
 *
 * البلاطة تعرض لحظة سحب الشريط **وقت الأذان القادم نفسه** «أذان
 * العصر ٤:١٥ م» لا عدّاد المتبقي — الرقم الثابت يُقرأ مرةً ويفهم
 * مرةً. والعقد نقيٌّ بزمنِ الآن ومنطقةٍ ممرّرين كي تُختبر عقاربه:
 * ١. لحظة مستقبلية → «أذان الفلان H:MM ص/م» بأرقامٍ عربيةٍ وبقرابة
 *    العرض لأعلى الدقيقة (قاعدة alpha63: المتأخر أسلم من المقدَّم).
 * ٢. لحظة ماضية أو اسم فارغ → نصٌّ عامٌّ بلا وقتٍ على صفر.
 * ٣. لا أرقامٍ لاتينية — صيغة المواقيت نفسها بالأرقام العربية.
 */
class QuickLaunchTileSubtitleTest {

    private val now = 1_766_000_000_000L // 2025-12-17T19:33:20Z — لحظة ثابتة للاختبار
    private val utc = ZoneId.of("UTC")

    @Test
    fun `لحظة مستقبلية تعرض وقت الأذان القادم لا المتبقي`() {
        // 19:38:20 بالضبط — قرابة العرض لأعلى الدقيقة تعطي 7:39 م
        val at = now + 5 * 60_000L
        val text = QuickLaunch.tileSubtitle("العصر", at, now, utc)
        assertEquals("أذان العصر ٧:٣٩ م", text)
    }

    @Test
    fun `الدقيقة الكاملة تمر بلا قرابة`() {
        // 19:40:00 بالضبط — لا ثوانٍ فلا قرابة، تظهر كما هي 7:40 م
        val at = now + (6 * 60 + 40) * 1000L
        val text = QuickLaunch.tileSubtitle("العشاء", at, now, utc)
        assertEquals("أذان العشاء ٧:٤٠ م", text)
    }

    @Test
    fun `لحظة ماضية أو فارغ الاسم تعطي النص العام بلا وقت`() {
        val past = now - 1_000L
        assertEquals(
            "وقت الصلاة القادمة — اضغط للفتح",
            QuickLaunch.tileSubtitle("العصر", past, now, utc),
        )
        assertEquals(
            "وقت الصلاة القادمة — اضغط للفتح",
            QuickLaunch.tileSubtitle("", now + 60_000L, now, utc),
        )
        // اللحظة الحية بالضبط (نفس الآن) عامةٌ كذلك
        assertEquals(
            "وقت الصلاة القادمة — اضغط للفتح",
            QuickLaunch.tileSubtitle("المغرب", now, now, utc),
        )
    }

    @Test
    fun `لا أرقام لاتينية في نص العرض أبداً — صيغة المواقيت العربية`() {
        val at = now + 5 * 60_000L
        val text = QuickLaunch.tileSubtitle("الفجر", at, now, utc)
        assertTrue(
            "يجب أن تظهر أرقامٌ عربيةٌ هندية: $text",
            text.any { it in '٠'..'٩' },
        )
        assertFalse(
            "يُمنع رجوع العدّاد اللاتيني: $text",
            text.any { it in '0'..'9' },
        )
        assertTrue("لا بدّ من مؤشر ص/م: $text", text.endsWith("م") || text.endsWith("ص"))
    }
}
