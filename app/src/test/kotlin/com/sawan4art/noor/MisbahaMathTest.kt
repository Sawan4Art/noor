package com.sawan4art.noor

import com.sawan4art.noor.domain.tasbih.MisbahaMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقود المسبحة — العدّ الصافي بلا مجال:
 * بلوغ عدّاد القسم هدفه يُكمله ويعيد الصفر في الخطوة نفسها،
 * والأرقام هندية موحّدة بلغة التطبيق.
 * (alpha25: عقود الخرزات ماتت مع ميزتها — الدوائر أُزيلت بطلب المالك)
 * (alpha26: «بلا حدّ» ماتت مع الأهداف المتغيرة — التسلسل الثابت أعداده سنّه)
 */
class MisbahaMathTest {

    // ===== عقد التسبيحة الواحدة =====

    @Test
    fun `تسبيحة أولى تبدأ من الصفر إلى واحد بلا اكتمال`() {
        val step = MisbahaMath.step(0, 33)
        assertEquals(1, step.count)
        assertFalse(step.roundCompleted)
    }

    @Test
    fun `بلوغ الهدف يكمل القسم ويعيد الصفر في الخطوة نفسها`() {
        val step = MisbahaMath.step(32, 33)
        assertEquals(0, step.count)
        assertTrue(step.roundCompleted)
    }

    @Test
    fun `الخطوة التالية للاكتمال تبدأ عدّاً جديداً من واحد`() {
        // بعد إكتمال القسم ٣٣ يعود العدّ صفراً؛ التسبيحة الأولى في العدّ الجديد
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
    fun `الهدف المائة يكتمل عند المائة بالضبط`() {
        val at99 = MisbahaMath.step(98, 100)
        assertEquals(99, at99.count)
        assertFalse(at99.roundCompleted)
        val at100 = MisbahaMath.step(99, 100)
        assertEquals(0, at100.count)
        assertTrue(at100.roundCompleted)
    }

    @Test
    fun `هدف واحد — تكميلة المائة — يكتمل مع كل تسبيحة`() {
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

    // ===== عقد العدّاد التنازلي =====

    @Test
    fun `العداد التنازلي يبدأ من العدد المسنون يهبط حتى الصفر`() {
        assertEquals(33, MisbahaMath.remaining(0, 33))
        assertEquals(32, MisbahaMath.remaining(1, 33))
        assertEquals(1, MisbahaMath.remaining(32, 33))
        assertEquals(0, MisbahaMath.remaining(33, 33))
    }

    @Test
    fun `التنازلي لا يهبط تحت الصفر مهما زاد المدخل`() {
        assertEquals(0, MisbahaMath.remaining(50, 33))
    }

    @Test
    fun `تكميلة المائة عدّادها التنازلي واحد ثم صفر`() {
        assertEquals(1, MisbahaMath.remaining(0, 1))
        assertEquals(0, MisbahaMath.remaining(1, 1))
    }

    // ===== عقد الأرقام الهندية =====

    @Test
    fun `الأرقام الهندية تطابق لغة التطبيق`() {
        assertEquals("٠", MisbahaMath.arabicNumber(0L))
        assertEquals("٣٣", MisbahaMath.arabicNumber(33L))
        assertEquals("١٢٣٤", MisbahaMath.arabicNumber(1234L))
    }
}
