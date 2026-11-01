package com.sawan4art.noor

import com.sawan4art.noor.domain.tasbih.MisbahaRushGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقود حارس التسرع (توجيه المالك alpha25 وضبطه alpha26): «عند التسرع في
 * الضغط بسرعة تفوق نطق الجمل يصدر تنبيه بعدم التسرع في التسبيح و يتوقف
 * العداد عن العد» — و«رساله التنبيه بتاعت تنبيه التسرع بتظهر بسرعه خليها
 * تظهر لو المستخدم تسرع اكثر من كده» — عتبة ٤٥٠ مللي سكّة لا تلمس فتيرة
 * العجلة المعتادة (نقرة كل نصف ثانية أو أبطأ) وتكشف نقرَ الآلة الخالص.
 */
class MisbahaRushGuardTest {

    /** العتبات الافتراضية نفسها التي يعمل بها التطبيق — لا أرقام مختارة للاختبار */
    private fun guard() = MisbahaRushGuard()

    @Test
    fun `النقر بسرعة النطق الطبيعية لا يُكشف أبداً`() {
        val g = guard()
        var now = 0L
        // ٥٠ نقرة بفاصل ثانية كاملة — أبطأ من نطق أي جملة
        repeat(50) {
            assertEquals(MisbahaRushGuard.Verdict.COUNTED, g.onTap(now))
            now += 1000L
        }
        assertFalse(g.isPaused(now))
    }

    @Test
    fun `نقرة كل نصف ثانية — فتيرة العجلة المعتادة — لا تُنبه أبداً`() {
        // هذا عقد ضبط alpha26: العتبة القديمة ٧٠٠ كانت تنبه هذه الفتيرة
        // الصادقة، والجديدة ٤٥٠ تتركها سليمة مهما طالت
        val g = guard()
        var now = 0L
        repeat(30) {
            assertEquals(MisbahaRushGuard.Verdict.COUNTED, g.onTap(now))
            now += 500L
        }
        assertFalse(g.isPaused(now))
    }

    @Test
    fun `ست نقرات متواصلة بأقل من نصف ثانية تكشف التسرع`() {
        val g = guard()
        var now = 0L
        repeat(5) {
            assertEquals(MisbahaRushGuard.Verdict.COUNTED, g.onTap(now))
            now += 400L
        }
        // النقرة السادسة تكمل السلسلة المستمرة — التنبيه والإيقاف
        val verdict = g.onTap(now)
        assertEquals(MisbahaRushGuard.Verdict.RUSH_DETECTED, verdict)
        assertEquals(now + 3000L, g.pausedUntil)
    }

    @Test
    fun `حدّ العتبة نفسه ٤٥٠ مللي يكشف وما فوقه بنقرة لا يكشف`() {
        // عند الحدّ بالضبط: ٤٥٠ ليس أبطأ من العتبة — كشف
        val atLimit = guard()
        var now = 0L
        repeat(5) { atLimit.onTap(now); now += 450L }
        assertEquals(MisbahaRushGuard.Verdict.RUSH_DETECTED, atLimit.onTap(now))
        // نقرة أبوة واحدة فوق الحدّ تكسر الاستمرارية — لا كشف
        val above = guard()
        now = 0L
        repeat(6) { above.onTap(now); now += 451L }
        assertFalse(above.isPaused(now))
    }

    @Test
    fun `فاصل بطيء واحد وسط العجلة يقطع السلسلة فلا كشف`() {
        val g = guard()
        var now = 0L
        // 3 سريعة + بطيئة + 3 سريعة = 7 نقرات بلا استمرارية كاملة
        val intervals = listOf(300L, 300L, 300L, 1200L, 300L, 300L, 300L)
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
        repeat(5) { g.onTap(now); now += 400L }
        // النقرة السادسة على الفاصل نفسه (400ms) تكمل السلسلة
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
        repeat(5) { g.onTap(now); now += 400L }
        now += 400L
        g.onTap(now) // كشف
        now = g.pausedUntil + 1L
        assertEquals(MisbahaRushGuard.Verdict.COUNTED, g.onTap(now))
    }

    @Test
    fun `بعد الكشف يبدأ سجل السلاسل من جديد لا يحمل العجلة القديمة`() {
        val g = guard()
        var now = 0L
        repeat(5) { g.onTap(now); now += 400L }
        now += 400L
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
        repeat(6) { g.onTap(now); now += 400L }
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
