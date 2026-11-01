package com.sawan4art.noor.domain.tasbih

/**
 * منطق المسبحة الإلكترونية — نقيّ بلا أندرويد كي يُختبر آلياً بعقود صارمة.
 *
 * الدلالات المعتمدة:
 * - **الهدف** عتبة دورة كاملة: بلوغه يُكمل دورة (rounds+1) ويعيد العدّ
 *   للصفر تلقائياً مع نبضة إتمام. الهدف صفر = «بلا حدّ»: العدّ يكبر
 *   بلا إعادة.
 * - alpha25: خرزات الحلقة أُزيلت بطلب المالك («بدل الدوائر التي يتغير
 *   لونها مع كل عد اعمل عد رقمي عادي») فماتت عقود الخرز مع ميزتها،
 *   وبقيت عقود العدّ الصافية.
 */
object MisbahaMath {

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
