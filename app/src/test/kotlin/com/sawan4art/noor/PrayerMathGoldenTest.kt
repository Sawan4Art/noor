package com.sawan4art.noor

import com.sawan4art.noor.domain.prayer.CalculationMethod
import com.sawan4art.noor.domain.prayer.PrayerMath
import com.sawan4art.noor.domain.prayer.PrayerTimeKey
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * اختبار ذهبي: نتائج النسخة الكوتلنية لازم تطابق مخرجات prayerMath.ts
 * (المرجع المتحقق منه على الأجهزة) ثانيةً بثانية — أي انحراف يفشل البناء.
 * المرجع حُسب بـ bun مع TZ منطقة كل حالة في 2026-10-30 (ملف prayerMath.ts
 * من فرع legacy-web) — تطابق تام بلا أي انحراف.
 */
class PrayerMathGoldenTest {

    private fun fmt(t: ZonedDateTime): String =
        "%02d:%02d:%02d".format(t.hour, t.minute, t.second)

    @Test
    fun `cairo egypt method matches reference`() {
        val zone = ZoneId.of("Africa/Cairo")
        val times = PrayerMath.compute(
            LocalDate.of(2026, 10, 30), 30.0444, 31.2357, zone,
            CalculationMethod.EGYPT, isHanafi = false,
        )
        assertEquals("04:39:30", fmt(times[PrayerTimeKey.FAJR]))
        assertEquals("06:07:18", fmt(times[PrayerTimeKey.SUNRISE]))
        assertEquals("11:38:42", fmt(times[PrayerTimeKey.DHUHR]))
        assertEquals("14:46:22", fmt(times[PrayerTimeKey.ASR]))
        assertEquals("17:09:45", fmt(times[PrayerTimeKey.MAGHRIB]))
        assertEquals("18:28:13", fmt(times[PrayerTimeKey.ISHA]))
    }

    @Test
    fun `cairo makkah method hanafi asr matches reference`() {
        val zone = ZoneId.of("Africa/Cairo")
        val times = PrayerMath.compute(
            LocalDate.of(2026, 10, 30), 30.0444, 31.2357, zone,
            CalculationMethod.MAKKAH, isHanafi = true,
        )
        assertEquals("04:44:09", fmt(times[PrayerTimeKey.FAJR]))
        assertEquals("06:07:18", fmt(times[PrayerTimeKey.SUNRISE]))
        assertEquals("11:38:42", fmt(times[PrayerTimeKey.DHUHR]))
        assertEquals("15:32:25", fmt(times[PrayerTimeKey.ASR]))
        assertEquals("17:09:45", fmt(times[PrayerTimeKey.MAGHRIB]))
        assertEquals("18:39:45", fmt(times[PrayerTimeKey.ISHA]))
    }

    @Test
    fun `makkah umm alqura matches reference`() {
        val zone = ZoneId.of("Asia/Riyadh")
        val times = PrayerMath.compute(
            LocalDate.of(2026, 10, 30), 21.3891, 39.8579, zone,
            CalculationMethod.MAKKAH, isHanafi = false,
        )
        assertEquals("05:05:08", fmt(times[PrayerTimeKey.FAJR]))
        assertEquals("06:22:33", fmt(times[PrayerTimeKey.SUNRISE]))
        assertEquals("12:04:13", fmt(times[PrayerTimeKey.DHUHR]))
        assertEquals("15:20:15", fmt(times[PrayerTimeKey.ASR]))
        assertEquals("17:45:38", fmt(times[PrayerTimeKey.MAGHRIB]))
        // عشاء أم القرى = مغرب + 90 دقيقة
        assertEquals("19:15:38", fmt(times[PrayerTimeKey.ISHA]))
    }

    @Test
    fun `london mwl winter solstice matches reference`() {
        val zone = ZoneId.of("Europe/London")
        val times = PrayerMath.compute(
            LocalDate.of(2026, 12, 21), 51.5074, -0.1278, zone,
            CalculationMethod.MWL, isHanafi = false,
        )
        assertEquals("05:59:21", fmt(times[PrayerTimeKey.FAJR]))
        assertEquals("08:03:42", fmt(times[PrayerTimeKey.SUNRISE]))
        assertEquals("11:58:34", fmt(times[PrayerTimeKey.DHUHR]))
        assertEquals("13:37:33", fmt(times[PrayerTimeKey.ASR]))
        assertEquals("15:53:25", fmt(times[PrayerTimeKey.MAGHRIB]))
        assertEquals("17:51:06", fmt(times[PrayerTimeKey.ISHA]))
    }

    @Test
    fun `qibla and kaaba distance match reference for cairo`() {
        val qibla = PrayerMath.calculateQibla(30.0444, 31.2357)
        val distance = PrayerMath.calculateDistanceToKaaba(30.0444, 31.2357)
        assertEquals(136.1373, qibla, 0.0005)
        assertEquals(1287, distance)
    }

    @Test
    fun `next prayer picks the coming prayer and clamps progress`() {
        val zone = ZoneId.of("Africa/Cairo")
        val times = PrayerMath.compute(
            LocalDate.of(2026, 10, 30), 30.0444, 31.2357, zone,
            CalculationMethod.EGYPT, isHanafi = false,
        )
        // بين الفجر والشروق → القادمة شروق
        val between = times[PrayerTimeKey.FAJR].plusMinutes(30)
        val next = PrayerMath.nextPrayer(times, between)
        assertEquals(PrayerTimeKey.SUNRISE, next.key)
        assertEquals("الشروق", next.arabicName)
        assertEquals(true, next.progressPercent in 0..100)
        assertEquals(true, next.remainingSeconds > 0)
    }
}
