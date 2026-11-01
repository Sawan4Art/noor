package com.sawan4art.noor.domain.tasbih

/**
 * منطق المسبحة الإلكترونية — نقيّ بلا أندرويد كي يُختبر آلياً بعقود صارمة.
 *
 * الدلالات المعتمدة (سنة المسبحة الحقيقية):
 * - الحلقة **٣٣ خرزة** مهما كان الهدف — تُضاء خرزة بكل تسبيحة وتلتفّ
 *   من جديد (count % 33) كي يشبه العدّ السبحةً في اليد.
 * - **الهدف** عتبة دورة كاملة: بلوغه يُكمل دورة (rounds+1) ويعيد العدّ
 *   للصفر تلقائياً مع نبضة إتمام. الهدف صفر = «بلا حدّ»: العدّ يكبر
 *   بلا إعادة والحلقة تلتفّ كل ٣٣.
 */
object MisbahaMath {

    /** عدد خرزات الحلقة — سنة السبحة الشائعة */
    const val BEADS = 33

    /** الهدف الصفري = بلا حدّ */
    const val TARGET_UNLIMITED = 0

    /**
     * نتيجة تسبيحة واحدة: العدّ الجديد وهل اكتملت دورة بهذه التسبيحة.
     * لا حالة ولا جانبية — دالة تامة تُختبر على كل الحدود.
     */
    data class Step(val count: Int, val roundCompleted: Boolean)

    /**
     * التسبيحة التالية: العدّ يتقدم واحداً، وعند بلوغ الهدف يُكمل دورة
     * ويعود للصفر في الخطوة نفسها. الهدف الصفري (بلا حدّ) لا يعيد أبداً.
     */
    fun step(count: Int, target: Int): Step {
        val next = count.coerceAtLeast(0) + 1
        return if (target >= 1 && next % target == 0) {
            Step(count = 0, roundCompleted = true)
        } else {
            Step(count = next, roundCompleted = false)
        }
    }

    /** خرزات مضاءة على الحلقة — تلتفّ كل ٣٣ دائماً */
    fun beadsLit(count: Int): Int {
        val c = count.coerceAtLeast(0)
        return c % BEADS
    }

    /** نسبة تقدم الدورة الحالية 0..1 (بلا حدّ → التقدم حول الحلقة فقط) */
    fun progressFraction(count: Int, target: Int): Float {
        if (target <= 0) {
            if (count <= 0) return 0f
            val within = count % BEADS
            return if (within == 0) 1f else within.toFloat() / BEADS
        }
        return count.coerceIn(0, target).toFloat() / target
    }

    /** المتبقي لإتمام الدورة — بلا حدّ يبقى بلا متبقٍّ (null) */
    fun remaining(count: Int, target: Int): Int? {
        if (target <= 0) return null
        return (target - count.coerceAtLeast(0)).coerceAtLeast(0)
    }

    /**
     * الأرقام الهندية ٠-٩ — لغة التطبيق العربية موحّدة (نفس اتفاقية
     * TimeFormat في المواقيت والعدّاد التنازلي).
     */
    fun arabicNumber(value: Long): String {
        val latin = value.toString()
        val sb = StringBuilder(latin.length)
        for (ch in latin) {
            val d = ch - '0'
            sb.append(if (d in 0..9) ('٠' + d) else ch)
        }
        return sb.toString()
    }
}
