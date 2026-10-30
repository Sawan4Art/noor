package com.sawan4art.noor.domain.prayer

import java.time.ZonedDateTime

/** مفاتيح أوقات اليوم الستة — بالترتيب الشرعي (FAJR أولاً… ISHA أخيرًا) */
enum class PrayerTimeKey { FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA }

/** مواقيت يوم كامل لكل صلاة */
data class PrayerTimes(val times: Map<PrayerTimeKey, ZonedDateTime>) {
    operator fun get(key: PrayerTimeKey): ZonedDateTime = times.getValue(key)
}

/** معلومة الصلاة القادمة — بنفس دلالات NextPrayerInfo في المرجع */
data class NextPrayer(
    val key: PrayerTimeKey,
    val arabicName: String,
    val target: ZonedDateTime,
    val previous: ZonedDateTime,
    val remainingSeconds: Int,
    /** نسبة ما انقضى بين الصلاة السابقة والقادمة (0..100) لحلقة التقدم */
    val progressPercent: Int,
)
