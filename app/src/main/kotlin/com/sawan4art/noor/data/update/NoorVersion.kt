package com.sawan4art.noor.data.update

/**
 * نسخة نور — تحليل ومقارنة صيغة «0.5.0-alpha6» (والوسم «v0.5.0-alpha6» يُقبل بأشكاله).
 *
 * قواعد المقارنة المصممة لقناة إصداراتنا:
 * • الأساس الرقمي أولاً: 0.6.0 > 0.5.0 مهما كانت المراحل (alpha9 لا يتجاوز نسخة أساس أعلى)
 * • النسخة المستقرة (بلا مرحلة) أكبر من أي مرحلة تمهيدية بنفس الأساس: 0.5.0 > 0.5.0-alpha99
 * • أسماء المراحل مرتبة: alpha < beta < rc < (اسم غير معروف)
 * • أرقام المراحل تُقارن رقماً لا حرفاً: alpha10 > alpha9 — فخ المقارنة الحرفية الشائع
 */
data class NoorVersion private constructor(
    private val numbers: List<Int>,
    private val stageRank: Int,
    private val stageNumber: Int,
) : Comparable<NoorVersion> {

    override fun compareTo(other: NoorVersion): Int {
        val size = maxOf(numbers.size, other.numbers.size)
        for (i in 0 until size) {
            val a = numbers.getOrElse(i) { 0 }
            val b = other.numbers.getOrElse(i) { 0 }
            if (a != b) return a.compareTo(b)
        }
        if (stageRank != other.stageRank) return stageRank.compareTo(other.stageRank)
        if (stageNumber != other.stageNumber) return stageNumber.compareTo(other.stageNumber)
        return 0
    }

    /** عرض مقروء بلا حرف الوسم: 0.5.0-alpha6 */
    val display: String
        get() = numbers.joinToString(".") + when (stageRank) {
            STABLE_RANK -> ""
            else -> "-${stageNameFor(stageRank)}$stageNumber"
        }

    companion object {
        /** رتبة النسخة المستقرة — أكبر من كل المراحل (alpha0 < beta0 < rc0 < مستقرة) */
        const val STABLE_RANK = 4

        private val STAGE_RANKS = mapOf("alpha" to 0, "beta" to 1, "rc" to 2, "" to 3)
        private val STAGE_NAMES = mapOf(0 to "alpha", 1 to "beta", 2 to "rc", 3 to "pre")

        /**
         * تحليل «v0.5.0-alpha6» أو «0.5.0» — يرجع null للصيغ غير المفهومة
         * (وسم غريب أو شيد) فيُتخطى بأمان ولا يُعرض كتحديث.
         */
        fun parse(raw: String?): NoorVersion? {
            if (raw.isNullOrBlank()) return null
            val text = raw.trim().removePrefix("v").removePrefix("V")
            val base = text.substringBefore('-')
            val pre = text.substringAfter('-', "").takeIf { it.isNotEmpty() }
            val numbers = base.split('.').map { it.trim() }.map { token ->
                token.toIntOrNull() ?: return null
            }
            if (numbers.isEmpty()) return null
            if (pre == null) return NoorVersion(numbers, STABLE_RANK, 0)

            val stage = pre.takeWhile { it.isLetter() }.lowercase()
            val number = pre.dropWhile { it.isLetter() }
                .trimStart('-', '.', '_')
                .takeWhile { it.isDigit() }
                .toIntOrNull() ?: 0
            val rank = STAGE_RANKS[stage] ?: 3
            return NoorVersion(numbers, rank, number)
        }

        private fun stageNameFor(rank: Int): String = STAGE_NAMES[rank] ?: "pre"

        /**
         * هل «المرشح» أحدث من «الحالية»؟
         * فشل تحليل الحالية → أي مرشح مفهوم يُعد أحدث (نسخة قديمة الصيغة).
         * فشل تحليل المرشح → لا يُعد تحديثاً أبداً.
         */
        fun isNewer(candidate: String?, current: String?): Boolean {
            val candidateVersion = parse(candidate) ?: return false
            val currentVersion = parse(current)
            return currentVersion == null || candidateVersion > currentVersion
        }
    }
}
