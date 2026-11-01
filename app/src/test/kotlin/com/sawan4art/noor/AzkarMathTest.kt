package com.sawan4art.noor

import com.sawan4art.noor.domain.azkar.AzkarContent
import com.sawan4art.noor.domain.azkar.AzkarMath
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقود محرك الأذكار (alpha27 — القسم الموعود):
 * «الصلاه على النبي ده ذكر لوحده .. اما في قسم الاذكار مستقبلا
 * هنحط اذكار كتير» — علامات الإتمام يومية تتجدد، والعدّ الحرّ
 * للصلاة على النبي يتصاعد بلا هدف ولا مائات.
 */
class AzkarMathTest {

    // ===== عقد مفتاح اليوم =====

    @Test
    fun `مفتاح اليوم رقم اليوم بالمنطقة المحلية`() {
        val cairo = ZoneId.of("Africa/Cairo")
        val millis = ZonedDateTime.of(2026, 11, 2, 12, 0, 0, 0, cairo)
            .toInstant().toEpochMilli()
        assertEquals(LocalDate.of(2026, 11, 2).toEpochDay(), AzkarMath.epochDayOf(millis, cairo))
    }

    @Test
    fun `مفتاح اليوم يتغير عند منتصف الليل لا قبله`() {
        val cairo = ZoneId.of("Africa/Cairo")
        val justBeforeMidnight = ZonedDateTime.of(2026, 11, 2, 23, 59, 59, 0, cairo)
            .toInstant().toEpochMilli()
        val justAfterMidnight = ZonedDateTime.of(2026, 11, 3, 0, 0, 1, 0, cairo)
            .toInstant().toEpochMilli()
        val before = AzkarMath.epochDayOf(justBeforeMidnight, cairo)
        val after = AzkarMath.epochDayOf(justAfterMidnight, cairo)
        assertEquals(LocalDate.of(2026, 11, 2).toEpochDay(), before)
        assertEquals(LocalDate.of(2026, 11, 3).toEpochDay(), after)
        assertTrue(after > before)
    }

    // ===== عقد العلامات اليومية =====

    @Test
    fun `علامات اليوم نفسه تبقى كما هي`() {
        val done = setOf("morning_kursi", "morning_radiytu")
        assertEquals(done, AzkarMath.effectiveDone(today = 20_000L, storedDay = 20_000L, storedDone = done))
    }

    @Test
    fun `يوم جديد يصفّر العلامات كاملة بلا بقايا`() {
        val done = setOf("morning_kursi", "evening_subhan_100", "sleep_bismika")
        assertTrue(AzkarMath.effectiveDone(today = 20_001L, storedDay = 20_000L, storedDone = done).isEmpty())
    }

    @Test
    fun `يوم مخزن صفري أو ناقص يبدأ نظيفاً`() {
        assertTrue(
            AzkarMath.effectiveDone(
                today = 20_000L,
                storedDay = 0L,
                storedDone = setOf("morning_kursi"),
            ).isEmpty(),
        )
    }

    @Test
    fun `تعليم ذكر غائب يضيفه وإلغاؤه ينزعه دون مساس بغيره`() {
        val done = setOf("a", "b")
        assertEquals(setOf("a", "b", "c"), AzkarMath.toggle(done, "c"))
        assertEquals(setOf("b"), AzkarMath.toggle(done, "a"))
        // الإلغاء لغير الموجود لا يضيف ولا يكسر
        assertEquals(setOf("a", "b"), AzkarMath.toggle(done, "x"))
    }

    // ===== عقد تقدم المجموعات =====

    @Test
    fun `تقدم المجموعة يحصى من ذكورها هي فقط`() {
        val group = AzkarContent.MORNING
        val done = setOf(
            "morning_kursi",
            "morning_radiytu",
            "evening_subhan_100", // لمجموعة أخرى — لا يُحصى هنا
            "sleep_bismika", // لمجموعة أخرى
        )
        val p = AzkarMath.groupProgress(done, group)
        assertEquals(2, p.doneCount)
        assertEquals(group.items.size, p.total)
        assertFalse(p.allDone)
    }

    @Test
    fun `اكتمال المجموعة كلها علامة الاكتمال`() {
        val group = AzkarContent.SLEEP
        val done = group.items.map { it.id }.toSet()
        val p = AzkarMath.groupProgress(done, group)
        assertEquals(group.items.size, p.doneCount)
        assertTrue(p.allDone)
        assertEquals(1f, p.fraction, 1e-6f)
    }

    @Test
    fun `التقدم الكسري بين صفر وواحد`() {
        val group = AzkarContent.EVENING
        val p = AzkarMath.groupProgress(setOf("evening_kursi"), group)
        assertTrue(p.fraction > 0f && p.fraction < 1f)
        assertEquals(0f, AzkarMath.groupProgress(emptySet(), group).fraction, 1e-6f)
    }

    // ===== عقد العدّ الحر للصلاة على النبي =====

    @Test
    fun `العد الحر يتصاعد واحداً بلا سقف ولا التفاف`() {
        assertEquals(1, AzkarMath.nextPraiseCount(0))
        assertEquals(2, AzkarMath.nextPraiseCount(1))
        assertEquals(10_001, AzkarMath.nextPraiseCount(10_000))
    }

    @Test
    fun `عد قديم سالب يبدأ نظيفاً لا مكسوراً`() {
        assertEquals(1, AzkarMath.nextPraiseCount(-5))
    }

    @Test
    fun `كل مائةٍ احتفال والصفر ليس مائة أبدا`() {
        assertFalse(AzkarMath.isHundredth(0))
        assertFalse(AzkarMath.isHundredth(99))
        assertTrue(AzkarMath.isHundredth(100))
        assertFalse(AzkarMath.isHundredth(101))
        assertTrue(AzkarMath.isHundredth(200))
        assertFalse(AzkarMath.isHundredth(250))
    }
}
