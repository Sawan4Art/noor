package com.sawan4art.noor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.sawan4art.noor.data.update.UpdateDownloadPlan

/**
 * عقد تقطيع تنزيل التحديث (alpha61 — بلاغ المالك: «تحميل التحديث صار
 * بطيء»): الخطة تغطي كل بايتٍ مرةً واحدةً بالترتيب، والصغير قطعةً
 * واحدة، والمثالي أربعة أقطارٍ متساوية تقريباً.
 */
class UpdateDownloadPlanTest {

    @Test
    fun `صفر بايت يعود بلا قطع`() {
        assertTrue(UpdateDownloadPlan.plan(0L, 8 * 1024 * 1024, 4).isEmpty())
        assertTrue(UpdateDownloadPlan.plan(-5L, 8 * 1024 * 1024, 4).isEmpty())
    }

    @Test
    fun `النسخة الصغيرة قطعة واحدة تغطيها كلها`() {
        val plan = UpdateDownloadPlan.plan(1024L, 8 * 1024 * 1024, 4)
        assertEquals(1, plan.size)
        assertEquals(0L, plan[0].start)
        assertEquals(1023L, plan[0].endInclusive)
        assertEquals(1024L, plan[0].length())
    }

    @Test
    fun `النسخة الكبيرة أربعة أقطار متتالية بلا فجوة ولا تكرار`() {
        val total = 100_000_000L
        val plan = UpdateDownloadPlan.plan(total, 8 * 1024 * 1024, 4)
        assertEquals(4, plan.size)
        // التغطية الكاملة بترتيب: start التالية = end السابقة + ١
        assertEquals(0L, plan.first().start)
        assertEquals(total - 1, plan.last().endInclusive)
        for (i in 1 until plan.size) {
            assertEquals(plan[i - 1].endInclusive + 1, plan[i].start)
        }
        assertEquals(total, plan.sumOf { it.length() })
        // أطوال متساوية تقريباً — الفارق الأقصى بايت واحد
        val lengths = plan.map { it.length() }
        assertTrue(lengths.max() - lengths.min() <= 1L)
    }

    @Test
    fun `المتبقي يوزّع على القطع الأولى`() {
        val total = 10L
        val plan = UpdateDownloadPlan.plan(total, 1L, 4)
        assertEquals(4, plan.size)
        assertEquals(listOf(3L, 3L, 2L, 2L), plan.map { it.length() })
        assertEquals(total, plan.sumOf { it.length() })
    }

    @Test
    fun `اتصال واحد يعود قطعة واحدة دائما`() {
        val plan = UpdateDownloadPlan.plan(50_000_000L, 8 * 1024 * 1024, 1)
        assertEquals(1, plan.size)
        assertEquals(50_000_000L, plan[0].length())
    }
}
