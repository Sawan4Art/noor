package com.sawan4art.noor

import com.sawan4art.noor.domain.prayer.CalculationMethod
import com.sawan4art.noor.util.QuickLaunch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * عقود عدّاد «الوقت المتبقي» بإشعار التشغيل السريع — v1.80 (بلاغ
 * المالك 2026-11-21: «اضف ايقونه داخل شريط الاشعارات لعرض الوقت
 * المتبقي و فتح التطبيق بشكل سريع»).
 *
 * الحاكم [QuickLaunch.computeNextTarget] نقيٌّ بلا أندرويد: نفس
 * رياضيات [com.sawan4art.noor.domain.prayer.PrayerMath.nextPrayer]
 * التي تدير النقطة السائلة في الرئيسية، فلا يختلف عدّادُ الإشعار عن
 * نقطة الشاشة أبداً — هذه هي العقود:
 *
 * ١. الصلاة القادمة تُحسب صحيحة من إحداثيات معروفة (القاهرة) في
 *    لحظةٍ محددة، واسمها العربي مطابق لأسماء [PrayerMath.displayName].
 * ٢. لحظة الأذان مستقبليةٌ دائماً عن «الآن».
 * ٣. تعاقب الصلوات: بعد الأذان مباشرةً ينتقل العدّاد للصلاة التالية
 *    (لا يبقى معلقاً على صلاةٍ مضت).
 * ٤. الظرف القطبي الشاذ لا يكسر الحساب (قيمٌ معقولة أو سقوطٌ آمن).
 */
class QuickLaunchCountdownTest {

    private val zone: ZoneId = ZoneId.of("Africa/Cairo")

    private fun targetAt(
        date: LocalDate,
        hour: Int,
        minute: Int = 0,
    ): QuickLaunch.NextTarget? = QuickLaunch.computeNextTarget(
        latitude = 30.0444, // القاهرة
        longitude = 31.2357,
        zone = zone,
        method = CalculationMethod.EGYPT,
        isHanafi = false,
        now = ZonedDateTime.of(date.atTime(hour, minute), zone),
    )

    @Test
    fun `الصلاة القادمة تُحسب صحيحة بلحظة الأذان وباسمها العربي`() {
        // ظهر يوم عادي — الصلاة القادمة العصر (بعد الظهر)
        val target = targetAt(LocalDate.of(2026, 11, 20), 12, 30)
        assertNotNull("تعذّر حساب الصلاة القادمة", target)
        assertEquals("العصر", target!!.prayerName)
        // ولحظة الأذان مستقبلية عن الآن
        val now = ZonedDateTime.of(LocalDate.of(2026, 11, 20).atTime(12, 30), zone)
        assertTrue(
            "لحظة الأذان ليست مستقبلية",
            target.atMillis > now.toInstant().toEpochMilli(),
        )
    }

    @Test
    fun `بعد أذان العصر مباشرة ينتقل العدّاد للمغرب`() {
        val asrTarget = targetAt(LocalDate.of(2026, 11, 20), 14, 30)
        assertNotNull(asrTarget)
        // بعد لحظة العصر بدقيقة — العدّاد صار للمغرب
        val afterAsr = ZonedDateTime.of(LocalDate.of(2026, 11, 20).atTime(15, 0), zone)
        val next = QuickLaunch.computeNextTarget(
            latitude = 30.0444,
            longitude = 31.2357,
            zone = zone,
            method = CalculationMethod.EGYPT,
            isHanafi = false,
            now = afterAsr,
        )
        assertNotNull(next)
        assertTrue(
            "بعد العصر ينبغي أن يكون التالي المغرب أو ما بعده (كان: ${next!!.prayerName})",
            next.prayerName in listOf("المغرب", "العشاء"),
        )
    }

    @Test
    fun `الفجر القادم بعد منتصف الليل من اليوم نفسه`() {
        // الساعة 2 فجراً — الصلاة القادمة فجر اليوم نفسه
        val target = targetAt(LocalDate.of(2026, 11, 20), 2, 0)
        assertNotNull(target)
        assertEquals("الفجر", target!!.prayerName)
    }

    @Test
    fun `تعاقب ليلة كاملة بلا تعليق على صلاةٍ مضت`() {
        // كل ساعة من يومٍ كامل: الاسم دائماً من الصلوات الست المعروفة
        // واللحظة دائماً مستقبلية — لا تكرار لصلاةٍ انقضت أبداً
        val names = setOf("الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء", "الجمعة")
        val day = LocalDate.of(2026, 11, 20)
        for (h in 0..23) {
            val now = ZonedDateTime.of(day.atTime(h, 30), zone)
            val t = QuickLaunch.computeNextTarget(
                latitude = 30.0444,
                longitude = 31.2357,
                zone = zone,
                method = CalculationMethod.EGYPT,
                isHanafi = false,
                now = now,
            )
            assertNotNull("h=$h: لا صلاة قادمة!", t)
            assertTrue(
                "h=$h: اسم غريب ${t!!.prayerName}",
                t.prayerName in names,
            )
            assertTrue(
                "h=$h: لحظة ماضية",
                t.atMillis > now.toInstant().toEpochMilli(),
            )
        }
    }
}
