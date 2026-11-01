package com.sawan4art.noor.domain.tasbih

/**
 * منطق المسبحة الإلكترونية — نقيّ بلا أندرويد كي يُختبر آلياً بعقود صارمة.
 *
 * الدلالات المعتمدة:
 * - **التسلسل الثابت** (alpha26 — توجيه المالك): المسبحة تسبيح ما بعد
 *   الصلاة حصراً — ٣٣ سبحانَ الله ثم ٣٣ حمداً ثم ٣٣ تكبيراً ثم المائة
 *   مرةً واحدة. بلوغ عدّاد القسم هدفه يُكمِل القسم وينتقل تلقائياً
 *   للقسم التالي بترتيبه، وبلوغ آخر قسم يُكمِل المائة كلها ويعود لأولها
 *   ويزيد عدّاد المائات المكتملة. لا اختيار حرّ ولا أهداف متغيرة.
 * - alpha25: خرزات الحلقة أُزيلت بطلب المالك فماتت عقود الخرز مع ميزتها.
 */
object MisbahaMath {

    /**
     * نتيجة تسبيحة واحدة: العدّ الجديد وهل اكتمل القسم بهذه التسبيحة.
     * لا حالة ولا جانبية — دالة تامة تُختبر على كل الحدود.
     */
    data class Step(val count: Int, val roundCompleted: Boolean)

    /**
     * التسبيحة التالية: العدّ يتقدم واحداً، وعند بلوغ الهدف يُكمل القسم
     * ويعود للصفر في الخطوة نفسها.
     */
    fun step(count: Int, target: Int): Step {
        val next = count.coerceAtLeast(0) + 1
        return if (target >= 1 && next % target == 0) {
            Step(count = 0, roundCompleted = true)
        } else {
            Step(count = next, roundCompleted = false)
        }
    }

    /**
     * المتبقي للقسم — **هو العدّاد التنازلي المعروض** (توجيه المالك:
     * «مع عداد تنازلي»): يبدأ من العدد المسنون ويهبط تسبيحةً تسبيحة
     * حتى الصفر.
     */
    fun remaining(count: Int, target: Int): Int =
        (target - count.coerceAtLeast(0)).coerceAtLeast(0)

    /**
     * نتيجة تطبيق تسبيحة على حالة التسلسل كاملة — تُطبَّق كما هي:
     * [stepIndex] و[count] هما الحالة الجديدة، و[completedSequences]
     * عدّاد المائات بعد الخطوة، و[dhikrCompleted] اكتمال قسمٍ (اهتزاز
     * وانتقال)، و[sequenceCompleted] اكتمال المائة كلها (احتفال أكبر).
     */
    data class SequenceAdvance(
        val stepIndex: Int,
        val count: Int,
        val completedSequences: Int,
        val dhikrCompleted: Boolean,
        val sequenceCompleted: Boolean,
    )

    /**
     * خطوة داخل التسلسل الثابت — قلب «الانتقال التلقائي بالترتيب»:
     * العدّ يكبر داخل القسم حتى عدده المسنون فيكتمل القسم وينتقل
     * للذي يليه، وآخر القسم الأربعة تعود بها للحلقة الأولى وتُحسب
     * مائةً مكتملة. العدّ المحفوظ خارج حدود الهدف (من نسخة قديمة)
     * يُكلَّف داخل القسم ولا يكسر الترتيب أبداً.
     */
    fun advanceSequence(stepIndex: Int, count: Int, completedSequences: Int): SequenceAdvance {
        val idx = stepIndex.coerceIn(0, MisbahaDhikrList.SEQUENCE.lastIndex)
        val target = MisbahaDhikrList.SEQUENCE[idx].target
        val step = step(count.coerceIn(0, target - 1), target)
        if (!step.roundCompleted) {
            return SequenceAdvance(idx, step.count, completedSequences, false, false)
        }
        return if (idx == MisbahaDhikrList.SEQUENCE.lastIndex) {
            SequenceAdvance(0, 0, completedSequences + 1, true, true)
        } else {
            SequenceAdvance(idx + 1, 0, completedSequences, true, false)
        }
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
