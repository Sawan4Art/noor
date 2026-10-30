package com.sawan4art.noor.domain.prayer

/**
 * طرق حساب المواقيت المعتمدة — زوايا الفجر والعشاء حسب كل جهة شرعية.
 * النقل الأمين من prayerMath.ts (المتحقق منه على الأجهزة) — القيم الذهبية في
 * PrayerMathGoldenTest تضمن تطابق النتائج مع المرجع دقيقة بدقيقة.
 */
enum class CalculationMethod(
    val id: String,
    val englishName: String,
    val arabicName: String,
    val fajrAngle: Double,
    val ishaAngle: Double?,
    val ishaIntervalMinutes: Int?,
) {
    EGYPT(
        "Egypt",
        "Egyptian General Authority of Survey",
        "الهيئة المصرية العامة للمساحة",
        19.5, 17.5, null,
    ),
    MAKKAH(
        "Makkah",
        "Umm Al-Qura University, Makkah",
        "جامعة أم القرى - مكة المكرمة",
        18.5, null, 90,
    ),
    MWL(
        "MWL",
        "Muslim World League",
        "رابطة العالم الإسلامي",
        18.0, 17.0, null,
    ),
    ISNA(
        "ISNA",
        "Islamic Society of North America",
        "الجمعية الإسلامية لأمريكا الشمالية",
        15.0, 15.0, null,
    ),
    KARACHI(
        "Karachi",
        "University of Islamic Sciences, Karachi",
        "جامعة العلوم الإسلامية بكراتشي",
        18.0, 18.0, null,
    ),
    TEHRAN(
        "Tehran",
        "Institute of Geophysics, University of Tehran",
        "معهد الجيوفيزياء بجامعة طهران",
        17.7, 14.0, null,
    );

    companion object {
        fun fromId(id: String?): CalculationMethod = entries.firstOrNull { it.id == id } ?: EGYPT
    }
}
