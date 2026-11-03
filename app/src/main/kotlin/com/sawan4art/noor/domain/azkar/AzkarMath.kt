package com.sawan4art.noor.domain.azkar

import java.time.Instant
import java.time.ZoneId

/**
 * محرك الأذكار — نقيّ بلا أندرويد كي يُختبر آلياً بعقود صارمة، على
 * نمط MisbahaMath نفسه:
 *
 * - **تقدّم المجموعة يوميّ**: علامات الإتمام تنتمي ليومٍ واحد
 *   (مفتاح اليوم = رقم اليوم epochDay بالمنطقة المحلية) ويتجدد
 *   الصفر مع كل يوم جديد تلقائياً — لا إجماليات تراكمية (درس
 *   المالك: «تشيل عداد الاجمالي اللي فوق خالص»).
 * - **الصلاة على النبي عدّاد حرّ**: يتصاعد ولا سقف له ولا هدف،
 *   ويُحتفى بكل مائةٍ (المائة = مائة صلاة كاملة تُكتب لصاحبها).
 */
object AzkarMath {

    /**
     * مفتاح اليوم — رقم اليوم (epochDay) للتوقيت المعطى بالمنطقة
     * المحلية. المنطقة تُمرَّر من المستدعي كي يبقى الحساب تاماً
     * قابلاً للاختبار على أي منطقة وتاريخ.
     */
    fun epochDayOf(millis: Long, zone: ZoneId): Long =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().toEpochDay()

    /**
     * العلامات الفعلّية اليوم: لو مخزَّن اليوم هو نفسه يوم المجموعة
     * تُعاد العلامات كما هي، وإلا فصفرٌ نظيف — بلا أي بقايا أمس.
     */
    fun effectiveDone(today: Long, storedDay: Long, storedDone: Set<String>): Set<String> =
        if (today == storedDay) storedDone else emptySet()

    /**
     * نتيجة نقرة عدّ على ذكرٍ بعدّه الجاري — العدّاد الرقمي للبطاقات
     * (alpha28 توجيه المالك: «كروت الاذكار تكون فيها عداد رقمي و عند
     * الانتهاء من العدد المطلوب يتغيير لون الكارت مثل الحالي»):
     *
     * - كل نقرة تُصاعد العدّ الجاري واحداً.
     * - بلوغ العدد المسنون **تكتمل** البطاقة (تُعلَّم مُتمَّة فيتغير
     *   لونها كما في التعليم القديم) ويبدأ العدّ الجاري من الصفر من
     *   جديد — فالإتمام حالة العلامة لا قيمة العدّ.
     * - نقرة البطاقة المكتملة تعيد دورةً جديدة (إلغاء التعليم والعدّ
     *   من الصفر) — هي إلغاء التعليم القديم نفسه بلسان العدّاد.
     *
     * العدد السالب أو الصفر يُعامَل مرةً واحدة، والعدّ الجاري السالب
     * يبدأ من الصفر — الدالة نقية بلا حالة.
     */
    data class RoundTap(val round: Int, val completed: Boolean)

    fun tapRound(current: Int, target: Int): RoundTap {
        val targetSafe = target.coerceAtLeast(1)
        val base = current.coerceAtLeast(0)
        return if (base + 1 >= targetSafe) {
            RoundTap(round = 0, completed = true)
        } else {
            RoundTap(round = base + 1, completed = false)
        }
    }

    /** تقدّم مجموعةٍ في يومها — المعدود [doneCount] لا يتجاوز الإجمالي أبداً */
    data class GroupProgress(val doneCount: Int, val total: Int) {
        val allDone: Boolean get() = doneCount >= total
        val fraction: Float get() = if (total <= 0) 0f else doneCount.toFloat() / total
    }

    /**
     * تقدّم المجموعة اليوم — يُحصى فقط ما يعود لذكور المجموعة نفسها
     * (علامة لمجموعةٍ أخرى لا تعد تقدّماً هنا).
     */
    fun groupProgress(done: Set<String>, group: AzkarGroup): GroupProgress {
        val ids = group.items.mapTo(HashSet()) { it.id }
        val doneCount = done.count { it in ids }
        return GroupProgress(doneCount = doneCount, total = group.items.size)
    }

    /**
     * الصلاة التالية على النبي — العدّ الحرّ يتصاعد واحداً بلا سقف:
     * لا هبوط ولا التفاف (عقد «ذكر لوحده» — لا هدف ولا مائات).
     */
    fun nextPraiseCount(count: Int): Int = count.coerceAtLeast(0) + 1

    /**
     * تطهير عدد الذكر المضاف من المستخدم (alpha44) — قيمة فاسدة
     * (صفر أو سالب أو ضخم) تعود آمنة داخل المدى المعقول ١..١٠٠٠،
     * فلا يكسر المستخدم المكتبة بعددٍ مهول ولا بعددٍ معدوم.
     */
    fun sanitizedCount(count: Int): Int = count.coerceIn(1, 1000)

    /**
     * هل هذه الصلاةُ المائةَ؟ تُحتفى بها (رشّة أقصى واهتزاز أوضح):
     * الأولى عند ١٠٠ وكل مائة بعدها — والصفر ليس مائةً أبداً.
     */
    fun isHundredth(count: Int): Boolean = count > 0 && count % 100 == 0
}
