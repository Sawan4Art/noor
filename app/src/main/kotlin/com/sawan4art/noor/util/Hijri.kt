package com.sawan4art.noor.util

import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.chrono.HijrahChronology
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.util.Locale

/**
 * التاريخ الهجري (أم القرى) — مثل Intl islamic-umalqura في المرجع.
 * java.time يدعم تقويم أم القرى أصلاً عبر HijrahChronology.
 */
object Hijri {

    private val MONTHS_AR = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
        "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
        "رمضان", "شوال", "ذو القعدة", "ذو الحجة",
    )

    data class HijriDate(val day: Int, val month: Int, val monthName: String, val year: Int) {
        val formatted: String get() = "$day $monthName $year هـ"
        val isRamadan: Boolean get() = monthName == "رمضان"
    }

    fun of(date: LocalDate): HijriDate {
        val hijrah = HijrahChronology.INSTANCE.date(date)
        val day = hijrah.get(ChronoField.DAY_OF_MONTH)
        val month = hijrah.get(ChronoField.MONTH_OF_YEAR)
        val year = hijrah.get(ChronoField.YEAR)
        val monthIndex = (month - 1).coerceIn(0, 11)
        return HijriDate(day, month, MONTHS_AR[monthIndex], year)
    }

    fun today(): HijriDate = of(LocalDate.now())

    /** التاريخ الميلادي بصيغة عربية: ٣٠ أكتوبر ٢٠٢٦ */
    fun gregorianArabic(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ar", "EG"))
        return date.format(formatter)
    }
}
