package com.sawan4art.noor

import com.sawan4art.noor.domain.tasbih.MisbahaDhikrList
import com.sawan4art.noor.domain.tasbih.MisbahaMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقود التسلسل الثابت (توجيه المالك alpha26):
 * «سُبْحَانَ اللَّهِ» ٣٣ — «الْحَمْدُ لِلَّهِ» ٣٣ — «اللَّهُ أَكْبَرُ» ٣٣ —
 * «لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ
 * وَلَهُ الْحَمْدُ، وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ» ١ (تكميلة المائة)
 * «عايز بعد اتمام تسبيح معين اوتماتك ينتقل للي بعده بس بترتيب .. مجموعهم
 * ٩٩ و تمام المائه بتاعتهم .. مش عايز بقا لخبطه بين كل قسم».
 */
class MisbahaSequenceTest {

    // ===== عقد الأقسام النصية =====

    @Test
    fun `التسلسل أربعة أقسام بأعدادها المسنونة`() {
        assertEquals(4, MisbahaDhikrList.SEQUENCE.size)
        assertEquals(listOf(33, 33, 33, 1), MisbahaDhikrList.SEQUENCE.map { it.target })
    }

    @Test
    fun `نصوص الأقسام المشكولة حرفياً كما أوردها المالك`() {
        assertEquals("سُبْحَانَ اللَّهِ", MisbahaDhikrList.SEQUENCE[0].text)
        assertEquals("الْحَمْدُ لِلَّهِ", MisbahaDhikrList.SEQUENCE[1].text)
        assertEquals("اللَّهُ أَكْبَرُ", MisbahaDhikrList.SEQUENCE[2].text)
        assertEquals(
            "لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، " +
                "لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ، وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
            MisbahaDhikrList.SEQUENCE[3].text,
        )
    }

    @Test
    fun `معرفات الأقسام فريدة وأسماؤها المختصرة موجودة`() {
        val ids = MisbahaDhikrList.SEQUENCE.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(MisbahaDhikrList.SEQUENCE.all { it.shortLabel.isNotBlank() })
    }

    // ===== عقد الانتقال التلقائي بالترتيب =====

    @Test
    fun `ثلاث وثلاثون تسبيحة تُكمل القسم الأول وتنتقل للثاني تلقائياً`() {
        var idx = 0
        var count = 0
        var completed = 0
        repeat(32) {
            val adv = MisbahaMath.advanceSequence(idx, count, completed)
            assertFalse("قبل بلوغ الهدف لا اكتمال", adv.dhikrCompleted)
            idx = adv.stepIndex; count = adv.count; completed = adv.completedSequences
        }
        // التسبيحة ٣٣ تكمل القسم الأول وينتقل وحده للثاني بعدّ نظيف
        val adv = MisbahaMath.advanceSequence(idx, count, completed)
        assertTrue(adv.dhikrCompleted)
        assertFalse(adv.sequenceCompleted)
        assertEquals(1, adv.stepIndex)
        assertEquals(0, adv.count)
        assertEquals(0, adv.completedSequences)
    }

    @Test
    fun `مائة تسبيحة كاملة تمر بالترتيب وتُكمل مائةً واحدة`() {
        var idx = 0
        var count = 0
        var completed = 0
        var dhikrCompletions = 0
        var sequenceCompletions = 0
        // مسار الأقسام الذي نعرفه: كل تسبيحة يجب أن تبقى داخل الترتيب
        val visits = mutableListOf<Int>()
        repeat(100) {
            visits.add(idx)
            val adv = MisbahaMath.advanceSequence(idx, count, completed)
            idx = adv.stepIndex
            count = adv.count
            completed = adv.completedSequences
            if (adv.dhikrCompleted && !adv.sequenceCompleted) dhikrCompletions++
            if (adv.sequenceCompleted) sequenceCompletions++
        }
        // ٣٣ صفراً ثم ٣٣ واحداً ثم ٣٣ اثنين ثم ١ ثلاثة — بلا خلط
        assertEquals(List(33) { 0 } + List(33) { 1 } + List(33) { 2 } + List(1) { 3 }, visits)
        // ثلاثة انتقالات أقسام صرفة (٣٣/٣٣/٣٣) + اكتمال المائة في التسبيحة المائة
        assertEquals(3, dhikrCompletions)
        assertEquals(1, sequenceCompletions)
        assertEquals(1, completed)
        // والعودة لأول الترتيب بعددّ نظيف
        assertEquals(0, idx)
        assertEquals(0, count)
    }

    @Test
    fun `كل قسم عدّه يبدأ من الصفر عند الانتقال إليه`() {
        var idx = 0
        var count = 0
        var completed = 0
        repeat(34) {
            val adv = MisbahaMath.advanceSequence(idx, count, completed)
            idx = adv.stepIndex; count = adv.count; completed = adv.completedSequences
        }
        // بعد أول تسبيحة من الحمد لله: قسم ثانٍ وعدّه واحد لا يحمل شيئاً من السبح
        assertEquals(1, idx)
        assertEquals(1, count)
        assertEquals(0, completed)
    }

    @Test
    fun `تكميلة المائة تسبيحة واحدة تكتمل المائة وتعود لأول الترتيب`() {
        val adv = MisbahaMath.advanceSequence(3, 0, 7)
        assertTrue(adv.dhikrCompleted)
        assertTrue(adv.sequenceCompleted)
        assertEquals(0, adv.stepIndex)
        assertEquals(0, adv.count)
        assertEquals(8, adv.completedSequences)
    }

    @Test
    fun `العد المحفوظ خارج حدود الهدف يُكلَّف داخل القسم لا يكسر الترتيب`() {
        // عدّ قديم من نسخة سابقة (٥٠ في قسم عدّه ٣٣) يُعامل داخل الحدود:
        // التسبيحة التالية تُكمل القسم ولا تتجاوزه
        val adv = MisbahaMath.advanceSequence(0, 50, 0)
        assertTrue(adv.dhikrCompleted)
        assertEquals(1, adv.stepIndex)
        assertEquals(0, adv.count)
    }

    @Test
    fun `موضع قسم خارج الحدود يُطبَّع إلى آخر التسلسل`() {
        val adv = MisbahaMath.advanceSequence(99, 0, 2)
        assertTrue(adv.sequenceCompleted)
        assertEquals(2 + 1, adv.completedSequences)
    }

    @Test
    fun `العد السالب المدخل يُعدَّل لا يكسر`() {
        val adv = MisbahaMath.advanceSequence(0, -7, 0)
        assertEquals(1, adv.count)
        assertFalse(adv.dhikrCompleted)
    }
}
