package com.sawan4art.noor

import com.sawan4art.noor.domain.tasbih.MisbahaMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقود المسبحة — دلالات السبحة الحقيقية بلا مجال:
 * ٣٣ خرزة تلتفّ، وبلوغ الهدف يُكمل دورة ويعيد العدّ للصفر في الخطوة نفسها،
 * وبلا حدّ لا إعادة ولا متبقٍّ، والأرقام هندية موحّدة بلغة التطبيق.
 */
class MisbahaMathTest {

    // ===== عقد التسبيحة الواحدة =====

    @Test
    fun `تسبيحة أولى تبدأ من الصفر إلى واحد بلا دورة`() {
        val step = MisbahaMath.step(0, 33)
        assertEquals(1, step.count)
        assertFalse(step.roundCompleted)
    }

    @Test
    fun `بلوغ الهدف يكمل الدورة ويعيد الصفر في الخطوة نفسها`() {
        val step = MisbahaMath.step(32, 33)
        assertEquals(0, step.count)
        assertTrue(step.roundCompleted)
    }

    @Test
    fun `الخطوة التالية لإتمام الدورة تبدأ وردة جديدة من واحد`() {
        // بعد إتمام وردة ٣٣ يعود العدّ صفراً؛ التسبيحة الأولى في الوردة الجديدة
        // من الصفر → واحد (والبقعة الثانية → اثنان كما في السبحة الحقيقية)
        val after = MisbahaMath.step(32, 33)
        assertEquals(0, after.count)
        val first = MisbahaMath.step(after.count, 33)
        assertEquals(1, first.count)
        assertFalse(first.roundCompleted)
        val second = MisbahaMath.step(first.count, 33)
        assertEquals(2, second.count)
    }

    @Test
    fun `الهدف ٩٩ لا يكمل دورة عند ٣٣ ولا عند ٦٦`() {
        assertFalse(MisbahaMath.step(32, 99).roundCompleted) // → 33
        assertEquals(33, MisbahaMath.step(32, 99).count)
        assertFalse(MisbahaMath.step(65, 99).roundCompleted) // → 66
        val complete = MisbahaMath.step(98, 99)
        assertTrue(complete.roundCompleted)
        assertEquals(0, complete.count)
    }

    @Test
    fun `الهدف المائة يكتمل عند المائة بالضبط`() {
        val at99 = MisbahaMath.step(98, 100)
        assertEquals(99, at99.count)
        assertFalse(at99.roundCompleted)
        val at100 = MisbahaMath.step(99, 100)
        assertEquals(0, at100.count)
        assertTrue(at100.roundCompleted)
    }

    @Test
    fun `بلا حدّ لا يكمل دورة أبداً ويكبر العدّ`() {
        val step = MisbahaMath.step(1000, MisbahaMath.TARGET_UNLIMITED)
        assertEquals(1001, step.count)
        assertFalse(step.roundCompleted)
    }

    @Test
    fun `هدف واحد يعيد الصفر مع كل تسبيحة`() {
        val step = MisbahaMath.step(0, 1)
        assertEquals(0, step.count)
        assertTrue(step.roundCompleted)
    }

    @Test
    fun `العد السالب المدخل يُعدَّل صعوداً لا يكسر`() {
        val step = MisbahaMath.step(-5, 33)
        assertEquals(1, step.count)
        assertFalse(step.roundCompleted)
    }

    // ===== عقد الحلقة (٣٣ خرزة) =====

    @Test
    fun `الخرزات المضاءة تلتف كل ثلاثة وثلاثين`() {
        assertEquals(0, MisbahaMath.beadsLit(0))
        assertEquals(1, MisbahaMath.beadsLit(1))
        assertEquals(33 % MisbahaMath.BEADS, MisbahaMath.beadsLit(33))
        assertEquals(1, MisbahaMath.beadsLit(67))
    }

    @Test
    fun `الخرزات سالبة تساوي صفراً`() {
        assertEquals(0, MisbahaMath.beadsLit(-3))
    }

    // ===== عقد التقدم والمتبقي =====

    @Test
    fun `نسبة التقدم صفر في البداية وواحد عند بلوغ الهدف`() {
        assertEquals(0f, MisbahaMath.progressFraction(0, 33), 1e-6f)
        assertEquals(1f, MisbahaMath.progressFraction(33, 33), 1e-6f)
    }

    @Test
    fun `نسبة التقدم بين الحدود دائماً`() {
        val f = MisbahaMath.progressFraction(10, 100)
        assertTrue(f in 0.0999f..0.1001f)
    }

    @Test
    fun `بلا حدّ يتقدم التقدم حول الحلقة فقط ولا يتجاوز واحداً`() {
        val f = MisbahaMath.progressFraction(40, 0)
        assertTrue(f in 0f..1f)
        val f0 = MisbahaMath.progressFraction(0, 0)
        assertEquals(0f, f0, 1e-6f)
    }

    @Test
    fun `المتبقي يهبط حتى الصفر عند الهدف`() {
        assertEquals(32, MisbahaMath.remaining(1, 33))
        assertEquals(0, MisbahaMath.remaining(33, 33))
    }

    @Test
    fun `بلا حدّ لا متبقي له`() {
        assertNull(MisbahaMath.remaining(10, 0))
    }

    // ===== عقد الأرقام الهندية =====

    @Test
    fun `الأرقام الهندية تطابق لغة التطبيق`() {
        assertEquals("٠", MisbahaMath.arabicNumber(0L))
        assertEquals("٣٣", MisbahaMath.arabicNumber(33L))
        assertEquals("١٢٣٤", MisbahaMath.arabicNumber(1234L))
    }
}
