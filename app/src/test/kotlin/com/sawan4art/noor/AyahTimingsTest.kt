package com.sawan4art.noor

import com.sawan4art.noor.data.mushaf.AyahTimings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * عقود قارئ توقيتات الآيات — بلاغ المالك ٢٠٢٦-١١-١٢: «قاعدة بيانات
 * SQLite فيها جدول timings (sura, ayah, time) لمزامنة الآيات …
 * بأعلى كفاءة لمقارنة وقت المشغل الحالي وتحديد الآية النشطة لحظياً».
 *
 * العقود تحرس أربعة أشياء:
 * ١. البناء النقي: الصفوف الفاسدة تُرمى والمكررة تنال أقدم لحظةٍ
 *    والترتيب زمنياً صارماً أياً كان ترتيب الملف.
 * ٢. تطبيع الوحدات: عمود الثواني يُعرف بأقصى قيمةٍ دون ١٠ آلاف
 *    ويُضرب في ألفٍ فالمللي ثانية عملةُ المقارنة الوحيدة.
 * ٣. «الآية النشطة عند موضعٍ» بحثٌ ثنائيٌّ بحدوده الثلاثة: قبل أول
 *    بدايةٍ وبين بدايتين وبعد آخرها.
 * ٤. مرساة البداية والنهاية لكل آية تُقرأ بحثاً ثنائياً بلا مسحٍ خطي.
 */
class AyahTimingsTest {

    // سورة نازلةٌ بترتيبٍ مثالي — البقرة المصغَّرة: آيات ١..٥
    private fun ideal() = listOf(
        1 to 0L, 2 to 4_000L, 3 to 9_500L, 4 to 15_000L, 5 to 21_000L,
    )

    @Test
    fun `البناء النقي يرمي الفاسد ويأخذ الأقدم من المكرر ويرتب زمنيا`() {
        val rows = listOf(
            3 to 9_500L,          // فوضى المقصد — والبناء يرتب
            1 to 0L,
            0 to 1_000L,          // آيةٌ بلا رقم — تُرمى
            -2 to 1_000L,         // رقمٌ سالب — يُرمى
            2 to -5L,             // زمنٌ سالب — يُرمى
            4 to 15_000L,
            2 to 4_000L,
            2 to 6_000L,          // تكرارٌ لنفس الآية — الأقدم يغلب
            5 to 21_000L,
        )
        val t = AyahTimings.buildSuraTimings(2, rows)!!
        assertEquals(listOf(1, 2, 3, 4, 5).toIntArray().toList(), t.ayahs.toList())
        assertEquals(listOf(0L, 4_000L, 9_500L, 15_000L, 21_000L), t.times.toList())
        assertEquals(4_000L, t.startTimeOf(2))
        assertEquals(9_500L, t.endTimeOf(2))
        assertEquals(-1L, t.startTimeOf(99))
        assertEquals(-1L, t.endTimeOf(5)) // الأخيرة لا نهاية لها
    }

    @Test
    fun `لا صفوفٍ سليمة تعني لا توقيتات`() {
        assertNull(AyahTimings.buildSuraTimings(1, emptyList()))
        assertNull(AyahTimings.buildSuraTimings(1, listOf(0 to 5L, -1 to 7L)))
    }

    @Test
    fun `عمود الثواني يُطبَّع إلى مللي الثانية آلياً`() {
        val t = AyahTimings.buildSuraTimings(1, listOf(1 to 0L, 2 to 3L, 3 to 7L))!!
        assertEquals(listOf(0L, 3_000L, 7_000L), t.times.toList())
    }

    @Test
    fun `عمود المللي الثانية الكبير يبقى كما هو`() {
        val t = AyahTimings.buildSuraTimings(2, listOf(1 to 0L, 2 to 7_200_000L))!!
        assertEquals(listOf(0L, 7_200_000L), t.times.toList())
    }

    @Test
    fun `الآية النشطة قبل أول بداية هي الأولى`() {
        val t = AyahTimings.buildSuraTimings(1, ideal())!!
        assertEquals(1, AyahTimings.ayahAt(t, 0L))
        assertEquals(1, AyahTimings.ayahAt(t, -100L))
    }

    @Test
    fun `الآية النشطة بين بدايتين هي الأسبق منهما`() {
        val t = AyahTimings.buildSuraTimings(1, ideal())!!
        assertEquals(2, AyahTimings.ayahAt(t, 4_000L))   // على البداية نفسها
        assertEquals(2, AyahTimings.ayahAt(t, 4_001L))   // أول جزءٍ من ثانيتها
        assertEquals(2, AyahTimings.ayahAt(t, 9_499L))   // قبيل تاليتها
        assertEquals(3, AyahTimings.ayahAt(t, 9_500L))
    }

    @Test
    fun `الآية النشطة بعد آخر بداية هي الأخيرة`() {
        val t = AyahTimings.buildSuraTimings(1, ideal())!!
        assertEquals(5, AyahTimings.ayahAt(t, 21_000L))
        assertEquals(5, AyahTimings.ayahAt(t, 99_999_999L))
    }

    @Test
    fun `المسار الحي يتقدم رتيباً بلا ارتجاع ولا قفزة`() {
        val t = AyahTimings.buildSuraTimings(1, ideal())!!
        var prev = 1
        for (ms in 0L..25_000L step 100L) {
            val a = AyahTimings.ayahAt(t, ms)
            assert(a >= prev) { "ارتجاع عند $ms" }
            prev = a
        }
        assertEquals(5, prev)
    }
}
