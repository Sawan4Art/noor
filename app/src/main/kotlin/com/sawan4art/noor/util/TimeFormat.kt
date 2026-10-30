package com.sawan4art.noor.util

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** تنسيق الأوقات بالعربية — مطابق لسلوك toLocaleTimeString('ar-EG') في المرجع */
object TimeFormat {

    private val PRAYER_TIME_FORMAT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("h:mm a", Locale("ar", "EG"))

    /** ٤:٣٩ م — أرقام هندية وص/م مثل النسخة السابقة */
    fun prayerTime(t: ZonedDateTime): String = t.format(PRAYER_TIME_FORMAT)

    /** ٠٣:٢٤:١٥ — أرقام هندية للعدّاد التنازلي */
    fun remainingArabic(seconds: Int): String =
        formatRemaining(seconds).map { ch ->
            val digit = ch.digitToIntOrNull()
            if (digit != null) ARABIC_INDIC[digit] else ch
        }.joinToString("")

    private const val ARABIC_INDIC = "٠١٢٣٤٥٦٧٨٩"

    /** 03:24:15 — بأرقام لاتينية للاستخدام الداخلي */
    fun formatRemaining(totalSeconds: Int): String {
        val s = totalSeconds.coerceAtLeast(0)
        val hours = s / 3600
        val minutes = (s % 3600) / 60
        val secs = s % 60
        return "%02d:%02d:%02d".format(hours, minutes, secs)
    }
}
