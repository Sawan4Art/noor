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

    /** أسماء أيام الأسبوع بالعربية — بالترتيب DayOfWeek */
    val DAY_NAMES_AR = listOf(
        "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد",
    )

    /** اسم يوم التاريخ — الجمعة/السبت… */
    fun dayNameAr(date: LocalDate): String = DAY_NAMES_AR[date.dayOfWeek.value - 1]

    /** أرقام هندية ٠١٢٣ */
    fun arabicDigits(s: String): String {
        val arabicIndic = "٠١٢٣٤٥٦٧٨٩"
        return s.map { ch -> if (ch in '0'..'9') arabicIndic[ch - '0'] else ch }
            .joinToString("")
    }

    /**
     * صيغ الهجري للويدجت (alpha57 — «التاريخ الهجري و الميلادي بكذا صيغه»):
     * ٠ كاملة «١٤ رمضان ١٤٤٧ هـ» — ١ مختصرة «١٤ رمضان» — ٢ رقمية «١٤/٩/١٤٤٧»
     * وكل صيغةٍ غريبةٍ تسير إلى الكاملة.
     */
    fun formatHijri(h: HijriDate, style: Int): String = when (style) {
        1 -> arabicDigits("${h.day} ${h.monthName}")
        2 -> arabicDigits("%02d/%02d/%04d".format(h.day, h.month, h.year))
        else -> arabicDigits(h.formatted)
    }

    /**
     * صيغ الميلادي للويدجت:
     * ٠ «السبت ٧ نوفمبر ٢٠٢٦» — ١ مختصرة «٧ نوفمبر» — ٢ رقمية «٠٧/١١/٢٠٢٦»
     * وكل صيغةٍ غريبةٍ تسير إلى الكاملة.
     */
    fun formatGregorian(d: LocalDate, style: Int): String = when (style) {
        1 -> gregorianArabic(d).substringBefore(d.year.toString()).trim()
        2 -> arabicDigits(d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        else -> "${dayNameAr(d)} ${gregorianArabic(d)}"
    }

    /** التاريخ الميلادي بصيغة عربية: ٣٠ أكتوبر ٢٠٢٦ */
    fun gregorianArabic(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ar", "EG"))
        return date.format(formatter)
    }
}
