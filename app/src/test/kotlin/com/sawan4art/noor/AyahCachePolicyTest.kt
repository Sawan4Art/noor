package com.sawan4art.noor

import com.sawan4art.noor.data.mushaf.AyahAudioCache
import com.sawan4art.noor.data.mushaf.AyahAudioCache.Entry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقود سياسة إخلاء الذاكرة الذكية — بلاغ المالك ٢٠٢٦-١١-١٢: «ضع
 * سياسة لإدارة مساحة التخزين المؤقت لحماية مساحة الهاتف من الامتلاء».
 *
 * العقود تحرس أربعة أشياء:
 * ١. دون السقف لا يُمسح شيءٌ أبداً.
 * ٢. فوق السقف يمضي الأقدم استعمالاً أولاً حتى ينزل الحجم دون السقف.
 * ٣. المحميّون (آيات السير الجارية والمحضَّرة) لا يُمسّون مهما نضب
 *    السقف — والسقف يُعجز فلا مسحٌ لهم ولا مسحٌ زائدٌ لغيرهم.
 * ٤. لا مسحٌ يبعد الحجم عن السقف أكثر مما يلزم — فلا يُهدر ما
 *    استمع إليه القارئ حديثاً بلا داعٍ.
 */
class AyahCachePolicyTest {

    private val MB = 1_000_000L
    private fun entry(name: String, sizeMb: Long, modified: Long) =
        Entry("/audio/$name.mp3", sizeMb * MB, modified)

    @Test
    fun `دون السقف لا يمسح شيء`() {
        val plan = AyahAudioCache.evictionPlan(
            listOf(entry("a", 100, 1), entry("b", 100, 2)),
            capBytes = AyahAudioCache.MAX_BYTES,
            keep = emptySet(),
        )
        assertTrue(plan.isEmpty())
    }

    @Test
    fun `فوق السقف يمضي الأقدم استعمالاً أولاً`() {
        val entries = listOf(
            entry("قديم", 800, 100),
            entry("وسط", 800, 200),
            entry("جديد", 800, 300),
        )
        // العجز ٧٥٠ ميغا فقط — مسحُ الأقدم (٨٠٠) يكفي وحده
        val plan = AyahAudioCache.evictionPlan(entries, capBytes = 1_650 * MB, keep = emptySet())
        assertEquals(listOf("/audio/قديم.mp3"), plan)
        // وعجزٌ أكبر يمضي بالأقدم ثم من يليه بالترتيب نفسه
        val plan2 = AyahAudioCache.evictionPlan(entries, capBytes = 1_500 * MB, keep = emptySet())
        assertEquals(listOf("/audio/قديم.mp3", "/audio/وسط.mp3"), plan2)
    }

    @Test
    fun `الإخلاء يتوقف لحظة نزول الحجم دون السقف فلا هدر`() {
        val entries = listOf(
            entry("أ", 500, 1),
            entry("ب", 500, 2),
            entry("ج", 500, 3),
            entry("د", 500, 4),
        )
        // العجز ٢٥٠ ميغا فقط — مسحُ الأقدم (٥٠٠) يكفي فلا يُمسح غيره
        val plan = AyahAudioCache.evictionPlan(entries, capBytes = 1_750 * MB, keep = emptySet())
        assertEquals(listOf("/audio/أ.mp3"), plan)
    }

    @Test
    fun `المحميّون لا يُمسّون ولو نضب السقف`() {
        val entries = listOf(
            entry("محمي", 900, 1),
            entry("قديم", 900, 2),
        )
        val plan = AyahAudioCache.evictionPlan(
            entries, capBytes = 500 * MB, keep = setOf("/audio/محمي.mp3"),
        )
        assertEquals(listOf("/audio/قديم.mp3"), plan)
    }

    @Test
    fun `كلهم محميّون فلا مسح ولا انفجار`() {
        val entries = listOf(entry("أ", 900, 1), entry("ب", 900, 2))
        val plan = AyahAudioCache.evictionPlan(
            entries, capBytes = 100 * MB, keep = entries.map { it.path }.toSet(),
        )
        assertTrue(plan.isEmpty())
    }

    @Test
    fun `الملفات العديمة الحجم تُرمى ولا تعوق الخطة`() {
        val entries = listOf(
            entry("صفر", 0, 5),
            entry("قديم", 300, 1),
        )
        val plan = AyahAudioCache.evictionPlan(entries, capBytes = 100 * MB, keep = emptySet())
        assertEquals(listOf("/audio/قديم.mp3"), plan)
    }

    @Test
    fun `السقف الحقيقي ٢٫٥ غيغابايت كما وجه المالك`() {
        assertEquals(2_500_000_000L, AyahAudioCache.MAX_BYTES)
    }
}
