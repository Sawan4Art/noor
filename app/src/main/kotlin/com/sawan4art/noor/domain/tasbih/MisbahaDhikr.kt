package com.sawan4art.noor.domain.tasbih

/**
 * أذكار المسبحة — مجموعة السنن الثابتة بعد الصلاة والمستحبات اليومية.
 * الهدف الافتراضي لكل ذكر هو عدده المسنون، والمستخدم يبدّله من الشاشة.
 */
data class MisbahaDhikr(
    val id: String,
    val text: String,
    val defaultTarget: Int,
    val targetLabel: String,
)

object MisbahaDhikrList {

    val ALL: List<MisbahaDhikr> = listOf(
        MisbahaDhikr("subhanallah", "سبحان الله", 33, "الوردة المسنونة بعد الصلاة"),
        MisbahaDhikr("alhamdulillah", "الحمد لله", 33, "الوردة المسنونة بعد الصلاة"),
        MisbahaDhikr("allahuakbar", "الله أكبر", 34, "تكملة الوردة المسنونة"),
        MisbahaDhikr("lailahaillallah", "لا إله إلا الله", 100, "أفضل الذكر يوم القيامة"),
        MisbahaDhikr("astaghfirullah", "أستغفر الله", 100, "سبّب الرزق وتفريج الهموم"),
        MisbahaDhikr("subhanallah_wabihamdih", "سبحان الله وبحمده", 100, "حُطّت به الخطايا"),
        MisbahaDhikr("salah_on_prophet", "اللهم صلّ على محمد", 100, "صلاةٌ عليك منها عشر"),
        MisbahaDhikr("hawla_wala_quwwata", "لا حول ولا قوة إلا بالله", 100, "كنز من كنوز الجنة"),
        MisbahaDhikr("subhanallah_azeem", "سبحان الله العظيم", 100, "غُرست له نخلة في الجنة"),
    )

    private val byId = ALL.associateBy { it.id }

    fun byId(id: String): MisbahaDhikr = byId[id] ?: ALL.first()

    /** الأهداف المعروضة في الشاشة — الثلاثة السننية + المائة + بلا حدّ */
    val TARGET_OPTIONS = listOf(33, 99, 100, MisbahaMath.TARGET_UNLIMITED)
}
