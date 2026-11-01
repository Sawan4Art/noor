package com.sawan4art.noor

import com.sawan4art.noor.domain.tasbih.MisbahaRushGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقود حارس التسرع (توجيه المالك alpha25): «عند التسرع في الضغط بسرعة
 * تفوق نطق الجمل يصدر تنبيه بعدم التسرع في التسبيح و يتوقف العداد عن العد»
 * — النقر بآلة أسرع من النطق يُكشف بالاستمرارية، وعدالة التسبيح لا تُلمس.
 */
class MisbahaRushGuardTest {

    private fun guard() = MisbahaRushGuard(
        maxAvgIntervalMs = 700L,
        triggerTaps = 6,
        cooldownMs = 3000L,
    )

    @Test
    fun `النقر بسرعة النطق الطبيعية لا يُكشف أبداً`() {
        val g = guard()
        var now = 0L
        // ٥٠ نقرة بفاصل ثانية كاملة — أسرع من نطق معظم الجمل
        repeat(50) {
            assertEquals(MisbahaRushGuard.Verdict.COUNTED, g.onTap(now))
            now += 1000L
        }
        assertFalse(g.isPaused(now))
    }

    @Test
    fun `ست نقرات متواصلة كل نصف ثانية تكشف التسرع`() {
        val g = guard()
        var now = 0L
        repeat(5) {
            assertEquals(MisbahaRushGuard.Verdict.COUNTED, g.onTap(now))
            now += 500L
        }
        // النقرة السادسة تكمل السلسلة المستمرة — التنبيه والإيقاف
        val verdict = g.onTap(now)
        assertEquals(MisbahaRushGuard.Verdict.RUSH_DETECTED, verdict)
        assertEquals(now + 3000L, g.pausedUntil)
    }

    @Test
    fun `فاصل بطيء واحد وسط العجلة يقطع السلسلة فلا كشف`() {
        val g = guard()
        var now = 0L
        // 3 سريعة + بطيئة + 3 سريعة = 7 نقرات بلا استمرارية كاملة
        val intervals = listOf(400L, 400L, 400L, 1200L, 400L, 400L, 400L)
        for (dt in intervals) {
            now += dt
            assertEquals(MisbahaRushGuard.Verdict.COUNTED, g.onTap(now))
        }
        assertFalse(g.isPaused(now))
    }

    @Test
    fun `النقر أثناء الإيقاف مُهمَل ولا يمدّ فترة الإيقاف`() {
        val g = guard()
        var now = 0L
        repeat(5) { g.onTap(now); now += 500L }
        // النقرة السادسة على الفاصل نفسه (500ms) تكمل السلسلة
        assertEquals(MisbahaRushGuard.Verdict.RUSH_DETECTED, g.onTap(now))
        val end = g.pausedUntil
        // نقرات محمومة أثناء الإيقاف — كلها مُهمَلة ولا تزيد العقوبة
        // (حدّ الإيقاف حصري: last الآن يجب أن يبقى قبل end)
        while (now + 100L < end) {
            now += 100L
            assertEquals(MisbahaRushGuard.Verdict.IGNORED_PAUSED, g.onTap(now))
            assertEquals("الإيقاف ثابت لا يُمدّ", end, g.pausedUntil)
        }
    }

    @Test
    fun `بعد انقضاء الإيقاف يعود العدّ سليماً`() {
        val g = guard()
        var now = 0L
        repeat(5) { g.onTap(now); now += 500L }
        now += 500L
        g.onTap(now) // كشف
        now = g.pausedUntil + 1L
        assertEquals(MisbahaRushGuard.Verdict.COUNTED, g.onTap(now))
    }

    @Test
    fun `بعد الكشف يبدأ سجل السلاسل من جديد لا يحمل العجلة القديمة`() {
        val g = guard()
        var now = 0L
        repeat(5) { g.onTap(now); now += 500L }
        now += 500L
        g.onTap(now) // كشف ومسح السجل
        // نقرتان سريعتان بعد الاستئناف — لا سبب لكشفٍ فوري
        now = g.pausedUntil + 100L
        assertEquals(MisbahaRushGuard.Verdict.COUNTED, g.onTap(now))
        now += 400L
        assertEquals(MisbahaRushGuard.Verdict.COUNTED, g.onTap(now))
        assertFalse(g.isPaused(now))
    }

    @Test
    fun `التطبيع يصفّر الإيقاف والسجل`() {
        val g = guard()
        var now = 0L
        repeat(6) { g.onTap(now); now += 500L }
        g.reset()
        assertEquals(0L, g.pausedUntil)
        assertFalse(g.isPaused(now))
        assertEquals(MisbahaRushGuard.Verdict.COUNTED, g.onTap(now))
    }

    @Test
    fun `نقرتان في اللحظة نفسها فاصل صفر عدّه أسرع من أي نطق`() {
        val g = guard()
        assertEquals(MisbahaRushGuard.Verdict.COUNTED, g.onTap(0L))
        // النقرة المزدوجة في المللي نفسه تُسجّل — لا تُهمَل (لسنا في إيقاف)
        val v2 = g.onTap(0L)
        assertTrue(
            v2 == MisbahaRushGuard.Verdict.COUNTED ||
                v2 == MisbahaRushGuard.Verdict.RUSH_DETECTED,
        )
    }
}
